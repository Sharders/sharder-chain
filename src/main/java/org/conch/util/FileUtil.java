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

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.tools.ClientUpgradeTool;

public class FileUtil {

    /**
     * zip the file 文件压缩成zip文件
     * @param filepath source file path
     * @param zipOutPath zip output path
     */
    public static void ZipFile(String filepath ,String zipOutPath) {
        try {
            File file = new File(filepath);
            File zipFile = new File(zipOutPath);
            InputStream input = new FileInputStream(file);
            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
            zipOut.putNextEntry(new ZipEntry(file.getName()));
            int temp = 0;
            while((temp = input.read()) != -1){
                zipOut.write(temp);
            }
            input.close();
            zipOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Unzips a file, placing its contents in the given output location.
     *
     * @param zipFilePath    input zip file
     * @param outputLocation zip file output folder
     * @param deleteSource   true or false - delete source file
     * @throws IOException if there was an error reading the zip file or writing the unzipped data
     */
    public static void unzip(final String zipFilePath, final String outputLocation, boolean deleteSource) throws IOException {

        ZipFile zipFile = null;
        SevenZFile zIn = null;
        Path appRootPath = Paths.get(".");
        try{
            if(zipFilePath.endsWith(".zip")){
                zipFile = new ZipFile(zipFilePath);
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

                    // Extract the file 提取文件
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
            }else if(zipFilePath.endsWith(".7z")){
                zIn = new SevenZFile(new File(zipFilePath));

                SevenZArchiveEntry entry = null;
                while ((entry = zIn.getNextEntry()) != null) {
                    final String name = entry.getName();
                    final File outputFile = new File(outputLocation + File.separator + name);
                    if(entry.isDirectory()){
                        outputFile.mkdirs();

                        final File parent = outputFile.getParentFile();
                        if (parent != null) {
                            parent.mkdirs();
                        }
                    }else if(!entry.isDirectory()){

                        // Extract the file 提取文件
                        OutputStream out = null;
                        BufferedOutputStream bos = null;
                        try {
                            String targetName = name;
                            Path targetPath = appRootPath.resolve(targetName);
                            out = new FileOutputStream(targetPath.toFile());
                            bos = new BufferedOutputStream(out);
                            int len = -1;
                            byte[] buf = new byte[1024];
                            while ((len = zIn.read(buf)) != -1) {
                                bos.write(buf, 0, len);
                            }
                        } catch (IOException e) {
                            Logger.logErrorMessage(String.format("[ ERROR ] copy and replae the upgrade files %s", name, e.getMessage()));
                        } finally {
                            if (bos != null) {
                                bos.close();
                            }
                            if (out != null) {
                                out.close();
                            }
                        }
                    }
                }
            }
        }catch (Exception e) {
            Logger.logErrorMessage(String.format("unzip the file %s -> %s failed caused by %s", zipFilePath, outputLocation, e.getMessage()));
            throw e;
        }finally {
            if(zipFile != null){
                zipFile.close();
            }else if(zIn != null){
                zIn.close();
            }
        }
    }

    /**
     * @param path
     */
    public static void deleteDirectory(Path path){
        if(path == null) return;
        File delFile = new File(path.toString());
        deleteDirectory(delFile);
    }

    /**
     *
     * @param delFile
     */
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
     *
     *
     *
     */
    // lib 包
    private static Map<String, String> libFileMap = Maps.newConcurrentMap();
    //旧版本 lib
    private static List<String> oldLibFiles = Lists.newArrayList();
    public static synchronized void unzipAndReplace(final File archive,String mode, boolean deleteSource) throws IOException {

        //unzip files into application catalog 将文件解压到应用程序目录中
        //String uncompressedDirectory = new File(".").getCanonicalPath() + File.separator;
        Path appRootPath = Paths.get(".");
        boolean isFullMode = ClientUpgradeTool.isFullUpgrade(mode); //  FULL || incremental

        //判断是全量还是增量更新
        //全量：将html、lib中所有文件删除
        //增量：deleteSource参数判断
        //	true删除html false备份html之后再删除
        //	得到所有lib下的jar 包 保存于libFileMap(key=commons-compress:value=./lib/commons-compress-1.9.jar)
        if(isFullMode){
            deleteDirectory(appRootPath.resolve("html"));
            deleteDirectory(appRootPath.resolve("lib"));
        }else {
            // backup html folder and clear all files that under it
            //备份html文件夹并清除其下的所有文件
            String htmlFolder = Paths.get(".","html").toString();
            if(!deleteSource) {
                backupFolder(htmlFolder, false);
            }
            Logger.logDebugMessage("clear www folder");
            deleteDirectory(Paths.get(htmlFolder,"www"));
           
            // get file lists of current lib folder
            libFileMap.clear();
            libFileMap = getLibFileMap();
        }

        Logger.logInfoMessage("[UPGRADE CLIENT] start to upgrade...");
        String upgradeDetail = "UPGRADE CLIENT Detail \n\r";
        String failedDetail = "FAILED Detail \n\r";
        int count = 0; //更新文件个数
        int failedCount = 0; //更新失败个数
        long size = 0; //更新大小

        //Iterate over entries 迭代所有的entries
        String archiveRoot = ""; //文件目录名称

        //根据后缀判断是.zip还是.7z
        ZipFile zfile = null;
        SevenZFile zIn = null;
        try {
            oldLibFiles.clear();
            if(archive.getName().endsWith(".zip")){
                zfile = new ZipFile(archive);
                Enumeration<? extends ZipEntry> entries = zfile.entries();
                ZipEntry zipEntry = null;
                while (entries.hasMoreElements()) {
                    try {
                        zipEntry = entries.nextElement();
                        String name = zipEntry.getName();

                        File outputFile = new File(appRootPath.resolve(name).toString());

                        // get the root folder of unzip
                        //得到待解压文件目录
                        if (name.endsWith("/")) {
                            long fileSeparatorCount = name.chars().filter(c -> c == '/').count();
                            if (fileSeparatorCount == 1) {
                                archiveRoot = name;
                            }
                            outputFile.mkdirs();
                            continue;
                        }

                        //此文件的上级目录
                        final File parent = outputFile.getParentFile();

                        if (parent != null) {
                            //目录名称
                            String folderName = parent.getName();

                            dealDbFile(folderName,appRootPath,deleteSource,archive);

                            if(!parent.exists()){
                                System.out.println(folderName + " mkdirs");
                                parent.mkdirs();
                            }
                        }


                        // copy and replace the upgrade files
                        try(InputStream is = zfile.getInputStream(zipEntry)){
                            String targetName = name;
                            Path targetPath = appRootPath.resolve(targetName);

                            // lib folder
                            // Constants.GENERATE_EXPIRED_FILE_BUTTON 是否生成过期文件，默认 false
                            generteOldLibFileList(name);

                            //累加升级文件size，文件数count
                            size += Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            upgradeDetail += "[ OK ] Create or replace " + targetPath.toString() + " \n";
                            count++;
                        }
                    } catch (Exception e) {
                        failedDetail += "[ ERROR ] Failed to upgrade " + zipEntry.getName() + " caused by [" + e.getMessage() + "] \n";
                        failedCount++;
                    }
                }
            }else if(archive.getName().endsWith(".7z")){
                zIn = new SevenZFile(archive);
                SevenZArchiveEntry entry = null;
                while ((entry = zIn.getNextEntry()) != null) {
                    final String name = entry.getName();
                    if(entry.isDirectory()){
                        File outputFile = new File(appRootPath.resolve(name).toString());
                        // get the root folder of unzip
                        //得到待解压文件目录
                        System.out.println(name + " mkdirs");
                        outputFile.mkdirs();
                        if (name.chars().filter(c -> c == '/').count() == 0 ) {
                            archiveRoot = name;
                            continue;
                        }

                        //此文件的上级目录
                        final File parent = outputFile.getParentFile();

                        if (parent != null) {
                            //目录名称
                            String folderName = parent.getName();
                            dealDbFile(folderName,appRootPath,deleteSource,archive);
                        }
                    }else if (!entry.isDirectory()) {
                        try {
                            // copy and replae the upgrade files
                            OutputStream out = null;
                            BufferedOutputStream bos = null;
                            try {
                                // lib folder
                                // Constants.GENERATE_EXPIRED_FILE_BUTTON 是否生成过期文件，默认 false
                                generteOldLibFileList(name);

                                String targetName = name;
                                Path targetPath = appRootPath.resolve(targetName);
                                out = new FileOutputStream(targetPath.toFile());
                                bos = new BufferedOutputStream(out);
                                int len = -1;
                                byte[] buf = new byte[1024];
                                while ((len = zIn.read(buf)) != -1) {
                                    bos.write(buf, 0, len);
                                }

                                upgradeDetail += "[ OK ] Create or replace " + targetPath.toString() + " \n";
                                count++;
                            } catch (IOException e) {
                                Logger.logErrorMessage(String.format("[ ERROR ] copy and replae the upgrade files %s", name, e.getMessage()));
                            } finally {
                                if (bos != null) {
                                    bos.close();
                                }
                                if (out != null) {
                                    out.close();
                                }
                            }
                        } catch (Exception e) {
                            failedDetail += "[ ERROR ] Failed to upgrade " + entry.getName() + " caused by [" + e.getMessage() + "] \n";
                            failedCount++;
                        }
                    }
                }
            }else{
                return;
            }
        }catch (Exception e){
            Logger.logErrorMessage(String.format("unzipAndReplace the file %s  failed caused by %s", archive.getName(), e.getMessage()));
            throw e;
        }finally {
            if(zfile != null){
                zfile.close();
            }else if(zIn != null){
                zIn.close();
            }
        }

        //copy folder form unzipFolder to appFolder
        File unzipFolder = new File(appRootPath.resolve(archiveRoot).toString());
        File appFolder = new File(appRootPath.toString());
        Logger.logInfoMessage("copy folder form %s to %s", unzipFolder.getPath(), appFolder.getPath());
        FileUtils.copyDirectory(unzipFolder,appFolder);

        //删除旧lib包
        if (Constants.GENERATE_EXPIRED_FILE_BUTTON) {
            generateDeleteListFile(appRootPath,oldLibFiles);
        }
        //String deletedOldLibFiles = "";
        // delete old lib files
        //for (String libName : removeOldLibFiles) {
        //    deletedOldLibFiles += "[ OK ] Deleted old lib file " + libPath.getFileName() + " on path " + libPath.toString() + " \n";
        //    Files.deleteIfExists(libPath);
        //}
        //if(deletedOldLibFiles.length() > 1){
        //    upgradeDetail += "--- old lib file deletion ---\n" + deletedOldLibFiles;
        //}

        //替换配置文件
        //replaceConfFiles(uncompressedDirectory);

        //删除压出来的的文件夹
        Path tmpUpgradeFolder = appRootPath.resolve(archiveRoot);
        Logger.logInfoMessage("delete temp upgrade folder" + tmpUpgradeFolder.toString());
        FileUtils.deleteDirectory(new File(tmpUpgradeFolder.toString()));

        //版本升级log反馈
        String upgradeSummary = "[UPGRADE CLIENT] Updated " + count + " files, Updated bytes " + size + ". Failed " + failedCount + " files\n\r";
        Logger.logInfoMessage(upgradeSummary);
        Logger.logDebugMessage(upgradeDetail + "\n\r" +  upgradeSummary);
        if (failedCount > 0) {
            Logger.logDebugMessage(failedDetail);
        }

        //删除升级包
        if (deleteSource) {
            FileUtils.forceDelete(archive);
            Logger.logDebugMessage("[ UPGRADE CLIENT ] delete temp upgrade archive file " + archive.getName());
        }
    }

    private static void generteOldLibFileList(String targetName)throws IOException{
        // lib folder
        // Constants.GENERATE_EXPIRED_FILE_BUTTON 是否生成过期文件，默认 false
        if (Constants.GENERATE_EXPIRED_FILE_BUTTON) {
            try {
                // check and add the old version lib file into remove list  旧版本添加到移除列表中
                if (StringUtils.isNotEmpty(targetName) && targetName.contains("lib")) {
                    //先去掉上级目录，再去版本号 conch/lib/commons-compress-0.4.1.jar ==> commons-compress-0.4.1.jar
                    String targetLibFile = removePath(targetName);
                    //commons-compress-0.4.1.jar ==>commons-compress
                    String targetFile = removeVersion(targetLibFile);
                    Logger.logDebugMessage("found targetLibFile[full name=" + targetName + ", name=" + targetFile + "]");
                    if (libFileMap.containsKey(targetFile) && !targetName.endsWith(libFileMap.get(targetFile))) {
                        //版本号不一致，旧版本移除
                        oldLibFiles.add(libFileMap.get(targetFile));
                    }
                }
            } catch (Exception e) {
                Logger.logWarningMessage("[Ignore] Compare and generation expired file list failed, caused by" + " to %s", e.getMessage());
            }
        }
    }

    private static boolean containDbFolder = false;
    private static String DB_NAME_TEST = "ss_test_db";
    private static String DB_NAME = "ss_db";
    private static void dealDbFile(String folderName,Path appRootPath, Boolean deleteSource, File archive) throws IOException {
        //db folder 数据库目录
        //如果containDbFolder为false(初始值为false)，
        //判断文件夹的名字是否为COS的数据库名
        //是则根据deleteSource决定是删除还是备份
        if(!containDbFolder) {
            containDbFolder = StringUtils.isNotEmpty(folderName) && (DB_NAME_TEST.equals(folderName) || DB_NAME.equals(folderName));
            // delete db folder only once 只删除一次数据库文件夹
            if(containDbFolder) {
                File dbFile = new File(appRootPath.resolve(DB_NAME_TEST).toString());
                if(!dbFile.exists()) {
                    dbFile = new File(appRootPath.resolve(DB_NAME).toString());
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
    }

    /**
     * Generate a expired file list that start.sh will delete these files
     * 生成过期文件列表  start.sh将删除这些文件
     *
     * @param appRootPath
     * @param oldLibFiles
     */
    private static void generateDeleteListFile(Path appRootPath, List<String> oldLibFiles) {
        if (oldLibFiles.size() > 0) {
            File deleteList = appRootPath.resolve("lib").resolve("ExpiredFiles.data").toFile();
            // read the content of exist file of deleteList.json
            if (deleteList.exists()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(deleteList));
                    String read = "";
                    String content = "";
                    while ((read = reader.readLine()) != null) {
                        content += read;
                    }
                    reader.close();
                    // add the list of exist filename to the added lib filename list
                    if (StringUtils.isNotEmpty(content)) {
                        List<String> oldFileList = JSONObject.parseObject(content, List.class);
                        if (oldFileList != null && oldFileList.size() > 0) {
                            for (String fileName : oldFileList) {
                                if (!oldLibFiles.contains(fileName)) {
                                    oldLibFiles.add(fileName);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    Logger.logDebugMessage("read old file fail:" + e);
                }finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            Logger.logDebugMessage("close source error:" + e);
                        }
                    }
                }
            }
            String fileString = JSONObject.toJSONString(oldLibFiles);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(deleteList);
                fos.write(fileString.getBytes("UTF-8"));
                fos.close();
            } catch (IOException e) {
                Logger.logDebugMessage("write out file fail:" + e);
            }finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        Logger.logDebugMessage("close source error:" + e);
                    }
                }
            }
            oldLibFiles.clear();
        }
    }

