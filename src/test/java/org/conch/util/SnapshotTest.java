package org.conch.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.BlockchainImpl;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.peer.CertifiedPeer;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.tx.TransactionType;

/**
 * @author <a href="mailto:xy@mwfs.io">Ben</a>
 * @since 2019-05-31
 */
public class SnapshotTest {

    private static final String AMOUNT_400 = "40000000000";
    private static final String AMOUNT_100 = "10000000000";

    static class TransferInfo {
        public String recipientRS;
        public String amountNQT;
        public String recipientPublicKey;

        TransferInfo() {}

        public TransferInfo(String recipientRS, String amountNQT) {
            this.recipientRS = recipientRS;
            this.amountNQT = amountNQT;
        }
    }
    static class Miner {
        public String accountRS;
        public String recipientPublicKey;
        public String amountNQT;
        Miner() {}

        public Miner(String accountRS) {
            this.accountRS = accountRS;
        }

        public Miner(String accountRS, String amountNQT) {
            this.accountRS = accountRS;
            this.amountNQT = amountNQT;
        }

        public String getRecipientPublicKey() {
            return recipientPublicKey;
        }

        public void setRecipientPublicKey(String recipientPublicKey) {
            this.recipientPublicKey = recipientPublicKey;
        }

        public String getAmountNQT() {
            return amountNQT;
        }

        public void setAmountNQT(String amountNQT) {
            this.amountNQT = amountNQT;
        }

        public String getAccountRS() {
            return accountRS;
        }

        public void setAccountRS(String accountRS) {
            this.accountRS = accountRS;
        }

    }

    public static void main(String[] args) {
//        ssAmountSnapshot();
        ssHoldingAmount();
//        pocTxsSnapshot();
//        ssPaymentTxsSnapshot();
//        amountAirdropBySnapshot();
//        airdropDataStatistics();
//        fileDataComparison();
    }

    /**
     * 交互式矿工数据比对
     * 支持大于等于2个文件进行数据比对，取所有文件交集数据并输出到指定的文件
     * @return
     */
    private static String fileDataComparison() {
        // 交互式
        Scanner scanner = null;
        if(INTERACTION_MODE) {
            scanner = new Scanner(System.in);
        }
        // 1.1. 输入文件路径
        String path = null;
        if(INTERACTION_MODE) {
            System.out.println(String.format("Input the file path(Press enter, default is %s): ", DEFAULT_BATCH_PATH));
            path = scanner.nextLine();
        }
        path = StringUtils.isEmpty(path) ? DEFAULT_BATCH_PATH : path;
        // 判断该路径是否存在
        File pathFile = new File(path);
        if (!pathFile.exists()) {
            System.out.println("file path is not exists!\n");
        }

        // 1.2. 输入文件名和文件格式
        String filename = null;
        if(INTERACTION_MODE) {
            System.out.println("Input the file name or files name:");
            System.out.println("- filename_1.json or array mode: filename_1.json,filename_2.json...");
            System.out.println("- input * or enter directly means scan all files below the path(default): ");
            filename = scanner.nextLine();
        }
        boolean isScanFilesMode = StringUtils.isEmpty(filename) || "*".equalsIgnoreCase(filename);
        List<String> jsonFiles = Lists.newArrayList();

        if(isScanFilesMode) {
            File[] files = pathFile.listFiles();
            for(int i = 0 ; i < files.length ; i++){
                String airdropFileName = files[i].getName();
                boolean isIgnoreFile = (".DS_Store".equalsIgnoreCase(airdropFileName))
                        || (StringUtils.isNotEmpty(airdropFileName) && (airdropFileName.startsWith(".")));
                if(isIgnoreFile){
                    continue;
                }
                jsonFiles.add(path + File.separator + airdropFileName);
            }
        }else{
            boolean arrayMode = filename.contains(",");
            if(arrayMode){
                String[] fileArray = filename.split(",");
                for(int i = 0 ; i < fileArray.length ; i++){
                    jsonFiles.add(path + File.separator + fileArray[i]);
                }
            }else{
                jsonFiles.add(path + File.separator + filename);
            }
        }

        String typeStr = null;
        if(INTERACTION_MODE) {
            System.out.println(String.format("Choose the code of type (1-Intersection, 2-Difference, " +
                    "enter will use the default value %s): ", DEFAULT_STATIS_TYPE));
            typeStr = scanner.nextLine();
        }
        typeStr = StringUtils.isEmpty(typeStr) ? DEFAULT_STATIS_TYPE : typeStr;
        int type = Integer.valueOf(typeStr).intValue();
        String typeStrPrint = (type == 1 ? "Intersection" : "Difference");
        // 1.3. 解析文件
        if(jsonFiles.size() == 0) {
            System.out.println(String.format("Not found files[path=%s, type=%s] to analyze, exit the statistic", path, typeStrPrint));
            System.exit(1);
        }
        // 存储所有数据
        List<List<Miner>> complexList = Lists.newArrayList();
        jsonFiles.forEach(jsonFile -> {
            String singleStatistic = dataComparison(jsonFile, type, jsonFiles.size(), complexList);
            if(StringUtils.isNotEmpty(singleStatistic)){
                System.out.println(singleStatistic);
            }
        });
        return path;
    }

