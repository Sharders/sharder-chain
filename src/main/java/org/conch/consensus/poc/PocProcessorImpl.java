package org.conch.consensus.poc;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.BlockchainProcessor;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {

    // 分制转换率，将10分制 转为 500000000分制（SS总发行量 5亿）， 所以转换率是50000000
    public static final BigInteger POINT_SYSTEM_CONVERSION_RATE = BigInteger.valueOf(50000000L);

    // 百分之除数，在算总分完成后需要除以这个数才是最终分数
    public static final BigInteger PERCENT_DIVISOR = BigInteger.valueOf(100L);

    // POC分数ss持有权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger SS_HOLD_PERCENT = BigInteger.valueOf(40L);

    // POC分数节点类型权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger NODE_TYPE_PERCENT = BigInteger.valueOf(25L);
    // POC节点分数
    public static final BigInteger NODE_TYPE_FOUNDATION_SCORE = BigInteger.TEN.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger NODE_TYPE_COMMUNITY_SCORE = BigInteger.valueOf(8L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger NODE_TYPE_HUB_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger NODE_TYPE_BOX_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger NODE_TYPE_COMMON_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);

    // POC分数服务开启权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger SERVER_OPEN_PERCENT = BigInteger.valueOf(20L);
    // POC开启服务分数
    public static final BigInteger SERVER_OPEN_SCORE = BigInteger.valueOf(4L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(SERVER_OPEN_PERCENT).divide(PERCENT_DIVISOR);

    // POC分数硬件配置权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger HARDWARE_PERCENT = BigInteger.valueOf(5L);
    // POC硬件配置分数
    public static final BigInteger HARDWARE_CONFIGURATION_LOW_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(HARDWARE_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger HARDWARE_CONFIGURATION_MEDIUM_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(HARDWARE_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger HARDWARE_CONFIGURATION_HIGH_SCORE = BigInteger.TEN.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(HARDWARE_PERCENT).divide(PERCENT_DIVISOR);

    // POC分数网络配置权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger NETWORK_PERCENT = BigInteger.valueOf(5L);
    // POC网络配置分数
    public static final BigInteger NETWORK_CONFIGURATION_POOR_SCORE = BigInteger.ZERO.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NETWORK_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger NETWORK_CONFIGURATION_LOW_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NETWORK_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger NETWORK_CONFIGURATION_MEDIUM_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NETWORK_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger NETWORK_CONFIGURATION_HIGH_SCORE = BigInteger.TEN.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NETWORK_PERCENT).divide(PERCENT_DIVISOR);

    // POC分数交易处理性能权重百分比， 先不算百分之，后面加完了统一除
    public static final BigInteger TRADE_HANDLE_PERCENT = BigInteger.valueOf(5L);
    // POC交易处理性能分数
    public static final BigInteger TRADE_HANDLE_LOW_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(TRADE_HANDLE_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger TRADE_HANDLE_MEDIUM_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(TRADE_HANDLE_PERCENT).divide(PERCENT_DIVISOR);
    public static final BigInteger TRADE_HANDLE_HIGH_SCORE = BigInteger.TEN.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(TRADE_HANDLE_PERCENT).divide(PERCENT_DIVISOR);

    // POC在线时长分数
    public static final BigInteger FOUNDATION_ONLINE_RATE_GREATER99_LESS9999_SCORE = BigInteger.valueOf(-2L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 基金会节点在线率大于99%小于99.99%
    public static final BigInteger FOUNDATION_ONLINE_RATE_GREATER97_LESS99_SCORE = BigInteger.valueOf(-5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 基金会节点在线率大于97%小于99%
    public static final BigInteger FOUNDATION_ONLINE_RATE_LESS97_SCORE = BigInteger.valueOf(-10L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 基金会节点在线率小于97%
    public static final BigInteger COMMUNITY_ONLINE_RATE_GREATER97_LESS99_SCORE = BigInteger.valueOf(-2L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 社区节点在线率大于97%小于99%
    public static final BigInteger COMMUNITY_ONLINE_RATE_GREATER90_LESS97_SCORE = BigInteger.valueOf(-5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 社区节点在线率大于90%小于97%
    public static final BigInteger COMMUNITY_ONLINE_RATE_LESS90_SCORE = BigInteger.valueOf(-10L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 社区节点在线率小于90%
    public static final BigInteger HUB_BOX_ONLINE_RATE_GREATER99_SCORE = BigInteger.valueOf(5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // HUB/BOX节点在线率大于99%
    public static final BigInteger HUB_BOX_ONLINE_RATE_GREATER97_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE); // HUB/BOX节点在线率大于97%
    public static final BigInteger HUB_BOX_ONLINE_RATE_LESS90_SCORE = BigInteger.valueOf(-5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // HUB/BOX节点在线率小于90%
    public static final BigInteger COMMON_ONLINE_RATE_GREATER97_SCORE = BigInteger.valueOf(5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 普通节点在线率大于97%
    public static final BigInteger COMMON_ONLINE_RATE_GREATER90_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 普通节点在线率大于90%

    // POC出块错过惩罚分
    public static final BigInteger BLOCKING_MISS_LOW_SCORE = BigInteger.valueOf(-3L).multiply(POINT_SYSTEM_CONVERSION_RATE);
    public static final BigInteger BLOCKING_MISS_MEDIUM_SCORE = BigInteger.valueOf(-6L).multiply(POINT_SYSTEM_CONVERSION_RATE);
    public static final BigInteger BLOCKING_MISS_HIGH_SCORE = BigInteger.valueOf(-10L).multiply(POINT_SYSTEM_CONVERSION_RATE);

    // POC分叉收敛惩罚分
    public static final BigInteger BIFURCATION_CONVERGENCE_SLOW_SCORE = BigInteger.valueOf(-3L).multiply(POINT_SYSTEM_CONVERSION_RATE);
    public static final BigInteger BIFURCATION_CONVERGENCE_MEDIUM_SCORE = BigInteger.valueOf(-6L).multiply(POINT_SYSTEM_CONVERSION_RATE);
    public static final BigInteger BIFURCATION_CONVERGENCE_HARD_SCORE = BigInteger.valueOf(-10L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 硬分叉

    private static Map<Long, Long> accountBalanceMap = new HashMap<>();

    private static Map<Long, Attachment.PocNodeConfiguration> accountConfigMap = new HashMap<>();

    private static Map<Long, Attachment.PocOnlineRate> accountOnlineMap = new HashMap<>();

    private static Map<Long, Attachment.PocBlockingMiss> accountBlockingMissMap = new HashMap<>();

    private static Map<Long, Attachment.PocBifuractionOfConvergence> accountBocMap = new HashMap<>();

    private static Map<Long, BigInteger> accountScoreMap = new HashMap<>();

    public static PocProcessorImpl instance = getOrCreate();

    private PocProcessorImpl(){}

    private static synchronized PocProcessorImpl getOrCreate(){
        if(instance != null) return instance;

        return new PocProcessorImpl();
    }

    static{
        Conch.getBlockchainProcessor().addListener(PocProcessorImpl::scoreMapping, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    Map<Long, Attachment.PocWeight> scoreMap = new HashMap<>();

    @Override
    public BigInteger calPocScore(Account account,int height) {
        BigInteger ssHold = BigInteger.valueOf(accountBalanceMap.get(account.getId()));
        Attachment.PocNodeConfiguration pocNodeConfiguration = accountConfigMap.get(account.getId());
        BigInteger ssScore = ssHold.multiply(SS_HOLD_PERCENT).divide(PERCENT_DIVISOR);
        if (accountScoreMap.containsKey(account.getId())) {
            accountScoreMap.put(account.getId(), accountScoreMap.get(account.getId()).add(ssScore));
        } else {
            accountScoreMap.put(account.getId(), ssScore);
        }
        return BigInteger.ZERO;
    }

    // Listener process
    private static Map pocTemplateMap;
    private static void templateMapping(Block block){
        block.getHeight();
        // read the PocTemplate TX and parse them to PocTemplate object
    }

    private static void scoreMapping(Block block){
        int height = block.getHeight();
        for (Transaction transaction: block.getTransactions()) {
            Account account = Account.getAccount(transaction.getSenderId());
            if (transaction.getAttachment() instanceof Attachment.PocOnlineRate) {
                accountOnlineMap.put(account.getId(), (Attachment.PocOnlineRate) transaction.getAttachment());
            }
            if (transaction.getAttachment() instanceof Attachment.PocBlockingMiss) {
                accountBlockingMissMap.put(account.getId(), (Attachment.PocBlockingMiss) transaction.getAttachment());
            }
            if (transaction.getAttachment() instanceof Attachment.PocBifuractionOfConvergence) {
                accountBocMap.put(account.getId(), (Attachment.PocBifuractionOfConvergence) transaction.getAttachment());
            }
            if (transaction.getAttachment() instanceof Attachment.PocNodeConfiguration) {
                Attachment.PocNodeConfiguration configuration = (Attachment.PocNodeConfiguration) transaction.getAttachment();
                accountBalanceMap.put(account.getId(), account.getBalanceNQT());
                accountConfigMap.put(account.getId(), configuration);
                PocProcessorImpl.getOrCreate().calPocScore(account, height);
            }
        }



        // read the ref PocTx and cal the score to generate accountScoreMap

        // use pocTemplateMap and pocTx to cal score

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
