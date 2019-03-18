/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.http.biz.handler;

import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.db.*;
import org.conch.db.*;
import org.conch.http.APIServlet;
import org.conch.http.APITag;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


public final class GetTxStatistics extends APIServlet.APIRequestHandler {

    public static final GetTxStatistics instance = new GetTxStatistics();

    private GetTxStatistics() {
        super(new APITag[]{APITag.BIZ});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        Long transferCount = 0L;
        Long transferAmount = 0L;
        Long transferCount24H = 0L;
        Long transferAmount24H = 0L;
        Long storageCount = 0L;
        Long storageDataLength = 0L;
        Long storageCount24H = 0L;
        Long storageDataLength24H = 0L;
        JSONObject jsonObject = new JSONObject();

        Connection con = null;
        String sqlTransfer = "SELECT COUNT(*),SUM(AMOUNT)/100000000 FROM TRANSACTION WHERE VERSION=1 AND TYPE=0 AND SUBTYPE=0 AND HEIGHT>0 AND TIMESTAMP > ?";
        String sqlStorage = "SELECT COUNT(*),SUM(LENGTH (t2.DATA)) FROM TRANSACTION t1 , TAGGED_DATA t2 WHERE t1.ID = t2.ID AND t1.VERSION=1 AND t1.TYPE=6 AND t1.SUBTYPE=0 AND t1.HEIGHT>0 AND TIMESTAMP > ?";
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sqlTransfer);
            pstmt.setInt(1, 0);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    transferCount = rs.getLong(1);
                    transferAmount = rs.getLong(2);
                }
            }
            Instant timestampNow = Instant.now();
            Instant timestamp24H = timestampNow.minus(1, ChronoUnit.DAYS);
            long timestamp = timestamp24H.getEpochSecond() - Constants.EPOCH_BEGINNING / 1000;
            pstmt = con.prepareStatement(sqlTransfer);
            pstmt.setLong(1, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    transferCount24H = rs.getLong(1);
                    transferAmount24H = rs.getLong(2);
                }
            }
            pstmt = con.prepareStatement(sqlStorage);
            pstmt.setLong(1, 0);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    storageCount = rs.getLong(1);
                    storageDataLength = rs.getLong(2);
                }
            }
            pstmt = con.prepareStatement(sqlStorage);
            pstmt.setLong(1, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    storageCount24H = rs.getLong(1);
                    storageDataLength24H = rs.getLong(2);
                }
            }
            jsonObject.put("transferCount", transferCount);
            jsonObject.put("transferAmount", transferAmount);
            jsonObject.put("transferCount24H", transferCount24H);
            jsonObject.put("transferAmount24H", transferAmount24H);
            jsonObject.put("storageCount", storageCount);
            jsonObject.put("poolCount", getTransactionCountByType(con, 8));
            jsonObject.put("coinBaseCount", getTransactionCountByType(con, 9));
            jsonObject.put("storageDataLength", storageDataLength);
            jsonObject.put("storageCount24H", storageCount24H);
            jsonObject.put("storageDataLength24H", storageDataLength24H);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
            return jsonObject;
        }
    }


    /**
     * 更具交易类型获得交易总数
     *
     * @param con
     * @param type
     * @return
     * @throws SQLException
     */
    private long getTransactionCountByType(Connection con, int type) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM TRANSACTION WHERE TYPE = ?");
        ps.setInt(1, type);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getLong(1);
        }
        return 0L;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
