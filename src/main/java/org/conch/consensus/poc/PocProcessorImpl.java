package org.conch.consensus.poc;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockchainProcessor;

import java.math.BigInteger;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {
    public static PocProcessorImpl instance = getOrCreate();

    private PocProcessorImpl(){}

    private static synchronized PocProcessorImpl getOrCreate(){
        if(instance != null) return instance;

        return new PocProcessorImpl();
    }

    static{
        Conch.getBlockchainProcessor().addListener(
                block -> { scoreMapping(block); },
                BlockchainProcessor.Event.AFTER_BLOCK_APPLY
        );
    }

    @Override
    public BigInteger calPocScore(Account account,int height) {
        //TODO
        //Choice2: use this account's balance to cal


        return BigInteger.ZERO;
    }


    // Listener process
    private static Map pocTemplateMap;
    private static void templateMapping(Block block){
        block.getHeight();
        // read the PocTemplate TX and parse them to PocTemplate object
    }

    private static Map accountScoreMap;
    private static void scoreMapping(Block block){
        block.getHeight();


        // read the ref PocTx and cal the score to generate accountScoreMap

        // use pocTemplateMap and pocTx to cal score

        //>>Choice1: loop the account balance at height
        nodeHardwareTxProcess();
    }

    private static void nodeHardwareTxProcess(){
        //TODO read the PocConfigTx and update the hardware and performance info to node

        //TODO gee the lifecycle from api.sharder.io and check the above info whether is in the alive.
        // Warning the api.sharder.io if exceed the max lifecycle.
    }

    //TODO valid node list holder( extend the current node list) and valid method
    //Thread to run
    private static Map nodeMap;
    private static void nodeRefresh(){
        // read the PocTemplate TX and parse them to PocTemplate object

        // read the ref PocTx and cal the score to generate accountScoreMap
    }


}
