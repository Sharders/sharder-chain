package org.conch.mint.pool;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.chain.Block;
import org.conch.chain.BlockchainProcessor;
import org.conch.common.Constants;
import org.conch.mint.Generator;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.DiskStorageUtil;
import org.conch.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author ben-xy
 */
public class SharderPoolProcessor implements Serializable {
    private static final long serialVersionUID = 8653213465471743671L;
    private static ConcurrentMap<Long, SharderPoolProcessor> sharderPools = new ConcurrentHashMap<>();
    private static ConcurrentMap<Long, List<SharderPoolProcessor>> destroyedPools = new ConcurrentHashMap<>();
    public static final long PLEDGE_AMOUNT = 20000 * Constants.ONE_SS;

    public enum State {
        /**
         * user created pool, but not produce block yet
         */
        INIT,
        /**
         * pool is creating
         */
        CREATING,
        /**
         * pool is working
         */
        WORKING,
        /**
         * pool is abandoned
         */
        DESTROYED
    }

    private final long creatorId;
    private final long poolId;
    private final int level;
    private float chance;
    private State state;
    private final int startBlockNo;
    private int endBlockNo;
    private int historicalBlocks;
    /**
     * the sum of all destroyed mint pool's life time
     */
    private int totalBlocks;
    /**
     * total income
     */
    private long historicalIncome;
    /**
     * mint rewards
     */
    private long historicalMintRewards;
    private long mintRewards;
    /**
     * fees
     */
    private long historicalFees;
    private long power;
    private final ConcurrentMap<Long, Consignor> consignors = new ConcurrentHashMap<>();
    private int number = 0;
    private int updateHeight;
    private Map<String, Object> rule;

