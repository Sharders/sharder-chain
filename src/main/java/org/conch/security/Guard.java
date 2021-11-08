package org.conch.security;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.peer.Errors;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.Logger;
import sun.net.util.IPAddressUtil;

/**
 * Used to guard the client to avoid the viciously tcp/ip connect,
 * api request and others resources consumption
 *
 * @author bowen, ben
 */
public class Guard {

    private static boolean forceOpenGuard = Conch.getBooleanProperty("sharder.forceOpenGuard", false);
    /** Set 0 to close, non-0 to open */
    private static int OPEN_BLACKLIST_FILTER = 0;
    /** Whether is self closing mode | 是否处于自闭模式 */
    private static Boolean SELF_CLOSING_MODE = Boolean.TRUE;
    /** peer host : {"reason":"XXX", "selfCosingTime": 11} */
    private static String CLOSING_KEY_REASON = "reason";
    private static String CLOSING_KEY_TIME = "selfCosingTime";
    private static Map<String, JSONObject> SELF_CLOSING_MAP = Maps.newConcurrentMap();

    //TODO time based block list
    // black peer validation in : org.conch.peer.PeerServlet.process
    private static final int EXPIRED_TIME = 4 * (60 * 60 * 1000); //4hours
    private static final String FIRST_ACCESS_TIME_KEY = "firstAccessTime";
    private static final String LAST_ACCESS_TIME_KEY = "lastAccessTime";
    private static final String LATEST_ACCESS_TIME_KEY = "latestAccessTime";
    private static final String ACCESS_COUNT_KEY = "accessCount";
    private static Map<String, JSONObject> BLACK_PEERS_MAP = Maps.newConcurrentMap();
    private static Map<String, JSONObject> PEERS_ACCESS_RECORD_MAP = Maps.newConcurrentMap();
    /**
     * 若读取配置为空，则使用默认配置 * 放大倍率
     */
    private static final int MULTIPLE = 2;
    /**
     * Guard策略配置
     */
    private static int MAX_VICIOUS_COUNT_PER_SAME_HOST = 50;
    private static int FREQUENCY = 6 * MULTIPLE;
    private static int FREQUENCY_TO_BLACK = 20 * MULTIPLE;
    private static int MAX_THRESHOLD_PER_HOUR = 1 * MULTIPLE;
    private static int MAX_TOTAL_CONNECT_COUNT_PER_DAY = 500 * MULTIPLE;

    private static Integer threshold = 0;
    private static final Integer ONE_HOUR = 1000 * 60 * 60;
    private static final Integer FIVE_MINUTE = 1000 * 60 * 5;
    /** Interval of force connect to bootstrap nodes | 强制连接引导节点的间隔 */
    private static Integer FORCE_CONNECT_BOOTSTRAP_INTERVAL = Constants.isDevnet() ? 1000 * 1 : 1000 * 60 * 5;
    /**
     * Whether force connect to BootNode before download blocks from peers | 下载区块前是否强制连接Boot节点
     * NOTE: BootNode means only one node, bootstrap nodes means all bootstrap nodes in the network
     */
    private static Boolean FORCE_CONNECT_BOOT_NODE = Boolean.FALSE;

    private static long lastTime = System.currentTimeMillis();
    private static String lastDate = getCurrentDate(new Date());

    public static boolean needConnectBoot(long lastForceConnectMS) {
        return (System.currentTimeMillis() - lastForceConnectMS) > FORCE_CONNECT_BOOTSTRAP_INTERVAL;
    }

