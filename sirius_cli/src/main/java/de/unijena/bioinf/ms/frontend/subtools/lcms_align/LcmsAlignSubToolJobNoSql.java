/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.lcms_align;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.lcms.adducts.AdductManager;
import de.unijena.bioinf.lcms.adducts.AdductNetwork;
import de.unijena.bioinf.lcms.adducts.ProjectSpaceTraceProvider;
import de.unijena.bioinf.lcms.adducts.assignment.OptimalAssignmentViaBeamSearch;
import de.unijena.bioinf.lcms.align.AlignmentBackbone;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.projectspace.SiriusProjectDocumentDbAdapter;
import de.unijena.bioinf.lcms.quality.*;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.filter.GaussFilter;
import de.unijena.bioinf.lcms.trace.filter.NoFilter;
import de.unijena.bioinf.lcms.trace.filter.SavitzkyGolayFilter;
import de.unijena.bioinf.lcms.trace.filter.WaveletFilter;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractFeature;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.CorrelatedIonPair;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Getter;
import org.apache.commons.io.function.IOSupplier;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LcmsAlignSubToolJobNoSql extends PreprocessingJob<ProjectSpaceManager> {
    List<Path> inputFiles;

    private final IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier;

    private final Set<PrecursorIonType> ionTypes;

    private final TraceSegmentationStrategy mergedTraceSegmenter;

    private final boolean alignRuns;

    private final String tag;

    @Getter
    private LongList importedCompounds = new LongArrayList();

    private final boolean saveImportedCompounds;

    private long progress;

    private long totalProgress;


    public LcmsAlignSubToolJobNoSql(InputFilesOptions input, @NotNull IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier, LcmsAlignOptions options, Set<PrecursorIonType> ionTypes) {
        this(input.msInput.msParserfiles.keySet().stream().sorted().collect(Collectors.toList()), projectSupplier, options, ionTypes);
    }

    public LcmsAlignSubToolJobNoSql(@NotNull List<Path> inputFiles, @NotNull IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier, LcmsAlignOptions options, Set<PrecursorIonType> ionTypes) {
        super();
        this.inputFiles = inputFiles;
        this.projectSupplier = projectSupplier;
        this.ionTypes = ionTypes;
        this.alignRuns = !options.noAlign;
        this.tag = options.tag;
        this.mergedTraceSegmenter = new PersistentHomology(switch (options.smoothing) {
            case AUTO -> inputFiles.size() < 3 ? new GaussFilter(0.5) : new NoFilter();
            case NOFILTER -> new NoFilter();
            case GAUSSIAN -> new GaussFilter(options.sigma);
            case WAVELET -> new WaveletFilter(options.scaleLevel);
            case SAVITZKY_GOLAY -> new SavitzkyGolayFilter();
        }, options.noiseCoefficient, options.persistenceCoefficient, options.mergeCoefficient);
        this.saveImportedCompounds = false;
    }

    public LcmsAlignSubToolJobNoSql(
            @NotNull List<Path> inputFiles,
            @NotNull IOSupplier<? extends NoSQLProjectSpaceManager> projectSupplier,
            boolean alignRuns,
            String tag,
            DataSmoothing filter,
            double sigma,
            int scale,
            double noise,
            double persistence,
            double merge,
            Set<PrecursorIonType> ionTypes,
            boolean saveImportedCompounds
    ) {
        super();
        this.inputFiles = inputFiles;
        this.projectSupplier = projectSupplier;
        this.ionTypes = ionTypes;
        this.alignRuns = alignRuns;
        this.tag = tag;
        this.mergedTraceSegmenter = new PersistentHomology(switch (filter) {
            case AUTO -> inputFiles.size() < 3 ? new GaussFilter(0.5) : new NoFilter();
            case NOFILTER -> new NoFilter();
            case GAUSSIAN -> new GaussFilter(sigma);
            case WAVELET -> new WaveletFilter(scale);
            case SAVITZKY_GOLAY -> new SavitzkyGolayFilter();
        }, noise, persistence, merge);
        this.saveImportedCompounds = saveImportedCompounds;
    }

    private void compute(SiriusProjectDatabaseImpl<? extends Database<?>> ps, Database<?> store, List<Path> files) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        LCMSProcessing processing = new LCMSProcessing(new SiriusProjectDocumentDbAdapter(ps), saveImportedCompounds);
        processing.setMergedTraceSegmentationStrategy(mergedTraceSegmenter);

        {
            updateProgress(totalProgress, progress, "Reading files");
            List<BasicJJob<ProcessedSample>> jobs = new ArrayList<>();
            int atmost = Integer.MAX_VALUE;
            for (Path f : files) {
                if (--atmost < 0) break;
                jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<ProcessedSample>() {
                    @Override
                    protected ProcessedSample compute() throws Exception {
                        ProcessedSample sample = processing.processSample(f);
                        int hasIsotopes = 0, hasNoIsotopes = 0;
                        for (MoI m : sample.getStorage().getAlignmentStorage()) {
                            if (m.hasIsotopes()) ++hasIsotopes;
                            else ++hasNoIsotopes;
                        }
                        sample.inactive();
                        System.out.println(sample.getUid() + " with " + hasIsotopes + " / " + (hasIsotopes + hasNoIsotopes) + " isotope features");
                        return sample;
                    }
                }));
            }

            int count = 0;
            for (BasicJJob<ProcessedSample> job : jobs) {
                System.out.println(job.takeResult().getUid() + " (" + ++count + " / " + jobs.size() + ")");
                updateProgress(totalProgress, ++progress, "Reading files");
            }
        }

        updateProgress(totalProgress, progress,"Aligning runs");
        AlignmentBackbone bac = processing.align();
        updateProgress(totalProgress, ++progress, "Merging runs");
        ProcessedSample merged = processing.merge(bac);
        DoubleArrayList avgAl = new DoubleArrayList();
        System.out.println("AVERAGE = " + avgAl.doubleStream().sum() / avgAl.size());
        System.out.println("Good Traces = " + avgAl.doubleStream().filter(x -> x >= 5).sum());

        updateProgress(totalProgress, ++progress, "Importing features");
        if (processing.extractFeaturesAndExportToProjectSpace(merged, bac, tag) == 0) {
            System.err.println("No features found.");
            progress += 2;
            updateProgress(totalProgress, progress, "No features");
            return;
        }

        updateProgress(totalProgress, ++progress,"Detecting adducts");
        System.out.printf("\nMerged Run: %s\n\n", merged.getRun().getName());

        final double allowedAdductRtDeviation;
        if (bac.getSamples().length <= 3) {
            FloatArrayList peakWidths = new FloatArrayList();
            for (long fid : processing.getImportedFeatureIds()) {
                ps.getStorage().getByPrimaryKey(fid, AlignedFeatures.class).ifPresent((feature)->{
                    // here we can also obtain statistics if we need them
                    Double v = feature.getFwhm();
                    if (v!=null) peakWidths.add(v.floatValue());

                });
            }
            float medianPeakWidth = 1;
            if (!peakWidths.isEmpty()) {
                peakWidths.sort(null);
                medianPeakWidth = peakWidths.getFloat(peakWidths.size()/2);
            }
            allowedAdductRtDeviation = Math.max(1,medianPeakWidth);
        } else {
            allowedAdductRtDeviation = bac.getStatistics().getExpectedRetentionTimeDeviation();
        }
        LoggerFactory.getLogger(LcmsAlignSubToolJobNoSql.class).info("Use " + allowedAdductRtDeviation + " s as allowed deviation between adducts" );

        AdductManager adductManager = new AdductManager(merged.getPolarity());
        if (merged.getPolarity()>0){
            adductManager.add(Set.of(PrecursorIonType.getPrecursorIonType("[M+H]+"), PrecursorIonType.getPrecursorIonType("[M+Na]+"),
                            PrecursorIonType.getPrecursorIonType("[M+K]+"),  PrecursorIonType.getPrecursorIonType("[M+NH3+H]+"),
                            PrecursorIonType.getPrecursorIonType("[M + FA + H]+"),
                            PrecursorIonType.getPrecursorIonType("[M + ACN + H]+"),

                            PrecursorIonType.getPrecursorIonType("[M - H2O + H]+"),

                            PrecursorIonType.getPrecursorIonType("[2M + Na]+"),
                            PrecursorIonType.getPrecursorIonType("[2M + H]+"),
                            PrecursorIonType.getPrecursorIonType("[2M + K]+")
                    )
            );
        } else {
            adductManager.add(Set.of(PrecursorIonType.getPrecursorIonType("[M-H]-"), PrecursorIonType.getPrecursorIonType("[M+Cl]-"),
                            PrecursorIonType.getPrecursorIonType("[M+Br]-"),
                            PrecursorIonType.getPrecursorIonType("[2M + H]-"),
                            PrecursorIonType.getPrecursorIonType("[2M + Br]-"),
                            PrecursorIonType.getPrecursorIonType("[2M + Cl]-"),
                            PrecursorIonType.fromString("[M+Na-2H]-"),
                            PrecursorIonType.fromString("[M + CH2O2 - H]-"),
                            PrecursorIonType.fromString("[M + C2H4O2 - H]-"),
                            PrecursorIonType.fromString("[M + H2O - H]-"),
                            PrecursorIonType.fromString("[M - H3N - H]-"),
                            PrecursorIonType.fromString("[M - CO2 - H]-"),
                            PrecursorIonType.fromString("[M - CH2O3 - H]-"),
                            PrecursorIonType.fromString("[M - CH3 - H]-")
                    )
            );
        }

        // -_- na toll, die Liste ist nicht identisch mit den Configs. Macht irgendwie auch Sinn. Ich will aber ungern
        // Multimere in die AductSettings reinpacken, das zu debuggen wird die Hoelle. Machen wir ein andern Mal.
        adductManager.add(((merged.getPolarity()<0) ? PeriodicTable.getInstance().getNegativeAdducts() : PeriodicTable.getInstance().getPositiveAdducts()).stream().filter(PrecursorIonType::isMultimere).collect(Collectors.toSet()));
        ProjectSpaceTraceProvider provider = new ProjectSpaceTraceProvider(ps);
        {
            AdductNetwork network = new AdductNetwork(provider,
                    store.findAllStr(AlignedFeatures.class)
                            .filter(f -> f.getApexIntensity() != null)
                            .filter(AbstractFeature::isRTInterval)
                            .toArray(AlignedFeatures[]::new),
                    adductManager, allowedAdductRtDeviation);
            network.buildNetworkFromMassDeltas(SiriusJobs.getGlobalJobManager());
            network.assign(SiriusJobs.getGlobalJobManager(), new OptimalAssignmentViaBeamSearch(), merged.getPolarity(), (compound) -> {
                try {
                    groupFeaturesToCompound(store, compound);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        updateProgress(totalProgress, ++progress, "Assessing data quality");
        // quality assessment
        HashMap<DataQuality, Integer> countMap = new HashMap<>();
        for (DataQuality q : DataQuality.values()) {
            countMap.put(q, 0);
        }
        final QualityAssessment qa = alignRuns ? new QualityAssessment(): new QualityAssessment(List.of(new CheckPeakQuality(), new CheckIsotopeQuality(), new CheckMs2Quality(), new CheckAdductQuality()));
        ArrayList<BasicJJob<DataQuality>> jobs = new ArrayList<>();
        store.fetchChild(merged.getRun(), "runId", "retentionTimeAxis", RetentionTimeAxis.class);
        ps.fetchLCMSRuns((MergedLCMSRun) merged.getRun());
        store.findStr(Filter.where("runId").eq(merged.getRun().getRunId()), AlignedFeatures.class).filter(x->x.getDataQuality()==DataQuality.NOT_APPLICABLE).forEach(feature -> {
            jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<DataQuality>() {
                @Override
                protected DataQuality compute() throws Exception {
                    QualityReport report = QualityReport.withDefaultCategories(alignRuns);
                    ps.fetchFeatures(feature);
                    ps.fetchIsotopicFeatures(feature);
                    ps.fetchMsData(feature);
                    try {
                        qa.addToReport(report, (MergedLCMSRun) merged.getRun(), feature, provider);
                        report.setAlignedFeatureId(feature.getAlignedFeatureId());
                        feature.setDataQuality(report.getOverallQuality());
                        store.insert(report);
                        store.upsert(feature);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return report.getOverallQuality();
                }
            }));
        });
        jobs.forEach(x->{
            DataQuality q = x.takeResult();
            countMap.put(q, countMap.get(q)+1);
        });

        System.out.printf(
                """
                       
                        -------- Preprocessing Summary ---------
                        Preprocessed data in:      %s
                        # Good Al. Features:           %d
                        # Decent Al. Features:         %d
                        # Bad Al. Features:            %d
                        # Lowest Quality Al. Features: %d
                       \s""",
                stopWatch,
                countMap.get(DataQuality.GOOD), countMap.get(DataQuality.DECENT), countMap.get(DataQuality.BAD), countMap.get(DataQuality.LOWEST)
        );
    }

    @Override
    protected NoSQLProjectSpaceManager compute() throws Exception {
        importedCompounds.clear();
        NoSQLProjectSpaceManager space = projectSupplier.get();
        SiriusProjectDatabaseImpl<? extends Database<?>> ps = space.getProject();
        Database<?> store = space.getProject().getStorage();

        progress = 0;
        if (alignRuns) {
            totalProgress = inputFiles.size() + 5L;
            compute(ps, store, inputFiles);
        } else {
            // TODO parallelize
            totalProgress = inputFiles.size() * 5L + 1;
            int atmost = Integer.MAX_VALUE;
            for (Path f : inputFiles) {
                if (--atmost < 0) break;
                compute(ps, store, List.of(f));
            }
        }
        updateProgress(totalProgress, totalProgress, "Done");

        return space;
    }

    private static void groupFeaturesToCompound(Database<?> ps, Compound compound) throws IOException {
        ps.insert(compound);
        for (CorrelatedIonPair pair : compound.getCorrelatedIonPairs().get()) {
            ps.insert(pair);
        }
        List<AlignedFeatures> adducts = compound.getAdductFeatures().get();
        for (AlignedFeatures f : adducts) {
            if (f.getCompoundId() == null || f.getCompoundId() != compound.getCompoundId()) {
                f.setCompoundId(compound.getCompoundId());
                ps.upsert(f);
            }
        }
        final SimpleMutableSpectrum ms1Spectra = new SimpleMutableSpectrum();
        List<MSData> msDataList = new ArrayList<>();
        for (int f = 0; f < adducts.size(); ++f) {
            List<MSData> ms = ps.findStr(Filter.where("alignedFeatureId").eq(adducts.get(f).getAlignedFeatureId()), MSData.class).toList();
            if (!ms.isEmpty()) {
                MSData m = ms.get(0);
                msDataList.add(m);
                if (m.getIsotopePattern() != null) {
                    SimpleSpectrum b = m.getIsotopePattern();
                    for (int i = 0; i < b.size(); ++i) {
                        ms1Spectra.addPeak(b.getMzAt(i), b.getIntensityAt(i) * adducts.get(f).getApexIntensity());
                    }
                }
            }
        }
        SimpleSpectrum ms1 = new SimpleSpectrum(ms1Spectra);
        for (MSData m : msDataList) {
            m.setMergedMs1Spectrum(ms1);
            ps.upsert(m);
        }
    }
}


