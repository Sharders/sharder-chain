package org.conch.consensus.reward;

import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.PocHolder;
import org.conch.consensus.poc.PocScore;
import org.conch.mint.pool.PoolRule;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.Attachment.CoinBase;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.LocalDebugTool;
import org.conch.util.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/8
 */
public class RewardCalculator {

    /**
     * Reward definition, amount is the reward amount
     */
    public enum RewardDef {
        MINT(1333),
        CROWD_MINERS(667);

        private final long amount;

        public long getAmount() {
            return amount;
        }

        RewardDef(long amount) {
            this.amount = amount;
        }

    }

    //    private static final int HALVE_COUNT = 210240;
    public static final int MINER_JOINING_PHASE = 1999;
    /**
     * how much one block reward
     * @return
     */
    public static long blockReward(int height) {
        /**
         * halving logic
         double turn = 0d;
         if(Conch.getBlockchain().getHeight() > HALVE_COUNT){
         turn = Conch.getBlockchain().getHeight() / HALVE_COUNT;
         }
         double rate = Math.pow(0.5d,turn);
         return (long)(Constants.ONE_SS * RewardDef.MINT.getAmount() * rate);
         **/

        // No block rewards in the miner joining phase
//        if(Conch.getHeight() <= MINER_JOINING_PHASE) return 1L;
        if(height <= MINER_JOINING_PHASE) return 1L;

        return RewardDef.MINT.getAmount() * Constants.ONE_SS;
    }

    public static long crowdMinerReward(int height){
        if((height >= Constants.COINBASE_CROWD_MINER_OPEN_HEIGHT && -1 != Constants.COINBASE_CROWD_MINER_OPEN_HEIGHT)
                || LocalDebugTool.isLocalDebugAndBootNodeMode){
            return RewardDef.CROWD_MINERS.getAmount() * Constants.ONE_SS;
        }
        return 0L;
    }

    public static long blockMiningReward(int height){
        return blockReward(height) - crowdMinerReward(height);
    }

    /**
     *
     * @param publicKey public key of miner's account
     * @param height blockchain's height
     * @return
     */
    public static TransactionImpl.BuilderImpl generateCoinBaseTxBuilder(final byte[] publicKey, int height){
        Account creator = Account.getAccount(publicKey);

        // Pool owner -> pool rewards map (send rewards to pool joiners)
        // Single miner -> empty rewards map (send rewards to miner)
        Map<Long, Long> map = new HashMap<>();
        long poolId = SharderPoolProcessor.findOwnPoolId(creator.getId());
        if (poolId == -1 || SharderPoolProcessor.isDead(poolId)) {
            poolId = creator.getId();
        } else {
            map = SharderPoolProcessor.getPool(poolId).getConsignorsAmountMap();
        }

        Attachment.CoinBase coinBase = null;

        if(isOpenCrowdMiners(height) || LocalDebugTool.isLocalDebugAndBootNodeMode){
            // crowd miner mode
            Map<Long, Long> crowdMinerPocScoreMap = generateCrowdMinerPocScoreMap(Lists.newArrayList(creator.getId()), height);
            coinBase = new CoinBase(creator.getId(), poolId, map, crowdMinerPocScoreMap);
        }else{
            // single miner or pool reward mode
            coinBase = new CoinBase(CoinBase.CoinBaseType.BLOCK_REWARD, creator.getId(), poolId, map);
        }

        return new TransactionImpl.BuilderImpl(
                publicKey,
                blockReward(height),
                0,
                (short) 10,
                coinBase);
    }


