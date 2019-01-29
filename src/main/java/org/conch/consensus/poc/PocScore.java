package org.conch.consensus.poc;

import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;

import java.math.BigInteger;

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
        PocCalculator.inst.ssHoldCal(this);
    }
    
    public void nodeConfCal(PocTxBody.PocNodeConf nodeConf){
        PocCalculator.inst.nodeConfCal(this,nodeConf);
    }
    
    public void nodeTypeCal(PocTxBody.PocNodeType nodeType){
        PocCalculator.inst.nodeTypeCal(this,nodeType);
    }
    
    public void onlineRateCal(Peer.Type nodeType,PocTxBody.PocOnlineRate onlineRate){
        PocCalculator.inst.onlineRateCal(this,nodeType,onlineRate);
    }
    
    public void blockMissCal(PocTxBody.PocGenerationMissing pocBlockMissing){
        PocCalculator.inst.blockMissCal(this, pocBlockMissing);
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
}
