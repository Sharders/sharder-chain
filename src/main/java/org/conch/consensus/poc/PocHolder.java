package org.conch.consensus.poc;

import com.google.common.collect.Maps;
import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.Peer;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PocHolder is a singleton to hold the score map.
 * This map stored in the memory, changed by the poc txs.
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-29
 */

public class PocHolder implements Serializable {

    static PocHolder inst = new PocHolder();

    // accountId : pocScore
    static Map<Long, PocScore> scoreMap = new ConcurrentHashMap<>();
    // height : { accountId : pocScore }
    static Map<Integer,Map<Long,PocScore>> historyScore = new ConcurrentHashMap<>();

    // certified miner: foundation node,sharder hub, community node
    // height : <bindAccountId,peer>
    static Map<Integer, Map<Long, Peer>> certifiedMinerPeerMap = new ConcurrentHashMap<>();
    // peerType : <bindAccountId,peerIp> 
    static Map<Peer.Type, Map<Long, String>> certifiedBindAccountMap = Maps.newConcurrentMap();

    static int lastHeight = -1;

    static {
        certifiedBindAccountMap.put(Peer.Type.HUB,Maps.newConcurrentMap());
        certifiedBindAccountMap.put(Peer.Type.COMMUNITY,Maps.newConcurrentMap());
        certifiedBindAccountMap.put(Peer.Type.FOUNDATION,Maps.newConcurrentMap());
    }

    private PocHolder(){}


    private static void _defaultPocScore(long accountId,int height){
        scoreMapping(new PocScore(accountId,height));
    }

    /**
     * get the poc score of the specified height
     * @param height
     * @param accountId
     * @return
     */
    static BigInteger getPocScore(int height, long accountId) {
        if(height < 0) height = 0;
        if (!scoreMap.containsKey(accountId)) {
            PocProcessorImpl.instance.notifySynTxNow();
            _defaultPocScore(accountId,height);
        }

        PocScore pocScore = scoreMap.get(accountId);
        //newest poc score when query height is bigger than last height of poc score 
        if(pocScore.height <= height) {
            return pocScore.total();
        }else{
            //get from history
            pocScore = getHistoryPocScore(height, accountId);
            if(pocScore != null) {
                return pocScore.total();
            }
        }
        return BigInteger.ZERO;
    }

    /**
     * update the poc score of account
     * @param pocScore a poc score object
     */
    static synchronized void scoreMapping(PocScore pocScore){
        PocScore _pocScore = pocScore;
        if(scoreMap.containsKey(pocScore.accountId)) {
            _pocScore = scoreMap.get(pocScore.accountId);
            _pocScore.synScoreFrom(pocScore);
            recordHistoryScore(pocScore);
        }

        scoreMap.put(pocScore.accountId,_pocScore);
        lastHeight = pocScore.height > lastHeight ? pocScore.height : lastHeight;
    }

    static BigInteger getTotal(int height,Long accountId){
        Map<Long,PocScore> map = historyScore.get(height);
        if(map == null) return BigInteger.ZERO;
        PocScore score = map.get(accountId);
        return score !=null ? score.total() : BigInteger.ZERO;
    }

    /**
     * record current poc score into history
     */
    static void recordHistoryScore(PocScore pocScore){
        Map<Long,PocScore> map = historyScore.get(pocScore.height);
        if(map == null) map = new HashMap<>();

        map.put(pocScore.accountId,new PocScore(pocScore.height, pocScore));

        historyScore.put(pocScore.height,map);
    }

    static PocScore getHistoryPocScore(int height,long accountId){
        if(!historyScore.containsKey(height)) {
            return null;
        }
        return historyScore.get(height).get(accountId);
    }

    static PocTxBody.PocWeightTable getPocWeightTable(){
        return PocCalculator.inst.getCurWeightTable();
    }

}