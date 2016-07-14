/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

public class FingerIdData {

    protected ProbabilityFingerprint platts;
    protected Compound[] compounds;
    protected double[] scores;
    protected double confidence;
    public double topScore;

    public FingerIdData(Compound[] compounds, double[] scores, ProbabilityFingerprint platts) {
        this.compounds = compounds;
        this.scores = scores;
        this.platts = platts;
        this.topScore = scores.length == 0 ? Double.NEGATIVE_INFINITY : scores[0];
        this.confidence = Double.NaN;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
