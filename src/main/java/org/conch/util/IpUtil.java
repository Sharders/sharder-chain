package org.conch.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/18
 */
public class IpUtil {
    
    public static String getIp(String url){
        try {
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
    
}
