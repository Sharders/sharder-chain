package org.conch.common;

import org.conch.Conch;

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
    public static final String PEERS_LIST_EOLINKER = "http://result.eolinker.com/iDmJAldf2e4eb89669d9b305f7e014c215346e225f6fe41?uri=https://sharder.org/sc/peer/list.ss";
    public static final String PEERS_LIST_PATH = "/sc/peer/list.ss";

    /*=============================================Foundation API END========================================*/

    public static String getFoundationUrl(String eoLinkerUrl, String path) {
        if (Constants.isMainnet() || Constants.isTestnet()) {
            return Constants.HTTP + Conch.getSharderFoundationURL() + path;
        }
        return eoLinkerUrl;
    }
}