    /**
     * read the current qualified miners and calculate the reward distribution according to poc score rate:
     * - qualified condition:
     * a) create a tx to declared the node;
     * b) linked the mining address;
     * c) current height's balance  > 4256 MW（32T staking amount）;
     * @return map: miner's account id : poc score
     */
    private static long QUALIFIED_CROWD_MINER_HOLDING_AMOUNT_MIN = 32*133L; // 1T-133MW
    private static Map<Long, Long> generateCrowdMinerPocScoreMap(List<Long> exceptAccounts, int height){
        Map<Long, Long> crowdMinerPocScoreMap = Maps.newHashMap();
        // read the qualified miner list
        Map<Long, CertifiedPeer>  certifiedPeers = Conch.getPocProcessor().getCertifiedPeers();
        if(certifiedPeers == null || certifiedPeers.size() == 0) {
            return crowdMinerPocScoreMap;
        }

        // generate the poc score map
        for(CertifiedPeer certifiedPeer : certifiedPeers.values()){
            // only reward once for same miner
            if(exceptAccounts != null
                    && exceptAccounts.contains(certifiedPeer.getBoundAccountId())){
                continue;
            }
            // qualified miner judgement
            Account declaredAccount = Account.getAccount(certifiedPeer.getBoundAccountId());
            if(declaredAccount == null) continue;

            long holdingMwAmount = 0;
            try{
                holdingMwAmount = declaredAccount.getEffectiveBalanceSS(height);
            }catch(Exception e){
                Logger.logWarningMessage("[QualifiedMiner] not valid miner because can't get balance of account %s at height %d, caused by %s",  declaredAccount.getRsAddress(), height, e.getMessage());
                holdingMwAmount = 0;
            }
            if(holdingMwAmount < QUALIFIED_CROWD_MINER_HOLDING_AMOUNT_MIN) continue;

            // poc score judgement
            PocScore pocScore = PocHolder.getPocScore(height, declaredAccount.getId());
            if(pocScore == null || pocScore.total().longValue() <= 0) continue;

            crowdMinerPocScoreMap.put(declaredAccount.getId(), pocScore.total().longValue());
        }

        return crowdMinerPocScoreMap;
    }

    private static  JSONArray calAndSetCrowdMinerReward(Account minerAccount, Transaction tx, Map<Long, Long> crowdMiners, boolean stageTwo){
        return _calAndSetCrowdMinerReward(true, minerAccount, tx, crowdMiners, stageTwo);
    }

    /**
     *
     * @param minerAccount
     * @param tx
     * @param crowdMiners
     * @return crowd miner reward json array:[{accountId:,accountRS:,pocScore:,rewardAmount:}]
     */
    public static  JSONArray calCrowdMinerReward(Account minerAccount, Transaction tx, Map<Long, Long> crowdMiners){
        return _calAndSetCrowdMinerReward(false, minerAccount, tx, crowdMiners, false);
    }

    /**
     *
     * @param senderId
     * @param blockGeneratorId
     * @param tx
     * @param consignors
     * @return pool reward json array:
     * [{
     *  accountId: -7108135922261388000,
     *  accountRS: "CDW-P6TU-3A78-GRF2-BNQQU",
     *  investAmount: 16000,
     *  rewardAmount: 11491
     * }]
     */
    public static JSONArray calPoolReward(long senderId, long blockGeneratorId, Transaction tx, Map<Long, Long> consignors){
        JSONArray poolRewardArray = new JSONArray();
        Map<Long, Long> poolRewardMap = PoolRule.calRewardMapAccordingToRules(senderId, blockGeneratorId, blockMiningReward(tx.getHeight()), consignors);
        for (long accountId : poolRewardMap.keySet()) {
            Account account = Account.getAccount(accountId);
            poolRewardArray.add(new JSONObject()
                    .put("accountId", accountId)
                    .put("accountRS", account.getRsAddress())
                    .put("investAmount", consignors.get(accountId))
                    .put("rewardAmount", poolRewardMap.get(accountId)));
        }
        return poolRewardArray;
    }

