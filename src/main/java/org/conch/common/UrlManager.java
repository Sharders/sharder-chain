package org.conch.common;

import org.conch.Conch;
import org.conch.util.IpUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * 统一管理eoLinker的URLs
 * 根据环境返回对应的接口URL
 *
 * @author CloudSen
 */
public class UrlManager {

    /*=============================================Foundation API START========================================*/
    /**
     * get NAT settings via sharder account
     */
    public static final String HUB_SETTING_ACCOUNT_CHECK_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/bounties/hubDirectory/check.ss";
    public static final String HUB_SETTING_ACCOUNT_CHECK_PATH = "/bounties/hubDirectory/check.ss";

    /**
     * report node configuration performance
     */
    public static final String NODE_CONFIG_REPORT_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/sc/peer/report.ss";
    public static final String NODE_CONFIG_REPORT_PATH = "/sc/peer/report.ss";

    /**
     * get peers list
     */
    public static final String PEERS_LIST_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/sc/peer/list.ss";
    public static final String PEERS_LIST_PATH = "/sc/peer/list.ss";

    /**
     * add message
     */
    public static final String ADD_MESSAGE_TO_SHARDER_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://localhost:8080/messageQueue/add.ss";
    public static final String ADD_MESSAGE_TO_SHARDER_PATH = "/messageQueue/add.ss";
    /*=============================================Foundation API END========================================*/

    /*=============================================HUB UPGRADE API START========================================*/

    private static final String ZIP_SUFFIX = ".zip";
    public static final String LATEST_VERSION_ONLINE_URL = "https://oss.sharder.org/cos/client/release/lastest-version";
    public static final String LATEST_VERSION_DEV_URL = "https://resource.sharder.io/sharder-hub/dev/release/lastest-version";
    public static final String DOWNLOAD_PACKAGE_ONLINE_URL = "https://oss.sharder.org/cos/client/release/cos-hub-";
    public static final String DOWNLOAD_PACKAGE_DEV_URL = "https://resource.sharder.io/sharder-hub/release/cos-hub-";

    /*=============================================HUB UPGRADE API END========================================*/

    public static String getFoundationUrl(String eoLinkerUrl, String path) {
        if (Constants.isMainnet() || Constants.isTestnet()) {
            return Constants.HTTP + Conch.getSharderFoundationURL() + path;
        }
        return eoLinkerUrl;
    }

    /**
     * foundation url filter
     *
     * @param request HttpServletRequest
     * @throws ConchException.NotValidException
     */
    public static void foundationHostFilter(HttpServletRequest request) throws ConchException.NotValidException {
        if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
            throw new ConchException.NotValidException("Not valid host! ONLY " + Conch.getSharderFoundationURL() + " can do this operation!");
        }
    }

    /**
     * Hub Latest Version URLs
     *
     * @return url
     */
    public static String getHubLatestVersionUrl() {
        return Constants.isMainnet() ? LATEST_VERSION_ONLINE_URL : LATEST_VERSION_DEV_URL;
    }

    /**
     * get latest hub version upgrade package download URLs
     *
     * @param version latest hub version
     * @return url
     */
    public static String getPackageDownloadUrl(String version) {
        return Constants.isMainnet() ? DOWNLOAD_PACKAGE_ONLINE_URL + version + ZIP_SUFFIX : DOWNLOAD_PACKAGE_DEV_URL + version + ZIP_SUFFIX;
    }
}
