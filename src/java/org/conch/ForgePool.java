package org.conch;
import org.conch.util.Logger;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ForgePool implements Serializable {
    private static final long serialVersionUID = 8653213465471743671L;
    private static final ConcurrentMap<Long,ForgePool> forgePools;
    private static final ConcurrentMap<Long,List<ForgePool>> destroyForgePool;
    enum State {
        INIT,CREATING,WORKING,DESTROYED
    }
    private final long creatorId;
    private final long poolId;
    private final int level;
    private float chance;
    private State state;
    private final int startBlockNo;
    private int endBlockNo;
    private int historicalBlocks;
    private int totalBlocks; //the sum of all destroyed forge pool's life time
    private long historicalIncome;
    private long power;
    private final ConcurrentMap<Long,Consignor> consignors = new ConcurrentHashMap<>();
    private int number = 0;
    private int updateHeight;
    private Map<String,Object> rule;

    public ForgePool(long creatorId, long id, int startBlockNo,int endBlockNo){
        this.creatorId = creatorId;
        this.poolId = id;
        this.startBlockNo = startBlockNo;
        this.endBlockNo = endBlockNo;
        this.level = Rule.getLevel(creatorId);
    }

    public static void createForgePool(long creatorId ,long id, int startBlockNo, int endBlockNo,Map<String,Object> rule) {
        if (destroyForgePool.containsKey(creatorId)) {
            ForgePool forgePool = new ForgePool(creatorId,id,startBlockNo,endBlockNo);
            ForgePool past = getNewForgePoolFromDestroy(creatorId);
            forgePool.chance = past.chance;
            forgePool.state = State.INIT;
            forgePool.historicalBlocks = past.historicalBlocks;
            forgePool.historicalIncome = past.historicalIncome;
            forgePool.totalBlocks = past.totalBlocks;
            forgePool.rule = rule;
            forgePools.put(forgePool.poolId,forgePool);
            Logger.logDebugMessage("create forge pool from old pool, chance " + past);
        } else {
            ForgePool forgePool = new ForgePool(creatorId,id,startBlockNo,endBlockNo);
            forgePool. chance = 0;
            forgePool.state = State.INIT;
            forgePool.historicalBlocks = 0;
            forgePool.historicalIncome = 0;
            forgePool.totalBlocks = 0;
            forgePool.rule = rule;
            forgePools.put(forgePool.poolId,forgePool);
            Logger.logDebugMessage(creatorId + " create new forge pool");
        }
    }

    public void destroyForgePool(int height){
        state = State.DESTROYED;
        endBlockNo = height;

        for(Consignor consignor : consignors.values()){
            long amount = consignor.getAmount();
            if(amount != 0){
                power -= amount;
                Account.getAccount(consignor.getId()).frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.FORGE_POOL_QUIT,-1,-amount);
            }
        }
        if(startBlockNo > endBlockNo){
            forgePools.remove(poolId);
            Logger.logDebugMessage("destroy forge pool " + poolId + " before start ");
            return;
        }
        if (destroyForgePool.containsKey(creatorId)) {
            destroyForgePool.get(creatorId).add(this);
            forgePools.remove(poolId);
        }else {
            List<ForgePool> destroy = new ArrayList<>();
            destroy.add(this);
            destroyForgePool.put(creatorId,destroy);
            forgePools.remove(poolId);
        }
        Logger.logDebugMessage("destroy forge pool " + poolId);
    }

    public void addOrUpdateConsignor(long id , long txId, int startBlockNo, int endBlockNo, long amount){
        if (consignors.containsKey(id)) {
            Consignor consignor = consignors.get(id);
            consignor.addTransaction(txId,startBlockNo,endBlockNo,amount);
            power += amount;
        }else {
            Consignor consignor = new Consignor(id,txId,startBlockNo,endBlockNo,amount);
            consignors.put(id,consignor);
            power += amount;
            number++;
        }
        Logger.logDebugMessage(id + " join in forge pool " + poolId);
    }

    public long quitConsignor(long id,long txId){
        if (!consignors.containsKey(id)) {
            Logger.logErrorMessage("forge pool:" + poolId + " don't have consignor:" + id);
            return -1;
        }
        Consignor consignor = consignors.get(id);
        long amount = consignor.getTransactionAmount(txId);
        if(amount == -1){
            Logger.logErrorMessage("consignor:" + id + " don't have transaction:" + txId);
            return -1;
        }
        power -= amount;
        if(consignor.removeTransaction(txId)){
            consignors.remove(id);
            number--;
            Logger.logDebugMessage(id + "quit forge pool " + poolId + ",tx id is " + txId);
        }
        return amount;
    }

    public boolean hasSenderAndTransaction(long senderId ,long txId){
        if(consignors.containsKey(senderId) && consignors.get(senderId).hasTransaction(txId)){
            return true;
        }else {
            return false;
        }
    }

    public static long ownOnePool(long creator){
            for (ForgePool forgePool : forgePools.values()) {
            if(forgePool.creatorId == creator){
                return forgePool.poolId;
            }
        }
        return -1;
    }

    private static void updateForePool(long poolId, long incom){
        ForgePool forgePool = forgePools.get(poolId);
        forgePool.historicalBlocks++;
        forgePool.historicalIncome += incom;
    }

    static {
        File file = new File("ForgePools");
        if(file.exists()){
            forgePools = (ConcurrentMap<Long,ForgePool>)getObjFromFile("ForgePools");
        }else {
            //TODO delete by user ,pop off get block from network
            forgePools = new ConcurrentHashMap<>();
        }
        file = new File("DestroyForgePool");
        if(file.exists()){
            destroyForgePool = (ConcurrentMap<Long,List<ForgePool>>)getObjFromFile("DestroyForgePool");
        }else {
            //TODO delete by user
            destroyForgePool = new ConcurrentHashMap<>();
        }

        Conch.getBlockchainProcessor().addListener(block -> {
            int height = block.getHeight();
            for(ForgePool forgePool : forgePools.values()){
                forgePool.updateHeight = height;
                if(forgePool.consignors.size() ==0 && height - forgePool.startBlockNo > Constants.FORGE_POOL_DEADLINE){
                    forgePool.destroyForgePool(height);
                    continue;
                }
                if(forgePool.endBlockNo == height){
                    forgePool.destroyForgePool(height);
                    continue;
                }
                if(forgePool.startBlockNo == height){
                    forgePool.state = State.WORKING;
                    //TODO add to generator list
                    continue;
                }
                //TODO auto destroy pool because the number or amount of pool is too small
                if(forgePool.startBlockNo + Constants.FORGE_POOL_DEADLINE == height && forgePool.consignors.size() == 0){
                    forgePool.destroyForgePool(height);
                    continue;
                }
                //transaction is time out
                for(Consignor consignor : forgePool.consignors.values()){
                    long amount = consignor.validateHeight(height);
                    if(amount != 0){
                        forgePool.power -= amount;
                        Account.getAccount(consignor.getId()).frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.FORGE_POOL_QUIT,0,-amount);
                    }
                }

                if(forgePool.startBlockNo < height){
                    forgePool.totalBlocks++;
                }
                if(forgePool.totalBlocks == 0){
                    forgePool.chance = 0;
                }else {
                    forgePool.chance = forgePool.historicalBlocks / forgePool.totalBlocks;
                }
            }

            long id = ownOnePool(block.getGeneratorId());
            if(id != -1 && ForgePool.getForgePool(id).getState().equals(ForgePool.State.WORKING)){
                updateForePool(id,block.getTotalFeeNQT());
            }

            if(height > Constants.FORGE_REWARD_DELAY){
                Block past = Conch.getBlockchain().getBlockAtHeight(height - Constants.FORGE_REWARD_DELAY);
                for(Transaction transaction : past.getTransactions()){
                    if(transaction.getType() == TransactionType.CoinBase.ORDINARY){
                        TransactionType.CoinBase.unFreezeForgeBalance(transaction);
                    }
                }
            }

            saveObjToFile(forgePools,"ForgePools");
            saveObjToFile(destroyForgePool,"DestroyForgePool");
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    public static void init(){
        Rule.init();
    }

    private static void saveObjToFile(Object o,String file){
        try{
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(o);
            oos.close();
        }catch (Exception e){
            Logger.logErrorMessage("save forge pool to file failed, file " + file + e.toString());
        }
    }

    private static Object getObjFromFile(String file){
        try{
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            Object object = ois.readObject();
            return object;
        }catch (Exception e){
            Logger.logErrorMessage("failed read forge pool from file " + file + e.toString());
            return null;
        }
    }

    public static ForgePool getNewForgePoolFromDestroy(long creator){
        if(!destroyForgePool.containsKey(creator)){
            return null;
        }
        ForgePool past = destroyForgePool.get(creator).get(0);
        for(ForgePool destroy : destroyForgePool.get(creator)){
            if(destroy.getEndBlockNo() > past.getEndBlockNo()){
                past = destroy;
            }
        }
        return past;
    }

    public static ForgePool getForgePool(long poolId) {
        return forgePools.containsKey(poolId) ? forgePools.get(poolId) : null;
    }

    public static ForgePool getForgePoolFromAll(long creatorId,long poolId) {
        ForgePool forgePool = getForgePool(poolId);
        if(forgePool != null){
            return forgePool;
        }
        for(ForgePool destroy : destroyForgePool.get(creatorId)){
            if(destroy.poolId == poolId){
                return destroy;
            }
        }
        return null;
    }

    public static JSONObject getForgePoolsFromNowAndDestroy(long creatorId) {
        ForgePool now = null;
        for(ForgePool forgePool : forgePools.values()){
            if(forgePool.creatorId == creatorId){
                now = forgePool;
                break;
            }
        }
        List<ForgePool> pasts = new ArrayList<>();
        if(destroyForgePool.containsKey(creatorId)){
            pasts = destroyForgePool.get(creatorId);
        }

        if(now != null){
            pasts.add(now);
        }
        JSONObject jsonObject = new JSONObject();
        if(pasts.size() == 0){
            jsonObject.put("errorCode", 1);
            jsonObject.put("errorDescription", "current id doesn't create any forge pool");
            return jsonObject;
        }
        for(ForgePool forgePool : pasts){
            jsonObject.put(forgePool.poolId,forgePool.toJSonObject());
        }
        return jsonObject;
    }

    public Map<Long,Long>  getConsignorsAmountMap(){
        Map<Long,Long> map = new HashMap<>();
        for(Consignor consignor : consignors.values()){
            map.put(consignor.getId(),consignor.getAmount());
        }
        return map;
    }

    public boolean validateConsignorsAmountMap(Map<Long,Long> map){
        Map<Long,Long> myMap = getConsignorsAmountMap();
        if(myMap.size() != map.size()){
            return false;
        }
        for(Long id: myMap.keySet()){
            if(!map.containsKey(id)){
                return false;
            }
            if(!map.get(id).equals(myMap.get(id))){
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

    public JSONObject toJSonObject(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("poolId",poolId);
        jsonObject.put("creatorID",creatorId);
        jsonObject.put("level",level);
        jsonObject.put("number",number);
        jsonObject.put("power",power);
        jsonObject.put("chance",chance);
        jsonObject.put("historicalBlocks",historicalBlocks);
        jsonObject.put("historicalIncome",historicalIncome);
        jsonObject.put("startBlockNo",startBlockNo);
        jsonObject.put("endBlockNo",endBlockNo);
        jsonObject.put("state",state);
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ForgePool forgePool = (ForgePool) o;

        return poolId == forgePool.poolId;
    }
}
