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

import org.conch.account.Account;
import org.conch.db.DbClause;
import org.conch.db.DbIterator;
import org.conch.db.DbKey;
import org.conch.db.VersionedEntityDbTable;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrencySellOffer extends CurrencyExchangeOffer {

    private static final DbKey.LongKeyFactory<CurrencySellOffer> sellOfferDbKeyFactory = new DbKey.LongKeyFactory<CurrencySellOffer>("id") {

        @Override
        public DbKey newKey(CurrencySellOffer sell) {
            return sell.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencySellOffer> sellOfferTable = new VersionedEntityDbTable<CurrencySellOffer>("sell_offer", sellOfferDbKeyFactory) {

        @Override
        protected CurrencySellOffer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new CurrencySellOffer(rs, dbKey);
        }

        @Override
        protected void save(Connection con, CurrencySellOffer sell) throws SQLException {
            sell.save(con, table);
        }

    };

    public static int getCount() {
        return sellOfferTable.getCount();
    }

    public static CurrencySellOffer getOffer(long id) {
        return sellOfferTable.get(sellOfferDbKeyFactory.newKey(id));
    }

    public static DbIterator<CurrencySellOffer> getAll(int from, int to) {
        return sellOfferTable.getAll(from, to);
    }

    public static DbIterator<CurrencySellOffer> getOffers(Currency currency, int from, int to) {
        return getCurrencyOffers(currency.getId(), false, from, to);
    }

    public static DbIterator<CurrencySellOffer> getCurrencyOffers(long currencyId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return sellOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    public static DbIterator<CurrencySellOffer> getAccountOffers(long accountId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return sellOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    public static CurrencySellOffer getOffer(Currency currency, Account account) {
        return getOffer(currency.getId(), account.getId());
    }

    public static CurrencySellOffer getOffer(final long currencyId, final long accountId) {
        return sellOfferTable.getBy(new DbClause.LongClause("currency_id", currencyId).and(new DbClause.LongClause("account_id", accountId)));
    }

    public static DbIterator<CurrencySellOffer> getOffers(DbClause dbClause, int from, int to) {
        return sellOfferTable.getManyBy(dbClause, from, to);
    }

    public static DbIterator<CurrencySellOffer> getOffers(DbClause dbClause, int from, int to, String sort) {
        return sellOfferTable.getManyBy(dbClause, from, to, sort);
    }

    public static void addOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        sellOfferTable.insert(new CurrencySellOffer(transaction, attachment));
    }

    public static void remove(CurrencySellOffer sellOffer) {
        sellOfferTable.delete(sellOffer);
    }

    public static void init() {}

    private final DbKey dbKey;

    private CurrencySellOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getSellRateNQT(),
                attachment.getTotalSellLimit(), attachment.getInitialSellSupply(), attachment.getExpirationHeight(), transaction.getHeight(),
                transaction.getIndex());
        this.dbKey = sellOfferDbKeyFactory.newKey(id);
    }

    private CurrencySellOffer(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.dbKey = dbKey;
    }

    @Override
    public CurrencyBuyOffer getCounterOffer() {
        return CurrencyBuyOffer.getOffer(id);
    }

    long increaseSupply(long delta) {
        long excess = super.increaseSupply(delta);
        sellOfferTable.insert(this);
        return excess;
    }

    void decreaseLimitAndSupply(long delta) {
        super.decreaseLimitAndSupply(delta);
        sellOfferTable.insert(this);
    }
}