    private static String dataComparison(String pathFileName, int type, int size, List<List<Miner>> complexList) {
        String statis = "";
        try{
            String readJsonStr = org.conch.util.JSON.readJsonFile(pathFileName);
            JSONObject parseObject = JSON.parseObject(readJsonStr);
            List<Miner> listOrigin = null;
            listOrigin = JSONObject.parseArray(parseObject.getJSONArray("crowdMiners").toJSONString(), Miner.class);
            complexList.add(listOrigin);
            if (complexList.size() >= 2) {
                // 比对complexList第一项 & 最后一项文件数据，取交集，存入第一项
                List<Miner> oldList = complexList.get(0);
                List<Miner> newList = complexList.get(complexList.size() - 1);
                // todo list去重 - 暂不处理

                // 判断较大数组作为old，较大数组作为基准
                if (oldList.size() < newList.size()) {
                    List<Miner> middleList = null;
                    middleList = newList;
                    newList = oldList;
                    oldList = middleList;
                }
                // 新建list保存 交集
                List<Miner> list = Lists.newArrayList();
                // 转换oldList, 用于比对
                List<String> strList = oldList.stream().map(Miner::getAccountRS).collect(Collectors.toList());
                if (type == 1) {
                    for (Miner miner : newList) {
                        if (strList.contains(miner.accountRS)) {
                            miner.setAmountNQT(AMOUNT_400);
                            miner.setRecipientPublicKey("");
                            list.add(miner);
                        }
                    }

                } else if (type == 2) {
                    for (Miner miner : newList) {
                        if (strList.contains(miner.accountRS)) {
                            strList.remove(miner.accountRS);
                        }
                    }
                    // strList 转为 List<Miner>
                    for (String s : strList) {
                        list.add(new Miner(s, AMOUNT_100));
                    }
                }
                // 将list存入complexList第一项
                complexList.set(0, list);
            }
            if (complexList.size() == size) {
                // 遍历结束，输出交集
                List<Miner> minerList = complexList.get(0);
                // javabean 转换
                List<TransferInfo> transferInfoList = Lists.newArrayList();
                for (Miner miner : minerList) {
                    transferInfoList.add(new TransferInfo(miner.accountRS, miner.amountNQT));
                }
                org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
                jsonObject.put("list", JSON.toJSON(transferInfoList));
                jsonObject.put("listCount", transferInfoList.size());
                jsonObject.put("feeNQT", "0");
                jsonObject.put("deadline", "30");
                jsonObject.put("secretPhrase", "");

                org.conch.util.JSON.JsonWrite(jsonObject, DEFAULT_BATCH_PATH + File.separator + (type == 1 ? MINER_INTERSECTION : MINER_DIFF));
                statis = "operate successfully!\n";
            }

        }catch(Exception e){
            statis = "operate aborted!\n";
        }
        return statis;
    }

