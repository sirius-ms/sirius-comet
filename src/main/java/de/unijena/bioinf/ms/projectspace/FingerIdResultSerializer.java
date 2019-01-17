package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.CSVExporter;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.db.CustomDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDatabases;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.fingerid.webapi.VersionsInfo;
import de.unijena.bioinf.fingerid.webapi.WebAPI;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//todo handle fingerprint version correctly
public class FingerIdResultSerializer implements MetaDataSerializer, SummaryWriter {
    private static Pattern DBPAT = Pattern.compile("([^(])+\\(([^)]+)\\)");

    protected final WebAPI api;

    public FingerIdResultSerializer(WebAPI api) {
        this.api = api;
    }

    @Override
    public void read(@NotNull final ExperimentResult result, @NotNull final DirectoryReader reader, @NotNull Set<String> names) throws IOException {
        final DirectoryReader.ReadingEnvironment env = reader.env;
        final List<IdentificationResult> results = result.getResults();

        if (!new HashSet<>(env.list()).contains(FingerIdLocations.FINGERID_CANDIDATES.directory)) return;

        Map<String, String> versionInfo = reader.env.readKeyValueFile(FingerIdLocations.SIRIUS_VERSION_FILE.fileName());


        try {
            env.enterDirectory(FingerIdLocations.FINGERID_CANDIDATES.directory);
            // read compound candidates identificationResult list
            final HashSet<String> files = new HashSet<>(env.list());
            for (IdentificationResult r : results) {
                String s = FingerIdLocations.FINGERID_CANDIDATES.fileName(r);
                if (files.contains(s)) {
                    r.setAnnotation(FingerIdResult.class, env.read(s, r1 -> {
                        BufferedReader br = new BufferedReader(r1);
                        String line = br.readLine();
                        final List<Scored<FingerprintCandidate>> fpcs = new ArrayList<>();
                        double confidence = 0;
                        while ((line = br.readLine()) != null) {
                            String[] tabs = line.split("\t");
                            final FingerprintCandidate fpc = new FingerprintCandidate(new InChI(tabs[0], tabs[1]), null);
                            fpc.setName(tabs[5]);
                            fpc.setSmiles(tabs[6]);
                            final List<DBLink> links = new ArrayList<>();
                            for (String pubchemId : tabs[8].split(";")) {
                                links.add(new DBLink(DatasourceService.Sources.PUBCHEM.name, pubchemId));
                            }
                            for (String dbPair : tabs[9].split(";")) {
                                final Matcher m = DBPAT.matcher(dbPair);
                                if (m.find()) {
                                    final String dbName = m.group(1);
                                    for (String id : m.group(2).split(" ")) {
                                        links.add(new DBLink(dbName, id));
                                    }
                                }
                            }
                            fpc.setLinks(links.toArray(new DBLink[links.size()]));
                            fpcs.add(new Scored<>(fpc, Double.parseDouble(tabs[4])));
                        }
                        return new FingerIdResult(fpcs, 0, null, null);
                    }));
                }
            }
        } finally {
            env.leaveDirectory();
        }

        //read Fingerprints
        if (!new HashSet<>(env.list()).contains(FingerIdLocations.FINGERID_FINGERPRINT.directory)) return;
        if (isFingerIdCompatible(versionInfo.get("csi:fingerid"))) return;




        for (IdentificationResult r : results) {
            env.enterDirectory(FingerIdLocations.FINGERID_FINGERPRINT.directory);

            if (r.hasAnnotation(FingerIdResult.class)) {
                final FingerIdResult fingerIdResult = r.getAnnotation(FingerIdResult.class);

                //read fingerprint
                try {
                    fingerIdResult.setPredictedFingerprint(env.read(FingerIdLocations.FINGERID_FINGERPRINT.fileName(r), w -> {
                        return new ProbabilityFingerprint(
                                api.getFingerprintMaskedVersion(result.getExperiment().getPrecursorIonType().getCharge()), new BufferedReader(w).lines().mapToDouble(Double::valueOf).toArray());
                    }));
                } catch (IllegalArgumentException e) {
                    LoggerFactory.getLogger(getClass()).warn("Fingerprint version of the imported data is imcompatible with the current version. " +
                            "Fingerpringerprint has to be recomputed!", e);
                }


                //read fingerprint meta data
                Map<String, String> expInfo = env.readKeyValueFile(FingerIdLocations.FINGERID_FINGERPRINT_INFO.fileName(r));
                //readConfidence
                if (expInfo.containsKey("csi_confidence"))
                    fingerIdResult.setConfidence(Double.valueOf(expInfo.get("csi_confidence")));

                final String db = expInfo.get("searched_db");
                if (db != null && !db.equals("unknown")) {
                    if (db.equals(SearchableDatabases.getPubchemDb().name())) {
                        fingerIdResult.setAnnotation(SearchableDatabase.class, SearchableDatabases.getPubchemDb());
                    } else if (db.equals(SearchableDatabases.getBioDb().name())) {
                        fingerIdResult.setAnnotation(SearchableDatabase.class, SearchableDatabases.getBioDb());
                    } else { //custom dbs
                        for (CustomDatabase customDatabase : SearchableDatabases.getCustomDatabases()) {
                            if (customDatabase.name().equals(db)) {
                                fingerIdResult.setAnnotation(SearchableDatabase.class, customDatabase);
                                break;
                            }
                        }
                    }
                }


            }
            env.leaveDirectory();
        }
    }


