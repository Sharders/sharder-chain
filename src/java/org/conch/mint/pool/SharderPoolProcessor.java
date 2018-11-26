package org.conch.mint.pool;

import org.conch.*;
import org.conch.util.Logger;
import org.json.simple.JSONObject;

import java.io.*;
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
            SharderPoolProcessor forgePool =
                    new SharderPoolProcessor(creatorId, id, startBlockNo, endBlockNo);
            SharderPoolProcessor past = newSharderPoolFromDestroyed(creatorId);
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
                        .frozenBalanceAndUnconfirmedBalanceNQT(
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

    private static void updateSharderPool(long poolId, long incom) {
        SharderPoolProcessor forgePool = sharderPools.get(poolId);
        forgePool.historicalBlocks++;
        forgePool.historicalIncome += incom;
    }

    private static final String LOCAL_STORAGE_FORDER = Db.getDir() + File.separator + "local";
    private static final String LOCAL_STOAGE_SHARDER_POOLS = "SharderPools";
    private static final String LOCAL_STOAGE_DESTROYED_POOLS = "DestroyedPools";

    static {
        File file = new File(getLocalStoragePath(LOCAL_STOAGE_SHARDER_POOLS));
        if (file.exists()) {
            sharderPools =
                    (ConcurrentMap<Long, SharderPoolProcessor>) getObjFromFile(LOCAL_STOAGE_SHARDER_POOLS);
        } else {
            // TODO delete by user ,pop off get block from network
            sharderPools = new ConcurrentHashMap<>();
        }

        file = new File(getLocalStoragePath(LOCAL_STOAGE_DESTROYED_POOLS));
        if (file.exists()) {
            destroyedPools =
                    (ConcurrentMap<Long, List<SharderPoolProcessor>>)
                            getObjFromFile(LOCAL_STOAGE_DESTROYED_POOLS);
        } else {
            // TODO delete by user
            destroyedPools = new ConcurrentHashMap<>();
        }

        Conch.getBlockchainProcessor()
                .addListener(
                        block -> {
                            int height = block.getHeight();
                            for (SharderPoolProcessor forgePool : sharderPools.values()) {
                                forgePool.updateHeight = height;
                                if (forgePool.consignors.size() == 0
                                        && height - forgePool.startBlockNo > Constants.SHARDER_POOL_DEADLINE) {
                                    forgePool.destroySharderPool(height);
                                    continue;
                                }
                                if (forgePool.endBlockNo == height) {
                                    forgePool.destroySharderPool(height);
                                    continue;
                                }
                                if (forgePool.startBlockNo == height) {
                                    forgePool.state = State.WORKING;
                                    // TODO add to generator list
                                    continue;
                                }
                                // TODO auto destroy pool because the number or amount of pool is too small
                                if (forgePool.startBlockNo + Constants.SHARDER_POOL_DEADLINE == height
                                        && forgePool.consignors.size() == 0) {
                                    forgePool.destroySharderPool(height);
                                    continue;
                                }
                                // transaction is time out
                                for (Consignor consignor : forgePool.consignors.values()) {
                                    long amount = consignor.validateHeight(height);
                                    if (amount != 0) {
                                        forgePool.power -= amount;
                                        Account.getAccount(consignor.getId())
                                                .frozenBalanceAndUnconfirmedBalanceNQT(
                                                        AccountLedger.LedgerEvent.FORGE_POOL_QUIT, 0, -amount);
                                    }
                                }

                                if (forgePool.startBlockNo < height) {
                                    forgePool.totalBlocks++;
                                }
                                if (forgePool.totalBlocks == 0) {
                                    forgePool.chance = 0;
                                } else {
                                    forgePool.chance = forgePool.historicalBlocks / forgePool.totalBlocks;
                                }
                            }

                            long id = ownOnePool(block.getGeneratorId());
                            if (id != -1
                                    && SharderPoolProcessor.getSharderPool(id)
                                    .getState()
                                    .equals(SharderPoolProcessor.State.WORKING)) {
                                updateSharderPool(id, block.getTotalFeeNQT());
                            }

                            if (height > Constants.SHARDER_REWARD_DELAY) {
                                Block past =
                                        Conch.getBlockchain().getBlockAtHeight(height - Constants.SHARDER_REWARD_DELAY);
                                for (Transaction transaction : past.getTransactions()) {
                                    if (transaction.getType() == TransactionType.CoinBase.ORDINARY) {
                                        TransactionType.CoinBase.unFreezeForgeBalance(transaction);
                                    }
                                }
                            }

                            saveObjToFile(sharderPools, LOCAL_STOAGE_SHARDER_POOLS);
                            saveObjToFile(destroyedPools, LOCAL_STOAGE_DESTROYED_POOLS);
                        },
                        BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    public static void init() {
        PoolRule.init();
    }

    private static String getLocalStoragePath(String fileName) {
        return LOCAL_STORAGE_FORDER + File.separator + fileName;
    }

    private static void saveObjToFile(Object o, String fileName) {
        try {
            File localStorageFolder = new File(LOCAL_STORAGE_FORDER);
            if (!localStorageFolder.exists()) localStorageFolder.mkdir();

            ObjectOutputStream oos =
                    new ObjectOutputStream(new FileOutputStream(getLocalStoragePath(fileName)));
            oos.writeObject(o);
            oos.close();
        } catch (Exception e) {
            Logger.logErrorMessage("save sharder pool to file failed, file " + fileName + e.toString());
        }
    }

    private static Object getObjFromFile(String fileName) {
        try {
            ObjectInputStream ois =
                    new ObjectInputStream(new FileInputStream(getLocalStoragePath(fileName)));
            Object object = ois.readObject();
            return object;
        } catch (Exception e) {
            Logger.logErrorMessage("failed to read sharder pool from file " + fileName + e.toString());
            return null;
        }
    }

    public static SharderPoolProcessor newSharderPoolFromDestroyed(long creator) {
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

    public static SharderPoolProcessor getSharderPool(long poolId) {
        return sharderPools.containsKey(poolId) ? sharderPools.get(poolId) : null;
    }

    public static SharderPoolProcessor getSharderPoolFromAll(long creatorId, long poolId) {
        SharderPoolProcessor forgePool = getSharderPool(poolId);
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

    public static JSONObject getSharderPoolsFromNowAndDestroy(long creatorId) {
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
            jsonObject.put(forgePool.poolId, forgePool.toJSonObject());
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

    public JSONObject toJSonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("poolId", poolId);
        jsonObject.put("creatorID", creatorId);
        jsonObject.put("level", level);
        jsonObject.put("number", number);
        jsonObject.put("power", power);
        jsonObject.put("chance", chance);
        jsonObject.put("historicalBlocks", historicalBlocks);
        jsonObject.put("historicalIncome", historicalIncome);
        jsonObject.put("startBlockNo", startBlockNo);
        jsonObject.put("endBlockNo", endBlockNo);
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
