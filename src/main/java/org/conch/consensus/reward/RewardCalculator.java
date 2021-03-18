package org.conch.consensus.reward;

import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.chain.Block;
import org.conch.chain.BlockDb;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.poc.PocCalculator;
import org.conch.consensus.poc.PocHolder;
import org.conch.consensus.poc.PocScore;
import org.conch.db.Db;
import org.conch.mint.pool.PoolRule;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.Attachment.CoinBase;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionDb;
import org.conch.tx.TransactionImpl;
import org.conch.util.Convert;
import org.conch.util.LocalDebugTool;
import org.conch.util.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;


/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/8
 */
public class RewardCalculator {

    /**
     * Reward definition
     * NOTE: Mining Reward = Block Reward - Crowd Miner Reward
     */
    private enum RewardDef {
        BLOCK_REWARD(1333 * Constants.ONE_SS),
        CROWD_MINERS_REWARD(667 * Constants.ONE_SS),
        ROBUST_PHASE_CROWD_MINERS_REWARD(9 * Constants.ONE_SS / 10),
        ROBUST_PHASE_BLOCK_REWARD(1 * Constants.ONE_SS),
        STABLE_PHASE_BLOCK_REWARD(1 * Constants.ONE_SS),
        STABLE_PHASE_CROWD_MINERS_REWARD(1 * Constants.ONE_SS / 2);

        private final long amount;

        public long getAmount() {
            return amount;
        }

        RewardDef(long amount) {
            this.amount = amount;
        }

    }

    /**
     * Halve height, -1 means close halve
     */
    private static final int HALVE_COUNT = -1;
    /**
     * Estimated stable height after network reset
     */
    public static final int NETWORK_STABLE_PHASE = Constants.isDevnet() ? 5 : 2008;
    /**
     * Estimated robust height after network reset
     */
    public static final int NETWORK_ROBUST_PHASE = Constants.isDevnet() ? 10 : 11111;
    /**
     * how much one block reward
     * @return
     */
    public static long blockReward(int height) {
        // Halving logic
        if(HALVE_COUNT != -1) {
            double turn = 0d;
            if(Conch.getBlockchain().getHeight() > HALVE_COUNT){
                turn = Conch.getBlockchain().getHeight() / HALVE_COUNT;
            }
            double rate = Math.pow(0.5d, turn);
            return (long)(RewardDef.BLOCK_REWARD.getAmount() * rate);
        }

        // No block rewards in the miner joining phase
        if(height <= NETWORK_STABLE_PHASE) {
            return RewardDef.STABLE_PHASE_BLOCK_REWARD.getAmount();
        } else if (height <= NETWORK_ROBUST_PHASE) {
            return RewardDef.BLOCK_REWARD.getAmount();
        } else {
            return RewardDef.ROBUST_PHASE_BLOCK_REWARD.getAmount();
        }
    }