    private static final String DEFAULT_BATCH_PATH="batch";
    private static final String DEFAULT_STATIS_TYPE = "1";
    private static final boolean INTERACTION_MODE = true;
    private static final String MINER_INTERSECTION = "minerIntersectionList.json";
    private static final String MINER_DIFF = "minerDiffList.json";
    private static void airdropDataStatistics() {
        // 交互式
        Scanner scanner = null;
        if(INTERACTION_MODE) {
            scanner = new Scanner(System.in);
        }
        // 1.1. 输入文件路径
        String path = null;
        if(INTERACTION_MODE) {
            System.out.println(String.format("Input the file path(Press enter, default is %s): ", DEFAULT_BATCH_PATH));
            path = scanner.nextLine();
        }
        path = StringUtils.isEmpty(path) ? DEFAULT_BATCH_PATH : path;
        // 判断该路径是否存在
        File pathFile = new File(path);
        if (!pathFile.exists()) {
            System.out.println("file path is not exists!\n");
        }

        // 1.2. 输入文件名和文件格式
        String filename = null;
        if(INTERACTION_MODE) {
            System.out.println("Input the file name or files name:");
            System.out.println("- airdrop_1.json or array mode: airdrop_1.json,airdrop_2.json...");
            System.out.println("- input * or enter directly means scan all files below the path): ");
            filename = scanner.nextLine();
        }
        boolean isScanFilesMode = StringUtils.isEmpty(filename) || "*".equalsIgnoreCase(filename);
        List<String> jsonFiles = Lists.newArrayList();

        if(isScanFilesMode) {
            File[] files = pathFile.listFiles();
            for(int i = 0 ; i < files.length ; i++){
                String airdropFileName = files[i].getName();
                boolean isIgnoreFile = (".DS_Store".equalsIgnoreCase(airdropFileName))
                        || (StringUtils.isNotEmpty(airdropFileName) && airdropFileName.startsWith("."));
                if(isIgnoreFile){
                    continue;
                }
                jsonFiles.add(path + File.separator + airdropFileName);
            }
        }else{
            boolean arrayMode = filename.contains(",");
            if(arrayMode){
                String[] fileArray = filename.split(",");
                for(int i = 0 ; i < fileArray.length ; i++){
                    jsonFiles.add(path + File.separator + fileArray[i]);
                }
            }else{
                jsonFiles.add(path + File.separator + filename);
            }
        }

        String typeStr = null;
        if(INTERACTION_MODE) {
            System.out.println(String.format("Choose the code of type (1-airdrop, 2-airdrop result, " +
                    "enter will use the default value %s): ", DEFAULT_STATIS_TYPE));
            typeStr = scanner.nextLine();
        }
        typeStr = StringUtils.isEmpty(typeStr) ? DEFAULT_STATIS_TYPE : typeStr;
        int type = Integer.valueOf(typeStr).intValue();
        String typeStrPrint = (type == 1 ? "airdrop" : "airdrop result");
        // 1.3. 解析文件
        if(jsonFiles.size() == 0) {
            System.out.println(String.format("Not found airdrop files[path=%s, type=%s] to analyze, exit the statistic", path, typeStrPrint));
            System.exit(1);
        }
        jsonFiles.forEach(jsonFile -> {
            String singleStatistic = singleAirdropFileStatistics(jsonFile, type);
            if(StringUtils.isNotEmpty(singleStatistic)){
                System.out.println(singleStatistic);
            }
        });
        System.out.println(String.format("##########################\n" +
                        "Statistic %d airdrop files\nPath: %s\nType: %s \nAirdrop files: %s\n",
                jsonFiles.size(), path, typeStrPrint, Arrays.toString(jsonFiles.toArray())));
    }