    public static void init(Integer frequency, Integer frequencyToBack, Integer maxThreshold,
                            Integer maxTotalConnection, Integer maxViciousCount, Integer openBlacklist,
                            Boolean openSelfClosingMode, Integer connectBootstrapInterval, Boolean forceConnectBoot) {
        if (frequency != null && frequency.intValue() > 0) {
            FREQUENCY = frequency;
        }
        if (frequencyToBack != null && frequencyToBack.intValue() > 0) {
            FREQUENCY_TO_BLACK = frequencyToBack;
        }
        if (maxThreshold != null && maxThreshold.intValue() > 0) {
            MAX_THRESHOLD_PER_HOUR = maxThreshold;
        }
        if (maxTotalConnection != null && maxTotalConnection.intValue() > 0) {
            MAX_TOTAL_CONNECT_COUNT_PER_DAY = maxTotalConnection;
        }
        if (maxViciousCount != null && maxViciousCount.intValue() > 0) {
            MAX_VICIOUS_COUNT_PER_SAME_HOST = maxViciousCount;
        }
        if (openBlacklist != null && openBlacklist.intValue() > 0) {
            OPEN_BLACKLIST_FILTER = openBlacklist;
        }
        if (openSelfClosingMode != null) {
            SELF_CLOSING_MODE = openSelfClosingMode;
        }
        if (connectBootstrapInterval != null && connectBootstrapInterval.longValue() > 0) {
            FORCE_CONNECT_BOOTSTRAP_INTERVAL = connectBootstrapInterval * 1000 * 60;
        }
        if (forceConnectBoot != null) {
            FORCE_CONNECT_BOOT_NODE = forceConnectBoot;
        }
    }

    public static boolean forceConnectToBootNode() {
        return FORCE_CONNECT_BOOT_NODE;
    }

    /**
     * OPEN_BLACKLIST_FILTER is a global setting for whole nodes
     * forceOpenGuard is a local setting that configured in the properties
     * Priority: forceOpenGuard > OPEN_BLACKLIST_FILTER
     *
     * @return
     */
    public static boolean isOpen() {
        return forceOpenGuard || OPEN_BLACKLIST_FILTER != 0;
    }

    /**
     * Whether is self closing mode
     * 是否处于自闭模式
     *
     * @return {"needClosing":true/false,"errorSummary":"XXX","errorReason":"XXX"}
     */
    public static final String KEY_NEED_CLOSING = "needClosing";
    public static final String KEY_ERROR_SUMMARY = "error";
    public static final String KEY_ERROR_REASON = "cause";

    public static org.json.simple.JSONObject isSelfClosingPeer(String peerHost) {
        org.json.simple.JSONObject result = new org.json.simple.JSONObject();
        if (!SELF_CLOSING_MODE) {
            result.put(KEY_NEED_CLOSING, false);
            return result;
        }

        if (SELF_CLOSING_MAP.containsKey(peerHost)) {
            // peer is the selfClosing peer
            result.put(KEY_NEED_CLOSING, true);
            result.put(KEY_ERROR_SUMMARY, Errors.BLACKLISTED_BY_THEM);
            result.put(KEY_ERROR_REASON, SELF_CLOSING_MAP.get(peerHost).getString(CLOSING_KEY_REASON));
            String errorReason = String.format("Peer %s returned error: %s", peerHost, Errors.BLACKLISTED_BY_THEM);
            Logger.logDebugMessage("%s", errorReason);
        } else {
            result.put(KEY_NEED_CLOSING, false);
        }
        return result;
    }

    public static void updateSelfClosingPeer(String peerHost, String reason) {
        // devnet off guard
        if ("127.0.0.1".equals(peerHost)
                || "localhost".equals(peerHost)
                || Constants.isDevnet() ? true : internalIp(peerHost)) {
            // don't guard the local request
            return;
        }
        if (!SELF_CLOSING_MODE) {
            return;
        }
        JSONObject detail = new JSONObject();
        detail.put(CLOSING_KEY_REASON, reason);
        detail.put(CLOSING_KEY_TIME, Conch.getEpochTime());
        SELF_CLOSING_MAP.put(peerHost, detail);
    }

    /**
     * Check whether exceed unlock time
     *
     * @param peerHost
     * @param curTime
     */
    public static void checkAndRemoveSelfClosingPeer(String peerHost, int curTime) {
        if (!SELF_CLOSING_MAP.containsKey(peerHost)) {
            return;
        }
        JSONObject detail = SELF_CLOSING_MAP.get(peerHost);
        int closingStarTime = detail.getInteger(CLOSING_KEY_TIME);
        if (closingStarTime > 0
                && closingStarTime + Peers.blacklistingPeriod <= curTime) {
            SELF_CLOSING_MAP.remove(peerHost);
        }
    }