    public static long crowdMinerReward(int height){
        if(height >= Constants.COINBASE_CROWD_MINER_OPEN_HEIGHT
        || LocalDebugTool.isLocalDebugAndBootNodeMode){
            if(height <= NETWORK_STABLE_PHASE) {
                return RewardDef.STABLE_PHASE_CROWD_MINERS_REWARD.getAmount();
            } else if (height <= NETWORK_ROBUST_PHASE) {
                return RewardDef.CROWD_MINERS_REWARD.getAmount();
            } else {
                return RewardDef.ROBUST_PHASE_CROWD_MINERS_REWARD.getAmount();
            }
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
        HashMap<Long, Long> consignorMap = Maps.newHashMap();
        long generatorId = SharderPoolProcessor.findOwnPoolId(creator.getId());
        if (generatorId == -1 || SharderPoolProcessor.isDead(generatorId)) {
            generatorId = creator.getId();
        } else {
            consignorMap = SharderPoolProcessor.getPool(generatorId).getConsignorsAmountMap();
        }

        Attachment.CoinBase coinBase = null;
        if(height >= Constants.COINBASE_CROWD_MINER_OPEN_HEIGHT
        || LocalDebugTool.isLocalDebugAndBootNodeMode){
            // crowd miner mode
            HashMap<Long, Long> crowdMinerPocScoreMap = generateCrowdMinerPocScoreMap(Lists.newArrayList(creator.getId()), height);
            coinBase = new CoinBase(creator.getId(), generatorId, consignorMap, crowdMinerPocScoreMap);
        }else{
            // single miner or pool reward mode
            coinBase = new CoinBase(CoinBase.CoinBaseType.BLOCK_REWARD, creator.getId(), generatorId, consignorMap);
        }

        return new TransactionImpl.BuilderImpl(
                publicKey,
                blockReward(height),
                0,
                (short) 10,
                coinBase);
    }

    /***
     *
     */
    private static final String CROWD_MINER_CONFIG_PATH = "./conf/crowd_miner.json";
    public static final boolean EXIST_CROWD_MINER_CONFIG = containPeerConfig();
    private static HashMap<Long, Long> localPeerAndScoreMap = null;

    private static boolean containPeerConfig(){
//        File file = new File(CROWD_MINER_CONFIG_PATH);
//        return file.exists();
        return false;
    }

    private static HashMap<Long,Long> readFromConfigFile() {
        Logger.logInfoMessage("List all peer and score pairs from config file");
        HashMap<Long,Long> peerAndScoreMap = Maps.newHashMap();
        File file = new File(CROWD_MINER_CONFIG_PATH);
        if(!file.exists()) {
            return peerAndScoreMap;
        }

        FileReader fr = null;
        try {
            fr = new FileReader(file);
            char[] data = new char[23];
            int length = 0;
            StringBuilder stringBuilder = new StringBuilder();
            while((length = fr.read(data))>0){
                stringBuilder.append(new String(data, 0, length));
            }
            Map map = com.alibaba.fastjson.JSONObject.parseObject(stringBuilder.toString(), Map.class);
            Random random = new Random();
            for (Object oj : map.keySet()) {
                com.alibaba.fastjson.JSONObject jsonObject = (com.alibaba.fastjson.JSONObject) map.get(oj);
                CertifiedPeer certifiedPeer = null;
                try{
                    String host = jsonObject.getString("host");
                    Long linkedAccountId= jsonObject.getLong("boundAccountId");
                    Peer.Type type = Peer.Type.getByCode(jsonObject.getInteger("typeCode"));
                    // use the height as the score
                    int score = jsonObject.getInteger("height");
                    int lastUpdateEpochTime = jsonObject.getInteger("updateTimeInEpochFormat");

                    if(!peerAndScoreMap.containsKey(linkedAccountId)){
                        peerAndScoreMap.put(linkedAccountId, Long.valueOf(score * random.nextInt(10000)));
                    }
                }catch (Exception e) {
                    Logger.logDebugMessage(e.getMessage());
                    continue;
                }
            }
            fr.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return peerAndScoreMap;
    }

    /**
     * read the current qualified miners and calculate the reward distribution according to poc score rate:
     * - qualified condition:
     * a) create a tx to declared the node;
     * b) linked the mining address;
     * c) current height's balance  > 4256（32T staking amount）;
     * @return map: miner's account id : poc score
     */
    private static long QUALIFIED_CROWD_MINER_HOLDING_AMOUNT_MIN = 32*133L; // 1T-133
    private static HashMap<Long, Long> generateCrowdMinerPocScoreMap(List<Long> exceptAccounts, int height){
        if(EXIST_CROWD_MINER_CONFIG) {
            if(localPeerAndScoreMap == null || localPeerAndScoreMap.size() == 0) {
                localPeerAndScoreMap = readFromConfigFile();
            }
            return localPeerAndScoreMap;
        }

        HashMap<Long, Long> crowdMinerPocScoreMap = Maps.newHashMap();
        // read the qualified miner list
        Map<Long, CertifiedPeer> certifiedPeers = Conch.getPocProcessor().getCertifiedPeers();
        if (certifiedPeers == null || certifiedPeers.size() == 0) {
            return crowdMinerPocScoreMap;
        }
        long startMS = System.currentTimeMillis();
        Logger.logDebugMessage("Start to generate crow miner poc-score map [miner size=%d]", certifiedPeers.size());
        // generate the poc score map
        for (CertifiedPeer certifiedPeer : certifiedPeers.values()) {
            // only reward once for same miner
            if (exceptAccounts != null
                    && exceptAccounts.contains(certifiedPeer.getBoundAccountId())) {
                continue;
            }
            // qualified miner judgement
            Account declaredAccount = Account.getAccount(certifiedPeer.getBoundAccountId());
            if (declaredAccount == null) {
                continue;
            }

            long holdingMwAmount = 0;
            try{
//                holdingMwAmount = declaredAccount.getEffectiveBalanceSS(height);
                holdingMwAmount = declaredAccount.getConfirmedEffectiveBalanceSS(height);
            }catch(Exception e){
                Logger.logWarningMessage("[QualifiedMiner] not valid miner because can't get balance of account %s at height %d, caused by %s",  declaredAccount.getRsAddress(), height, e.getMessage());
                holdingMwAmount = 0;
            }
            if(holdingMwAmount < QUALIFIED_CROWD_MINER_HOLDING_AMOUNT_MIN) {
                continue;
            }

            // poc score judgement
            PocScore pocScore = PocHolder.getPocScore(height, declaredAccount.getId());
            if (pocScore == null || pocScore.total().longValue() <= 0) {
                continue;
            }
            crowdMinerPocScoreMap.put(declaredAccount.getId(), pocScore.total().longValue());
        }
        long usedTimeMS = System.currentTimeMillis() - startMS;
        Logger.logDebugMessage("Finish generate crow miner poc-score map[used time≈%dS]", usedTimeMS / 1000);
        return crowdMinerPocScoreMap;
    }

    /**
     * Total capacity of qualified miner hardware
     * @return
     * @param height
     * @param boundAccountList
     */
    public static String crowdMinerHardwareCapacity(int height, List<Long> boundAccountList) {
        // TODO add cache, per 10min cache once
        Long crowdMinerHardwareScoreTotal = 0L;
        if (boundAccountList != null) {
            // read the qualified miner list
            for (Long boundAccountId : boundAccountList) {
                // poc score judgement
                PocScore pocScore = PocHolder.getPocScore(height, boundAccountId);
                crowdMinerHardwareScoreTotal += pocScore.getHardwareScore().longValue();
            }
        } else {
            // generate the poc score map
            for(Long boundAccountId : Conch.getPocProcessor().getCertifiedPeers().keySet()){
                // poc score judgement
                PocScore pocScore = PocHolder.getPocScore(height, boundAccountId);
                crowdMinerHardwareScoreTotal += pocScore.getHardwareScore().longValue();
            }
        }
        return PocCalculator.hardwareCapacity(new BigInteger(crowdMinerHardwareScoreTotal.toString()));
    }

    /**
     * Check whether reach the settlement height
     * Settle all un-settlement blocks before this height
     * Combine changes of the same account from these blocks
     *
     * NOTE: db transaction should be open by outside caller
     *
     * @param tx coinbase tx of block at the settlement height
     */
    private static boolean checkAndSettleCrowdMinerRewards(Transaction tx) throws ConchException.StopException {
        if (!Db.db.isInTransaction()) {
            throw new IllegalStateException("RewardCalculator#checkAndSettleCrowdMinerRewards method should in a " +
                    "transaction, open the tx before call this method");
        }
        try {
            int settlementHeight = tx.getHeight();
            if (settlementHeight <= 0) {
                Logger.logWarningMessage("Can't finish the crowd miner rewards settlement when height %d <= 0. Break " +
                        "and wait next turn.", settlementHeight);
                return false;
            }

            //if (!Constants.reachRewardSettlementHeight(settlementHeight)) {
            if (!BlockDb.reachRewardSettlementHeight(settlementHeight)) {
                Logger.logDebugMessage("Current height %d not reach the crowd miner rewards settlement height. Break " +
                        "and wait next turn.", settlementHeight);
                return false;
            }

            // Settlement
            Map<Long, Long> crowdMinerRewardMap = Maps.newHashMap();
            List<Long> blockIds = Lists.newArrayList();
            List<? extends Block> rewardDistributionTxs = BlockDb.getSettlementBlocks(settlementHeight);
            if (rewardDistributionTxs == null || rewardDistributionTxs.size() == 0) {
                Logger.logDebugMessage("No crowd rewards txs need be settlement at current height %d, " +
                        "maybe these txs be distributed at settlement height %d already.", Conch.getHeight(),
                        settlementHeight);
                return false;
            }

            for (Block block : rewardDistributionTxs) {
                List<TransactionImpl> blockTransactions = TransactionDb.findBlockTransactions(block.getId());
                Transaction rewardTx = getCoinBase(blockTransactions);
                if (null == rewardTx) {
                    continue;
                }
                Attachment.CoinBase coinBase = (Attachment.CoinBase) rewardTx.getAttachment();
                Account minerAccount = Account.getAccount(coinBase.getCreator());

                long totalPocScoreLong = 0;
                Map<Long, Long> crowdMiners = coinBase.getCrowdMiners();
                for(long pocScore : crowdMiners.values()){
                    totalPocScoreLong += pocScore;
                }

                BigDecimal totalPocScore = BigDecimal.valueOf(totalPocScoreLong);
                long crowdMinerRewards = crowdMinerReward(tx.getHeight());
                BigDecimal crowdMinerRewardsAmount = new BigDecimal(crowdMinerRewards);
                // calculate the single miner's rewards
                long allocatedRewards = 0;

                for (Long accountId : crowdMiners.keySet()) {
                    long pocScoreLong = crowdMiners.get(accountId);
                    BigDecimal pocScore = BigDecimal.valueOf(pocScoreLong);
                    BigDecimal pocScoreRate = pocScore.divide(totalPocScore, 10, BigDecimal.ROUND_DOWN);
                    long rewards = crowdMinerRewardsAmount.multiply(pocScoreRate).longValue();
                    if (crowdMinerRewardMap.containsKey(accountId)) {
                        rewards = rewards + crowdMinerRewardMap.get(accountId);
                    }
                    crowdMinerRewardMap.put(accountId, rewards);
                    allocatedRewards += rewards;
                }

                // remain amount distribute to creator
                long remainRewards = (crowdMinerRewards > allocatedRewards) ? (crowdMinerRewards - allocatedRewards) : 0;
                if (remainRewards != 0) {
                    if (crowdMinerRewardMap.containsKey(minerAccount.getId())) {
                        remainRewards = remainRewards + crowdMinerRewardMap.get(minerAccount.getId());
                    }
                    crowdMinerRewardMap.put(minerAccount.getId(), remainRewards);
                }
                blockIds.add(block.getId());
            }
            String details = "";
            String notExistAccounts = "";
            for (Long accountId : crowdMinerRewardMap.keySet()) {
                Account account = Account.addOrGetAccount(accountId);
                if (account == null) {
                    notExistAccounts += accountId + ",";
                    continue;
                }
                if (crowdMinerRewardMap.get(accountId) == 0) {
                    Logger.logDebugMessage("reward is zero , account is " + accountId);
                }
                details += updateBalance(account, tx, crowdMinerRewardMap.get(accountId));
            }
            BlockDb.updateDistributionState(blockIds, settlementHeight);
            String tail = "[DEBUG] ----------------------------\n[DEBUG] Total count: " + (crowdMinerRewardMap.size());
            Logger.logDebugMessage("[%d-StageTwo%s] Unfreeze crowdMiners rewards and add it in mined amount. \n[DEBUG] CrowdMiner Reward Detail Format: txid | address: distribution amount\n%s%s\n", settlementHeight, "", details, tail);
            if(notExistAccounts.length() > 0) {
                Logger.logDebugMessage("Not exist crowd miners can't distribute the rewards [%s]", settlementHeight, "", details, tail);
            }
        } catch (Exception e) {
            if (e instanceof ConchException.StopException) {
                throw e;
            }
            Logger.logErrorMessage("setCrowdMinerReward occur error", e);
            throw new RuntimeException("Distribute rewards error");
        }
        return true;
    }

    private static Transaction getCoinBase(List<TransactionImpl> blockTransactions) {
        for (Transaction transaction : blockTransactions) {
            if(isBlockRewardTx(transaction.getAttachment())) {
                return transaction;
            }
        }
        return null;
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
     * reward calculation algo.: miner's PoC score / total miner's PoC score * 667
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

        if (crowdMiners.size() == 0) {
            return crowdMinerRewardArray;
        }

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
            if (crowdMinerRewardMap.containsKey(accountId)) {
                continue;
            }

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
        try {
            if (!stageTwo) {
                account.addBalanceAddUnconfirmed(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), amount);
                account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), amount);
            } else {
                if (Constants.isTestnet() && account.getFrozenBalanceNQT() <= 0) {
                    account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), account.getFrozenBalanceNQT());
                } else if (Constants.isTestnet() && account.getFrozenBalanceNQT() <= amount) {
                    account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), -account.getFrozenBalanceNQT());
                } else {
                    account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), -amount);
                }
                account.addMintedBalance(amount);
                account.pocChanged();
            }
            return String.format("[DEBUG] txid/%d | %s: %d\n", tx.getId(), account.getRsAddress(), amount);
        } catch (Exception e) {
            Logger.logErrorMessage("updateBalanceAndFrozeIt occur error", e);
            return "";
        }
    }

    /**
     *
     * @param account
     * @param tx
     * @param amount
     */
    private static String updateBalance(Account account, Transaction tx, long amount){
        account.addBalanceAddUnconfirmed(AccountLedger.LedgerEvent.BLOCK_GENERATED, tx.getId(), amount);
        account.addMintedBalance(amount);
        account.pocChanged();
        return  String.format("[DEBUG] txid/%d | %s: %d\n", tx.getId(), account.getRsAddress(), amount);
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
    public static long blockRewardDistribution(Transaction tx, boolean stageTwo) throws ConchException.StopException {
        Attachment.CoinBase coinBase = (Attachment.CoinBase) tx.getAttachment();
        Account senderAccount = Account.getAccount(tx.getSenderId());
        Account minerAccount = Account.getAccount(coinBase.getCreator());

        rewardCalStartMS = System.currentTimeMillis();

        String stage = stageTwo ? "Two" : "One";
        // Mining Reward
        long miningRewards =  tx.getAmountNQT();
        Map<Long, Long> crowdMiners = Maps.newHashMap();
        boolean crowdRewardsDistributed = false;
        if(coinBase.isType(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD)) {
            crowdMiners = coinBase.getCrowdMiners();
            Logger.logDebugMessage("[Height%d-Rewards-Stage%s] Distribute crowd miner's rewards[crowd miner size=%d] at height %d", tx.getHeight(), stage, crowdMiners.size(), Conch.getHeight());

            crowdRewardsDistributed = checkAndSettleCrowdMinerRewards(tx);

            if(crowdMiners.size() > 0){
                miningRewards = tx.getAmountNQT() - crowdMinerReward(tx.getHeight());
            }
        }
        long crowdRewardProcessingMS = System.currentTimeMillis() - rewardCalStartMS;
        long miningCalStartMS = System.currentTimeMillis();
        // Mining Reward (include Pool mode)
        Map<Long, Long> consignors = coinBase.getConsignors();
        Logger.logDebugMessage("[Height%d-Rewards-Stage%s] Distribute block mining's rewards[ mining joiner size=%d] at height %d. " +
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
            Logger.logDebugMessage("[Height%d-StageOne] Add mining rewards to account's unconfirmed balance and freeze it. \n[DEBUG] Mining Reward Detail Format { txid | address: distribution amount }\n%s%s\n", tx.getHeight(), details, tail);
        }else {
            Logger.logDebugMessage("[Height%d-StageTwo] Unfreeze mining rewards and add it in mined amount. \n[DEBUG] Mining Reward Detail Format { txid | address: distribution amount }\n%s%s\n", tx.getHeight(), details, tail);
        }

        long miningRewardProcessingMS = System.currentTimeMillis() - miningCalStartMS;
        long totalUsedMs = System.currentTimeMillis() - rewardCalStartMS;
        Peer feeder = Conch.getBlockchainProcessor().getLastBlockchainFeeder();
        String crowdInfo = crowdRewardsDistributed ? "-IncludeCrowdSettlement" : "";
        if(feeder != null &&  Conch.getBlockchainProcessor().isDownloading()) {
            String feederAddress = feeder != null ? feeder.getAnnouncedAddress() : "UndefinedAddress";
            String feederHost = feeder != null ? feeder.getHost() : "UndefinedHost";
            Logger.logInfoMessage("[Height %d-Stage%s%s] Distribute rewards of block(%s mined at %s) used time[crowd miners≈%dS, mining joiners≈%dS, total used time≈%dS], rewards[crowd miner size=%d, mining joiner size=%d] at current height %d -> height %d of feeder %s[%s]\n",
                    tx.getHeight(), stage, crowdInfo
                    , minerAccount.getRsAddress(), Convert.dateFromEpochTime(tx.getBlockTimestamp())
                    , crowdRewardProcessingMS / 1000, miningRewardProcessingMS / 1000, totalUsedMs / 1000
                    , crowdMiners.size(), miningJoinerCount, Conch.getHeight()
                    , Conch.getBlockchainProcessor().getLastBlockchainFeederHeight(), feederAddress, feederHost);
        }else {
            Logger.logInfoMessage("[Height %d-Stage%s%s] Distribute rewards of block(%s mined at %s) used time[crowd miners≈%dS, mining joiners≈%dS, total used time≈%dS], rewards[crowd miner size=%d, mining joiner size=%d] at current height %d\n",
                    tx.getHeight(), stage, crowdInfo
                    , minerAccount.getRsAddress(), Convert.dateFromEpochTime(tx.getBlockTimestamp())
                    , crowdRewardProcessingMS / 1000, miningRewardProcessingMS / 1000, totalUsedMs / 1000
                    , crowdMiners.size(), miningJoinerCount, Conch.getHeight());
        }
        return tx.getAmountNQT();
    }

    /**
     * if the commonBlockHeight less than the latest reward height, roll back the reward height of block to 0.
     * @param commonBlockHeight
     * @throws RuntimeException
     */
    public static void rollBackTo (int commonBlockHeight) throws RuntimeException {
        if (!Db.db.isInTransaction()) {
            try {
                Db.db.beginTransaction();
                rollBackTo(commonBlockHeight);
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
        }
        int latestRewardHeight = BlockDb.getLatestRewardHeight();
        if (latestRewardHeight <= commonBlockHeight) {
            return;
        }
        BlockDb.rollBackRewardHeight(latestRewardHeight);
        rollBackTo(commonBlockHeight);

    }

    /**
     * CoinBase tx attachment judgement
     * @param attachment
     * @return
     */
    public static boolean isBlockRewardTx(Attachment attachment) {
        if(!(attachment instanceof Attachment.CoinBase)) {
            return false;
        }

        Attachment.CoinBase coinbaseBody = (Attachment.CoinBase) attachment;
        return coinbaseBody.isType(Attachment.CoinBase.CoinBaseType.BLOCK_REWARD)
                || coinbaseBody.isType(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD);
    }

    public static boolean isBlockCrowdRewardTx(Attachment attachment) {
        if(!(attachment instanceof Attachment.CoinBase)) {
            return false;
        }

        Attachment.CoinBase coinbaseBody = (Attachment.CoinBase) attachment;
        return coinbaseBody.isType(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD);
    }

    private static Attachment.CoinBase parseToCoinBase(Attachment attachment){
        if(!(attachment instanceof Attachment.CoinBase)) {
            return null;
        }
        return (Attachment.CoinBase) attachment;
    }

    public static int crowdMinerCount(Attachment attachment) {
        try{
            Attachment.CoinBase coinBaseObj = parseToCoinBase(attachment);
            if(coinBaseObj == null) {
                return -1;
            }

            if(coinBaseObj.isType(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD)) {
                return coinBaseObj.getCrowdMiners() != null ? coinBaseObj.getCrowdMiners().size() : 0;
            }
        }catch(Exception e){
            Logger.logErrorMessage("calculate the size of crowd miners failed", e);
        }
        return 0;
    }

    public static boolean applyUnconfirmedReward(TransactionImpl transaction) {
        return false;
    }

    // temporary switch
    public static boolean closeValidationForCrowdCoinbaseTx = false;

}