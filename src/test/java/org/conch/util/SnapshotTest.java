package org.conch.util;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.conch.account.Account;
import org.conch.chain.BlockchainImpl;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-05-31
 */
public class SnapshotTest {


    public static void main(String[] args) {
        ssAmountSnapshot();
//        pocTxsSnapshot();
    }
    
    static void pocTxsSnapshot(){
        Db.init();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM TRANSACTION t WHERE t.TYPE=12 ORDER BY HEIGHT ASC");

            DbIterator<TransactionImpl> transactions = null;
            List<Transaction> txList = Lists.newArrayList();
            try {
                transactions = BlockchainImpl.getInstance().getTransactions(con, pstmt);
                while (transactions.hasNext()) {
                    Transaction transaction = transactions.next();
                    txList.add(transaction);

                    System.out.println("pocNodeTypeTxsMap.put(" + transaction.getHeight() + ",JSON.parseObject(\"" + transaction.getAttachment().getJSONObject().toJSONString().replaceAll("\"", "\\\\\"") + "\"));");
                }
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
}
