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

package org.conch.tools;

import com.google.common.collect.Lists;
import org.conch.Conch;
import org.conch.chain.Block;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.mint.Generator;
import org.conch.util.FileUtil;
import org.conch.util.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ArchiveDbTool {
    private static final String OSS_DB_ARCHIVE_PATH = "cos/client/release/";
    private static final String OSS_DB_ARCHIVE_MEMO_FILE_NAME = "cos-db-archive";
    private static final String OSS_DB_ARCHIVE_MEMO_PATH = "cos/client/release/" + OSS_DB_ARCHIVE_MEMO_FILE_NAME;
    private static final int ARCHIVE_INTERVAL_DAYS = Conch.getIntProperty("sharder.oss.archive.interval", 15);
    private static final Boolean AUTO_ARCHIVE_OPEN = Conch.getBooleanProperty("sharder.oss.archive.open");

    /**
     * auto archive switch
     * @return
     */
    public static boolean openAutoArchive() {
        if(!AUTO_ARCHIVE_OPEN) return false;

        return AliyunOssUtil.openAutoBackDB();
    }

    private static long archiveIntervalHeight = calArchiveIntervalHeight();
    private static long lastArchiveHeight = -1L;

    private static long calArchiveIntervalHeight(){
        // default is 15days to archive once
        return 60L*24L*60L*ARCHIVE_INTERVAL_DAYS / (long)Constants.getBlockGapSeconds();
    }
    /**
     * current height whether match the backup condition
     * @return true / false
     */
    private static boolean bakNow(){
        if(lastArchiveHeight == -1L){
            lastArchiveHeight = Conch.getHeight();
            return false;
        }

        if(Conch.getHeight() - lastArchiveHeight < 0) {
            return false;
        }

        if(Conch.getHeight() - lastArchiveHeight < archiveIntervalHeight){
            return false;
        }

        return true;
    }


    /**
     * zip and upload the db archive to OSS
     */
    public static void checkAndArchiveDB(Block block) {
        if(!bakNow()){
            long archiveHeight = lastArchiveHeight + archiveIntervalHeight;
            Logger.logInfoMessage("Not reach the archive height[" + archiveHeight + "], exit the archive operation.");
            return;
        }

        Logger.logInfoMessage("New a thread to archive and upload the archive to OSS...");
        // set the archive height before processing to avoid the duplicate archiving
        lastArchiveHeight = Conch.getHeight();
        Thread dbArchiveThread = new Thread(() -> {
            String[] dbArchiveArray = archiveDb(null);
            if(dbArchiveArray == null || dbArchiveArray.length == 0) {
                Logger.logInfoMessage("zip db archive and memo file failed. EXIT the db archive operation.");
            }
            AliyunOssUtil.uploadFile(OSS_DB_ARCHIVE_PATH, dbArchiveArray[0], true);
            AliyunOssUtil.uploadFile(OSS_DB_ARCHIVE_MEMO_PATH, dbArchiveArray[1], true);
        });
        dbArchiveThread.start();
    }

    /**
     * archive the database
     * @param path
     * @return String[2]: String[0]- db archive file path; String[1]- db archive memo file path
     */
    private static String[] archiveDb(String path) {
            String[] archiveArray = new String[2];
            String pathStr;
            String fileNameStr;
            try {
                Conch.getBlockchain().updateLock();
                Path appRootPath = Paths.get(".");

                pathStr = (path == null) ? appRootPath.resolve("ARCHIVE/").toString() : path;
                if(!pathStr.endsWith(File.separator)) {
                    pathStr += File.separator;
                }

                //db archive file
                fileNameStr = Db.getName() + "_" + Conch.getHeight() +".zip";

                File bakFolder = new File(pathStr);
                if(!bakFolder.exists()) {
                    bakFolder.mkdirs();
                }

                // generate db archive
                String dbArchivePath = pathStr + fileNameStr;
                FileUtil.ZipFile(pathStr + Db.getName(), pathStr + fileNameStr);

                // return values
                archiveArray[0] = dbArchivePath;
                archiveArray[1] = generateArchiveMemoFile(pathStr);

                // upload to OSS
                AliyunOssUtil.delFile(Lists.newArrayList(OSS_DB_ARCHIVE_MEMO_PATH));
                AliyunOssUtil.uploadFile(archiveArray[0], dbArchivePath, true);
                AliyunOssUtil.uploadFile(archiveArray[1], dbArchivePath, true);

                return archiveArray;
            } finally {
                Conch.getBlockchain().updateUnlock();
            }

        }

    private static String generateArchiveMemoFile(String path){
        FileWriter writer = null;
        String memoFilePath = null;
        try {
            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }
            memoFilePath = path + OSS_DB_ARCHIVE_MEMO_FILE_NAME;
            String dbArchiveName = Db.getName() + "_" + Conch.getHeight();
            String content = "testLastArchive=" + dbArchiveName
                    + "\ntestKnownArchive=" + Conch.getHeight();

            File file = new File(memoFilePath);
            if (!file.exists()) {
                file.createNewFile();
            }

            // rewrite the file content
            writer = new FileWriter(file.getName());
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if(writer != null){
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return memoFilePath;
    }

    private static boolean deleteOldBackupFiles(String path, List<String> ignoreList){
        File file = new File(path);

        if(!file.isDirectory()) {
            return false;
        }

        File[] oldFiles = file.listFiles();
        for(int i = 0 ; i < oldFiles.length; i++){
            if(ignoreList.contains(oldFiles[i].getName())) {
                continue;
            }
            oldFiles[i].delete();
        }

        return true;
    }

    public static void main(String[] args) {

    }
}
