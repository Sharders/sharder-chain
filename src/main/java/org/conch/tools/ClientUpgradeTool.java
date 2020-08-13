package org.conch.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.db.Db;
import org.conch.util.FileUtil;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipException;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-29
 */
public class ClientUpgradeTool {
    
    public static final String VER_MODE_FULL = "FULL";
    public static final String VER_MODE_INCREMENTAL = "INCREMENTAL";
    
    public static final String BAK_MODE_DELETE = "delete";
    public static final String BAK_MODE_BACKUP = "backup";

    public static final String DB_ARCHIVE_DEFAULT = "default";
    
    public static final String PROPERTY_COS_UPDATE = "sharder.cosUpdateDate";
    private static final String ENV_PREFIX = Constants.isDevnet() ? "dev" : (Constants.isTestnet() ? "test" : "main");

    public static final String cosLastUpdateDate = Conch.getStringProperty(PROPERTY_COS_UPDATE,"");

    public static boolean isFullUpgrade(String mode){
        return VER_MODE_FULL.equalsIgnoreCase(mode);
    }

    public static void upgradeCos(boolean restart) throws IOException {
        upgradePackageThread(fetchLastCosVersion(),restart);  
    }
    
    public static Thread upgradePackageThread(com.alibaba.fastjson.JSONObject cosVerObj, Boolean restart) {
        Thread upgradePackageThread = new Thread(() -> {
            try {
                Conch.pause();
                fetchAndInstallUpgradePackage(cosVerObj);
                if (restart) {
                    Conch.restartApplication(null);
                }
            } catch (IOException e) {
                Logger.logErrorMessage("Can't fetch and install the latest version " + cosVerObj.getString("version") + ", ABORT the upgrade thread",e);
                Thread.currentThread().interrupt();
            }finally {
                Conch.unpause();
            }
        });
        
        upgradePackageThread.setDaemon(true);
        upgradePackageThread.start();
        return upgradePackageThread;
    }

    private static final long FETCH_INTERVAL_MS = 60*60*1000L;  // 60 minutes
    private static volatile JSONObject lastCosVerObj = null;
    private static long lastFetchTime = -1;
    
    private static boolean fetchLastPackageNow(){
        if(lastCosVerObj == null) return true;
        if(lastFetchTime == -1) return true;
        if(System.currentTimeMillis() - lastFetchTime > FETCH_INTERVAL_MS) return true;
        
        return false;
    }
    /**
     * {
     * "version":"0.1.3"
     * ,"mode":"incremental"
     * ,"bakMode":"delete"
     * ,"updateTime":"2019-04-22"
     * }
     * 
     * @return
     * @throws IOException
     */
    public static JSONObject fetchLastCosVersion() throws IOException {
        if(fetchLastPackageNow()) {
            String url = UrlManager.getHubLatestVersionUrl();
            Logger.logDebugMessage("fetch the last cos version from " + url);
            RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(url).get().request();
            lastCosVerObj = JSON.parseObject(response.getContent());
            lastFetchTime = System.currentTimeMillis();
        }
        
        return lastCosVerObj;
    }

    private static final String KEY_DB_LAST_ARCHIVE = "LastArchive";
    private static final String KEY_DB_ARCHIVE_KNOWN_HEIGHT = "KnownArchive";
    private static final String KEY_DB_DOWNLOAD_URL = "DownloadUrl";

    public static String lastDbArchive = null;
    public static Integer lastDbArchiveHeight = null;
    public static int restoredDbArchiveHeight = 0;
    public static List<Integer> knownDbArchives = Lists.newArrayList();

    public static volatile boolean forceDownloadFromOSS = false;
    private static volatile boolean restoring = false;
    private static final long FETCH_DB_ARCHIVE_INTERVAL_MS = 30*60*1000L;
    // default value is 5 days
    private static final long DOWNLOAD_DB_ARCHIVE_INTERVAL_MS = 5*(24*60*60*1000L);
    private static volatile JSONObject lastDbArchiveObj = null;
    private static long lastDbArchiveFetchTime = -1;
    private static long lastDownloadDbArchiveTime = -1;

