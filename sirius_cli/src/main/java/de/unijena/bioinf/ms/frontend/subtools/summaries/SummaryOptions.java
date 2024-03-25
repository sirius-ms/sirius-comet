/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.ms.frontend.subtools.export.tables.PredictionsOptions;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "write-summaries", aliases = {"W"}, description = "<STANDALONE, POSTPROCESSING> Write Summary files from a given project-space into the given project-space or a custom location.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SummaryOptions implements PostprocessingTool<SummarySubToolJob>, StandaloneTool<SummarySubToolJob> {

    //specify negated  name since default is true ->  special picocli behavior
    //https://picocli.info/#_negatable_options
    @CommandLine.Option(names = {"--no-top-hit-summary"}, description = "Write project wide summary files with all Top Hits.", defaultValue = "true", negatable = true)
    protected boolean topHitSummary;

    public boolean isTopHitSummary() {
        return topHitSummary;
    }

    @CommandLine.Option(names = {"--top-hit-adduct-summary"}, description = "Write project wide summary files with all Top Hits and their adducts", defaultValue = "false", negatable = true)
    protected boolean topHitWithAdductsSummary;

    public boolean isTopHitWithAdductsSummary() {
        return topHitWithAdductsSummary;
    }

    @CommandLine.Option(names = {"--full-summary"}, description = {"Write project wide summary files with ALL Hits. ", "(Use with care! Might create large files and consume large amounts of memory for large projects.)"}, defaultValue = "false", negatable = true)
    protected boolean fullSummary;

    public boolean isFullSummary() {
        return fullSummary;
    }

    @CommandLine.Option(names = {"--output", "-o"}, description = "Specify location (outside the project) for writing summary files. Per default summaries are written to the project-space")
    Path location;

    @CommandLine.Option(names = {"--compress", "--zip", "-c"}, description = "Summaries will be written into a compressed zip archive. This parameter will be ignored if the summary is written into the project-space.")
    boolean compress;


    @CommandLine.ArgGroup(exclusive = false, heading = "Include Predictions Table")
    @Nullable
    protected PredictionsOptions predictionsOptions;

    public boolean isAnyPredictionOptionSet() {
        if (predictionsOptions == null)
            return false;
        return predictionsOptions.isAnyPredictionSet();
    }

    @Override
    public SummarySubToolJob makePostprocessingJob() {
        return new SummarySubToolJob(this);
    }

    @Override
    public SummarySubToolJob makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new SummarySubToolJob(this);
    }
}
