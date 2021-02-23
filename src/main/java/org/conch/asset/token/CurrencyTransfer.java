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

package org.conch.asset.token;

import org.conch.Conch;
import org.conch.db.*;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.util.Listener;
import org.conch.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrencyTransfer {

    public enum Event {
        TRANSFER
    }

    private static final Listeners<CurrencyTransfer,Event> listeners = new Listeners<>();

    private static final DbKey.LongKeyFactory<CurrencyTransfer> currencyTransferDbKeyFactory = new DbKey.LongKeyFactory<CurrencyTransfer>("id") {

        @Override
        public DbKey newKey(CurrencyTransfer transfer) {
            return transfer.dbKey;
        }

    };

    private static final EntityDbTable<CurrencyTransfer> currencyTransferTable = new EntityDbTable<CurrencyTransfer>("currency_transfer", currencyTransferDbKeyFactory) {

        @Override
        protected CurrencyTransfer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new CurrencyTransfer(rs, dbKey);
        }

        @Override
        protected void save(Connection con, CurrencyTransfer transfer) throws SQLException {
            transfer.save(con);
        }

    };

    public static DbIterator<CurrencyTransfer> getAllTransfers(int from, int to) {
        return currencyTransferTable.getAll(from, to);
    }

    public static int getCount() {
        return currencyTransferTable.getCount();
    }

    public static boolean addListener(Listener<CurrencyTransfer> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<CurrencyTransfer> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static DbIterator<CurrencyTransfer> getCurrencyTransfers(long currencyId, int from, int to) {
        return currencyTransferTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    public static DbIterator<CurrencyTransfer> getAccountCurrencyTransfers(long accountId, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM currency_transfer WHERE sender_id = ?"
                    + " UNION ALL SELECT * FROM currency_transfer WHERE recipient_id = ? AND sender_id <> ? ORDER BY height DESC, db_id DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return currencyTransferTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<CurrencyTransfer> getAccountCurrencyTransfers(long accountId, long currencyId, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM currency_transfer WHERE sender_id = ? AND currency_id = ?"
                    + " UNION ALL SELECT * FROM currency_transfer WHERE recipient_id = ? AND sender_id <> ? AND currency_id = ? ORDER BY height DESC, db_id DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, currencyId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, currencyId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return currencyTransferTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static int getTransferCount(long currencyId) {
        return currencyTransferTable.getCount(new DbClause.LongClause("currency_id", currencyId));
    }

    public static CurrencyTransfer addTransfer(Transaction transaction, Attachment.MonetarySystemCurrencyTransfer attachment) {
        CurrencyTransfer transfer = new CurrencyTransfer(transaction, attachment);
        currencyTransferTable.insert(transfer);
        listeners.notify(transfer, Event.TRANSFER);
        return transfer;
    }

    public static void init() {}


    private final long id;
    private final DbKey dbKey;
    private final long currencyId;
    private final int height;
    private final long senderId;
    private final long recipientId;
    private final long units;
    private final int timestamp;

    private CurrencyTransfer(Transaction transaction, Attachment.MonetarySystemCurrencyTransfer attachment) {
        this.id = transaction.getId();
        this.dbKey = currencyTransferDbKeyFactory.newKey(this.id);
        this.height = Conch.getBlockchain().getHeight();
        this.currencyId = attachment.getCurrencyId();
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.units = attachment.getUnits();
        this.timestamp = Conch.getBlockchain().getLastBlockTimestamp();
    }

    private CurrencyTransfer(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.currencyId = rs.getLong("currency_id");
        this.senderId = rs.getLong("sender_id");
        this.recipientId = rs.getLong("recipient_id");
        this.units = rs.getLong("units");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_transfer (id, currency_id, "
                + "sender_id, recipient_id, units, timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.currencyId);
            pstmt.setLong(++i, this.senderId);
            pstmt.setLong(++i, this.recipientId);
            pstmt.setLong(++i, this.units);
            pstmt.setInt(++i, this.timestamp);
            pstmt.setInt(++i, this.height);
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getCurrencyId() { return currencyId; }

    public long getSenderId() {
        return senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public long getUnits() { return units; }

    public int getTimestamp() {
        return timestamp;
    }

    public int getHeight() {
        return height;
    }

}
