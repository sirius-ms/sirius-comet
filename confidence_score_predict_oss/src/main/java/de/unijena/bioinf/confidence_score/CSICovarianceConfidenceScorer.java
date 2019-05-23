package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.svm.SVMPredict;
import de.unijena.bioinf.confidence_score.svm.SVMUtils;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.CovarianceScoring;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by martin on 20.06.18.
 */
public class CSICovarianceConfidenceScorer implements ConfidenceScorer {

    private final Map<String, TrainedSVM> trainedSVMs;
    private final CovarianceScoring covarianceScoring;
    private final CSIFingerIdScoring csiFingerIdScoring;


    public CSICovarianceConfidenceScorer(@NotNull Map<String, TrainedSVM> trainedsvms, @NotNull CovarianceScoring covarianceScoring, @NotNull CSIFingerIdScoring csiFingerIDScoring) {
        this.trainedSVMs = trainedsvms;
        this.covarianceScoring = covarianceScoring;
        this.csiFingerIdScoring = csiFingerIDScoring;
    }

    /*@Override
    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, Scored<FingerprintCandidate>[] filteredCandidates, ProbabilityFingerprint query, final long filterFlag) {
        //todo fleisch -> scoring method as input and rescore only the missing one
        //final long filterFlag

        //re-scoring the candidates?
        Scored<FingerprintCandidate>[] ranked_candidates_csiscore = new Scored[allCandidates.length];
        Scored<FingerprintCandidate>[] ranked_candidates_covscore = new Scored[allCandidates.length];

        csiFingerIdScoring.prepare(query);
        for (int i = 0; i < allCandidates.length; i++) {
            ranked_candidates_csiscore[i] = new Scored<>(allCandidates[i].getCandidate(), csiFingerIdScoring.score(query, allCandidates[i].getCandidate().getFingerprint()));
            ranked_candidates_covscore[i] = new Scored<>(allCandidates[i].getCandidate(), covarianceScoring.getScoring().score(query, allCandidates[i].getCandidate().getFingerprint()));
        }
        Arrays.sort(ranked_candidates_csiscore);
        Arrays.sort(ranked_candidates_covscore);
        return 0;
    }*/

    @Override
    public double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] ranked_candidates_covscore, Scored<FingerprintCandidate>[] ranked_candidates_csiscore, Scored<FingerprintCandidate>[] ranked_candidates_covscore_filtered, Scored<FingerprintCandidate>[] ranked_candidates_csiscore_filtered, ProbabilityFingerprint query) {
        if (ranked_candidates_covscore.length != ranked_candidates_csiscore.length)
            throw new IllegalArgumentException("Covariance scored candidate list has different length from fingerid scored candidates list!");

        if (ranked_candidates_covscore.length <= 1) {
            LoggerFactory.getLogger(getClass()).warn("Cannot calculate confidence with only one candidate in PubChem");
            return Double.NaN;
        }


        //find collision energy in spectrum
        String ce = CE_NOTHING;
        for (Ms2Spectrum spec : exp.getMs2Spectra()) {
            if (ce.equals(CE_NOTHING)) {
                ce = spec.getCollisionEnergy().toString();
            } else if (!ce.equals(spec.getCollisionEnergy().toString()) || spec.getCollisionEnergy().getMaxEnergy() != spec.getCollisionEnergy().getMinEnergy()) {
                ce = CE_RAMP;
                break;
            }
        }

        //calculate score for pubChem lists
        final CombinedFeatureCreatorALL pubchemConfidence = new CombinedFeatureCreatorALL(ranked_candidates_csiscore, ranked_candidates_covscore, csiFingerIdScoring.getPerfomances(), covarianceScoring);
        pubchemConfidence.prepare(csiFingerIdScoring.getPerfomances());
        final double[] pubchemConfidenceFeatures = pubchemConfidence.computeFeatures(query, idResult);
        final boolean sameTopHit = ranked_candidates_covscore[0] == ranked_candidates_covscore_filtered[0];
        final double pubchemConf = calculateConfidence(pubchemConfidenceFeatures, DB_ALL_ID, "", ce);

        //calculate score for filtered lists
        final CombinedFeatureCreator comb;
        final String distanceType;
        if (ranked_candidates_covscore_filtered.length > 1) {
            comb = new CombinedFeatureCreatorBIODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, ranked_candidates_csiscore_filtered, ranked_candidates_covscore_filtered, csiFingerIdScoring.getPerfomances(), covarianceScoring, pubchemConf, sameTopHit);
            distanceType = NO_DISATANCE_ID;

        } else {
            comb = new CombinedFeatureCreatorBIONODISTANCE(ranked_candidates_csiscore, ranked_candidates_covscore, ranked_candidates_csiscore_filtered, ranked_candidates_covscore_filtered, csiFingerIdScoring.getPerfomances(), covarianceScoring, pubchemConf, sameTopHit);
            distanceType = DISATANCE_ID;
        }

        comb.prepare(csiFingerIdScoring.getPerfomances());
        final double[] bioConfidenceFeatures = comb.computeFeatures(query, idResult);
        return calculateConfidence(bioConfidenceFeatures, DB_BIO_ID, distanceType, ce);
    }

    private double calculateConfidence(@NotNull double[] feature, @NotNull String dbType, @NotNull String distanceType, @NotNull String collisionEnergy) {
        final TrainedSVM svm = trainedSVMs.get(dbType + distanceType + collisionEnergy);
        final double[][] featureMatrix = new double[1][feature.length];
        featureMatrix[0] = feature;
        SVMUtils.standardize_features(featureMatrix, svm.scales);
        SVMUtils.normalize_features(featureMatrix, svm.scales);
        return new SVMPredict().predict_confidence(featureMatrix, svm)[0];
    }
}
