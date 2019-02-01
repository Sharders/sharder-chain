package org.conch.mint.pool;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.chain.Block;
import org.conch.chain.BlockchainProcessor;
import org.conch.common.Constants;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.DiskStorageUtil;
import org.conch.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SharderPoolProcessor implements Serializable {
    private static final long serialVersionUID = 8653213465471743671L;
    private static final ConcurrentMap<Long, SharderPoolProcessor> sharderPools;
    private static final ConcurrentMap<Long, List<SharderPoolProcessor>> destroyedPools;

    public enum State {
        INIT,
        CREATING,
        WORKING,
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
    private int totalBlocks; // the sum of all destroyed forge pool's life time
    private long historicalIncome;
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

    public static void createSharderPool(
            long creatorId, long id, int startBlockNo, int endBlockNo, Map<String, Object> rule) {
        if (destroyedPools.containsKey(creatorId)) {
            SharderPoolProcessor forgePool = new SharderPoolProcessor(creatorId, id, startBlockNo, endBlockNo);
            SharderPoolProcessor past = newPoolFromDestroyed(creatorId);
            forgePool.chance = past.chance;
            forgePool.state = State.INIT;
            forgePool.historicalBlocks = past.historicalBlocks;
            forgePool.historicalIncome = past.historicalIncome;
            forgePool.totalBlocks = past.totalBlocks;
            forgePool.rule = rule;
            sharderPools.put(forgePool.poolId, forgePool);
            Logger.logDebugMessage("create forge pool from old pool, chance " + past);
        } else {
            SharderPoolProcessor forgePool =
                    new SharderPoolProcessor(creatorId, id, startBlockNo, endBlockNo);
            forgePool.chance = 0;
            forgePool.state = State.INIT;
            forgePool.historicalBlocks = 0;
            forgePool.historicalIncome = 0;
            forgePool.totalBlocks = 0;
            forgePool.rule = rule;
            sharderPools.put(forgePool.poolId, forgePool);
            Logger.logDebugMessage(creatorId + " create new forge pool");
        }
    }

    public void destroySharderPool(int height) {
        state = State.DESTROYED;
        endBlockNo = height;

        for (Consignor consignor : consignors.values()) {
            long amount = consignor.getAmount();
            if (amount != 0) {
                power -= amount;
                Account.getAccount(consignor.getId())
                        .frozenAndUnconfirmedBalanceNQT(
                                AccountLedger.LedgerEvent.FORGE_POOL_QUIT, -1, -amount);
            }
        }
        if (startBlockNo > endBlockNo) {
            sharderPools.remove(poolId);
            Logger.logDebugMessage("destroy forge pool " + poolId + " before start ");
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
        Logger.logDebugMessage("destroy forge pool " + poolId);
    }

    public void addOrUpdateConsignor(
            long id, long txId, int startBlockNo, int endBlockNo, long amount) {
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
        Logger.logDebugMessage(id + " join in forge pool " + poolId);
    }

    public long quitConsignor(long id, long txId) {
        if (!consignors.containsKey(id)) {
            Logger.logErrorMessage("forge pool:" + poolId + " don't have consignor:" + id);
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
            Logger.logDebugMessage(id + "quit forge pool " + poolId + ",tx id is " + txId);
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

    public static long ownOnePool(long creator) {
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

    private static void updatePool(long poolId, long incom) {
        SharderPoolProcessor forgePool = sharderPools.get(poolId);
        forgePool.historicalBlocks++;
        forgePool.historicalIncome += incom;
    }

    private static final String LOCAL_STORAGE_SHARDER_POOLS = "StoredSharderPools";
    private static final String LOCAL_STORAGE_DESTROYED_POOLS = "StoredDestroyedPools";

    static {
        File file = new File(DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_SHARDER_POOLS));
        if (file.exists()) {
            sharderPools =
                    (ConcurrentMap<Long, SharderPoolProcessor>) DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_SHARDER_POOLS);
        } else {
            // TODO delete by user ,pop off get block from network
            sharderPools = new ConcurrentHashMap<>();
        }

        file = new File(DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_DESTROYED_POOLS));
        if (file.exists()) {
            destroyedPools =
                    (ConcurrentMap<Long, List<SharderPoolProcessor>>)
                            DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_DESTROYED_POOLS);
        } else {
            // TODO delete by user
            destroyedPools = new ConcurrentHashMap<>();
        }

        /**
         * After block accepted:
         * - Update the pool state
         * - Update the remaining block numbers of pool
         * - Destroy pool when lifecycle finished
         * - Coinbase reward unfreeze
         * - Persistence the pool to local disk
         */
        Conch.getBlockchainProcessor().addListener(
                        block -> {
                            int height = block.getHeight();
                            for (SharderPoolProcessor sharderPool : sharderPools.values()) {
                                sharderPool.updateHeight = height;
                                if (sharderPool.consignors.size() == 0 && height - sharderPool.startBlockNo > Constants.SHARDER_POOL_DEADLINE) {
                                    sharderPool.destroySharderPool(height);
                                    continue;
                                }
                                if (sharderPool.endBlockNo == height) {
                                    sharderPool.destroySharderPool(height);
                                    continue;
                                }
                                if (sharderPool.startBlockNo == height) {
                                    sharderPool.state = State.WORKING;
                                    // TODO add to generator list
                                    continue;
                                }
                                // TODO auto destroy pool because the number or amount of pool is too small
                                if (sharderPool.startBlockNo + Constants.SHARDER_POOL_DEADLINE == height && sharderPool.consignors.size() == 0) {
                                    sharderPool.destroySharderPool(height);
                                    continue;
                                }
                                // transaction is time out
                                for (Consignor consignor : sharderPool.consignors.values()) {
                                    long amount = consignor.validateHeight(height);
                                    if (amount != 0) {
                                        sharderPool.power -= amount;
                                        Account.getAccount(consignor.getId())
                                                .frozenAndUnconfirmedBalanceNQT(
                                                        AccountLedger.LedgerEvent.FORGE_POOL_QUIT, 0, -amount);
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
                            long id = ownOnePool(block.getGeneratorId());
                            if (id != -1
                                    && SharderPoolProcessor.getPool(id)
                                    .getState()
                                    .equals(SharderPoolProcessor.State.WORKING)) {
                                updatePool(id, block.getTotalFeeNQT());
                            }
                            
                            //unfreeze the reward
                            if (height > Constants.SHARDER_REWARD_DELAY) {
                                Block past = Conch.getBlockchain().getBlockAtHeight(height - Constants.SHARDER_REWARD_DELAY);
                                for (Transaction transaction : past.getTransactions()) {
                                    Attachment attachment = transaction.getAttachment();
                                    if(attachment instanceof Attachment.CoinBase){
                                        Attachment.CoinBase coinbaseBody = (Attachment.CoinBase) attachment;
                                        if(Attachment.CoinBase.CoinBaseType.BLOCK_REWARD == coinbaseBody.getCoinBaseType()){
                                            TransactionType.CoinBase.unFreezeMintBalance(transaction);
                                        }
                                    }
                                }
                            }

                            DiskStorageUtil.saveObjToFile(sharderPools, LOCAL_STORAGE_SHARDER_POOLS);
                            DiskStorageUtil.saveObjToFile(destroyedPools, LOCAL_STORAGE_DESTROYED_POOLS);
                        },
                        BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
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
        List<SharderPoolProcessor> pasts = new ArrayList<>();
        if (destroyedPools.containsKey(creatorId)) {
            pasts = destroyedPools.get(creatorId);
        }

        if (now != null) {
            pasts.add(now);
        }
        JSONObject jsonObject = new JSONObject();
        if (pasts.size() == 0) {
            jsonObject.put("errorCode", 1);
            jsonObject.put("errorDescription", "current id doesn't create any forge pool");
            return jsonObject;
        }
        for (SharderPoolProcessor forgePool : pasts) {
            jsonObject.put(forgePool.poolId, forgePool.toJsonObject());
        }
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
}
