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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

abstract class DesktopUserDirProvider implements DirProvider {

    public static final String LOG_FILE_PATTERN = "java.util.logging.FileHandler.pattern";

    private File logFileDir;

    @Override
    public boolean isLoadPropertyFileFromUserDir() {
        return true;
    }

    @Override
    public void updateLogFileHandler(Properties loggingProperties) {
        if (loggingProperties.getProperty(LOG_FILE_PATTERN) == null) {
            logFileDir = new File(getUserHomeDir(), "logs");
            return;
        }
        Path logFilePattern = Paths.get(getUserHomeDir()).resolve(Paths.get(loggingProperties.getProperty(LOG_FILE_PATTERN)));
        loggingProperties.setProperty(LOG_FILE_PATTERN, logFilePattern.toString());

        Path logDirPath = logFilePattern.getParent();
        System.out.printf("Logs dir %s\n", logDirPath.toString());
        this.logFileDir = new File(logDirPath.toString());
        if (!Files.isReadable(logDirPath)) {
            System.out.printf("Creating dir %s\n", logDirPath);
            try {
                Files.createDirectory(logDirPath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot create " + logDirPath, e);
            }
        }
    }

    @Override
    public File getLogFileDir() {
        return logFileDir;
    }

    @Override
    public String getDbDir(String dbDir) {
        return Paths.get(getUserHomeDir()).resolve(Paths.get(dbDir)).toString();
    }

    @Override
    public File getConfDir() {
        return new File(getUserHomeDir(), "conf");
    }

    @Override
    public abstract String getUserHomeDir();

}
