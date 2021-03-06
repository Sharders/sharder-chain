/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.http.biz.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Block
 *
 * @author bubai
 * @date 2018/4/16
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Constants {
    private long epochBeginning;

    public long getEpochBeginning() {
        return epochBeginning;
    }

    public void setEpochBeginning(long epochBeginning) {
        this.epochBeginning = epochBeginning;
    }
}
