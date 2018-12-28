package org.conch.consensus.poc;

import org.conch.account.Account;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.peer.Peer;
import org.conch.tx.Transaction;

import java.math.BigInteger;

public class PocCaculator {

    // 分制转换率，将10分制 转为 500000000分制（SS总发行量 5亿）， 所以转换率是50000000
    private static final BigInteger POINT_SYSTEM_CONVERSION_RATE = BigInteger.valueOf(50000000L);

    // 百分之除数，在算总分完成后需要除以这个数才是最终分数
    private static final BigInteger PERCENT_DIVISOR = BigInteger.valueOf(100L);

    // 当前使用的权重表模板
    static volatile PocTxBody.PocWeightTable pocWeightTable = null;

    public static void setCurWeightTable(PocTxBody.PocWeightTable weightTable){
            pocWeightTable = weightTable;
        }

    private static BigInteger nodeTypeCal(PocTxBody.PocNodeType nodeType){
        if (nodeType.getType().equals(Peer.Type.OFFICIAL)) {
            return pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getOptionValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.OFFICIAL.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        } else if (nodeType.getType().equals(Peer.Type.COMMUNITY)) {
            return pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getOptionValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.COMMUNITY.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        } else if (nodeType.getType().equals(Peer.Type.HUB)) {
            return pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getOptionValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.HUB.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        } else if (nodeType.getType().equals(Peer.Type.BOX)) {
            return pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getOptionValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.BOX.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        } else if (nodeType.getType().equals(Peer.Type.NORMAL)) {
            return pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getOptionValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.NORMAL.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        }
        return BigInteger.ZERO;
    }

