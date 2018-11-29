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
package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.ms.properties.PropertyManager;

@DefaultProperty(propertyParent = "IsotopeHandling")
public enum IsotopePatternHandling {

    omit, filter, score, both;

    public static final IsotopePatternHandling DEFAULT() {
        return PropertyManager.DEFAULTS.createInstanceWithDefaults(IsotopePatternHandling.class);
    }

    public boolean isFiltering() {
        return this==filter || this == both;
    }

    public boolean isScoring() {
        return this == score || this == both;
    }

}
