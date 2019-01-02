package org.conch.consensus.poc;

import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/29
 */
public class PocScore {
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
    
    // height : { accountId : pocScore }
    Map<Integer,Map<Long,BigInteger>> historySocre = new ConcurrentHashMap<>();
    
    public PocScore(Long accountId,int height){
        this.accountId = accountId;
        this.height = height;
    }

    public BigInteger total(){
        return ssScore.add(nodeTypeScore).add(serverScore).add(hardwareScore).add(networkScore).add(performanceScore).add(onlineRateScore).add(blockMissScore).add(bcScore);
    }
    
    public BigInteger getTotal(int height,Long accountId){
        Map<Long,BigInteger> map = historySocre.get(height);
        if(map == null) return BigInteger.ZERO;
        BigInteger score = map.get(accountId);
        return score !=null ? score : BigInteger.ZERO;
    }
    
    
    public void nodeConfCal(PocTxBody.PocNodeConf nodeConf){
        PocCalculator.nodeConfCal(this,nodeConf);
    }
    
    public void nodeTypeCal(PocTxBody.PocNodeType nodeType){
        PocCalculator.nodeTypeCal(this,nodeType);
    }
    
    public void onlineRateCal(Peer.Type nodeType,PocTxBody.PocOnlineRate onlineRate){
        PocCalculator.onlineRateCal(this,nodeType,onlineRate);
    }
    
    public void blockMissCal(PocTxBody.PocBlockMiss pocBlockMiss){
        PocCalculator.blockMissCal(this,pocBlockMiss);
    }

    public static void setCurWeightTable(PocTxBody.PocWeightTable weightTable) {
        PocCalculator.setCurWeightTable(weightTable);
    }
    
    
    private void _recordHistoryScore(PocScore another){
        Map<Long,BigInteger> map = historySocre.get(height);
        if(map == null) map = new HashMap<>();

        map.put(accountId,total());
        
        historySocre.put(height,map);
    }

    public void synScoreFrom(PocScore another){
        _recordHistoryScore(another);
        this.ssScore = another.ssScore;
        this.nodeTypeScore = another.nodeTypeScore;
        this.serverScore = another.serverScore;
        this.hardwareScore = another.hardwareScore;
        this.networkScore = another.networkScore;
        this.performanceScore = another.performanceScore;
        this.onlineRateScore = another.onlineRateScore;
        this.blockMissScore = another.blockMissScore;
        this.bcScore = another.bcScore;
    }

    public static BigInteger calEffectiveBalance(Account account , int height) {
        BigInteger balance = BigInteger.ZERO;
        if (account == null) return balance;
        
        long id = SharderPoolProcessor.ownOnePool(account.getId());
        if (id != -1 && SharderPoolProcessor.getSharderPool(id).getState().equals(SharderPoolProcessor.State.WORKING)) {
            balance = BigInteger.valueOf(Math.max(SharderPoolProcessor.getSharderPool(id).getPower() / Constants.ONE_SS, 0))
                    .add(BigInteger.valueOf(Math.max(account.getEffectiveBalanceSS(height), 0)));
        }else {
            balance = BigInteger.valueOf(Math.max(account.getEffectiveBalanceSS(height), 0));
        }
        return balance;
    }

    /** Poc calculator instance **/
    static class PocCalculator {

        // 分制转换率，将10分制 转为 500000000分制（SS总发行量 5亿）， 所以转换率是50000000
        private static final BigInteger POINT_SYSTEM_CONVERSION_RATE = BigInteger.valueOf(50000000L);

        // 百分之除数，在算总分完成后需要除以这个数才是最终分数
        private static final BigInteger PERCENT_DIVISOR = BigInteger.valueOf(100L);

        // 当前使用的权重表模板
        static volatile PocTxBody.PocWeightTable pocWeightTable = null;

        public static void setCurWeightTable(PocTxBody.PocWeightTable weightTable){
            pocWeightTable = weightTable;
        }

