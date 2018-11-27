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
import java.nio.file.Paths;
import java.util.Properties;

public class DefaultDirProvider implements DirProvider {

    private static boolean islocalTest = false;

    {
        String env = System.getProperty("conch.env");
        if(env != null && "Local".equalsIgnoreCase(env)) islocalTest = true;
    }


    @Override
    public boolean isLoadPropertyFileFromUserDir() {
        return islocalTest;
    }

    @Override
    public void updateLogFileHandler(Properties loggingProperties) {}

    @Override
    public String getDbDir(String dbDir) {
        return dbDir;
    }

    @Override
    public File getLogFileDir() {
        return new File(getUserHomeDir(), "logs");
    }

    @Override
    public File getConfDir() {
        return new File(getUserHomeDir(), "conf");
    }

    @Override
    public String getUserHomeDir() {
        return islocalTest ? Paths.get(".").toAbsolutePath().getParent().getParent().toString() : Paths.get(".").toAbsolutePath().getParent().toString();
    }

}
