package org.conch.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import sun.net.util.IPAddressUtil;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            if(StringUtils.isEmpty(url) 
            || "undefined".equalsIgnoreCase(url)){
                return "";    
            }
            
            if(!isDomain(url)) return url;
            return InetAddress.getByName(url).getHostAddress();

        } catch (UnknownHostException e) {
            Logger.logErrorMessage("can't finish checkOrToIp with url[" + url + "] caused by " + e.getMessage());
        }
        return "";
    }
    
    private final static List<String> natServers = Lists.newArrayList(
            "nat.sharder.network",
            "nat.sharder.org",
            "nat.sharder.io");
    
    private static String checkOrParseNatUrl(String url){
        if(StringUtils.isEmpty(url)) return "";
        for(String natServer : natServers) {
            if(!url.contains(natServer)) continue;
            
            // get domain
            int portIndex = url.indexOf(":");
            if(portIndex > 0) {
               return url.substring(0,portIndex);
            }
        }
        return "";
    }

    /**
     * @param url can't include the port
     */
    public static String getHost(String url){
        if(StringUtils.isEmpty(url)) return "";
        
        String parsedUrl = checkOrParseNatUrl(url);
        if(StringUtils.isNotBlank(parsedUrl)) return parsedUrl;
        
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
        if(StringUtils.isEmpty(ip)) return false;
        return ip.equals(checkOrToIp(host));
    }
    
    public static boolean isFoundationDomain(String host){
        if(StringUtils.isEmpty(host)) return false;
        
        if(host.endsWith("sharder.io") 
        || host.endsWith("sharder.org")
        || host.endsWith("sharder.network")){
            return true;
        }
        
        return false;
    }

    /**
     * ip addr whether is the internal ip
     * @param ipv4Addr
     * @return
     */
    public static boolean isInternalIp(String ipv4Addr){
        return _isInternalIp(IPAddressUtil.textToNumericFormatV4(ipv4Addr));
    }

    /**
     * ipv4
     * tcp/ip协议中，专门保留了三个IP地址区域作为私有地址，其地址范围如下：
     * 10.0.0.0/8：10.0.0.0～10.255.255.255 
     * 172.16.0.0/12：172.16.0.0～172.31.255.255 
     * 192.168.0.0/16：192.168.0.0～192.168.255.255
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
     * @return
     */
    public static String getNetworkIp() {
        try{
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
        }catch (Exception e) {
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
     * @param url 
     * @return
     */
    public static boolean isDomain(String url){
        if(StringUtils.isEmpty(url)) return false;
        
        Pattern ipPattern = Pattern.compile(PATTERN_IP);
        Matcher matcher = ipPattern.matcher(url);
        return !matcher.find();
    }

    /**
     * get host from url
     * @param url domain ot url contains port
     * @return host: domain or public ip
     */
    public static String getHostFromUrl(String url) {
        if(isDomain(url) 
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
     * @param url domain ot url contains port
     * @return public network ip
     */
    public static String getIpFromUrl(String url) {
        String host = getHostFromUrl(url);
        return checkOrToIp(host);
    }
}
