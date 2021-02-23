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
import org.conch.util.Listener;
import org.conch.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AssetDividend {

    public enum Event {
        ASSET_DIVIDEND
    }

    private static final Listeners<AssetDividend,Event> listeners = new Listeners<>();

    private static final DbKey.LongKeyFactory<AssetDividend> dividendDbKeyFactory = new DbKey.LongKeyFactory<AssetDividend>("id") {

        @Override
        public DbKey newKey(AssetDividend assetDividend) {
            return assetDividend.dbKey;
        }

    };

    private static final EntityDbTable<AssetDividend> assetDividendTable = new EntityDbTable<AssetDividend>("asset_dividend", dividendDbKeyFactory) {

        @Override
        protected AssetDividend load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AssetDividend(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AssetDividend assetDividend) throws SQLException {
            assetDividend.save(con);
        }

    };

    public static boolean addListener(Listener<AssetDividend> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<AssetDividend> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static DbIterator<AssetDividend> getAssetDividends(long assetId, int from, int to) {
        return assetDividendTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    public static AssetDividend getLastDividend(long assetId) {
        DbIterator<AssetDividend> dividends = null;
        try {
            dividends = assetDividendTable.getManyBy(new DbClause.LongClause("asset_id", assetId), 0, 0);
            if (dividends.hasNext()) {
                return dividends.next();
            }
        }finally {
            DbUtils.close(dividends);
        }
        return null;
    }

    public static AssetDividend addAssetDividend(long transactionId, Attachment.ColoredCoinsDividendPayment attachment,
                                          long totalDividend, long numAccounts) {
        AssetDividend assetDividend = new AssetDividend(transactionId, attachment, totalDividend, numAccounts);
        assetDividendTable.insert(assetDividend);
        listeners.notify(assetDividend, Event.ASSET_DIVIDEND);
        return assetDividend;
    }

    public static void init() {}


    private final long id;
    private final DbKey dbKey;
    private final long assetId;
    private final long amountNQTPerQNT;
    private final int dividendHeight;
    private final long totalDividend;
    private final long numAccounts;
    private final int timestamp;
    private final int height;

    private AssetDividend(long transactionId, Attachment.ColoredCoinsDividendPayment attachment,
                          long totalDividend, long numAccounts) {
        this.id = transactionId;
        this.dbKey = dividendDbKeyFactory.newKey(this.id);
        this.assetId = attachment.getAssetId();
        this.amountNQTPerQNT = attachment.getAmountNQTPerQNT();
        this.dividendHeight = attachment.getHeight();
        this.totalDividend = totalDividend;
        this.numAccounts = numAccounts;
        this.timestamp = Conch.getBlockchain().getLastBlockTimestamp();
        this.height = Conch.getBlockchain().getHeight();
    }

    private AssetDividend(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.assetId = rs.getLong("asset_id");
        this.amountNQTPerQNT = rs.getLong("amount");
        this.dividendHeight = rs.getInt("dividend_height");
        this.totalDividend = rs.getLong("total_dividend");
        this.numAccounts = rs.getLong("num_accounts");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset_dividend (id, asset_id, "
                + "amount, dividend_height, total_dividend, num_accounts, timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.amountNQTPerQNT);
            pstmt.setInt(++i, this.dividendHeight);
            pstmt.setLong(++i, this.totalDividend);
            pstmt.setLong(++i, this.numAccounts);
            pstmt.setInt(++i, this.timestamp);
            pstmt.setInt(++i, this.height);
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getAmountNQTPerQNT() {
        return amountNQTPerQNT;
    }

    public int getDividendHeight() {
        return dividendHeight;
    }

    public long getTotalDividend() {
        return totalDividend;
    }

    public long getNumAccounts() {
        return numAccounts;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getHeight() {
        return height;
    }

}