        static void nodeTypeCal(PocScore pocScore,PocTxBody.PocNodeType nodeType){
            BigInteger typeScore = BigInteger.ZERO;
            if (nodeType.getType().equals(Peer.Type.FOUNDATION)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.FOUNDATION.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.COMMUNITY)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.COMMUNITY.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.HUB)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.HUB.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.BOX)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.BOX.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.NORMAL)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.NORMAL.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            }
            pocScore.nodeTypeScore = typeScore;
        }

        static void nodeConfCal(PocScore pocScore, PocTxBody.PocNodeConf nodeConf){

            // 打开服务得分
            BigInteger serverScore = BigInteger.ZERO;
            // 硬件配置得分
            BigInteger hardwareScore = BigInteger.ZERO;
            // 网络配置得分
            BigInteger networkScore = BigInteger.ZERO;
            // 交易处理性能得分
            BigInteger performanceScore = BigInteger.ZERO;
            
            Long[] openedServices = nodeConf.getSystemInfo().getOpenServices();
            if (openedServices != null && openedServices.length > 0) {
                
                for (Long serviceCode : openedServices) {
                    BigInteger _scorePreDefined = pocWeightTable.getServerOpenTemplate().get(serviceCode);
                    if(_scorePreDefined == null) continue;
                    serverScore = serverScore.add(_scorePreDefined);
                }
                
                pocScore.serverScore = serverScore.multiply(POINT_SYSTEM_CONVERSION_RATE).multiply(pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.SERVER_OPEN.getValue())).divide(PERCENT_DIVISOR);
            }

            if (nodeConf.getSystemInfo().getCore() >= 8 && nodeConf.getSystemInfo().getAverageMHz() >= 3600 && nodeConf.getSystemInfo().getMemoryTotal() >= 15 && nodeConf.getSystemInfo().getHardDiskSize() >= 10 * 1000) {
                hardwareScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getValue()).multiply(pocWeightTable.getHardwareConfigTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeConf.getSystemInfo().getCore() >= 4 && nodeConf.getSystemInfo().getAverageMHz() >= 3100 && nodeConf.getSystemInfo().getMemoryTotal() >= 7 && nodeConf.getSystemInfo().getHardDiskSize() >= 1000) {
                hardwareScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getValue()).multiply(pocWeightTable.getHardwareConfigTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeConf.getSystemInfo().getCore() >= 2 && nodeConf.getSystemInfo().getAverageMHz() >= 2400 && nodeConf.getSystemInfo().getMemoryTotal() >= 3 && nodeConf.getSystemInfo().getHardDiskSize() >= 100) {
                hardwareScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getValue()).multiply(pocWeightTable.getHardwareConfigTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            }

            pocScore.hardwareScore = hardwareScore;

            if (nodeConf.getSystemInfo().isHadPublicIp()) {
                if (nodeConf.getSystemInfo().getBandWidth() >= 10) {
                    networkScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getValue()).multiply(pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
                } else if (nodeConf.getSystemInfo().getBandWidth() >= 5) {
                    networkScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getValue()).multiply(pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
                } else if (nodeConf.getSystemInfo().getBandWidth() >= 1) {
                    networkScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getValue()).multiply(pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
                }
            } else {
                networkScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getValue()).multiply(pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.POOR.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            }
            pocScore.networkScore = networkScore;

            if (nodeConf.getSystemInfo().getTradePerformance() >= 1000) {
                performanceScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.TX_HANDLE_PERFORMANCE.getValue()).multiply(pocWeightTable.getTxHandlePerformanceTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeConf.getSystemInfo().getTradePerformance() >= 500) {
                performanceScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.TX_HANDLE_PERFORMANCE.getValue()).multiply(pocWeightTable.getTxHandlePerformanceTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeConf.getSystemInfo().getTradePerformance() >= 300) {
                performanceScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.TX_HANDLE_PERFORMANCE.getValue()).multiply(pocWeightTable.getTxHandlePerformanceTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            }
            pocScore.performanceScore = performanceScore;

        }

        static void onlineRateCal(PocScore pocScore,Peer.Type nodeType, PocTxBody.PocOnlineRate onlineRate){
            BigInteger onlineRateScore = BigInteger.ZERO;

            if (nodeType.equals(Peer.Type.FOUNDATION)) {
                if (onlineRate.getNetworkRate() >= 9900 && onlineRate.getNetworkRate() < 9999) {
                    onlineRateScore = pocWeightTable.getOnlineRateOfficialTemplate().get(PocTxBody.OnlineStatusDef.FROM_99_00_TO_99_99.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                } else if (onlineRate.getNetworkRate() >= 9700 && onlineRate.getNetworkRate() < 9900) {
                    onlineRateScore = pocWeightTable.getOnlineRateOfficialTemplate().get(PocTxBody.OnlineStatusDef.FROM_97_00_TO_99_00.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                } else if (onlineRate.getNetworkRate() < 9700) {
                    onlineRateScore = pocWeightTable.getOnlineRateOfficialTemplate().get(PocTxBody.OnlineStatusDef.FROM_00_00_TO_97_00.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                }
            } else if (nodeType.equals(Peer.Type.COMMUNITY)) {
                if (onlineRate.getNetworkRate() >= 9700 && onlineRate.getNetworkRate() < 9900) {
                    onlineRateScore = pocWeightTable.getOnlineRateCommunityTemplate().get(PocTxBody.OnlineStatusDef.FROM_97_00_TO_99_00.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                } else if (onlineRate.getNetworkRate() >= 9000 && onlineRate.getNetworkRate() < 9700) {
                    onlineRateScore = pocWeightTable.getOnlineRateCommunityTemplate().get(PocTxBody.OnlineStatusDef.FROM_90_00_TO_97_00.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                } else if (onlineRate.getNetworkRate() < 9000) {
                    onlineRateScore = pocWeightTable.getOnlineRateCommunityTemplate().get(PocTxBody.OnlineStatusDef.FROM_00_00_TO_90_00.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                }
            } else if (nodeType.equals(Peer.Type.HUB) || nodeType.equals(Peer.Type.BOX)) {
                if (onlineRate.getNetworkRate() >= 9900) {
                    onlineRateScore = pocWeightTable.getOnlineRateHubBoxTemplate().get(PocTxBody.OnlineStatusDef.FROM_99_00_TO_100.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                } else if (onlineRate.getNetworkRate() >= 9700) {
                    onlineRateScore = pocWeightTable.getOnlineRateHubBoxTemplate().get(PocTxBody.OnlineStatusDef.FROM_97_00_TO_100.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                } else if (onlineRate.getNetworkRate() < 9000) {
                    onlineRateScore = pocWeightTable.getOnlineRateHubBoxTemplate().get(PocTxBody.OnlineStatusDef.FROM_00_00_TO_90_00.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                }
            } else if (nodeType.equals(Peer.Type.NORMAL)) {
                if (onlineRate.getNetworkRate() >= 9700) {
                    onlineRateScore = pocWeightTable.getOnlineRateNormalTemplate().get(PocTxBody.OnlineStatusDef.FROM_97_00_TO_100.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                } else if (onlineRate.getNetworkRate() >= 9000) {
                    onlineRateScore = pocWeightTable.getOnlineRateNormalTemplate().get(PocTxBody.OnlineStatusDef.FROM_90_00_TO_100.getValue()).multiply(POINT_SYSTEM_CONVERSION_RATE);
                }
            }
            pocScore.onlineRateScore = onlineRateScore;
        }
        
        static Map<Long,Integer> missBlockMap = new HashMap<>();

        static void blockMissCal(PocScore pocScore,PocTxBody.PocBlockMiss blockMiss){
            Long accountId = pocScore.accountId;
            Integer missCount = 0;
            if(missBlockMap.containsKey(accountId)) missCount = missBlockMap.get(accountId);
            missCount++;

            BigInteger missBlcokScore = BigInteger.ZERO; 
            /** 低: 1块/月 累积丢失数小于 3 , 中: 小于3块/周 累积丢失数小于 10 , 高: 大于3块每周 累积丢失数大于 10 
             *  只实现了累积的判断，还未实现一段时间丢失率的判断检查
             * */
            if(missCount <= 3){
                missBlcokScore = pocWeightTable.getBlockingMissTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            }else if(missCount > 3 && missCount <= 10){
                missBlcokScore = pocWeightTable.getBlockingMissTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            }else if(missCount > 10){
                missBlcokScore = pocWeightTable.getBlockingMissTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel()).multiply(POINT_SYSTEM_CONVERSION_RATE);
            }
            pocScore.blockMissScore = missBlcokScore;
            // TODO xy impl miss_block check in interval
        }

        static void bcCal(PocScore pocScore, Account account, PocTxBody.PocBC pocBC){
            //TODO un-impl now -20181230
        }

    }
}
