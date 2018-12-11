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

    private static final String COLON = ":";

    // 分制转换率，将10分制 转为 500000000分制（SS总发行量 5亿）， 所以转换率是50000000
    private static final BigInteger POINT_SYSTEM_CONVERSION_RATE = BigInteger.valueOf(50000000L);

    // 百分之除数，在算总分完成后需要除以这个数才是最终分数
    private static final BigInteger PERCENT_DIVISOR = BigInteger.valueOf(100L);

    // POC分数ss持有权重百分比， 先不算百分之，后面加完了统一除
    private static final BigInteger SS_HOLD_PERCENT = BigInteger.valueOf(40L);

    // POC分数节点类型权重百分比， 先不算百分之，后面加完了统一除
    private static final BigInteger NODE_TYPE_PERCENT = BigInteger.valueOf(25L);
    // POC节点分数
    private static final BigInteger NODE_TYPE_FOUNDATION_SCORE = BigInteger.TEN.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger NODE_TYPE_COMMUNITY_SCORE = BigInteger.valueOf(8L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger NODE_TYPE_HUB_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger NODE_TYPE_BOX_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger NODE_TYPE_COMMON_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NODE_TYPE_PERCENT).divide(PERCENT_DIVISOR);

    // POC分数服务开启权重百分比， 先不算百分之，后面加完了统一除
    private static final BigInteger SERVER_OPEN_PERCENT = BigInteger.valueOf(20L);
    // POC开启服务分数
    private static final BigInteger SERVER_OPEN_SCORE = BigInteger.valueOf(4L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(SERVER_OPEN_PERCENT).divide(PERCENT_DIVISOR);

    // POC分数硬件配置权重百分比， 先不算百分之，后面加完了统一除
    private static final BigInteger HARDWARE_PERCENT = BigInteger.valueOf(5L);
    // POC硬件配置分数
    private static final BigInteger HARDWARE_CONFIGURATION_LOW_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(HARDWARE_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger HARDWARE_CONFIGURATION_MEDIUM_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(HARDWARE_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger HARDWARE_CONFIGURATION_HIGH_SCORE = BigInteger.TEN.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(HARDWARE_PERCENT).divide(PERCENT_DIVISOR);

    // POC分数网络配置权重百分比， 先不算百分之，后面加完了统一除
    private static final BigInteger NETWORK_PERCENT = BigInteger.valueOf(5L);
    // POC网络配置分数
    private static final BigInteger NETWORK_CONFIGURATION_POOR_SCORE = BigInteger.ZERO.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NETWORK_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger NETWORK_CONFIGURATION_LOW_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NETWORK_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger NETWORK_CONFIGURATION_MEDIUM_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NETWORK_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger NETWORK_CONFIGURATION_HIGH_SCORE = BigInteger.TEN.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(NETWORK_PERCENT).divide(PERCENT_DIVISOR);

    // POC分数交易处理性能权重百分比， 先不算百分之，后面加完了统一除
    private static final BigInteger TRADE_HANDLE_PERCENT = BigInteger.valueOf(5L);
    // POC交易处理性能分数
    private static final BigInteger TRADE_HANDLE_LOW_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(TRADE_HANDLE_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger TRADE_HANDLE_MEDIUM_SCORE = BigInteger.valueOf(6L).multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(TRADE_HANDLE_PERCENT).divide(PERCENT_DIVISOR);
    private static final BigInteger TRADE_HANDLE_HIGH_SCORE = BigInteger.TEN.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(TRADE_HANDLE_PERCENT).divide(PERCENT_DIVISOR);

    // POC在线时长分数
    private static final BigInteger FOUNDATION_ONLINE_RATE_GREATER99_LESS9999_SCORE = BigInteger.valueOf(-2L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 基金会节点在线率大于99%小于99.99%
    private static final BigInteger FOUNDATION_ONLINE_RATE_GREATER97_LESS99_SCORE = BigInteger.valueOf(-5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 基金会节点在线率大于97%小于99%
    private static final BigInteger FOUNDATION_ONLINE_RATE_LESS97_SCORE = BigInteger.valueOf(-10L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 基金会节点在线率小于97%
    private static final BigInteger COMMUNITY_ONLINE_RATE_GREATER97_LESS99_SCORE = BigInteger.valueOf(-2L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 社区节点在线率大于97%小于99%
    private static final BigInteger COMMUNITY_ONLINE_RATE_GREATER90_LESS97_SCORE = BigInteger.valueOf(-5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 社区节点在线率大于90%小于97%
    private static final BigInteger COMMUNITY_ONLINE_RATE_LESS90_SCORE = BigInteger.valueOf(-10L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 社区节点在线率小于90%
    private static final BigInteger HUB_BOX_ONLINE_RATE_GREATER99_SCORE = BigInteger.valueOf(5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // HUB/BOX节点在线率大于99%
    private static final BigInteger HUB_BOX_ONLINE_RATE_GREATER97_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE); // HUB/BOX节点在线率大于97%
    private static final BigInteger HUB_BOX_ONLINE_RATE_LESS90_SCORE = BigInteger.valueOf(-5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // HUB/BOX节点在线率小于90%
    private static final BigInteger COMMON_ONLINE_RATE_GREATER97_SCORE = BigInteger.valueOf(5L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 普通节点在线率大于97%
    private static final BigInteger COMMON_ONLINE_RATE_GREATER90_SCORE = BigInteger.valueOf(3L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 普通节点在线率大于90%

    // POC出块错过惩罚分
    private static final BigInteger BLOCKING_MISS_LOW_SCORE = BigInteger.valueOf(-3L).multiply(POINT_SYSTEM_CONVERSION_RATE);
    private static final BigInteger BLOCKING_MISS_MEDIUM_SCORE = BigInteger.valueOf(-6L).multiply(POINT_SYSTEM_CONVERSION_RATE);
    private static final BigInteger BLOCKING_MISS_HIGH_SCORE = BigInteger.valueOf(-10L).multiply(POINT_SYSTEM_CONVERSION_RATE);

    // POC分叉收敛惩罚分
    private static final BigInteger BIFURCATION_CONVERGENCE_SLOW_SCORE = BigInteger.valueOf(-6L).multiply(POINT_SYSTEM_CONVERSION_RATE);
    private static final BigInteger BIFURCATION_CONVERGENCE_MEDIUM_SCORE = BigInteger.valueOf(-3L).multiply(POINT_SYSTEM_CONVERSION_RATE);
    private static final BigInteger BIFURCATION_CONVERGENCE_HARD_SCORE = BigInteger.valueOf(-10L).multiply(POINT_SYSTEM_CONVERSION_RATE); // 硬分叉

    private static final Map<Integer, Map<Long, BigInteger>> SCORE_MAP = new HashMap<>();

    private static final Map<Integer, Map<Long, Long>> BALANCE_MAP = new HashMap<>();

    private static final Map<Integer, Map<String, Attachment.PocNodeConfiguration>> POC_CONFIG_MAP = new HashMap<>();

    private static final Map<Integer, Map<String, Attachment.PocWeight>> POC_WEIGHT_MAP = new HashMap<>();

    private static final Map<Integer, Map<String, Attachment.PocBlockingMiss>> POC_BLOCKING_MISS_MAP = new HashMap<>();

    private static final Map<Integer, Map<String, Attachment.PocBifuractionOfConvergence>> POC_BIFURACTION_OF_CONVERGENCE_MAP = new HashMap<>();

    private static final Map<Integer, Map<String, Attachment.PocOnlineRate>> POC_ONLINE_RATE_MAP = new HashMap<>();

    private static final Map<Integer, Map<Long, String>> ACCOUNT_NODE_MAP = new HashMap<>();

    public static PocProcessorImpl instance = getOrCreate();

    private PocProcessorImpl(){}

    private static synchronized PocProcessorImpl getOrCreate(){
        return instance != null? instance: new PocProcessorImpl();
    }

    static{
        Conch.getBlockchainProcessor().addListener(PocProcessorImpl::scoreMapping, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    @Override
    public BigInteger calPocScore(Account account, int height) {

        if (SCORE_MAP.containsKey(height) && SCORE_MAP.get(height).containsKey(account.getId())) {
            return SCORE_MAP.get(height).get(account.getId());
        }

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
        BigInteger tradeScore = BigInteger.ZERO;

        // 在线率奖惩得分
        BigInteger onlineRateScore = BigInteger.ZERO;

        // 出块错过惩罚分
        BigInteger blockingMissScore = BigInteger.ZERO;

        // 分叉收敛惩罚分
        BigInteger bifuractionConvergenceScore = BigInteger.ZERO;

        if (ACCOUNT_NODE_MAP.containsKey(height) && ACCOUNT_NODE_MAP.get(height).containsKey(account.getId())) {
            String node = ACCOUNT_NODE_MAP.get(height).get(account.getId());

            if (BALANCE_MAP.containsKey(height) && BALANCE_MAP.get(height).containsKey(account.getId())) {
                ssScore = BigInteger.valueOf(BALANCE_MAP.get(height).get(account.getId())).multiply(SS_HOLD_PERCENT).divide(PERCENT_DIVISOR);
            }

            if (POC_CONFIG_MAP.containsKey(height) && POC_CONFIG_MAP.get(height).containsKey(node)) {
                Attachment.PocNodeConfiguration pocNodeConfiguration = POC_CONFIG_MAP.get(height).get(node);

                if (pocNodeConfiguration.getDeviceInfo().getType() == 1) {
                    nodeTypeScore = NODE_TYPE_FOUNDATION_SCORE;
                } else if (pocNodeConfiguration.getDeviceInfo().getType() == 2) {
                    nodeTypeScore = NODE_TYPE_COMMUNITY_SCORE;
                } else if (pocNodeConfiguration.getDeviceInfo().getType() == 3) {
                    nodeTypeScore = NODE_TYPE_HUB_SCORE;
                } else if (pocNodeConfiguration.getDeviceInfo().getType() == 4) {
                    nodeTypeScore = NODE_TYPE_BOX_SCORE;
                } else if (pocNodeConfiguration.getDeviceInfo().getType() == 5) {
                    nodeTypeScore = NODE_TYPE_COMMON_SCORE;
                }

                if (pocNodeConfiguration.getDeviceInfo().isServerOpen()) {
                    serverScore = SERVER_OPEN_SCORE;
                }

                if (pocNodeConfiguration.getSystemInfo().getCore() >= 8 && pocNodeConfiguration.getSystemInfo().getAverageMHz() >= 3600 && pocNodeConfiguration.getSystemInfo().getMemoryTotal() >= 15 && pocNodeConfiguration.getSystemInfo().getHardDiskSize() >= 10 * 1000) {
                    hardwareScore = HARDWARE_CONFIGURATION_HIGH_SCORE;
                } else if (pocNodeConfiguration.getSystemInfo().getCore() >= 4 && pocNodeConfiguration.getSystemInfo().getAverageMHz() >= 3100 && pocNodeConfiguration.getSystemInfo().getMemoryTotal() >= 7 && pocNodeConfiguration.getSystemInfo().getHardDiskSize() >= 1000) {
                    hardwareScore = HARDWARE_CONFIGURATION_MEDIUM_SCORE;
                } else if (pocNodeConfiguration.getSystemInfo().getCore() >= 2 && pocNodeConfiguration.getSystemInfo().getAverageMHz() >= 2400 && pocNodeConfiguration.getSystemInfo().getMemoryTotal() >= 4 && pocNodeConfiguration.getSystemInfo().getHardDiskSize() >= 100) {
                    hardwareScore = HARDWARE_CONFIGURATION_LOW_SCORE;
                }

                if (pocNodeConfiguration.getDeviceInfo().getHadPublicIp()) {
                    if (pocNodeConfiguration.getDeviceInfo().getBandWidth() >= 10) {
                        networkScore = NETWORK_CONFIGURATION_HIGH_SCORE;
                    } else if (pocNodeConfiguration.getDeviceInfo().getBandWidth() >= 5) {
                        networkScore = NETWORK_CONFIGURATION_MEDIUM_SCORE;
                    } else if (pocNodeConfiguration.getDeviceInfo().getBandWidth() >= 1) {
                        networkScore = NETWORK_CONFIGURATION_LOW_SCORE;
                    }
                } else {
                    networkScore = NETWORK_CONFIGURATION_POOR_SCORE;
                }

                if (pocNodeConfiguration.getDeviceInfo().getTradePerformance() >= 1000) {
                    tradeScore = TRADE_HANDLE_HIGH_SCORE;
                } else if (pocNodeConfiguration.getDeviceInfo().getTradePerformance() >= 500) {
                    tradeScore = TRADE_HANDLE_MEDIUM_SCORE;
                } else if (pocNodeConfiguration.getDeviceInfo().getTradePerformance() >= 300) {
                    tradeScore = TRADE_HANDLE_LOW_SCORE;
                }

                if (POC_ONLINE_RATE_MAP.containsKey(height) && POC_ONLINE_RATE_MAP.get(height).containsKey(node)) {
                    Attachment.PocOnlineRate pocOnlineRate = POC_ONLINE_RATE_MAP.get(height).get(node);
                    if (pocNodeConfiguration.getDeviceInfo().getType() == 1) {
                        if (pocOnlineRate.getNetworkRate() >= 9900 && pocOnlineRate.getNetworkRate() < 9999) { // 99% ~ 99.99%
                            onlineRateScore = FOUNDATION_ONLINE_RATE_GREATER99_LESS9999_SCORE;
                        } else if (pocOnlineRate.getNetworkRate() >= 9700 && pocOnlineRate.getNetworkRate() < 9900) { // 97% ~ 99%
                            onlineRateScore = FOUNDATION_ONLINE_RATE_GREATER97_LESS99_SCORE;
                        } else if (pocOnlineRate.getNetworkRate() < 9700) { // < 97%
                            onlineRateScore = FOUNDATION_ONLINE_RATE_LESS97_SCORE;
                        }
                    } else if (pocNodeConfiguration.getDeviceInfo().getType() == 2) {
                        if (pocOnlineRate.getNetworkRate() >= 9700 && pocOnlineRate.getNetworkRate() < 9900) { // 97% ~ 99%
                            onlineRateScore = COMMUNITY_ONLINE_RATE_GREATER97_LESS99_SCORE;
                        } else if (pocOnlineRate.getNetworkRate() >= 9000 && pocOnlineRate.getNetworkRate() < 9700) { // 90% ~ 97%
                            onlineRateScore = COMMUNITY_ONLINE_RATE_GREATER90_LESS97_SCORE;
                        } else if (pocOnlineRate.getNetworkRate() < 9000) { // < 90%
                            onlineRateScore = COMMUNITY_ONLINE_RATE_LESS90_SCORE;
                        }
                    } else if (pocNodeConfiguration.getDeviceInfo().getType() == 3 || pocNodeConfiguration.getDeviceInfo().getType() == 4) {
                        if (pocOnlineRate.getNetworkRate() >= 9900) { // > 99%
                            onlineRateScore = HUB_BOX_ONLINE_RATE_GREATER99_SCORE;
                        } else if (pocOnlineRate.getNetworkRate() >= 9700) { // > 97%
                            onlineRateScore = HUB_BOX_ONLINE_RATE_GREATER97_SCORE;
                        } else if (pocOnlineRate.getNetworkRate() < 9000) { // < 90%
                            onlineRateScore = HUB_BOX_ONLINE_RATE_LESS90_SCORE;
                        }
                    } else if (pocNodeConfiguration.getDeviceInfo().getType() == 5) {
                        if (pocOnlineRate.getNetworkRate() >= 9700) { // > 97%
                            onlineRateScore = COMMON_ONLINE_RATE_GREATER97_SCORE;
                        }
                        if (pocOnlineRate.getNetworkRate() >= 9000) { // > 90%
                            onlineRateScore = COMMON_ONLINE_RATE_GREATER90_SCORE;
                        }
                    }
                }

                if (POC_BLOCKING_MISS_MAP.containsKey(height) && POC_BLOCKING_MISS_MAP.get(height).containsKey(node)) {
                    Attachment.PocBlockingMiss pocBlockingMiss =  POC_BLOCKING_MISS_MAP.get(height).get(node);
                    if (pocBlockingMiss.getMissLevel() == 1) {
                        blockingMissScore = BLOCKING_MISS_LOW_SCORE;
                    } else if (pocBlockingMiss.getMissLevel() == 2) {
                        blockingMissScore = BLOCKING_MISS_MEDIUM_SCORE;
                    } else if (pocBlockingMiss.getMissLevel() == 3) {
                        blockingMissScore = BLOCKING_MISS_HIGH_SCORE;
                    }
                }

                if (POC_BIFURACTION_OF_CONVERGENCE_MAP.containsKey(height) && POC_BIFURACTION_OF_CONVERGENCE_MAP.get(height).containsKey(node)) {
                    Attachment.PocBifuractionOfConvergence pocBifuractionOfConvergence =  POC_BIFURACTION_OF_CONVERGENCE_MAP.get(height).get(node);
                    if (pocBifuractionOfConvergence.getSpeed() == 1) {
                        bifuractionConvergenceScore = BIFURCATION_CONVERGENCE_HARD_SCORE;
                    } else if (pocBifuractionOfConvergence.getSpeed() == 2) {
                        bifuractionConvergenceScore = BIFURCATION_CONVERGENCE_SLOW_SCORE;
                    } else if (pocBifuractionOfConvergence.getSpeed() == 3) {
                        bifuractionConvergenceScore = BIFURCATION_CONVERGENCE_MEDIUM_SCORE;
                    }
                }

                Attachment.PocWeight pocW = new Attachment.PocWeight(pocNodeConfiguration.getIp(), pocNodeConfiguration.getPort(), nodeTypeScore, serverScore, hardwareScore, networkScore, tradeScore, ssScore, blockingMissScore, bifuractionConvergenceScore, onlineRateScore);
                Map<String, Attachment.PocWeight> pocWeight = new HashMap<>();
                if (POC_WEIGHT_MAP.containsKey(height)) {
                    pocWeight = POC_WEIGHT_MAP.get(height);
                }
                pocWeight.put(node, pocW);
                POC_WEIGHT_MAP.put(height, pocWeight);

            }

        }

        BigInteger totalScore =  nodeTypeScore.add(serverScore).add(hardwareScore).add(networkScore).add(tradeScore).add(ssScore).add(blockingMissScore).add(bifuractionConvergenceScore).add(onlineRateScore);
        Map<Long, BigInteger> score = new HashMap<>();
        if (SCORE_MAP.containsKey(height)) {
            score = SCORE_MAP.get(height);
        }
        score.put(account.getId(), totalScore);
        SCORE_MAP.put(height, score);

        return totalScore;
    }

    public static Attachment.PocNodeConfiguration getPocConfiguration (String ip, String port, int height) {
        if (height < 0) {
            height = Conch.getBlockchain().getHeight();
        }
        if (POC_CONFIG_MAP.containsKey(height) && POC_CONFIG_MAP.get(height).containsKey(ip + COLON + port)) {
            POC_CONFIG_MAP.get(height).get(ip + COLON + port);
        }
        return null;
    }

    public static Attachment.PocWeight getPocWeight (String ip, String port, int height) {
        if (height < 0) {
            height = Conch.getBlockchain().getHeight();
        }
        if (POC_WEIGHT_MAP.containsKey(height) && POC_WEIGHT_MAP.get(height).containsKey(ip + COLON + port)) {
            POC_WEIGHT_MAP.get(height).get(ip + COLON + port);
        }
        return null;
    }

    public static Attachment.PocBlockingMiss getPocBlockingMiss (String ip, String port, int height) {
        if (height < 0) {
            height = Conch.getBlockchain().getHeight();
        }
        if (POC_BLOCKING_MISS_MAP.containsKey(height) && POC_BLOCKING_MISS_MAP.get(height).containsKey(ip + COLON + port)) {
            POC_BLOCKING_MISS_MAP.get(height).get(ip + COLON + port);
        }
        return null;
    }

    public static Attachment.PocBifuractionOfConvergence getPocBOC (String ip, String port, int height) {
        if (height < 0) {
            height = Conch.getBlockchain().getHeight();
        }
        if (POC_BIFURACTION_OF_CONVERGENCE_MAP.containsKey(height) && POC_BIFURACTION_OF_CONVERGENCE_MAP.get(height).containsKey(ip + COLON + port)) {
            POC_BIFURACTION_OF_CONVERGENCE_MAP.get(height).get(ip + COLON + port);
        }
        return null;
    }

    public static Attachment.PocOnlineRate getPocOnlineRate (String ip, String port, int height) {
        if (height < 0) {
            height = Conch.getBlockchain().getHeight();
        }
        if (POC_ONLINE_RATE_MAP.containsKey(height) && POC_ONLINE_RATE_MAP.get(height).containsKey(ip + COLON + port)) {
            POC_ONLINE_RATE_MAP.get(height).get(ip + COLON + port);
        }
        return null;
    }

    // Listener process
    private static Map pocTemplateMap;
    private static void templateMapping(Block block){
        block.getHeight();
        // read the PocTemplate TX and parse them to PocTemplate object
    }

    private static void scoreMapping(Block block){
        for (Transaction transaction: block.getTransactions()) {
            Account account = Account.getAccount(transaction.getSenderId());
            if (transaction.getAttachment() instanceof Attachment.PocOnlineRate) {
                Attachment.PocOnlineRate pocOR = (Attachment.PocOnlineRate) transaction.getAttachment();
                Map<String, Attachment.PocOnlineRate> pocOnlineRate = new HashMap<>();
                if (POC_ONLINE_RATE_MAP.containsKey(block.getHeight())) {
                    pocOnlineRate = POC_ONLINE_RATE_MAP.get(block.getHeight());
                }
                pocOnlineRate.put(pocOR.getIp() + COLON + pocOR.getPort(), pocOR);
                POC_ONLINE_RATE_MAP.put(block.getHeight(), pocOnlineRate);
            }
            if (transaction.getAttachment() instanceof Attachment.PocBlockingMiss) {
                Attachment.PocBlockingMiss pocBM = (Attachment.PocBlockingMiss) transaction.getAttachment();
                Map<String, Attachment.PocBlockingMiss> pocBlockingMiss = new HashMap<>();
                if (POC_BLOCKING_MISS_MAP.containsKey(block.getHeight())) {
                    pocBlockingMiss = POC_BLOCKING_MISS_MAP.get(block.getHeight());
                }
                pocBlockingMiss.put(pocBM.getIp() + COLON + pocBM.getPort(), pocBM);
                POC_BLOCKING_MISS_MAP.put(block.getHeight(), pocBlockingMiss);
            }
            if (transaction.getAttachment() instanceof Attachment.PocBifuractionOfConvergence) {
                Attachment.PocBifuractionOfConvergence pocBOC = (Attachment.PocBifuractionOfConvergence) transaction.getAttachment();
                Map<String, Attachment.PocBifuractionOfConvergence> pocBifuractionOfConvergence = new HashMap<>();
                if (POC_BIFURACTION_OF_CONVERGENCE_MAP.containsKey(block.getHeight())) {
                    pocBifuractionOfConvergence = POC_BIFURACTION_OF_CONVERGENCE_MAP.get(block.getHeight());
                }
                pocBifuractionOfConvergence.put(pocBOC.getIp() + COLON + pocBOC.getPort(), pocBOC);
                POC_BIFURACTION_OF_CONVERGENCE_MAP.put(block.getHeight(), pocBifuractionOfConvergence);
            }
            if (transaction.getAttachment() instanceof Attachment.PocNodeConfiguration) {
                Attachment.PocNodeConfiguration pocNodeConfiguration = (Attachment.PocNodeConfiguration) transaction.getAttachment();

                Map<Long, String> accountNode = new HashMap<>();
                if (ACCOUNT_NODE_MAP.containsKey(block.getHeight())) {
                    accountNode = ACCOUNT_NODE_MAP.get(block.getHeight());
                }
                accountNode.put(account.getId(), pocNodeConfiguration.getIp() + COLON + pocNodeConfiguration.getPort());
                ACCOUNT_NODE_MAP.put(block.getHeight(), accountNode);

                Map<String, Attachment.PocNodeConfiguration> pocConfig = new HashMap<>();
                if (POC_CONFIG_MAP.containsKey(block.getHeight())) {
                    pocConfig = POC_CONFIG_MAP.get(block.getHeight());
                }
                pocConfig.put(pocNodeConfiguration.getIp() + COLON + pocNodeConfiguration.getPort(), pocNodeConfiguration);
                POC_CONFIG_MAP.put(block.getHeight(), pocConfig);

                Map<Long, Long> balance = new HashMap<>();
                if (BALANCE_MAP.containsKey(block.getHeight())) {
                    balance = BALANCE_MAP.get(block.getHeight());
                }
                balance.put(account.getId(), account.getBalanceNQT());
                BALANCE_MAP.put(block.getHeight(), balance);
            }
        }

        for (Transaction transaction: block.getTransactions()) {
            if (transaction.getAttachment() instanceof Attachment.PocNodeConfiguration) {
                Account account = Account.getAccount(transaction.getSenderId());
                PocProcessorImpl.getOrCreate().calPocScore(account, block.getHeight());
            }
        }

        nodeHardwareTxProcess();
    }

    private static void nodeHardwareTxProcess(){

        //

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