    public static String getCurrentDate(Date date) {
        SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
        return ft.format(date);
    }

    public static boolean internalIp(String ip) {
        if (ip == null) {
            return false;
        }
        byte[] addr = IPAddressUtil.textToNumericFormatV4(ip);
        if(addr == null || addr.length == 0){
            return false;
        }
        return internalIp(addr);
    }

    public static boolean internalIp(byte[] addr) {
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        //10.x.x.x/8
        final byte SECTION_1 = 0x0A;
        //172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        //192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        switch (b0) {
            case SECTION_1:
                return true;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4) {
                    return true;
                }
            case SECTION_5:
                switch (b1) {
                    case SECTION_6:
                        return true;
                }
            default:
                return false;
        }
    }

    /**
     * 单个IP连接频率统计 & 防护
     *  * 加入IP连接数限制逻辑
     *  1. 对IP连接数 进行计数
     *  2. 连接数比对，超过最大值加入黑名单
     *  3. 连接频次记录，超过阈值加入黑名单
     *
     * <p>
     * 定义频率阈值 FREQUENCY = 6次/min, 超过20次/min直接拉入黑名单
     * 每小时可超过阈值次数 threshold <= MAX_THRESHOLD_PER_HOUR
     * 每小时后将超过阈值次数 threshold 置零
     * 计算得出单日最大连接数 total = ( 20 * 1 + 6 * 59 ) * 24 == 8976
     * total值仍然过大，因此设定单日最大值 MAX_TOTAL_CONNECT_COUNT_PER_DAY = 500
     * <p>
     * 以上规则仅在本次程序生命周期有效，重启后会重新载入 & 统计
     * TODO 需完善重启后数据的持久化（考虑将数据存入数据库）
     * <p>
     * 目前黑名单策略：一旦触发便加入黑名单（过期10min），再次触发：
     * 1. 若未过期则更新黑名单开始时间
     * 2. 若过期，则重新加入黑名单
     *
     * @param host
     */
    public static void defense(String host) {
        long startTime = System.currentTimeMillis();
        String startDate = getCurrentDate(new Date());
        try {
            if (FREQUENCY == -1 || !isOpen()) {
                return;
            }
            // devnet off guard
            if ("127.0.0.1".equals(host)
                    || "localhost".equals(host)
                    || Constants.isDevnet() ? true : internalIp(host)) {
                // don't guard the local request
                return;
            }
            if (!startDate.equals(lastDate)) {
                // 将该日数据存储到指定文件
//                ConcurrentMap<String, JSONObject> map = Maps.newConcurrentMap();
//                map.put(lastDate, JSONObject.parseObject(JSON.toJSONString(BLACK_PEERS_MAP_2)));
//                JSONObject parseObject = JSONObject.parseObject(JSON.toJSONString(map));
//                org.conch.util.JSON.JsonAppendAlibaba(parseObject, "conf/guardData.json");
                // TODO 每日将数据存入数据库
                PEERS_ACCESS_RECORD_MAP.clear();
            }
            JSONObject accessPeerObj = PEERS_ACCESS_RECORD_MAP.get(host);
            if (accessPeerObj == null) {
                accessPeerObj = new JSONObject();
                accessPeerObj.put(FIRST_ACCESS_TIME_KEY, System.currentTimeMillis());
                accessPeerObj.put(LAST_ACCESS_TIME_KEY, accessPeerObj.getLongValue(FIRST_ACCESS_TIME_KEY));
                accessPeerObj.put(LATEST_ACCESS_TIME_KEY, accessPeerObj.getLongValue(FIRST_ACCESS_TIME_KEY));
                accessPeerObj.put(ACCESS_COUNT_KEY, 1);
            } else {
                accessPeerObj.put(LAST_ACCESS_TIME_KEY, accessPeerObj.getLongValue(LATEST_ACCESS_TIME_KEY));
                accessPeerObj.put(LATEST_ACCESS_TIME_KEY, System.currentTimeMillis());
                accessPeerObj.put(ACCESS_COUNT_KEY, accessPeerObj.getIntValue(ACCESS_COUNT_KEY) + 1);
            }
            // 将更新的内容存入MAP
            PEERS_ACCESS_RECORD_MAP.put(host, accessPeerObj);
            long intervalTime = accessPeerObj.getLongValue(LATEST_ACCESS_TIME_KEY) - accessPeerObj.getLongValue(FIRST_ACCESS_TIME_KEY);
            // 因初期时间间隔不足 x min时，会导致分母过小致使frequency的值会过大，设定一个平均频率稳定期 stablePeriod = 5 min
            if (intervalTime > 0 && intervalTime > FIVE_MINUTE) {
                // 计算平均连接频率
                float frequency = (accessPeerObj.getLongValue(ACCESS_COUNT_KEY) * 1000 * 60) / intervalTime;
                if (startTime - lastTime > ONE_HOUR) {
                    threshold = 0;
                }
                int total;
                if (frequency > FREQUENCY_TO_BLACK) {
                    blackPeer(host, String.format("Exceed the access max frequency number %d", FREQUENCY_TO_BLACK));
                } else if (frequency > FREQUENCY) {
                    // 记录 threshold
                    threshold++;
                    if (threshold > MAX_THRESHOLD_PER_HOUR) {
                        blackPeer(host, String.format("Exceed the access max frequency count %d times", threshold));
                    }
                } else {
                    total = accessPeerObj.getIntValue(ACCESS_COUNT_KEY);
                    if (total > MAX_TOTAL_CONNECT_COUNT_PER_DAY) {
                        blackPeer(host, String.format("Exceed the access max count %d at one day", MAX_TOTAL_CONNECT_COUNT_PER_DAY));
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } finally {
            lastTime = startTime;
            lastDate = startDate;
        }

    }


    public static void viciousAccess(String host) {
        if ("127.0.0.1".equals(host)
            || "localhost".equals(host)
            || internalIp(host)) {
            // don't guard the local request
            return;
        }

        JSONObject accessPeerObj = BLACK_PEERS_MAP.get(host);
        if (accessPeerObj == null) {
            accessPeerObj = new JSONObject();
            accessPeerObj.put(FIRST_ACCESS_TIME_KEY, System.currentTimeMillis());
            accessPeerObj.put(ACCESS_COUNT_KEY, 1);
        } else {
            accessPeerObj.put(LAST_ACCESS_TIME_KEY, System.currentTimeMillis());
            accessPeerObj.put(ACCESS_COUNT_KEY, accessPeerObj.getIntValue(ACCESS_COUNT_KEY) + 1);
        }
//        else if (accessPeerObj.getLong(ACCESS_TIME_KEY) + EXPIRED_TIME > System.currentTimeMillis()) {
//            return accessPeerObj;
//        }

        if (accessPeerObj.getIntValue(ACCESS_COUNT_KEY) >= MAX_VICIOUS_COUNT_PER_SAME_HOST) {
            blackPeer(host, String.format("Exceed the vicious access max count %d", MAX_VICIOUS_COUNT_PER_SAME_HOST));
        }
    }

    public static boolean blackPeer(String host, String cause) {
        Peer peer = Peers.getPeer(host, true);
        if (peer == null) {
            rejectPeer(host);
        } else {
            peer.blacklist(String.format("Black the peer %s[%s] caused by %s", peer.getAnnouncedAddress(), peer.getHost(), cause));
        }
        return true;
    }

    public static boolean rejectPeer(String host) {
        addRejectRuleIntoFirewall(host);
        return true;
    }

    /**
     * call the shell to add reject rule into firewall of OS
     * - just support the CentOS and firewalld
     */
    private static void addRejectRuleIntoFirewall(String host) {
        Logger.logInfoMessage("Not implement addRejectRuleIntoFirewall now");
    }

    public static void main(String[] args) {
    }
}