    @Override
    public void write(@NotNull final ExperimentResult input, @NotNull final DirectoryWriter writer) throws IOException {
        final DirectoryWriter.WritingEnvironment W = writer.env;
        final List<IdentificationResult> results = input.getResults();

        if (writer.isAllowed(FingerIdResult.CANDIDATE_LISTS) && hasFingerId(results)) {
            // now write CSI:FingerID candidates
            W.enterDirectory(FingerIdLocations.FINGERID_CANDIDATES.directory);
            final List<FingerIdResult> frs = new ArrayList<>();
            for (IdentificationResult result : results) {
                final FingerIdResult f = result.getAnnotation(FingerIdResult.class);
                if (f != null) {
                    frs.add(f);
                    writeFingerIdResult(result, f, writer);
                }
            }
            W.leaveDirectory();


            // now write CSI:FingerID fingerprints
            W.enterDirectory(FingerIdLocations.FINGERID_FINGERPRINT.directory);
            for (IdentificationResult result : results) {
                final FingerIdResult f = result.getAnnotation(FingerIdResult.class);
                if (f != null && f.getPredictedFingerprint() != null) {
                    writeFingerprint(result, f.getPredictedFingerprint(), writer);

                    final String db;
                    if (f.hasAnnotation(SearchableDatabase.class))
                        db = f.getAnnotation(SearchableDatabase.class).name();
                    else db = "unknown";

                    //write additional information
                    writer.write(FingerIdLocations.FINGERID_FINGERPRINT_INFO.fileName(result), w -> {
                        w.write("confidence\t" + f.getConfidence());
                        w.write(System.lineSeparator());
                        w.write("searched_db\t" + db);
                        w.write(System.lineSeparator());
                        w.flush();
                    });
                }
            }
            W.leaveDirectory();


            // and CSI:FingerID summary
            writeFingerIdResultsSummaryCSV(frs, writer);
        }
    }

    private void writeFingerprint(final IdentificationResult result, final ProbabilityFingerprint pfp, final DirectoryWriter writer) throws IOException {
        writer.write(FingerIdLocations.FINGERID_FINGERPRINT.fileName(result), w -> {
            for (FPIter fp : pfp) {
                w.write(String.format(Locale.US, "%.3f\n", fp.getProbability()));
            }
        });
    }

    private boolean isFingerIdCompatible(@Nullable final String version) {
        DefaultArtifactVersion needed = new DefaultArtifactVersion(FingerIDProperties.fingeridVersion());

        boolean r = false;
        if (version != null)
            r = VersionsInfo.areMinorEqual(needed, new DefaultArtifactVersion(version));
        if (!r)
            LoggerFactory.getLogger(getClass()).warn("CSI:FingerID Fingerprints cannot be imported due to Version incompatibility. Expected: " + needed + " Found in ProjectSpace: " + version);
        return r;
    }

