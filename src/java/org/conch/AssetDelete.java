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

package org.conch;

import org.conch.db.DbClause;
import org.conch.db.DbIterator;
import org.conch.db.DbKey;
import org.conch.db.EntityDbTable;
import org.conch.util.Listener;
import org.conch.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AssetDelete {

    public enum Event {
        ASSET_DELETE
    }

    private static final Listeners<AssetDelete,Event> listeners = new Listeners<>();

    private static final DbKey.LongKeyFactory<AssetDelete> deleteDbKeyFactory = new DbKey.LongKeyFactory<AssetDelete>("id") {

        @Override
        public DbKey newKey(AssetDelete assetDelete) {
            return assetDelete.dbKey;
        }

    };

    private static final EntityDbTable<AssetDelete> assetDeleteTable = new EntityDbTable<AssetDelete>("asset_delete", deleteDbKeyFactory) {

        @Override
        protected AssetDelete load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AssetDelete(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AssetDelete assetDelete) throws SQLException {
            assetDelete.save(con);
        }

    };

    public static boolean addListener(Listener<AssetDelete> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<AssetDelete> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static DbIterator<AssetDelete> getAssetDeletes(long assetId, int from, int to) {
        return assetDeleteTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    public static DbIterator<AssetDelete> getAccountAssetDeletes(long accountId, int from, int to) {
        return assetDeleteTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to, " ORDER BY height DESC, db_id DESC ");
    }

    public static DbIterator<AssetDelete> getAccountAssetDeletes(long accountId, long assetId, int from, int to) {
        return assetDeleteTable.getManyBy(new DbClause.LongClause("account_id", accountId).and(new DbClause.LongClause("asset_id", assetId)),
                from, to, " ORDER BY height DESC, db_id DESC ");
    }

    static AssetDelete addAssetDelete(Transaction transaction, long assetId, long quantityQNT) {
        AssetDelete assetDelete = new AssetDelete(transaction, assetId, quantityQNT);
        assetDeleteTable.insert(assetDelete);
        listeners.notify(assetDelete, Event.ASSET_DELETE);
        return assetDelete;
    }

    static void init() {}


    private final long id;
    private final DbKey dbKey;
    private final long assetId;
    private final int height;
    private final long accountId;
    private final long quantityQNT;
    private final int timestamp;

    private AssetDelete(Transaction transaction, long assetId, long quantityQNT) {
        this.id = transaction.getId();
        this.dbKey = deleteDbKeyFactory.newKey(this.id);
        this.assetId = assetId;
        this.accountId = transaction.getSenderId();
        this.quantityQNT = quantityQNT;
        this.timestamp = Conch.getBlockchain().getLastBlockTimestamp();
        this.height = Conch.getBlockchain().getHeight();
    }

    private AssetDelete(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.assetId = rs.getLong("asset_id");
        this.accountId = rs.getLong("account_id");
        this.quantityQNT = rs.getLong("quantity");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset_delete (id, asset_id, "
                + "account_id, quantity, timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.accountId);
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

    public long getAccountId() {
        return accountId;
    }

    public long getQuantityQNT() { return quantityQNT; }

    public int getTimestamp() {
        return timestamp;
    }

    public int getHeight() {
        return height;
    }

}
