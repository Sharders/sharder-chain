package org.conch.consensus.poc;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockchainProcessor;
import org.conch.tx.Transaction;

import java.math.BigInteger;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {

    /**
     * 
     */
    private static final class PocHolder {
        static Map<Integer, Map<Long, BigInteger>> pocScoreMap = null;

        static BigInteger getPocScore(int height, long accountId){
            if(!pocScoreMap.containsKey(height)) {
                //TODO notify to compare current height, then parse the poc tx
                return BigInteger.ZERO;
            }
            return pocScoreMap.get(height).containsKey(accountId) ? pocScoreMap.get(height).get(accountId) : BigInteger.ZERO;
        }
    }
    
    public static PocProcessorImpl instance = getOrCreate();

    private PocProcessorImpl(){}

    private static synchronized PocProcessorImpl getOrCreate(){
        return instance != null? instance: new PocProcessorImpl();
    }

    static{
        Conch.getBlockchainProcessor().addListener(PocProcessorImpl::pocTxProcess, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
        
        loadExistPocHolder();
    }
    
    
    private static void loadExistPocHolder(){
        //TODO load the exist poc object from DB
        
        //TODO load the poc object from block history if db is null or db is older than current block
    
//        BlockchainImpl.getInstance().getBlocks()
    }
    
    
    //TODO thread to read the all blocks to combine the poc holder


    @Override
    public BigInteger calPocScore(Account account, int height) {
        return PocHolder.getPocScore(height,account.getId());
    }



    public static void init(){

    }
    
    private static void pocTxProcess(Block block){
        weightTableMapping(block);
        scoreMapping(block);
    }


    private static void scoreMapping(Block block){
        for (Transaction transaction: block.getTransactions()) {
            Account account = Account.getAccount(transaction.getSenderId());
            
        }

        nodeHardwareTxProcess();
    }


    // Listener process
    private static void weightTableMapping(Block block){
        block.getHeight();
        // read the PocTemplate TX and parse them to PocTemplate object
    }

    private static void nodeHardwareTxProcess(){
        //TODO read the PocConfigTx and update the hardware and performance info to node

        //TODO gee the lifecycle from api.sharder.io and check the above info whether is in the alive.
        // Warning the api.sharder.io if exceed the max lifecycle.
    }

    //TODO valid node list holder( extend the current node list) and valid method
    //Thread to run
    private static final Map<Integer, Map<Long, String>> ACCOUNT_NODE_MAP = null;
    private static void nodeRefresh(){
        // read the PocTemplate TX and parse them to PocTemplate object

        // read the ref PocTx and cal the score to generate accountScoreMap
    }


}
