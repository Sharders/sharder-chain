package org.conch.tx;

import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class GetTXsByAccount {
    public static void getTxsByAccount() {
        Connection con = null;
        PreparedStatement pstmt = null;
        String sqlTransfer = "SELECT * FROM TRANSACTION WHERE VERSION>=3 AND TYPE=0 AND SUBTYPE=0 AND HEIGHT>0 AND RECIPIENT_ID=?";
        JSONArray jsonArray = new JSONArray();
        String address = "CDW-5EV6-PFZ8-CE9U-ANBHA";
        String filename = "conf/accoutTxs.json";
        String csvFilename = "conf/accoutTxs.csv";
        try {
            con = Db.db.getConnection();
            // transfer statistics
            pstmt = con.prepareStatement(sqlTransfer);
            pstmt.setLong(1, Account.rsAccountToId(address));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject jsonObject = new JSONObject();
                    long sender_id = rs.getLong("SENDER_ID");
                    jsonObject.put("sender", Account.rsAccount(sender_id));
                    int timestamp = rs.getInt("TIMESTAMP");
                    jsonObject.put("timestamp", Convert.dateFromEpochTime(timestamp));
                    long amount = rs.getLong("AMOUNT");
                    jsonObject.put("amount", amount / Constants.ONE_SS);
                    jsonArray.add(jsonObject);
                }
            }
            // write jsonArray to jsonFile
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(address, jsonArray);
            JSON.JsonWrite(jsonObject, filename);
            // json to csv
            JSON.JsonToCSV(filename, csvFilename);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            DbUtils.close(con, pstmt);
        }
    }

    public static void main(String[] args) {
        Db.init();
        getTxsByAccount();
    }
}
