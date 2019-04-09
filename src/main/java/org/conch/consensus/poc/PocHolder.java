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
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.tx.Transaction;
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
   
    // height : [bound account id]
    private Map<Integer, Set<Long>> heightMinerMap = Maps.newConcurrentMap();

    // certified peer: foundation node,sharder hub/box, community node
    // account id : certified peer
    private Map<Long, CertifiedPeer> certifiedPeerMap = Maps.newConcurrentMap();
    
    public static final long UN_VERIFIED_ID = -1;
    
    // unverified peer
    private Map<String, CertifiedPeer> unverifiedPeerMap = Maps.newConcurrentMap();
    
    // syn peers: used by org.conch.consensus.poc.PocProcessorImpl.peerSynThread
    private volatile Set<String> synPeerList = Sets.newHashSet();
    
    private volatile Set<Long> delayProcessTxs = Sets.newHashSet();

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


    public static CertifiedPeer getBoundPeer(long account) {
        return inst.certifiedPeerMap.get(account);
    }

    public static boolean isBoundPeer(Peer.Type type, long account) {
        CertifiedPeer certifiedPeer = inst.certifiedPeerMap.get(account);
        return certifiedPeer == null ? false : certifiedPeer.isType(type);
    }


    /**
     * record certified peer and bind account
     *
     */
    public static void addOrUpdateBoundPeer(Peer.Type type, String host, long accountId) {
        CertifiedPeer newPeer = new CertifiedPeer(type, host, accountId);
        
        // remove from unverified collection and add it into certified map when account id updated
        if(inst.unverifiedPeerMap.containsKey(host) && accountId > UN_VERIFIED_ID) {
            inst.certifiedPeerMap.put(accountId, inst.unverifiedPeerMap.get(host));
            inst.unverifiedPeerMap.remove(host);
        }
        
        // update exist peer infos
        if (inst.certifiedPeerMap.containsKey(newPeer.getBoundAccountId())) {
            inst.certifiedPeerMap.get(newPeer.getBoundAccountId()).update(newPeer.getBoundAccountId()).update(newPeer.getType());
        } else {
            inst.certifiedPeerMap.put(newPeer.getBoundAccountId(), newPeer);
        }
    }
    
    
    private static void updateHeightMinerMap(int height, long accountId){
        // height mapping
        if (!inst.heightMinerMap.containsKey(height)) {
            inst.heightMinerMap.put(height, Sets.newHashSet());
        }
        inst.heightMinerMap.get(height).add(accountId);
    }

    /**
     *  add certifiedPeer 
     *  
     * @param height
     * @param type
     * @param host
     * @param accountId
     */
    public static void addCertifiedPeer(int height, Peer.Type type, String host, long accountId) {
        addOrUpdateBoundPeer(type, host, accountId);
        updateHeightMinerMap(height, accountId);
    }
    

    /**
     * add certifiedPeer 
     *
     * @param height
     * @param peer
     */
    public static void addCertifiedPeer(Integer height, Peer peer) {
        String rsAccount = peer.getBindRsAccount();
        long accountId = StringUtils.isEmpty(rsAccount) ? PocHolder.UN_VERIFIED_ID : Account.rsAccountToId(rsAccount);
     
        if(StringUtils.isEmpty(rsAccount)) {
            inst.unverifiedPeerMap.put(peer.getHost(),new CertifiedPeer(height, peer, accountId));
        }else {
            addOrUpdateBoundPeer(peer.getType(), peer.getHost(), accountId);
        }

        updateHeightMinerMap(height, accountId);
    }


    public static Set<Long> delayPocTxs() {
       return inst.delayProcessTxs;
    }

    public static void addDelayProcessTx(Long txid) {
       inst.delayProcessTxs.add(txid);
    }

    public static void removeProcessedTxs(Set<Long> processedTxs) {
        inst.delayProcessTxs.removeAll(processedTxs);
    }
    

    static {
        initDefaultMiners();
    }

    private static void initDefaultMiners(){
        // genesis account binding
        String bootNodeDomain = Constants.isDevnet() ? "devboot.sharder.io" : Constants.isTestnet() ? "testboot.sharder.io" : "mainboot.sharder.io";
        addCertifiedPeer(0, Peer.Type.FOUNDATION, bootNodeDomain, SharderGenesis.CREATOR_ID);
        GenesisRecipient.getAll().forEach(recipient -> addCertifiedPeer(0, Peer.Type.FOUNDATION, bootNodeDomain, recipient.id));
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
            _defaultPocScore(accountId, height);
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