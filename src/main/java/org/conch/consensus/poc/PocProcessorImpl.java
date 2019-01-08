package org.conch.consensus.poc;

import com.google.common.collect.Lists;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {


  /** 
   * PocHolder is a singleton to hold the score map.
   * This map stored in the memory, changed by the poc txs.
   */
  public static class PocHolder {
    
    static PocHolder inst = new PocHolder();
    
    // poc score map
    static Map<Long, PocScore> scoreMap = new ConcurrentHashMap<>();
    // certified miner: foundation node,sharder hub, community node
    static Map<Integer, Map<Long, Peer>> certifiedMinerPeerMap = new ConcurrentHashMap<>();
    
    static int lastHeight = -1;
  
    private PocHolder(){}
    
    
    static void _defaultPocScore(long accountId){
      PocHolder.inst.scoreMapping(new PocScore(accountId,-1));  
    }
    
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
        _defaultPocScore(accountId);
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
       PocScore _pocScore = pocScore;
       if(scoreMap.containsKey(pocScore.accountId)) {
          _pocScore = scoreMap.get(pocScore.accountId);
          _pocScore.synScoreFrom(pocScore);
       }
       
       scoreMap.put(pocScore.accountId,_pocScore);
       lastHeight = pocScore.height > lastHeight ? pocScore.height : lastHeight;
    }
    
    static PocTxBody.PocWeightTable getPocWeightTable(){
      boolean hasPocWeightTable = true;
      if(scoreMap == null || PocScore.PocCalculator.pocWeightTable == null) hasPocWeightTable = false;
      
      return hasPocWeightTable ? PocScore.PocCalculator.pocWeightTable : PocTxBody.PocWeightTable.defaultPocWeightTable();
    }
 
  }

  public static PocProcessorImpl instance = getOrCreate();

  private PocProcessorImpl() {}

  private static synchronized PocProcessorImpl getOrCreate() {
    return instance != null ? instance : new PocProcessorImpl();
  }

  static {
    Conch.getBlockchainProcessor().addListener(PocProcessorImpl::savePocHolder, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    
    loadExistPocHolder();
  }

  /**
   * load the poc holder backup from local disk
   */
  private static void loadExistPocHolder() {
    // read the disk backup
    File file = new File(DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_POC_HOLDER));
    if (file.exists()) {
      PocHolder.inst = (PocHolder) DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_POC_HOLDER);
    } 

    //if no disk backup, read the poc txs from history blocks
    if(PocHolder.inst.lastHeight <= Conch.getBlockchain().getHeight()) synPocTxNow = true;

  }
  
  @Override
  public BigInteger calPocScore(Account account, int height) {
    return PocHolder.getPocScore(height, account.getId());
  }

  @Override
  public PocTxBody.PocWeightTable getPocWeightTable(Long version) {
    return PocHolder.inst.getPocWeightTable();
  }

  public static void init() {
    ThreadPool.scheduleThread("PocTxSynThread", pocTxSynThread, 10, TimeUnit.SECONDS);
    ThreadPool.scheduleThread("PeerSynThread", peerSynThread, 10, TimeUnit.SECONDS);
  }

  private static boolean synPocTxNow = true;
  private static final Runnable pocTxSynThread = new Runnable() {
      @Override
      public void run() {
        try {
          
          if(!synPocTxNow) {
            Logger.logInfoMessage("No needs to syn now, sleep 10 minutes...");
            Thread.sleep(10 * 60 * 1000);
          }
          
          int fromHeight = PocHolder.inst.lastHeight;
          int toHeight = BlockchainImpl.getInstance().getHeight();
          if(fromHeight <= -1) fromHeight = 0;


          DbIterator<BlockImpl> blocks = BlockchainImpl.getInstance().getBlocks(fromHeight,toHeight);
          for(BlockImpl block : blocks) pocSeriesTxProcess(block);

          synPocTxNow = false;
          
        } catch (Exception e) {
          Logger.logDebugMessage("Poc tx syn thread interrupted");
        } catch (Throwable t) {
          Logger.logErrorMessage(
              "CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
          System.exit(1);
        }
      }
  };
  
  private static volatile List<String> synPeerList = Lists.newArrayList();
  private static final Runnable peerSynThread = new Runnable() {
      @Override
      public void run() {
        try {
          
          if(synPeerList.size() <= 0) {
            Logger.logInfoMessage("No needs to syn peer, sleep 10 minutes...");
            Thread.sleep(10 * 60 * 1000);
          }

          for(String ip : synPeerList){
            //TODO call Peers to get peer info by host and add it into map
            Peer.Type type = null;
            _updateCertifiedNodes(ip, type, -1);
          }
          synPeerList.clear();
          
        } catch (Exception e) {
          Logger.logDebugMessage("Peer syn thread interrupted");
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
      }else if(PocTxWrapper.SUBTYPE_POC_WEIGHT_TABLE == tx.getType().getSubtype()){
        PocScore.PocCalculator.setCurWeightTable((PocTxBody.PocWeightTable)tx.getAttachment(),block.getHeight());
      }
    }
    
  }
  
  private static final String LOCAL_STORAGE_POC_HOLDER = "PocHolder";

  /**
   * save the poc holder to disk
   * @param block
   */
  private static void savePocHolder(Block block){
      DiskStorageUtil.saveObjToFile(PocHolder.inst, LOCAL_STORAGE_POC_HOLDER);
  }
  
  private static void _updateCertifiedNodes(String ip, Peer.Type type, int height){
    Peer peer = Peers.getPeer(ip);
    
    // update peer type
    long peerBindAccountId = peer.getBindAccountId();
    peer.setType(type);

    // update certified nodes
    Map<Long, Peer> peerMap = PocHolder.inst.certifiedMinerPeerMap.get(height);
    if(peerMap == null) peerMap = new ConcurrentHashMap<>();
    peerMap.put(peerBindAccountId,peer);

    PocHolder.inst.certifiedMinerPeerMap.put(height,peerMap);
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
      synPeerList.add(pocNodeType.getIp());
      return false;
    }
    
    _updateCertifiedNodes(pocNodeType.getIp(),pocNodeType.getType(),height);
    
    // re-calculate poc score
    PocScore pocScoreToUpdate = new PocScore(peer.getBindAccountId(),height);
    pocScoreToUpdate.nodeTypeCal(pocNodeType);

    PocHolder.inst.scoreMapping(pocScoreToUpdate);
    
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
      synPeerList.add(pocNodeConf.getIp());
      return false;
    }
    
    long peerBindAccountId = peer.getBindAccountId();
    PocScore pocScoreToUpdate = new PocScore(peerBindAccountId,height);
    pocScoreToUpdate.nodeConfCal(pocNodeConf);

    PocHolder.inst.scoreMapping(pocScoreToUpdate);
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
      synPeerList.add(onlineRate.getIp());
      return false;
    }
    
    long peerBindAccountId = peer.getBindAccountId();
    PocScore pocScoreToUpdate = new PocScore(peerBindAccountId,height);
    pocScoreToUpdate.onlineRateCal(peer.getType(),onlineRate);

    PocHolder.inst.scoreMapping(pocScoreToUpdate);
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

    PocHolder.inst.scoreMapping(pocScoreToUpdate);
    return false;
  }
  
}
