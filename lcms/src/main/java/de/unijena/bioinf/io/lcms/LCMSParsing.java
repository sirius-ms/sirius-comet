/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.model.lcms.LCMSRun;

import java.io.File;
import java.io.IOException;

public class LCMSParsing {

    public static LCMSRun parseRun(File source, SpectrumStorage storage) throws IOException {
        if (source.getName().toLowerCase().endsWith(".mzml")) {
            return parseRunFromMzMl(source, storage);
        } else if (source.getName().toLowerCase().endsWith(".mzxml")) {
            return parseRunFromMzXml(source, storage);
        }
        throw new IOException("Illegal file extension. Only .mzml and .mzxml are supported");
    }

    public static LCMSRun parseRunFromMzXml(File source, SpectrumStorage storage) throws IOException {
        return new MzXMLParser().parse(source, storage);
    }

    public static LCMSRun parseRunFromMzMl(File source, SpectrumStorage storage) throws IOException {
        return new MzMLParser().parse(source, storage);
    }
}
