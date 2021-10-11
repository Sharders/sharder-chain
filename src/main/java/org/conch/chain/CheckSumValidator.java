package org.conch.chain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.crypto.Crypto;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;
import org.conch.security.Guard;
import org.conch.tx.TransactionImpl;
import org.conch.util.Convert;
import org.conch.util.Listener;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.conch.util.ThreadPool;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/11
 */
public class CheckSumValidator {

    private static final byte[] CHECKSUM_POC_BLOCK =
            Constants.isTestnet()
                    ? new byte[]{
                    110, -1, -56, -56, -58, 48, 43, 12, -41, -37, 90, -93, 80, 20, 3, -76, -84, -15, -113,
                    -34, 30, 32, 57, 85, -30, 16, -10, 127, -101, 17, 121, 124
            }
                    : new byte[]{
                    -90, -42, -57, -76, 88, -49, 127, 6, -47, -72, -39, -56, 51, 90, -90, -105,
                    121, 71, -94, -97, 49, -24, -12, 86, 7, -48, 90, -91, -24, -105, -17, -104
            };

    // not opened yet
    private static final byte[] CHECKSUM_PHASING_BLOCK =
            Constants.isTestnet()
                    ? new byte[]{
                    -1
            }
                    : new byte[]{
                    -1
            };

    private static final CheckSumValidator inst = new CheckSumValidator();

    public static CheckSumValidator getInst() {
        return inst;
    }

