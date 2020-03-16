package org.conch.util;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.conch.account.Account;
import org.conch.chain.BlockchainImpl;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.tx.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-05-31
 */
public class SnapshotTest {


    public static void main(String[] args) {
//        ssAmountSnapshot();
        pocTxsSnapshot();
//        ssPaymentTxsSnapshot();
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
                    Long accountId = (long)txObj.get("accountId");
                    if(accountId != 0){
                        txObj.put("rsAccount", Account.rsAccount(accountId));
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
                long frozenBalance = rs.getLong("FROZEN_BALANCE");
                int height = rs.getInt("HEIGHT");
                amountJson.put("totalBalance" , amountJson.getLongValue("totalBalance") + (balance / Constants.ONE_SS));
                amountJson.put("totalMined" , amountJson.getLongValue("totalMined") + (minedBalance/ Constants.ONE_SS));
                amountJson.put("totalFrozen" , amountJson.getLongValue("totalFrozen") + (frozenBalance/ Constants.ONE_SS));
                amountJson.put("totalUnconfirmed" , amountJson.getLongValue("totalUnconfirmed") + (unconfirmedBalance/ Constants.ONE_SS));
                accountBalStr += Account.rsAccount(accountId)
                        + ",accountId=" + accountId
                        + ",balance=" + balance
                        + ",unconfirmedBalance=" + unconfirmedBalance
                        + ",minedBalance=" + minedBalance
                        + ",frozenBalance=" + frozenBalance
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
    
    private static Set<String> ignoreReciepects = Sets.newHashSet("SSA-L9V5-6FNQ-NJKX-8UNH9");
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
}