    private static String singleAirdropFileStatistics(String pathFileName, int type){
        String statis = "";
        try{
            String readJsonStr = org.conch.util.JSON.readJsonFile(pathFileName);
            JSONObject parseObject = JSON.parseObject(readJsonStr);
            JSONArray listOrigin = null;
            if(type == 1){
                listOrigin = parseObject.getJSONArray("list");
            }else if(type == 2){
                listOrigin = parseObject.getJSONArray("failList");
            }
            // 2. 输出文件数据的统计信息
            Integer airdropSize = listOrigin.size();
            Long totalAmount = 0L;
            Long maxSingleAmount = 0L;
            Long minSingleAmount = 0L;
            List<TransferInfo> list = JSONObject.parseArray(listOrigin.toJSONString(), TransferInfo.class);
            for (TransferInfo info : list) {
                Long singleAmount = Long.parseLong(info.amountNQT);
                if(maxSingleAmount == 0 || singleAmount >= maxSingleAmount){
                    maxSingleAmount = singleAmount;
                }
                if(minSingleAmount == 0 || singleAmount <= minSingleAmount){
                    minSingleAmount = singleAmount;
                }
                totalAmount += singleAmount;
            }
            BigDecimal totalAmountBD = new BigDecimal(totalAmount / Constants.ONE_SS).setScale(0, RoundingMode.UP);
            BigDecimal avgSingleAmountBD = new BigDecimal(totalAmount / Constants.ONE_SS / airdropSize).setScale(0, RoundingMode.UP);
            statis += pathFileName + "\n";
            statis += "----------------------" + "\n";
            statis += String.format("Account Count: %d \n", airdropSize);
            statis += String.format("Max Amount: %d | Min Amount: %d | Avg Amount: %d\n", maxSingleAmount / Constants.ONE_SS, minSingleAmount / Constants.ONE_SS, avgSingleAmountBD.longValue());
            statis += String.format("Total Amount: %d %s (%d NQT) \n", totalAmountBD.longValue(), Conch.COIN_UNIT, totalAmount);
            statis += String.format("Suggest airdrop out account balance: %d %s (Estimated Fees %d %s) \n", (totalAmountBD.longValue() + airdropSize), Conch.COIN_UNIT, airdropSize, Conch.COIN_UNIT);
        }catch(Exception e){
           // System.out.println(e.getMessage());
            statis = null;
        }
        return statis;
    }