    private static BigInteger nodeConfCal(PocTxBody.PocNodeType nodeType, PocTxBody.PocNodeConf nodeConf){

        // 打开服务得分
        BigInteger serverScore = BigInteger.ZERO;
        // 硬件配置得分
        BigInteger hardwareScore = BigInteger.ZERO;
        // 网络配置得分
        BigInteger networkScore = BigInteger.ZERO;
        // 交易处理性能得分
        BigInteger txScore = BigInteger.ZERO;

        if (nodeConf.getSystemInfo().isServerOpen()) {
            if (nodeType.getType().equals(Peer.Type.OFFICIAL)) {
                serverScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.SERVER_OPEN.getOptionValue()).multiply(pocWeightTable.getServerOpenTemplate().get(Peer.Type.OFFICIAL.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.COMMUNITY)) {
                serverScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.SERVER_OPEN.getOptionValue()).multiply(pocWeightTable.getServerOpenTemplate().get(Peer.Type.COMMUNITY.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.HUB)) {
                serverScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.SERVER_OPEN.getOptionValue()).multiply(pocWeightTable.getServerOpenTemplate().get(Peer.Type.HUB.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.BOX)) {
                serverScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.SERVER_OPEN.getOptionValue()).multiply(pocWeightTable.getServerOpenTemplate().get(Peer.Type.BOX.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.NORMAL)) {
                serverScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.SERVER_OPEN.getOptionValue()).multiply(pocWeightTable.getServerOpenTemplate().get(Peer.Type.NORMAL.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            }
        }

        if (nodeConf.getSystemInfo().getCore() >= 8 && nodeConf.getSystemInfo().getAverageMHz() >= 3600 && nodeConf.getSystemInfo().getMemoryTotal() >= 15 && nodeConf.getSystemInfo().getHardDiskSize() >= 10 * 1000) {
            hardwareScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getOptionValue()).multiply(pocWeightTable.getHardwareConfigTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        } else if (nodeConf.getSystemInfo().getCore() >= 4 && nodeConf.getSystemInfo().getAverageMHz() >= 3100 && nodeConf.getSystemInfo().getMemoryTotal() >= 7 && nodeConf.getSystemInfo().getHardDiskSize() >= 1000) {
            hardwareScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getOptionValue()).multiply(pocWeightTable.getHardwareConfigTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        } else if (nodeConf.getSystemInfo().getCore() >= 2 && nodeConf.getSystemInfo().getAverageMHz() >= 2400 && nodeConf.getSystemInfo().getMemoryTotal() >= 3 && nodeConf.getSystemInfo().getHardDiskSize() >= 100) {
            hardwareScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getOptionValue()).multiply(pocWeightTable.getHardwareConfigTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        }

        if (nodeConf.getSystemInfo().isHadPublicIp()) {
            if (nodeConf.getSystemInfo().getBandWidth() >= 10) {
                networkScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getOptionValue()).multiply(pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeConf.getSystemInfo().getBandWidth() >= 5) {
                networkScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getOptionValue()).multiply(pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeConf.getSystemInfo().getBandWidth() >= 1) {
                networkScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getOptionValue()).multiply(pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            }
        } else {
            networkScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getOptionValue()).multiply(pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.POOR.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        }

        if (nodeConf.getSystemInfo().getTradePerformance() >= 1000) {
            txScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.TX_HANDLE_PERFORMANCE.getOptionValue()).multiply(pocWeightTable.getTxHandlePerformanceTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        } else if (nodeConf.getSystemInfo().getTradePerformance() >= 500) {
            txScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.TX_HANDLE_PERFORMANCE.getOptionValue()).multiply(pocWeightTable.getTxHandlePerformanceTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        } else if (nodeConf.getSystemInfo().getTradePerformance() >= 300) {
            txScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.TX_HANDLE_PERFORMANCE.getOptionValue()).multiply(pocWeightTable.getTxHandlePerformanceTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        }

        return serverScore.add(hardwareScore).add(networkScore).add(txScore);
    }

    private static BigInteger onlineRateCal(PocTxBody.PocNodeType nodeType, PocTxBody.PocOnlineRate onlineRate){
        if (nodeType.getType().equals(Peer.Type.OFFICIAL)) {
            if (onlineRate.getNetworkRate() >= 9900 && onlineRate.getNetworkRate() < 9999) {
                return pocWeightTable.getOnlineRateOfficialTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            } else if (onlineRate.getNetworkRate() >= 9700 && onlineRate.getNetworkRate() < 9900) {
                return pocWeightTable.getOnlineRateOfficialTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            } else if (onlineRate.getNetworkRate() < 9700) {
                return pocWeightTable.getOnlineRateOfficialTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            }
        } else if (nodeType.getType().equals(Peer.Type.COMMUNITY)) {
            if (onlineRate.getNetworkRate() >= 9700 && onlineRate.getNetworkRate() < 9900) {
                return pocWeightTable.getOnlineRateCommunityTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            } else if (onlineRate.getNetworkRate() >= 9000 && onlineRate.getNetworkRate() < 9700) {
                return pocWeightTable.getOnlineRateCommunityTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            } else if (onlineRate.getNetworkRate() < 9000) {
                return pocWeightTable.getOnlineRateCommunityTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            }
        } else if (nodeType.getType().equals(Peer.Type.HUB) || nodeType.getType().equals(Peer.Type.BOX)) {
            if (onlineRate.getNetworkRate() >= 9900) {
                return pocWeightTable.getOnlineRateHubBoxTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            } else if (onlineRate.getNetworkRate() >= 9700) {
                return pocWeightTable.getOnlineRateHubBoxTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            } else if (onlineRate.getNetworkRate() < 9000) {
                return pocWeightTable.getOnlineRateHubBoxTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            }
        } else if (nodeType.getType().equals(Peer.Type.NORMAL)) {
            if (onlineRate.getNetworkRate() >= 9700) {
                return pocWeightTable.getOnlineRateNormalTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            } else if (onlineRate.getNetworkRate() >= 9000) {
                return pocWeightTable.getOnlineRateNormalTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            }
        }
        return BigInteger.ZERO;
    }

    private static BigInteger blockMissCal(Account account, PocTxBody.PocBlockMiss blockMiss){
        //TODO xy impl
        return BigInteger.ZERO;
    }

    private static BigInteger bcCal(Account account, PocTxBody.PocBC pocBC){
        //TODO un-impl now
        return BigInteger.ZERO;
    }

    public static BigInteger calPocScore(Account account, Transaction transaction) {
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

//            if (ACCOUNT_NODE_MAP.containsKey(height) && ACCOUNT_NODE_MAP.get(height).containsKey(account.getId())) {
//                String node = ACCOUNT_NODE_MAP.get(height).get(account.getId());
//
//                if (BALANCE_MAP.containsKey(height) && BALANCE_MAP.get(height).containsKey(account.getId())) {
//                    ssScore = BigInteger.valueOf(BALANCE_MAP.get(height).get(account.getId())).multiply(SS_HOLD_PERCENT).divide(PERCENT_DIVISOR);
//                }
//
//                if (POC_CONFIG_MAP.containsKey(height) && POC_CONFIG_MAP.get(height).containsKey(node)) {
//                    Attachment.PocNodeConfiguration pocNodeConfiguration = POC_CONFIG_MAP.get(height).get(node);
//
//                    if (pocNodeConfiguration.getDeviceInfo().getType() == 1) {
//                        nodeTypeScore = NODE_TYPE_FOUNDATION_SCORE;
//                    } else if (pocNodeConfiguration.getDeviceInfo().getType() == 2) {
//                        nodeTypeScore = NODE_TYPE_COMMUNITY_SCORE;
//                    } else if (pocNodeConfiguration.getDeviceInfo().getType() == 3) {
//                        nodeTypeScore = NODE_TYPE_HUB_SCORE;
//                    } else if (pocNodeConfiguration.getDeviceInfo().getType() == 4) {
//                        nodeTypeScore = NODE_TYPE_BOX_SCORE;
//                    } else if (pocNodeConfiguration.getDeviceInfo().getType() == 5) {
//                        nodeTypeScore = NODE_TYPE_COMMON_SCORE;
//                    }
//
//                    if (pocNodeConfiguration.getDeviceInfo().isServerOpen()) {
//                        serverScore = SERVER_OPEN_SCORE;
//                    }
//
//                    if (pocNodeConfiguration.getSystemInfo().getCore() >= 8 && pocNodeConfiguration.getSystemInfo().getAverageMHz() >= 3600 && pocNodeConfiguration.getSystemInfo().getMemoryTotal() >= 15 && pocNodeConfiguration.getSystemInfo().getHardDiskSize() >= 10 * 1000) {
//                        hardwareScore = HARDWARE_CONFIGURATION_HIGH_SCORE;
//                    } else if (pocNodeConfiguration.getSystemInfo().getCore() >= 4 && pocNodeConfiguration.getSystemInfo().getAverageMHz() >= 3100 && pocNodeConfiguration.getSystemInfo().getMemoryTotal() >= 7 && pocNodeConfiguration.getSystemInfo().getHardDiskSize() >= 1000) {
//                        hardwareScore = HARDWARE_CONFIGURATION_MEDIUM_SCORE;
//                    } else if (pocNodeConfiguration.getSystemInfo().getCore() >= 2 && pocNodeConfiguration.getSystemInfo().getAverageMHz() >= 2400 && pocNodeConfiguration.getSystemInfo().getMemoryTotal() >= 4 && pocNodeConfiguration.getSystemInfo().getHardDiskSize() >= 100) {
//                        hardwareScore = HARDWARE_CONFIGURATION_LOW_SCORE;
//                    }
//
//                    if (pocNodeConfiguration.getDeviceInfo().getHadPublicIp()) {
//                        if (pocNodeConfiguration.getDeviceInfo().getBandWidth() >= 10) {
//                            networkScore = NETWORK_CONFIGURATION_HIGH_SCORE;
//                        } else if (pocNodeConfiguration.getDeviceInfo().getBandWidth() >= 5) {
//                            networkScore = NETWORK_CONFIGURATION_MEDIUM_SCORE;
//                        } else if (pocNodeConfiguration.getDeviceInfo().getBandWidth() >= 1) {
//                            networkScore = NETWORK_CONFIGURATION_LOW_SCORE;
//                        }
//                    } else {
//                        networkScore = NETWORK_CONFIGURATION_POOR_SCORE;
//                    }
//
//                    if (pocNodeConfiguration.getDeviceInfo().getTradePerformance() >= 1000) {
//                        tradeScore = TRADE_HANDLE_HIGH_SCORE;
//                    } else if (pocNodeConfiguration.getDeviceInfo().getTradePerformance() >= 500) {
//                        tradeScore = TRADE_HANDLE_MEDIUM_SCORE;
//                    } else if (pocNodeConfiguration.getDeviceInfo().getTradePerformance() >= 300) {
//                        tradeScore = TRADE_HANDLE_LOW_SCORE;
//                    }
//
//                    if (POC_ONLINE_RATE_MAP.containsKey(height) && POC_ONLINE_RATE_MAP.get(height).containsKey(node)) {
//                        Attachment.PocOnlineRate pocOnlineRate = POC_ONLINE_RATE_MAP.get(height).get(node);
//                        if (pocNodeConfiguration.getDeviceInfo().getType() == 1) {
//                            if (pocOnlineRate.getNetworkRate() >= 9900 && pocOnlineRate.getNetworkRate() < 9999) { // 99% ~ 99.99%
//                                onlineRateScore = FOUNDATION_ONLINE_RATE_GREATER99_LESS9999_SCORE;
//                            } else if (pocOnlineRate.getNetworkRate() >= 9700 && pocOnlineRate.getNetworkRate() < 9900) { // 97% ~ 99%
//                                onlineRateScore = FOUNDATION_ONLINE_RATE_GREATER97_LESS99_SCORE;
//                            } else if (pocOnlineRate.getNetworkRate() < 9700) { // < 97%
//                                onlineRateScore = FOUNDATION_ONLINE_RATE_LESS97_SCORE;
//                            }
//                        } else if (pocNodeConfiguration.getDeviceInfo().getType() == 2) {
//                            if (pocOnlineRate.getNetworkRate() >= 9700 && pocOnlineRate.getNetworkRate() < 9900) { // 97% ~ 99%
//                                onlineRateScore = COMMUNITY_ONLINE_RATE_GREATER97_LESS99_SCORE;
//                            } else if (pocOnlineRate.getNetworkRate() >= 9000 && pocOnlineRate.getNetworkRate() < 9700) { // 90% ~ 97%
//                                onlineRateScore = COMMUNITY_ONLINE_RATE_GREATER90_LESS97_SCORE;
//                            } else if (pocOnlineRate.getNetworkRate() < 9000) { // < 90%
//                                onlineRateScore = COMMUNITY_ONLINE_RATE_LESS90_SCORE;
//                            }
//                        } else if (pocNodeConfiguration.getDeviceInfo().getType() == 3 || pocNodeConfiguration.getDeviceInfo().getType() == 4) {
//                            if (pocOnlineRate.getNetworkRate() >= 9900) { // > 99%
//                                onlineRateScore = HUB_BOX_ONLINE_RATE_GREATER99_SCORE;
//                            } else if (pocOnlineRate.getNetworkRate() >= 9700) { // > 97%
//                                onlineRateScore = HUB_BOX_ONLINE_RATE_GREATER97_SCORE;
//                            } else if (pocOnlineRate.getNetworkRate() < 9000) { // < 90%
//                                onlineRateScore = HUB_BOX_ONLINE_RATE_LESS90_SCORE;
//                            }
//                        } else if (pocNodeConfiguration.getDeviceInfo().getType() == 5) {
//                            if (pocOnlineRate.getNetworkRate() >= 9700) { // > 97%
//                                onlineRateScore = COMMON_ONLINE_RATE_GREATER97_SCORE;
//                            }
//                            if (pocOnlineRate.getNetworkRate() >= 9000) { // > 90%
//                                onlineRateScore = COMMON_ONLINE_RATE_GREATER90_SCORE;
//                            }
//                        }
//                    }
//
//                    if (POC_BLOCK_MISS_MAP.containsKey(height) && POC_BLOCK_MISS_MAP.get(height).containsKey(node)) {
//                        Attachment.PocBlockingMiss pocBlockingMiss =  POC_BLOCK_MISS_MAP.get(height).get(node);
//                        if (pocBlockingMiss.getMissLevel() == 1) {
//                            blockingMissScore = BLOCKING_MISS_LOW_SCORE;
//                        } else if (pocBlockingMiss.getMissLevel() == 2) {
//                            blockingMissScore = BLOCKING_MISS_MEDIUM_SCORE;
//                        } else if (pocBlockingMiss.getMissLevel() == 3) {
//                            blockingMissScore = BLOCKING_MISS_HIGH_SCORE;
//                        }
//                    }
//
//                    if (POC_BC_MAP.containsKey(height) && POC_BC_MAP.get(height).containsKey(node)) {
//                        Attachment.PocBifuractionOfConvergence pocBifuractionOfConvergence =  POC_BC_MAP.get(height).get(node);
//                        if (pocBifuractionOfConvergence.getSpeed() == 1) {
//                            bifuractionConvergenceScore = BIFURCATION_CONVERGENCE_HARD_SCORE;
//                        } else if (pocBifuractionOfConvergence.getSpeed() == 2) {
//                            bifuractionConvergenceScore = BIFURCATION_CONVERGENCE_SLOW_SCORE;
//                        } else if (pocBifuractionOfConvergence.getSpeed() == 3) {
//                            bifuractionConvergenceScore = BIFURCATION_CONVERGENCE_MEDIUM_SCORE;
//                        }
//                    }
//
//                    Attachment.PocWeight pocWeight = new Attachment.PocWeight(pocNodeConfiguration.getIp(), pocNodeConfiguration.getPort(), nodeTypeScore, serverScore, hardwareScore, networkScore, tradeScore, ssScore, blockingMissScore, bifuractionConvergenceScore, onlineRateScore);
//                    setPocWeight(account, pocWeight, height);
//
//                }
//
//            }
//
//            BigInteger pocScore = nodeTypeScore.add(serverScore).add(hardwareScore).add(networkScore).add(tradeScore).add(ssScore).add(blockingMissScore).add(bifuractionConvergenceScore).add(onlineRateScore);
//
//            setPocScore(account, height, pocScore);
        return BigInteger.ZERO;
    }
}
