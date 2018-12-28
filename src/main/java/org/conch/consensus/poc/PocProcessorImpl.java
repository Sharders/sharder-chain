package org.conch.consensus.poc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockImpl;
import org.conch.chain.BlockchainProcessor;
import org.conch.common.Constants;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.Https;
import org.conch.util.IpUtil;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {
  
  
  class PocScore {
    Long accountId;
    int height;
    // SS持有得分
    BigInteger ssScore = BigInteger.ZERO;
    // 节点类型得分
    BigInteger nodeTypeScore = BigInteger.ZERO;
    // 打开服务得分
    BigInteger serverScore = BigInteger.ZERO;
    // 硬件配置得分
    BigInteger hardwareScore = BigInteger.ZERO;
    // 网络配置得分
    BigInteger networkScore = BigInteger.ZERO;
    // 交易处理性能得分
    BigInteger performanceScore = BigInteger.ZERO;
    // 在线率奖惩得分
    BigInteger onlineRateScore = BigInteger.ZERO;
    // 出块错过惩罚分
    BigInteger blockMissScore = BigInteger.ZERO;
    // 分叉收敛惩罚分
    BigInteger bcScore = BigInteger.ZERO;
    
    public PocScore(){}
    
    public BigInteger total(){
      return ssScore.add(nodeTypeScore).add(serverScore).add(hardwareScore).add(networkScore).add(performanceScore).add(onlineRateScore).add(blockMissScore).add(bcScore);
    }
  }

  /** */
  static final class PocHolder {
    static Map<Integer, Map<Long, PocScore>> pocScoreMap = null;
    static Map<Integer, PocScore> heightScoreMap = null;

    static BigInteger getPocScore(int height, long accountId) {
      if (!pocScoreMap.containsKey(height)) {
        if(Conch.getBlockchain().getHeight() < height) {
          synPocTxNow = true;
        }
        return BigInteger.ZERO;
      }
      return pocScoreMap.get(height).containsKey(accountId)
          ? pocScoreMap.get(height).get(accountId).total()
          : BigInteger.ZERO;
    }

    static void scoreMapping(Transaction tx) {
      nodeHardwareTxProcess();
    }


    static Map<Integer, Map<Long, Peer>> accountPeerMap = new ConcurrentHashMap<>();
    
    
  }

  public static PocProcessorImpl instance = getOrCreate();

  private PocProcessorImpl() {}

  private static synchronized PocProcessorImpl getOrCreate() {
    return instance != null ? instance : new PocProcessorImpl();
  }

  static {
    Conch.getBlockchainProcessor()
        .addListener(PocProcessorImpl::pocTxProcess, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    
    loadExistPocHolder();
  }

  private static void loadExistPocHolder() {
    // TODO load the exist poc object from DB

    // TODO load the poc object from block history if db is null or db is older than current block

    // BlockchainImpl.getInstance().getBlocks()
  }

  // TODO thread to read the all blocks to combine the poc holder

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
  
  public void scoreMapping(Transaction tx) { PocHolder.scoreMapping(tx); }
  
  private static void weightTableMapping(Transaction tx) {
    tx.getBlock().getHeight();
    // read the PocTemplate TX and parse them to PocTemplate object
  }

  private static void validNodeMapping(Transaction tx) {
    tx.getBlock().getHeight();
    // read the PocTemplate TX and parse them to PocTemplate object
  }
  
  public static void init() {
    
    ThreadPool.scheduleThread("PocTxSyn", pocTxSynThread, 10, TimeUnit.MINUTES);
    ThreadPool.scheduleThread("validNodeSyn", validNodeSynThread, 30, TimeUnit.MINUTES);
  }

  private static void pocTxProcess(Block block) {
    //@link: org.conch.chain.BlockchainProcessorImpl.autoExtensionAppend update the ext tag
    Boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
    if(containPoc == null || !containPoc) return;
    
    //just process poc tx
    for(Transaction tx : block.getTransactions()) {
      if(TransactionType.TYPE_POC ==  tx.getType().getType()) {
        weightTableMapping(tx);
        PocHolder.scoreMapping(tx);
      }
    }
  }



  private static void nodeHardwareTxProcess() {
    // TODO read the PocConfigTx and update the hardware and performance info to node
    
    // TODO gee the lifecycle from api.sharder.io and check the above info whether is in the alive.
    // Warning the api.sharder.io if exceed the max lifecycle.
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
  
  public static boolean validNode(String host,Integer height){
    Peer peer = Peers.getPeer(host);
    long peerBindAccountId = peer.getBindedAccountId();
    
    return false;
  }
  
  
  public static boolean addOrUpdateNodeType(int height,String host){
    Peer peer = Peers.getPeer(host);
    
    if(peer == null) {
      //PeerImpl newPeer = Peers.findOrCreatePeer(inetAddress, announcedAddress, useNATService, true);
      //TODO get peer info by host and add it into map later
    }
    
    long peerBindAccountId = peer.getBindedAccountId();
    
    Map<Long, Peer> peerMap = PocHolder.accountPeerMap.get(height);
    if(peerMap == null) peerMap = new ConcurrentHashMap<>();
    peerMap.put(peerBindAccountId,peer);

    PocHolder.accountPeerMap.put(height,peerMap);
    
    return false;
  }
  

  private static void nodeRefresh() {
    // read the PocTemplate TX and parse them to PocTemplate object

    // read the ref PocTx and cal the score to generate accountScoreMap
  }
}