    public SharderPoolProcessor(long creatorId, long id, int startBlockNo, int endBlockNo) {
        this.creatorId = creatorId;
        this.poolId = id;
        this.startBlockNo = startBlockNo;
        this.endBlockNo = endBlockNo;
        this.level = PoolRule.getLevel(creatorId);
    }
    
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * check the end block to verify the pool life cycle can't exceed the end date
     * @param endBlockNo
     * @return
     */
    private static int checkAndReturnEndBlockNo(int endBlockNo){
        try {
            Date phaseOneEndDate = dateFormat.parse(Constants.TESTNET_PHASE_ONE_TIME);
            Date now = new Date();
            // phase one check
            if(now.before(phaseOneEndDate)){
                if(endBlockNo > Constants.TESTNET_PHASE_ONE) {
                    return Constants.TESTNET_PHASE_ONE;
                }
            }else {
            // phase two check
                Date phaseTwoEndDate = dateFormat.parse(Constants.TESTNET_PHASE_TWO_TIME);
                if(now.before(phaseTwoEndDate) && endBlockNo > Constants.TESTNET_PHASE_TWO) {
                    return Constants.TESTNET_PHASE_TWO;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
       return endBlockNo;
    }
    
//    private static void joinOrQuitPool(Account account,AccountLedger.LedgerEvent ledgerEvent, long amount, int height, boolean quit){
//        if(!quit) {
//            account.addToBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), amount);
//            account.frozenNQT(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), amount);
//            Logger.logDebugMessage("[Stage One]add mining rewards %d to %s unconfirmed balance and freeze it at height %d",
//                    amount, account.getRsAddress(), transaction.getId() , transaction.getHeight());
//        }else{
//            account.frozenNQT(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), -amount);
//            account.addToMintedBalanceNQT(amount);
//            Logger.logDebugMessage("[Stage Two]unfreeze mining rewards %d of %s and add it in mined amount of tx %d at height %d",
//                    amount, account.getRsAddress(), transaction.getId() , transaction.getHeight());
//        }
//    }

    public static void createSharderPool(long creatorId, long id, int startBlockNo, int endBlockNo, Map<String, Object> rule) {
        int height = startBlockNo - Constants.SHARDER_POOL_DELAY;
        endBlockNo = checkAndReturnEndBlockNo(endBlockNo);
        SharderPoolProcessor pool = new SharderPoolProcessor(creatorId, id, startBlockNo, endBlockNo);
        Account.getAccount(creatorId).frozenAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.FORGE_POOL_CREATE, -1, PLEDGE_AMOUNT);
        pool.power += PLEDGE_AMOUNT;
        
        if (destroyedPools.containsKey(creatorId)) {
            SharderPoolProcessor pastPool = newPoolFromDestroyed(creatorId);
            pool.chance = pastPool.chance;
            pool.state = State.INIT;
            pool.historicalBlocks = pastPool.historicalBlocks;
            pool.historicalIncome = pastPool.historicalIncome;
            pool.historicalFees = pastPool.historicalFees;
            pool.historicalMintRewards = pastPool.historicalMintRewards;
            pool.totalBlocks = pastPool.totalBlocks;
            pool.rule = rule;
            Logger.logDebugMessage(creatorId + " create mint pool from old pool, chance " + pastPool.chance);
        } else {
            pool.chance = 0;
            pool.state = State.INIT;
            pool.historicalBlocks = 0;
            pool.historicalIncome = 0;
            pool.historicalFees = 0;
            pool.historicalMintRewards = 0;
            pool.totalBlocks = 0;
            pool.rule = rule;
            Logger.logDebugMessage(creatorId + " create a new mint pool");
        }
        sharderPools.put(pool.poolId, pool);

        checkOrAddIntoActiveGenerator(pool);
    }

    /**
     * - set the attributes of pool
     * - calculate and reset the ref balances of pool owner and joiners
     * @param height
     */
    public void destroySharderPool(int height) {
        state = State.DESTROYED;
        endBlockNo = height;
        
        Account.getAccount(creatorId).frozenAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.FORGE_POOL_DESTROY, -1, -PLEDGE_AMOUNT);
        power -= PLEDGE_AMOUNT;
        
        for (Consignor consignor : consignors.values()) {
            long amount = consignor.getAmount();
            if (amount != 0) {
                power -= amount;
                Account.getAccount(consignor.getId()).frozenAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.FORGE_POOL_DESTROY, -1, -amount);
            }
        }
        if (startBlockNo > endBlockNo) {
            sharderPools.remove(poolId);
            Logger.logDebugMessage("destroy mint pool " + poolId + " before start ");
            return;
        }
        if (destroyedPools.containsKey(creatorId)) {
            destroyedPools.get(creatorId).add(this);
            sharderPools.remove(poolId);
        } else {
            List<SharderPoolProcessor> destroy = new ArrayList<>();
            destroy.add(this);
            destroyedPools.put(creatorId, destroy);
            sharderPools.remove(poolId);
        }
        Logger.logDebugMessage("destroy mint pool " + poolId);
    }

    public void addOrUpdateConsignor(long id, long txId, int startBlockNo, int endBlockNo, long amount) {
        if (consignors.containsKey(id)) {
            Consignor consignor = consignors.get(id);
            consignor.addTransaction(txId, startBlockNo, endBlockNo, amount);
            power += amount;
        } else {
            Consignor consignor = new Consignor(id, txId, startBlockNo, endBlockNo, amount);
            consignors.put(id, consignor);
            power += amount;
            number++;
        }
        Logger.logDebugMessage(id + " join in mint pool " + poolId);
    }

    public long quitConsignor(long id, long txId) {
        if (!consignors.containsKey(id)) {
            Logger.logErrorMessage("mint pool:" + poolId + " don't have consignor:" + id);
            return -1;
        }
        Consignor consignor = consignors.get(id);
        long amount = consignor.getTransactionAmount(txId);
        if (amount == -1) {
            Logger.logErrorMessage("consignor:" + id + " don't have transaction:" + txId);
            return -1;
        }
        power -= amount;
        if (consignor.removeTransaction(txId)) {
            consignors.remove(id);
            number--;
            Logger.logDebugMessage(id + "quit mint pool " + poolId + ",tx id is " + txId);
        }
        return amount;
    }

