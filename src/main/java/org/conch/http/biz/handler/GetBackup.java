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

import org.conch.Conch;
import org.conch.db.*;
import org.conch.db.*;
import org.conch.http.APIServlet;
import org.conch.http.APITag;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.conch.http.JSONResponses.MISSING_TRANSACTION;
import static org.conch.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class GetBackup extends APIServlet.APIRequestHandler {

    public static final GetBackup instance = new GetBackup();

    private GetBackup() {
        super(new APITag[] {APITag.BIZ}, "txID");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        String transactionIdString = Convert.emptyToNull(req.getParameter("txID"));
        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }

        JSONObject jsonO = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        Connection con = null;
        String sql = "SELECT STORER_ID, BACKUP_TRANSACTION, HEIGHT FROM STORAGE_BACKUP WHERE STORE_TRANSACTION = ?";
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, transactionIdString);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JSONObject jsonObject = new JSONObject();
                    Long storerId = rs.getLong(1);
                    jsonObject.put("storerId", storerId);
                    jsonObject.put("storerAddress", Convert.rsAccount(storerId));
                    String transactionId = rs.getString(2);
                    jsonObject.put("backup_Tx", transactionId);
                    Transaction transaction = Conch.getBlockchain().getTransaction(Convert.parseUnsignedLong(transactionId));
                    if (transaction == null) {
                        transaction = Conch.getTransactionProcessor().getUnconfirmedTransaction(Long.getLong(transactionId));
                        if (transaction == null) {
                            return UNKNOWN_TRANSACTION;
                        }
                    }
                    jsonObject.put("backup_Tx_confirmations", Conch.getBlockchain().getHeight() - transaction.getHeight());
                    jsonArray.add(jsonObject);
                }
            }
        } catch (SQLException e) {
            Logger.logErrorMessage(e.getMessage());
            jsonO.put("error", e.toString());
        } finally {
            jsonO.put("backups", jsonArray);
            DbUtils.close(con);
            return jsonO;
        }
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
