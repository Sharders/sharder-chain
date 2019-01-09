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

package org.conch.env;

import java.io.File;
import java.net.URI;

public class SystemTrayDataProvider {

    private final String toolTip;
    private final URI wallet;
    private final File logFile;

    public SystemTrayDataProvider(String toolTip, URI wallet, File logFile) {
        this.toolTip = toolTip;
        this.wallet = wallet;
        this.logFile = logFile;
    }

    public String getToolTip() {
        return toolTip;
    }

    public URI getWallet() {
        return wallet;
    }

    public File getLogFile() {
        return logFile;
    }
}