    public boolean hasSenderAndTransaction(long senderId, long txId) {
        if (consignors.containsKey(senderId) && consignors.get(senderId).hasTransaction(txId)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkOwnPoolState(long creator, State state) {
        SharderPoolProcessor poolProcessor = getPool(creator);
        return (poolProcessor != null) && state.equals(poolProcessor.getState());
    }

    public static long findOwnPoolId(long creator) {
        for (SharderPoolProcessor forgePool : sharderPools.values()) {
            if (forgePool.creatorId == creator) {
                return forgePool.poolId;
            }
        }
        return -1;
    }

    public static SharderPoolProcessor getPoolByCreator(long creator) {
        for (SharderPoolProcessor forgePool : sharderPools.values()) {
            if (forgePool.creatorId == creator) {
                return forgePool;
            }
        }
        return null;
    }
    
    public static JSONObject getPoolJSON(long creator) {
        SharderPoolProcessor poolProcessor = getPool(creator);
        return poolProcessor != null ? poolProcessor.toJsonObject() : new JSONObject();
    }

    private static void updateHistoricalFees(long poolId, long fee) {
        if(poolId != -1 && SharderPoolProcessor.getPool(poolId).getState().equals(SharderPoolProcessor.State.WORKING)) {
            SharderPoolProcessor pool = sharderPools.get(poolId);
            pool.historicalBlocks++;
            pool.historicalIncome += fee;
        }
    }
    
    private static void updateHistoricalRewards(long poolId, long reward) {
        if(poolId != -1 && SharderPoolProcessor.getPool(poolId).getState().equals(SharderPoolProcessor.State.WORKING)) {
            SharderPoolProcessor pool = sharderPools.get(poolId);
            pool.historicalIncome += reward;
            pool.historicalMintRewards += reward;
            pool.mintRewards += reward;
        }
    }
    
    private static void checkOrAddIntoActiveGenerator(SharderPoolProcessor sharderPool){
        if(Generator.containMiner(sharderPool.creatorId)) {
            Logger.logInfoMessage("current creator %s of pool %s already mining on this node", Account.rsAccount(sharderPool.creatorId), sharderPool.poolId);
            return;
        }
        
        if(Generator.isAutoMiningAccount(sharderPool.creatorId)){
            Logger.logInfoMessage("current creator %s of pool %s isn't mining on this node, force to open auto mining", Account.rsAccount(sharderPool.creatorId), sharderPool.poolId);
            Generator.forceOpenAutoMining();
        }
    }

    private static final String LOCAL_STORAGE_SHARDER_POOLS = "StoredSharderPools";
    private static final String LOCAL_STORAGE_DESTROYED_POOLS = "StoredDestroyedPools";

    static {
        // load pools from local cached files
        Logger.logInfoMessage("load exist pools info from local disk[" + DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_SHARDER_POOLS) + "]");
        Object poolsObj = DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_SHARDER_POOLS);
        if(poolsObj != null) {
            sharderPools = (ConcurrentMap<Long, SharderPoolProcessor>) poolsObj;
        }

        Logger.logInfoMessage("load exist destroyed pools info from local [" + DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_DESTROYED_POOLS) + "]");
        Object destroyedPoolsObj = DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_DESTROYED_POOLS);
        if(destroyedPoolsObj != null) {
            destroyedPools = (ConcurrentMap<Long, List<SharderPoolProcessor>>) destroyedPoolsObj;
        }

        // AFTER_BLOCK_APPLY event listener
        Conch.getBlockchainProcessor().addListener(block -> processNewBlockAccepted(block), 
                BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    /**
     * After block accepted:
     * - Update the pool state
     * - Update the remaining block numbers of pool
     * - Destroy pool when lifecycle finished
     * - Coinbase reward unfreeze
     * - Persistence the pool to local disk
     */
    private static void processNewBlockAccepted(Block block){
        int height = block.getHeight();
        for (SharderPoolProcessor sharderPool : sharderPools.values()) {
            sharderPool.updateHeight = height;

            if (sharderPool.consignors.size() == 0 
                && height - sharderPool.startBlockNo > Constants.SHARDER_POOL_DEADLINE) {
                sharderPool.destroySharderPool(height);
                continue;
            }
            if (sharderPool.endBlockNo == height) {
                sharderPool.destroySharderPool(height);
                continue;
            }
            
            // check the miner whether running before poll started
            if(sharderPool.startBlockNo-height <=3 
                && sharderPool.startBlockNo > height){
                checkOrAddIntoActiveGenerator(sharderPool);
            }
            
            if (sharderPool.startBlockNo == height) {
                sharderPool.state = State.WORKING;
                continue;
            }
            // TODO auto destroy pool because the number or amount of pool is too small
            if (sharderPool.startBlockNo + Constants.SHARDER_POOL_DEADLINE == height 
                && sharderPool.consignors.size() == 0) {
                sharderPool.destroySharderPool(height);
                continue;
            }
            //  time out transaction
            for (Consignor consignor : sharderPool.consignors.values()) {
                long amount = consignor.validateHeight(height);
                if (amount != 0) {
                    sharderPool.power -= amount;
                    Account account = Account.getAccount(consignor.getId());
                    Logger.logDebugMessage("frozenAndUnconfirmedBalanceNQT in Pool#processNewBlockAccepted amount[%d] account[%s]", -amount, account.getRsAddress());
                    account.frozenAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.FORGE_POOL_QUIT, 0, -amount);
                }
            }

            if (sharderPool.startBlockNo < height) {
                sharderPool.totalBlocks++;
            }
            if (sharderPool.totalBlocks == 0) {
                sharderPool.chance = 0;
            } else {
                sharderPool.chance = sharderPool.historicalBlocks / sharderPool.totalBlocks;
            }
            
        }

        // update pool summary
        long id = findOwnPoolId(block.getGeneratorId());
        updateHistoricalFees(id, block.getTotalFeeNQT());

        //unfreeze the reward
        if (height > Constants.SHARDER_REWARD_DELAY) {
            Block pastBlock = Conch.getBlockchain().getBlockAtHeight(height - Constants.SHARDER_REWARD_DELAY);
            for (Transaction transaction : pastBlock.getTransactions()) {
                Attachment attachment = transaction.getAttachment();
                if(attachment instanceof Attachment.CoinBase){
                    Attachment.CoinBase coinbaseBody = (Attachment.CoinBase) attachment;
                    if(Attachment.CoinBase.CoinBaseType.BLOCK_REWARD == coinbaseBody.getCoinBaseType()){
                        long mintReward = TransactionType.CoinBase.mintReward(transaction,true);
                        updateHistoricalRewards(id,mintReward);
                    }
                }
            }
        }

        saveToDisk();
    }

    /**
     * save the pools to disk,
     * If be called outside, the caller should be org.conch.Conch#shutdown()
     */
    public static void saveToDisk(){
        DiskStorageUtil.saveObjToFile(sharderPools, LOCAL_STORAGE_SHARDER_POOLS);
        DiskStorageUtil.saveObjToFile(destroyedPools, LOCAL_STORAGE_DESTROYED_POOLS);
    }

    public static void init() {
        PoolRule.init();
    }

    public static SharderPoolProcessor newPoolFromDestroyed(long creator) {
        if (!destroyedPools.containsKey(creator)) {
            return null;
        }
        SharderPoolProcessor past = destroyedPools.get(creator).get(0);
        for (SharderPoolProcessor destroy : destroyedPools.get(creator)) {
            if (destroy.getEndBlockNo() > past.getEndBlockNo()) {
                past = destroy;
            }
        }
        return past;
    }

    public static boolean isDead(long poolId){
        return !SharderPoolProcessor.State.WORKING.equals(getPool(poolId).getState());
    }

    public static SharderPoolProcessor getPool(long poolId) {
        return sharderPools.get(poolId);
    }
    
    public static long getCreatorIdByPoolId(long poolId) {
        SharderPoolProcessor poolProcessor = getPool(poolId);
        return poolProcessor != null ? poolProcessor.getCreatorId() : -1;
    }

    public static JSONObject getPoolsFromNow(){
        List<SharderPoolProcessor> pasts = new ArrayList<>();
        for(SharderPoolProcessor forgePool : sharderPools.values()){
            pasts.add(forgePool);
        }
        JSONArray array = new JSONArray();
        for (SharderPoolProcessor forgePool : pasts) {

            array.add(forgePool.toJsonObject());
        }
        JSONObject json = new JSONObject();
        json.put("pools",array);
        return json;
    }

    public static SharderPoolProcessor getPoolFromAll(long creatorId, long poolId) {
        SharderPoolProcessor forgePool = getPool(poolId);
        if (forgePool != null) {
            return forgePool;
        }
        for (SharderPoolProcessor destroy : destroyedPools.get(creatorId)) {
            if (destroy.poolId == poolId) {
                return destroy;
            }
        }
        return null;
    }

    public static JSONObject getPoolsFromNowAndDestroy(long creatorId) {
        SharderPoolProcessor now = null;
        for (SharderPoolProcessor forgePool : sharderPools.values()) {
            if (forgePool.creatorId == creatorId) {
                now = forgePool;
                break;
            }
        }
        List<SharderPoolProcessor> pastPools = new ArrayList<>();
        if (destroyedPools.containsKey(creatorId)) {
            pastPools = destroyedPools.get(creatorId);
        }

        if (now != null) {
            pastPools.add(now);
        }
        JSONObject jsonObject = new JSONObject();
        if (pastPools.size() == 0) {
            jsonObject.put("errorCode", 1);
            jsonObject.put("errorDescription", "current id doesn't create any mint pool");
            return jsonObject;
        }

        pastPools.forEach(pool -> jsonObject.put(pool.poolId, pool.toJsonObject()) );
        
        return jsonObject;
    }

    public Map<Long, Long> getConsignorsAmountMap() {
        Map<Long, Long> map = new HashMap<>();
        for (Consignor consignor : consignors.values()) {
            map.put(consignor.getId(), consignor.getAmount());
        }
        return map;
    }

    public boolean validateConsignorsAmountMap(Map<Long, Long> map) {
        Map<Long, Long> myMap = getConsignorsAmountMap();
        if (myMap.size() != map.size()) {
            return false;
        }
        for (Long id : myMap.keySet()) {
            if (!map.containsKey(id)) {
                return false;
            }
            if (!map.get(id).equals(myMap.get(id))) {
                return false;
            }
        }
        return true;
    }

    public int getStartBlockNo() {
        return startBlockNo;
    }

    public int getEndBlockNo() {
        return endBlockNo;
    }

    public Map<String, Object> getRule() {
        return rule;
    }

    public HashMap<String, Object> getRootRuleMap(){
        Object rootRuleMap = getRule().get("level0");
        rootRuleMap = rootRuleMap != null ? rootRuleMap : getRule().get("level1");
        return (HashMap<String, Object>) rootRuleMap;
    }

    public long getPower() {
        return power;
    }

    public State getState() {
        return state;
    }

    public ConcurrentMap<Long, Consignor> getConsignors() {
        return consignors;
    }

    public int getLevel() {
        return level;
    }

    public long getCreatorId() {
        return creatorId;
    }

    /**
     * whether creator has created a working mine pool
     *
     * @param creatorId
     * @return true(creator has working pool) or false(creator has no working pool)
     */
    public static Boolean whetherCreatorHasWorkingMinePool(Long creatorId) {
        return sharderPools.values().stream().map(SharderPoolProcessor::getCreatorId)
                .anyMatch(poolCreatorId -> poolCreatorId.equals(creatorId));
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("poolId", String.valueOf(poolId));
        jsonObject.put("creatorID", String.valueOf(creatorId));
        jsonObject.put("level", level);
        jsonObject.put("number", number);
        jsonObject.put("power", power);
        jsonObject.put("chance", chance);
        jsonObject.put("historicalBlocks", historicalBlocks);
        jsonObject.put("historicalIncome", historicalIncome);
        jsonObject.put("historicalFees", historicalFees);
        jsonObject.put("historicalMintRewards", historicalMintRewards);
        jsonObject.put("mintRewards", mintRewards);
        jsonObject.put("totalBlocks",totalBlocks);
        jsonObject.put("consignors",consignors);
        jsonObject.put("startBlockNo", startBlockNo);
        jsonObject.put("endBlockNo", endBlockNo);
        jsonObject.put("updateHeight", updateHeight);
        jsonObject.put("rule",rule);
        jsonObject.put("state", state);
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SharderPoolProcessor forgePool = (SharderPoolProcessor) o;

        return poolId == forgePool.poolId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
