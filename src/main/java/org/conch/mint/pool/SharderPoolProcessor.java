package org.conch.mint.pool;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.chain.Block;
import org.conch.chain.BlockchainProcessor;
import org.conch.common.Constants;
import org.conch.consensus.poc.db.PoolDb;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.mint.Generator;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wuji
 * @date  2019-01-16 updated by Ben: fix the pool tx bugs
 * @date  2019-05-15 updated by Ben: dirty pool tx remove
 */
public class SharderPoolProcessor implements Serializable {
    private static final long serialVersionUID = 8653213465471743671L;
    private static ConcurrentMap<Long, SharderPoolProcessor> sharderPools = Maps.newConcurrentMap();
    private static ConcurrentMap<Long, List<SharderPoolProcessor>> destroyedPools = Maps.newConcurrentMap();
    public static final long PLEDGE_AMOUNT_NQT = 20000 * Constants.ONE_SS;
    public static final long POOL_MAX_AMOUNT_NQT = 500000 * Constants.ONE_SS;
    
    // join tx id <-> tx id
    private static ConcurrentMap<Long, Long> processingQuitTxMap = Maps.newConcurrentMap();
    // creator id <-> tx id
    private static ConcurrentMap<Long, Long> processingCreateTxMap = Maps.newConcurrentMap();
    // pool id <-> tx id
    private static ConcurrentMap<Long, Long> processingDestroyTxMap = Maps.newConcurrentMap();
    
    public enum State {
        INIT, //user created pool, but not produce block yet
        CREATING,
        WORKING,
        DESTROYED
    }

    private long creatorId;
    private long poolId;
    private int level;
    private float chance;
    private State state;
    private int startBlockNo;
    private int endBlockNo;
    private int historicalBlocks;
    /**
     * the sum of all destroyed mining pool's life time
     */
    private int totalBlocks;
    /**
     * total incomes
     */
    private long historicalIncome;
    /**
     * mining rewards
     */
    private long historicalMintRewards;
    private long mintRewards;
    /**
     * fees
     */
    private long historicalFees;
    private long power = 0;
    private long joiningAmount = 0;
    private ConcurrentMap<Long, Consignor> consignors = new ConcurrentHashMap<>();
    private int number = 0;
    private int updateHeight;
    private Map<String, Object> rule;
    
    public SharderPoolProcessor(){} 

