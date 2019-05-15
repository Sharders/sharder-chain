package org.conch.chain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.crypto.Crypto;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.tx.TransactionImpl;
import org.conch.util.Convert;
import org.conch.util.Listener;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;

import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/11
 */
public class CheckSumValidator {

    private static final byte[] CHECKSUM_POC_BLOCK =
            Constants.isTestnet()
                    ? new byte[] {
                    110, -1, -56, -56, -58, 48, 43, 12, -41, -37, 90, -93, 80, 20, 3, -76, -84, -15, -113,
                    -34, 30, 32, 57, 85, -30, 16, -10, 127, -101, 17, 121, 124
            }
                    : new byte[] {
                    -90, -42, -57, -76, 88, -49, 127, 6, -47, -72, -39, -56, 51, 90, -90, -105,
                    121, 71, -94, -97, 49, -24, -12, 86, 7, -48, 90, -91, -24, -105, -17, -104
            };
    
    // not opened yet
    private static final byte[] CHECKSUM_PHASING_BLOCK =
            Constants.isTestnet()
                    ? new byte[] {
                    -1
            }
                    : new byte[] {
                   -1
            };

    private static final CheckSumValidator inst = new CheckSumValidator();

    public static CheckSumValidator getInst(){
        return inst;
    }

    /**
     * validate checksum after Event.BLOCK_SCANNED or Event.BLOCK_PUSHED
     * @return
     */
    public static Listener<Block> eventProcessor(){
        return inst.checksumListener;
    }
    
    // pop off to previous right height when checksum validation failed
    private final Listener<Block> checksumListener = block -> {
                if (block.getHeight() == Constants.POC_BLOCK_HEIGHT) {
                    if (!verifyChecksum(CHECKSUM_POC_BLOCK, 0, Constants.POC_BLOCK_HEIGHT)) {
                        Conch.getBlockchainProcessor().popOffTo(0);
                    }
                } else if (block.getHeight() == Constants.PHASING_BLOCK_HEIGHT) {
                    if (!verifyChecksum(CHECKSUM_PHASING_BLOCK, Constants.POC_BLOCK_HEIGHT, Constants.PHASING_BLOCK_HEIGHT)) {
                        Conch.getBlockchainProcessor().popOffTo(Constants.POC_BLOCK_HEIGHT);
                    }
                }
            };


