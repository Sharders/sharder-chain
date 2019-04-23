package org.conch.chain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
            try (DbIterator<TransactionImpl> iterator = BlockchainImpl.getInstance().getTransactions(con, pstmt)) {
                while (iterator.hasNext()) {
                    digest.update(iterator.next().getBytes());
                }
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
    
    static boolean synIgnoreBlock = true;
    /**  **/
    // known ignore blocks
    private static final Set<Long> knownIgnoreBlocks = Sets.newHashSet(
            //Testnet
            -8556361949057624360L,
            211456030592803100L
    );
    
    private static final Map<Long,JSONObject> ignoreBlockMap = Maps.newConcurrentMap();
    
    
    static {
        if(!synIgnoreBlock) {
            updateKnownIgnoreBlocks();
            synIgnoreBlock = true;
        }
    }


    public static boolean isKnownIgnoreBlock(long blockId){
        if(!synIgnoreBlock) {
            new Thread(() -> updateKnownIgnoreBlocks()).start();
        }
        return knownIgnoreBlocks.contains(blockId);
    }
    
    private static boolean updateSingle(JSONObject object){
        long blockId = object.getLong("id");
        if(knownIgnoreBlocks.contains(blockId)) return false;
        
        knownIgnoreBlocks.add(blockId);
        return true;
    }

    public static void updateKnownIgnoreBlocks(){
        RestfulHttpClient.HttpResponse response = null;
        String url = UrlManager.KNOWN_IGNORE_BLOCKS;
        try {
            response = RestfulHttpClient.getClient(url).get().request();
            String content = response.getContent();
            String updateDetail = "\n\r";
            if(content.startsWith("[")) {
                com.alibaba.fastjson.JSONArray array = JSON.parseArray(content);
                for(int i = 0; i < array.size(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    if(updateSingle(object)){
                        updateDetail += object.toString() + "\n\r";
                    }
                }
            }else if(content.startsWith("{")){
                com.alibaba.fastjson.JSONObject object = JSON.parseObject(content);
                if(updateSingle(object)){
                    updateDetail += object.toString() + "\n\r";
                }
            }else{
                Logger.logWarningMessage("not correct known ignore block get from " + url + " : " + content);
                return ;
            }
            if(updateDetail.length() > 4){
                Logger.logDebugMessage("last known ignore blocks updated:" + updateDetail);
            }else{
                Logger.logDebugMessage("don't updated last known ignore blocks");
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


    public static void main(String[] args) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("id",-8556361949057624360L);
        jsonObject1.put("checksum",new byte[]{110, -1, -56, -56, -58, 48});
        jsonObject1.put("network","testnet");

        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("id",211456030592803100L);
        jsonObject2.put("checksum",Convert.toString(new byte[]{110, -11, -22, -56, -58, 33}, false));
        jsonObject2.put("network","testnet");
        
        jsonArray.add(jsonObject1);
        jsonArray.add(jsonObject2);

        System.out.println(jsonArray.toString());

        System.out.println(Arrays.toString(Convert.toBytes(jsonObject2.getString("checksum"), false)));
    }
}
