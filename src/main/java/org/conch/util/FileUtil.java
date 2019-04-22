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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.tools.ClientUpgradeTool;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
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

    /**
     * full mode:
     * delete html, lib folder firstly
     * 
     * VER_MODE_INCREMENTAL
     * incremental mode:
     * back up html folder, delete old version lib file
     *
     * replace the same file in two mode, like: cos.jar.
     */
    public static synchronized void unzipAndReplace(File archive,String mode, boolean deleteAfterDone) throws IOException {

        //unzip files into application catalog
//        String uncompressedDirectory = new File(".").getCanonicalPath() + File.separator;
        String uncompressedDirectory = Paths.get(".").toString();
        boolean isFullMode = ClientUpgradeTool.isFullUpgrade(mode);
        
        Map<String, String> libFileMap = Maps.newConcurrentMap();
        List<Path> removeOldLibFiles = Lists.newArrayList();
        if(isFullMode){
            Files.deleteIfExists(Paths.get(".","html"));
            Files.deleteIfExists(Paths.get(".","lib"));
        }else {
            // backup html folder and clear all files that under it
            String htmlFolder = Paths.get(".","html").toString();
            backupFolder(htmlFolder, true);
            
            // get file lists of current lib folder
            libFileMap = getLibFileMap();
        }

        // TODO config files process
        // read properties from current config file and combine them with the properties of new config files
  
        Logger.logInfoMessage("[UPGRADE CLIENT] start to upgrade...");
        String upgradeDetail = "UPGRADE CLIENT Detail \n\r";
        String failedDetail = "FAILED Detail \n\r";
        int count = 0;
        int failedCount = 0;
        long size = 0;
        boolean containDbFolder = false;

        //Iterate over entries
        String targetRoot = "";
        ZipFile file = new ZipFile(archive);
        FileSystem fileSystem = FileSystems.getDefault();
        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            try {
                final String name = zipEntry.getName();
                final File outputFile = new File(uncompressedDirectory + File.separator + name);

                // get the root folder of unzip
                if (name.endsWith("/")) {
                    long fileSeparatorCount = name.chars().filter(c -> c == '/').count();
                    if (fileSeparatorCount == 1) {
                        targetRoot = name;
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
                if (targetRoot.length() > 1) {
                    targetName = targetName.replace(targetRoot, "");
                }
                
                // lib folder 
                // check and add the old version lib file into remove list
                if(StringUtils.isNotEmpty(targetName) && targetName.startsWith("lib")) {
                    String targetLibFile = removeVersion((targetName));
                    if(libFileMap.containsKey(targetLibFile)) {
                        removeOldLibFiles.add(Paths.get(".",libFileMap.get(targetLibFile)));
                    }
                }
                
                //db folder
                if(!containDbFolder) {
                    containDbFolder = StringUtils.isNotEmpty(targetName)
                            && (targetName.equals("sharder_test_db") || targetName.equals("sharder_db"));
                    Logger.logDebugMessage("found db folder in the upgrade zip[%s], delete current db folder firstly.");
                    // delete db folder only once
                    if(containDbFolder) {
                        Files.deleteIfExists(Paths.get(uncompressedDirectory, "sharder_test_db"));
                        Files.deleteIfExists(Paths.get(uncompressedDirectory, "sharder_db"));
                    } 
                }
                
                Path targetPath = fileSystem.getPath(uncompressedDirectory + File.separator + targetName);
                size += Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                upgradeDetail += "[ OK ] Create or replace " + targetPath.toString() + " \n";
                count++;
            } catch (Exception e) {
                failedDetail += "[ ERROR ] Failed to upgrade " + zipEntry.getName() + " caused by [" + e.getMessage() + "] \n";
                failedCount++;
            }
        }
        file.close();
        
        String upgradeSummary = "[ OK ] Updated " + count + " files, Total bytes: " + size + "\n\r";
        Logger.logInfoMessage(upgradeSummary);
        Logger.logDebugMessage(upgradeDetail + upgradeSummary);
        if (failedCount > 0) {
            Logger.logDebugMessage(failedDetail);
        }
        
        String deletedOldLibFiles = "";
        // delete old lib files
        if(removeOldLibFiles.size() > 0) {
            for (Path libPath : removeOldLibFiles) {
                deletedOldLibFiles += "[ OK ] Deleted old lib file " + libPath.getFileName() + " \n";
                Files.deleteIfExists(libPath);
            }
        }
        if(deletedOldLibFiles.length() > 1){
            Logger.logDebugMessage(deletedOldLibFiles);
        }
        
        replaceConfFiles(uncompressedDirectory);

        if (deleteAfterDone) {
            FileUtils.forceDelete(archive);
            FileUtils.deleteDirectory(new File(uncompressedDirectory + targetRoot));
            Logger.logDebugMessage("[ UPGRADE CLIENT ] delete temp upgrade archive file " + archive.getName());
        }
    }

    private static void replaceConfFiles(String uncompressedDirectory) {
        String configFolder = uncompressedDirectory + "conf";
        String targetFolder = Conch.getConfDir().getAbsolutePath();
        Logger.logDebugMessage("[ UPGRADE CLIENT ] copy and replace exist config files: %s -> %s", configFolder, targetFolder);
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
                            FileChannel readChannel = new RandomAccessFile(temp.getAbsolutePath(), "rw").getChannel();
                            FileChannel writeChannel = new RandomAccessFile(newPath + File.separator + temp.getName(), "rw").getChannel()
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
    

    
    static Map<String, String> libFileMap = Maps.newConcurrentMap();
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    static protected void backupFolder(String appRoot, boolean deleteAfterSuccess){
        File root = new File(appRoot);
        File[] appFiles = root.listFiles();
        if(appFiles.length <= 0 ) {
            return ;
        }

        String timeStr = dateFormat.format(System.currentTimeMillis());
        String bakFolder = Paths.get(appRoot, timeStr).toString();

        copyFolder(appRoot, bakFolder);
        
        if(deleteAfterSuccess) {
            root.deleteOnExit();;
        }
    }
    
    private static final String VERSION_SPILLER = "-";

    static String removeVersion(String fullName){
        if(fullName.contains(VERSION_SPILLER)){ 
           int lastFileSepIndex = fullName.lastIndexOf(File.separator);
           int startIndex = (lastFileSepIndex == -1) ? 0 : lastFileSepIndex;
           return  fullName.substring(startIndex,fullName.lastIndexOf(VERSION_SPILLER));
        }
        return fullName;
    }
    
    protected static Map<String, String> getLibFileMap(){
        Map<String, String> libFileMap = Maps.newConcurrentMap();
        File targetPathFile = new File(Paths.get(".","lib").toString());
        
        if(!targetPathFile.exists()) return libFileMap;
        
        File[] libFiles = targetPathFile.listFiles();
        if(libFiles.length <= 0 ) return libFileMap;

        for(int j=0; j < libFiles.length; j++){
            String libName = libFiles[j].getName();
            libFileMap.put(removeVersion(libName), libName);
        }
        return libFileMap;
    }

    public static void main(String[] args) {
        
    }
}
