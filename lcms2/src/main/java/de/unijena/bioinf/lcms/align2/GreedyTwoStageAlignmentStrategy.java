package de.unijena.bioinf.lcms.align2;

import de.unijena.bioinf.ChemistryBase.algorithm.Sorting;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.statistics.TraceStats;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.traceextractor.MassOfInterestConfidenceEstimatorStrategy;
import de.unijena.bioinf.recal.MzRecalibration;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.*;

/**
 * Greedy strategy that aligns one sample after each other.
 * This is a two stage strategy where in the first stage we align all confident MoIs with each other.
 * From these, we calculate average retention time and build a backbone with these average rts and a mapping that
 * covers all single samples.
 * The second Stage then aligns all MoIs again onto these backbone.
 */
public class GreedyTwoStageAlignmentStrategy implements AlignmentStrategy{


    public AlignmentBackbone makeAlignmentBackbone(AlignmentStorage storage, List<ProcessedSample> samples, AlignmentAlgorithm algorithm, AlignmentScorer scorer) {
        List<BasicJJob<Object>> todo = new ArrayList<>();
        // sort samples by number of confident annotations
        final AlignmentStatistics stats = collectStatistics(samples);
        samples.sort(Comparator.comparingInt((ProcessedSample x)->x.getTraceStats().getNumberOfHighQualityTraces()).reversed());
        ProcessedSample first = samples.get(0);
        JobManager globalJobManager = SiriusJobs.getGlobalJobManager();
        final double[] bins = makeBins(stats.minMz, stats.maxMz);
        first.active();
        // copy all MoIs into storage
        for (MoI moi : first.getTraceStorage().getAlignmentStorage()) {
            if (moi.getConfidence()>= MassOfInterestConfidenceEstimatorStrategy.CONFIDENT) {
                storage.addMoI(moi);
            }
        }
        first.inactive();
        for (int k=1; k < samples.size(); ++k) {
            ProcessedSample S = samples.get(k);
            S.active();
            for (int i=0; i < (bins.length-1); ++i) {
                final double from = bins[i];
                final double to = bins[i+1];
                todo.add(globalJobManager.submitJob(new BasicJJob<Object>() {
                    @Override
                    protected Object compute() throws Exception {
                        final MoI[] leftSet = storage.getMoIWithin(from, to).toArray(MoI[]::new);
                        if (leftSet.length==0) return false;
                        final MoI[] rightSet = S.getTraceStorage().getAlignmentStorage().getMoIWithin(from, to).stream().
                                filter(x->x.getConfidence()>=MassOfInterestConfidenceEstimatorStrategy.CONFIDENT).toArray(MoI[]::new);
                        if (rightSet.length==0) return false;
                        algorithm.align(stats, scorer, AlignWithRecalibration.noRecalibration(), leftSet,rightSet,
                                (al, left, right, leftIndex, rightIndex) -> storage.mergeMoIs(al, left[leftIndex], right[rightIndex]),
                                (al,right,rightIndex)->storage.addMoI(AlignedMoI.merge(al, right[rightIndex]))
                        );
                        return true;
                    };
                }));
            }
            todo.forEach(JJob::takeResult);
            todo.clear();
            S.inactive();
        }
        // all mois that are aligned with at least 10% of the samples are part of the backbone
        final long[] backboneMois;
        {
            final LongArrayList backboneMoisList = new LongArrayList();
            final int minSamples = Math.max(2, (int) Math.ceil(samples.size() * 0.1));
            for (MoI m : storage) {
                if (m instanceof AlignedMoI) {
                    if (((AlignedMoI) m).getAligned().length >= minSamples) {
                        storage.addMoI(((AlignedMoI) m).finishMerging());
                        backboneMoisList.add(m.getUid());
                    }
                }
            }
            backboneMois = backboneMoisList.toLongArray();
        }
        final ScanPointMapping backboneMapping = createBackboneMapping(stats);
        // compute recalibration functions from backbone mois
        final RecalibrationFunction[] rtRecalibrations = new RecalibrationFunction[samples.size()];
        final RecalibrationFunction[] mzRecalibrations = new RecalibrationFunction[samples.size()];
        HashMap<Integer,int[]> counts = getNumberOfSamplePointsPerRegions(storage,backboneMapping, samples, backboneMois);
        double[] rtErrors = new double[samples.size()];
        for (int s=0; s < samples.size(); ++s) {
            final int sidx = s;
            todo.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<Object>() {
                @Override
                protected Object compute() throws Exception {
                    rtErrors[sidx] = recalibrateByAlignment(samples.get(sidx), storage, backboneMois, counts);
                    return true;
                }
            }));
        }
        todo.forEach(JJob::takeResult);
        todo.clear();
        stats.setExpectedRetentionTimeDeviation(Statistics.robustAverage(rtErrors));
        return AlignmentBackbone.builder().scanPointMapping(backboneMapping).samples(samples.toArray(ProcessedSample[]::new)).statistics(stats).build();
    }

    protected void cleanupOldMoIs(ProcessedSample sample, List<ProcessedSample> samples, int currentIdx, int deleteLast) {
        final IntOpenHashSet map = new IntOpenHashSet();
        for (int k=0; k < currentIdx-deleteLast; ++k) {
            map.add(samples.get(k).getUid());
        }
        sample.getTraceStorage().getAlignmentStorage().removeMoIsIf(moi->{
            if (moi instanceof AlignedMoI) {
                if (((AlignedMoI) moi).getAligned().length>1) return false;
                MoI key = ((AlignedMoI) moi).getAligned()[0];
                return map.contains(key.getSampleIdx()) || key.getConfidence()>=MassOfInterestConfidenceEstimatorStrategy.KEEP_FOR_ALIGNMENT;
            } else return true;
        });
    }

    /*
    TODO: we have to ensure that one outlier sample with huge step size does not destroy the backbone
     */
    private ScanPointMapping createBackboneMapping(AlignmentStatistics stats) {
        final double[] rts = new double[stats.maxMappingLen+1];
        final int[] scanpoints = new int[stats.maxMappingLen+1];
        rts[0] = stats.minRt;
        rts[rts.length-1] = stats.maxRt;
        final double stepSize = (stats.maxRt-stats.minRt)/rts.length;
        for (int k=1; k < rts.length-1; ++k) {
            rts[k] = rts[k-1]+stepSize;
        }
        for (int k=0; k < rts.length; ++k) {
            scanpoints[k] = k;
        }
        return new ScanPointMapping(rts, scanpoints);

    }

    protected AlignmentStatistics collectStatistics(List<ProcessedSample> samples) {
        AlignmentStatistics stats = new AlignmentStatistics();
        stats.minRt = Double.POSITIVE_INFINITY;
        stats.maxRt = Double.NEGATIVE_INFINITY;
        stats.minMz = Double.POSITIVE_INFINITY;
        stats.maxMz = Double.NEGATIVE_INFINITY;
        double mzabs=0d, mzrel=0d;
        stats.maxMappingLen = 0;
        for (ProcessedSample sample : samples) {
            final TraceStats st = sample.getTraceStats();
            final ScanPointMapping M = sample.getMapping();
            stats.minRt = Math.min(stats.minRt, M.getRetentionTimeAt(0));
            stats.maxRt = Math.max(stats.maxRt, M.getRetentionTimeAt(M.length()-1));
            stats.minMz = Math.min(stats.minMz, st.getMinMz());
            stats.maxMz = Math.max(stats.maxMz, st.getMaxMz());
            stats.maxMappingLen = Math.max(M.length(), stats.maxMappingLen);
            mzabs += st.getAverageDeviationWithinFwhm().getAbsolute();
            mzrel += st.getAverageDeviationWithinFwhm().getPpm();
        }
        mzabs /= samples.size();
        mzrel /= samples.size();
        stats.expectedRetentionTimeDeviation = (stats.maxRt-stats.minRt)/20d;
        stats.expectedMassDeviationBetweenSamples = new Deviation(mzrel, mzabs);
        return stats;
    }

    private double[] makeBins(final double minmz, final double maxmz) {
        DoubleArrayList bins = new DoubleArrayList();
        int bin=((int)minmz)-1;
        final int maxbin = ((int)maxmz+1);
        while (bin <= maxbin) {
            bins.add(bin + getsplit(bin));
            if (bin < 600) bin++;
            else if (bin < 800) bin += 2;
            else if (bin < 1000) bin += 4;
            else bin += 10;
        }
        bins.add(Double.POSITIVE_INFINITY);
        return bins.toDoubleArray();
    }

    private static double getsplit(double mz) {
        if (mz < 400) return 0.6;
        if (mz < 500) return 0.65;
        if (mz < 600) return 0.7;
        if (mz < 700) return 0.75;
        if (mz < 800) return 0.8;
        if (mz < 900) return 0.9;
        return 0d; // from here, all mass bins can be populated
    }

    @Override
    public AlignmentBackbone align(ProcessedSample merge, AlignmentBackbone backbone, List<ProcessedSample> samples, AlignmentAlgorithm algorithm, AlignmentScorer scorer) {
        AlignmentStorage storage = merge.getTraceStorage().getAlignmentStorage();
        List<BasicJJob<Object>> todo = new ArrayList<>();
        // sort samples by number of confident annotations
        final AlignmentStatistics stats = backbone.getStatistics();
        samples.sort(Comparator.comparingInt((ProcessedSample x)->x.getTraceStats().getNumberOfHighQualityTraces()).reversed());
        JobManager globalJobManager = SiriusJobs.getGlobalJobManager();
        final double[] bins = makeBins(stats.minMz, stats.maxMz);
        {
            storage.clearMoIs();
            ProcessedSample first = samples.get(0);
            first.active();
            // transfer all mois to merge sample and recalibrate them
            for (MoI moi : first.getTraceStorage().getAlignmentStorage()) {
                storage.addMoI(AlignedMoI.merge(backbone, moi));
            }
            first.inactive();
        }
        for (int k=1; k < samples.size(); ++k) {
            ProcessedSample S = samples.get(k);
            S.active();
            for (int i=0; i < (bins.length-1); ++i) {
                final double from = bins[i];
                final double to = bins[i+1];
                todo.add(globalJobManager.submitJob(new BasicJJob<Object>() {
                    @Override
                    protected Object compute() throws Exception {
                        final MoI[] leftSet = storage.getMoIWithin(from, to).toArray(MoI[]::new);
                        if (leftSet.length==0) return false;
                        final MoI[] rightSet = S.getTraceStorage().getAlignmentStorage().getMoIWithin(from, to).stream().
                                toArray(MoI[]::new);
                        if (rightSet.length==0) return false;
                        algorithm.align(stats, scorer, backbone, leftSet,rightSet,
                                (al, left, right, leftIndex, rightIndex) -> storage.mergeMoIs(al, left[leftIndex], right[rightIndex]),
                                (al, right, rightIndex) -> storage.addMoI(
                                        AlignedMoI.merge(al, right[rightIndex])
                                )
                        );
                        return true;
                    };
                }));
            }
            todo.forEach(JJob::takeResult);
            todo.clear();
            if (k > 10 && (k % 5 == 0)) cleanupOldMoIs(merge, samples, k, 5);
            S.inactive();
        }
        // all mois that are aligned with at least 10% of the samples are part of the backbone
        final long[] backboneMois;
        {
            final LongArrayList backboneMoisList = new LongArrayList();
            final LongArrayList deleteList = new LongArrayList();
            final int minSamples = Math.max(2, (int) Math.ceil(samples.size() * 0.1));
            for (MoI m : storage) {
                if (m instanceof AlignedMoI) {
                    if (((AlignedMoI) m).getAligned().length >= minSamples) {
                        storage.addMoI(((AlignedMoI) m).finishMerging());
                        backboneMoisList.add(m.getUid());
                    }
                } else {
                    deleteList.add(m.getUid());
                }
            }
            backboneMois = backboneMoisList.toLongArray();
            deleteList.forEach(storage::removeMoI);
        }
        final ScanPointMapping backboneMapping = merge.getMapping();
        // compute recalibration functions from backbone mois
        final RecalibrationFunction[] rtRecalibrations = new RecalibrationFunction[samples.size()];
        final RecalibrationFunction[] mzRecalibrations = new RecalibrationFunction[samples.size()];
        HashMap<Integer, int[]> counts = getNumberOfSamplePointsPerRegions(storage,backboneMapping, samples, backboneMois);
        DoubleArrayList rtErrors = new DoubleArrayList();
        DoubleArrayList mzErrors = new DoubleArrayList();
        DoubleArrayList ppmErrors = new DoubleArrayList();
        final List<BasicJJob<double[]>> recalJobs = new ArrayList<>();
        for (int s=0; s < samples.size(); ++s) {
            final int sidx = s;
            recalJobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<double[]>() {
                @Override
                protected double[] compute() throws Exception {
                    return recalibrateByAlignmentWithMzRecal(samples.get(sidx), storage, backboneMois, counts);
                }
            }));
        }
        for (BasicJJob<double[]> recalJob : recalJobs) {
            double[] err = recalJob.takeResult();
            rtErrors.add(err[0]);
            ppmErrors.add(err[2]);
            mzErrors.add(err[3]);
        }
        stats.setExpectedRetentionTimeDeviation(Statistics.robustAverage(rtErrors.toDoubleArray()));
        stats.setExpectedMassDeviationBetweenSamples(new Deviation(Statistics.robustAverage(ppmErrors.toDoubleArray()),Statistics.robustAverage(mzErrors.toDoubleArray())));
        cleanupOldMoIs(merge, samples, samples.size(), samples.size());
        return AlignmentBackbone.builder().statistics(stats).samples(samples.toArray(ProcessedSample[]::new)).build();
    }


    /**
     * we only do recalibration for samples for which we have enough data points distributed across the complete retention time
     * range. Otherwise, results might be biased if all data points are in the lower or upper region, because for full
     * alignment we want to take all data points into account.
     */
    private HashMap<Integer, int[]> getNumberOfSamplePointsPerRegions(AlignmentStorage storage, ScanPointMapping alignedMapping, List<ProcessedSample> samples, long[] alignments) {
        int MINBC = 3;
        HashMap<Integer, int[]> counts = new HashMap<>();
        for (ProcessedSample s : samples) counts.put(s.getUid(), new int[MINBC]);
        double span = (alignedMapping.getRetentionTimeAt(alignedMapping.length()-1)-alignedMapping.getRetentionTimeAt(0))/MINBC;
        for (long uid : alignments) {
            AlignedMoI moi = (AlignedMoI) storage.getMoI(uid);
            int region = Math.min(MINBC-1,(int)(Math.round(moi.getRetentionTime()-alignedMapping.getRetentionTimeAt(0))/span));
            for (MoI al : moi.getAligned()) {
                counts.get(al.getSampleIdx())[region]++;
            }
        }
        return counts;
    }

    // recalibrate and return average retention time error
    private double recalibrateByAlignment(ProcessedSample sample, AlignmentStorage storage, long[] alignments, HashMap<Integer, int[]> bucketCounts) {
        final int minimumBuckSize;
        double rtError = 0d;
        {
            int minbc = Integer.MAX_VALUE;
            for (int c : bucketCounts.get(sample.getUid())) {
                minbc = Math.min(c, minbc);
            }
            minimumBuckSize = minbc;
        }

        DoubleArrayList xs=new DoubleArrayList(), ys=new DoubleArrayList(), xs2 = new DoubleArrayList(), ys2 = new DoubleArrayList();
        populate(storage, xs, ys, null, null, alignments, sample.getUid());

        if (minimumBuckSize > 1 && minimumBuckSize < 25) {
            // linear recalibration
            PolynomialFunction medianLinearRecalibration;
            if (xs.size() < 500) {
                medianLinearRecalibration = MzRecalibration.getMedianLinearRecalibration(xs.toDoubleArray(), ys.toDoubleArray());
            } else {
                medianLinearRecalibration = MzRecalibration.getLinearRecalibration(xs.toDoubleArray(), ys.toDoubleArray());
            }
            for (int i=0; i < xs.size(); ++i) {
                final double recal = medianLinearRecalibration.value(xs.getDouble(i));
                final double delta = recal-ys.getDouble(i);
                rtError += Math.abs(delta);
            }
            rtError /= xs.size();

            sample.setRtRecalibration(RecalibrationFunction.linear(medianLinearRecalibration));
            sample.setMzRecalibration(RecalibrationFunction.identity());
        } else if (minimumBuckSize>=25) {
            double[] x,y;
            strictMonotonic(xs,ys);
            x=xs.toDoubleArray();
            y=ys.toDoubleArray();
            {
                PolynomialFunction linearRecalibration;
                if (x.length < 500) {
                    linearRecalibration = MzRecalibration.getMedianLinearRecalibration(x, y);
                } else {
                    linearRecalibration = MzRecalibration.getLinearRecalibration(x, y);
                }
                double bandwidth = Math.min(0.3, Math.max(0.1, (200d / x.length)));
                PolynomialSplineFunction loess = new LoessInterpolator(bandwidth, 2).interpolate(x, y);
                // get rt error
                double linError=0d,loessError=0d;
                for (int k=0; k < xs.size(); ++k) {
                    linError += Math.abs(linearRecalibration.value(xs.getDouble(k))-ys.getDouble(k));
                    loessError += Math.abs(loess.value(xs.getDouble(k))-ys.getDouble(k));
                }
                if (linError<loessError) {
                    sample.setRtRecalibration(RecalibrationFunction.linear(linearRecalibration));
                    sample.setMzRecalibration(RecalibrationFunction.identity());
                    rtError = linError/xs.size();
                } else {
                    sample.setRtRecalibration(RecalibrationFunction.loess(loess, linearRecalibration));
                    sample.setMzRecalibration(RecalibrationFunction.identity());
                    rtError = loessError/xs.size();
                }
            }
        }
        return rtError;
    }
    private double[] recalibrateByAlignmentWithMzRecal(ProcessedSample sample, AlignmentStorage storage, long[] alignments, HashMap<Integer, int[]> bucketCounts) {
        final int minimumBuckSize;
        double rtError=0d, mzError=0d, mzPPMError=0d, mzAbsError=0d;
        {
            int minbc = Integer.MAX_VALUE;
            for (int c : bucketCounts.get(sample.getUid())) {
                minbc = Math.min(c, minbc);
            }
            minimumBuckSize = minbc;
        }

        DoubleArrayList xs=new DoubleArrayList(), ys=new DoubleArrayList(), xs2 = new DoubleArrayList(), ys2 = new DoubleArrayList();
        populate(storage, xs, ys, xs2, ys2, alignments, sample.getUid());

        if (minimumBuckSize > 1 && minimumBuckSize < 25) {
            // linear recalibration
            PolynomialFunction medianLinearRecalibration, medianLinearRecalibrationMz;
            if (xs.size() < 500) {
                medianLinearRecalibration = MzRecalibration.getMedianLinearRecalibration(xs.toDoubleArray(), ys.toDoubleArray());
                medianLinearRecalibrationMz = MzRecalibration.getMedianLinearRecalibration(xs2.toDoubleArray(), ys2.toDoubleArray());
            } else {
                medianLinearRecalibration = MzRecalibration.getLinearRecalibration(xs.toDoubleArray(), ys.toDoubleArray());
                medianLinearRecalibrationMz = MzRecalibration.getLinearRecalibration(xs2.toDoubleArray(), ys2.toDoubleArray());
            }
            for (int i=0; i < xs.size(); ++i) {
                final double recal = medianLinearRecalibration.value(xs.getDouble(i));
                final double delta = recal-ys.getDouble(i);
                rtError += Math.abs(delta);
            }
            rtError /= xs.size();
            sample.setRtRecalibration(RecalibrationFunction.linear(medianLinearRecalibration));
            sample.setMzRecalibration(RecalibrationFunction.linear(medianLinearRecalibrationMz));
        } else if (minimumBuckSize>=25) {
            double linearRtError, NoCalRtError, LoessRtError;
            double[] x,y,x2,y2;
            strictMonotonic(xs,ys);
            x=xs.toDoubleArray();
            y=ys.toDoubleArray();
            strictMonotonic(xs2, ys2);
            x2=xs2.toDoubleArray();
            y2=ys2.toDoubleArray();
            {
                PolynomialFunction linearRecalibration,linearRecalibrationMz;
                if (x.length < 500) {
                    linearRecalibration = MzRecalibration.getMedianLinearRecalibration(x, y);
                    linearRecalibrationMz = MzRecalibration.getMedianLinearRecalibration(x2, y2);
                } else {
                    linearRecalibration = MzRecalibration.getLinearRecalibration(x, y);
                    linearRecalibrationMz = MzRecalibration.getLinearRecalibration(x2, y2);
                }
                double bandwidth = Math.min(0.3, Math.max(0.1, (200d / x.length)));
                PolynomialSplineFunction loess = new LoessInterpolator(bandwidth, 2).interpolate(x, y);
                PolynomialSplineFunction loessMz = new LoessInterpolator(bandwidth, 2).interpolate(x2, y2);
                // get rt error
                double linError=0d,loessError=0d, linMzError=0d, loessMzError=0d;
                for (int k=0; k < xs.size(); ++k) {
                    linError += Math.abs(linearRecalibration.value(xs.getDouble(k))-ys.getDouble(k));
                    loessError += Math.abs(loess.value(xs.getDouble(k))-ys.getDouble(k));
                }
                for (int k=0; k < xs2.size(); ++k) {
                    linMzError += Math.abs(linearRecalibrationMz.value(xs2.getDouble(k))-ys2.getDouble(k));
                    loessMzError += Math.abs(loessMz.value(xs2.getDouble(k))-ys2.getDouble(k));
                }
                if (linError<loessError) {
                    sample.setRtRecalibration(RecalibrationFunction.linear(linearRecalibration));
                    rtError = (linError/xs.size());
                } else {
                    sample.setRtRecalibration(RecalibrationFunction.loess(loess, linearRecalibration));
                    rtError = loessError/xs.size();
                }
                if (linMzError<loessMzError) {
                    sample.setMzRecalibration(RecalibrationFunction.linear(linearRecalibrationMz));
                    int large=0, small=0;
                    for (int k=0; k < xs2.size(); ++k) {
                        if (ys2.getDouble(k) > 250) {
                            ++large;
                            mzPPMError += 1e6*Math.abs(linearRecalibrationMz.value(xs2.getDouble(k))-ys2.getDouble(k))/ys2.getDouble(k);
                        } else {
                            ++small;
                            mzAbsError += Math.abs(linearRecalibrationMz.value(xs2.getDouble(k))-ys2.getDouble(k));
                        }
                    }
                    mzError = linMzError/xs.size();
                    mzPPMError /= large;
                    mzAbsError /= small;
                } else {
                    sample.setMzRecalibration(RecalibrationFunction.loess(loessMz, linearRecalibrationMz));
                    int large=0, small=0;
                    for (int k=0; k < xs2.size(); ++k) {
                        if (ys2.getDouble(k) > 250) {
                            ++large;
                            mzPPMError += 1e6*Math.abs(loessMz.value(xs2.getDouble(k))-ys2.getDouble(k))/ys2.getDouble(k);
                        } else {
                            ++small;
                            mzAbsError += Math.abs(loessMz.value(xs2.getDouble(k))-ys2.getDouble(k));
                        }
                    }
                    mzPPMError /= large;
                    mzAbsError /= small;
                    mzError = loessMzError/xs.size();
                }
            }
        }
        return new double[]{rtError, mzError, mzPPMError, mzAbsError};
    }

    private void populate(AlignmentStorage storage, DoubleArrayList xs, DoubleArrayList ys, DoubleArrayList xs2, DoubleArrayList ys2, long[] alignments, int sampleIdx) {
        for (long uid : alignments) {
            AlignedMoI moI = (AlignedMoI) storage.getMoI(uid);
            Optional<MoI> m = moI.forSampleIdx(sampleIdx);
            if (m.isPresent()) {
                xs.add(m.get().getRetentionTime());
                ys.add(moI.getRetentionTime());
                if (xs2!=null) {
                    xs2.add(m.get().getMz());
                    ys2.add(moI.getMz());
                }
            }
        }
    }

    private void strictMonotonic(DoubleArrayList xs, DoubleArrayList ys) {
        int[] idx = Sorting.argsort(xs.toDoubleArray());
        DoubleArrayList l = new DoubleArrayList(), r = new DoubleArrayList();
        for (int i = 0; i < idx.length; ++i) {
            l.add(xs.getDouble(idx[i]));
            double y = ys.getDouble(idx[i]);
            int c=1;
            while (i+1 < idx.length) {
                if (xs.getDouble(idx[i+1])==xs.getDouble(idx[i])) {
                    ++i;
                    ++c;
                    y += ys.getDouble(idx[i]);
                } else break;
            }
            y /= c;
            r.add(y);
        }
        xs.clear();ys.clear();
        xs.addAll(l);
        ys.addAll(r);
    }

}
