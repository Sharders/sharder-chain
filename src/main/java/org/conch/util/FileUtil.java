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

    static void deleteDirectory(Path path){
        if(path == null) return;
        File delFile = new File(path.toString());
        deleteDirectory(delFile);
    }
    
    static void deleteDirectory(File delFile){
        try {
            if(delFile == null) return;
            
            if(!delFile.exists()) return;
            
            FileUtils.deleteDirectory(delFile);
        } catch (Exception e) {
            Logger.logErrorMessage(String.format("delete folder %s failed caused by %s", delFile.getPath(), e.getMessage()));
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
    public static synchronized void unzipAndReplace(File archive,String mode, boolean deleteSource) throws IOException {

        //unzip files into application catalog
//        String uncompressedDirectory = new File(".").getCanonicalPath() + File.separator;
        Path appRootPath = Paths.get(".");
        boolean isFullMode = ClientUpgradeTool.isFullUpgrade(mode);
        
        Map<String, String> libFileMap = Maps.newConcurrentMap();
        List<Path> removeOldLibFiles = Lists.newArrayList();
        if(isFullMode){
            deleteDirectory(appRootPath.resolve("html"));
            deleteDirectory(appRootPath.resolve("lib"));
        }else {
            // backup html folder and clear all files that under it
            String htmlFolder = Paths.get(".","html").toString();
            if(!deleteSource) {
                backupFolder(htmlFolder, false);
            }
            Logger.logDebugMessage("clear www folder");
            deleteDirectory(Paths.get(htmlFolder,"www"));
           
            // get file lists of current lib folder
            libFileMap = getLibFileMap();
        }

//        System.out.println("libFileMap => " + JSONObject.toJSON(libFileMap).toString());
  
        Logger.logInfoMessage("[UPGRADE CLIENT] start to upgrade...");
        String upgradeDetail = "UPGRADE CLIENT Detail \n\r";
        String failedDetail = "FAILED Detail \n\r";
        int count = 0;
        int failedCount = 0;
        long size = 0;
        boolean containDbFolder = false;

        //Iterate over entries
        String archiveRoot = "";
        ZipFile file = new ZipFile(archive);
        FileSystem fileSystem = FileSystems.getDefault();
        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            try {
                final String name = zipEntry.getName();
                final File outputFile = new File(appRootPath.resolve(name).toString());

                // get the root folder of unzip
                if (name.endsWith("/")) {
                    long fileSeparatorCount = name.chars().filter(c -> c == '/').count();
                    if (fileSeparatorCount == 1) {
                        archiveRoot = name;
                    }
                    outputFile.mkdirs();
                    continue;
                }

                final File parent = outputFile.getParentFile();
                if (parent != null) {
                    String folderName = parent.getName();
//                    System.out.println("found a parent folder -> " + folderName + ", path=" + parent.getPath());
                    //db folder
                    if(!containDbFolder) {
                        containDbFolder = StringUtils.isNotEmpty(folderName)
                                          && ("sharder_test_db".equals(folderName) || "sharder_db".equals(folderName));
                        // delete db folder only once
                        if(containDbFolder) {
                            File dbFile = new File(appRootPath.resolve("sharder_test_db").toString());
                            if(!dbFile.exists()) {
                                dbFile = new File(appRootPath.resolve("sharder_db").toString());
                            }
                            
                            if(deleteSource){
                                Logger.logDebugMessage("found db folder in the upgrade zip[%s], delete current db folder[%s] firstly", archive.getName(), dbFile.getName());
                                FileUtils.deleteDirectory(dbFile);
                                deleteDirectory(dbFile);
                            }else{
                                Logger.logDebugMessage("found db folder in the upgrade zip[%s], backup and delete current db folder[%s] firstly", archive.getName(), dbFile.getName());
                                backupFolder(dbFile.getPath(), true);
                            }  
                        }
                    }
                    
                    if(!parent.exists()){
                        System.out.println(folderName + " mkdirs");
                        parent.mkdirs();
                    }
                }

                // copy and replace the upgrade files
                InputStream is = file.getInputStream(zipEntry);
                String targetName = name;
//                if (targetRoot.length() > 1) {
//                    targetName = targetName.replace(targetRoot, "");
//                }

                Path targetPath = appRootPath.resolve(targetName);
//                Path targetPath = fileSystem.getPath(appRootFolder + File.separator + targetName);
                
                // lib folder 
                // check and add the old version lib file into remove list
                if(StringUtils.isNotEmpty(targetName) && targetName.contains("lib")) {
                    String targetLibFile = removeVersion((targetName));
                    Logger.logDebugMessage("found targetLibFile[full name=" + targetName + ", name=" + targetLibFile + "]");
                    if(libFileMap.containsKey(targetLibFile)
                    && !targetName.endsWith(libFileMap.get(targetLibFile))) {
                        removeOldLibFiles.add(appRootPath.resolve("lib").resolve(libFileMap.get(targetLibFile)));
                    }
                }
                
                size += Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                upgradeDetail += "[ OK ] Create or replace " + targetPath.toString() + " \n";
                count++;
            } catch (Exception e) {
                failedDetail += "[ ERROR ] Failed to upgrade " + zipEntry.getName() + " caused by [" + e.getMessage() + "] \n";
                failedCount++;
            }
        }
        file.close();
        
        File unzipFolder = new File(appRootPath.resolve(archiveRoot).toString());
        File appFolder = new File(appRootPath.toString());
        Logger.logDebugMessage("copy folder form %s to %s", unzipFolder.getPath(), appFolder.getPath());
        FileUtils.copyDirectory(unzipFolder,appFolder);
        
        
        String deletedOldLibFiles = "";
        // delete old lib files
        if(removeOldLibFiles.size() > 0) {
            for (Path libPath : removeOldLibFiles) {
                deletedOldLibFiles += "[ OK ] Deleted old lib file " + libPath.getFileName() + " on path " + libPath.toString() + " \n";
                Files.deleteIfExists(libPath);
            }
        }
        if(deletedOldLibFiles.length() > 1){
            upgradeDetail += "--- old lib file deletion ---\n" + deletedOldLibFiles;
        }

//      replaceConfFiles(uncompressedDirectory);
        Path tmpUpgradeFolder = appRootPath.resolve(archiveRoot);
        Logger.logDebugMessage("delete temp upgrade folder" + tmpUpgradeFolder.toString());
        FileUtils.deleteDirectory(new File(tmpUpgradeFolder.toString()));

        String upgradeSummary = "[UPGRADE CLIENT] Updated " + count + " files, Updated bytes " + size + ". Failed " + failedCount + " files\n\r";
        Logger.logInfoMessage(upgradeSummary);
        Logger.logDebugMessage(upgradeDetail + "\n\r" +  upgradeSummary);

        if (failedCount > 0) {
            Logger.logDebugMessage(failedDetail);
        }
        
        if (deleteSource) {
            FileUtils.forceDelete(archive);
            Logger.logDebugMessage("[ UPGRADE CLIENT ] delete temp upgrade archive file " + archive.getName());
        }
    }

    private static void replaceConfFiles(String uncompressedDirectory) {
        // TODO config files process
        // read properties from current config file and combine them with the properties of new config files
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
    

    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    static final String BAK_FOLDER = Paths.get(".","bak").toString();

    public static void backupFolder(String appRoot, boolean deleteSource){
        File root = new File(appRoot);
        if(!root.exists()) return;
        
        File[] appFiles = root.listFiles();
        if(appFiles.length <= 0 ) {
            return ;
        }
    
        String timeStr = dateFormat.format(System.currentTimeMillis());
        String bakFolder = Paths.get(BAK_FOLDER).resolve(appRoot + "_" + timeStr).toString();

        Logger.logDebugMessage("back up folder %s -> %s", appRoot, bakFolder);
        copyFolder(appRoot, bakFolder);
        
        if(deleteSource) {
            try {
                FileUtils.deleteDirectory(root);
            } catch (IOException e) {
                Logger.logErrorMessage(String.format("delete folder %s in #backupFolder failed caused by %s", appRoot, e.getMessage()));
            }
        }
    }
    
    private static final String VERSION_SPILLER = "-";

    static String removeVersion(String fullName){
        if(fullName.contains(VERSION_SPILLER)){ 
           int lastFileSepIndex = fullName.lastIndexOf(File.separator);
           int startIndex = (lastFileSepIndex == -1) ? 0 : lastFileSepIndex + 1;
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

}
