package org.conch.http;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.db.DbUtils;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class GetSnapshotAccounts extends APIServlet.APIRequestHandler {

    static final GetSnapshotAccounts instance = new GetSnapshotAccounts();

    private GetSnapshotAccounts() {
        super(new APITag[] {APITag.ACCOUNTS}, "height");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {

        String reqHeight = request.getParameter("height");

        return getAccounts();
    }

    private JSONStreamAware getAccounts() {
        Connection con = null;
        String accountBalStr = "";
        JSONObject response = new JSONObject();
        try {
            // TODO 数据库快照（zip格式） -> 可访问的数据库形式
            // 1. 获取到快照所在文件夹

            File file = new File(Conch.getStringProperty("sharder.db.backup.path"));
            File[] files = file.listFiles();
            String snapShotName = files[0].getName();
            System.out.println("snapshotName" + snapShotName);
            // 2. 执行RUNSCRIPT SQL语句进行恢复

            // 3. 创建对该数据库的连接及相关初始化
//            con = SnapshotDb.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ACCOUNT WHERE LATEST=TRUE ORDER BY HEIGHT ASC");

            ResultSet rs = pstmt.executeQuery();
            int count = 1;
            com.alibaba.fastjson.JSONObject amountJson = new com.alibaba.fastjson.JSONObject();
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
            Logger.logDebugMessage("Total count is " + count + "\n\r" + accountBalStr);
            Logger.logDebugMessage("Total balance is " + amountJson.toString() );

            Logger.logDebugMessage(transferJson);
            Logger.logDebugMessage(transferRecords);
            response.put("accountsBalance", accountBalStr);
            response.put("totalAmount", amountJson);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
            return response;
        }
    }


}
