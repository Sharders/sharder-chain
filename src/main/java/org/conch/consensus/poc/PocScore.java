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

    public PocScore(Long accountId,int height){
        this.accountId = accountId;
        this.height = height;
        this.ssScore = _calBalance(accountId,height);
        ssScoreCal();
    }

    public PocScore(int height, PocScore another){
        this.accountId = another.accountId;
        this.ssScore = another.ssScore;
        this.nodeTypeScore = another.nodeTypeScore;
        this.serverScore = another.serverScore;
        this.hardwareScore = another.hardwareScore;
        this.networkScore = another.networkScore;
        this.performanceScore = another.performanceScore;
        this.onlineRateScore = another.onlineRateScore;
        this.blockMissScore = another.blockMissScore;
        this.bcScore = another.bcScore;
        this.height = height;
    }

    public BigInteger total(){
        return ssScore.add(nodeTypeScore).add(serverScore).add(hardwareScore).add(networkScore).add(performanceScore).add(onlineRateScore).add(blockMissScore).add(bcScore);
    }
    
    public void ssScoreCal(){
        PocCalculator.ssHoldCal(this);
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
    
    public void blockMissCal(PocTxBody.PocGenerationMissing pocBlockMissing){
        PocCalculator.blockMissCal(this, pocBlockMissing);
    }

    /**
     * replace the attributes of poc 
     * @param another 
     */
    public void synScoreFrom(PocScore another){
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
    

    /**
     * effective balance is pool balance if the miner own a sharder pool 
     * @param accountId
     * @param height
     * @return
     */
    private static BigInteger _calBalance(Long accountId, int height){
        BigInteger balance = BigInteger.ZERO;
        if (accountId == null) return balance;
        
        Account account = Account.getAccount(accountId, height);
        if (account == null) return balance;
        
        long id = SharderPoolProcessor.ownOnePool(accountId);
        if (id != -1 && SharderPoolProcessor.getSharderPool(id).getState().equals(SharderPoolProcessor.State.WORKING)) {
            balance = BigInteger.valueOf(Math.max(SharderPoolProcessor.getSharderPool(id).getPower() / Constants.ONE_SS, 0))
                    .add(BigInteger.valueOf(Math.max(account.getEffectiveBalanceSS(height), 0)));
        }else {
            balance = BigInteger.valueOf(Math.max(account.getEffectiveBalanceSS(height), 0));
        }
        return balance;
    }
    
    /**
     * effective balance calculate
     * @param account 
     * @param height
     * @return
     */
    public static BigInteger calEffectiveBalance(Account account , int height) {
        return _calBalance(account.getId(), height);
    }

    /** Poc calculator instance **/
    public static class PocCalculator {

        // 分制转换率，将10分制 转为 500000000分制（SS总发行量 5亿）， 所以转换率是50000000
        private static final BigInteger POINT_SYSTEM_CONVERSION_RATE = BigInteger.valueOf(50000000L);

        // 百分之除数，在算总分完成后需要除以这个数才是最终分数
        private static final BigInteger PERCENT_DIVISOR = BigInteger.valueOf(100L);

        // 当前使用的权重表模板
        static volatile PocTxBody.PocWeightTable pocWeightTable = null;
        
        static volatile int lastHeight = -1;

        public static void setCurWeightTable(PocTxBody.PocWeightTable weightTable, int height) {
            pocWeightTable = weightTable;
            lastHeight = height;
        }

        static void ssHoldCal(PocScore pocScore) {
            pocScore.ssScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.SS_HOLD.getValue()).multiply(pocScore.ssScore);
        }
        
        static void nodeTypeCal(PocScore pocScore,PocTxBody.PocNodeType nodeType) {
            BigInteger typeScore = BigInteger.ZERO;
            if (nodeType.getType().equals(Peer.Type.FOUNDATION)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE_TYPE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.FOUNDATION.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.COMMUNITY)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE_TYPE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.COMMUNITY.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.HUB)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE_TYPE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.HUB.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.BOX)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE_TYPE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.BOX.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            } else if (nodeType.getType().equals(Peer.Type.NORMAL)) {
                typeScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NODE_TYPE.getValue()).multiply(pocWeightTable.getNodeTypeTemplate().get(Peer.Type.NORMAL.getCode())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            }
            pocScore.nodeTypeScore = typeScore;
        }

        static void nodeConfCal(PocScore pocScore, PocTxBody.PocNodeConf nodeConf) {
            
            BigInteger serverScore = BigInteger.ZERO, hardwareScore = BigInteger.ZERO , networkScore = BigInteger.ZERO , performanceScore = BigInteger.ZERO;
            
            Long[] openedServices = nodeConf.getSystemInfo().getOpenServices();
            if (openedServices != null && openedServices.length > 0) {
                
                for (Long serviceCode : openedServices) {
                    BigInteger _scorePreDefined = pocWeightTable.getServerOpenTemplate().get(serviceCode);
                    if(_scorePreDefined == null) continue;
                    serverScore = serverScore.add(_scorePreDefined);
                }
                pocScore.serverScore = serverScore.multiply(pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.SERVER_OPEN.getValue())).multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            }

            
            if (nodeConf.getSystemInfo().getCore() >= 8 && nodeConf.getSystemInfo().getAverageMHz() >= 3600 && nodeConf.getSystemInfo().getMemoryTotal() >= 15 && nodeConf.getSystemInfo().getHardDiskSize() >= 10 * 1000) {
                hardwareScore = pocWeightTable.getHardwareConfigTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel());
            } else if (nodeConf.getSystemInfo().getCore() >= 4 && nodeConf.getSystemInfo().getAverageMHz() >= 3100 && nodeConf.getSystemInfo().getMemoryTotal() >= 7 && nodeConf.getSystemInfo().getHardDiskSize() >= 1000) {
                hardwareScore = pocWeightTable.getHardwareConfigTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel());
            } else if (nodeConf.getSystemInfo().getCore() >= 2 && nodeConf.getSystemInfo().getAverageMHz() >= 2400 && nodeConf.getSystemInfo().getMemoryTotal() >= 3 && nodeConf.getSystemInfo().getHardDiskSize() >= 100) {
                hardwareScore = pocWeightTable.getHardwareConfigTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel());
            }
            pocScore.hardwareScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.HARDWARE_CONFIG.getValue()).multiply(hardwareScore.multiply(POINT_SYSTEM_CONVERSION_RATE)).divide(PERCENT_DIVISOR);

            
            if (nodeConf.getSystemInfo().isHadPublicIp()) {
                if (nodeConf.getSystemInfo().getBandWidth() >= 10) {
                    networkScore = pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel());
                } else if (nodeConf.getSystemInfo().getBandWidth() >= 5) {
                    networkScore = pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel());
                } else if (nodeConf.getSystemInfo().getBandWidth() >= 1) {
                    networkScore = pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel());
                }
            } else {
                networkScore = pocWeightTable.getNetworkConfigTemplate().get(PocTxBody.DeviceLevels.POOR.getLevel());
            }
            pocScore.networkScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.NETWORK_CONFIG.getValue()).multiply(networkScore.multiply(POINT_SYSTEM_CONVERSION_RATE)).divide(PERCENT_DIVISOR);

            
            if (nodeConf.getSystemInfo().getTradePerformance() >= 1000) {
                performanceScore = pocWeightTable.getTxPerformanceTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel());
            } else if (nodeConf.getSystemInfo().getTradePerformance() >= 500) {
                performanceScore = pocWeightTable.getTxPerformanceTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel());
            } else if (nodeConf.getSystemInfo().getTradePerformance() >= 300) {
                performanceScore =pocWeightTable.getTxPerformanceTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel());
            }
            pocScore.performanceScore = pocWeightTable.getWeightMap().get(PocTxBody.WeightTableOptions.TX_PERFORMANCE.getValue()).multiply(performanceScore.multiply(POINT_SYSTEM_CONVERSION_RATE)).divide(PERCENT_DIVISOR);

        }

        static void onlineRateCal(PocScore pocScore,Peer.Type nodeType, PocTxBody.PocOnlineRate onlineRate) {
            BigInteger onlineRateScore = BigInteger.ZERO;

            if (nodeType.equals(Peer.Type.FOUNDATION)) {
                if (onlineRate.getNetworkRate() >= 9900 && onlineRate.getNetworkRate() < 9999) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.FOUNDATION).get(PocTxBody.OnlineStatusDef.FROM_99_00_TO_99_99.getValue());
                } else if (onlineRate.getNetworkRate() >= 9700 && onlineRate.getNetworkRate() < 9900) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.FOUNDATION).get(PocTxBody.OnlineStatusDef.FROM_97_00_TO_99_00.getValue());
                } else if (onlineRate.getNetworkRate() < 9700) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.FOUNDATION).get(PocTxBody.OnlineStatusDef.FROM_00_00_TO_97_00.getValue());
                }
            } else if (nodeType.equals(Peer.Type.COMMUNITY)) {
                if (onlineRate.getNetworkRate() >= 9700 && onlineRate.getNetworkRate() < 9900) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.COMMUNITY).get(PocTxBody.OnlineStatusDef.FROM_97_00_TO_99_00.getValue());
                } else if (onlineRate.getNetworkRate() >= 9000 && onlineRate.getNetworkRate() < 9700) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.COMMUNITY).get(PocTxBody.OnlineStatusDef.FROM_90_00_TO_97_00.getValue());
                } else if (onlineRate.getNetworkRate() < 9000) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.COMMUNITY).get(PocTxBody.OnlineStatusDef.FROM_00_00_TO_90_00.getValue());
                }
            } else if (nodeType.equals(Peer.Type.HUB) || nodeType.equals(Peer.Type.BOX)) {
                if (onlineRate.getNetworkRate() >= 9900) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.HUB).get(PocTxBody.OnlineStatusDef.FROM_99_00_TO_100.getValue());
                } else if (onlineRate.getNetworkRate() >= 9700) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.HUB).get(PocTxBody.OnlineStatusDef.FROM_97_00_TO_100.getValue());
                } else if (onlineRate.getNetworkRate() < 9000) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.HUB).get(PocTxBody.OnlineStatusDef.FROM_00_00_TO_90_00.getValue());
                }
            } else if (nodeType.equals(Peer.Type.NORMAL)) {
                if (onlineRate.getNetworkRate() >= 9700) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.NORMAL).get(PocTxBody.OnlineStatusDef.FROM_97_00_TO_100.getValue());
                } else if (onlineRate.getNetworkRate() >= 9000) {
                    onlineRateScore = pocWeightTable.getOnlineRateTemplate(Peer.Type.NORMAL).get(PocTxBody.OnlineStatusDef.FROM_90_00_TO_100.getValue());
                }
            }
            pocScore.onlineRateScore = onlineRateScore.multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
        }
        
        static Map<Long,Integer> missBlockMap = new HashMap<>();

        static void blockMissCal(PocScore pocScore,PocTxBody.PocGenerationMissing blockMiss) {
            Long accountId = pocScore.accountId;
            Integer missCount = 0;
            if(missBlockMap.containsKey(accountId)) missCount = missBlockMap.get(accountId);
            missCount++;

            BigInteger missBlcokScore = BigInteger.ZERO; 
            /** 低: 1块/月 累积丢失数小于 3 , 中: 小于3块/周 累积丢失数小于 10 , 高: 大于3块每周 累积丢失数大于 10 
             *  只实现了累积的判断，还未实现一段时间丢失率的判断检查
             * */
            if(missCount <= 3){
                missBlcokScore = pocWeightTable.getGenerationMissingTemplate().get(PocTxBody.DeviceLevels.GOOD.getLevel());
            }else if(missCount > 3 && missCount <= 10){
                missBlcokScore = pocWeightTable.getGenerationMissingTemplate().get(PocTxBody.DeviceLevels.MIDDLE.getLevel());
            }else if(missCount > 10){
                missBlcokScore = pocWeightTable.getGenerationMissingTemplate().get(PocTxBody.DeviceLevels.BAD.getLevel());
            }
            pocScore.blockMissScore = missBlcokScore.multiply(POINT_SYSTEM_CONVERSION_RATE).divide(PERCENT_DIVISOR);
            // TODO xy impl miss_block check in interval
        }

        static void bcCal(PocScore pocScore, Account account, PocTxBody.PocBcSpeed pocBcSpeed){
            //TODO un-impl now -20181230
        }

    }
}
