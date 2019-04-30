package org.conch.consensus.poc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/29
 */
public class PocScore implements Serializable {
    Long accountId;
    int height;
    BigInteger ssScore = BigInteger.ZERO;
    BigInteger nodeTypeScore = BigInteger.ZERO;
    BigInteger serverScore = BigInteger.ZERO;
    BigInteger hardwareScore = BigInteger.ZERO;
    BigInteger networkScore = BigInteger.ZERO;
    BigInteger performanceScore = BigInteger.ZERO;
    BigInteger onlineRateScore = BigInteger.ZERO;
    BigInteger blockMissScore = BigInteger.ZERO;
    BigInteger bcScore = BigInteger.ZERO;

    BigInteger effectiveBalance;
    
    private static BigInteger MULTIPLIER = new BigInteger("10");
    
    //TODO 
    int luck = 0;

    /**
     * default poc score that contains the ss score
     * @param accountId
     * @param height
     */
    public PocScore(Long accountId, int height) {
        this.accountId = accountId;
        this.height = height;
        this.effectiveBalance = this.ssScore = _calBalance(accountId, height);
        PocCalculator.inst.ssHoldCal(this);
    }
    
    public PocScore(int height, PocScore another) {
        this.accountId = another.accountId;
        this.effectiveBalance = another.effectiveBalance;
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

    public BigInteger total() {
        // 90% of block rewards for hub miner, 10% for other miners in Testnet phase1 (before end of 2019.Q2)
        BigInteger rate = Conch.getPocProcessor().isCertifiedPeerBind(accountId) ? BigInteger.valueOf(90) : BigInteger.valueOf(10);
        BigInteger score = ssScore.add(nodeTypeScore).add(serverScore).add(hardwareScore).add(networkScore).add(performanceScore).add(onlineRateScore).add(blockMissScore).add(bcScore);
        return score.multiply(MULTIPLIER).multiply(rate).divide(BigInteger.valueOf(100));
    }

    public void nodeConfCal(PocTxBody.PocNodeConf nodeConf) {
        PocCalculator.inst.nodeConfCal(this, nodeConf);
    }

    public void nodeTypeCal(PocTxBody.PocNodeType nodeType) {
        PocCalculator.inst.nodeTypeCal(this, nodeType);
    }

    public void onlineRateCal(Peer.Type nodeType, PocTxBody.PocOnlineRate onlineRate) {
        PocCalculator.inst.onlineRateCal(this, nodeType, onlineRate);
    }

    public void blockMissCal(PocTxBody.PocGenerationMissing pocBlockMissing) {
        PocCalculator.inst.blockMissCal(this, pocBlockMissing);
    }

    public void synFrom(PocScore another){
        combineFrom(another, true); 
    }

    public void synFromExceptSSHold(PocScore another){
        combineFrom(another, false);
    }
    
    /**
     * replace the attributes of poc
     *
     * @param another
     */
    private void combineFrom(PocScore another, boolean updateSS) {
        if(BigInteger.ZERO != another.ssScore && updateSS) this.ssScore = another.ssScore;
        if(BigInteger.ZERO != another.nodeTypeScore) this.nodeTypeScore = another.nodeTypeScore;
        if(BigInteger.ZERO != another.serverScore) this.serverScore = another.serverScore;
        if(BigInteger.ZERO != another.hardwareScore) this.hardwareScore = another.hardwareScore;
        if(BigInteger.ZERO != another.networkScore) this.networkScore = another.networkScore;
        if(BigInteger.ZERO != another.performanceScore) this.performanceScore = another.performanceScore;
        if(BigInteger.ZERO != another.onlineRateScore) this.onlineRateScore = another.onlineRateScore;
        if(BigInteger.ZERO != another.blockMissScore) this.blockMissScore = another.blockMissScore;
        if(BigInteger.ZERO != another.bcScore) this.bcScore = another.bcScore;
        if(BigInteger.ZERO != another.effectiveBalance) this.effectiveBalance = another.effectiveBalance;
    }

    /**
     * effective balance is pool balance if the miner own a sharder pool
     *
     * @param accountId
     * @param height
     * @return
     */
    private static BigInteger _calBalance(Long accountId, int height) {
        BigInteger balance = BigInteger.ZERO;
        if (accountId == null) return balance;

        Account account = Account.getAccount(accountId, height);
        if (account == null) return balance;

        SharderPoolProcessor poolProcessor = SharderPoolProcessor.getPool(accountId);
        if (poolProcessor != null && SharderPoolProcessor.State.WORKING.equals(poolProcessor.getState())) {
            balance = BigInteger.valueOf(Math.max(poolProcessor.getPower() / Constants.ONE_SS, 0))
                    .add(BigInteger.valueOf(Math.max(account.getEffectiveBalanceSS(height), 0)));
        } else {
            balance = BigInteger.valueOf(Math.max(account.getEffectiveBalanceSS(height), 0));
        }
        return balance;
    }

    private static final String SCORE_KEY = "poc_score";
    public JSONObject toJsonObject() {
        JSONObject jsonObject = JSON.parseObject(toJsonString());
        jsonObject.put(SCORE_KEY, total());
        return jsonObject;
    }

    public String toJsonString() {
        return JSON.toJSONString(this);
    }
    
    public Long getAccountId() {
        return accountId;
    }

    public int getHeight() {
        return height;
    }

    public BigInteger getSsScore() {
        return ssScore;
    }

    public BigInteger getNodeTypeScore() {
        return nodeTypeScore;
    }

    public BigInteger getServerScore() {
        return serverScore;
    }

    public BigInteger getHardwareScore() {
        return hardwareScore;
    }

    public BigInteger getNetworkScore() {
        return networkScore;
    }

    public BigInteger getPerformanceScore() {
        return performanceScore;
    }

    public BigInteger getOnlineRateScore() {
        return onlineRateScore;
    }

    public BigInteger getBlockMissScore() {
        return blockMissScore;
    }

    public BigInteger getBcScore() {
        return bcScore;
    }

    public BigInteger getEffectiveBalance() {
        return effectiveBalance;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