    private boolean verifyChecksum(byte[] validChecksum, int fromHeight, int toHeight) {
        MessageDigest digest = Crypto.sha256();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE height > ? AND height <= ? ORDER BY id ASC, timestamp ASC");
            pstmt.setInt(1, fromHeight);
            pstmt.setInt(2, toHeight);
            DbIterator<TransactionImpl> iterator = null;
            try {
                iterator = BlockchainImpl.getInstance().getTransactions(con, pstmt);
                while (iterator.hasNext()) {
                    digest.update(iterator.next().getBytes());
                }
            }finally {
                DbUtils.close(iterator);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
        
        byte[] checksum = digest.digest();
        if (validChecksum == null) {
            Logger.logMessage("Checksum calculated:\n" + Arrays.toString(checksum));
            return true;
        } else if (!Arrays.equals(checksum, validChecksum)) {
            Logger.logErrorMessage("Checksum failed at block " + Conch.getBlockchain().getHeight() + ": " + Arrays.toString(checksum));
            return false;
        } else {
            Logger.logMessage("Checksum passed at block " + Conch.getBlockchain().getHeight());
            return true;
        }
    }
    
    static int badCount = 0;
    static boolean synIgnoreBlock = false;
    /**  **/
    // known ignore blocks
    private static final Set<Long> knownIgnoreBlocks = Sets.newHashSet(
            //Testnet
            -8556361949057624360L,
            211456030592803100L,
            -9051459710885545966L
    );
    
    private static final Set<Long> knownIgnoreTxs = Sets.newHashSet();
    private static final Map<Integer,Set<Long>> knownDirtyPoolTxs = Maps.newConcurrentMap();
    private static final Map<Integer,Set<Long>> knownDirtyPocTxs = Maps.newConcurrentMap();

    public static final int CHECK_INTERVAL = Conch.getIntProperty("sharder.knownBlockCheckInterval", 1000);
    //TODO 
    private static final Map<Long,JSONObject> ignoreBlockMap = Maps.newConcurrentMap();
    
    static {
        if(!synIgnoreBlock) {
            updateKnownIgnoreBlocks();
            synIgnoreBlock = true;
        }
    }
    
    private static void countBad(boolean isBad){
        if(!isBad) {
            if(badCount++ > CHECK_INTERVAL) {
                new Thread(() -> updateKnownIgnoreBlocks()).start();
                badCount = 0;   
            }
        }
    }

    public static boolean isKnownIgnoreBlock(long blockId){
        if(!synIgnoreBlock) {
            new Thread(() -> updateKnownIgnoreBlocks()).start();
        }
        boolean result = knownIgnoreBlocks.contains(blockId);
        countBad(result);
        
        return result;
    }
    
    public static boolean isKnownIgnoreTx(long txId){
        boolean result = knownIgnoreTxs.contains(txId);
        countBad(result);
        
        return result;
    }

    public static boolean isDirtyPoolTx(int height, long accountId){
        if(!knownDirtyPoolTxs.containsKey(height)) {
            countBad(false);
            return false;
        }

        boolean result = knownDirtyPoolTxs.get(height).contains(accountId);
        countBad(result);
        
        if(result){
            Logger.logDebugMessage("found a known dirty pool tx[account id=%s] at height %d, ignore this tx" , accountId, height);
        }
        return result;
    }
    
    private static boolean updateSingle(JSONObject object){
        try{
            if(object.containsKey("id") && object.getLong("id") != -1L) {
                long blockId = object.getLong("id");
                if (!knownIgnoreBlocks.contains(blockId)) {
                    knownIgnoreBlocks.add(blockId);
                    ignoreBlockMap.put(blockId, object);
                }
            }

            if(object.containsKey("txs")){
                com.alibaba.fastjson.JSONArray array = object.getJSONArray("txs");
                for(int i = 0; i < array.size(); i++) {
                    Long txid = array.getLong(i);
                    if(!knownIgnoreTxs.contains(txid)) {
                        knownIgnoreTxs.add(txid);
                    }
                }
            }

            if(object.containsKey("dirtyPoolAccounts")){
                Integer height = object.getInteger("height");
                if(!knownDirtyPoolTxs.containsKey(height)) {
                    knownDirtyPoolTxs.put(height, Sets.newHashSet());
                }
                Set<Long> dirtyPoolAccounts = knownDirtyPoolTxs.get(height);

                com.alibaba.fastjson.JSONArray array = object.getJSONArray("dirtyPoolAccounts");
                for(int i = 0; i < array.size(); i++) {
                    dirtyPoolAccounts.add(array.getLong(i));
                }
            }
            
            if(object.containsKey("dirtyPocTxs")){
                Integer height = object.getInteger("height");
                if(!knownDirtyPocTxs.containsKey(height)) {
                    knownDirtyPocTxs.put(height, Sets.newHashSet());
                }
                Set<Long> dirtyPocTxs = knownDirtyPocTxs.get(height);

                com.alibaba.fastjson.JSONArray array = object.getJSONArray("dirtyPocTxs");
                for(int i = 0; i < array.size(); i++) {
                    dirtyPocTxs.add(array.getLong(i));
                }
            }
            
        }catch(Exception e){
            Logger.logErrorMessage("parsed and set single ignore block error caused by " + e.getMessage());
            return false;
        }
        return true;
    }


    public static void updateKnownIgnoreBlocks(){
        RestfulHttpClient.HttpResponse response = null;
        String url = UrlManager.KNOWN_IGNORE_BLOCKS;
        try {
            response = RestfulHttpClient.getClient(url).get().request();
            if(response == null) return;
            
            String content = response.getContent();
            String updateDetail = "\n\r";
            String totalIgnoreBlocks = "\n\r";
            if(content.startsWith("[")) {
                com.alibaba.fastjson.JSONArray array = JSON.parseArray(content);
                for(int i = 0; i < array.size(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    totalIgnoreBlocks += object.toString() + "\n\r";
                    if(updateSingle(object)){
                        updateDetail += object.toString() + "\n\r";
                    }
                }
            }else if(content.startsWith("{")){
                com.alibaba.fastjson.JSONObject object = JSON.parseObject(content);
                totalIgnoreBlocks += object.toString() + "\n\r";
                if(updateSingle(object)){
                    updateDetail += object.toString() + "\n\r";
                }
            }else{
                Logger.logWarningMessage("not correct known ignore block get from " + url + " : " + content);
                return ;
            }
            if(totalIgnoreBlocks.length() > 4){
                Logger.logDebugMessage("total ignore blocks get from %s as follow:" + totalIgnoreBlocks, url);
            }
            
            if(updateDetail.length() > 4){
                Logger.logDebugMessage("last known ignore blocks updated:" + updateDetail);
            }
            
            // remove the dirty poc txs
            if(knownDirtyPocTxs.size() > 0) {
                Set<Long> dirtyPocTxs = Sets.newHashSet();
                knownDirtyPocTxs.values().forEach(ids -> dirtyPocTxs.addAll(ids));
                new Thread(() ->  Conch.getPocProcessor().removeDelayedPocTxs(dirtyPocTxs)).start();
            }

            // remove the dirty pools
            if(knownDirtyPoolTxs.size() > 0) {
                new Thread(() -> SharderPoolProcessor.removePools(knownDirtyPoolTxs)).start();
            }
            
            if(!synIgnoreBlock) synIgnoreBlock = true;
        } catch (IOException e) {
           Logger.logErrorMessage("Can't get known ignore blocks from " + url + " caused by " + e.getMessage());
        }
    }
    
    public static JSONObject generateIgnoreBlock(long id, byte[] checksum, String network){
        if(StringUtils.isEmpty(network)) network = "testnet";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id",id);
        jsonObject.put("checksum",Convert.toString(checksum, false));
        jsonObject.put("network",network);
        return jsonObject;
    }

}
