package org.conch.common;

import org.conch.Conch;
import org.conch.util.IpUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Manage all of the URLs in project
 *
 * @author CloudSen
 */
public class UrlManager {

    public static final Boolean USE_EOLINKER = Optional.ofNullable(Conch.getStringProperty("sharder.dev.useEolinker"))
            .map(Boolean::valueOf).orElse(false);
    public static final Boolean USE_LOCAL =Optional.ofNullable(Conch.getStringProperty("sharder.dev.local"))
            .map(Boolean::valueOf).orElse(false);
    private static final String HTTP_SCHEME = "http://";
    private static final String HTTPS_SCHEME = "https://";

    /*=============================================Foundation API START========================================*/
    /**
     * check sharder hardware product
     */
    public static final String GET_HARDWARE_TYPE_EOLINKER = "https://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://localhost:8080/sc/ssHardwareProduct/serialNum/";
    public static final String GET_HARDWARE_TYPE_LOCAL = "http://localhost:8080/sc/ssHardwareProduct/serialNum/{serialNum}";
    public static final String GET_HARDWARE_TYPE_PATH = "/sc/ssHardwareProduct/serialNum/{serialNum}";

    /**
     * get NAT settings via sharder account
     */
    public static final String HUB_SETTING_ACCOUNT_CHECK_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/bounties/hubDirectory/check.ss";
    public static final String HUB_SETTING_ACCOUNT_CHECK_LOCAL = "http://localhost:8080/sc/natServices/fetch";
    public static final String HUB_SETTING_ACCOUNT_CHECK_PATH = "/sc/natServices/fetch";
    
    public static final String HUB_SETTING_ADDRESS_BIND_LOCAL = "http://localhost:8080/sc/natServices/bind";
    public static final String HUB_SETTING_ADDRESS_BIND_PATH = "/sc/natServices/bind";

    /**
     * report node configuration performance
     */
    public static final String NODE_CONFIG_REPORT_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/sc/peer/report.ss";
    public static final String NODE_CONFIG_REPORT_LOCAL = "http://localhost:8080/sc/peer/report.ss";
    public static final String NODE_CONFIG_REPORT_PATH = "/sc/peer/report.ss";

    /**
     * get peers list
     */
    public static final String PEERS_LIST_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/sc/peer/list.ss";
    public static final String PEERS_LIST_LOCAL = "http://localhost:8080/sc/peer/list.ss";
    public static final String PEERS_LIST_PATH = "/sc/peer/list.ss";

    /**
     * add message
     */
    public static final String ADD_MESSAGE_TO_SHARDER_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://localhost:8080/messageQueue/add.ss";
    public static final String ADD_MESSAGE_TO_SHARDER_LOCAL = "http://localhost:8080/messageQueue/add.ss";
    public static final String ADD_MESSAGE_TO_SHARDER_PATH = "/messageQueue/add.ss";
    /*=============================================Foundation API END========================================*/

    /*=============================================HUB UPGRADE API START========================================*/

    private static final String ZIP_SUFFIX = ".zip";
    private static final String SEVENZIP_SUFFIX = ".7z";
    private static final String LATEST_VERSION_ONLINE_URL = Constants.OSS_PREFIX + "cos/client/release/cos-latest-version";
    private static final String LATEST_VERSION_DEV_URL = Constants.OSS_PREFIX + "cos/client/dev/cos-latest-version";
    private static final String DOWNLOAD_PACKAGE_ONLINE_URL = Constants.OSS_PREFIX + "cos/client/release/cos-";
    private static final String DOWNLOAD_PACKAGE_DEV_URL = Constants.OSS_PREFIX + "cos/client/dev/cos-";

    private static final String ARCHIVE_DB_ONLINE_URL = Constants.OSS_PREFIX + "cos/client/release/cos-db-archive";
    private static final String ARCHIVE_DB_DEV_URL = Constants.OSS_PREFIX + "cos/client/dev/cos-db-archive";
    
    private static final String COS_RELEASE_URL = Constants.OSS_PREFIX + "cos/client/release/";

    /**
     * 该地址更新，避免文件格式变化后未升级的节点无法正常读取文件 filename = constant-settings
     */
    public static final String KNOWN_IGNORE_BLOCKS = Constants.OSS_PREFIX + "cos/client/release/ignore-blocks";
    public static final String CONSTANT_SETTINGS = Constants.OSS_PREFIX + "cos/client/release/constant-settings";
    public static final String CMD_TOOLS = Constants.OSS_PREFIX + "cos/client/release/cmd-tools";
    /*=============================================HUB UPGRADE API END========================================*/
    
    /**
     * get foundation API URLs
     *
     * @param eoLinkerUrl eoLinker interface url
     * @param localUrl    local test url
     * @param path        query path
     * @return URLs
     */
    public static String getFoundationUrl(String eoLinkerUrl, String localUrl, String path) {
        if (!USE_LOCAL) {
            return HTTPS_SCHEME + Conch.getSharderFoundationURL() + path;
        }
        return USE_EOLINKER ? eoLinkerUrl : localUrl;
    }

    /**
     * get API URLs
     */
    public static String getFoundationApiUrl(String path) {
        String domain = USE_LOCAL ? "127.0.0.1" : Conch.getSharderFoundationURL();
        return HTTPS_SCHEME + domain + path;
    }

    /**
     * check validation of request host
     *
     * @param request HttpServletRequest
     * @throws ConchException.NotValidException
     */
    public static boolean validFoundationHost(HttpServletRequest request) throws ConchException.NotValidException {
        if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
            return false;
//            throw new ConchException.NotValidException(Convert.stringTemplate(Constants.HOST_FILTER_INFO, Conch.getSharderFoundationURL()));
        }

        return true;
    }

    /**
     * Hub Latest Version URLs
     *
     * @return url
     */
    public static String getHubLatestVersionUrl() {
        return Constants.isDevnet() ?  LATEST_VERSION_DEV_URL : LATEST_VERSION_ONLINE_URL;
    }

    /**
     * get latest hub version upgrade package download URLs ZIP
     *
     * @param version latest hub version
     * @return url
     */
    public static String getPackageDownloadUrlZip(String version) {
        String prefix = Constants.isDevnet() ?  DOWNLOAD_PACKAGE_DEV_URL : DOWNLOAD_PACKAGE_ONLINE_URL;
//        String prefix = DOWNLOAD_PACKAGE_DEV_URL;
        return prefix + version + ZIP_SUFFIX;
    }

    /**
     * get latest hub version upgrade package download URLs 7Z
     *
     * @param version latest hub version
     * @return url
     */
    public static String getPackageDownloadUrlSevenZip(String version) {
        String prefix = Constants.isDevnet() ?  DOWNLOAD_PACKAGE_DEV_URL : DOWNLOAD_PACKAGE_ONLINE_URL;
//        String prefix = DOWNLOAD_PACKAGE_DEV_URL;
        return prefix + version + SEVENZIP_SUFFIX;
    }
    
    public static String getDbArchiveDescriptionFileUrl() {
        return Constants.isDevnet() ?  ARCHIVE_DB_DEV_URL : ARCHIVE_DB_ONLINE_URL;
    }
    
    public static String getDbArchiveUrl(String archivedDbFile) {
        return archivedDbFile.startsWith("http") ? archivedDbFile : (COS_RELEASE_URL + archivedDbFile);
    }
}
