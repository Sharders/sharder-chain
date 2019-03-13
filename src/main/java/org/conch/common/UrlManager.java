package org.conch.common;

import org.conch.Conch;
import org.conch.util.Convert;
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
    private static final String HTTP_SCHEME = "http://";
    private static final String HTTPS_SCHEME = "https://";

    /*=============================================Foundation API START========================================*/
    /**
     * get NAT settings via sharder account
     */
    public static final String HUB_SETTING_ACCOUNT_CHECK_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=http://sharder.org/bounties/hubDirectory/check.ss";
    public static final String HUB_SETTING_ACCOUNT_CHECK_LOCAL = "http://localhost:8080/bounties/hubDirectory/check.ss";
    public static final String HUB_SETTING_ACCOUNT_CHECK_PATH = "/bounties/hubDirectory/check.ss";

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
    private static final String LATEST_VERSION_ONLINE_URL = "https://oss.sharder.org/cos/client/release/cos-latest-version";
    private static final String LATEST_VERSION_DEV_URL = "https://resource.sharder.io/sharder-hub/dev/release/cos-latest-version";
    private static final String DOWNLOAD_PACKAGE_ONLINE_URL = "https://oss.sharder.org/cos/client/release/cos-hub-";
    private static final String DOWNLOAD_PACKAGE_DEV_URL = "https://resource.sharder.io/sharder-hub/dev/release/cos-hub-";

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
        if (Constants.isMainnet() || Constants.isTestnet()) {
            return HTTP_SCHEME + Conch.getSharderFoundationURL() + path;
        }
        return USE_EOLINKER ? eoLinkerUrl : localUrl;
    }

    /**
     * check validation of request host
     *
     * @param request HttpServletRequest
     * @throws ConchException.NotValidException
     */
    public static void validFoundationHost(HttpServletRequest request) throws ConchException.NotValidException {
        if (!IpUtil.matchHost(request, Conch.getSharderFoundationURL())) {
            throw new ConchException.NotValidException(Convert.stringTemplate(Constants.HOST_FILTER_INFO, Conch.getSharderFoundationURL()));
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