    /**
     * reward calculation algo.: miner's PoC score / total miner's PoC score * 667 MW
     * @param updateBalance true: update and froze the balance after calculate, false: just calculate the rewards
     * @param minerAccount block's miner account
     * @param tx coinbase tx
     * @param crowdMiners crowd miner map: account id : miner's poc score
     * @param stageTwo
     * @return crowd miner reward json array:
     * [{
     *  accountId: -7108135922261388000,
     *  accountRS: "CDW-P6TU-3A78-GRF2-BNQQU",
     *  pocScore: 3836000,
     *  rewardAmount: 2451491
     * }]
     */
    private static JSONArray _calAndSetCrowdMinerReward(boolean updateBalance, Account minerAccount, Transaction tx, Map<Long, Long> crowdMiners, boolean stageTwo){
        JSONArray crowdMinerRewardArray = new JSONArray();
        Map<Long,Long> crowdMinerRewardMap = Maps.newHashMap();

        if (crowdMiners.size() == 0) return crowdMinerRewardArray;

        long totalPocScoreLong = 0;
        for(long pocScore : crowdMiners.values()){
            totalPocScoreLong += pocScore;
        }

        BigDecimal totalPocScore = BigDecimal.valueOf(totalPocScoreLong);
        long crowdMinerRewards = crowdMinerReward(tx.getHeight());
        BigDecimal crowdMinerRewardsAmount = new BigDecimal(crowdMinerRewards);
        // calculate the single miner's rewards
        long allocatedRewards = 0;

        String details = "";
        for (Long accountId : crowdMiners.keySet()) {
            if (crowdMinerRewardMap.containsKey(accountId)) continue;

            long pocScoreLong = crowdMiners.get(accountId);
            BigDecimal pocScore = BigDecimal.valueOf(pocScoreLong);
            BigDecimal pocScoreRate = pocScore.divide(totalPocScore, 10, BigDecimal.ROUND_DOWN);
            long rewards = crowdMinerRewardsAmount.multiply(pocScoreRate).longValue();
            if(updateBalance){
                details += updateBalanceAndFrozeIt(Account.getAccount(accountId), tx, rewards, stageTwo);
            }
            crowdMinerRewardMap.put(accountId, rewards);

            crowdMinerRewardArray.add(new JSONObject()
                    .put("accountId", accountId)
                    .put("accountRS", Account.rsAccount(accountId))
                    .put("pocScore", pocScoreLong)
                    .put("rewardAmount", rewards));
            allocatedRewards += rewards;
        }

        // remain amount distribute to creator
        long remainRewards = (crowdMinerRewards > allocatedRewards) ? (crowdMinerRewards - allocatedRewards) : 0;
        if(updateBalance) {
            details += updateBalanceAndFrozeIt(minerAccount, tx, remainRewards, stageTwo);
        }

        String isCalOnly = updateBalance ? "" : "-CalOnly";
        String tail = "[DEBUG] ----------------------------\n[DEBUG] Total count: " + (crowdMiners.size() + 1);
        if(!stageTwo){
            Logger.logDebugMessage("[%d-StageOne%s] Add crowdMiners rewards to account's unconfirmed balance and freeze it. \n[DEBUG] CrowdMiner Reward Detail Format: txid | address: distribution amount\n%s%s\n", tx.getHeight(), isCalOnly,  details, tail);
        }else {
            Logger.logDebugMessage("[%d-StageTwo%s] Unfreeze crowdMiners rewards and add it in mined amount. \n[DEBUG] CrowdMiner Reward Detail Format: txid | address: distribution amount\n%s%s\n", tx.getHeight(), isCalOnly,  details, tail);
        }

//        crowdMinerRewardMap.put(minerAccount.getId(), remainRewards);
        crowdMinerRewardArray.add(new JSONObject()
                .put("accountId", minerAccount.getId())
                .put("accountRS", minerAccount.getRsAddress())
                .put("pocScore", -1L)
                .put("rewardAmount", remainRewards));
        return crowdMinerRewardArray;
    }

