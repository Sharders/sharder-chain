package org.conch.consensus.poc;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.genesis.GenesisRecipient;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.db.PocDb;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.mint.Generator;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.tx.Transaction;
import org.conch.util.IpUtil;
import org.conch.util.Logger;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PocHolder is a singleton to hold the score and reference map.
 * This map stored in the memory, changed by the poc txs.
 * 
 * PocHolder is just available for poc package 
 * 
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-29
 */

class PocHolder implements Serializable {
    
    // you can use the key word 'transient' exclude the attribute to persist
    
    static PocHolder inst = new PocHolder();
    
    /** poc score **/
    int lastHeight = -1;
    
    // accountId : pocScore
    private Map<Long, PocScore> scoreMap = new ConcurrentHashMap<>();
   
    // height : { accountId : pocScore }
    private Map<Integer, Map<Long, PocScore>> historyScore = Maps.newConcurrentMap();
    /** poc score **/

    /** certified peers **/
//    // height : [bound account id]
//    private Map<Integer, Set<Long>> heightMinerMap = Maps.newConcurrentMap();

    // certified peer: foundation node,sharder hub/box, community node
    // account id : certified peer
    private Map<Long, CertifiedPeer> certifiedPeers = Maps.newConcurrentMap();
    // height : { accountId : certifiedPeer }
    private Map<Integer, Map<Long,CertifiedPeer>> historyCertifiedPeers = Maps.newConcurrentMap();
    
//    public static final long UN_VERIFIED_ID = -1;
//    // unverified peer
//    private Map<String, CertifiedPeer> unverifiedPeerMap = Maps.newConcurrentMap();
    /** certified peers **/
    
    // syn peers: used by org.conch.consensus.poc.PocProcessorImpl.peerSynThread
    private volatile Set<String> synPeerList = Sets.newHashSet();
    
    private volatile Map<Integer, List<Long>> delayPocTxsByHeight = Maps.newConcurrentMap();
    private static volatile int pocTxHeight = -1;
    
    public static Set<String> synPeers() {
        return inst.synPeerList;
    }
    