    /**
     * validate checksum after Event.BLOCK_SCANNED or Event.BLOCK_PUSHED
     *
     * @return
     */
    public static Listener<Block> eventProcessor() {
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
            } finally {
                DbUtils.close(iterator);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
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


    /**
     * DEBUG
     **/
    static Set<Long> debugAccounts = Sets.newHashSet(
//            3960463107034192150L,
//            2792673654720227339L
//            963382008953913442L
    );

    public static boolean isDebugPoint(long accountId) {
        return debugAccounts.contains(accountId);
    }

    /**
     * DEBUG
     **/

    static int badCount = 0;
    static boolean synIgnoreBlock = false;
    /**
     * known ignore blocks
      */
    private static Map<Long, JSONObject> ignoreBlockMap = loadDefaultKnownIgnoreBlocks();

    private static Set<Long> knownIgnoreTxs = Sets.newHashSet();
    private static Map<Integer, Set<Long>> knownDirtyPoolTxs = Maps.newConcurrentMap();
    private static Map<Integer, Set<Long>> knownDirtyPocTxs = Maps.newConcurrentMap();

    static Map<Integer, Map<Long, PocTxBody.PocNodeTypeV2>> pocNodeTypeTxsMap = Maps.newHashMap();

    public static final int CHECK_INTERVAL_IN_MINUTES = Conch.getIntProperty("sharder.fetchConstantsInterval", 60);

    private static final Runnable updateKnownIgnoreBlocksThread = () -> {
        try {
            updateKnownIgnoreBlocks();
        } catch (Exception e) {
            Logger.logMessage("Error updateKnownIgnoreBlocksThread, wait for next turn.", e);
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    };

    private static Map<Long, JSONObject> loadDefaultKnownIgnoreBlocks() {
        Map<Long, JSONObject> defaultMap = Maps.newConcurrentMap();

        JSONArray defaultIgnoreBlocks = JSON.parseArray("["
//                +"{\"checksum\":\"9b6557deca53712337e330968fe7f98709ee0bdcd9ad1a60fbf0f50d7963650b4c4cfd4cf1a5a776c860b025055d4ef2f08aad609bb7fc908734bd1b79d8ef29\",\"height\":678,\"id\":-6134666443975256414,\"network\":\"testnet\"},"
                        + "]"
        );

        if (defaultIgnoreBlocks.size() <= 0) {
            return defaultMap;
        }

        for (int i = 0; i < defaultIgnoreBlocks.size(); i++) {
            JSONObject ignoreBlock = defaultIgnoreBlocks.getJSONObject(i);
            defaultMap.put(ignoreBlock.getLong("id"), ignoreBlock);
        }
        return defaultMap;
    }

    static {
        ThreadPool.scheduleThread("UpdateKnownIgnoreBlocksThread", updateKnownIgnoreBlocksThread, CHECK_INTERVAL_IN_MINUTES, TimeUnit.MINUTES);
    }

    public static boolean isKnownIgnoreBlock(long blockId, byte[] blockSignature) {
        if (!ignoreBlockMap.containsKey(blockId)) {
            return false;
        }

        // checksum compare
        JSONObject ignoreBlock = ignoreBlockMap.get(blockId);
        return StringUtils.equals(Convert.toHexString(blockSignature), ignoreBlock.getString("checksum"));
    }

    public static boolean isDoubleSpendingIgnoreTx(TransactionImpl tx) {
        //TODO: add ignore list
        return false;
    }

    public static boolean isKnownIgnoreTx(long txId) {
        boolean result = knownIgnoreTxs.contains(txId);

        return result;
    }

    public static boolean isDirtyPoolTx(int height, long accountId) {
        if (!knownDirtyPoolTxs.containsKey(height)) {
            return false;
        }

        boolean result = knownDirtyPoolTxs.get(height).contains(accountId);

        if (result) {
            Logger.logDebugMessage("found a known dirty pool tx[account id=%s] at height %d, ignore this tx", accountId, height);
        }
        return result;
    }


    /**
     * [POLYFILL]
     *
     * @param accountId
     * @param height
     * @return
     */
    public static PocTxBody.PocNodeTypeV2 isPreAccountsInTestnet(long accountId, int height) {
        if (Constants.isTestnet()
                && pocNodeTypeTxsMap.containsKey(height)) {
            return pocNodeTypeTxsMap.get(height).get(accountId);
        }
        return null;
    }

    public static PocTxBody.PocNodeTypeV2 isPreAccountsInTestnet(String host, int height) {
        NavigableSet<Integer> heightSet = Sets.newTreeSet(pocNodeTypeTxsMap.keySet()).descendingSet();
        for (Integer historyHeight : heightSet) {
            if (historyHeight <= height) {
                Map<Long, PocTxBody.PocNodeTypeV2> peerMap = pocNodeTypeTxsMap.get(historyHeight);
                Collection<PocTxBody.PocNodeTypeV2> nodeTypeTxs = peerMap.values();
                for (PocTxBody.PocNodeTypeV2 nodeTypeTx : nodeTypeTxs) {
                    if (StringUtils.equals(host, nodeTypeTx.getIp())) {
                        return nodeTypeTx;
                    }
                }
            }
        }
        return null;
    }

    static private boolean closeIgnore = false;

    private static boolean parseAndUpdateIgnoreSettings(JSONObject ignoreSettingObj) {
        try {
            if (closeIgnore) {
                return true;
            }

            JSONObject guardSettings = null;
            if (ignoreSettingObj.containsKey("GuardSettings")) {
                guardSettings = ignoreSettingObj.getJSONObject("GuardSettings");
            }
            JSONObject dirtyPocTxs = null;
            if (ignoreSettingObj.containsKey("DirtyPocTxs")) {
                dirtyPocTxs = ignoreSettingObj.getJSONObject("DirtyPocTxs");
            }
            JSONObject dirtyPoolAccounts = null;
            if (ignoreSettingObj.containsKey("DirtyPoolAccounts")) {
                dirtyPoolAccounts = ignoreSettingObj.getJSONObject("DirtyPoolAccounts");
            }
            JSONObject ignoreTxs = null;
            if (ignoreSettingObj.containsKey("IgnoreTxs")) {
                ignoreTxs = ignoreSettingObj.getJSONObject("IgnoreTxs");
            }

            // ignore blocks parse to support array and single object mode
            String ignoreBlocksKey = "IgnoreBlocks";
            if (ignoreSettingObj.containsKey("IgnoreBlokcs")) {
                ignoreBlocksKey = "IgnoreBlokcs";
            }
            String ignoreBlocksStr = ignoreSettingObj.getString(ignoreBlocksKey);

            JSONArray ignoreBlockArray = new JSONArray();
            if (ignoreBlocksStr.startsWith("[")) {
                ignoreBlockArray = JSONObject.parseArray(ignoreBlocksStr);
            } else {
                ignoreBlockArray.add(ignoreSettingObj.getJSONObject(ignoreBlocksKey));
            }

            JSONObject pocNodeTypeTxsV1 = null;
            if (ignoreSettingObj.containsKey("pocNodeTypeTxsV1")) {
                pocNodeTypeTxsV1 = ignoreSettingObj.getJSONObject("pocNodeTypeTxsV1");
            }

            try {
                if (guardSettings != null) {
                    Guard.init(guardSettings.getIntValue("FREQUENCY"),
                            guardSettings.getIntValue("FREQUENCY_TO_BLACK"),
                            guardSettings.getIntValue("MAX_THRESHOLD_PER_HOUR"),
                            guardSettings.getIntValue("MAX_TOTAL_CONNECT_COUNT_PER_DAY"),
                            guardSettings.getIntValue("MAX_VICIOUS_COUNT_PER_SAME_HOST"),
                            guardSettings.getIntValue("OPEN_BLACKLIST_FILTER"),
                            guardSettings.getBooleanValue("OPEN_SELF_CLOSING_MODE"),
                            guardSettings.getIntValue("CONNECT_BOOT_INTERVAL"),
                            guardSettings.getBooleanValue("FORCE_CONNECT_BOOT_NODE")
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.logErrorMessage("parsed GuardSettings error caused by " + e.getMessage());
            }

            try {
                if (ignoreBlockArray != null && ignoreBlockArray.size() > 0) {
                    // parse and update the ignore block collection
                    synchronized (ignoreBlockMap) {
                        for (int i = 0; i < ignoreBlockArray.size(); i++) {
                            JSONObject parsedObj = ignoreBlockArray.getJSONObject(i);
                            long blockId = parsedObj.getLong("id");
                            if (ignoreBlockMap.containsKey(blockId)) {
                                continue;
                            }
                            ignoreBlockMap.put(blockId, parsedObj);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.logErrorMessage("parsed known ignore blocks error caused by " + e.getMessage());
            }

            try {
                if (ignoreTxs != null) {
                    com.alibaba.fastjson.JSONArray array = ignoreTxs.getJSONArray("txs");
                    synchronized (knownIgnoreTxs) {
                        for (int i = 0; i < array.size(); i++) {
                            Long txid = array.getLong(i);
                            if (!knownIgnoreTxs.contains(txid)) {
                                knownIgnoreTxs.add(txid);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.logErrorMessage("parsed known ignore txs error caused by " + e.getMessage());
            }


            try {
                if (dirtyPoolAccounts != null) {
                    Integer height = dirtyPoolAccounts.getInteger("height");
                    synchronized (knownDirtyPoolTxs) {
                        if (!knownDirtyPoolTxs.containsKey(height)) {
                            knownDirtyPoolTxs.put(height, Sets.newHashSet());
                        }

                        Set<Long> dirtyPoolAccountsSet = knownDirtyPoolTxs.get(height);
                        com.alibaba.fastjson.JSONArray array = dirtyPoolAccounts.getJSONArray("dirtyPoolAccounts");
                        for (int i = 0; i < array.size(); i++) {
                            dirtyPoolAccountsSet.add(array.getLong(i));
                        }
                    }
                }
            } catch (Exception e) {
                Logger.logErrorMessage("parsed known dirty pool accounts error caused by " + e.getMessage());
            }


            try {
                if (dirtyPocTxs != null) {
                    Integer height = dirtyPocTxs.getInteger("height");
                    synchronized (knownDirtyPocTxs) {
                        if (!knownDirtyPocTxs.containsKey(height)) {
                            knownDirtyPocTxs.put(height, Sets.newHashSet());
                        }
                        Set<Long> dirtyPocTxsSet = knownDirtyPocTxs.get(height);

                        com.alibaba.fastjson.JSONArray array = dirtyPocTxs.getJSONArray("dirtyPocTxs");
                        for (int i = 0; i < array.size(); i++) {
                            dirtyPocTxsSet.add(array.getLong(i));
                        }
                    }
                }
            } catch (Exception e) {
                Logger.logErrorMessage("parsed known dirty poc txs error caused by " + e.getMessage());
            }


            try {
                if (pocNodeTypeTxsV1 != null) {
                    MultiValueMap pocNodeTypeTxsV1Map = JSONObject.parseObject(pocNodeTypeTxsV1.getString("pocNodeTypeTxsV1"), MultiValueMap.class);
                    Set<Integer> heightSet = pocNodeTypeTxsV1Map.keySet();

                    synchronized (pocNodeTypeTxsMap) {
                        for (Integer height : heightSet) {
                            Collection collection = pocNodeTypeTxsV1Map.getCollection(height);

                            if (!pocNodeTypeTxsMap.containsKey(height)) {
                                pocNodeTypeTxsMap.put(height, Maps.newHashMap());
                            }
                            Map<Long, PocTxBody.PocNodeTypeV2> pocNodeTypeV2Map = pocNodeTypeTxsMap.get(height);

                            for (Iterator it = collection.iterator(); it.hasNext(); ) {
                                Object attachment = it.next();
                                if (attachment instanceof com.alibaba.fastjson.JSONArray) {
                                    JSONArray jsonArray = (JSONArray) attachment;

                                    for (int i = 0; i < jsonArray.size(); i++) {
                                        JSONObject jsonObject = null;
                                        try {
                                            jsonObject = jsonArray.getJSONObject(i);
                                            String ip = jsonObject.getString("ip");
                                            String accountRs = jsonObject.getString("accountRs");
                                            int type = jsonObject.getIntValue("type");
                                            Byte version = jsonObject.getByte("version");
                                            Long accountId = Account.rsAccountToId(accountRs);
                                            //String ip, Peer.Type type, long accountId
                                            PocTxBody.PocNodeTypeV2 pocNodeTypeV2 = new PocTxBody.PocNodeTypeV2(ip, Peer.Type.getByCode(type), accountId);
                                            pocNodeTypeV2Map.put(accountId, pocNodeTypeV2);
                                        } catch (Exception e) {
                                            Logger.logErrorMessage("Poc node type tx convert failed caused by[%s] and detail is %s" + e.getMessage(), jsonObject == null ? "null" : jsonObject.toString());
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            } catch (Exception e) {
                Logger.logErrorMessage("parsed known poc node type v1 txs error caused by " + e.getMessage());
            }


        } catch (Exception e) {
            Logger.logErrorMessage("parsed and set single ignore block error caused by " + e.getMessage());
            return false;
        }
        return true;
    }


    public static void updateKnownIgnoreBlocks() {
        RestfulHttpClient.HttpResponse response = null;
        String url = UrlManager.CONSTANT_SETTINGS;
        try {
            response = RestfulHttpClient.getClient(url).get().request();
            if (response == null) {
                return;
            }
            String content = response.getContent();
            String totalIgnoreBlocks = "\n\r";
            if (content.startsWith("[")) {
                com.alibaba.fastjson.JSONArray array = JSON.parseArray(content);
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    totalIgnoreBlocks += object.toString() + "\n\r";
                    parseAndUpdateIgnoreSettings(object);
                }
            } else if (content.startsWith("{")) {
                com.alibaba.fastjson.JSONObject object = JSON.parseObject(content);
                totalIgnoreBlocks += object.toString() + "\n\r";
                parseAndUpdateIgnoreSettings(object);
            } else {
                Logger.logWarningMessage("not correct known ignore block get from " + url + " : " + content);
                return;
            }
//            if(totalIgnoreBlocks.length() > 4){
//                Logger.logDebugMessage("total ignore blocks get from %s as follow:" + totalIgnoreBlocks, url);
//            }

            // remove the dirty poc txs
            if (knownDirtyPocTxs.size() > 0) {
                Set<Long> dirtyPocTxs = Sets.newHashSet();
                knownDirtyPocTxs.values().forEach(ids -> dirtyPocTxs.addAll(ids));
                Conch.getPocProcessor().removeDelayedPocTxs(dirtyPocTxs);
            }

            // remove the dirty pools
            if (knownDirtyPoolTxs.size() > 0) {
                SharderPoolProcessor.removePools(knownDirtyPoolTxs);
            }

            if (!synIgnoreBlock) {
                synIgnoreBlock = true;
            }
        } catch (IOException e) {
            Logger.logErrorMessage("Can't get known ignore blocks from " + url + " caused by " + e.getMessage());
        }
    }

    public static JSONObject generateIgnoreBlock(long id, byte[] checksum, String network) {
        if (StringUtils.isEmpty(network)) {
            network = "testnet";
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("checksum", Convert.toString(checksum, false));
        jsonObject.put("network", network);
        return jsonObject;
    }

    public static void main(String[] args) {
        //updateKnownIgnoreBlocks();
        //Map<Long, JSONObject> map = loadDefaultKnownIgnoreBlocks();
        parseAndUpdateIgnoreSettings(new JSONObject());

    }

}
