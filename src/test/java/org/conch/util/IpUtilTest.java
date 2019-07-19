package org.conch.util;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-03-28
 */
public class IpUtilTest {

    public static void getIpFromUrl(){
        System.out.println(IpUtil.getIpFromUrl("192.168.31.1"));
        System.out.println(IpUtil.getIpFromUrl("devboot.sharder.io"));
        System.out.println(IpUtil.getIpFromUrl("testboot.sharder.io"));
        System.out.println(IpUtil.getIpFromUrl("testboot.sharder.io:7717"));
        System.out.println(IpUtil.getIpFromUrl("devboot.sharder.io"));
        System.out.println(IpUtil.getIpFromUrl("192.168.31.1:8080"));
        System.out.println(IpUtil.getIpFromUrl("devboot.sharder.io:8080"));  
        System.out.println(IpUtil.getIpFromUrl("cn.testnat.sharder.io:8917"));  
    }
    
    public static void getHostFromUrl(){
        System.out.println(IpUtil.getHostFromUrl("192.168.31.1"));
        System.out.println(IpUtil.getHostFromUrl("devboot.sharder.io"));
        System.out.println(IpUtil.getHostFromUrl("testboot.sharder.io"));
        System.out.println(IpUtil.getHostFromUrl("testboot.sharder.io:7717"));
        System.out.println(IpUtil.getHostFromUrl("devboot.sharder.io"));
        System.out.println(IpUtil.getHostFromUrl("192.168.31.1:8080"));
        System.out.println(IpUtil.getHostFromUrl("devboot.sharder.io:8080"));  
        System.out.println(IpUtil.getHostFromUrl("cn.testnat.sharder.io:8917"));  
    }
    
    public static void isDomainTest(){
        System.out.println(IpUtil.isDomain("192.168.31.1"));
        System.out.println(IpUtil.isDomain("devboot.sharder.io"));
        System.out.println(IpUtil.isDomain("192.168.31.1:8080"));
        System.out.println(IpUtil.isDomain("devboot.sharder.io:8080")); 
    }
    
    
    public static void main(String[] args) {
//        isDomainTest();
        System.out.println("---getIpFromUrl--");
        getIpFromUrl();
        System.out.println("--getHostFromUrl---");
        getHostFromUrl();
        System.out.println("-----");
        System.out.println(IpUtil.getNetworkIp());
    }
}
