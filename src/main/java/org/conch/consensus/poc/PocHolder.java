package org.conch.consensus.poc;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.consensus.genesis.GenesisRecipient;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.Peer;
import org.conch.util.IpUtil;
import org.conch.util.Logger;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PocHolder is a singleton to hold the score and reference map.
 * This map stored in the memory, changed by the poc txs.
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-29
 */

public class PocHolder implements Serializable {

    static PocHolder inst = new PocHolder();
    
    // accountId : pocScore
    private Map<Long, PocScore> scoreMap = new ConcurrentHashMap<>();
    
    // height : { accountId : pocScore }
    private Map<Integer, Map<Long, PocScore>> historyScore = Maps.newConcurrentMap();

    // certified miner: foundation node,sharder hub, community node
    // height : <bindAccountId,peer>
    private Map<Integer, Map<Long, Peer>> certifiedMinerPeerMap = Maps.newConcurrentMap();

    // peerType : <bindAccountId,peerHost> # peerHost is public ip or announcedAddress(NatIp+Port) 
    private Map<Peer.Type, Map<Long, String>> certifiedBindAccountMap = Maps.newConcurrentMap();

    // syn peers
    private volatile Set<String> synPeerList = Sets.newHashSet();

    int lastHeight = -1;


    public static Set<String> synPeers() {
        return inst.synPeerList;
    }

    public static void addSynPeer(String host) {
        String ip = IpUtil.checkOrToIp(host);

        if (StringUtils.isEmpty(ip) && StringUtils.isEmpty(host)) return;
        if (inst.synPeerList.contains(ip)) return;

        inst.synPeerList.add(StringUtils.isEmpty(ip) ? host : ip);
    }

    public static void removeConnectedPeers(Set<String> connectedPeers) {
        inst.synPeerList.removeAll(connectedPeers);
    }


    public static Map<Long, String> getBindAccountPeerMap(Peer.Type type) {
        if (!inst.certifiedBindAccountMap.containsKey(type)) {
            inst.certifiedBindAccountMap.put(type, new ConcurrentHashMap<>());
        }
        return inst.certifiedBindAccountMap.get(type);
    }

    public static String getBoundPeerIp(Peer.Type type, long account) {
        return getBindAccountPeerMap(type).get(account);
    }

    public static boolean boundPeer(Peer.Type type, long account) {
        return getBindAccountPeerMap(type).containsKey(account);
    }

    /**
     * record peer and bind account
     *
     * @param type      peer type
     * @param accountId bind account id
     * @param peerHost  peer host
     * @param replace   true: replace the exist account id of peer
     */
    public static void addOrUpdateBoundAccountPeer(Peer.Type type, Long accountId, String peerHost, boolean replace) {
        Map<Long, String> bindAccountMap = getBindAccountPeerMap(type);
//        String peerIp = IpUtil.checkOrToIp(peerHost);
        if (replace && bindAccountMap.containsKey(accountId)) {
            if (!peerHost.equalsIgnoreCase(bindAccountMap.get(accountId))) {
                bindAccountMap.remove(accountId);
            }
        }
        bindAccountMap.put(accountId, peerHost);
    }

    public static Map<Long, Peer> getMinerPeerMap(Integer height) {
        if (!inst.certifiedMinerPeerMap.containsKey(height)) {
            inst.certifiedMinerPeerMap.put(height, new ConcurrentHashMap<>());
        }
        return inst.certifiedMinerPeerMap.get(height);
    }

    public static void addMinerPeer(Integer height, long accountId, Peer peer) {
        getMinerPeerMap(height).put(accountId, peer);
    }


    static {
        initBindMiners();
    }

    private static void initBindMiners(){
        inst.certifiedBindAccountMap.put(Peer.Type.HUB,Maps.newConcurrentMap());
        inst.certifiedBindAccountMap.put(Peer.Type.COMMUNITY,Maps.newConcurrentMap());
        inst.certifiedBindAccountMap.put(Peer.Type.FOUNDATION,Maps.newConcurrentMap());
        
        // genesis account binding
        String bootNodeDomain = Constants.isDevnet() ? "devboot.sharder.io" : Constants.isTestnet() ? "testboot.sharder.io" : "mainboot.sharder.io";
        String ip = IpUtil.checkOrToIp(bootNodeDomain);
        inst.certifiedBindAccountMap.get(Peer.Type.FOUNDATION).put(SharderGenesis.CREATOR_ID,ip);
        GenesisRecipient.getAll().forEach(recipient -> inst.certifiedBindAccountMap.get(Peer.Type.FOUNDATION).put(recipient.id,ip));
    }

    private PocHolder(){}


