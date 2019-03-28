package org.conch.util;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/18
 */
public class IpUtil {
    
    public static String domain2Ip(String url){
        try {
            if(!isDomain(url)) return url;
            return InetAddress.getByName(url).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getHost(String url){
        try {
            return InetAddress.getByName(url).getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getSenderIp(HttpServletRequest req) {
        return req.getHeader("x-forwarded-for") == null ? req.getRemoteAddr() : req.getHeader("x-forwarded-for");
    }

    public static boolean matchHost(HttpServletRequest req, String host) {
        String senderIp = getSenderIp(req);
        return senderIp.equals(domain2Ip(host));
    }
    
    
    public static String getNetworkIp() throws SocketException {
        String localip = null;// 本地IP，如果没有配置外网IP则返回它
        String netip = null;// 外网IP

        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        InetAddress ip = null;
        boolean finded = false;// 是否找到外网IP
        while (netInterfaces.hasMoreElements() && !finded) {
            NetworkInterface ni = netInterfaces.nextElement();
            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                ip = address.nextElement();
                if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":") == -1) {// 外网IP
                    netip = ip.getHostAddress();
                    finded = true;
                    break;
                } else if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
                        && ip.getHostAddress().indexOf(":") == -1) {// 内网IP
                    localip = ip.getHostAddress();
                }
            }
        }

        if (netip != null && !"".equals(netip)) {
            return netip;
        } else {
            return localip;
        }
    }

    /* domain pattern */
    public static String PATTERN_L2DOMAIN = "\\w*\\.\\w*:";
//    public static String PATTERN_IP = "(\\d*\\.){3}\\d*";
    public static String PATTERN_IP = "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";
    
    public static boolean isDomain(String url){
        Pattern ipPattern = Pattern.compile(PATTERN_IP);
        Matcher matcher = ipPattern.matcher(url);
        if (matcher.find()) {
            return false;
        }
        return true;
    }
    
    public static String getIpFromUrl(String url) {
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

}
