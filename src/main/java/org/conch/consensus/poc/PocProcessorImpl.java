package org.conch.consensus.poc;

import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockImpl;
import org.conch.chain.BlockchainImpl;
import org.conch.chain.BlockchainProcessor;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.db.DbIterator;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.DiskStorageUtil;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
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

  public static Object _getMaxKey() {
    if (PocHolder.scoreMap == null || PocHolder.scoreMap.size() <= 0) return null;
    
    Set<Long> set = PocHolder.scoreMap.keySet();
    Object[] obj = set.toArray();
    Arrays.sort(obj);
    return obj[obj.length-1];
  }
  
  private static void loadExistPocHolder() {
    // read the disk backup
    boolean loadedFromDisk = false;
    int height = 0;
    File file = new File(DiskStorageUtil.getLocalStoragePath(LOCAL_STOAGE_POC_SCORE_MAP));
    if (file.exists()) {
      PocHolder.scoreMap = (Map<Long, PocScore>) DiskStorageUtil.getObjFromFile(LOCAL_STOAGE_POC_SCORE_MAP);
      loadedFromDisk = true;
      PocHolder.scoreMap.get(PocHolder.scoreMap.size() - 1);
    } else {
      PocHolder.scoreMap =  new ConcurrentHashMap<>();
    }

    if(loadedFromDisk) return;
    
    //if no disk backup, read the poc txs from history blocks
    DbIterator<BlockImpl> blocks = BlockchainImpl.getInstance().getAllBlocks();
    for(BlockImpl block : blocks) pocSeriesTxProcess(block);
    
  }
  
  @Override
  public BigInteger calPocScore(Account account, int height) {

    return PocScore.calEffectiveBalance(account,height);
//    temporary closed for dev test
//    return PocHolder.getPocScore(height, account.getId());
  }

  @Override
  public PocTxBody.PocWeightTable getPocWeightTable(Long version) {
    //TODO get the tx from holder
    return PocTxBody.PocWeightTable.defaultPocWeightTable();
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

  

  private static void pocSeriesTxProcess(Block block) {
    //@link: org.conch.chain.BlockchainProcessorImpl.autoExtensionAppend update the ext tag
    Boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
    if(containPoc == null || !containPoc) return;

    //just process poc tx
    for(Transaction tx : block.getTransactions()) {
      if(TransactionType.TYPE_POC !=  tx.getType().getType()) continue;
      
      if(PocTxWrapper.SUBTYPE_POC_NODE_TYPE == tx.getType().getSubtype()) {
        nodeTypeTxProcess(tx.getHeight(), (PocTxBody.PocNodeType)tx.getAttachment());
      }else if(PocTxWrapper.SUBTYPE_POC_NODE_CONF == tx.getType().getSubtype()){
        nodeConfTxProcess(tx.getHeight(), (PocTxBody.PocNodeConf)tx.getAttachment());
      }else if(PocTxWrapper.SUBTYPE_POC_ONLINE_RATE == tx.getType().getSubtype()){
        onlineRateTxProcess(tx.getHeight(), (PocTxBody.PocOnlineRate)tx.getAttachment());
      }else if(PocTxWrapper.SUBTYPE_POC_BLOCK_MISS == tx.getType().getSubtype()){
        blockMissTxProcess(tx.getHeight(), (PocTxBody.PocBlockMiss)tx.getAttachment());
      }else if(PocTxWrapper.SUBTYPE_POC_WEIGHT == tx.getType().getSubtype()){
        PocScore.setCurWeightTable((PocTxBody.PocWeightTable)tx.getAttachment());
      }
    }
    
  }
  
  private static final String LOCAL_STOAGE_POC_SCORE_MAP = "PocScoreMap";
  private static void savePocScoreMap(Block block){
      DiskStorageUtil.saveObjToFile(PocHolder.scoreMap, LOCAL_STOAGE_POC_SCORE_MAP);
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
  
}
