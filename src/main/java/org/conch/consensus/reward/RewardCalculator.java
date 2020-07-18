package org.conch.consensus.reward;

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
import org.conch.tx.Attachment;
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
        MINT(128),
        CROWD_MINERS(64);

        private final long amount;

        public long getAmount() {
            return amount;
        }

        RewardDef(long amount) {
            this.amount = amount;
        }
        
    }
    
    private static final int HALVE_COUNT = 210240;
    /**
     * how much one block reward
     * @return
     */
    public static long blockReward(int height) {
        double turn = 0d;
        if(height > HALVE_COUNT){
            turn = height / HALVE_COUNT;
        }
        double rate = Math.pow(0.5d,turn);
        return (long)(Constants.ONE_SS * RewardDef.MINT.getAmount() * rate);
    }

    public static long crowdMinerReward(int height){
        if(height >= Constants.COINBASE_CROWD_MINER_OPEN_HEIGHT){
            return RewardDef.CROWD_MINERS.getAmount() * Constants.ONE_SS;
        }
        return 0L;
    }

    public static long blockMinedReward(int height){
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
        if(height >= Constants.COINBASE_CROWD_MINER_OPEN_HEIGHT
                || LocalDebugTool.isLocalDebugAndBootNodeMode){
            // crowd miner mode
            Map<Long, Long> crowdMinerPocScoreMap = generateCrowdMinerPocScoreMap(Lists.newArrayList(creator.getId()), height);
            coinBase = new Attachment.CoinBase(creator.getId(), poolId, map, crowdMinerPocScoreMap);
        }else{
            // single miner or pool reward mode
            coinBase = new Attachment.CoinBase(Attachment.CoinBase.CoinBaseType.BLOCK_REWARD, creator.getId(), poolId, map);
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
     * b) lined the mining address;
     * c) balance at current height > 1064 MW（8T staking amount）;
     * @return map: miner's account id : poc score
     */
    private static long QUALIFIED_MINER_HOLDING_MW_MIN = 1064L;
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
            long holdingMwAmount = 0;
            try{
                holdingMwAmount = declaredAccount.getEffectiveBalanceSS(height);
            }catch(Exception e){
                Logger.logErrorMessage("[QualifiedMiner]can't get balance of account %s at height %d",  declaredAccount.getRsAddress(), height);
            }
            if(holdingMwAmount < QUALIFIED_MINER_HOLDING_MW_MIN) continue;

            // poc score judgement
            PocScore pocScore = PocHolder.getPocScore(height, declaredAccount.getId());
            if(pocScore == null || pocScore.total().longValue() <= 0) continue;

            crowdMinerPocScoreMap.put(declaredAccount.getId(), pocScore.total().longValue());
        }

        return crowdMinerPocScoreMap;
    }

    /**
     * reward calculation algo.: miner's PoC score / total miner's PoC score * 667 MW
     * @param minerAccount block's miner account
     * @param tx coinbase tx
     * @param crowdMiners crowd miner map: account id : miner's poc score
     * @param stageTwo
     */
    private static void calAndSetCrowdMinerReward(Account minerAccount, Transaction tx, Map<Long, Long> crowdMiners, boolean stageTwo){
        if (crowdMiners.size() == 0) return;

        Map<Long,Long> crowdMinerRewardMap = Maps.newHashMap();

        long totalPocScoreLong = 0;
        for(long pocScore : crowdMiners.values()){
            totalPocScoreLong += pocScore;
        }

        BigDecimal totalPocScore = BigDecimal.valueOf(totalPocScoreLong);
        long crowdMinerRewards = crowdMinerReward(tx.getHeight());
        BigDecimal crowdMinerRewardsAmount = new BigDecimal(crowdMinerRewards);
        // calculate the single miner's rewards
        long allocatedRewards = 0;
        for (Long accountId : crowdMiners.keySet()) {
            if (crowdMinerRewardMap.containsKey(accountId)) continue;

            BigDecimal pocScore = BigDecimal.valueOf(crowdMiners.get(accountId));
            BigDecimal pocScoreRate = pocScore.divide(totalPocScore,4,BigDecimal.ROUND_DOWN);
            long rewards = crowdMinerRewardsAmount.multiply(pocScoreRate).longValue();

            updateBalanceAndFrozeIt(Account.getAccount(accountId), tx, rewards, stageTwo);
            crowdMinerRewardMap.put(accountId, rewards);
            allocatedRewards += rewards;
        }

        // remain amount distribute to creator
        long remainRewards = (crowdMinerRewards > allocatedRewards) ? (crowdMinerRewards - allocatedRewards) : 0;
        updateBalanceAndFrozeIt(minerAccount, tx, remainRewards, stageTwo);
    }

    /**
     *
     * @param account
     * @param tx
     * @param amount
     * @param stageTwo
     */
    private static void updateBalanceAndFrozeIt(Account account, Transaction tx, long amount, boolean stageTwo){
        if(!stageTwo) {
            account.addBalanceAddUnconfirmed(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), amount);
            account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), amount);
            Logger.logDebugMessage("[Stage One]add mining rewards %d to %s unconfirmed balance and freeze it of tx %d at height %d",
                    amount, account.getRsAddress(), tx.getId() , tx.getHeight());
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
            Logger.logDebugMessage("[Stage Two]unfreeze mining rewards %d of %s and add it in mined amount of tx %d at height %d",
                    amount, account.getRsAddress(), tx.getId() , tx.getHeight());
        }
    }

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

        // Crowd Miner Reward
        long miningRewards =  tx.getAmountNQT();
        if(coinBase.isType(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD)) {
            Map<Long, Long> crowdMiners = coinBase.getCrowdMiners();
            calAndSetCrowdMinerReward(minerAccount, tx, crowdMiners, stageTwo);
            if(crowdMiners.size() > 0){
                miningRewards = tx.getAmountNQT() - crowdMinerReward(tx.getHeight());
            }
        }

        // Mining Reward (include Pool mode)
        Map<Long, Long> consignors = coinBase.getConsignors();
        if (consignors.size() == 0) {
            updateBalanceAndFrozeIt(senderAccount, tx, miningRewards, stageTwo);
        } else {
            Map<Long, Long> rewardList = PoolRule.calRewardMapAccordingToRules(senderAccount.getId(), coinBase.getGeneratorId(), miningRewards, consignors);
            for (long id : rewardList.keySet()) {
                Account account = Account.getAccount(id);
                updateBalanceAndFrozeIt(account, tx, rewardList.get(id), stageTwo);
            }
        }
        return tx.getAmountNQT();
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
        } else if(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD == coinBase.getCoinBaseType()){
            // sum the
        } else if (Attachment.CoinBase.CoinBaseType.GENESIS == coinBase.getCoinBaseType()) {
            if (!SharderGenesis.isGenesisCreator(coinBase.getCreator())) {
                throw new ConchException.NotValidException("the Genesis coin base tx is not created by genesis creator");
            }
        }
        return true;
    }

}