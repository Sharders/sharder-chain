package org.conch.consensus.poc;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.consensus.genesis.GenesisRecipient;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.util.Logger;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PocHolder is a singleton to hold the score and reference map.
 * This map stored in the memory, changed by the poc txs.
 *
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
    private Map<Integer, Set<Long>> certifiedMinerPeerMap = Maps.newConcurrentMap();

//    // peerType : <bindAccountId,peerHost> # peerHost is public ip or announcedAddress(NatIp+Port) 
//    private Map<Peer.Type, Set<Long>> certifiedBindAccountMap = Maps.newConcurrentMap();
    
    private Map<Long, CertifiedPeer> certifiedPeerMap = Maps.newConcurrentMap();
    
    public static final long UN_VERIFIED_ID = -1;
    
    private Map<String, CertifiedPeer> unverifiedCertifiedPeers = Maps.newConcurrentMap();
    
    // syn peers: used by org.conch.consensus.poc.PocProcessorImpl.peerSynThread
    private volatile Set<String> synPeerList = Sets.newHashSet();

    int lastHeight = -1;

    public static Set<String> synPeers() {
        return inst.synPeerList;
    }

    /**
     * add host into syn peer list
     *
     * @param host
     */
    public static void addSynPeer(String host) {
        if (StringUtils.isEmpty(host)) return;

        inst.synPeerList.add(host);
    }

    public static void removeConnectedPeers(Set<String> connectedPeers) {
        inst.synPeerList.removeAll(connectedPeers);
    }

//    public static Set<Long> getBindAccountPeerMap(Peer.Type type) {
//        if (!inst.certifiedBindAccountMap.containsKey(type)) {
//            inst.certifiedBindAccountMap.put(type, Sets.newHashSet());
//        }
//        return inst.certifiedBindAccountMap.get(type);
//    }

    public static CertifiedPeer getBoundPeer(long account) {
        return inst.certifiedPeerMap.get(account);
    }

    public static boolean isBoundPeer(Peer.Type type, long account) {
        CertifiedPeer certifiedPeer = inst.certifiedPeerMap.get(account);
        return certifiedPeer == null ? false : certifiedPeer.isType(type);
    }

//    /**
//     * record certified peer and bind account
//     * default action: update the exist certifiedPeer information
//     *
//     * @param type      peer type
//     * @param accountId bind account id
//     * @param peerHost  peer host
//     */
//    public static void addOrUpdateBoundAccountPeer(Peer.Type type, Long accountId, String peerHost) {
//        addOrUpdateBoundAccountPeer(new CertifiedPeer(type, peerHost, accountId));
//    }

    /**
     * record certified peer and bind account
     *
     */
    public static void addOrUpdateBoundPeer(Peer.Type type, String host, long accountId) {
        CertifiedPeer newPeer = new CertifiedPeer(type, host, accountId);
        
        
        //TODO compare unverified map fisrtly
        
        if (inst.certifiedPeerMap.containsKey(newPeer.getBoundAccountId())) {
            inst.certifiedPeerMap.get(newPeer.getBoundAccountId()).update(newPeer.getBoundAccountId()).update(newPeer.getType());
        } else {
            inst.certifiedPeerMap.put(newPeer.getBoundAccountId(), newPeer);
        }
    }

    private static Set<Long> getMinerPeerMap(Integer height) {
        if (!inst.certifiedMinerPeerMap.containsKey(height)) {
            inst.certifiedMinerPeerMap.put(height, Sets.newHashSet());
        }
        return inst.certifiedMinerPeerMap.get(height);
    }

    /**
     * add or update certifiedPeer and append it into 2 maps
     *
     * @param height
     * @param rsAccount
     * @param peer
     */
    public static void addMinerPeer(Integer height, String rsAccount, Peer peer) {
        long accountId = StringUtils.isEmpty(rsAccount) ? PocHolder.UN_VERIFIED_ID : Account.rsAccountToId(rsAccount);
     
        if(StringUtils.isEmpty(rsAccount)) {
            inst.unverifiedCertifiedPeers.put(peer.getHost(),new CertifiedPeer(height, peer, accountId));
        }else {
            addOrUpdateBoundPeer(peer.getType(), peer.getHost(), accountId);
        }
    
        
//        getMinerPeerMap(height).put(accountId, certifiedPeer);
        
//        // type mapping
//        if (!inst.certifiedBindAccountMap.containsKey(certifiedPeer.getType())) {
//            inst.certifiedBindAccountMap.put(certifiedPeer.getType(), Sets.newHashSet());
//        }
        
        // height mapping
        if (!inst.certifiedMinerPeerMap.containsKey(height)) {
            inst.certifiedMinerPeerMap.put(height, Sets.newHashSet());
        }
        
       
    }

    static {
        initBindMiners();
    }

    private static void initBindMiners(){
        // genesis account binding
        String bootNodeDomain = Constants.isDevnet() ? "devboot.sharder.io" : Constants.isTestnet() ? "testboot.sharder.io" : "mainboot.sharder.io";
        inst.addOrUpdateBoundPeer(Peer.Type.FOUNDATION, bootNodeDomain, SharderGenesis.CREATOR_ID);
        GenesisRecipient.getAll().forEach(recipient -> inst.addOrUpdateBoundPeer(Peer.Type.FOUNDATION, bootNodeDomain, recipient.id));
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