package org.conch.consensus.poc;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.poc.db.PocDb;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.mint.Generator;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.tx.*;
import org.conch.util.Logger;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    // Tips: you can use the key word 'transient' exclude the attribute to persist
    static PocHolder inst = new PocHolder();

    int lastHeight = -1;
    
    // Map: accountId : pocScore
    protected transient Map<Long, PocScore> scoreMap = PocDb.listAllScore();

    // certified peer: foundation node, hub/box, community node
    // Map: account id : certified peer
    protected Map<Long, CertifiedPeer> certifiedPeers = PocDb.listAllPeers();

    private volatile Map<Integer, List<Long>> delayPocTxsByHeight = Maps.newConcurrentMap();
    private static volatile int pocTxHeight = -1;
    
    public static void updateHeight(int height){
        if(height == -1) {
            return;
        }
        
        inst.lastHeight = height;
    }
    
    public static boolean resetCertifiedPeers(){
        synchronized (inst.certifiedPeers) {
            inst.certifiedPeers.clear();
        }
        return true;
    }

    public static CertifiedPeer getBoundPeer(long accountId, int height) {
        if(height == -1 || accountId == -1) {
            return null;
        }
        CertifiedPeer certifiedPeer = inst.certifiedPeers.get(accountId);
        if(certifiedPeer == null) {
            PocDb.getPeer(accountId, height, true);
        }
        
        return certifiedPeer;
    }

    public static CertifiedPeer getBoundPeer(String host, int height) {
        if(height == -1 || StringUtils.isEmpty(host)) {
            return null;
        }
        
        CertifiedPeer certifiedPeer = null;
        Collection<CertifiedPeer> peers = inst.certifiedPeers.values();
        for(CertifiedPeer peer : peers){
            if(StringUtils.equals(host,peer.getHost())){
                certifiedPeer = peer;
                break;
            }
        }
        
        if(certifiedPeer == null) {
            certifiedPeer = PocDb.getPeer(host, height, true);
        }
        
        return certifiedPeer;
    }

    public static boolean isBoundPeer(Peer.Type type, long account) {
        CertifiedPeer certifiedPeer = inst.certifiedPeers.get(account);
        return certifiedPeer == null ? false : certifiedPeer.isType(type);
    }

    /**
     * Add or update certified peer and bind account
     * 3 callers: PocHolder, PoC tx processor, Hub syn thread in Peers
     * 
     * @param type peer type
     * @param host peer host
     * @param accountId bound account id
     */
    public synchronized static void addOrUpdateCertifiedPeer(int height, Peer.Type type, String host, long accountId) {
        CertifiedPeer newPeer = new CertifiedPeer(height, type, host, accountId);
        try{
            newPeer.check();
        }catch(ConchException.NotValidException e){
            Logger.logWarningMessage("failed to add or update a certified peer caused by [%s], Peer is %s",
                    e.getMessage(), newPeer.toString());
            return;
        }

        PocDb.saveOrUpdatePeer(newPeer);

        if (!inst.certifiedPeers.containsKey(accountId)) {
            inst.certifiedPeers.put(accountId, newPeer);
            return;
        }

    }
    
    public static int countDelayPocTxs(int queryHeight) {
        int count = 0;
        // order by height
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
    
    private PocHolder(){}

    static PocScore saveOrUpdate(PocScore pocScore) {
        PocDb.saveOrUpdateScore(pocScore);
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
        height = height < 0 ? 0 : height;

        PocScore pocScore = inst.scoreMap.containsKey(accountId) ? inst.scoreMap.get(accountId) : null;
        if (pocScore != null) {
            pocScore.total = pocScore.total();
        }
        if(pocScore == null
        || pocScore.total().intValue() == 0) {
            pocScore = getExistedPocScore(height, accountId);
        }

        if(pocScore == null) {
            pocScore = new PocScore(accountId,height);
            // inst form txs
            TransactionImpl nodeTypeTx = TransactionDb.findTxByType(height, TransactionType.TYPE_POC, PocTxWrapper.SUBTYPE_POC_NODE_TYPE);
            if (nodeTypeTx != null) {
                PocTxBody.PocNodeTypeV3 nodeType = null;
                Attachment attachment = nodeTypeTx.getAttachment();
                if(attachment instanceof PocTxBody.PocNodeTypeV3){
                    nodeType = (PocTxBody.PocNodeTypeV3) attachment;
                }
                pocScore.nodeTypeCal(nodeType);
            }
            scoreMapping(pocScore);
        }else{
            inst.scoreMap.put(accountId, pocScore);
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
        }
        
        PocDb.saveOrUpdateScore(_pocScore);
        inst.scoreMap.put(pocScore.accountId,_pocScore);

        //TODO use the event to notify (there will have many consumers later):
        // define an event 'POC_SCORE_CHANGED' to notify the all listeners: Generator and so on
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
        PocScore score = PocDb.getPocScore(accountId, height, true);
        PocScorePrinter.print();
        return score;
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
        }

        static void print(){
            if(!debug || (debug && (count++  <= printCount)) ) {
                return;
            }
            putin();
            Logger.logDebugMessage(summary);
            summary = reset();
        }
    }

    public static void main(String[] args) {
        System.out.println(Peer.BlockchainState.UP_TO_DATE);
    }

}