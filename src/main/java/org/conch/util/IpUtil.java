package org.conch.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import sun.net.util.IPAddressUtil;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/18
 */
public class IpUtil {

    /**
     * @param url don't include the port
     * @return
     */
    public static String checkOrToIp(String url) {
        try {
            if (StringUtils.isEmpty(url)
                    || "undefined".equalsIgnoreCase(url)) {
                return "";
            }

            if (!isDomain(url)) {
                return url;
            }
            return InetAddress.getByName(url).getHostAddress();

        } catch (UnknownHostException e) {
            Logger.logErrorMessage("can't finish checkOrToIp with url[" + url + "] caused by " + e.getMessage());
        }
        return "";
    }

    private final static List<String> natServers = Lists.newArrayList(
            "nat.mw.run",
            "nat.mwfs.io",
            "nat.sharder.network",
            "nat.sharder.org",
            "nat.sharder.io");

    private static String checkOrParseNatUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return "";
        }
        for (String natServer : natServers) {
            if (!url.contains(natServer)) {
                continue;
            }

            // get domain
            int portIndex = url.indexOf(":");
            if (portIndex > 0) {
                return url.substring(0, portIndex);
            }
        }
        return "";
    }

    /**
     * @param url can't include the port
     */
    public static String getHost(String url) {
        if (StringUtils.isEmpty(url)) {
            return "";
        }

        String parsedUrl = checkOrParseNatUrl(url);
        if (StringUtils.isNotBlank(parsedUrl)) {
            return parsedUrl;
        }

        try {
            return InetAddress.getByName(url).getHostName();
        } catch (UnknownHostException e) {
            Logger.logErrorMessage("can't finish getHost with url[" + url + "] caused by " + e.getMessage());
        }
        return "";
    }

    public static String getSenderIp(HttpServletRequest req) {
        return req.getHeader("x-forwarded-for") == null ? req.getRemoteAddr() : req.getHeader("x-forwarded-for");
    }

    public static boolean matchHost(HttpServletRequest req, String host) {
        String senderIp = getSenderIp(req);
        return senderIp.equals(checkOrToIp(host));
    }

    public static boolean matchHost(String host, String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        return ip.equals(checkOrToIp(host));
    }

    public static boolean isFoundationDomain(String host) {
        if (StringUtils.isEmpty(host)) {
            return false;
        }

        if (host.endsWith("sharder.io")
                || host.endsWith("mwfs.io")
                || host.endsWith("mw.run")
                || host.endsWith("sharder.network")) {
            return true;
        }

        return false;
    }

    /**
     * ip addr whether is the internal ip
     *
     * @param ipv4Addr
     * @return
     */
    public static boolean isInternalIp(String ipv4Addr) {
        return _isInternalIp(IPAddressUtil.textToNumericFormatV4(ipv4Addr));
    }

    /**
     * ipv4
     * tcp/ip协议中，专门保留了三个IP地址区域作为私有地址，其地址范围如下：
     * 10.0.0.0/8：10.0.0.0～10.255.255.255 
     * 172.16.0.0/12：172.16.0.0～172.31.255.255 
     * 192.168.0.0/16：192.168.0.0～192.168.255.255
     *
     * @param addr
     * @return
     */
    private static boolean _isInternalIp(byte[] addr) {
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
     * public ip -> internal ip
     *
     * @return
     */
    public static String getNetworkIp() {
        try {
            String localIp = null;
            String netIp = null;

            Enumeration<NetworkInterface> netInterfaces = null;
            netInterfaces = NetworkInterface.getNetworkInterfaces();

            InetAddress ip = null;
            boolean fined = false;// 
            while (netInterfaces.hasMoreElements() && !fined) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> address = ni.getInetAddresses();
                while (address.hasMoreElements()) {
                    ip = address.nextElement();
                    if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":") == -1) {
                        netIp = ip.getHostAddress();
                        fined = true;
                        break;
                    } else if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(":") == -1) {
                        // 
                        localIp = ip.getHostAddress();
                    }
                }
            }

            if (netIp != null && !"".equals(netIp)) {
                return netIp;
            } else {
                return localIp;
            }
        } catch (Exception e) {
            Logger.logErrorMessage("can't finish getNetworkIp caused by " + e.getMessage());
            return "";
        }
    }

    /* domain pattern */
    public static String PATTERN_L2DOMAIN = "\\w*\\.\\w*:";
    //    public static String PATTERN_IP = "(\\d*\\.){3}\\d*";
    public static String PATTERN_IP = "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";

    /**
     * url whether is domain
     *
     * @param url
     * @return
     */
    public static boolean isDomain(String url) {
        if (StringUtils.isEmpty(url)) {
            return false;
        }

        Pattern ipPattern = Pattern.compile(PATTERN_IP);
        Matcher matcher = ipPattern.matcher(url);
        return !matcher.find();
    }

    /**
     * get host from url
     *
     * @param url domain ot url contains port
     * @return host: domain or public ip
     */
    public static String getHostFromUrl(String url) {
        if (isDomain(url)
                && !url.contains(":")
                && !url.contains("/")
                && !url.contains("http")) {
            return url;
        }

        // return ip if ip pattern matched
        Pattern ipPattern = Pattern.compile(PATTERN_IP);
        Matcher matcher = ipPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }

        // return domain if ip pattern matched
        Pattern pattern = Pattern.compile(PATTERN_L2DOMAIN);
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            int endIndex = url.lastIndexOf(":");
            return url.substring(0, endIndex);
        }
        return null;
    }

    /**
     * get external ip from url
     *
     * @param url domain ot url contains port
     * @return public network ip
     */
    public static String getIpFromUrl(String url) {
        String host = getHostFromUrl(url);
        return checkOrToIp(host);
    }

    /** ip to GEO **/
    private static final int AGING = 1000 * 60 * 60; //60 minutes
    private static final String CACHE_TIME_KEY = "cacheTime";
    private static Map<String, JSONObject> NAT_ADDR_IP_AND_MAP = Maps.newConcurrentMap();
    private static JSONObject COORDINATES_CACHE = new JSONObject();

    private static JSONObject DEFAULT_COORDINATES = new JSONObject();

    static {
        // default coordinates
        DEFAULT_COORDINATES.put("180.149.130.27", JSON.parseObject("{\"X\":\"39.9289\",\"Y\":\"116.3883\"}"));
        DEFAULT_COORDINATES.put("114.80.68.233", JSON.parseObject("{\"X\":\"31.0456\",\"Y\":\"121.3997\"}"));
        DEFAULT_COORDINATES.put("223.104.96.113", JSON.parseObject("{\"X\":\"28.1375\",\"Y\":\"106.8200\"}"));
        DEFAULT_COORDINATES.put("14.215.160.13", JSON.parseObject("{\"X\":\"23.0268\",\"Y\":\"113.1315\"}"));
        DEFAULT_COORDINATES.put("123.151.77.71", JSON.parseObject("{\"X\":\"39.1422\",\"Y\":\"117.1767\"}"));
        DEFAULT_COORDINATES.put("112.66.1.203", JSON.parseObject("{\"X\":\"20.0458\",\"Y\":\"110.3417\"}"));
        DEFAULT_COORDINATES.put("113.116.52.10", JSON.parseObject("{\"X\":\"22.5333\",\"Y\":\"114.1333\"}"));
        DEFAULT_COORDINATES.put("49.83.135.23", JSON.parseObject("{\"X\":\"32.0617\",\"Y\":\"118.7778\"}"));
        DEFAULT_COORDINATES.put("36.149.20.90", JSON.parseObject("{\"X\":\"39.9289\",\"Y\":\"116.3883\"}"));
        DEFAULT_COORDINATES.put("223.240.28.101", JSON.parseObject("{\"X\":\"31.8639\",\"Y\":\"117.2808\"}"));
        DEFAULT_COORDINATES.put("23.251.52.66", JSON.parseObject("{\"X\":\"22.3310\",\"Y\":\"114.1592\"}"));
        DEFAULT_COORDINATES.put("113.76.101.170", JSON.parseObject("{\"X\":\"22.2769\",\"Y\":\"113.5678\"}"));
    }

    /**
     * @param hostList
     * @return
     */
    public static JSONObject parseAndConvertToCoordinates(List<String> hostList) {
        JSONObject coordinates = new JSONObject();
        coordinates.putAll(DEFAULT_COORDINATES);
        if (hostList == null || hostList.size() == 0) {
            return coordinates;
        }

        //        if (hostList.size() > 1000) {
        //            Logger.logWarningMessage("Quantity is too large than 1000");
        //            return coordinates;
        //        }

        // convert the ip to the coordinate
        Set<String> natAddrSet = Sets.newHashSet();
        for (String ip : hostList) {
            if (StringUtils.isEmpty(ip)) {
                continue;
            }

            if (isIP(ip)) {
                coordinates.put(ip, queryAndFillCoordinate(ip));
            } else {
                natAddrSet.add(ip);
            }
        }

        // nat addr processing: nat addr -> public ip -> coordinate
        if (natAddrSet.size() > 0) {
            Map<String, String> natAddrIpMap = convertNatAddrToIP(natAddrSet);
            for (String natAddr : natAddrSet) {
                String realIp = natAddrIpMap.get(natAddr);
                if (StringUtils.isEmpty(realIp)) {
                    continue;
                }
                coordinates.put(natAddr, queryAndFillCoordinate(realIp));
            }
        }

        return coordinates;
    }

    /**
     * send request to certification to convert
     * and cache to the local
     *
     * @param natAddrSet
     * @return
     */
    private static Map<String, String> convertNatAddrToIP(Set<String> natAddrSet) {
        Map<String, String> natAddrAndIpMap = Maps.newHashMap();

        Set<String> wishToUpdateNatAddrSet = Sets.newHashSet();
        // read from local cache in the cache lifecycle firstly
        for (String natAddr : natAddrSet) {
            if (NAT_ADDR_IP_AND_MAP.containsKey(natAddr)) {
                JSONObject cacheObj = NAT_ADDR_IP_AND_MAP.get(natAddr);
                // judgement for cache time expired
                if (cacheObj.getLong(CACHE_TIME_KEY) + AGING > System.currentTimeMillis()) {
                    natAddrAndIpMap.put(cacheObj.getString("natAddr"), cacheObj.getString("ip"));
                } else {
                    wishToUpdateNatAddrSet.add(natAddr);
                }
            } else {
                wishToUpdateNatAddrSet.add(natAddr);
            }
        }

        // send request to certification to convert nat addr to real ip
        if (wishToUpdateNatAddrSet.size() > 0) {
            RestfulHttpClient.HttpResponse verifyResponse = null;
            try {
                verifyResponse = RestfulHttpClient.getClient(UrlManager.getFoundationApiUrl("/sc/natServices" +
                        "/natAddrToIP"))
                        .post()
                        .addPostParam("natAddrList", wishToUpdateNatAddrSet.toString())
                        .request();
            } catch (Exception e) {
                Logger.logErrorMessage("Send request to convert the nat addr to real ip failed", e);
            }

            // update
            boolean querySuccess = false;
            if (verifyResponse != null && StringUtils.isNotEmpty(verifyResponse.getContent())) {
                querySuccess = JSONObject.parseObject(verifyResponse.getContent())
                        .getBooleanValue(Constants.SUCCESS);
            }

            if (querySuccess) {
                JSONObject natAddrAndIpObj =
                        JSON.parseObject(verifyResponse.getContent()).getJSONObject(Constants.DATA);

                if (natAddrAndIpObj != null) {
                    Map<String, String> currentMapping = natAddrAndIpObj.toJavaObject(new TypeReference<Map<String,
                            String>>() {});
                    natAddrAndIpMap.putAll(currentMapping);

                    Set<String> currentNatAddrSet = currentMapping.keySet();
                    // update the current result to cache and update the cache time
                    for (String natAddr : currentNatAddrSet) {
                        JSONObject cacheObj = NAT_ADDR_IP_AND_MAP.containsKey(natAddr) ?
                                NAT_ADDR_IP_AND_MAP.get(natAddr) : new JSONObject();
                        cacheObj.put(CACHE_TIME_KEY, System.currentTimeMillis());
                        cacheObj.put("natAddr", natAddr);
                        cacheObj.put("ip", currentMapping.get(natAddr));
                        NAT_ADDR_IP_AND_MAP.put(natAddr, cacheObj);
                    }
                }
            } else {
                Logger.logWarningMessage("Can't fetch and convert the nat addr to real ip {}",
                        wishToUpdateNatAddrSet.toString());
            }
        }

        return natAddrAndIpMap;
    }

    private static boolean isIP(String ip) {
        return ip.matches("(2(5[0-5]{1}|[0-4]\\d{1})|[0-1]?\\d{1,2})(\\.(2(5[0-5]{1}|[0-4]\\d{1})|[0-1]?\\d{1,2})){3}");
    }

    private static JSONObject queryAndFillCoordinate(String ip) {
        // read from cache firstly
        JSONObject coordinateObj = COORDINATES_CACHE.getJSONObject(ip);
        if (coordinateObj == null) {
            coordinateObj = new JSONObject();
            coordinateObj.put(CACHE_TIME_KEY, System.currentTimeMillis());
        } else if (coordinateObj.getLong(CACHE_TIME_KEY) + AGING > System.currentTimeMillis()) {
            return coordinateObj;
        }

        // query and convert coordinate
        switch (new Random().nextInt(3) + 1) {
            /*case 0:
                request882667(json, ip);
                break;*/
            case 1:
                requestIpLocationTools(coordinateObj, ip);
                break;
            case 2:
                requestIpIp(coordinateObj, ip);
                break;
            case 3:
                requestGeoIpTool(coordinateObj, ip);
                break;
            default:
                requestIpLocationTools(coordinateObj, ip);
        }

        // update the cache lifecycle
        if (!"".equals(coordinateObj.getString("X"))
                && !"".equals(coordinateObj.getString("Y"))) {
            coordinateObj.put(CACHE_TIME_KEY, System.currentTimeMillis());
        } else {
            // 5 minutes to convert again when convert failed
            long failedAdjustmentMS = AGING - 5 * 60 * 1000;
            coordinateObj.put(CACHE_TIME_KEY, System.currentTimeMillis() - failedAdjustmentMS);
        }

        synchronized (COORDINATES_CACHE) {
            COORDINATES_CACHE.put(ip, coordinateObj);
        }

        return coordinateObj;
    }

    /*
    private void request882667(JSONObject json, String ip) {
        try {
            Document document = Jsoup.connect("http://www.882667.com/ip_" + ip + ".html").get();
            Elements el = document.select(".shuru.biankuang p:last-child");
            json.put("X", el.select("span.lansezi:nth-child(1)").text());
            json.put("Y", el.select("span.lansezi:nth-child(2)").text());
        } catch (IOException e) {
            log.warn("request882667 failed" + e.getMessage());
        }
    }*/

    private static boolean geoQueryExceptionSwitch = false;

    private static void requestIpLocationTools(JSONObject json, String ip) {
        try {
            Document document = Jsoup.connect("https://www.iplocationtools.com/" + ip).get();
            String ne = document.select(".table tbody tr:nth-child(2) td:nth-child(2)").text();
            json.put("X", ne.substring(ne.indexOf("(") + 1, ne.indexOf(",")));
            json.put("Y", ne.substring(ne.indexOf(",") + 2, ne.indexOf(")")));
        } catch (Exception e) {
            if (geoQueryExceptionSwitch
                    && Logger.printNow(Logger.IpUtil_geoTransformFailed)) {
                Logger.logDebugMessage("requestGeoIpTool failed: " + e.getMessage());
            }
        }
    }

    private static void requestIpIp(JSONObject json, String ip) {
        try {
            Document document = Jsoup.connect("https://www.ipip.net/ip.html")
                    .data("ip", ip).post();
            String el = document.select(".inner table:nth-child(2) tbody tr:last-child td:last-child").text();
            System.out.println("el:" + el);
            if (el.equals("局域网 产品详情")) {
                json.put("X", "0");
                json.put("Y", "0");
            } else {
                json.put("X", el.split(", ")[0]);
                json.put("Y", el.split(", ")[1]);
            }

        } catch (Exception e) {
            if (geoQueryExceptionSwitch
                    && Logger.printNow(Logger.IpUtil_geoTransformFailed)) {
                Logger.logDebugMessage("requestGeoIpTool failed: " + e.getMessage());
            }
        }
    }

    private static void requestGeoIpTool(JSONObject json, String ip) {
        try {
            if (isLanIp(ip)) {
                json.put("X", "0");
                json.put("Y", "0");
            } else {
                Document document = Jsoup.connect("https://geoiptool.com/zh/?ip=" + ip).get();
                Elements el = document.select(".sidebar-data.hidden-xs.hidden-sm .data-item");
                json.put("X", el.eq(8).select("span:nth-child(2)").text());
                json.put("Y", el.eq(9).select("span:nth-child(2)").text());
            }

        } catch (Exception e) {
            if (geoQueryExceptionSwitch
                    && Logger.printNow(Logger.IpUtil_geoTransformFailed)) {
                Logger.logDebugMessage("requestGeoIpTool failed: " + e.getMessage());
            }
        }
    }

    private static boolean isLanIp(String ip) {
        return ip.substring(0, 3).equals("10.") || ip.substring(0, 4).equals("172.") || ip.substring(0, 4).equals("192.");
    }

    public static void main(String[] args) {

    }
}
