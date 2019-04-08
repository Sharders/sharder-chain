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
import org.conch.Conch;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtil {
    /**
     * Unzips a file, placing its contents in the given output location.
     *
     * @param zipFilePath    input zip file
     * @param outputLocation zip file output folder
     * @throws IOException if there was an error reading the zip file or writing the unzipped data
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

        //unzip files into application catalog
        String uncompressedDirectory = new File(".").getCanonicalPath() + File.separator;
        String uncompressedRoot = "";
        
        String upgradeDetail = "UPGRADE CLIENT Detail \n\r";
        int count = 0;
        long size = 0;
        //Iterate over entries
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            final String name = zipEntry.getName();
            final File outputFile = new File(uncompressedDirectory + File.separator + name);
            
            // get the root folder of unzip 
            if (name.endsWith(File.separator)) {
                long fileSeparatorCount = name.chars().filter(c -> c == File.separatorChar).count();
                if (fileSeparatorCount == 1) {
                    uncompressedRoot = name;
                }
                outputFile.mkdirs();
                continue;
            }
        
            final File parent = outputFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            
            // copy and replace the upgrade files
            InputStream is = file.getInputStream(zipEntry);
            String targetName = name;
            if(uncompressedRoot.length() > 1){
                targetName = targetName.replace(uncompressedRoot, "");
            }
            Path targetPath = fileSystem.getPath(uncompressedDirectory + File.separator + targetName);
            size += Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            upgradeDetail += "create or replace " + targetPath.toString() + " \n";
            count++;
        }
        String upgradeSummary = "UPDATE " + count + " files, " + size + " bytes UPGRADE \n\r";
        Logger.logInfoMessage(upgradeSummary);
        Logger.logDebugMessage(upgradeDetail + upgradeSummary);
        
        file.close();

        replaceConfFiles(uncompressedDirectory);

        if (deleteAfterDone) {
            FileUtils.forceDelete(archive);
            FileUtils.deleteDirectory(new File(uncompressedDirectory + uncompressedRoot));
            Logger.logDebugMessage("[UPGRADE CLIENT] delete temp upgrade archive file " + archive.getName());
        }
    }

    private static void replaceConfFiles(String uncompressedDirectory) {
        String configFolder = uncompressedDirectory + "conf";
        String targetFolder = Conch.getConfDir().getAbsolutePath();
        Logger.logDebugMessage("[UPGRADE CLIENT] copy and replace exist config files: %s -> %s", configFolder, targetFolder);
        copyFolder(configFolder, targetFolder);
    }

    public static void copyFolder(String oldPath, String newPath) {
        try {
            
            if (oldPath.equalsIgnoreCase(newPath)) {
                return;
            }
            File dist = new File(newPath);
            if (!dist.exists()) dist.mkdirs();

            String[] file = new File(oldPath).list();
            File temp = null;
            for (int i = 0; i < file.length; i++) {
                if (oldPath.endsWith(File.separator)) {
                    temp = new File(oldPath + file[i]);
                } else {
                    temp = new File(oldPath + File.separator + file[i]);
                }
                
                // don't replace the exist sharder.properties
                if (temp.isFile() && !"sharder.properties".equalsIgnoreCase(temp.getName())) {
                    try (
                            FileChannel readChannel = new RandomAccessFile(temp.getAbsolutePath(),"rw").getChannel();
                            FileChannel writeChannel = new RandomAccessFile(newPath + File.separator + temp.getName(),"rw").getChannel()
                    ) {
                        ByteBuffer buf = ByteBuffer.allocate(1024 * 5);
                        while (readChannel.read(buf) != -1) {
                            // limit=>position，position=>0
                            buf.flip();
                            writeChannel.write(buf);
                            // position=>0，limit=>capacity
                            buf.clear();
                        }
                    }
                }
                //sub folders
                if (temp.isDirectory()) {
                    copyFolder(oldPath + File.separator + file[i], newPath + File.separator + file[i]);
                }
            }
        } catch (Exception e) {
            Logger.logErrorMessage("copy and replace files error", e);
        }
    }
}