    /**
     * 替换配文件
     * @param uncompressedDirectory 未压缩目录
     *
     */
    private static void replaceConfFiles(String uncompressedDirectory) {
        // TODO config files process
        // read properties from current config file and combine them with the properties of new config files
        String configFolder = uncompressedDirectory + "conf";
        String targetFolder = Conch.getConfDir().getAbsolutePath();
        Logger.logDebugMessage("[ UPGRADE CLIENT ] copy and replace exist config files: %s -> %s", configFolder, targetFolder);
        copyFolder(configFolder, targetFolder);
    }

    /**
     * 复制文件夹及其目录下所有文件，递归
     * @param oldPath
     * @param newPath
     */
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

    /**
     * 备份文件夹
     * @param appRoot 目标文件夹名称
     * @param deleteSource 是否删除原文件
     * 传入待备份目标文件夹名称（appoot），备份成到bak/原名称_时间
     * test ====> ./bak/test_20201217110928
     *
     */
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
    private static final String PATH_SPILLER = "/";

    /**
     *
     * @param fullName
     * @return
     *
     * 移除字符串中版本信息
     * lib/commons-compress-0.4.1.jar ===> commons-compress
     */
    static String removeVersion(String fullName){
        if(fullName.contains(VERSION_SPILLER)){ 
           int lastFileSepIndex = fullName.lastIndexOf(File.separator);
           int startIndex = (lastFileSepIndex == -1) ? 0 : lastFileSepIndex + 1;
           return  fullName.substring(startIndex,fullName.lastIndexOf(VERSION_SPILLER));
        }
        return fullName;
    }

