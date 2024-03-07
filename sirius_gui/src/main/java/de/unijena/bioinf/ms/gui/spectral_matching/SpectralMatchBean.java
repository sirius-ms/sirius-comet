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

package de.unijena.bioinf.ms.gui.spectral_matching;

import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchSubtoolJob;
import de.unijena.bioinf.ms.nightsky.sdk.model.BasicSpectrum;
import de.unijena.bioinf.ms.nightsky.sdk.model.DBLink;
import de.unijena.bioinf.ms.nightsky.sdk.model.SpectralLibraryMatch;
import de.unijena.bioinf.projectspace.InstanceBean;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Getter
class SpectralMatchBean implements SiriusPCS, Comparable<SpectralMatchBean> {

    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

    private final SpectralLibraryMatch match;

    private String queryName;

    private int rank;

    public SpectralMatchBean(SpectralLibraryMatch match, InstanceBean instance) {
        this.match = match;
        try {
            if (instance != null) {
                BasicSpectrum query = instance.getMsData().getMs2Spectra().get(match.getQuerySpectrumIndex());
                this.queryName = SpectraSearchSubtoolJob.getQueryName(
                        query.getMsLevel(),
                        query.getScanNumber(),
                        query.getCollisionEnergy(),
                        instance.getIonType().getIonization().toString(),
                        match.getQuerySpectrumIndex());
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error retrieving spectral matching data.", e);
        }
    }

    public Optional<BasicSpectrum> getReference() {
        return Optional.ofNullable(getMatch().getReferenceSpectrum());
    }

    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    @Override
    public int compareTo(@NotNull SpectralMatchBean o) {
        return Double.compare(o.getMatch().getSimilarity(), match.getSimilarity());
    }

    public DBLink getDBLink() {
        return new DBLink().name(getMatch().getDbName()).id(getMatch().getDbId());
    }
}
