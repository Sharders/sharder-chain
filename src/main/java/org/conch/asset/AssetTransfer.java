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

package org.conch.asset;

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

public final class AssetTransfer {

    public enum Event {
        ASSET_TRANSFER
    }

    private static final Listeners<AssetTransfer,Event> listeners = new Listeners<>();

    private static final DbKey.LongKeyFactory<AssetTransfer> transferDbKeyFactory = new DbKey.LongKeyFactory<AssetTransfer>("id") {

        @Override
        public DbKey newKey(AssetTransfer assetTransfer) {
            return assetTransfer.dbKey;
        }

    };

    private static final EntityDbTable<AssetTransfer> assetTransferTable = new EntityDbTable<AssetTransfer>("asset_transfer", transferDbKeyFactory) {

        @Override
        protected AssetTransfer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AssetTransfer(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AssetTransfer assetTransfer) throws SQLException {
            assetTransfer.save(con);
        }

    };

    public static DbIterator<AssetTransfer> getAllTransfers(int from, int to) {
        return assetTransferTable.getAll(from, to);
    }

    public static int getCount() {
        return assetTransferTable.getCount();
    }

    public static boolean addListener(Listener<AssetTransfer> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<AssetTransfer> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static DbIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to) {
        return assetTransferTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    public static DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM asset_transfer WHERE sender_id = ?"
                    + " UNION ALL SELECT * FROM asset_transfer WHERE recipient_id = ? AND sender_id <> ? ORDER BY height DESC, db_id DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return assetTransferTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM asset_transfer WHERE sender_id = ? AND asset_id = ?"
                    + " UNION ALL SELECT * FROM asset_transfer WHERE recipient_id = ? AND sender_id <> ? AND asset_id = ? ORDER BY height DESC, db_id DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, assetId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, assetId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return assetTransferTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static int getTransferCount(long assetId) {
        return assetTransferTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

    public static AssetTransfer addAssetTransfer(Transaction transaction, Attachment.ColoredCoinsAssetTransfer attachment) {
        AssetTransfer assetTransfer = new AssetTransfer(transaction, attachment);
        assetTransferTable.insert(assetTransfer);
        listeners.notify(assetTransfer, Event.ASSET_TRANSFER);
        return assetTransfer;
    }

    public static void init() {}


    private final long id;
    private final DbKey dbKey;
    private final long assetId;
    private final int height;
    private final long senderId;
    private final long recipientId;
    private final long quantityQNT;
    private final int timestamp;

    private AssetTransfer(Transaction transaction, Attachment.ColoredCoinsAssetTransfer attachment) {
        this.id = transaction.getId();
        this.dbKey = transferDbKeyFactory.newKey(this.id);
        this.height = Conch.getBlockchain().getHeight();
        this.assetId = attachment.getAssetId();
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.quantityQNT = attachment.getQuantityQNT();
        this.timestamp = Conch.getBlockchain().getLastBlockTimestamp();
    }

    private AssetTransfer(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.assetId = rs.getLong("asset_id");
        this.senderId = rs.getLong("sender_id");
        this.recipientId = rs.getLong("recipient_id");
        this.quantityQNT = rs.getLong("quantity");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset_transfer (id, asset_id, "
                + "sender_id, recipient_id, quantity, timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.senderId);
            pstmt.setLong(++i, this.recipientId);
            pstmt.setLong(++i, this.quantityQNT);
            pstmt.setInt(++i, this.timestamp);
            pstmt.setInt(++i, this.height);
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getAssetId() { return assetId; }

    public long getSenderId() {
        return senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public long getQuantityQNT() { return quantityQNT; }

    public int getTimestamp() {
        return timestamp;
    }

    public int getHeight() {
        return height;
    }

}
