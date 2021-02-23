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
import org.conch.db.*;
import org.conch.db.*;
import org.conch.db.VersionedEntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Each CurrencyFounder instance represents a single founder contribution for a non issued currency
 * Once the currency is issued all founder contributions are removed
 * In case the currency is not issued because of insufficient funding, all funds are returned to the founders
 */
public class CurrencyFounder {

    private static final DbKey.LinkKeyFactory<CurrencyFounder> currencyFounderDbKeyFactory = new DbKey.LinkKeyFactory<CurrencyFounder>("currency_id", "account_id") {

        @Override
        public DbKey newKey(CurrencyFounder currencyFounder) {
            return currencyFounder.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencyFounder> currencyFounderTable = new VersionedEntityDbTable<CurrencyFounder>("currency_founder", currencyFounderDbKeyFactory) {

        @Override
        protected CurrencyFounder load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new CurrencyFounder(rs, dbKey);
        }

        @Override
        protected void save(Connection con, CurrencyFounder currencyFounder) throws SQLException {
            currencyFounder.save(con);
        }

        @Override
        public String defaultSort() {
            return " ORDER BY height DESC ";
        }

    };

    public static void init() {}

    private final DbKey dbKey;
    private final long currencyId;
    private final long accountId;
    private long amountPerUnitNQT;

    private CurrencyFounder(long currencyId, long accountId, long amountPerUnitNQT) {
        this.currencyId = currencyId;
        this.dbKey = currencyFounderDbKeyFactory.newKey(currencyId, accountId);
        this.accountId = accountId;
        this.amountPerUnitNQT = amountPerUnitNQT;
    }

    private CurrencyFounder(ResultSet rs, DbKey dbKey) throws SQLException {
        this.currencyId = rs.getLong("currency_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = dbKey;
        this.amountPerUnitNQT = rs.getLong("amount");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO currency_founder (currency_id, account_id, amount, height, latest) "
                + "KEY (currency_id, account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.getCurrencyId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setLong(++i, this.getAmountPerUnitNQT());
            pstmt.setInt(++i, Conch.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getCurrencyId() {
        return currencyId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getAmountPerUnitNQT() {
        return amountPerUnitNQT;
    }

    static void addOrUpdateFounder(long currencyId, long accountId, long amount) {
        CurrencyFounder founder = getFounder(currencyId, accountId);
        if (founder == null) {
            founder = new CurrencyFounder(currencyId, accountId, amount);
        } else {
            founder.amountPerUnitNQT += amount;
        }
        currencyFounderTable.insert(founder);
    }

    public static CurrencyFounder getFounder(long currencyId, long accountId) {
        return currencyFounderTable.get(currencyFounderDbKeyFactory.newKey(currencyId, accountId));
    }

    public static DbIterator<CurrencyFounder> getCurrencyFounders(long currencyId, int from, int to) {
        return currencyFounderTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    public static DbIterator<CurrencyFounder> getFounderCurrencies(long accountId, int from, int to) {
        return currencyFounderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    static void remove(long currencyId) {
        List<CurrencyFounder> founders = new ArrayList<>();

        DbIterator<CurrencyFounder> currencyFounders = null;
        try {
            currencyFounders = CurrencyFounder.getCurrencyFounders(currencyId, 0, Integer.MAX_VALUE);
            for (CurrencyFounder founder : currencyFounders) {
                founders.add(founder);
            }
        }finally {
            DbUtils.close(currencyFounders);
        }
        founders.forEach(currencyFounderTable::delete);
    }
}
