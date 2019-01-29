package org.conch.consensus.poc;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
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
  public static class PocHolder implements Serializable {
    
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
    
    
    static void _defaultPocScore(long accountId,int height){
      scoreMapping(new PocScore(accountId,height));  
    }
    
    /**
     * get the poc score of the specified height
     * @param height 
     * @param accountId 
     * @return
     */
    static BigInteger getPocScore(int height,long accountId) {
      if(height < 0) height = 0;
      if (!scoreMap.containsKey(accountId)) {
        if(Conch.getBlockchain().getHeight() < height) {
          synPocTxNow = true;
        }
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

    public static PocScore getHistoryPocScore(int height,long accountId){
        if(!historyScore.containsKey(height)) { 
          return null;
        }
        return historyScore.get(height).get(accountId);
    }
    
    static PocTxBody.PocWeightTable getPocWeightTable(){
      boolean hasPocWeightTable = true;
      if(scoreMap == null || PocScore.PocCalculator.pocWeightTable == null) {
          hasPocWeightTable = false;
      }
      
      return hasPocWeightTable ? PocScore.PocCalculator.pocWeightTable : PocTxBody.PocWeightTable.defaultPocWeightTable();
    }
 
  }

  public static PocProcessorImpl instance = getOrCreate();

  private PocProcessorImpl() {}

  private static synchronized PocProcessorImpl getOrCreate() {
    return instance != null ? instance : new PocProcessorImpl();
  }

  public static boolean isCertifiedPeerBind(long accountId){
    boolean hubBindAccount = PocHolder.certifiedBindAccountMap.get(Peer.Type.HUB).containsKey(accountId);
    boolean communityBindAccount = PocHolder.certifiedBindAccountMap.get(Peer.Type.COMMUNITY).containsKey(accountId);
    boolean foundationBindAccount = PocHolder.certifiedBindAccountMap.get(Peer.Type.FOUNDATION).containsKey(accountId);
    return hubBindAccount || communityBindAccount || foundationBindAccount;
  }

  public static boolean isHubBind(long accountId){
    return PocHolder.certifiedBindAccountMap.get(Peer.Type.HUB).containsKey(accountId);
  }
  
  public static boolean isHubBind(long accountId, String peerIp){
    String bindPeerIp = PocHolder.certifiedBindAccountMap.get(Peer.Type.HUB).get(accountId);
   
    return bindPeerIp != null && peerIp != null && bindPeerIp.equalsIgnoreCase(peerIp);
  }

  private static final String LOCAL_STORAGE_POC_HOLDER = "PocHolder";

  static {
    // new block -> persistence poc holder (save the poc holder to disk)
    Conch.getBlockchainProcessor().addListener((Block block) -> {
      DiskStorageUtil.saveObjToFile(PocHolder.inst, LOCAL_STORAGE_POC_HOLDER);
    }, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    
    // balance changed -> ss hold score re-calculate
    Account.addListener((Account account) -> {
      //TODO height get from event or other ways
      balanceChangedProcess(-1,account);
    }, Account.Event.BALANCE);
    
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
    if(PocHolder.lastHeight <= Conch.getBlockchain().getHeight()) {
        synPocTxNow = true;
    }

  }
  
  @Override
  public BigInteger calPocScore(Account account, int height) {
    return PocHolder.getPocScore(height, account.getId());
  }

  @Override
  public PocTxBody.PocWeightTable getPocWeightTable(Long version) {
    return PocHolder.getPocWeightTable();
  }

  public static void init() {
    ThreadPool.scheduleThread("PocTxSynThread", pocTxSynThread, 10, TimeUnit.SECONDS);
    ThreadPool.scheduleThread("PeerSynThread", peerSynThread, 10, TimeUnit.SECONDS);
  }

  private static boolean synPocTxNow = true;
  private static final Runnable pocTxSynThread = () -> {
    try {
      
      if(!synPocTxNow) {
        Logger.logInfoMessage("No needs to syn now, sleep 10 minutes...");
        Thread.sleep(10 * 60 * 1000);
      }
      
      int fromHeight = (PocHolder.lastHeight <= -1) ? 0 : PocHolder.lastHeight;
      int toHeight = BlockchainImpl.getInstance().getHeight();


      DbIterator<BlockImpl> blocks = BlockchainImpl.getInstance().getBlocks(fromHeight,toHeight);
      for(BlockImpl block : blocks) {
          pocSeriesTxProcess(block);
      }

      synPocTxNow = false;
      
    } catch (Exception e) {
      Logger.logDebugMessage("Poc tx syn thread interrupted");
    } catch (Throwable t) {
      Logger.logErrorMessage(
          "CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
      System.exit(1);
    }
  };
  
  private static volatile List<String> synPeerList = Lists.newArrayList();
  private static final Runnable peerSynThread = () -> {
    try {
      
      if(synPeerList.size() <= 0) {
        Logger.logInfoMessage("No needs to syn peer, sleep 10 minutes...");
        Thread.sleep(10 * 60 * 1000);
      }

      for(String peerAddress : synPeerList){
        Peer peer = Peers.findOrCreatePeer(peerAddress, Peers.isUseNATService(peerAddress), true);
        if (peer != null) {
          Peers.addPeer(peer, peerAddress);
          Peers.connectPeer(peer);
        }
        peer = Peers.getPeer(peerAddress);
        _updateCertifiedNodes(peer.getHost(), peer.getType(), -1);
      }
      synPeerList.clear();
      
    } catch (Exception e) {
      Logger.logDebugMessage("Peer syn thread interrupted");
    } catch (Throwable t) {
      Logger.logErrorMessage(
          "CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
      System.exit(1);
    }
  };

  

  private static void pocSeriesTxProcess(Block block) {
    //@link: org.conch.chain.BlockchainProcessorImpl.autoExtensionAppend update the ext tag
    Boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
    if(containPoc == null || !containPoc) {
        return;
    }

    //just process poc tx
    for(Transaction tx : block.getTransactions()) {
      if(TransactionType.TYPE_POC !=  tx.getType().getType()) {
          continue;
      }
      
      if(PocTxWrapper.SUBTYPE_POC_NODE_TYPE == tx.getType().getSubtype()) {
        nodeTypeTxProcess(tx.getHeight(), (PocTxBody.PocNodeType)tx.getAttachment());
      }else if(PocTxWrapper.SUBTYPE_POC_NODE_CONF == tx.getType().getSubtype()){
        nodeConfTxProcess(tx.getHeight(), (PocTxBody.PocNodeConf)tx.getAttachment());
      }else if(PocTxWrapper.SUBTYPE_POC_ONLINE_RATE == tx.getType().getSubtype()){
        onlineRateTxProcess(tx.getHeight(), (PocTxBody.PocOnlineRate)tx.getAttachment());
      }else if(PocTxWrapper.SUBTYPE_POC_BLOCK_MISSING == tx.getType().getSubtype()){
        blockMissingTxProcess(tx.getHeight(), (PocTxBody.PocGenerationMissing)tx.getAttachment());
      }else if(PocTxWrapper.SUBTYPE_POC_WEIGHT_TABLE == tx.getType().getSubtype()){
        PocScore.PocCalculator.setCurWeightTable((PocTxBody.PocWeightTable)tx.getAttachment(),block.getHeight());
      }
    }
    
  }
  
  private static void _updateCertifiedNodes(String ip, Peer.Type type, int height){
    Peer peer = Peers.getPeer(ip);
    if(StringUtils.isEmpty(ip)){
      Logger.logWarningMessage("peer ip[" + ip + "] is null, can't find peer!");
      return;
    }
    
    // update peer type
    String bindRsAccount = peer.getBindRsAccount();
    if(StringUtils.isEmpty(bindRsAccount)){
      Logger.logWarningMessage("bind rs account of peer[ip=" + ip + "] is null, can't finish certified node updated");
      return;
    }
    long peerBindAccountId = Account.rsAccountToId(bindRsAccount);
    peer.setType(type);

    // update certified nodes
    Map<Long, Peer> peerMap = PocHolder.certifiedMinerPeerMap.get(height);
    if(peerMap == null) {
        peerMap = new ConcurrentHashMap<>();
    }
    peerMap.put(peerBindAccountId,peer);
    PocHolder.certifiedMinerPeerMap.put(height,peerMap);
    
    //update peer bind account by peer type
    if(!PocHolder.certifiedBindAccountMap.containsKey(type)) {
      PocHolder.certifiedBindAccountMap.put(type,new ConcurrentHashMap<>());
    }
    
    Map<Long, String> bindAccountMap = PocHolder.certifiedBindAccountMap.get(type);
    if(bindAccountMap.containsKey(peerBindAccountId)) {
      String peerIp = bindAccountMap.get(peerBindAccountId);
      if(!ip.equalsIgnoreCase(peerIp)) {
        bindAccountMap.remove(peerBindAccountId);
      }
    }
    bindAccountMap.put(peerBindAccountId,ip);
  }


  private static PocScore getPocScoreByPeer(int height, String ip){
    Peer peer = Peers.getPeer(ip);
    if(peer == null) {
      synPeerList.add(ip);
      return null;
    }

    long peerBindAccountId = Account.rsAccountToId(peer.getBindRsAccount());
    return new PocScore(peerBindAccountId,height);
  }
  

  /**
   * process the node type tx of poc series
   * @param height block height that included this tx
   * @param pocNodeType PocNodeType tx 
   * @return
   */
  public static boolean nodeTypeTxProcess(int height,PocTxBody.PocNodeType pocNodeType){
    if(pocNodeType == null || StringUtils.isEmpty(pocNodeType.getIp())) {
        return false;
    }

    PocScore pocScoreToUpdate = getPocScoreByPeer(height, pocNodeType.getIp());
    if(pocScoreToUpdate == null) {
      return false;
    }
    pocScoreToUpdate.nodeTypeCal(pocNodeType);

    PocHolder.scoreMapping(pocScoreToUpdate);

    _updateCertifiedNodes(pocNodeType.getIp(),pocNodeType.getType(),height);
    
    return true;
  }

  
  /**
   * process the node conf tx of poc series
   * @param height block height that included this tx
   * @param pocNodeConf PocNodeConf tx
   * @return
   */
  public static boolean nodeConfTxProcess(int height,PocTxBody.PocNodeConf pocNodeConf){
    PocScore pocScoreToUpdate = getPocScoreByPeer(height, pocNodeConf.getIp());
    if(pocScoreToUpdate == null) {
      return false;
    }
    
    pocScoreToUpdate.nodeConfCal(pocNodeConf);

    PocHolder.scoreMapping(pocScoreToUpdate);
    return true;
  }

  /**
   * process the online rate tx of poc series
   * @param height block height that included this tx
   * @param onlineRate OnlineRate tx
   * @return
   */
  public static boolean onlineRateTxProcess(int height,PocTxBody.PocOnlineRate onlineRate){
    Peer peer = Peers.getPeer(onlineRate.getIp());

    PocScore pocScoreToUpdate = getPocScoreByPeer(height, onlineRate.getIp());
    if(pocScoreToUpdate == null) {
      return false;
    }
    
    pocScoreToUpdate.onlineRateCal(peer.getType(),onlineRate);

    PocHolder.scoreMapping(pocScoreToUpdate);
    return true;
  }

  /**
   * process the block miss tx of poc series
   * @param height block height that included this tx
   * @param pocBlockMissing PocBlockMissing tx
   * @return
   */
  public static boolean blockMissingTxProcess(int height, PocTxBody.PocGenerationMissing pocBlockMissing){
    
    List<Long> missAccountIds = pocBlockMissing.getMissingAccountIds();
    for(Long missAccountId : missAccountIds){
      PocScore pocScoreToUpdate = new PocScore(missAccountId,height);
      pocScoreToUpdate.blockMissCal(pocBlockMissing);
      PocHolder.scoreMapping(pocScoreToUpdate);
    }
    return true;
  }
  
  /**
   * process the balance of account changed
   * @param height block height that included this tx
   * @param account which balance is changed
   * @return
   */
  private static boolean balanceChangedProcess(int height, Account account){
    if(account == null) {
      return false;
    }

    long accountId = account.getId();
    PocScore pocScoreToUpdate = new PocScore(accountId,height);
    pocScoreToUpdate.ssScoreCal();
    PocHolder.scoreMapping(pocScoreToUpdate);
    return true;
  }
  
}
