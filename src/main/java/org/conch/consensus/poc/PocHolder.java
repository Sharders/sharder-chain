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
import org.conch.consensus.poc.db.PocDb;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.mint.Generator;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.tx.Transaction;
import org.conch.util.IpUtil;
import org.conch.util.Logger;

import java.io.Serializable;
import java.util.*;

/**
 * PocHolder is a singleton to hold the score and reference map.
 * This map stored in the memory, changed by the poc txs.
 * 
 * PocHolder is just available for poc package 
 * 
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-29
 */

public class PocHolder implements Serializable {
    
    // you can use the key word 'transient' exclude the attribute to persist
    
    static PocHolder inst = new PocHolder();

    int lastHeight = -1;
    
    /** poc score **/
    // accountId : pocScore
    protected transient Map<Long, PocScore> scoreMap = PocDb.listAll();
   
    // height : { accountId : pocScore }
    protected transient Map<Integer, Map<Long, PocScore>> historyScore = Maps.newConcurrentMap();
    /** poc score **/

    /** certified peers **/
    // certified peer: foundation node,sharder hub/box, community node
    // account id : certified peer
    protected Map<Long, CertifiedPeer> certifiedPeers = Maps.newConcurrentMap();
    // height : { accountId : certifiedPeer }
    protected Map<Integer, Map<Long,CertifiedPeer>> historyCertifiedPeers = Maps.newConcurrentMap();
    /** certified peers **/
    
    
    private volatile Map<Integer, List<Long>> delayPocTxsByHeight = Maps.newConcurrentMap();
    private static volatile int pocTxHeight = -1;
    
    
    public static void updateHeight(int height){
        if(height == -1) return;
        
        inst.lastHeight = height;
    }
    
