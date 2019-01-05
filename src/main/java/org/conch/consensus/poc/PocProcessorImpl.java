package org.conch.consensus.poc;

import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockchainProcessor;
import org.conch.common.Constants;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {
  
  /** 
   * PocHolder to hold the score map.
   * This map stored in the memory, changed by the poc txs.
   */
  static class PocHolder {
    // poc score map
    static Map<Long, PocScore> scoreMap = null;
    // certified miner: foundation node,sharder hub, community node
    static Map<Integer, Map<Long, Peer>> certifiedMinerPeerMap = new ConcurrentHashMap<>();
  
    private PocHolder(){}

    /**
     * get the poc score of the specified height
     * @param height 
     * @param accountId 
     * @return
     */
    static BigInteger getPocScore(int height,long accountId) {
      if (!scoreMap.containsKey(accountId)) {
        if(Conch.getBlockchain().getHeight() < height) {
          synPocTxNow = true;
        }
        return BigInteger.ZERO;
      }

      PocScore pocScore = scoreMap.get(accountId);
      if(pocScore.height <= height) return pocScore.total();
      
      return BigInteger.ZERO;
    }

    /**
     * update the poc score of account
     * @param pocScore a poc score object
     */
    static synchronized void scoreMapping(PocScore pocScore){
       PocScore _pocScore = null;
       if(scoreMap.containsKey(pocScore.accountId)) {
          _pocScore = scoreMap.get(pocScore.accountId);
          _pocScore.synScoreFrom(pocScore);
       }else{
          _pocScore = pocScore;
       }
       scoreMap.put(pocScore.accountId,_pocScore);
    }
 
  }

  public static PocProcessorImpl instance = getOrCreate();

  private PocProcessorImpl() {}

  private static synchronized PocProcessorImpl getOrCreate() {
    return instance != null ? instance : new PocProcessorImpl();
  }

  static {
    Conch.getBlockchainProcessor().addListener(PocProcessorImpl::savePocScoreMap, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    
    loadExistPocHolder();
  }


  private static void loadExistPocHolder() {
    // TODO load the exist poc object from DB

    // TODO load the poc object from block history if db is null or db is older than current block

    // BlockchainImpl.getInstapnce().getBlocks()

    // TODO no disk backup, read the all blocks to combine the poc holder
  }

  

  @Override
  public BigInteger calPocScore(Account account, int height) {

    long id = SharderPoolProcessor.ownOnePool(account.getId());
    BigInteger effectiveBalance = BigInteger.ZERO;
    if (id != -1 && SharderPoolProcessor.getSharderPool(id).getState().equals(SharderPoolProcessor.State.WORKING)) {
        effectiveBalance = BigInteger.valueOf(Math.max(SharderPoolProcessor.getSharderPool(id).getPower() / Constants.ONE_SS, 0))
                .add(BigInteger.valueOf(Math.max(account.getEffectiveBalanceSS(height), 0)));
    }else {
        effectiveBalance = BigInteger.valueOf(Math.max(account.getEffectiveBalanceSS(height), 0));
    }
    return effectiveBalance;
            
//    temporary closed for dev test
//    return PocHolder.getPocScore(height, account.getId());
  }

  @Override
  public PocTxBody.PocWeightTable getPocWeightTable(Long version) {
    return null;
  }


  public static void init() {
    ThreadPool.scheduleThread("PocTxSyn", pocTxSynThread, 10, TimeUnit.MINUTES);
//    ThreadPool.scheduleThread("validNodeSyn", validNodeSynThread, 30, TimeUnit.MINUTES);
  }

  
  private static boolean synPocTxNow = false;
  private static final Runnable pocTxSynThread = new Runnable() {
        @Override
        public void run() {
          try {
            
          } catch (Exception e) {
            Logger.logDebugMessage("Blockchain download thread interrupted");
          } catch (Throwable t) {
            Logger.logErrorMessage(
                "CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
          }
        }
      };

  
  /**

  private static final String SC_FOUNDATION_API = "https://sharder.org/SC";
  private static final String SC_PEERS_API = SC_FOUNDATION_API + "/getPeers.ss";

  private static final Runnable validNodeSynThread = new Runnable() {
  @Override
  public void run() {
      try {
        
        String peersStr = Https.httpRequest(SC_PEERS_API,"GET", null);
        JSONArray peerArrayJson = com.alibaba.fastjson.JSON.parseArray(peersStr);
        Iterator iterator = peerArrayJson.iterator();
        while(iterator.hasNext()){
          JSONObject peerJson = (JSONObject)iterator.next();
          String host = peerJson.getString("host");
          String ip = IpUtil.getIp(host);
          Peer peer = Peers.getPeer(host);
        }
      } catch (Exception e) {
        Logger.logDebugMessage("syn valid node thread interrupted");
      } catch (Throwable t) {
        Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
        System.exit(1);
      }
    }
  };
  **/

//  private static void pocTxProcess(Block block) {
//    //@link: org.conch.chain.BlockchainProcessorImpl.autoExtensionAppend update the ext tag
//    Boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
//    if(containPoc == null || !containPoc) return;
//
//    //just process poc tx
//    for(Transaction tx : block.getTransactions()) {
//      if(TransactionType.TYPE_POC ==  tx.getType().getType()) {
//        weightTableMapping(tx);
//        PocHolder.scoreMapping(tx);
//      }
//    }
//  }
  private static void savePocScoreMap(Block block){
    //TODO save score map to local disk
  }

  /**
   * get the peer info and add it into the peer list by ip
   * @param ip
   */
  private static void _synPeer(String ip){
    //TODO get peer info by host and add it into map later
  }

  /**
   * process the node type tx of poc series
   * @param height block height that included this tx
   * @param pocNodeType PocNodeType tx 
   * @return
   */
  public static boolean nodeTypeTxProcess(int height,PocTxBody.PocNodeType pocNodeType){
    if(pocNodeType == null || StringUtils.isEmpty(pocNodeType.getIp())) return false;
    
    Peer peer = Peers.getPeer(pocNodeType.getIp());
    
    if(peer == null) { 
      _synPeer(pocNodeType.getIp());
      return false;
    }
    
    // update peer type
    long peerBindAccountId = peer.getBindAccountId();
    peer.setType(pocNodeType.getType());
    
    // update certified nodes
    Map<Long, Peer> peerMap = PocHolder.certifiedMinerPeerMap.get(height);
    if(peerMap == null) peerMap = new ConcurrentHashMap<>();
    peerMap.put(peerBindAccountId,peer);
    
    PocHolder.certifiedMinerPeerMap.put(height,peerMap);
    
    // re-calculate poc score
    PocScore pocScoreToUpdate = new PocScore(peerBindAccountId,height);
    pocScoreToUpdate.nodeTypeCal(pocNodeType);
 
    PocHolder.scoreMapping(pocScoreToUpdate);
    
    return false;
  }

  /**
   * process the node conf tx of poc series
   * @param height block height that included this tx
   * @param pocNodeConf PocNodeConf tx
   * @return
   */
  public static boolean nodeConfTxProcess(int height,PocTxBody.PocNodeConf pocNodeConf){
    Peer peer = Peers.getPeer(pocNodeConf.getIp());

    if(peer == null) {
      _synPeer(pocNodeConf.getIp());
      return false;
    }
    
    long peerBindAccountId = peer.getBindAccountId();
    PocScore pocScoreToUpdate = new PocScore(peerBindAccountId,height);
    pocScoreToUpdate.nodeConfCal(pocNodeConf);

    PocHolder.scoreMapping(pocScoreToUpdate);
    return false;
  }

  /**
   * process the online rate tx of poc series
   * @param height block height that included this tx
   * @param onlineRate OnlineRate tx
   * @return
   */
  public static boolean onlineRateTxProcess(int height,PocTxBody.PocOnlineRate onlineRate){
    Peer peer = Peers.getPeer(onlineRate.getIp());

    if(peer == null) {
      _synPeer(onlineRate.getIp());
      return false;
    }
    
    long peerBindAccountId = peer.getBindAccountId();
    PocScore pocScoreToUpdate = new PocScore(peerBindAccountId,height);
    pocScoreToUpdate.onlineRateCal(peer.getType(),onlineRate);

    PocHolder.scoreMapping(pocScoreToUpdate);
    return false;
  }

  /**
   * process the block miss tx of poc series
   * @param height block height that included this tx
   * @param pocBlockMiss PocBlockMiss tx
   * @return
   */
  public static boolean blockMissTxProcess(int height,PocTxBody.PocBlockMiss pocBlockMiss){
    
    long missAccountId = pocBlockMiss.getMissAccountId();
    PocScore pocScoreToUpdate = new PocScore(missAccountId,height);
    pocScoreToUpdate.blockMissCal(pocBlockMiss);

    PocHolder.scoreMapping(pocScoreToUpdate);
    return false;
  }
  

  private static void nodeRefresh() {
    // read the PocTemplate TX and parse them to PocTemplate object

    // read the ref PocTx and cal the score to generate accountScoreMap
  }
}
