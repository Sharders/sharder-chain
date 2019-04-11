package org.conch.consensus.poc;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockImpl;
import org.conch.chain.BlockchainImpl;
import org.conch.chain.BlockchainProcessor;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.DiskStorageUtil;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 *
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {

  public static PocProcessorImpl instance = getOrCreate();

  private PocProcessorImpl() {}

  private static synchronized PocProcessorImpl getOrCreate() {
    return instance != null ? instance : new PocProcessorImpl();
  }

  /**
   * get bind peer type
   * @param accountId
   * @return
   */
  public static Peer.Type bindPeerType(long accountId){
    CertifiedPeer certifiedPeer = PocHolder.getBoundPeer(accountId);
    return certifiedPeer == null ? Peer.Type.NORMAL : certifiedPeer.getType();
  }

  /**
   * account whether bound to certified peer
   *
   * @param accountId
   * @return
   */
  public static boolean isCertifiedPeerBind(long accountId) {
    boolean hubBindAccount = PocHolder.isBoundPeer(Peer.Type.HUB, accountId);
    boolean communityBindAccount = PocHolder.isBoundPeer(Peer.Type.COMMUNITY, accountId);
    boolean foundationBindAccount = PocHolder.isBoundPeer(Peer.Type.FOUNDATION, accountId);
    return hubBindAccount || communityBindAccount || foundationBindAccount;
  }

  public static boolean isFoundationBind(long accountId) {
    return PocHolder.isBoundPeer(Peer.Type.FOUNDATION, accountId);
  }
  
  public static boolean isHubBind(long accountId) {
    return PocHolder.isBoundPeer(Peer.Type.HUB, accountId);
  }

  public static boolean isHubBind(long accountId, String peerHost) {
    CertifiedPeer bindPeer = PocHolder.getBoundPeer(accountId);

    return bindPeer != null && bindPeer.isSame(peerHost) && bindPeer.isType(Peer.Type.HUB);
  }

  private static final String LOCAL_STORAGE_POC_HOLDER = "StoredPocHolder";
  private static final String LOCAL_STORAGE_POC_CALCULATOR = "StoredPocCalculator";
  
  private static Map<Long,Account> balanceChangedMap = new HashMap<>();
  static {
    // new block accepted
    Conch.getBlockchainProcessor().addListener((Block block) -> {
      // ss hold score re-calculate
      // remark: the potential logic is: received Account.Event.BALANCE firstly, then received Event.AFTER_BLOCK_ACCEPT
      boolean someAccountBalanceChanged = balanceChangedMap.size() > 0;
      if(someAccountBalanceChanged) {
        for(Account account : balanceChangedMap.values()){
          balanceChangedProcess(block.getHeight(),account);
        }
        balanceChangedMap.clear();
      }
      
      Boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
      boolean blockContainPocTxs = containPoc == null ? false : containPoc;
      
      //save to disk when poc score changed case of contains poc txs in block or account balance changed
      if(someAccountBalanceChanged || blockContainPocTxs) {
        //save the poc holder and calculator to disk
        saveToDisk();
      }
    }, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    
    // balance changed
    Account.addListener((Account account) -> {
      if(!balanceChangedMap.containsKey(account.getId())) {
        balanceChangedMap.put(account.getId(), account);
      }
    }, Account.Event.BALANCE);

    loadFromDisk();
  }

  /**
   * save the poc holder and calculator to disk
   */
  private static void saveToDisk() {
    DiskStorageUtil.saveObjToFile(PocHolder.inst, LOCAL_STORAGE_POC_HOLDER);
    DiskStorageUtil.saveObjToFile(PocCalculator.inst, LOCAL_STORAGE_POC_CALCULATOR);
  }

  /**
   * load the poc holder backup from local disk
   */
  private static void loadFromDisk() {
    // read the disk backup
    Logger.logInfoMessage("load exist poc holder instance from local disk[" + DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_POC_HOLDER) + "]");
    Object holderObj = DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_POC_HOLDER);
    if(holderObj != null) {
      PocHolder.inst = (PocHolder) holderObj;
    }

    Logger.logInfoMessage("load exist poc calculator instance from local disk[" + DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_POC_CALCULATOR) + "]");
    Object calcObj = DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_POC_CALCULATOR);
    if(calcObj != null) {
      PocCalculator.inst = (PocCalculator) calcObj;
    }
    

    //if no disk backup, read the poc txs from history blocks
    if(PocHolder.inst != null && PocHolder.inst.lastHeight <= Conch.getBlockchain().getHeight()) {
      oldPocTxsProcess = true;
    }
  }
  
  @Override
  public PocScore calPocScore(Account account, int height) {
    return PocHolder.getPocScore(height, account.getId());
  }

  @Override
  public PocTxBody.PocWeightTable getPocWeightTable(Long version) {
    return PocHolder.getPocWeightTable();
  }

  private static final int peerSynThreadInterval = 600;
  private static final int pocTxSynThreadInterval = 30;
  public static void init() {
    ThreadPool.scheduleThread("PocTxSynThread", pocTxSynThread, pocTxSynThreadInterval, TimeUnit.SECONDS);
    ThreadPool.scheduleThread("PeerSynThread", peerSynThread, peerSynThreadInterval, TimeUnit.SECONDS);
  }


  public static void notifySynTxNow(){
    oldPocTxsProcess = true;
  }

  private static boolean oldPocTxsProcess = false;
  private static final Runnable pocTxSynThread = () -> {
    try {
      
      if(Conch.getBlockchainProcessor().isDownloading()) {
        Logger.logDebugMessage("block is downloading, don't process delayed poc txs till blocks sync finished...");
        return;
      }
      
      if(PocHolder.delayPocTxs().size() <= 0 && !oldPocTxsProcess) {
        Logger.logDebugMessage("no needs to syn and process poc serial txs now, sleep %d seconds...", pocTxSynThreadInterval);
        return;
      }
      
      // delayed poc txs 
      Logger.logInfoMessage("process delayed poc txs[size=%d]", PocHolder.delayPocTxs().size());
      Set<Long> processedTxs = Sets.newHashSet();
      PocHolder.delayPocTxs().forEach(txid -> {
        boolean txProcessed = pocTxProcess(txid);
        if(txProcessed) {
          processedTxs.add(txid);
        }
      });
      
      // remove processed txs
      if(processedTxs.size() > 0) {
        Logger.logInfoMessage("success to process delayed poc txs[size=%d]", processedTxs.size());
        Logger.logDebugMessage("processed poc txs detail => " + Arrays.toString(processedTxs.toArray()));
        PocHolder.removeProcessedTxs(processedTxs);
      }

     
      
      if(oldPocTxsProcess) {
        // total poc txs from last height
//        int fromHeight = (PocHolder.inst.lastHeight <= -1) ? 0 : PocHolder.inst.lastHeight;
        int fromHeight = 0;
        int toHeight = BlockchainImpl.getInstance().getHeight();
        Logger.logInfoMessage("process old poc txs from %d to %d", fromHeight , toHeight);

        BlockchainImpl.getInstance().getBlocks(fromHeight,toHeight).forEach(block -> pocSeriesTxProcess(block));
        oldPocTxsProcess = false;
      }
      
    } catch (Exception e) {
      Logger.logDebugMessage("poc tx syn thread interrupted");
    } catch (Throwable t) {
      Logger.logErrorMessage(
          "CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
      System.exit(1);
    }
  };
  
  private static final Runnable peerSynThread = () -> {
    try {

      if (PocHolder.synPeers().size() <= 0) { 
        Logger.logInfoMessage("no needs to syn peer, sleep %d seconds...", peerSynThreadInterval);
      }

      Set<String> connectedPeers = Sets.newHashSet();
      for (String peerAddress : PocHolder.synPeers()) {
        try {
          Peer peer = Peers.findOrCreatePeer(peerAddress, Peers.isUseNATService(peerAddress), true);
          if (peer != null) {
            Peers.addPeer(peer, peerAddress);
            Peers.connectPeer(peer);
          }
          peer = Peers.getPeer(peerAddress, true);
//          _updateCertifiedNodes(peer.getHost(), peer.getType(), -1);
          connectedPeers.add(peer.getHost());
        } catch (Exception e) {
          if(Logger.printNow(PocProcessorImpl.class, 200)) {
            Logger.logDebugMessage("can't connect peer[%s] in peerSynThread, caused by %s", peerAddress, e.getMessage());
          }
          continue;
        }
      }

      if (connectedPeers.size() > 0) {
        PocHolder.removeConnectedPeers(connectedPeers);
        DiskStorageUtil.saveObjToFile(PocHolder.inst, LOCAL_STORAGE_POC_HOLDER);
      }

    } catch (Exception e) {
      Logger.logDebugMessage("peer syn thread interrupted %s", e.getMessage());
    } catch (Throwable t) {
      Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
      System.exit(1);
    }
  };

  /**
   * process poc txs of block
   * @param block block
   */
  private static void pocSeriesTxProcess(Block block) {
    //@link: org.conch.chain.BlockchainProcessorImpl.autoExtensionAppend update the ext tag
    List<? extends Transaction> txs = block.getTransactions();
    Boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
    if(txs == null || txs.size() <= 0 || containPoc == null || !containPoc) {
        return;
    }

    //just process poc tx
    txs.forEach(tx -> pocTxProcess(tx));
  }

  /**
   * PoC tx process
   * @param txid poc tx id
   * @return
   */
  private static boolean pocTxProcess(Long txid) {
    Transaction tx = Conch.getBlockchain().getTransaction(txid);
    if(tx == null) return false;
    return pocTxProcess(tx);
  }

  /**
   * PoC tx process
   * @param tx poc tx
   * @return
   */
  public static boolean pocTxProcess(Transaction tx){
    if(TransactionType.TYPE_POC !=  tx.getType().getType()) {
      return true;
    }
    
    boolean success = false;
    if(PocTxWrapper.SUBTYPE_POC_NODE_TYPE == tx.getType().getSubtype()) {
      success = nodeTypeTxProcess(tx.getHeight(), (PocTxBody.PocNodeType)tx.getAttachment());
    }else if(PocTxWrapper.SUBTYPE_POC_NODE_CONF == tx.getType().getSubtype()){
      success = nodeConfTxProcess(tx.getHeight(), (PocTxBody.PocNodeConf)tx.getAttachment());
    }else if(PocTxWrapper.SUBTYPE_POC_ONLINE_RATE == tx.getType().getSubtype()){
      success = onlineRateTxProcess(tx.getHeight(), (PocTxBody.PocOnlineRate)tx.getAttachment());
    }else if(PocTxWrapper.SUBTYPE_POC_BLOCK_MISSING == tx.getType().getSubtype()){
      success = blockMissingTxProcess(tx.getHeight(), (PocTxBody.PocGenerationMissing)tx.getAttachment());
    }else if(PocTxWrapper.SUBTYPE_POC_WEIGHT_TABLE == tx.getType().getSubtype()){
      PocTxBody.PocWeightTable weightTable = (PocTxBody.PocWeightTable)tx.getAttachment();
      PocCalculator.inst.setCurWeightTable(weightTable,tx.getHeight());
    }
    
    // process later
    if(!success) {
      PocHolder.addDelayProcessTx(tx.getId());
    }
    return success;
  }


  public static void _updateCertifiedNodes(String host, Peer.Type type, int height) {
    if(StringUtils.isEmpty(host)){ 
      Logger.logWarningMessage("peer host[" + host + "] is null, can't find peer!");
      return;
    }

    Peer peer = Peers.getPeer(host, true);
    peer.setType(type);
    if(StringUtils.isEmpty(peer.getBindRsAccount())){
      // connect peer to get account later
      PocHolder.addSynPeer(host);
      Logger.logWarningMessage("bind rs account of peer[host=" + host + "] is null, need syn peer and updated later in Peers.GetHubDetail thread");
    }
    
    // update certified nodes
    PocHolder.addCertifiedPeer(height,peer);
  }


  private static PocScore getPocScoreByPeer(int height, String host){
    Peer peer = Peers.getPeer(host, true);
    if(peer == null || StringUtils.isEmpty(peer.getBindRsAccount())) {
      PocHolder.addSynPeer(host);
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
  private static boolean nodeTypeTxProcess(int height,PocTxBody.PocNodeType pocNodeType){
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
  private static boolean nodeConfTxProcess(int height,PocTxBody.PocNodeConf pocNodeConf){
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
  private static boolean onlineRateTxProcess(int height,PocTxBody.PocOnlineRate onlineRate){
    Peer peer = Peers.getPeer(onlineRate.getIp(), true);

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
  private static boolean blockMissingTxProcess(int height, PocTxBody.PocGenerationMissing pocBlockMissing){
    
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
    PocHolder.scoreMapping(pocScoreToUpdate);
    return true;
  }
  
}