    public static boolean resetCertifiedPeers(){
        synchronized (inst.certifiedPeers) {
            inst.certifiedPeers.clear();
        }
        
//        synchronized (inst.unverifiedPeerMap) {
//            inst.unverifiedPeerMap.clear();
//        }
        
        try{
            Peers.synCertifiedPeers(); 
        }catch(Exception e) {
            Logger.logErrorMessage("reset certified peers occur error", e);
        }
        
        return true;
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


    public static CertifiedPeer getBoundPeer(long accountId, int height) {
        if(height == -1 || accountId == -1) return null;
        CertifiedPeer certifiedPeer = inst.certifiedPeers.get(accountId);
        if(certifiedPeer == null) {
            certifiedPeer = getHistoryBoundPeer(height, accountId);
        }
        
        return certifiedPeer;
    }
    public static CertifiedPeer getBoundPeer(String host, int height) {
        if(height == -1 || StringUtils.isEmpty(host)) return null;
        
        CertifiedPeer certifiedPeer = null;
        Collection<CertifiedPeer> peers = inst.certifiedPeers.values();
        for(CertifiedPeer peer : peers){
            if(StringUtils.equals(host,peer.getHost())){
                certifiedPeer = peer;
                break;
            }
        }
        
        if(certifiedPeer == null) {
            certifiedPeer = getHistoryBoundPeer(height, host);
        }
        
        return certifiedPeer;
    }

    public static boolean isBoundPeer(Peer.Type type, long account) {
        CertifiedPeer certifiedPeer = inst.certifiedPeers.get(account);
        return certifiedPeer == null ? false : certifiedPeer.isType(type);
    }

    
    private static CertifiedPeer getHistoryBoundPeer(int height, long accountId){
        NavigableSet<Integer> heightSet = Sets.newTreeSet(inst.historyCertifiedPeers.keySet()).descendingSet();
        for(Integer historyHeight : heightSet) {
            if(historyHeight <= height) {
                Map<Long,CertifiedPeer> peerMap = inst.historyCertifiedPeers.get(historyHeight);
                if(peerMap != null && peerMap.containsKey(accountId)) {
                    return  peerMap.get(accountId);
                }
            }
        }
        return null;
    }
    
    private static CertifiedPeer getHistoryBoundPeer(int height, String host){
        NavigableSet<Integer> heightSet = Sets.newTreeSet(inst.historyCertifiedPeers.keySet()).descendingSet();
        for(Integer historyHeight : heightSet) {
            if(historyHeight <= height) {
                Map<Long, CertifiedPeer> peerMap = inst.historyCertifiedPeers.get(historyHeight);
                Collection<CertifiedPeer> peers = peerMap.values();
                for(CertifiedPeer certifiedPeer : peers){
                    if(StringUtils.equals(host,certifiedPeer.getHost())){
                        return certifiedPeer;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * add or update certified peer and bind account
     * 3 callers: PocHolder, PoC tx processor, Hub syn thread in Peers
     * 
     * @param type peer type
     * @param host peer host
     * @param accountId bound account id
     */
    private synchronized static void addOrUpdateBoundPeer(int height, Peer.Type type, String host, long accountId) {
        CertifiedPeer newPeer = new CertifiedPeer(height, type, host, accountId);
        try{
            newPeer.check();
        }catch(ConchException.NotValidException e){
            Logger.logWarningMessage("failed to add or update a certified peer caused by [%s], Peer is %s", e.getMessage(), newPeer.toString());
            return;
        }

//        // remove from unverified collection and add it into certified map when account id updated
//        if(inst.unverifiedPeerMap.containsKey(host) && accountId != UN_VERIFIED_ID) {
//            // remove form unverified map and update peer detail later
//            inst.certifiedPeers.put(accountId, inst.unverifiedPeerMap.get(host));
//            inst.unverifiedPeerMap.remove(host);
//        }
        
//        // peer type check:  foundation type should check the domain whether valid
//        if(Peer.Type.FOUNDATION == type) {
//            if(IpUtil.isFoundationDomain(newPeer.getHost())){
//                newPeer.setType(type);
//            }
//        }else {
//            newPeer.setType(type);
//        }
        
        if (!inst.certifiedPeers.containsKey(accountId)) {
            Logger.logDebugMessage("#addOrUpdateBoundPeer# add a new certified peer: %s", newPeer.toString());
            inst.certifiedPeers.put(accountId, newPeer);
            return;
        }

        CertifiedPeer existPeer = inst.certifiedPeers.get(accountId);
        
        
        //TODO foundation type should check whether the domain is a valid foundation domain
        if(Peer.Type.FOUNDATION == type) {
            if(IpUtil.isFoundationDomain(newPeer.getHost())) {
                existPeer.update(type);
            }
        }else {
            if(Peer.Type.FOUNDATION == existPeer.getType()
                    && type.getCode() > existPeer.getType().getCode()
                    && !IpUtil.isFoundationDomain(newPeer.getHost())){
                // foundation node -> other type
                existPeer.update(type);
            }else {
                existPeer.update(type);
            }
        }
        
        // move the old certified peer into history map
        int peerCertifiedHeight = existPeer.getHeight();
        int historyHeight = height - 1;
        if(peerCertifiedHeight == -1 || historyHeight <= peerCertifiedHeight) {
            Logger.logWarningMessage("#addOrUpdateBoundPeer# can't update this certified peer which certified height is %d and update height is %d. Old=> %s, New=> %s"
                    , peerCertifiedHeight, height, existPeer.toString() ,newPeer.toString());
            inst.certifiedPeers.put(accountId, newPeer);
            return;
        }
        if(!inst.historyCertifiedPeers.containsKey(historyHeight)) {
            inst.historyCertifiedPeers.put(historyHeight, Maps.newHashMap());
        }
        
        try{
            existPeer.end(historyHeight); 
        }catch(ConchException.NotValidException e){
            Logger.logWarningMessage("failed to update a certified peer caused by [%s], Peer is %s", e.getMessage(), newPeer.toString());
            return;
        }
      
        inst.historyCertifiedPeers.get(historyHeight).put(existPeer.getBoundAccountId(), existPeer);
        inst.certifiedPeers.remove(accountId);
        
        // add the new certified peer into certifiedPeers map
        inst.certifiedPeers.put(accountId, newPeer);
                
        existPeer.update(newPeer.getBoundAccountId());
        
        if(type == null) {
            Logger.logDebugMessage("#addOrUpdateBoundPeer# update a certified peer: %s", existPeer.toString());
            return;
        }
        
        Logger.logDebugMessage("#addOrUpdateBoundPeer# update a certified peer: %s", existPeer.toString());
    }
    
//    private synchronized static void updateHeightMinerMap(int height, long accountId){
//        // height mapping
//        if (!inst.heightMinerMap.containsKey(height)) {
//            inst.heightMinerMap.put(height, Sets.newHashSet());
//        }
//        inst.heightMinerMap.get(height).add(accountId);
//    }

    
//    public static void updateBoundPeer(String host, long accountId) {
//        addOrUpdateBoundPeer(null, host, accountId);
//    }
    
    /**
     *  add certifiedPeer 
     *  
     * @param height
     * @param type
     * @param host
     * @param accountId
     */
    public static void addCertifiedPeer(int height, Peer.Type type, String host, long accountId) {
        addOrUpdateBoundPeer(height, type, host, accountId);
//        updateHeightMinerMap(height, accountId);
    }
    

//    /**
//     * add certifiedPeer by poc tx
//     *
//     * @param height
//     * @param peer
//     */
//    public static void addCertifiedPeer(Integer height, Peer peer) {
//        String rsAccount = peer.getBindRsAccount();
//        long accountId = StringUtils.isEmpty(rsAccount) ? PocHolder.UN_VERIFIED_ID : Account.rsAccountToId(rsAccount);
//        String host = peer.getAddress();
//            
//        if(StringUtils.isEmpty(rsAccount)) {
//            inst.unverifiedPeerMap.put(host, new CertifiedPeer(height, peer.getType(), host, accountId));
//        }else {
//            addOrUpdateBoundPeer(peer.getType(), host, accountId);
//        }
//        
////        updateHeightMinerMap(height, accountId);
//    }

    public static int countDelayPocTxs(int queryHeight) {
        int count = 0;
        //order by height number 
        for(int i = 0 ; i <= queryHeight ; i++) {
            if(inst.delayPocTxsByHeight.containsKey(i)) {
                count += inst.delayPocTxsByHeight.get(i).size();
            }
        }
        return count;
    }


    public static List<Long> delayPocTxs(int queryHeight) {
        List<Long> txs = Lists.newArrayList();
        //order by height number 
        for(int i = 0 ; i <= queryHeight ; i++) {
            if(inst.delayPocTxsByHeight.containsKey(i)) {
                txs.addAll(inst.delayPocTxsByHeight.get(i));
            }
        }
        return txs;
    }

    public static void addDelayPocTx(Transaction tx) {
        synchronized (inst.delayPocTxsByHeight) {
            if(!inst.delayPocTxsByHeight.containsKey(tx.getHeight())) {
                inst.delayPocTxsByHeight.put(tx.getHeight(), Lists.newArrayList());
            }
            List<Long> txIds = inst.delayPocTxsByHeight.get(tx.getHeight());
            if(!txIds.contains(tx.getId())) txIds.add(tx.getId());
        }
    }

    public static void removeProcessedTxs(Set<Long> processedTxs) {
        synchronized (inst.delayPocTxsByHeight) {
            Set<Integer> heights = inst.delayPocTxsByHeight.keySet();
            heights.forEach(height -> {
                inst.delayPocTxsByHeight.get(height).removeAll(processedTxs);
                if(height > inst.pocTxHeight) inst.pocTxHeight = height;
            });
        }
    }
    
    static {
        initDefaultMiners();
    }
    
    private static final boolean initDefaultMiner = false;
    private static void initDefaultMiners(){
        if(!initDefaultMiner) return;
        // genesis account binding
        String bootNodeDomain = Constants.isDevnet() ? "devboot.sharder.io" : Constants.isTestnet() ? "testboot.sharder.io" : "mainboot.sharder.io";
        addCertifiedPeer(0, Peer.Type.FOUNDATION, bootNodeDomain, SharderGenesis.CREATOR_ID);
        GenesisRecipient.getAll().forEach(recipient -> addCertifiedPeer(0, Peer.Type.FOUNDATION, bootNodeDomain, recipient.id));
    }

    private PocHolder(){}

    /**
     * get the poc score and detail of the specified height
     * @param height
     * @param accountId
     * @return
     */
    static PocScore getPocScore(int height, long accountId) {
        if(height < 0) height = 0;

        //new a default PocScore
        if (!inst.scoreMap.containsKey(accountId)) {
//            PocScore pocScore = PocDb.getPocScore(accountId, height, true);
            PocScore pocScore = getHistoryPocScore(height, accountId);
            if(pocScore == null) pocScore = new PocScore(accountId,height);
            
            scoreMapping(pocScore, false);
        }
        
        PocScore pocScoreDetail = inst.scoreMap.get(accountId);
        
//        //get history poc score when query height is bigger than last height of poc score
//        if(pocScoreDetail.height > height) {
//            PocScore historyScore = getHistoryPocScore(height, accountId);
//            if(historyScore != null) {
//                pocScoreDetail = historyScore;
//            }
//        }

        return pocScoreDetail;
    }

    /**
     * - mapping the poc score of account
     * - persistence
     * @param pocScore a poc score object
     */
    public static synchronized void scoreMappingAndPersist(PocScore pocScore){
        scoreMapping(pocScore, true);
    }

    /**
     * - mapping the poc score of account
     * @param pocScore a poc score object
     */
    public static synchronized void scoreMapping(PocScore pocScore){
        scoreMapping(pocScore, false);
    }
    
    /**
     * - mapping the poc score of account
     * - persistence
     * @param pocScore a poc score object
     * @param updateDB save or update db
     */
    private static synchronized void scoreMapping(PocScore pocScore, boolean updateDB) {
        PocScore _pocScore = pocScore;
        if(inst.scoreMap.containsKey(pocScore.accountId)) {
            _pocScore = inst.scoreMap.get(pocScore.accountId);
            _pocScore.synFrom(pocScore);
            recordHistoryScore(pocScore, updateDB);
        }
        
        if(updateDB) {
            PocDb.saveOrUpdate(_pocScore);
        }
       
        inst.scoreMap.put(pocScore.accountId,_pocScore);
        inst.lastHeight = pocScore.height > inst.lastHeight ? pocScore.height : inst.lastHeight;
        
        //TODO use the event to notify (there will have many consumers later): define an event 'POC_SCORE_CHANGED' 
        // to notify the all listeners: Generator and so on
        Generator.updatePocScore(_pocScore);
        
        PocScorePrinter.print();
    }

    static BigInteger getTotal(int height,Long accountId){
        Map<Long,PocScore> map = inst.historyScore.get(height);
        if(map == null) return BigInteger.ZERO;
        PocScore score = map.get(accountId);
        return score !=null ? score.total() : BigInteger.ZERO;
    }

    /**
     * - record current poc score into history
     * - persistence
     * - update old poc score records #abandoned
     * @param pocScore
     */
    static void recordHistoryScore(PocScore pocScore, boolean updateDB){
        PocScore historyPocScore = new PocScore(pocScore.height, pocScore);
        if(updateDB) PocDb.saveOrUpdate(historyPocScore);

        Map<Long,PocScore> map = inst.historyScore.get(pocScore.height);
        if(map == null) map = new HashMap<>();
        map.put(pocScore.accountId, historyPocScore);
        inst.historyScore.put(pocScore.height,map);
        
//        //check and update the old poc score
//        int currentHeight = Conch.getBlockchain().getHeight();
//        int fromHeight = pocScore.height < currentHeight ? pocScore.height : currentHeight;
//        for(int i = fromHeight; i <= currentHeight ; i++) {
//            try{
//                if(!inst.historyScore.containsKey(i)) continue;
//
//                Map<Long,PocScore> heightScoreMap = inst.historyScore.get(i);
//                if(heightScoreMap.containsKey(pocScore.accountId)) {
//                    heightScoreMap.get(pocScore.accountId).synFromExceptSSHold(pocScore);
//                } 
//            }catch(Exception e) {
//                //ignore to process next
//            }
//        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


    /**
     * get the poc score according to specified height
     * @param height
     * @param accountId
     * @return
     */
    static PocScore getHistoryPocScore(int height,long accountId){
        if(!inst.historyScore.containsKey(height)) {
            PocScore pocScore = PocDb.getPocScore(accountId, height, true);
            
            if(pocScore == null) return null;

            recordHistoryScore(pocScore, false);
        }
        PocScorePrinter.print();
        return inst.historyScore.get(height).get(accountId);
    }

    static PocTxBody.PocWeightTable getPocWeightTable(){
        return PocCalculator.inst.getCurWeightTable();
    }

    /**
     * open the poc persistence
     */
    static void openPocPersist(){
        
    }
    

    /**
     * Poc score map printer
     */
    private static class PocScorePrinter {
        static int count = 0;
        static final int printCount = 1;
        
        protected static boolean debug = Constants.isTestnetOrDevnet()  ? true : false;
        protected static boolean debugHistory = Constants.isDevnet() ? true : false;
        
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

            summary += appendSplitter("PocScore & Height Map[ accountId : PocScore Object ] height=" + Conch.getBlockchain().getHeight() + ", size=" + inst.scoreMap.size() + " >>>>>>>>",true);
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
            if(!debug || (debug && (count++  <= printCount)) ) return;
            putin();
            Logger.logDebugMessage(summary);
            summary = reset();
        }
    }

    public static void main(String[] args) {
        System.out.println(Peer.BlockchainState.UP_TO_DATE);
    }

}