    private boolean hasFingerId(List<IdentificationResult> results) {
        for (IdentificationResult r : results) {
            if (r.hasAnnotation(FingerIdResult.class)) return true;
        }
        return false;
    }

    private void writeFingerIdResultsSummaryCSV(final List<FingerIdResult> frs, DirectoryWriter writer) throws IOException {
        final StringWriter w = new StringWriter(128);
        new CSVExporter().exportFingerIdResults(w, frs);
        writer.write(FingerIdLocations.FINGERID_SUMMARY.fileName(), w1 -> w1.write(w.toString()));
    }

    private void writeFingerIdResult(final IdentificationResult result, final FingerIdResult f, DirectoryWriter writer) throws IOException {
        final String name = SiriusLocations.makeFileName(result);
        //write candidate list
        writer.write(FingerIdLocations.FINGERID_CANDIDATES.fileName(result), w ->
                new CSVExporter().exportFingerIdResults(w, Arrays.asList(f))
        );
    }

    @Override
    public void writeSummary(Iterable<ExperimentResult> experiments, DirectoryWriter writer) {
        try {
            FingerprintVersion csiVersion = null;
            final List<Scored<String>> topHits = new ArrayList<>();
            if (writer.isAllowed(FingerIdResult.CANDIDATE_LISTS)) {
                for (ExperimentResult experimentResult : experiments) {
                    final List<IdentificationResult> results = experimentResult.getResults();
                    if (hasFingerId(results)) {
                        final List<FingerIdResult> frs = results.stream().map((r) -> r.getAnnotation(FingerIdResult.class))
                                .filter(Objects::nonNull).collect(Collectors.toList());
                        final StringWriter w = new StringWriter(128);
                        new CSVExporter().exportFingerIdResults(w, frs);
                        final String topHit = w.toString();
                        final double confidence = frs.size() > 0 ? frs.get(0).getConfidence() : 0;
                        final String[] lines = topHit.split("\n", 3);
                        if (lines.length >= 2) {
                            topHits.add(new Scored<>(experimentResult.getExperimentSource() + "\t" + experimentResult.getExperimentName() + "\t" + confidence + "\t" + lines[1] + "\n", confidence));
                        }

                        if (csiVersion == null && !frs.isEmpty())
                            csiVersion = frs.get(0).getPredictedFingerprint().getFingerprintVersion();
                    }
                }

                if (topHits.size() > 0) {
                    writeSummaryCSV(topHits, writer);
                }

                writeFingerprintIndex(FingerIdLocations.FINGERID_FINGERPRINT_INDEX.fileName(), csiVersion, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writeSummaryCSV(List<Scored<String>> topHits, DirectoryWriter writer) throws IOException {
        writer.write(FingerIdLocations.FINGERID_SUMMARY.fileName(), w -> {
            Collections.sort(topHits, Scored.<String>desc());
            w.write("source\texperimentName\tconfidence\tinchikey2D\tinchi\tmolecularFormula\trank\tscore\tname\tsmiles\txlogp\tpubchemids\tlinks\n");
            for (Scored<String> s : topHits) {
                w.write(s.getCandidate());
            }
        });
    }

    private void writeFingerprintIndex(String filename, FingerprintVersion version, DirectoryWriter writer) throws IOException {
        if (version == null) return;
        writer.write(filename, (w) -> {
            final int[] indizes;
            if (version instanceof MaskedFingerprintVersion) {
                indizes = ((MaskedFingerprintVersion) version).allowedIndizes();
            } else {
                indizes = new int[version.size()];
                for (int i = 0; i < indizes.length; ++i) indizes[i] = i;
            }
            w.write("relativeIndex\tabsoluteIndex\tdescription\n");
            int k = 0;
            for (int index : indizes) {
                final MolecularProperty prop = (MolecularProperty) version.getMolecularProperty(index);
                w.write(String.valueOf(k++));
                w.write('\t');
                w.write(String.valueOf(index));
                w.write('\t');
                w.write(prop.getDescription());
                w.write('\n');
            }
        });
    }

    @Override
    public Map<String, String> getVersionInfo() {
        return Collections.singletonMap("csi:fingerid", FingerIDProperties.fingeridVersion());
    }
}