    private static boolean fetchLastDbArchiveNow(){
        if(lastDbArchive == null) return true;
        if(lastDbArchiveObj == null) return true;
        if(lastDbArchiveFetchTime == -1) return true;
       
        if(System.currentTimeMillis() - lastDbArchiveFetchTime > FETCH_DB_ARCHIVE_INTERVAL_MS) return true;

        return false;
    }

    private static boolean downloadNewDbArchiveNow(File archivedDbFile){
        // first time to check
        long archiveFileModifiedTimeMS = archivedDbFile.lastModified();
        if(lastDownloadDbArchiveTime == -1) {
            lastDownloadDbArchiveTime = archiveFileModifiedTimeMS;
            if((System.currentTimeMillis() - lastDownloadDbArchiveTime) > DOWNLOAD_DB_ARCHIVE_INTERVAL_MS){
                return true;
            } else {
                return false;
            }
        }
        
        if((archiveFileModifiedTimeMS - lastDownloadDbArchiveTime) > DOWNLOAD_DB_ARCHIVE_INTERVAL_MS){
            return true;
        }
        return false;
    }



    private static JSONObject fetchAndParseTheDbArchiveMemo(){
        JSONObject archiveMemoKV = new JSONObject();
        String url = UrlManager.getDbArchiveDescriptionFileUrl();
        Logger.logDebugMessage("fetch the db archive description file from " + url);
        RestfulHttpClient.HttpResponse response = null;
        try {
            response = RestfulHttpClient.getClient(url).get().request();
        } catch (IOException e) {
            Logger.logErrorMessage("Can't fetch the db file from oss caused by ", e.getMessage());
            return archiveMemoKV;
        }
        String content = response.getContent();

        // parse the description file
        String[] settingArray = null;
        if(StringUtils.isNotEmpty(content)
                && content.contains("\n")){
            settingArray = content.split("\n");
            for(String keyPair : settingArray){
                if(keyPair == null || !keyPair.contains("=")) continue;
                String[] keyPairArray = keyPair.split("=");
                archiveMemoKV.put(keyPairArray[0], keyPairArray[1]);
            }
        }

        return archiveMemoKV;
    }

    /**
     *
     * @param lastDbArchive
     * @return String[2] : string[0] - last db archive name; string[1] - last archive height
     */
    private static String[] parseDbArchiveFileName(JSONObject lastDbArchive){
        String[] arry = new String[2];
        if(lastDbArchive == null) return arry;

        // last db archive
        if(lastDbArchive.containsKey(ENV_PREFIX + KEY_DB_LAST_ARCHIVE)){
            String lastDbArchiveName = lastDbArchive.getString(ENV_PREFIX + KEY_DB_LAST_ARCHIVE);
            try {
                arry[0] = lastDbArchiveName + ".zip";
                arry[1] = lastDbArchiveName.replace(Db.getName() + "_", "");
            } catch (Exception e) {
                Logger.logErrorMessage("Can't parse the lastDbArchive attribute caused by ", e.getMessage());
            }
        }
        return arry;
    }

    /**
     * testLastArchive=mw_test_db_12118
     * testKnownArchive=mw_test_db_268
     * @return latest db archive
     * @throws IOException
     */
    public static String fetchLastDbArchive()  {
        if(!fetchLastDbArchiveNow()) return lastDbArchive;

        lastDbArchiveObj = fetchAndParseTheDbArchiveMemo();
        lastDbArchiveFetchTime = System.currentTimeMillis();
        if(lastDbArchiveObj == null || lastDbArchiveObj.size() == 0) {
            Logger.logWarningMessage("Can't fetch the last db archive memo, maybe db archive memo file don't exist on the OSS, please check it");
            return lastDbArchive;
        }
        String[] dbArchiveKV = parseDbArchiveFileName(lastDbArchiveObj);
        lastDbArchive = dbArchiveKV[0];
        lastDbArchiveHeight = Integer.valueOf(dbArchiveKV[1]);

        // known archive height
        if(lastDbArchiveObj.containsKey(ENV_PREFIX + KEY_DB_ARCHIVE_KNOWN_HEIGHT)){
            String knownHeightStr = lastDbArchiveObj.getString(ENV_PREFIX + KEY_DB_ARCHIVE_KNOWN_HEIGHT);
            if(StringUtils.isNotEmpty(knownHeightStr)) {
                String[] array = knownHeightStr.split(",");
                for(int i = 0 ; i < array.length ; i++){
                    knownDbArchives.add(Integer.valueOf(array[i]));
                }
            }
        }
        Collections.sort(knownDbArchives);
        return lastDbArchive;
    }