    public SharderPoolProcessor(long creatorId, long poolId, int startBlockNo, int endBlockNo) {
        this.creatorId = creatorId;
        this.poolId = poolId;
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
    
    
    public static boolean addProcessingQuitTx(long relatedJoinTxId, long txId){
        if(processingQuitTxMap.containsKey(relatedJoinTxId) 
            && processingQuitTxMap.get(relatedJoinTxId) != -1) {
            return false;
        }

        processingQuitTxMap.put(relatedJoinTxId, txId);
        return true;
    }

    public static void delProcessingQuitTx(long joinTxId){
        if(!processingQuitTxMap.containsKey(joinTxId)) return;
        
        processingQuitTxMap.remove(joinTxId);
    }
    
    public static long hasProcessingQuitTx(long joinTxId){
        return processingQuitTxMap.containsKey(joinTxId) ? processingQuitTxMap.get(joinTxId) : -1;
    }

    public static boolean addProcessingCreateTx(long creatorId, long txId){
        if(processingCreateTxMap.containsKey(creatorId)
            && processingCreateTxMap.get(creatorId) != -1) {
            return false;
        }

        processingCreateTxMap.put(creatorId, txId);
        return true;
    }

    public static void delProcessingCreateTx(long creatorId, long txId){
        if(!processingCreateTxMap.containsKey(creatorId)) return;
        
        if(processingCreateTxMap.get(creatorId) == txId) {
            processingCreateTxMap.remove(creatorId);  
        }
    }

    public static long hasProcessingCreateTx(long creatorId){
        return processingCreateTxMap.containsKey(creatorId) ? processingCreateTxMap.get(creatorId) : -1;
    }

    public static boolean addProcessingDestroyTx(long poolId, long txId){
        if(processingDestroyTxMap.containsKey(poolId)
            && processingDestroyTxMap.get(poolId) != -1) {
            return false;
        }

        processingDestroyTxMap.put(poolId, txId);
        return true;
    }

    public static void delProcessingDestroyTx(long poolId){
        if(!processingDestroyTxMap.containsKey(poolId)) return;

        processingDestroyTxMap.remove(poolId);
    }
    
    public static long hasProcessingDestroyTx(long poolId){
        return processingDestroyTxMap.containsKey(poolId) ? processingDestroyTxMap.get(poolId) : -1;
    }
    
    public static SharderPoolProcessor createSharderPool(long creatorId, long poolId, int startBlockNo, int endBlockNo, Map<String, Object> rule) {
        Account creator = Account.getAccount(creatorId);
        SharderPoolProcessor poolProcessor = SharderPoolProcessor.getPoolByCreator(creatorId);
        boolean notExist = (poolProcessor == null) || (poolProcessor.state == State.DESTROYED);
        if(PoolDb.countByAccountId(creatorId, State.WORKING.ordinal()) > 0
            || !notExist){
            Logger.logDebugMessage(Account.rsAccount(creatorId) + " has a working pool[pool id=%d] already, can't create a new pool", poolProcessor.poolId);
            return null;
        }
        
        endBlockNo = checkAndReturnEndBlockNo(endBlockNo);
        SharderPoolProcessor pool = new SharderPoolProcessor(creatorId, poolId, startBlockNo, endBlockNo);
        creator.addFrozenSubBalanceSubUnconfirmed(AccountLedger.LedgerEvent.FORGE_POOL_CREATE, pool.getPoolId(), PLEDGE_AMOUNT_NQT);
        pool.power += PLEDGE_AMOUNT_NQT;
        
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
            Logger.logDebugMessage(creatorId + " create a mining pool from old pool, chance " + pastPool.chance);
        } else {
            pool.chance = 0;
            pool.state = State.INIT;
            pool.historicalBlocks = 0;
            pool.historicalIncome = 0;
            pool.historicalFees = 0;
            pool.historicalMintRewards = 0;
            pool.totalBlocks = 0;
            pool.rule = rule;
            Logger.logDebugMessage(creatorId + " create a new mining pool");
        }
        sharderPools.put(pool.poolId, pool);

        checkOrAddIntoActiveGenerator(pool);
        
        return pool;
    }

    /**
     * destroy and remove the specified pools
     * @param removeMap height - removed creator ids
     * @return
     */
    public static boolean removePools(Map<Integer,Set<Long>> removeMap){
        if(removeMap == null || removeMap.size() <= 0 || sharderPools == null || sharderPools.size() <= 0) return false;
        
        // Removed the pool in which the start height is lower than the specified height
        Set<Integer> removeHeightSet = removeMap.keySet();

        Set<Long> removePoolId = Sets.newHashSet();
        for(Integer removeHeight : removeHeightSet){
            sharderPools.values().forEach(pool -> {
                if(pool.startBlockNo <= removeHeight
                    && removeMap.get(removeHeight).contains(pool.creatorId)) {
                    try{
                        pool.destroyAndRecord(removeHeight);
                        removePoolId.add(pool.poolId);
                    }catch(Exception e){
                        Logger.logDebugMessage("destroy dirty mining pool [id=%d, account=%s] at height [%d] failed caused by [%s], ignore and continue to do next one", 
                                pool.poolId, Account.rsAccount(pool.creatorId), removeHeight, e.getMessage());
                    }
                }
            });
        }
        
        if(removePoolId.size() > 0) {
            Logger.logInfoMessage("removed dirty pools " + Arrays.toString(removePoolId.toArray()));
        }
        
        return true;
    }
    
