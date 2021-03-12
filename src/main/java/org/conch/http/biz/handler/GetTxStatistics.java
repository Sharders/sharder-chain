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

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Lists;
import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.reward.RewardCalculator;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.http.APIServlet;
import org.conch.http.APITag;
import org.conch.mint.Generator;
import org.conch.tx.TransactionType;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;


public final class GetTxStatistics extends APIServlet.APIRequestHandler {

    public static final GetTxStatistics instance = new GetTxStatistics();

    private GetTxStatistics() {
        super(new APITag[]{APITag.BIZ});
    }

    private static JSONObject jsonObject = new JSONObject();
    private static int height = 0;

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        // if curTime > lastBlockTime + gapTime => update jsonObject , else return
        if (height == Conch.getHeight() && Constants.GAP_SECONDS > Conch.getEpochTime() - Conch.getBlockchain().getLastBlockTimestamp()) {
            return jsonObject;
        }
        height = Conch.getHeight();
        Long transferCount = 0L;
        Long transferAmount = 0L;
        Long transferCount24H = 0L;
        Long transferAmount24H = 0L;
        Long storageCount = 0L;
        Long storageDataLength = 0L;
        Long storageCount24H = 0L;
        Long storageDataLength24H = 0L;
        Long declaredPeerSize24H = 0L;
        String capacity24H;
        String capacity;
        String capacityActive;
        List<Long> boundAccountList = Lists.newArrayList();

        Connection con = null;
        PreparedStatement pstmt = null;
        String sqlTransfer = "SELECT COUNT(*),SUM(AMOUNT)/100000000 FROM TRANSACTION WHERE VERSION>=3 AND TYPE=0 AND SUBTYPE=0 AND HEIGHT>0 AND TIMESTAMP > ?";
        String sqlDeclaredPeer = "SELECT COUNT(*) FROM TRANSACTION WHERE VERSION>=3 AND TYPE=12 AND SUBTYPE=0 AND HEIGHT>0 AND TIMESTAMP > ?";
        String sqlDeclaredPeerAccounts = "SELECT ACCOUNT_ID FROM certified_peer WHERE LATEST=true AND LAST_UPDATED > ?";
        String sqlStorage = "SELECT COUNT(*),SUM(LENGTH (t2.DATA)) FROM TRANSACTION t1 , TAGGED_DATA t2 WHERE t1.ID = t2.ID AND t1.VERSION>=3 AND t1.TYPE=6 AND t1.SUBTYPE=0 AND t1.HEIGHT>0 AND TIMESTAMP > ?";
        try {
            con = Db.db.getConnection();
            // transfer statistics
            pstmt = con.prepareStatement(sqlTransfer);
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
            // declaredPeer statistics
            pstmt = con.prepareStatement(sqlDeclaredPeer);
            pstmt.setLong(1, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    declaredPeerSize24H = rs.getLong(1);
                }
            }
            // storage statistics
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
            // capacity statistics
            pstmt = con.prepareStatement(sqlDeclaredPeerAccounts);
            pstmt.setLong(1, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        boundAccountList.add(rs.getLong("account_id"));
                    } catch (Exception e) {
                        // continue to fetch next
                        System.err.println(e);
                    }
                }
                capacity24H = RewardCalculator.crowdMinerHardwareCapacity(Conch.getHeight(), boundAccountList);
            }
            capacity = RewardCalculator.crowdMinerHardwareCapacity(Conch.getHeight(), null);
            capacityActive = Generator.hardwareCapacityActive();

            jsonObject.put("transferCount", transferCount);
            jsonObject.put("transferAmount", transferAmount);
            jsonObject.put("transferCount24H", transferCount24H);
            jsonObject.put("transferAmount24H", transferAmount24H);
            jsonObject.put("storageCount", storageCount);
            jsonObject.put("poolCount", Conch.getBlockchain().getTransactionCountByType(TransactionType.TYPE_SHARDER_POOL, con));
            jsonObject.put("coinBaseCount", Conch.getBlockchain().getTransactionCountByType(TransactionType.TYPE_COIN_BASE, con));
            jsonObject.put("storageDataLength", storageDataLength);
            jsonObject.put("storageCount24H", storageCount24H);
            jsonObject.put("storageDataLength24H", storageDataLength24H);
            jsonObject.put("declaredPeerSize", Conch.getPocProcessor().getCertifiedPeers().size());
            jsonObject.put("declaredPeerSize24H", declaredPeerSize24H);

            jsonObject.put("capacityActive", capacityActive);
            jsonObject.put("capacityTotal", capacity);
            jsonObject.put("capacity24H", capacity24H);

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con, pstmt);
            return jsonObject;
        }
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
