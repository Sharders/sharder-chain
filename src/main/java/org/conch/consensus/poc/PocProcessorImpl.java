package org.conch.consensus.poc;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockchainProcessor;
import org.conch.consensus.poc.tx.PocTx;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {

    // POC分数ss持有权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger SS_HOLD_PERCENT = new BigInteger("40");
    // POC分数节点类型权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger NODE_TYPE_PERCENT = new BigInteger("25");
    // POC分数服务开启权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger SERVER_OPEN_PERCENT = new BigInteger("10");
    // POC分数在线时长权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger ONLINE_PERCENT = new BigInteger("10");
    // POC分数硬件配置权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger HARDWARE_PERCENT = new BigInteger("5");
    // POC分数网络配置权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger NETWORK_PERCENT = new BigInteger("5");
    // POC分数交易处理性能权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger TRADE_HANDLE_PERCENT = new BigInteger("5");

    // 百分之除数，在算总分完成后需要除以这个数才是最终分数
    public static final BigInteger PERCENT_DIVISOR = new BigInteger("100");

    // 分制转换率，将10分制 转为 500000000分制（SS总发行量 5亿）， 所以转换率是50000000
    public static final BigInteger POINT_SYSTEM_CONVERSION_RATE = new BigInteger("50000000");




    public static PocProcessorImpl instance = getOrCreate();

    private PocProcessorImpl(){}

    private static synchronized PocProcessorImpl getOrCreate(){
        if(instance != null) return instance;

        return new PocProcessorImpl();
    }

    static{
        Conch.getBlockchainProcessor().addListener(PocProcessorImpl::scoreMapping, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
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
        int height = block.getHeight();

        List<Transaction> pocNodeTransactions = new ArrayList<>();

        for (Transaction transaction: block.getTransactions()) {
            if (transaction.getType().getType() == TransactionType.TYPE_POC && transaction.getType().getSubtype() == PocTx.POC_NODE_CONFIGURATION.getSubtype()) {
                pocNodeTransactions.add(transaction);
                Attachment.PocNodeConfiguration configuration = (Attachment.PocNodeConfiguration) transaction.getAttachment();

                Account account = Account.getAccount(transaction.getSenderId());
                // account 怎样关联到 config ?
                long balance = account.getBalanceNQT();
            }
        }

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