    private void destroy(int height){
        state = State.DESTROYED;
        endBlockNo = height;
        Account creator = Account.getAccount(creatorId);

        creator.addFrozenSubBalanceSubUnconfirmed(AccountLedger.LedgerEvent.FORGE_POOL_DESTROY, poolId, -PLEDGE_AMOUNT_NQT);
        power -= PLEDGE_AMOUNT_NQT;

        for (Consignor consignor : consignors.values()) {
            long amount = consignor.getAmount();
            if (amount <= 0) continue;

            power -= amount;
            Account account = Account.getAccount(consignor.getId());
            account.addFrozenSubBalanceSubUnconfirmed(AccountLedger.LedgerEvent.FORGE_POOL_DESTROY, poolId, -amount);
        }
    }

    
    /**
     * - set the attributes of pool
     * - calculate and reset the ref balances of pool owner and joiners
     * - update the records
     * @param height
     */
    public void destroyAndRecord(int height) {
        destroy(height);

        if (startBlockNo > endBlockNo) {
            sharderPools.remove(poolId);
            Logger.logDebugMessage("destroy mining pool " + poolId + " before it running ");
            return;
        }
        if (destroyedPools.containsKey(creatorId)) {
            destroyedPools.get(creatorId).add(this);
            sharderPools.remove(poolId);
        } else {
            List<SharderPoolProcessor> destroy = Lists.newArrayList(this);
            destroyedPools.put(creatorId, destroy);
            sharderPools.remove(poolId);
        }
        Logger.logDebugMessage("destroy mining pool [id=%d, creator=%s,height=%d]", poolId, Account.rsAccount(creatorId), height);
    }
    
    public boolean consignorHasTx(long accountId, long txId){
        
        if (!consignors.containsKey(accountId)) return false;
        
        Consignor consignor = consignors.get(accountId);
        return consignor.hasTx(txId);
    }

    public void addOrUpdateConsignor(long accountId, long txId, int startBlockNo, int endBlockNo, long amount) {
        if (consignors.containsKey(accountId)) {
            Consignor consignor = consignors.get(accountId);
            if(consignor.hasTx(txId)) {
                Logger.logDebugMessage("the tx[id=%d] of this account " + accountId + " already joined this mining pool", txId);
                return;
            }
            consignor.addTransaction(txId, startBlockNo, endBlockNo, amount);
            power += amount;
        } else {
            Consignor consignor = new Consignor(accountId, txId, startBlockNo, endBlockNo, amount);
            consignors.put(accountId, consignor);
            power += amount;
            number++;
        }
        Logger.logDebugMessage("account " + accountId + " join in mining pool " + poolId);
    }

    public long quitConsignor(long accountId, long txId) {
        if (!consignors.containsKey(accountId)) {
            Logger.logErrorMessage("mining pool:" + poolId + " don't have this consignor:" + accountId);
            return -1;
        }
        Consignor consignor = consignors.get(accountId);
        long amount = consignor.getTransactionAmount(txId);
        if (amount == -1) {
            Logger.logErrorMessage("consignor:" + accountId + " don't have this tx [id=" + txId + "]");
            return -1;
        }
        power -= amount;
        if (consignor.removeTransaction(txId)) {
            consignors.remove(accountId);
            number--;
            Logger.logDebugMessage("account[id=" + accountId + "] quit mining pool[pool id=" + poolId + ",tx id=" + txId + "]");
        }
        return amount;
    }
    
    private static void autoQuitWhenTxLifeEnd(SharderPoolProcessor sharderPool, int height){
        //  life end txs processing
        for (Consignor consignor : sharderPool.consignors.values()) {
            long amount = consignor.validateHeightAndRemove(height);
            if(amount <= 0) continue;
            
            sharderPool.power -= amount;
            Account account = Account.getAccount(consignor.getId());
            Logger.logDebugMessage("auto quit when the tx's life is end. amount[%d] account %s [id=%d]", -amount, account.getRsAddress(), account.getId());
            account.addFrozenSubBalanceSubUnconfirmed(AccountLedger.LedgerEvent.FORGE_POOL_QUIT, sharderPool.getPoolId(), -amount);
        }
    }

    public boolean hasSenderAndTransaction(long senderId, long txId) {
        if (consignors.containsKey(senderId) 
            && consignors.get(senderId).hasTx(txId)) {
            return true;
        } else {
            return false;
        }
    }

