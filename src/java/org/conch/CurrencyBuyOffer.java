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
import org.conch.db.VersionedEntityDbTable;
import org.conch.db.DbIterator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CurrencyBuyOffer extends CurrencyExchangeOffer {

    private static final DbKey.LongKeyFactory<CurrencyBuyOffer> buyOfferDbKeyFactory = new DbKey.LongKeyFactory<CurrencyBuyOffer>("id") {

        @Override
        public DbKey newKey(CurrencyBuyOffer offer) {
            return offer.dbKey;
        }

    };

    private static final VersionedEntityDbTable<CurrencyBuyOffer> buyOfferTable = new VersionedEntityDbTable<CurrencyBuyOffer>("buy_offer", buyOfferDbKeyFactory) {

        @Override
        protected CurrencyBuyOffer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new CurrencyBuyOffer(rs, dbKey);
        }

        @Override
        protected void save(Connection con, CurrencyBuyOffer buy) throws SQLException {
            buy.save(con, table);
        }

    };

    public static int getCount() {
        return buyOfferTable.getCount();
    }

    public static CurrencyBuyOffer getOffer(long offerId) {
        return buyOfferTable.get(buyOfferDbKeyFactory.newKey(offerId));
    }

    public static DbIterator<CurrencyBuyOffer> getAll(int from, int to) {
        return buyOfferTable.getAll(from, to);
    }

    public static DbIterator<CurrencyBuyOffer> getOffers(Currency currency, int from, int to) {
        return getCurrencyOffers(currency.getId(), false, from, to);
    }

    public static DbIterator<CurrencyBuyOffer> getCurrencyOffers(long currencyId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("currency_id", currencyId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return buyOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    public static DbIterator<CurrencyBuyOffer> getAccountOffers(long accountId, boolean availableOnly, int from, int to) {
        DbClause dbClause = new DbClause.LongClause("account_id", accountId);
        if (availableOnly) {
            dbClause = dbClause.and(availableOnlyDbClause);
        }
        return buyOfferTable.getManyBy(dbClause, from, to, " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
    }

    public static CurrencyBuyOffer getOffer(Currency currency, Account account) {
        return getOffer(currency.getId(), account.getId());
    }

    public static CurrencyBuyOffer getOffer(final long currencyId, final long accountId) {
        return buyOfferTable.getBy(new DbClause.LongClause("currency_id", currencyId).and(new DbClause.LongClause("account_id", accountId)));
    }

    public static DbIterator<CurrencyBuyOffer> getOffers(DbClause dbClause, int from, int to) {
        return buyOfferTable.getManyBy(dbClause, from, to);
    }

    public static DbIterator<CurrencyBuyOffer> getOffers(DbClause dbClause, int from, int to, String sort) {
        return buyOfferTable.getManyBy(dbClause, from, to, sort);
    }

    static void addOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        buyOfferTable.insert(new CurrencyBuyOffer(transaction, attachment));
    }

    static void remove(CurrencyBuyOffer buyOffer) {
        buyOfferTable.delete(buyOffer);
    }

    static void init() {}

    private final DbKey dbKey;

    private CurrencyBuyOffer(Transaction transaction, Attachment.MonetarySystemPublishExchangeOffer attachment) {
        super(transaction.getId(), attachment.getCurrencyId(), transaction.getSenderId(), attachment.getBuyRateNQT(),
                attachment.getTotalBuyLimit(), attachment.getInitialBuySupply(), attachment.getExpirationHeight(), transaction.getHeight(),
                transaction.getIndex());
        this.dbKey = buyOfferDbKeyFactory.newKey(id);
    }

    private CurrencyBuyOffer(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.dbKey = dbKey;
    }

    @Override
    public CurrencySellOffer getCounterOffer() {
        return CurrencySellOffer.getOffer(id);
    }

    long increaseSupply(long delta) {
        long excess = super.increaseSupply(delta);
        buyOfferTable.insert(this);
        return excess;
    }

    void decreaseLimitAndSupply(long delta) {
        super.decreaseLimitAndSupply(delta);
        buyOfferTable.insert(this);
    }

}
