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

package org.conch.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtil {
    /**
     * Unzips a file, placing its contents in the given output location.
     *
     * @param zipFilePath
     *            input zip file
     * @param outputLocation
     *            zip file output folder
     * @throws IOException
     *             if there was an error reading the zip file or writing the unzipped data
     */
    public static void unzip(final String zipFilePath, final String outputLocation) throws IOException {
        // Open the zip file
        try (final ZipFile zipFile = new ZipFile(zipFilePath)) {
            final Enumeration<? extends ZipEntry> enu = zipFile.entries();
            while (enu.hasMoreElements()) {

                final ZipEntry zipEntry = enu.nextElement();
                final String name = zipEntry.getName();
                final File outputFile = new File(outputLocation + File.separator + name);

                if (name.endsWith("/")) {
                    outputFile.mkdirs();
                    continue;
                }

                final File parent = outputFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }

                // Extract the file
                try (final InputStream inputStream = zipFile.getInputStream(zipEntry);
                     final FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    /*
                     * The buffer is the max amount of bytes kept in RAM during any given time while
                     * unzipping. Since most windows disks are aligned to 4096 or 8192, we use a
                     * multiple of those values for best performance.
                     */
                    final byte[] bytes = new byte[8192];
                    while (inputStream.available() > 0) {
                        final int length = inputStream.read(bytes);
                        outputStream.write(bytes, 0, length);
                    }
                }
            }
        }
    }


    public static void unzipAndReplace(File archive, boolean deleteAfterDone) throws IOException {
        //Open the file
        ZipFile file = new ZipFile(archive);
        FileSystem fileSystem = FileSystems.getDefault();
        //Get file entries
        Enumeration<? extends ZipEntry> entries = file.entries();

        //unzip files in root folder
        String uncompressedDirectory = new File(".").getCanonicalPath() + File.separator;

        //Iterate over entries
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            final String name = zipEntry.getName();
            final File outputFile = new File(uncompressedDirectory + File.separator + name);

            if (name.endsWith("/")) {
                outputFile.mkdirs();
                continue;
            }

            final File parent = outputFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            InputStream is = file.getInputStream(zipEntry);
            Path uncompressedFilePath = fileSystem.getPath(uncompressedDirectory + File.separator + name);
            Files.copy(is, uncompressedFilePath, StandardCopyOption.REPLACE_EXISTING);
            Logger.logDebugMessage("[UPGRADE CLIENT] Create Or Replace :" + zipEntry.getName());
        }
        file.close();
        if (deleteAfterDone) {
            FileUtils.forceDelete(archive);
            Logger.logDebugMessage("[UPGRADE CLIENT] Delete temp upgrade archive file :" + archive.getName());
        }
    }
}