    public static long findOwnPoolId(long creator) {
        for (SharderPoolProcessor forgePool : sharderPools.values()) {
            if (forgePool.creatorId == creator) {
                return forgePool.poolId;
            }
        }
        return -1;
    }

    public static long findOwnPoolId(long creator, int height) {
        if(height <= 0) height = 0;
       
        for (SharderPoolProcessor pool : sharderPools.values()) {
            int poolCreateHeight = pool.startBlockNo - Constants.SHARDER_POOL_DELAY;
            if(poolCreateHeight <= 0) {
                poolCreateHeight = 0;
            }
            
            if (pool.creatorId == creator && (height >= pool.startBlockNo || height >= poolCreateHeight)) {
                return pool.poolId;
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
        Generator.addMiner(sharderPool.creatorId);
        
        if(Generator.containMiner(sharderPool.creatorId)) {
            Logger.logInfoMessage("current creator %s of pool %s already mining on this node", Account.rsAccount(sharderPool.creatorId), sharderPool.poolId);
            return;
        }
        
        if(Generator.isAutoMiningAccount(sharderPool.creatorId)){
            Logger.logInfoMessage("current creator %s of pool %s isn't mining on this node, force to open auto mining", Account.rsAccount(sharderPool.creatorId), sharderPool.poolId);
            Generator.forceOpenAutoMining();
        }
    }
    
    /**
     * After block accepted:
     * - Update the pool state
     * - Update the remaining block numbers of pool
     * - Destroy pool when lifecycle finished
     * - Coinbase reward unfreeze
     * - Persistence the pool to local disk
     */
    private static void acceptNewBlock(Block block){
        int height = block.getHeight();
        
        for (SharderPoolProcessor sharderPool : sharderPools.values()) {
            sharderPool.updateHeight = height;
            sharderPool.clearJoiningAmount();
            
            // never end the pool auto after the Constants.POC_POOL_NEVER_END_HEIGHT
            if(height < Constants.POC_POOL_NEVER_END_HEIGHT){
                //pool will be destroyed automatically when it has nobody join
                if (sharderPool.consignors.size() == 0
                        && height - sharderPool.startBlockNo > Constants.SHARDER_POOL_DEADLINE) {
                    sharderPool.destroyAndRecord(height);
                    continue;
                }
                if (sharderPool.endBlockNo <= height) {
                    sharderPool.destroyAndRecord(sharderPool.endBlockNo);
                    continue;
                }  
            }
            
            // check the miner whether running before pool started
            if(sharderPool.startBlockNo-height <=3 
                && sharderPool.startBlockNo > height){
                checkOrAddIntoActiveGenerator(sharderPool);
            }
            
            if (sharderPool.startBlockNo == height) {
                sharderPool.state = State.WORKING;
                continue;
            }

            autoQuitWhenTxLifeEnd(sharderPool, height);

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
                if(!(attachment instanceof Attachment.CoinBase)) continue;
                
                Attachment.CoinBase coinbaseBody = (Attachment.CoinBase) attachment;
                if(!coinbaseBody.isType(Attachment.CoinBase.CoinBaseType.BLOCK_REWARD)) continue;
                
                long mintReward = TransactionType.CoinBase.mintReward(transaction,true);
                updateHistoricalRewards(id,mintReward);
            }
        }

        persistence();
    }

    /**
     * roll back to specified height
     * @param height
     */
    public static void rollback(int height){
        List<SharderPoolProcessor> poolList = Lists.newArrayList();
        List<SharderPoolProcessor> deleteList = Lists.newArrayList();

        if(sharderPools.size() > 0){
            List<Long> tmpList = Lists.newArrayList();
            sharderPools.values().forEach(poolProcessor -> {
                if(poolProcessor.getStartBlockNo() <= height
                    && height <= poolProcessor.getEndBlockNo()){
                    poolList.add(poolProcessor);
                }else{
                    tmpList.add(poolProcessor.getCreatorId());
                    deleteList.add(poolProcessor);
                }
            });
            
            if(tmpList.size() > 0) {
                tmpList.forEach(creatorId -> sharderPools.remove(creatorId));
            }
        }

        if(destroyedPools.size() > 0){
            Set<Long> accountIds = destroyedPools.keySet();
            for(Long accountId : accountIds){
                List<SharderPoolProcessor> poolsInDeletion = destroyedPools.get(accountId);
                if(poolsInDeletion == null || poolsInDeletion.size() <=0 ) continue;

                List<SharderPoolProcessor> tmpList = Lists.newArrayList();
                for(SharderPoolProcessor poolProcessor : poolsInDeletion){
                    if(poolProcessor.getStartBlockNo() <= height 
                    && height <= poolProcessor.getEndBlockNo()){
                        tmpList.add(poolProcessor);
                        
                        poolProcessor.setState(State.WORKING);
                        sharderPools.put(poolProcessor.getCreatorId(),poolProcessor);
                        poolList.add(poolProcessor);
                    }else if(poolProcessor.getStartBlockNo() < height){
                        tmpList.add(poolProcessor);
                        deleteList.add(poolProcessor);
                    }else {
                        poolList.add(poolProcessor);
                    }
                }
                poolsInDeletion.removeAll(tmpList);
            }
        }

        Connection con = null;
        try {
            con = Db.db.getConnection();

            if(poolList.size() > 0) {
                PoolDb.saveOrUpdate(con, poolList);
            }

            if(deleteList.size() > 0) {
                for(SharderPoolProcessor poolProcessor : deleteList){
                    PoolDb.delete(con, poolProcessor);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * save the pools into db,
     * If be called outside, the caller should be org.conch.Conch#shutdown()
     */
    public static void persistence(){
        List<SharderPoolProcessor> poolList = Lists.newArrayList();
        List<SharderPoolProcessor> deleteList = Lists.newArrayList();
        
        Map<Long,SharderPoolProcessor> checkPool = Maps.newHashMap();
        
        if(sharderPools.size() > 0){
            sharderPools.values().forEach(poolProcessor -> {
                if(checkPool.containsKey(poolProcessor.getCreatorId())) {
                    SharderPoolProcessor dpPool = checkPool.get(poolProcessor.getCreatorId());
                    if(dpPool.startBlockNo < poolProcessor.startBlockNo) {
                        poolList.add(dpPool);
                        checkPool.put(poolProcessor.getCreatorId(), poolProcessor);

                        deleteList.add(poolProcessor);
                    }else{
                        deleteList.add(dpPool);
                    }
                }else{
                    poolList.add(poolProcessor);
                    checkPool.put(poolProcessor.getCreatorId(), poolProcessor);
                }
            }); 
        }
       
        if(destroyedPools.size() > 0){
            Set<Long> accountIds = destroyedPools.keySet();
            for(Long accountId : accountIds){
                poolList.addAll(destroyedPools.get(accountId));
            }
        }
      
        if(poolList.size() > 0) {
            PoolDb.saveOrUpdate(null,poolList);  
        }
        
        if(deleteList.size() > 0) {
            deleteList.forEach(PoolProcessor -> PoolDb.delete(PoolProcessor));
        }
    }
    
    public static void instFromDB(){
        sharderPools.clear();
        destroyedPools.clear();
        
        List<SharderPoolProcessor> poolProcessors = PoolDb.list(State.DESTROYED.ordinal(), false);
        poolProcessors.forEach(pool -> {
            sharderPools.put(pool.poolId, pool);
        });

        List<SharderPoolProcessor> destroyedPoolProcessors = PoolDb.list(State.DESTROYED.ordinal(), true);
        destroyedPoolProcessors.forEach(pool -> {
            if(!destroyedPools.containsKey(pool.creatorId)) {
                destroyedPools.put(pool.creatorId, Lists.newArrayList());
            }
            destroyedPools.get(pool.creatorId).add(pool);
        });
    }
    
    private static void instProcessingMapFromTxs(){
        DbIterator<? extends Transaction> unconfirmedTxs = null;
        try {
            unconfirmedTxs = Conch.getTransactionProcessor().getAllUnconfirmedTransactions();
            unconfirmedTxs.forEach(tx -> {
                if (tx.getType().isType(TransactionType.TYPE_SHARDER_POOL)) {
                    if (tx.getType().isSubType(TransactionType.SUBTYPE_SHARDER_POOL_CREATE)) {
                       addProcessingCreateTx(tx.getSenderId(),tx.getId());
                    } else if (tx.getType().isSubType(TransactionType.SUBTYPE_SHARDER_POOL_DESTROY)) {
                        Attachment.SharderPoolDestroy destroy = (Attachment.SharderPoolDestroy) tx.getAttachment();
                        addProcessingDestroyTx(destroy.getPoolId(), tx.getId());
                    } else if (tx.getType().isSubType(TransactionType.SUBTYPE_SHARDER_POOL_QUIT)) {
                        Attachment.SharderPoolQuit quit = (Attachment.SharderPoolQuit) tx.getAttachment();
                        addProcessingQuitTx(quit.getTxId(),  tx.getId());
                    }
                }
            });
        } finally {
            DbUtils.close(unconfirmedTxs);
        }
    }

    public static void init() {
        PoolRule.init();
        instFromDB();
        instProcessingMapFromTxs();
        
        // AFTER_BLOCK_APPLY event listener
        Conch.getBlockchainProcessor().addListener(block -> acceptNewBlock(block),
                BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    public static SharderPoolProcessor newPoolFromDestroyed(long creator) {
        if (!destroyedPools.containsKey(creator) || destroyedPools.get(creator).size() == 0) {
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

    /**
     * return all working pool's creator 
     * @return
     */
    public static Set<Long> getAllCreators(){
        Set<Long> creators = Sets.newHashSet();
        sharderPools.values().forEach(pool -> {
            if(hasProcessingDestroyTx(pool.getPoolId()) == -1){
                creators.add(pool.getCreatorId());
            }
        });
        return creators;
    }

    /**
     * get all working pools
     * @return
     */
    public static JSONObject getPoolsFromNow(){
        JSONArray array = new JSONArray();
        sharderPools.values().forEach(pool -> {
            if(hasProcessingDestroyTx(pool.getPoolId()) == -1){
                array.add(pool.toJsonObject()); 
            }
        });
        JSONObject json = new JSONObject();
        json.put("pools",array);
        return json;
    }

    public static SharderPoolProcessor getPoolFromAll(long creatorId, long poolId) {
        SharderPoolProcessor forgePool = getPool(poolId);
        if (forgePool != null) {
            return forgePool;
        }
        
        if(destroyedPools != null && destroyedPools.size() > 0) {
            List<SharderPoolProcessor> dPools = destroyedPools.get(creatorId);
            if(dPools != null && dPools.size() > 0) {
                for (SharderPoolProcessor destroy : dPools) {
                    if (destroy != null && destroy.poolId == poolId) {
                        return destroy;
                    }
                }
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
            jsonObject.put("errorDescription", "current id doesn't create any mining pool");
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

    /**
     * validate the consignor amount in the tx with the amount from rule definition
     * @param map 
     * @return
     */
    public boolean validateConsignorsAmount(Map<Long, Long> map) {
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

    public int getRemainBlocks() {
        if(startBlockNo < 0 || endBlockNo < 0) return 0;
        int remain = endBlockNo - startBlockNo;
        return remain < 0 ? 0 : remain;
    }


    public Map<String, Object> getRule() {
        return rule;
    }
    
    public Map<String, Object> getRootRuleMap(){
        Object rootRuleMap = getRule().get("level0");
        rootRuleMap = rootRuleMap != null ? rootRuleMap : getRule().get("level1");
        
        if(rootRuleMap instanceof HashMap){
            return (HashMap<String, Object>) rootRuleMap;
        }else if(rootRuleMap instanceof JSONObject){
            return PoolRule.jsonObjectToMap((JSONObject)rootRuleMap);
        }else if(rootRuleMap instanceof com.alibaba.fastjson.JSONObject){
            return PoolRule.jsonObjectToMap((com.alibaba.fastjson.JSONObject)rootRuleMap);
        }
        return null;
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

    public long getPoolId() {
        return poolId;
    }

    public void setChance(float chance) {
        this.chance = chance;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setEndBlockNo(int endBlockNo) {
        this.endBlockNo = endBlockNo;
    }

    public void setHistoricalBlocks(int historicalBlocks) {
        this.historicalBlocks = historicalBlocks;
    }

    public void setTotalBlocks(int totalBlocks) {
        this.totalBlocks = totalBlocks;
    }

    public void setHistoricalIncome(long historicalIncome) {
        this.historicalIncome = historicalIncome;
    }

    public void setHistoricalMintRewards(long historicalMintRewards) {
        this.historicalMintRewards = historicalMintRewards;
    }

    public void setMintRewards(long mintRewards) {
        this.mintRewards = mintRewards;
    }

    public void setHistoricalFees(long historicalFees) {
        this.historicalFees = historicalFees;
    }

    public void setPower(long power) {
        this.power = power;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setUpdateHeight(int updateHeight) {
        this.updateHeight = updateHeight;
    }

    public void setRule(Map<String, Object> rule) {
        this.rule = rule;
    }

    public float getChance() { return chance; }

    public int getHistoricalBlocks() { return historicalBlocks; }

    public int getTotalBlocks() { return totalBlocks; }

    public long getHistoricalIncome() { return historicalIncome; }

    public long getHistoricalMintRewards() { return historicalMintRewards; }

    public long getMintRewards() { return mintRewards; }

    public long getHistoricalFees() { return historicalFees; }

    public int getNumber() { return number; }

    public int getUpdateHeight() { return updateHeight; }

    public void setCreatorId(long creatorId) { this.creatorId = creatorId; }

    public void setPoolId(long poolId) { this.poolId = poolId; }

    public void setLevel(int level) { this.level = level; }

    public void setStartBlockNo(int startBlockNo) { this.startBlockNo = startBlockNo; }

    public void setConsignors(ConcurrentMap<Long, Consignor> consignors) { this.consignors = consignors; }

    public long getJoiningAmount() {
        return joiningAmount;
    }

    public void setJoiningAmount(long joiningAmount) {
        this.joiningAmount = joiningAmount;
    }
    
    public void addJoiningAmount(long amount) {
        joiningAmount += amount;
    }

    public void clearJoiningAmount() {
        joiningAmount = 0;
    }
    
    public void subJoiningAmount(long amount) {
        if(joiningAmount <= 0) return;
        
        if(joiningAmount > amount) {
            joiningAmount -= amount;
        }else {
            joiningAmount = 0;
        }
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
        jsonObject.put("creatorRS", Account.rsAccount(creatorId));
        jsonObject.put("level", level);
        jsonObject.put("number", number);
        jsonObject.put("power", power);
        jsonObject.put("joiningAmount", joiningAmount);
        jsonObject.put("chance", chance);
        jsonObject.put("historicalBlocks", historicalBlocks);
        jsonObject.put("historicalIncome", historicalIncome);
        jsonObject.put("historicalFees", historicalFees);
        jsonObject.put("historicalMintRewards", historicalMintRewards);
        jsonObject.put("mintRewards", mintRewards);
        jsonObject.put("totalBlocks", totalBlocks);
        jsonObject.put("consignors", consignors);
        jsonObject.put("startBlockNo", startBlockNo);
        jsonObject.put("endBlockNo", endBlockNo);
        jsonObject.put("updateHeight", updateHeight);
        jsonObject.put("rule", rule);
        jsonObject.put("state", state);
        return jsonObject;
    }

    public String toJsonStr() {
        com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject(toJsonObject());

        jsonObject.remove("poolId");
        jsonObject.remove("creatorID");
//        jsonObject.remove("creatorRS");

        jsonObject.put("poolId", poolId);
        jsonObject.put("creatorId", creatorId);
        jsonObject.put("state", state.ordinal());

//        if(consignors != null && consignors.size() > 0) {
//            JSONObject consignorJson = new JSONObject();
//            Set<Long> ids = consignors.keySet();
//            for(Long id : ids){
//                consignorJson.put(id,consignors.get(id).toJsonStr());
//            }
//            jsonObject.put("consignors", consignorJson);
//        }
        return jsonObject.toJSONString();
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
