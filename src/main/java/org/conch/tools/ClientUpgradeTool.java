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
import java.util.List;

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
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }finally {
                Conch.unpause();
            }
        });
        
        upgradePackageThread.setDaemon(true);
        upgradePackageThread.start();
        return upgradePackageThread;
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
            if(StringUtils.isEmpty(dbFileName)) {
                Logger.logDebugMessage("[ UPGRADE DB ] upgrade db file name is null, break.");
                return;
            }
            Logger.logDebugMessage("[ UPGRADE DB ] Start to update the local db, pause the mining and blocks sync firstly");
            Conch.pause();

            // fetch the specified archived db file
            File tempPath = new File("temp/");
            File archivedDbFile = new File(tempPath, dbFileName);
            String downloadingUrl = UrlManager.getDbArchiveUrl(dbFileName);
            Logger.logInfoMessage("[ UPGRADE DB ] Downloading archived db file %s from %s", dbFileName, downloadingUrl);
            
            if(archivedDbFile.exists()){
                archivedDbFile.delete();
            }
            FileUtils.copyURLToFile(new URL(downloadingUrl), archivedDbFile);
   
            // backup the old db folder
            String dbFolder = Paths.get(".",Db.getName()).toString();
            Logger.logInfoMessage("[ UPGRADE DB ] Backup the current db folder %s ", dbFolder);
            FileUtil.backupFolder(dbFolder, true);

            //FileUtil.deleteDbFolder();
            
            // unzip the archived db file into application root
            String appRoot = Paths.get(".").toString();
            Logger.logInfoMessage("[ UPGRADE DB ] Unzip the archived db file %s into COS application folder %s", dbFileName, appRoot);
            FileUtil.unzip(archivedDbFile.getPath(), appRoot, true);
            Logger.logInfoMessage("[ UPGRADE DB ] Success to update the local db[upgrade db file=%s]", dbFileName);
        }catch(Exception e) {
            Logger.logErrorMessage("[ UPGRADE DB ] Failed to update the local db[upgrade db file=%s] caused by [%s]", dbFileName, e.getMessage());
        }finally{
            Logger.logInfoMessage("[ UPGRADE DB ] Finish the local db upgrade, resume the block mining and blocks sync", dbFileName);
            Conch.unpause();
        }
    }



    private static final long FETCH_INTERVAL_MS = 25*60*1000L;
    private static volatile JSONObject lastCosVerObj = null;
    private static long lastFetchTime = -1;
    
    private static boolean fetchNow(){
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
        if(fetchNow()) {
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
    
    public static String lastDbArchive = null;
    public static Integer lastDbArchiveHeight = null;
    public static int restoredDbArchiveHeight = 0;
    public static List<Integer> knownDbArchives = Lists.newArrayList();
    /**
     * testLastArchive=sharder_test_db_12118
     * testKnownArchive=sharder_test_db_268
     * @return latest db archive
     * @throws IOException
     */
    public static String fetchLastDbArchive()  {
        String url = UrlManager.getDbArchiveDescriptionFileUrl();
        Logger.logDebugMessage("fetch the db archive description file from " + url);
        RestfulHttpClient.HttpResponse response = null;
        try {
            response = RestfulHttpClient.getClient(url).get().request();
        } catch (IOException e) {
            Logger.logErrorMessage("Can't fetch the db file from oss caused by ", e.getMessage());
            return "";
        }
        String content = response.getContent();
        
        // parse the description file
        JSONObject dbArchiveObj = new JSONObject();
        while(StringUtils.isNotEmpty(content) 
                && content.contains("\n")){
            String[] array = content.split("\n");
            if(array != null 
            && array[0].contains("=")){
                String[] heightConfigAry =array[0].split("=");
                dbArchiveObj.put(heightConfigAry[0], heightConfigAry[1]);
            }
            content = array[1];
        }
        
        String envPrefix = Constants.isDevnet() ? "dev" : (Constants.isTestnet() ? "test" : "main");
        // last db archive
        if(dbArchiveObj.containsKey(envPrefix + KEY_DB_LAST_ARCHIVE)){
            String lastDbArchiveName = dbArchiveObj.getString(envPrefix + KEY_DB_LAST_ARCHIVE);
            try {
                lastDbArchive = lastDbArchiveName + ".zip";
                lastDbArchiveHeight = Integer.valueOf(lastDbArchiveName.replace(Db.getName() + "_", ""));
            } catch (Exception e) {
                Logger.logErrorMessage("Can't parse the lastDbArchive attribute caused by ", e.getMessage());
            }
        }
        
        // known archive height
        if(dbArchiveObj.containsKey(envPrefix + KEY_DB_ARCHIVE_KNOWN_HEIGHT)){
            String knownHeightStr = dbArchiveObj.getString(envPrefix + KEY_DB_ARCHIVE_KNOWN_HEIGHT);
            if(StringUtils.isNotEmpty(knownHeightStr)) {
                String[] array = content.split(",");
                for(int i = 0 ; i < array.length ; i++){
                    knownDbArchives.add(Integer.valueOf(array[i]));
                }
            }
        }
        Collections.sort(knownDbArchives);
        return lastDbArchive;
    }

    public static int getLastDbArchiveHeight() {
        if(lastDbArchiveHeight == null) fetchLastDbArchive();
        return lastDbArchiveHeight;
    }

    public static void restoreDbToLastArchive() {
        if(lastDbArchive == null || lastDbArchiveHeight == null) fetchLastDbArchive();
        restoreDb(lastDbArchive);
    }

    public static void restoreDbToKnownHeight() {
        if(lastDbArchive == null || lastDbArchiveHeight == null) fetchLastDbArchive();
        
        // calculate the restore height
        if(restoredDbArchiveHeight == 0) {
            restoredDbArchiveHeight = lastDbArchiveHeight;
        } else{
            int index = knownDbArchives.size() - 1;
            while(index > 0
                    && restoredDbArchiveHeight > knownDbArchives.get(index)){
                index--;
            }
            restoredDbArchiveHeight = knownDbArchives.get(index);
        }
        
        if(restoredDbArchiveHeight > 0) {
            String dbFileName =  Db.getName() + "_" + restoredDbArchiveHeight + ".zip";
            restoreDb(dbFileName);  
        }
    }
    
}
