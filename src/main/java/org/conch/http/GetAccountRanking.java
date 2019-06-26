package org.conch.http;


import org.conch.common.ConchException;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GetAccountRanking extends APIServlet.APIRequestHandler {
    static final GetAccountRanking instance = new GetAccountRanking();

    private GetAccountRanking() {
        super(new APITag[]{APITag.DEBUG});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        String account = request.getParameter("account");
        String ranking = request.getParameter("ranking");
        JSONObject json = new JSONObject();
        json.put("success", true);
        try {
            if (account != null) {
                json.put("data", getAccountRanking(Convert.parseUnsignedLong(account)));
            } else if (ranking != null) {
                json.put("data", getAccountRanking(Integer.valueOf(ranking)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            json.put("success", false);
        }
        return json;
    }

    /**
     * 获得资产最排行
     *
     * @param num
     * @return
     */
    private Object getAccountRanking(int num) {
        num = num > 100 ? 100 : num;
        Object obj = null;
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT a.ID,a.BALANCE from ACCOUNT as a " +
                    "where a.DB_ID in (select max(DB_ID) from ACCOUNT as ma where a.ID = ma.ID) order by a.BALANCE desc limit ?");
            ps.setInt(1, num);
            obj = result(ps.executeQuery());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.close(con);
        }
        return obj;
    }

    /**
     * 获得某个账户的资产排行
     *
     * @param account
     * @return
     */
    private Object getAccountRanking(long account) {
        Connection con = null;
        Object obj = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as randking from " +
                    "(SELECT * from ACCOUNT as a where a.DB_ID in " +
                    "(select max(DB_ID) from ACCOUNT as ma where a.ID = ma.ID) order by a.BALANCE desc) as ma " +
                    "where ma.BALANCE >= (SELECT a.BALANCE from ACCOUNT as a where a.ID = ? order by a.DB_ID desc limit 1)");
            ps.setLong(1, account);
            obj = result(ps.executeQuery());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.close(con);
        }
        return obj;
    }

    /**
     * 结果集 转 ArrayList<Map<String, Object>>
     *
     * @param rs
     * @return
     * @throws SQLException
     */
    public static Object result(ResultSet rs) throws SQLException {
        ArrayList<Map<String, Object>> mapList = new ArrayList<>();
        ResultSetMetaData rsmd = rs.getMetaData();
        while (rs.next()) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                String colName = rsmd.getColumnName(i + 1);
                Object colValue = "ID".equalsIgnoreCase(colName) ? signedToUnsigned(rs.getObject(colName).toString()) : rs.getObject(colName);
                map.put(colName, colValue);
            }
            mapList.add(map);
        }
        return mapList;
    }

    /**
     * 将数据库的负数ID 转成正数的账户ID
     *
     * @param s
     * @return
     */
    private static String signedToUnsigned(String s) {
        BigDecimal num = new BigDecimal(s);
        if (num.compareTo(new BigDecimal(0)) >= 0) {
            return num.toString();
        }
        num = num.abs();
        BigDecimal bd = new BigDecimal(Long.MIN_VALUE).abs().subtract(num).multiply(new BigDecimal(2));
        return num.add(bd).toString();
    }

}
