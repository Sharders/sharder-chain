package org.conch.tools;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.conch.Conch;
import org.conch.common.UrlManager;
import org.conch.util.FileUtil;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-29
 */
public class ClientUpgradeTool {
    
    public static final String VER_MODE_FULL = "FULL";
    public static final String VER_MODE_INCREMENTAL = "INCREMENTAL";
    
    public static final String BAK_MODE_DELETE = "delete";
    public static final String BAK_MODE_BACKUP = "backup";
    
    public static boolean isFullUpgrade(String mode){
        return VER_MODE_FULL.equalsIgnoreCase(mode);
    }
    
    static class CosVer {
        String version;
        String updateTime;
        String mode = VER_MODE_INCREMENTAL;
        String bakMode = BAK_MODE_DELETE;

        public CosVer(String version, String mode, String updateTime, String bakMode) {
            this.version = version;
            this.mode = mode;
            this.updateTime = updateTime;
            this.bakMode = bakMode;
        }
    }
    
    
    public static Thread upgradePackageThread(String version, String mode,String bakMode, Boolean restart) {
        Thread upgradePackageThread = new Thread(
                () -> {
                    try {
                        fetchUpgradePackage(version, mode, bakMode);
                        if (restart) {
                            Conch.restartApplication(null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }

        );
        upgradePackageThread.setDaemon(true);
        upgradePackageThread.start();
        return upgradePackageThread;
    }
    
    public static void fetchUpgradePackage(String version, String mode, String bakmode) throws IOException {
        File tempPath = new File("temp/");
        File archive = new File(tempPath, "cos-" + version + ".zip");
        boolean delete = true;
        if(StringUtils.isNotEmpty(bakmode) && BAK_MODE_BACKUP.equalsIgnoreCase(bakmode)) {
            delete = false;
        }
        if (!archive.exists()) {
            Logger.logDebugMessage("[ UPGRADE CLIENT ] Downloading upgrade package:" + archive.getName());
            FileUtils.copyURLToFile(new URL(UrlManager.getPackageDownloadUrl(version)), archive);
        }
        Logger.logDebugMessage("[ UPGRADE CLIENT ] Decompressing upgrade package:" + archive.getName());
        FileUtil.unzipAndReplace(archive, mode, delete);
        try {
            if (!SystemUtils.IS_OS_WINDOWS) {
                Runtime.getRuntime().exec("chmod -R +x " + Conch.getUserHomeDir());
            }
        } catch (Exception e) {
            Logger.logErrorMessage("Failed to run after start script: chmod -R +x " + Conch.dirProvider.getUserHomeDir(), e);
        }
    }
    
    public static CosVer fetchLastCosVersion() throws IOException {
        RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(UrlManager.getHubLatestVersionUrl()).get().request();
        return JSON.parseObject(response.getContent(), CosVer.class);
        // return Convert.nullToEmpty(cosVer.getString(VER_KEY_VERSION)).replaceAll("[\r\n]", "");
    }
}