    private static void _defaultPocScore(long accountId,int height){
        scoreMapping(new PocScore(accountId,height));
    }
    
    
    /**
     * get the poc score and detail of the specified height
     * @param height
     * @param accountId
     * @return
     */
    static JSONObject getPocScore(int height, long accountId) {
        if(height < 0) height = 0;
        JSONObject jsonObject = new JSONObject();
        if (!inst.scoreMap.containsKey(accountId)) {
            PocProcessorImpl.notifySynTxNow();
            _defaultPocScore(accountId,height);
        }
        PocScore pocScoreDetail = inst.scoreMap.get(accountId);
        //newest poc score when query height is bigger than last height of poc score

        if(pocScoreDetail.height > height) {
            //get from history
            pocScoreDetail = getHistoryPocScore(height, accountId);
        }

        if(pocScoreDetail != null) {
            jsonObject.put(PocProcessor.SCORE_KEY,pocScoreDetail.total());
            jsonObject.putAll(pocScoreDetail.toJsonObject());
        }
        return jsonObject;
    }
    
    /**
     * update the poc score of account
     * @param pocScore a poc score object
     */
    public static synchronized void scoreMapping(PocScore pocScore) {
        PocScore _pocScore = pocScore;
        if(inst.scoreMap.containsKey(pocScore.accountId)) {
            _pocScore = inst.scoreMap.get(pocScore.accountId);
            _pocScore.synScoreFrom(pocScore);
            recordHistoryScore(pocScore);
        }

        inst.scoreMap.put(pocScore.accountId,_pocScore);
        inst.lastHeight = pocScore.height > inst.lastHeight ? pocScore.height : inst.lastHeight;
        PocScorePrinter.print();
    }

    static BigInteger getTotal(int height,Long accountId){
        Map<Long,PocScore> map = inst.historyScore.get(height);
        if(map == null) return BigInteger.ZERO;
        PocScore score = map.get(accountId);
        return score !=null ? score.total() : BigInteger.ZERO;
    }
        
    /**
     * record current poc score into history
     */
    static void recordHistoryScore(PocScore pocScore){
        Map<Long,PocScore> map = inst.historyScore.get(pocScore.height);
        if(map == null) map = new HashMap<>();

        map.put(pocScore.accountId,new PocScore(pocScore.height, pocScore));

        inst.historyScore.put(pocScore.height,map);
    }

    static PocScore getHistoryPocScore(int height,long accountId){
        if(!inst.historyScore.containsKey(height)) {
            return null;
        }
        PocScorePrinter.print();
        return inst.historyScore.get(height).get(accountId);
    }

    static PocTxBody.PocWeightTable getPocWeightTable(){
        return PocCalculator.inst.getCurWeightTable();
    }

    /**
     * Poc score map printer
     */
    private static class PocScorePrinter {
        static int count = 0;
        static final int printCount = 100;
        
        protected static boolean debug = Constants.isTestnetOrDevnet()  ? true : false;
        protected static boolean debugHistory = false;
        
        protected static String summary = reset();
        private static final String splitter = "\n\r";

        static private String appendSplitter(String str, boolean appendEnd) {
            str = splitter + str ;
            if(appendEnd) {
                str += splitter;
            }
            return str;
        }
        
        static String reset(){
            String summary = appendSplitter("--------------PocScorePrinter-------------",false);
            count=0;
            return summary;
        }
        
        static String scoreMapStr( Map<Long, PocScore> scoreMap){
            Set<Long> accountIds = scoreMap.keySet();
            for(Long accountId : accountIds){
                PocScore pocScore = scoreMap.get(accountId);
                summary += appendSplitter(Account.rsAccount(pocScore.accountId) + ",poc score=" + pocScore.total() + ":" + pocScore.toJsonString(),false);
            }
            return summary;
        }
        
        static void putin(){
            // accountId : pocScore
            Map<Long, PocScore> scoreMap = new ConcurrentHashMap<>();
            // height : { accountId : pocScore }
            Map<Integer,Map<Long,PocScore>> historyScore = new ConcurrentHashMap<>();

            summary += appendSplitter("PocScore & Height Map[ accountId : PocScore Object ] size=" + inst.scoreMap.size() + " >>>>>>>>",true);
            scoreMapStr(inst.scoreMap);
            summary += appendSplitter("<<<<<<<<<<",true);
            
            if(debugHistory) {
                summary += appendSplitter("PocScore & Height Map[ height : Map{ accountId : PocScore Object } ] size=" + inst.historyScore.size() + " >>>>>>>>",true);
                Set<Integer> heights = inst.historyScore.keySet();

                for(Integer height : heights){
                    summary += "height: " + height +" -> {";
                    scoreMapStr(inst.historyScore.get(height));
                    summary += appendSplitter("}",true);
                }
                summary += appendSplitter("<<<<<<<<<<",true);  
            }
          
        }

        static void print(){
            if(!debug || (count++  <= printCount)) return;
            putin();
            Logger.logDebugMessage(summary);
            summary = reset();
        }
    }

}