    public static Integer getLastDbArchiveHeight() {
        if(lastDbArchiveHeight == null) fetchLastDbArchive();
        return lastDbArchiveHeight;
    }

    public static void fetchAndInstallUpgradePackage(com.alibaba.fastjson.JSONObject cosVerObj) throws IOException {
        String version = cosVerObj.getString("version");
        String mode = cosVerObj.getString("mode");
        String bakMode = cosVerObj.getString("bakMode");
        String updateTime = cosVerObj.getString("updateTime");

        File tempPath = new File("temp/");
        File archive = new File(tempPath, "cos-" + version + ".zip");
        boolean delete = true;
        if(StringUtils.isNotEmpty(bakMode) && BAK_MODE_BACKUP.equalsIgnoreCase(bakMode)) {
            delete = false;
        }

        if(archive.exists()) {
            archive.delete();
        }
        Logger.logInfoMessage("[ UPGRADE CLIENT ] Downloading upgrade package:" + archive.getName());
        FileUtils.copyURLToFile(new URL(UrlManager.getPackageDownloadUrl(version)), archive);
        Logger.logInfoMessage("[ UPGRADE CLIENT ] Decompressing upgrade package:" + archive.getName() + ",mode=" + mode + ",delete source=" + delete);
        FileUtil.unzipAndReplace(archive, mode, delete);
        try {
            if (!SystemUtils.IS_OS_WINDOWS) {
                Runtime.getRuntime().exec("chmod -R +x " + Conch.getUserHomeDir());
            }
        } catch (Exception e) {
            Logger.logErrorMessage("Failed to run after start script: chmod -R +x " + Conch.dirProvider.getUserHomeDir(), e);
        }

        if(StringUtils.isNotEmpty(updateTime)){
            Conch.storePropertieToFile(PROPERTY_COS_UPDATE, updateTime);
        }
    }

    /**
     * Update the local db to the archived db file of the specified height
     * @param upgradeDbHeight the height of the archived db file
     */
    public static void restoreDbAtHeight(String upgradeDbHeight) {
        String dbFileName =  Db.getName() + "_" + upgradeDbHeight + ".zip";
        restoreDb(dbFileName);
    }

