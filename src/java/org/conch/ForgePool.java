package org.conch;

import org.conch.util.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
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
    private float chance;
    private State state;
    private final int startBlockNo;
    private int endBlockNo;
    private int historicalBlocks;
    private long historicalIncome;
    private long power;
    private final ConcurrentMap<Long,Consignor> consignors = new ConcurrentHashMap<>();
    private int consignorNum = 0;
    private int updateHeight;

    public ForgePool(long creatorId, long id, int startBlockNo,int endBlockNo){
        this.creatorId = creatorId;
        this.poolId = id;
        this.startBlockNo = startBlockNo;
        this.endBlockNo = endBlockNo;
    }

    public static void createForgePool(long creatorId ,long id, int startBlockNo, int endBlockNo) {
        if (destroyForgePool.containsKey(creatorId)) {
            ForgePool forgePool = new ForgePool(creatorId,id,startBlockNo,endBlockNo);
            ForgePool past = destroyForgePool.get(creatorId).get(0);
            for(ForgePool destroy : destroyForgePool.get(creatorId)){
                if(destroy.getEndBlockNo() > past.getEndBlockNo()){
                    past = destroy;
                }
            }
            forgePool.chance = past.chance;
            forgePool.state = State.INIT;
            forgePool.historicalBlocks = past.historicalBlocks;
            forgePool.historicalIncome = past.historicalIncome;
            forgePools.put(forgePool.poolId,forgePool);
            Logger.logDebugMessage("create forge pool from old pool, chance " + past);
        } else {
            ForgePool forgePool = new ForgePool(creatorId,id,startBlockNo,endBlockNo);
            forgePool. chance = 0;
            forgePool.state = State.INIT;
            forgePool.historicalBlocks = 0;
            forgePool.historicalIncome = 0;
            forgePools.put(forgePool.poolId,forgePool);
            Logger.logDebugMessage(creatorId + " create new forge pool");
        }
    }

    public void destroyForgePool(int height){
        state = State.DESTROYED;
        endBlockNo = height;
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
            consignorNum++;
        }
        Logger.logDebugMessage(id + " join in forge pool ,pool id is" + poolId);
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
            consignorNum--;
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
                //TODO number or amount
                if(forgePool.startBlockNo + Constants.FORGE_POOL_DEADLINE == height && forgePool.consignors.size() == 0){
                    forgePool.destroyForgePool(height);
                    continue;
                }
                for(Consignor consignor : forgePool.consignors.values()){
                    long amount = consignor.validateHeight(height);
                    if(amount != 0){
                        forgePool.power -= amount;
                        Account.getAccount(consignor.getId()).frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.FORGE_POOL_QUIT,0,-amount);
                    }
                }
                forgePool.updateHeight = height;
            }

            if(forgePools.containsKey(block.getGeneratorId())){
                // TODO CoinBase
                updateForePool(block.getGeneratorId(),block.getTotalFeeNQT());
            }

            saveObjToFile(forgePools,"ForgePools");
            saveObjToFile(destroyForgePool,"DestroyForgePool");
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    public static void init(){

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

    public static ForgePool getForgePool(long poolId) {
        return forgePools.containsKey(poolId) ? forgePools.get(poolId) : null;
    }

    public int getStartBlockNo() {
        return startBlockNo;
    }

    public int getEndBlockNo() {
        return endBlockNo;
    }

    public State getState() {
        return state;
    }

    public ConcurrentMap<Long, Consignor> getConsignors() {
        return consignors;
    }

    public long getCreatorId() {
        return creatorId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ForgePool forgePool = (ForgePool) o;

        return poolId == forgePool.poolId;
    }
}
