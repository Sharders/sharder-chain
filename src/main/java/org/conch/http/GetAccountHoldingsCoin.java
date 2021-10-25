package org.conch.http;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.conch.http.JSONResponses.ACCESS_CLOSED;

public class GetAccountHoldingsCoin extends APIServlet.APIRequestHandler {
    static final GetAccountHoldingsCoin instance = new GetAccountHoldingsCoin();

    private GetAccountHoldingsCoin() {
        super(new APITag[] {APITag.ACCOUNTS});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        JSONObject response = new JSONObject();
        Connection con = null;
        com.alibaba.fastjson.JSONArray transferJsonArray = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject amountJson = new com.alibaba.fastjson.JSONObject();

        if (!Constants.featureConf.getBooleanValue("OPEN_HOLDINGS_COIN_API")) {
            return ACCESS_CLOSED;
        }

        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ACCOUNT WHERE LATEST=TRUE ORDER BY BALANCE DESC");
            ResultSet rs = pstmt.executeQuery();
            amountJson.put("totalBalance" , 0L);
            amountJson.put("totalMined" , 0L);
            amountJson.put("totalFrozen" , 0L);
            amountJson.put("totalUnconfirmed" , 0L);
            while(rs.next()){
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

                com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject();
                jsonObject.put("recipientPublicKey", publicKeyStr);
                jsonObject.put("recipientRS", Account.rsAccount(accountId));
                jsonObject.put("amountNQT", balance);
                transferJsonArray.add(jsonObject);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
            response.put("holdingsCoinList", transferJsonArray);
            response.put("totalBalance", amountJson.get("totalBalance").toString() + "MW");
            response.put("height", Conch.getHeight());
        }
        return response;
    }

    @Override
    protected final boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected final boolean requireBlockchain() {
        return false;
    }
}
