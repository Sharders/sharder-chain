package org.conch.consensus.poc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;
import org.conch.util.LocalDebugTool;
import org.conch.util.Logger;

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

    private static BigInteger SCORE_MULTIPLIER = parseAndGetScoreMagnification();

    /**
     * mag. of poc score is use to increase the poc score of the miner to make sure the mining gap is match the preset interval
     * @return
     */
    private static BigInteger parseAndGetScoreMagnification(){
        BigInteger mag = BigInteger.TEN;
        try{
            if(LocalDebugTool.isLocalDebugAndBootNodeMode){
                return new BigInteger("100000");
            }

            if(Conch.getHeight() <= (Constants.POC_TX_ALLOW_RECIPIENT + 70)) {
                mag = new BigInteger("1000");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return mag;
    }

    //TODO for every pool add the luck, used to battle the block generation chance
    int luck = 0;

    public PocScore(){}

    /**
     * default poc score that contains the ss score
     * @param accountId
     * @param height
     */
    public PocScore(Long accountId, int height) {
        this.accountId = accountId;
        this.height = height;
        ssCal();
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

    public PocScore(String pocDetailJson){
        JSONObject simpleObj = JSONObject.parseObject(pocDetailJson);

        this.accountId = simpleObj.getLong("accountId");
        int height = simpleObj.containsKey("height") ? simpleObj.getIntValue("height") : 0;
        this.height = height;

        // compatibility codes
        if(simpleObj.containsKey("bcScore")) this.bcScore = simpleObj.getBigInteger("bcScore");
        if(simpleObj.containsKey("bs")) this.bcScore = simpleObj.getBigInteger("bs");

        if(simpleObj.containsKey("blockMissScore")) this.blockMissScore = simpleObj.getBigInteger("blockMissScore");
        if(simpleObj.containsKey("bms")) this.blockMissScore = simpleObj.getBigInteger("bms");

        if(simpleObj.containsKey("effectiveBalance")) this.effectiveBalance = simpleObj.getBigInteger("effectiveBalance");
        if(simpleObj.containsKey("eb")) this.effectiveBalance = simpleObj.getBigInteger("eb");

        if(simpleObj.containsKey("hardwareScore")) this.hardwareScore = simpleObj.getBigInteger("hardwareScore");
        if(simpleObj.containsKey("hs")) this.hardwareScore = simpleObj.getBigInteger("hs");

        if(simpleObj.containsKey("networkScore")) this.networkScore = simpleObj.getBigInteger("networkScore");
        if(simpleObj.containsKey("ns")) this.networkScore = simpleObj.getBigInteger("ns");

        if(simpleObj.containsKey("nodeTypeScore")) this.nodeTypeScore = simpleObj.getBigInteger("nodeTypeScore");
        if(simpleObj.containsKey("nts")) this.nodeTypeScore = simpleObj.getBigInteger("nts");

        if(simpleObj.containsKey("onlineRateScore")) this.onlineRateScore = simpleObj.getBigInteger("onlineRateScore");
        if(simpleObj.containsKey("ors")) this.onlineRateScore = simpleObj.getBigInteger("ors");

        if(simpleObj.containsKey("performanceScore")) this.performanceScore = simpleObj.getBigInteger("performanceScore");
        if(simpleObj.containsKey("ps")) this.performanceScore = simpleObj.getBigInteger("ps");

        if(simpleObj.containsKey("serverScore")) this.serverScore = simpleObj.getBigInteger("serverScore");
        if(simpleObj.containsKey("ss")) this.serverScore = simpleObj.getBigInteger("ss");

        if(simpleObj.containsKey("ssScore")) this.ssScore = simpleObj.getBigInteger("ssScore");
        if(simpleObj.containsKey("sss")) this.ssScore = simpleObj.getBigInteger("sss");
    }

    public String toSimpleJson(){
        JSONObject simpleObj = new JSONObject();
        simpleObj.put("accountId",this.accountId);
        if(this.bcScore.intValue() > 0)  simpleObj.put("bs",this.bcScore);
        if(this.blockMissScore.intValue() > 0)  simpleObj.put("bms",this.blockMissScore);
        if(this.effectiveBalance != null && this.effectiveBalance.intValue() > 0)  simpleObj.put("eb",this.effectiveBalance);
        if(this.hardwareScore.intValue() > 0)  simpleObj.put("hs",this.hardwareScore);
        if(this.height > 0)  simpleObj.put("height",this.height);
        if(this.networkScore.intValue() > 0)  simpleObj.put("ns",this.networkScore);
        if(this.nodeTypeScore.intValue() > 0)  simpleObj.put("nts",this.nodeTypeScore);
        if(this.onlineRateScore.intValue() > 0)  simpleObj.put("ors",this.onlineRateScore);
        if(this.performanceScore.intValue() > 0)  simpleObj.put("ps",this.performanceScore);
        if(this.serverScore.intValue() > 0)  simpleObj.put("ss",this.serverScore);
        if(this.ssScore.intValue() > 0)  simpleObj.put("sss",this.ssScore);
        return simpleObj.toJSONString();
    }



    public BigInteger total() {
//        // 90% of block rewards for hub miner, 10% for other miners in Testnet phase1 (before end of 2019.Q2)
//        BigInteger rate = Conch.getPocProcessor().isCertifiedPeerBind(accountId, height) ? BigInteger.valueOf(90) : BigInteger.valueOf(10);
//        return score.multiply(SCORE_MULTIPLIER).multiply(rate).divide(BigInteger.valueOf(100));
        BigInteger score = ssScore.add(nodeTypeScore).add(serverScore).add(hardwareScore).add(networkScore).add(performanceScore).add(onlineRateScore).add(blockMissScore).add(bcScore);
        return score.multiply(SCORE_MULTIPLIER);
    }

    public PocScore nodeConfCal(PocTxBody.PocNodeConf nodeConf) {
        PocCalculator.inst.nodeConfCal(this, nodeConf);
        return this;
    }

    public PocScore nodeTypeCal(PocTxBody.PocNodeType nodeType) {
        PocCalculator.inst.nodeTypeCal(this, nodeType);
        return this;
    }

    public PocScore onlineRateCal(Peer.Type nodeType, PocTxBody.PocOnlineRate onlineRate) {
        PocCalculator.inst.onlineRateCal(this, nodeType, onlineRate);
        return this;
    }

    public PocScore blockMissCal(PocTxBody.PocGenerationMissing pocBlockMissing) {
        PocCalculator.inst.blockMissCal(this, pocBlockMissing);
        return this;
    }

    /**
     * two conditions:
     * - valid node (has the node type statement tx)
     * - own the MW
     */
    public boolean qualifiedMiner(){
        if(this.ssScore.signum() >= 0
                && this.ssScore.longValue() >= 0L
                && total().signum() > 0){
            return true;
        }

        return false;
    }

    public PocScore ssCal(){
        if (accountId != null) {
            Account account = Account.getAccount(accountId, height);
            long accountBalanceNQT = account != null ? account.getEffectiveBalanceNQT(height) : 0L;
            this.effectiveBalance = BigInteger.valueOf(accountBalanceNQT / Constants.ONE_SS);
            this.ssScore = _calEffectiveSS(account, accountBalanceNQT, height);
        }
        PocCalculator.inst.ssHoldCal(this);
        return this;
    }

    public PocScore setHeight(int height) {
        if(LocalDebugTool.isCheckPocAccount(this.accountId)) {
            Logger.logDebugMessage("[LocalDebugMode] " + Account.rsAccount(this.accountId) + " is updated at height " + height);
        }
        this.height = height;
        return this;
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
     * 0.3 in MW Testnet
     *
     * Testnet:
     * 0.5 :  0 < height < 4765
     * 0.3 : 4765 < height < 12500
     * 0.19 : 12500 <= height
     * NOTE: please see the height definition at: Constants.POC_SS_HELD_SCORE_PHASE1_HEIGHT
     * @return
     */
    private static Float ssHeldRate(int height){
//        if(height < Constants.POC_SS_HELD_SCORE_PHASE1_HEIGHT){
//            return 2f;
//        }else if (Constants.POC_SS_HELD_SCORE_PHASE1_HEIGHT <= height
//                && height < Constants.POC_SS_HELD_SCORE_PHASE2_HEIGHT){
//            return 3f;
//        } if(Constants.POC_SS_HELD_SCORE_PHASE2_HEIGHT <= height) {
//            return 0.19f;
//        }
        return 3f;
    }

    /**
     * - effective ss is pool balance if the miner own a sharder pool
     * - effective ss is not equals to effective balance always
     * - the max effective ss is all pools amounts that account owned
     * @param account
     * @param height
     * @return
     */
    private static BigInteger _calEffectiveSS(Account account,long accountBalanceNQT,int height) {
        BigInteger effectiveSS = BigInteger.ZERO;
        if (account == null) return effectiveSS;

        if(height >= Constants.POC_SCORE_CHANGE_HEIGHT
                || PocProcessorImpl.FORCE_RE_CALCULATE) {
            // the effective ss of genesis peer's miner force to limit to 100,000
            if(SharderGenesis.isGenesisPeerAccount(account.getId())){
                if(accountBalanceNQT >  100000L * Constants.ONE_SS) {
                    effectiveSS = BigInteger.valueOf(100000L);
                }else {
                    effectiveSS = BigInteger.valueOf(accountBalanceNQT / Constants.ONE_SS);
                }
            }else{
                effectiveSS = BigInteger.valueOf(accountBalanceNQT / Constants.ONE_SS);
            }

            if(PocProcessorImpl.FORCE_RE_CALCULATE) {
                return  effectiveSS;
            }
        }

        // pool not opening and reach the poc algo changed height
        if((Constants.POOL_OPENING_HEIGHT == -1 || Conch.getHeight() <= Constants.POOL_OPENING_HEIGHT)
                && height > Constants.POC_CAL_ALGORITHM) {
            return effectiveSS;
        }

        SharderPoolProcessor poolProcessor = SharderPoolProcessor.getPoolByCreator(account.getId());

        if(Constants.isDevnet() && SharderGenesis.isGenesisRecipients(account.getId())){
            // expand the balance to 10x at the dev env
            effectiveSS = BigInteger.valueOf(accountBalanceNQT * 10 / Constants.ONE_SS);
        }else if(Constants.isTestnet() && height < Constants.POC_NEW_ALGO_HEIGHT){
            if (poolProcessor != null && SharderPoolProcessor.State.WORKING.equals(poolProcessor.getState())) {
                effectiveSS = BigInteger.valueOf(Math.max(poolProcessor.getPower() / Constants.ONE_SS, 0))
                        .add(BigInteger.valueOf(accountBalanceNQT / Constants.ONE_SS));
            } else {
                effectiveSS = BigInteger.valueOf(accountBalanceNQT / Constants.ONE_SS);
            }
        }else{
            /**
             * pool owner: my_pool_power + other_held_ss(limit is pool capacity) * ssHeldRate
             * normal miner: held_ss(limit is pool capacity) * ssHeldRate
             */
            boolean exceedPoolMaxAmount = accountBalanceNQT >  SharderPoolProcessor.POOL_MAX_AMOUNT_NQT;
            long heldAmount = exceedPoolMaxAmount ? SharderPoolProcessor.POOL_MAX_AMOUNT_NQT : accountBalanceNQT;

            // !!NOTE: effective ss calculation method be changed from multiply to divide after phase 2
            Float ssHeldRate = ssHeldRate(height);
            if (height < Constants.POC_SS_HELD_SCORE_PHASE2_HEIGHT) {
                effectiveSS = BigInteger.valueOf( heldAmount / Constants.ONE_SS / ssHeldRate.longValue());
            }else {
                Float effectiveSSF = heldAmount / Constants.ONE_SS * ssHeldRate;
                effectiveSS = BigInteger.valueOf(effectiveSSF.longValue());
            }
        }

        return effectiveSS;
    }

    private static final String SCORE_KEY = "poc_score";
    public JSONObject toJsonObject() {
        if(LocalDebugTool.isCheckPocAccount(accountId)){
            Logger.logDebugMessage("[LocalDebugMode] convert the %s's poc score to json object", Account.rsAccount(accountId));
        }
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

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setSsScore(BigInteger ssScore) {
        this.ssScore = ssScore;
    }

    public void setNodeTypeScore(BigInteger nodeTypeScore) {
        this.nodeTypeScore = nodeTypeScore;
    }

    public void setServerScore(BigInteger serverScore) {
        this.serverScore = serverScore;
    }

    public void setHardwareScore(BigInteger hardwareScore) {
        this.hardwareScore = hardwareScore;
    }

    public void setNetworkScore(BigInteger networkScore) {
        this.networkScore = networkScore;
    }

    public void setPerformanceScore(BigInteger performanceScore) {
        this.performanceScore = performanceScore;
    }

    public void setOnlineRateScore(BigInteger onlineRateScore) {
        this.onlineRateScore = onlineRateScore;
    }

    public void setBlockMissScore(BigInteger blockMissScore) {
        this.blockMissScore = blockMissScore;
    }

    public void setBcScore(BigInteger bcScore) {
        this.bcScore = bcScore;
    }

    public void setEffectiveBalance(BigInteger effectiveBalance) {
        this.effectiveBalance = effectiveBalance;
    }

    public void setLuck(int luck) {
        this.luck = luck;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