    public static boolean resetCertifiedPeers(){
        synchronized (inst.certifiedPeers) {
            inst.certifiedPeers.clear();
        }
        
        return true;
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

        if (!inst.certifiedPeers.containsKey(accountId)) {
            Logger.logDebugMessage("#addOrUpdateBoundPeer# add a new certified peer: %s", newPeer.toString());
            inst.certifiedPeers.put(accountId, newPeer);
            return;
        }

        CertifiedPeer existPeer = inst.certifiedPeers.get(accountId);
        
        
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
            Logger.logDebugMessage("#addOrUpdateBoundPeer# can't update this certified peer which certified height is %d and update height is %d. Old=> %s, New=> %s"
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

        // Logger.logDebugMessage("#addOrUpdateBoundPeer# update a certified peer: %s", existPeer.toString());
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
        addOrUpdateBoundPeer(height, type, host, accountId);
    }

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
    
//    static {
//        synchronized (inst.scoreMap){
//            inst.scoreMap = PocDb.listAll();
//        }
//    }
    
    private PocHolder(){}


    static PocScore saveOrUpdate(PocScore pocScore) {
        PocDb.saveOrUpdate(pocScore);
        
        PocScore pocScoreDetail = inst.scoreMap.get(pocScore.accountId);

        if(pocScoreDetail == null
                || pocScore.height >= pocScoreDetail.height) {
            inst.scoreMap.put(pocScore.accountId, pocScore);
        }

        return pocScore;
    }
    
    /**
     * get the poc score and detail of the specified height
     * @param height
     * @param accountId
     * @return
     */
    public static PocScore getPocScore(int height, long accountId) {
        if(height < 0) height = 0;

        PocScore pocScore = inst.scoreMap.containsKey(accountId) ? inst.scoreMap.get(accountId) : null;
        PocScore existedScore = getExistedPocScore(height, accountId);
        // current poc score and existed poc score compare
        if(pocScore == null) {
            pocScore = existedScore;
        }else if (existedScore != null) {
            if(Conch.getHeight() >= existedScore.height && existedScore.height > pocScore.height) {
                inst.scoreMap.put(accountId, existedScore);
                pocScore = existedScore;
            }else if(pocScore.height > Conch.getHeight() && Conch.getHeight() >= existedScore.height){
                inst.scoreMap.put(accountId, existedScore);
                pocScore = existedScore;
            }
        }
        
        if(pocScore == null) {
            pocScore = new PocScore(accountId,height);
            scoreMapping(pocScore);
        }
       
        return pocScore;
    }
    
    /**
     * - mapping the poc score of account
     * - persistence
     * @param pocScore a poc score object
     */
    private static synchronized void scoreMapping(PocScore pocScore) {
        PocScore _pocScore = pocScore;
        if(inst.scoreMap.containsKey(pocScore.accountId)) {
            _pocScore = inst.scoreMap.get(pocScore.accountId);
            _pocScore.synFrom(pocScore);
            
            if(!inst.historyScore.containsKey(pocScore.height)) {
                recordHistoryScore(_pocScore);
            }
        }
        
        PocDb.saveOrUpdate(_pocScore);
      
        inst.scoreMap.put(pocScore.accountId,_pocScore);
//        inst.lastHeight = pocScore.height > inst.lastHeight ? pocScore.height : inst.lastHeight;
        
        //TODO use the event to notify (there will have many consumers later): define an event 'POC_SCORE_CHANGED' 
        // to notify the all listeners: Generator and so on
        Generator.updatePocScore(_pocScore);
        
        PocScorePrinter.print();
    }

    /**
     * get the poc score according to specified height
     * @param height
     * @param accountId
     * @return
     */
    static PocScore getExistedPocScore(int height,long accountId){
        boolean containedInHistory = inst.historyScore.size() > 0 
                                    && inst.historyScore.containsKey(height) 
                                    && inst.historyScore.get(height).containsKey(accountId);

        PocScore score = null;
        if(!containedInHistory) {
            PocScore historyScore = PocDb.getPocScore(accountId, height, true);
            if(historyScore != null) {
                // update history map
                if(!inst.historyScore.containsKey(historyScore.height)) {
                    inst.historyScore.put(historyScore.height, Maps.newHashMap());
                }
                inst.historyScore.get(historyScore.height).put(accountId, historyScore);
                score = historyScore;
            }
        }else{
            score = inst.historyScore.get(height).get(accountId);
        }
        
        PocScorePrinter.print();
        return score;
    }
    
    /**
     * - record current poc score into history
     * - persistence
     * - update old poc score records #abandoned
     * @param pocScore
     */               
    static void recordHistoryScore(PocScore pocScore){
        PocScore historyPocScore = new PocScore(pocScore.height, pocScore);
        PocDb.saveOrUpdate(historyPocScore);
        
        if(!inst.historyScore.containsKey(pocScore.height)) {
            inst.historyScore.put(pocScore.height, Maps.newHashMap());
        }
        inst.historyScore.get(pocScore.height).put(pocScore.accountId, historyPocScore);
        
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    static PocTxBody.PocWeightTable getPocWeightTable(){
        return PocCalculator.inst.getCurWeightTable();
    }


    /**
     * Poc score map printer
     */
    private static class PocScorePrinter {
        static int count = 0;
        static final int printCount = 1;
        
        protected static boolean debug = Constants.isTestnetOrDevnet()  ? false : false;
        protected static boolean debugHistory = Constants.isDevnet() ? false : false;
        
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
                summary += appendSplitter("[DEBUG]" + Account.rsAccount(pocScore.accountId) + ",poc score=" + pocScore.total() + ":" + pocScore.toJsonString(),false);
            }
            return summary;
        }
        
        static void putin(){
            summary += appendSplitter("PocScore & Height Map[ accountId : PocScore ] height=" + Conch.getBlockchain().getHeight() + ", size=" + inst.scoreMap.size() + " >>>>>>>>",true);
            scoreMapStr(inst.scoreMap);
            summary += appendSplitter("<<<<<<<<<<",true);
            
            if(debugHistory) {
                summary += appendSplitter("PocScore & Height Map[ height : Map{ accountId : PocScore } ] size=" + inst.historyScore.size() + " >>>>>>>>",true);
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