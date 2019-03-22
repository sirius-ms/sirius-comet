package de.unijena.bioinf.ms.io.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import de.unijena.bioinf.sirius.CSVOutputWriter;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdentificationResultSerializer implements MetaDataSerializer {
    private final static Pattern RESULT_PATTERN = Pattern.compile("(\\d+)_(.+)(_(.+))?\\.json");

    //API Methods
    @Override
    public void read(@NotNull final ExperimentResult result, @NotNull final DirectoryReader reader, @NotNull final Set<String> names) throws IOException {
        final List<IdentificationResult> results = result.getResults();

        // read trees
        if (names.contains(SiriusLocations.SIRIUS_TREES_JSON.directory)) {
            reader.env.enterDirectory(SiriusLocations.SIRIUS_TREES_JSON.directory);
            final List<String> trs = reader.env.list();
            trs.removeIf(s -> !RESULT_PATTERN.matcher(s).matches());

            for (final String s : trs) {
                Matcher m = RESULT_PATTERN.matcher(s);
                m.matches();
                final int rank = Integer.parseInt(m.group(1));
                final FTree tree = reader.env.read(s, r ->
                        new FTJsonReader().parse(new BufferedReader(r), reader.env.absolutePath(result.getAnnotationOrThrow(ExperimentDirectory.class).getDirectoryName() + "/"
                                + SiriusLocations.SIRIUS_TREES_JSON.directory + "/" + s))
                );
                results.add(new IdentificationResult(tree, rank));
            }
            reader.env.leaveDirectory();
        }
        results.sort(Comparator.comparingInt(IdentificationResult::getRank));
    }


    @Override
    public void write(@NotNull final ExperimentResult result, @NotNull final DirectoryWriter writer) throws IOException {
        if (result.getResults() != null) {
            writeIdentificationResults(result.getResults(), writer);
        }
    }


    //helper methods

    protected void writeIdentificationResults(List<IdentificationResult> results, DirectoryWriter writer) throws IOException {
        // JSON and DOT
        if (writer.isAllowed(OutputOptions.TREES_DOT) || writer.isAllowed(OutputOptions.TREES_JSON)) {
            writer.env.enterDirectory(SiriusLocations.SIRIUS_TREES_DOT.directory);
            writeTrees(results, writer);
            writer.env.leaveDirectory();
        }
        // CSV
        if (writer.isAllowed(OutputOptions.ANNOTATED_SPECTRA)) {
            writer.env.enterDirectory(SiriusLocations.SIRIUS_ANNOTATED_SPECTRA.directory);
            writeRecalibratedSpectra(results, writer);
            writer.env.leaveDirectory();
        }
        // formula summary
        writeFormulaSummary(results, writer);
    }

    private void writeFormulaSummary(final List<IdentificationResult> results, DirectoryWriter writer) throws IOException {
        writer.write(SiriusLocations.SIRIUS_SUMMARY.fileName(),
                w -> CSVOutputWriter.writeHits(w, results)
        );
    }

    protected void writeRecalibratedSpectra(List<IdentificationResult> results, DirectoryWriter writer) throws IOException {
        for (IdentificationResult result : results) {
            writeRecalibratedSpectrum(result, writer);
        }
    }

    protected void writeRecalibratedSpectrum(final IdentificationResult result, DirectoryWriter writer) throws IOException {
        writer.write(SiriusLocations.SIRIUS_ANNOTATED_SPECTRA.fileName(result),
                w -> new AnnotatedSpectrumWriter().write(w, result.getRawTree())
        );
    }

    protected void writeTrees(List<IdentificationResult> results, DirectoryWriter writer) throws IOException {
        for (IdentificationResult result : results) {
            writeJSONTree(result, writer);
            writeDOTTree(result, writer);
        }

    }

    private void writeDOTTree(final IdentificationResult result, DirectoryWriter writer) throws IOException {
        if (writer.isAllowed(OutputOptions.TREES_DOT)) {
            writer.write(SiriusLocations.SIRIUS_TREES_DOT.fileName(result), w
//                    -> new FTDotWriter(true, true).writeTree(w, result.getRawTree())
                    -> new FTDotWriter(true, true).writeTree(w, result.getResolvedTree())
            );
        }
    }

    private void writeJSONTree(final IdentificationResult result, DirectoryWriter writer) throws IOException {
        if (writer.isAllowed(OutputOptions.TREES_JSON)) {
            writer.write(SiriusLocations.SIRIUS_TREES_JSON.fileName(result), w ->
                    new FTJsonWriter().writeTree(w, result.getResolvedTree())
            );
        }
    }

}