    /**
     * Update the local db to specified archived db file
     * @param dbFileName target restore db file name
     */
    private static void restoreDb(String dbFileName){
        try{
//            if(restoring) return;
//            restoring = true;

            if(StringUtils.isEmpty(dbFileName)
                    || lastDbArchiveObj == null) {
                Logger.logDebugMessage("[ UPGRADE DB ] upgrade db file name is null, break.");
                return;
            }

            String urlPrefix = lastDbArchiveObj.getString(ENV_PREFIX + KEY_DB_DOWNLOAD_URL);
            String downloadingUrl = UrlManager.getDbArchiveUrl(urlPrefix + dbFileName);
            try {
                if(!RestfulHttpClient.findResource(downloadingUrl)) {
                    Logger.logWarningMessage("[ UPGRADE DB ] db archive %s dose not exist, break.");
                    return;
                }
            }catch(Exception e){
                Logger.logWarningMessage("[ UPGRADE DB ] db archive exist judgement occur error: %s, break and wait for next check turn.", e.getMessage());
                return;
            }

            Logger.logDebugMessage("[ UPGRADE DB ] Start to update the local db, pause the mining and blocks sync firstly");

            // check the last db file's download time and delete old file before download the new one.
            File tempPath = new File("temp/");
            File archivedDbFile = new File(tempPath, dbFileName);
            boolean downloadFromOSS = true;
            if(archivedDbFile.exists()){
                if(forceDownloadFromOSS || downloadNewDbArchiveNow(archivedDbFile)){
                    String lastDownloadTime = new Date(archivedDbFile.lastModified()).toString();
                    archivedDbFile.delete();
                }else{
                    long intervalHours = DOWNLOAD_DB_ARCHIVE_INTERVAL_MS / (60*60*1000L);
                    String nextDownloadTime = new Date(lastDownloadDbArchiveTime + DOWNLOAD_DB_ARCHIVE_INTERVAL_MS).toString();
                    String currentTime = new Date(System.currentTimeMillis()).toString();
                    Logger.logInfoMessage("[ UPGRADE DB ] Don't fetch the new db archive from OSS caused by: not reached the db archive download time[%s], " +
                                    "download interval should be %d hours [current time is %s]",
                            nextDownloadTime, intervalHours, currentTime);
                    downloadFromOSS = false;
                }
            }

            // fetch the specified archived db file
            if(downloadFromOSS) {
                FileUtil.clearOrDelFiles("temp/", false);
                Logger.logInfoMessage("[ UPGRADE DB ] Downloading archived db file %s from %s", dbFileName, downloadingUrl);
                try{
                    FileUtils.copyURLToFile(new URL(downloadingUrl), archivedDbFile);
                }catch(Exception e){
                    Logger.logWarningMessage("[ UPGRADE DB ] db archive downloading occur error: %s, break and wait for next check turn.", e.getMessage());
                    return;
                }
                lastDownloadDbArchiveTime = System.currentTimeMillis();
            }

            Conch.pause();

            // backup the old db folder
            Logger.logInfoMessage("[ UPGRADE DB ] Delete the bak folder");
            FileUtil.deleteDirectory(Paths.get(".","bak"));

            String dbFolder = Paths.get(".",Db.getName()).toString();
            Logger.logInfoMessage("[ UPGRADE DB ] Backup the current db folder %s ", dbFolder);
            FileUtil.backupFolder(dbFolder, true);

            // unzip the archived db file into application root
            String appRoot = Paths.get(".").toString();
            Logger.logInfoMessage("[ UPGRADE DB ] Unzip the archived db file %s into COS application folder %s", dbFileName, appRoot);
            try{
                FileUtil.unzip(archivedDbFile.getPath(), appRoot, false);
            }catch(Exception e){
                if(e instanceof ZipException) {
                    Logger.logInfoMessage(String.format("delete the zip file %s when the unzip operation failed[ %s ]", archivedDbFile.getPath() ,e.getMessage()));
                    if(archivedDbFile != null) {
                        archivedDbFile.delete();
                    }
                }
            }

            Logger.logInfoMessage("[ UPGRADE DB ] Success to update the local db[upgrade db file=%s]", dbFileName);
        }catch(Exception e) {
            Logger.logErrorMessage("[ UPGRADE DB ] Failed to update the local db[upgrade db file=%s] caused by [%s]", dbFileName, e.getMessage());
        }finally{
            Logger.logInfoMessage("[ UPGRADE DB ] Finish the local db upgrade, resume the block mining and blocks sync", dbFileName);
            Conch.unpause();
        }
    }

    private static void _restoreDbToLastArchive(boolean restartClient){
        if(lastDbArchive == null || lastDbArchiveHeight == null) fetchLastDbArchive();
        restoreDb(lastDbArchive);
        forceDownloadFromOSS = false;
        if(restartClient) Conch.restartApplication(null);
    }

    public static void restoreDbToLastArchive(boolean newThreadToExecute, boolean restartClient) {
        forceDownloadFromOSS = true;
        if(newThreadToExecute){
            new Thread(() -> {
                _restoreDbToLastArchive(restartClient);
            }).start();
        }else{
            _restoreDbToLastArchive(restartClient);
        }
    }

    public static void restoreDbToKnownHeight() {
        if(lastDbArchive == null || lastDbArchiveHeight == null) fetchLastDbArchive();
        
        // calculate the restore height
        if(restoredDbArchiveHeight == 0) {
            restoredDbArchiveHeight = lastDbArchiveHeight;
        } else{
            if(knownDbArchives.size() <= 0) {
                restoredDbArchiveHeight = lastDbArchiveHeight;
            } else {
                int index = knownDbArchives.size() - 1;
                while(index > 0
                        && restoredDbArchiveHeight > knownDbArchives.get(index)){
                    index--;
                }
                restoredDbArchiveHeight = knownDbArchives.get(index);  
            }
        }
        
        if(restoredDbArchiveHeight > 0) {
            String dbFileName =  Db.getName() + "_" + restoredDbArchiveHeight + ".zip";
            restoreDb(dbFileName);  
        }
    }
    
}
