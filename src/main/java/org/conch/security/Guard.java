package org.conch.security;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.Logger;

import java.util.Map;

/**
 * Used to guard the client to avoid the viciously tcp/ip connect,
 * api request and others resources consumption
 */
public class Guard {

    //TODO time based block list
    // black peer validation in : org.conch.peer.PeerServlet.process
    private static final int EXPIRED_TIME = 4 * (60 * 60 * 1000); //4hours
    private static final String FIRST_ACCESS_TIME_KEY = "firstAccessTime";
    private static final String LAST_ACCESS_TIME_KEY = "lastAccessTime";
    private static final String ACCESS_COUNT_KEY = "accessCount";
    private static Map<String, JSONObject> BLACK_PEERS_MAP = Maps.newConcurrentMap();
    private static final int MAX_VICIOUS_COUNT_PER_SAME_HOST = 50;

    public static void viciousAccess(String host){
        if("127.0.0.1".equals(host)
        || "localhost".equals(host)){
            // don't guard the local request
            return;
        }

        JSONObject accessPeerObj = BLACK_PEERS_MAP.get(host);
        if (accessPeerObj == null) {
            accessPeerObj = new JSONObject();
            accessPeerObj.put(FIRST_ACCESS_TIME_KEY, System.currentTimeMillis());
            accessPeerObj.put(ACCESS_COUNT_KEY, 1);
        }else {
            accessPeerObj.put(LAST_ACCESS_TIME_KEY, System.currentTimeMillis());
            accessPeerObj.put(ACCESS_COUNT_KEY, accessPeerObj.getIntValue(ACCESS_COUNT_KEY) + 1);
        }
//        else if (accessPeerObj.getLong(ACCESS_TIME_KEY) + EXPIRED_TIME > System.currentTimeMillis()) {
//            return accessPeerObj;
//        }

        if(accessPeerObj.getIntValue(ACCESS_COUNT_KEY) >= MAX_VICIOUS_COUNT_PER_SAME_HOST){
            blackPeer(host, String.format("Exceed the vicious access max count %d", MAX_VICIOUS_COUNT_PER_SAME_HOST));
        }
    }

    public static boolean blackPeer(String host,String cause){
        Peer peer = Peers.getPeer(host, true);
        if(peer == null) {
            rejectPeer(host);
        }else{
            peer.blacklist(String.format("Black the peer %s[%s] caused by %s", peer.getAnnouncedAddress(), peer.getHost(), cause));
        }
        return true;
    }

    public static boolean rejectPeer(String host){
        addRejectRuleIntoFirewall(host);
        return true;
    }

    /**
     * call the shell to add reject rule into firewall of OS
     * - just support the CentOS and firewalld
     */
    private static void addRejectRuleIntoFirewall(String host){
        Logger.logInfoMessage("Not implement addRejectRuleIntoFirewall now");
    }
}