    /**
     *
     * @param fullName
     * @return
     * 去掉上级文件夹，只要当前文件名
     * lib/commons-compress-0.4.1.jar ===>  commons-compress-0.4.1.jar
     */
    private static String removePath(String fullName) {
        if(fullName.contains(PATH_SPILLER)){
            return fullName.substring(fullName.lastIndexOf(PATH_SPILLER) + 1, fullName.length());
        }
        return fullName;
    }

    /**
     * 取lib包下所有文件
     * @return
     */
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

    /**
     * 清空log文件
     * @throws FileNotFoundException
     */
    public static void clearAllLogs() throws FileNotFoundException {
        String logPath = Conch.getUserHomeDir() + File.separator + "logs";
        clearOrDelFiles(logPath, true);
    }

    /**
     * 
     * @param folderPath folder path
     * @param justClear true: clear the content of the file, false: delete the file
     * @throws FileNotFoundException
     */
    public static void clearOrDelFiles(String folderPath, boolean justClear) throws FileNotFoundException {
        File folder = new File(folderPath);
        if(folder == null) return;
        
        File[] files = folder.listFiles();
        if (files != null && files.length > 0) {
            String mode = justClear ? "clear" : "delete";
            for (File file : files) {
                Logger.logInfoMessage("%s file %s", mode, file.getAbsolutePath());
                if(justClear){
                    PrintWriter writer = new PrintWriter(file);
                    writer.print("");
                    writer.close();  
                }else{
                    file.delete();
                }
            }
        }
    }

    /**
     * 删除数据库文件
     */
    public static void deleteDbFolder() {
        String dbPath = Paths.get(Conch.getUserHomeDir(), Db.getDir()).getParent().toString();
        File dbFolder = new File(dbPath);
        Logger.logInfoMessage("Deleting db folder: " + dbFolder.getAbsolutePath());
        deleteDirectory(dbFolder);
    }

}