    /**
     *
     * @param account
     * @param tx
     * @param amount
     * @param stageTwo
     */
    private static String updateBalanceAndFrozeIt(Account account, Transaction tx, long amount, boolean stageTwo){
        if(!stageTwo) {
            account.addBalanceAddUnconfirmed(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), amount);
            account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), amount);
        }else{
            if(Constants.isTestnet() && account.getFrozenBalanceNQT() <= 0) {
                account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), account.getFrozenBalanceNQT());
            }else if(Constants.isTestnet() && account.getFrozenBalanceNQT() <= amount){
                account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), -account.getFrozenBalanceNQT());
            }else{
                account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), -amount);
            }
            account.addMintedBalance(amount);
            account.pocChanged();
        }
        return  String.format("[DEBUG] txid/%d | %s: %d\n", tx.getId(), account.getRsAddress(), amount);
    }


    public static boolean isOpenCrowdMiners(int height){
        return height >= Constants.COINBASE_CROWD_MINER_OPEN_HEIGHT && -1 != Constants.COINBASE_CROWD_MINER_OPEN_HEIGHT;
    }

    private static long rewardCalStartMS = -1;
    /**
     * total 2 stages:
     * stage one is in tx accepted, the rewards need be lock;
     * stage two is the block confirmations reached, means unlock the rewards and record mined amount
     * @param tx reward tx
     * @param stageTwo true - stage two; false - stage one
     * @return
     */
    public static long blockRewardDistribution(Transaction tx, boolean stageTwo) {
        Attachment.CoinBase coinBase = (Attachment.CoinBase) tx.getAttachment();
        Account senderAccount = Account.getAccount(tx.getSenderId());
        Account minerAccount = Account.getAccount(coinBase.getCreator());

        rewardCalStartMS = System.currentTimeMillis();

        String stage = stageTwo ? "Two" : "One";
        // Crowd Miner Reward
        long miningRewards =  tx.getAmountNQT();

        Map<Long, Long> crowdMiners = Maps.newHashMap();
        if(coinBase.isType(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD)
        && isOpenCrowdMiners(tx.getHeight())) {
            crowdMiners = coinBase.getCrowdMiners();
            Logger.logDebugMessage("[Rewards-%d-Stage%s] Distribute crowd miner's rewards[crowd miner size=%d] at height %d", tx.getHeight(), stage, crowdMiners.size(), Conch.getHeight());
            calAndSetCrowdMinerReward(minerAccount, tx, crowdMiners, stageTwo);
            if(crowdMiners.size() > 0){
                miningRewards = tx.getAmountNQT() - crowdMinerReward(tx.getHeight());
            }
        }
        long crowdRewardProcessingMS = System.currentTimeMillis() - rewardCalStartMS;
        long miningCalStartMS = System.currentTimeMillis();
        // Mining Reward (include Pool mode)
        Map<Long, Long> consignors = coinBase.getConsignors();
        Logger.logDebugMessage("[Rewards-%d-Stage%s] Distribute block mining's rewards[ mining joiner size=%d] at height %d. " +
                        "Joiner size = 0 means solo miner mode, all block mined rewards will distribute to miner[%s]; " +
                        "Joiner size > 0 means pool mining mode, block mined rewards will distribute under the pool rules.",
                tx.getHeight(), stage, consignors.size(), Conch.getHeight(), minerAccount.getRsAddress());

        String details = "";
        String tail = "[DEBUG] ----------------------------\n[DEBUG] Total count:  ";
        int miningJoinerCount = 1;
        if (consignors.size() == 0) {
            details += updateBalanceAndFrozeIt(senderAccount, tx, miningRewards, stageTwo);
            tail += "1";
        } else {
            Map<Long, Long> rewardList = PoolRule.calRewardMapAccordingToRules(senderAccount.getId(), coinBase.getGeneratorId(), miningRewards, consignors);
            for (long id : rewardList.keySet()) {
                Account account = Account.getAccount(id);
                details += updateBalanceAndFrozeIt(account, tx, rewardList.get(id), stageTwo);
            }
            tail += rewardList.size();
            miningJoinerCount = rewardList.size();
        }

        if(!stageTwo){
            Logger.logDebugMessage("[%d-StageOne] Add mining rewards to account's unconfirmed balance and freeze it. \n[DEBUG] Mining Reward Detail Format: txid | address: distribution amount\n%s%s\n", tx.getHeight(), details, tail);
        }else {
            Logger.logDebugMessage("[%d-StageTwo] Unfreeze mining rewards and add it in mined amount. \n[DEBUG] Mining Reward Detail Format: txid | address: distribution amount\n%s%s\n", tx.getHeight(), details, tail);
        }

        long miningRewardProcessingMS = System.currentTimeMillis() - miningCalStartMS;
        long totalUsedMs = System.currentTimeMillis() - rewardCalStartMS;

        Peer feeder = Conch.getBlockchainProcessor().getLastBlockchainFeeder();
        if(Logger.isLevel(Logger.Level.INFO)) {
            Logger.logInfoMessage("[Rewards-%d-Stage%s] Distribution detail[crowd miner size=%d, mining joiner size=%d, processing used time≈ %d S(%d MS)] at current height %d -> height %d of feeder %s[%s]\n",
                    tx.getHeight(), stage, crowdMiners.size(), miningJoinerCount
                    , totalUsedMs / 1000, totalUsedMs
                    , Conch.getHeight(),
                    Conch.getBlockchainProcessor().getLastBlockchainFeederHeight(),
                    feeder != null ? feeder.getAnnouncedAddress() : "None",
                    feeder != null ? feeder.getHost() : "None");
        }else {
            Logger.logDebugMessage("[Rewards-%d-Stage%s] Distribution used time[crowd miners≈ %d S(%d MS), mining joiners≈ %d S(%d MS)], reward distribution detail[crowd miner size=%d, mining joiner size=%d] at height %d -> height %d of feeder %s[%s]\n",
                    tx.getHeight(), stage
                    , crowdRewardProcessingMS / 1000, crowdRewardProcessingMS
                    , miningRewardProcessingMS / 1000, miningRewardProcessingMS
                    , crowdMiners.size(), miningJoinerCount
                    , Conch.getHeight(),
                    Conch.getBlockchainProcessor().getLastBlockchainFeederHeight(),
                    feeder != null ? feeder.getAnnouncedAddress() : "None",
                    feeder != null ? feeder.getHost() : "None");
        }
        return tx.getAmountNQT();
    }

    /**
     * CoinBase tx attachment judgement
     * @param attachment
     * @return
     */
    public static boolean isBlockRewardTx(Attachment attachment) {
        if(!(attachment instanceof Attachment.CoinBase)) return false;

        Attachment.CoinBase coinbaseBody = (Attachment.CoinBase) attachment;
        return coinbaseBody.isType(Attachment.CoinBase.CoinBaseType.BLOCK_REWARD)
                || coinbaseBody.isType(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD);
    }

    private static Attachment.CoinBase parseToCoinBase(Attachment attachment){
        if(!(attachment instanceof Attachment.CoinBase)) return null;
        return (Attachment.CoinBase) attachment;
    }

    public static int crowdMinerCount(Attachment attachment) {
        try{
            Attachment.CoinBase coinBaseObj = parseToCoinBase(attachment);
            if(coinBaseObj == null) return -1;

            if(coinBaseObj.isType(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD)) {
                return coinBaseObj.getCrowdMiners() != null ? coinBaseObj.getCrowdMiners().size() : 0;
            }
        }catch(Exception e){
            Logger.logErrorMessage("calculate the size of crowd miners failed", e);
        }
        return 0;
    }

    /**
     * No needs to validate in the tx creation. rewards calculate and distribute at the block accepted in the Pool processor:
     * org.conch.consensus.reward.RewardCalculator#blockRewardDistribution(org.conch.tx.Transaction, boolean)
     * Ben-07.15.2020
     *
     * To replace the org.conch.tx.TransactionType.CoinBase#validateByType
     * @param transaction
     * @return
     */
    public static boolean validateCoinbaseTx(Transaction transaction) throws ConchException.NotValidException {
//        if(transaction.getAmountNQT() > RewardCalculator.blockReward(transaction.getHeight())){
//            throw new ConchException.NotValidException("block reward is large than the maxium amount " + );
//        }
        Attachment.CoinBase coinBase = (Attachment.CoinBase) transaction.getAttachment();
        if (Attachment.CoinBase.CoinBaseType.BLOCK_REWARD == coinBase.getCoinBaseType()) {
            // FIXME sum the consi
            Map<Long, Long> consignors = coinBase.getConsignors();

            if(consignors.size() <= 0) return true;
        } else if(CoinBase.CoinBaseType.CROWD_BLOCK_REWARD == coinBase.getCoinBaseType()){
            // sum the
        } else if (Attachment.CoinBase.CoinBaseType.GENESIS == coinBase.getCoinBaseType()) {
            if (!SharderGenesis.isGenesisCreator(coinBase.getCreator())) {
                throw new ConchException.NotValidException("the Genesis coin base tx is not created by genesis creator");
            }
        }
        return true;
    }

    //FIXME ignore the signature validation (temporary code to handle block stuck) -2020.07.24
    public static boolean temporaryCloseValidation = true;

}