    private static int startHeight = 270;
    static void pocTxsSnapshot(){
        Db.init();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM TRANSACTION t WHERE t.TYPE=12 ORDER BY HEIGHT ASC");

            DbIterator<TransactionImpl> transactions = null;
            List<Transaction> txList = Lists.newArrayList();
            Map<String, org.json.simple.JSONObject> txMap = new HashMap<>();
            try {
                transactions = BlockchainImpl.getInstance().getTransactions(con, pstmt);
                while (transactions.hasNext()) {
                    Transaction transaction = transactions.next();
                    txList.add(transaction);

                    org.json.simple.JSONObject txObj = transaction.getAttachment().getJSONObject();
                    txObj.put("height", transaction.getHeight());
                    if (null != txObj.get("accountId")) {
                        Long accountId = (long)txObj.get("accountId");
                        if(accountId != 0){
                            txObj.put("rsAccount", Account.rsAccount(accountId));
                        }
                    }
                    txMap.put((String) txObj.get("ip"), txObj);
                }

                txMap.values().forEach(jsonObject -> {
                    
                    int height = (int)jsonObject.get("height");
                    jsonObject.remove("height");
                    if(jsonObject.containsKey("rsAccount")) {
                        System.out.println("\"{\\\"ip\\\":\\\"IP\\\",\\\"type\\\":\\\"Hub\\\",\\\"bindRs\\\":\\\"RS\\\"},\" +".replace("IP", (String)jsonObject.get("ip")).replace("RS",(String)jsonObject.get("rsAccount")));
                    }
                   
//                    System.out.println("pocNodeTypeTxsMap.put(" +  height + ",JSON.parseObject(\"" + jsonObject.toJSONString().replaceAll("\"", "\\\\\"") + "\"));");
                });
                System.out.println("total tx size is " + txMap.size());
            } finally {
                DbUtils.close(con);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
    }

    static void ssAmountSnapshot(){
        Db.init();
        Connection con = null;
        String accountBalStr = "";
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ACCOUNT WHERE LATEST=TRUE ORDER BY HEIGHT ASC");

            ResultSet rs = pstmt.executeQuery();
            int count = 1;
            JSONObject amountJson = new JSONObject();
            amountJson.put("totalBalance" , 0L);
            amountJson.put("totalMined" , 0L);
            amountJson.put("totalFrozen" , 0L);
            amountJson.put("totalUnconfirmed" , 0L);
            String transferRecords = "\n\r******Transfers******\n\r";
            String transferJson = "\n\r******TransfersJson******\n\r[";
            while(rs.next()){
                long accountId = rs.getLong("ID");
                long balance = rs.getLong("BALANCE");
                long unconfirmedBalance = rs.getLong("UNCONFIRMED_BALANCE");
                long minedBalance = rs.getLong("FORGED_BALANCE");
                long frozenBlance = rs.getLong("FROZEN_BALANCE");
                int height = rs.getInt("HEIGHT");
                amountJson.put("totalBalance" , amountJson.getLongValue("totalBalance") + (balance / Constants.ONE_SS));
                amountJson.put("totalMined" , amountJson.getLongValue("totalMined") + (minedBalance/ Constants.ONE_SS));
                amountJson.put("totalFrozen" , amountJson.getLongValue("totalFrozen") + (frozenBlance/ Constants.ONE_SS));
                amountJson.put("totalUnconfirmed" , amountJson.getLongValue("totalUnconfirmed") + (unconfirmedBalance/ Constants.ONE_SS));
                accountBalStr += Account.rsAccount(accountId)
                        + ",accountId=" + accountId
                        + ",balance=" + balance
                        + ",unconfirmedBalance=" + unconfirmedBalance
                        + ",minedBalance=" + minedBalance
                        + ",frozenBlance=" + frozenBlance
                        + ",height=" + height
                        + "\n";
//                rs.getLong("SHARDER.PUBLIC.ACCOUNT.LATEST");
                transferRecords += Account.rsAccount(accountId) + "\n"
                        + ",accountId=" + accountId + "\n"
                        + ",balance=" + balance + "\n"
                        + ",unconfirmedBalance=" + unconfirmedBalance + "\n"
                        + "";
                transferJson += "{"
                        + "\"recipientRS\":\"" + Account.rsAccount(accountId) + "\""
                        + ",\"amountNQT\":\"" + balance + "\""
                        + "},\n";
                count++;
            }
            
            if(transferJson.endsWith(",\n")){
                transferJson = transferJson.substring(0,transferJson.length()-2);
            }
            System.out.println("Total count is " + count + "\n\r" + accountBalStr);
            System.out.println("Total balance is " + amountJson.toString() );

            System.out.println(transferRecords);
            System.out.println(transferJson);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
    }

    static void ssHoldingAmount(){
        Db.init();
        Connection con = null;
        String path = "batch";
        String filenamePrefix = "batch/holdingList";
        File file = new File(path);
        if(!file.exists()) {
            file.mkdir();
        }
        int count = 0;
        int batchUnit = 200000;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ACCOUNT WHERE LATEST=TRUE ORDER BY BALANCE DESC");
            ResultSet rs = pstmt.executeQuery();
            JSONObject amountJson = new JSONObject();
            amountJson.put("totalBalance" , 0L);
            amountJson.put("totalMined" , 0L);
            amountJson.put("totalFrozen" , 0L);
            amountJson.put("totalUnconfirmed" , 0L);
            String transferJsonStr = "";
            while(rs.next()){
                count++;
                long accountId = rs.getLong("ID");
                long balance = rs.getLong("BALANCE");
                long unconfirmedBalance = rs.getLong("UNCONFIRMED_BALANCE");
                long minedBalance = rs.getLong("FORGED_BALANCE");
                long frozenBlance = rs.getLong("FROZEN_BALANCE");
                byte[] publicKey = Account.getPublicKey(accountId);
                String publicKeyStr = Convert.toHexString(publicKey);
                amountJson.put("totalBalance" , amountJson.getLongValue("totalBalance") + (balance / Constants.ONE_SS));
                amountJson.put("totalMined" , amountJson.getLongValue("totalMined") + (minedBalance/ Constants.ONE_SS));
                amountJson.put("totalFrozen" , amountJson.getLongValue("totalFrozen") + (frozenBlance/ Constants.ONE_SS));
                amountJson.put("totalUnconfirmed" , amountJson.getLongValue("totalUnconfirmed") + (unconfirmedBalance/ Constants.ONE_SS));

                transferJsonStr += "{"
                        + "\"recipientPublicKey\":\"" + publicKeyStr + "\""
                        + ",\"recipientRS\":\"" + Account.rsAccount(accountId) + "\""
                        + ",\"amountNQT\":\"" + balance + "\""
                        + "},\n";

                if (batchUnit == 0) {
                    System.out.println("batchUnit can`t equal zero\n");
                }
                if (count % batchUnit == 0) {
                    String filename;
                    String csvFilename;
                    filename = filenamePrefix + "_" + count + ".json";
                    csvFilename = filenamePrefix + "_" + count + ".csv";
                    transferJsonStr = "[" + transferJsonStr + "]";
                    writeToHoldingFile(transferJsonStr, filename);
                    org.conch.util.JSON.JsonToCSV(filename, csvFilename);
                    transferJsonStr = "";
                }

            }
            if (StringUtils.isNotEmpty(transferJsonStr)) {
                // extra is stored in the file
                transferJsonStr = "[" + transferJsonStr + "]";
                writeToHoldingFile(transferJsonStr, filenamePrefix + "_" + count + ".json");
                org.conch.util.JSON.JsonToCSV(filenamePrefix + "_" + count + ".json", filenamePrefix + "_" + count + ".csv");
            }
            System.out.println("Total count is " + count + "\n\r");
            System.out.println("Total balance is " + amountJson.toString() );
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
    }

    static void amountAirdropBySnapshot(){
        Db.init();
        Connection con = null;
        String path = "batch";
        String filenamePrefix = "batch/airdrop";
        File file = new File(path);
        if(!file.exists()) {
            file.mkdir();
        }
        int count = 0;
        int batchUnit = 500;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ACCOUNT WHERE LATEST=TRUE ORDER BY BALANCE ASC");
            ResultSet rs = pstmt.executeQuery();
            JSONObject amountJson = new JSONObject();
            amountJson.put("totalBalance" , 0L);
            amountJson.put("totalMined" , 0L);
            amountJson.put("totalFrozen" , 0L);
            amountJson.put("totalUnconfirmed" , 0L);
            String transferJsonStr = "";
            while(rs.next()){
                count++;
                long accountId = rs.getLong("ID");
                long balance = rs.getLong("BALANCE");
                long unconfirmedBalance = rs.getLong("UNCONFIRMED_BALANCE");
                long minedBalance = rs.getLong("FORGED_BALANCE");
                long frozenBlance = rs.getLong("FROZEN_BALANCE");
                byte[] publicKey = Account.getPublicKey(accountId);
                String publicKeyStr = Convert.toHexString(publicKey);
                amountJson.put("totalBalance" , amountJson.getLongValue("totalBalance") + (balance / Constants.ONE_SS));
                amountJson.put("totalMined" , amountJson.getLongValue("totalMined") + (minedBalance/ Constants.ONE_SS));
                amountJson.put("totalFrozen" , amountJson.getLongValue("totalFrozen") + (frozenBlance/ Constants.ONE_SS));
                amountJson.put("totalUnconfirmed" , amountJson.getLongValue("totalUnconfirmed") + (unconfirmedBalance/ Constants.ONE_SS));

                transferJsonStr += "{"
                        + "\"recipientPublicKey\":\"" + publicKeyStr + "\""
                        + ",\"recipientRS\":\"" + Account.rsAccount(accountId) + "\""
                        + ",\"amountNQT\":\"" + balance + "\""
                        + "},\n";

                if (batchUnit == 0) {
                    System.out.println("batchUnit can`t equal zero\n");
                }
                if (count % batchUnit == 0) {
                    String filename;
                    filename = filenamePrefix + "_" + count + ".json";
                    transferJsonStr = "[" + transferJsonStr + "]";
                    writeToAirdropFile(transferJsonStr, filename);
                    transferJsonStr = "";
                }

            }
            if (StringUtils.isNotEmpty(transferJsonStr)) {
                // extra is stored in the file
                transferJsonStr = "[" + transferJsonStr + "]";
                writeToAirdropFile(transferJsonStr, filenamePrefix + "_" + count + ".json");
            }
            System.out.println("Total count is " + count + "\n\r");
            System.out.println("Total balance is " + amountJson.toString() );
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
    }

    private static void writeToAirdropFile(String transferJson, String filename) {
        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
        JSONArray jsonArray = JSON.parseArray(transferJson);
        jsonObject.put("list", jsonArray);
        jsonObject.put("secretPhrase", "");
        jsonObject.put("feeNQT", "0");
        jsonObject.put("deadline", "1440");

        org.conch.util.JSON.JsonWrite(jsonObject, filename);
        System.out.println(String.format("write to file %s succeed", filename));

    }

    private static void writeToHoldingFile(String transferJson, String filename) {
        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
        JSONArray jsonArray = JSON.parseArray(transferJson);
        jsonObject.put("list", jsonArray);

        org.conch.util.JSON.JsonWrite(jsonObject, filename);
        System.out.println(String.format("write to file %s succeed", filename));

    }

    private static Set<String> ignoreReciepects = Sets.newHashSet("CDW-L9V5-6FNQ-NJKX-8UNH9");
    static void ssPaymentTxsSnapshot(){
        Db.init();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM TRANSACTION t WHERE t.TYPE=" + TransactionType.TYPE_PAYMENT + " AND HEIGHT > " + startHeight + " ORDER BY HEIGHT ASC");
            DbIterator<TransactionImpl> transactions = null;
            List<Transaction> txList = Lists.newArrayList();
            JSONObject summaryObj = new JSONObject();
            
            String ignoreDetail = "--Ignore List--\n";
            try {
                transactions = BlockchainImpl.getInstance().getTransactions(con, pstmt);
                while (transactions.hasNext()) {
                    Transaction tx = transactions.next();
                    Account senderAccount = Account.getAccount(tx.getSenderId());
                    Account recipientAccount = Account.getAccount(tx.getRecipientId());
                    long amount =  tx.getAmountNQT() / Constants.ONE_SS;
                    String txStr = senderAccount.getRsAddress() + " -> " + recipientAccount.getRsAddress() + " amount " + amount + " at height " + tx.getHeight();
                   
                    if(ignoreReciepects.contains(recipientAccount.getRsAddress())) {
                        ignoreDetail += txStr + "\n";
                        continue;
                    }

                    System.out.println(txStr);
                    if(summaryObj.containsKey("totalAmount")){
                        summaryObj.put("totalAmount",summaryObj.getLongValue("totalAmount") + amount);
                    }else{
                        summaryObj.put("totalAmount",amount);
                    }

                    if(summaryObj.containsKey("count")){
                        summaryObj.put("count",summaryObj.getLongValue("count") + 1);
                    }else{
                        summaryObj.put("count",1);
                    }
                }
            } finally {
                DbUtils.close(transactions);
            }
            System.out.println(ignoreDetail);
            
            System.out.println("\n" + summaryObj.toString());

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
    }


    private static void exportDeclaredPeerList(Map<Long, CertifiedPeer> certifiedPeers, int height){
        // certified peer: foundation node, hub/box, community node
        // account id : certified peer
        Collection<CertifiedPeer> peers = certifiedPeers.values();
        String rsAddrList = "";
        String mwAmountList = "";
        String heightList = "";
        String hostList = "";
        String typeList = "";
        String updateTimeList = "";
        String originalData = "";
        for(CertifiedPeer certifiedPeer : peers){
            JSONObject peerObj = new JSONObject();
            long mwAmount = 0;
            try{
                mwAmount = Account.getAccount(certifiedPeer.getBoundAccountId()).getEffectiveBalanceSS(height);
            }catch(Exception e){

            }

            rsAddrList += certifiedPeer.getBoundRS() + "\n";
            mwAmountList += mwAmount + "\n";
            heightList += certifiedPeer.getHeight() + "\n";
            hostList += certifiedPeer.getHost() + "\n";
            typeList += certifiedPeer.getType().getSimpleName() + "\n";
            updateTimeList += certifiedPeer.getUpdateTime().toString() + "\n";

            peerObj.put("BindAddr",certifiedPeer.getBoundRS());
            peerObj.put("MwAmount",mwAmount);
            peerObj.put("DeclaredHeight",certifiedPeer.getHeight());
            peerObj.put("PeerHost",certifiedPeer.getHost());
            peerObj.put("PeerType",certifiedPeer.getType().getSimpleName());
            peerObj.put("UpdateTime",certifiedPeer.getUpdateTime().toString());
            originalData += peerObj.toJSONString() + "\n";
        }

        Logger.logDebugMessage("\n## BindAddr ##\n\r" + rsAddrList);
        Logger.logDebugMessage("\n## DeclaredHeight ##\n\r" + heightList);
        Logger.logDebugMessage("\n## mwAmountList ##\n\r" + mwAmountList);
        Logger.logDebugMessage("\n## PeerHost ##\n\r" + hostList);
        Logger.logDebugMessage("\n## PeerType ##\n\r" + typeList);
        Logger.logDebugMessage("\n## UpdateTime ##\n\r" + updateTimeList);
        Logger.logDebugMessage("\n## Original Data ##\n\r" + originalData);
    }

    public static void printDeclaredPeersSnapshot(int height){
        if(Conch.getHeight() == height) {
            // 2601
           exportDeclaredPeerList(Conch.getPocProcessor().getCertifiedPeers(), 2600);
        }
    }
}
