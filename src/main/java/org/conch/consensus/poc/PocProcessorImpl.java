package org.conch.consensus.poc;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockImpl;
import org.conch.chain.BlockchainProcessor;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;

import java.math.BigInteger;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {

  /** */
  private static final class PocHolder {
    static Map<Integer, Map<Long, BigInteger>> pocScoreMap = null;

    static BigInteger getPocScore(int height, long accountId) {
      if (!pocScoreMap.containsKey(height)) {
        if(Conch.getBlockchain().getHeight() < height) {
          synPocTxNow = true;
        }
        return BigInteger.ZERO;
      }
      return pocScoreMap.get(height).containsKey(accountId)
          ? pocScoreMap.get(height).get(accountId)
          : BigInteger.ZERO;
    }

    static void scoreMapping(Transaction tx) {

      nodeHardwareTxProcess();
    }
    
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
    return PocHolder.getPocScore(height, account.getId());
  }
  
  public void scoreMapping(Transaction tx) { PocHolder.scoreMapping(tx); }
  
  // Listener process
  private static void weightTableMapping(Transaction tx) {
    tx.getBlock().getHeight();
    // read the PocTemplate TX and parse them to PocTemplate object
  }
  
  public static void init() {
    ThreadPool.scheduleThread("PocTxSyn", pocTxSynThread, 10);
  }

  private static void pocTxProcess(Block block) {
    //@link: org.conch.chain.BlockchainProcessorImpl.autoExtensionAppend update the ext tag
    boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
    if(!containPoc) return;
    
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

  private static void nodeRefresh() {
    // read the PocTemplate TX and parse them to PocTemplate object

    // read the ref PocTx and cal the score to generate accountScoreMap
  }
}
