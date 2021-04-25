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

package org.conch.account;

import com.google.common.collect.Sets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.conch.Conch;
import org.conch.asset.AssetDividend;
import org.conch.asset.AssetTransfer;
import org.conch.asset.token.CurrencyTransfer;
import org.conch.chain.BlockchainProcessor;
import org.conch.chain.CheckSumValidator;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.crypto.Crypto;
import org.conch.crypto.EncryptedData;
import org.conch.db.Db;
import org.conch.db.DbClause;
import org.conch.db.DbIterator;
import org.conch.db.DbKey;
import org.conch.db.DbTrimUtils;
import org.conch.db.DbUtils;
import org.conch.db.DerivedDbTable;
import org.conch.db.VersionedEntityDbTable;
import org.conch.db.VersionedPersistentDbTable;
import org.conch.market.Exchange;
import org.conch.market.Trade;
import org.conch.shuffle.ShufflingTransaction;
import org.conch.tx.Appendix;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.conch.util.Listener;
import org.conch.util.Listeners;
import org.conch.util.Logger;

@SuppressWarnings({"UnusedDeclaration", "SuspiciousNameCombination"})
public final class Account {

    public enum Event {
        BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE, CURRENCY_BALANCE, UNCONFIRMED_CURRENCY_BALANCE,
        LEASE_SCHEDULED, LEASE_STARTED, LEASE_ENDED, SET_PROPERTY, DELETE_PROPERTY, POC
    }

    public enum ControlType {
        PHASING_ONLY
    }

    public static final class AccountAsset {

        private final long accountId;
        private final long assetId;
        private final DbKey dbKey;
        private long quantityQNT;
        private long unconfirmedQuantityQNT;

        private AccountAsset(long accountId, long assetId, long quantityQNT, long unconfirmedQuantityQNT) {
            this.accountId = accountId;
            this.assetId = assetId;
            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.assetId);
            this.quantityQNT = quantityQNT;
            this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
        }

        private AccountAsset(ResultSet rs, DbKey dbKey) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.assetId = rs.getLong("asset_id");
            this.dbKey = dbKey;
            this.quantityQNT = rs.getLong("quantity");
            this.unconfirmedQuantityQNT = rs.getLong("unconfirmed_quantity");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset "
                    + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) "
                    + "KEY (account_id, asset_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                pstmt.setLong(++i, this.assetId);
                pstmt.setLong(++i, this.quantityQNT);
                pstmt.setLong(++i, this.unconfirmedQuantityQNT);
                pstmt.setInt(++i, Conch.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public long getUnconfirmedQuantityQNT() {
            return unconfirmedQuantityQNT;
        }

        private void save() {
            checkBalance(this.accountId, this.quantityQNT, this.unconfirmedQuantityQNT);
            if (this.quantityQNT > 0 || this.unconfirmedQuantityQNT > 0) {
                accountAssetTable.insert(this);
            } else {
                accountAssetTable.delete(this);
            }
        }

        @Override
        public String toString() {
            return "AccountAsset account_id: " + Long.toUnsignedString(accountId) + " asset_id: " + Long.toUnsignedString(assetId)
                    + " quantity: " + quantityQNT + " unconfirmedQuantity: " + unconfirmedQuantityQNT;
        }

    }

    @SuppressWarnings("UnusedDeclaration")
    public static final class AccountCurrency {

        private final long accountId;
        private final long currencyId;
        private final DbKey dbKey;
        private long units;
        private long unconfirmedUnits;

        private AccountCurrency(long accountId, long currencyId, long quantityQNT, long unconfirmedQuantityQNT) {
            this.accountId = accountId;
            this.currencyId = currencyId;
            this.dbKey = accountCurrencyDbKeyFactory.newKey(this.accountId, this.currencyId);
            this.units = quantityQNT;
            this.unconfirmedUnits = unconfirmedQuantityQNT;
        }

        private AccountCurrency(ResultSet rs, DbKey dbKey) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.currencyId = rs.getLong("currency_id");
            this.dbKey = dbKey;
            this.units = rs.getLong("units");
            this.unconfirmedUnits = rs.getLong("unconfirmed_units");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_currency "
                    + "(account_id, currency_id, units, unconfirmed_units, height, latest) "
                    + "KEY (account_id, currency_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                pstmt.setLong(++i, this.currencyId);
                pstmt.setLong(++i, this.units);
                pstmt.setLong(++i, this.unconfirmedUnits);
                pstmt.setInt(++i, Conch.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

        public long getUnconfirmedUnits() {
            return unconfirmedUnits;
        }

        private void save() {
            checkBalance(this.accountId, this.units, this.unconfirmedUnits);
            if (this.units > 0 || this.unconfirmedUnits > 0) {
                accountCurrencyTable.insert(this);
            } else if (this.units == 0 && this.unconfirmedUnits == 0) {
                accountCurrencyTable.delete(this);
            }
        }

        @Override
        public String toString() {
            return "AccountCurrency account_id: " + Long.toUnsignedString(accountId) + " currency_id: " + Long.toUnsignedString(currencyId)
                    + " quantity: " + units + " unconfirmedQuantity: " + unconfirmedUnits;
        }

    }

    public static final class AccountLease {

        private final long lessorId;
        private final DbKey dbKey;
        private long currentLesseeId;
        private int currentLeasingHeightFrom;
        private int currentLeasingHeightTo;
        private long nextLesseeId;
        private int nextLeasingHeightFrom;
        private int nextLeasingHeightTo;

        private AccountLease(long lessorId,
                             int currentLeasingHeightFrom, int currentLeasingHeightTo, long currentLesseeId) {
            this.lessorId = lessorId;
            this.dbKey = accountLeaseDbKeyFactory.newKey(this.lessorId);
            this.currentLeasingHeightFrom = currentLeasingHeightFrom;
            this.currentLeasingHeightTo = currentLeasingHeightTo;
            this.currentLesseeId = currentLesseeId;
        }

        private AccountLease(ResultSet rs, DbKey dbKey) throws SQLException {
            this.lessorId = rs.getLong("lessor_id");
            this.dbKey = dbKey;
            this.currentLeasingHeightFrom = rs.getInt("current_leasing_height_from");
            this.currentLeasingHeightTo = rs.getInt("current_leasing_height_to");
            this.currentLesseeId = rs.getLong("current_lessee_id");
            this.nextLeasingHeightFrom = rs.getInt("next_leasing_height_from");
            this.nextLeasingHeightTo = rs.getInt("next_leasing_height_to");
            this.nextLesseeId = rs.getLong("next_lessee_id");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_lease "
                    + "(lessor_id, current_leasing_height_from, current_leasing_height_to, current_lessee_id, "
                    + "next_leasing_height_from, next_leasing_height_to, next_lessee_id, height, latest) "
                    + "KEY (lessor_id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.lessorId);
                DbUtils.setIntZeroToNull(pstmt, ++i, this.currentLeasingHeightFrom);
                DbUtils.setIntZeroToNull(pstmt, ++i, this.currentLeasingHeightTo);
                DbUtils.setLongZeroToNull(pstmt, ++i, this.currentLesseeId);
                DbUtils.setIntZeroToNull(pstmt, ++i, this.nextLeasingHeightFrom);
                DbUtils.setIntZeroToNull(pstmt, ++i, this.nextLeasingHeightTo);
                DbUtils.setLongZeroToNull(pstmt, ++i, this.nextLesseeId);
                pstmt.setInt(++i, Conch.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getLessorId() {
            return lessorId;
        }

        public long getCurrentLesseeId() {
            return currentLesseeId;
        }

        public int getCurrentLeasingHeightFrom() {
            return currentLeasingHeightFrom;
        }

        public int getCurrentLeasingHeightTo() {
            return currentLeasingHeightTo;
        }

        public long getNextLesseeId() {
            return nextLesseeId;
        }

        public int getNextLeasingHeightFrom() {
            return nextLeasingHeightFrom;
        }

        public int getNextLeasingHeightTo() {
            return nextLeasingHeightTo;
        }

    }

    public static final class AccountInfo {

        private final long accountId;
        private final DbKey dbKey;
        private String name;
        private String description;

        private AccountInfo(long accountId, String name, String description) {
            this.accountId = accountId;
            this.dbKey = accountInfoDbKeyFactory.newKey(this.accountId);
            this.name = name;
            this.description = description;
        }

        private AccountInfo(ResultSet rs, DbKey dbKey) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.dbKey = dbKey;
            this.name = rs.getString("name");
            this.description = rs.getString("description");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_info "
                    + "(account_id, name, description, height, latest) "
                    + "KEY (account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.accountId);
                DbUtils.setString(pstmt, ++i, this.name);
                DbUtils.setString(pstmt, ++i, this.description);
                pstmt.setInt(++i, Conch.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        private void save() {
            if (this.name != null || this.description != null) {
                accountInfoTable.insert(this);
            } else {
                accountInfoTable.delete(this);
            }
        }

    }

    public static final class AccountProperty {

        private final long id;
        private final DbKey dbKey;
        private final long recipientId;
        private final long setterId;
        private String property;
        private String value;

        private AccountProperty(long id, long recipientId, long setterId, String property, String value) {
            this.id = id;
            this.dbKey = accountPropertyDbKeyFactory.newKey(this.id);
            this.recipientId = recipientId;
            this.setterId = setterId;
            this.property = property;
            this.value = value;
        }

        private AccountProperty(ResultSet rs, DbKey dbKey) throws SQLException {
            this.id = rs.getLong("id");
            this.dbKey = dbKey;
            this.recipientId = rs.getLong("recipient_id");
            long setterId = rs.getLong("setter_id");
            this.setterId = setterId == 0 ? recipientId : setterId;
            this.property = rs.getString("property");
            this.value = rs.getString("value");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_property "
                    + "(id, recipient_id, setter_id, property, value, height, latest) "
                    + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.id);
                pstmt.setLong(++i, this.recipientId);
                DbUtils.setLongZeroToNull(pstmt, ++i, this.setterId != this.recipientId ? this.setterId : 0);
                DbUtils.setString(pstmt, ++i, this.property);
                DbUtils.setString(pstmt, ++i, this.value);
                pstmt.setInt(++i, Conch.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public long getId() {
            return id;
        }

        public long getRecipientId() {
            return recipientId;
        }

        public long getSetterId() {
            return setterId;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }

    }

    public static final class PublicKey {

        private final long accountId;
        private final DbKey dbKey;
        private byte[] publicKey;
        private int height;

        private PublicKey(long accountId, byte[] publicKey) {
            this.accountId = accountId;
            this.dbKey = publicKeyDbKeyFactory.newKey(accountId);
            this.publicKey = publicKey;
            this.height = Conch.getBlockchain().getHeight();
        }

        private PublicKey(ResultSet rs, DbKey dbKey) throws SQLException {
            this.accountId = rs.getLong("account_id");
            this.dbKey = dbKey;
            this.publicKey = rs.getBytes("public_key");
            this.height = rs.getInt("height");
        }

        private void save(Connection con) throws SQLException {
            height = Conch.getBlockchain().getHeight();
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO public_key (account_id, public_key, height, latest) "
                    + "KEY (account_id, height) VALUES (?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, accountId);
                DbUtils.setBytes(pstmt, ++i, publicKey);
                pstmt.setInt(++i, height);
                pstmt.executeUpdate();
            }
        }

        public long getAccountId() {
            return accountId;
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

        public int getHeight() {
            return height;
        }

    }

    public static class DoubleSpendingException extends RuntimeException {
        DoubleSpendingException(String message, long accountId, long confirmed, long unconfirmed) {
            super(message + " account: " +  Account.rsAccount(accountId) + "[id=" + accountId + "], confirmed: " + confirmed + ", unconfirmed: " + unconfirmed + ", current height:" + Conch.getHeight());
        }

    }

    private static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {

        @Override
        public DbKey newKey(Account account) {
            return account.dbKey == null ? newKey(account.id) : account.dbKey;
        }

        @Override
        public Account newEntity(DbKey dbKey) {
            return new Account(((DbKey.LongKey)dbKey).getId());
        }

    };

    private static final VersionedEntityDbTable<Account> accountTable = new VersionedEntityDbTable<Account>("account", accountDbKeyFactory) {

        @Override
        protected Account load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Account(rs, dbKey);
        }

        @Override
        protected void save(Connection con, Account account) throws SQLException {
            account.save(con);
        }

        @Override
        public void trim(int height) {
            if(Constants.SYNC_BUTTON) {
               return;
            }
            _trim("account",height);
        }

        @Override
        public void rollback(int height) {
            if (!db.isInTransaction()) {
                throw new IllegalStateException("Not in transaction");
            }
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmtSelect = con.prepareStatement("select distinct id FROM account WHERE height > ?");
                pstmtSelect.setInt(1, height);
                ResultSet resultSet = pstmtSelect.executeQuery();
                try (PreparedStatement pstmtWorkDelete = con.prepareStatement("DELETE FROM account WHERE height > ?");
                     PreparedStatement pstmtCacheDelete = con.prepareStatement("DELETE FROM account_cache WHERE height > ?");
                     PreparedStatement pstmtHistoryDelete = con.prepareStatement("DELETE FROM account_history WHERE height > ?");
                ) {
                    pstmtWorkDelete.setInt(1, height);
                    pstmtWorkDelete.executeUpdate();
                    pstmtCacheDelete.setInt(1, height);
                    pstmtCacheDelete.executeUpdate();
                    pstmtHistoryDelete.setInt(1, height);
                    pstmtHistoryDelete.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
                while (resultSet.next()) {
                    long accountId = resultSet.getLong("id");
                    PreparedStatement workTable = con.prepareStatement("select * FROM account WHERE id = ? order by height desc limit 1");
                    workTable.setLong(1, accountId);
                    ResultSet resultSetWork = workTable.executeQuery();
                    if (resultSetWork.next()) {
                        PreparedStatement update = con.prepareStatement("update account set latest = true where db_id = ?");
                        update.setLong(1,resultSetWork.getLong("db_id"));
                        update.executeUpdate();
                        continue;
                    }
                    PreparedStatement cacheTable = con.prepareStatement("select * FROM account_cache WHERE id = ? order by height desc limit 1");
                    cacheTable.setLong(1, accountId);
                    ResultSet resultSetCache = cacheTable.executeQuery();
                    if (!resultSetCache.next()) {
                        PreparedStatement historyTable = con.prepareStatement("select * FROM account_history WHERE id" +
                         " = ? order by height desc limit 1");
                        historyTable.setLong(1, accountId);
                        resultSetCache = historyTable.executeQuery();
                        if (!resultSetCache.next()) {
                            continue;
                        }
                    }
                    PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account (ID,BALANCE," +
                            "UNCONFIRMED_BALANCE,FORGED_BALANCE," +
                            "ACTIVE_LESSEE_ID,HAS_CONTROL_PHASING,HEIGHT,FROZEN_BALANCE) VALUES(?, ?, ?, ?, ?, ?, ?, " +
                            "?)");
                    pstmtInsert.setLong(1, resultSetCache.getLong("ID"));
                    pstmtInsert.setLong(2, resultSetCache.getLong("BALANCE"));
                    pstmtInsert.setLong(3, resultSetCache.getLong("UNCONFIRMED_BALANCE"));
                    pstmtInsert.setLong(4, resultSetCache.getLong("FORGED_BALANCE"));
                    pstmtInsert.setLong(5, resultSetCache.getLong("ACTIVE_LESSEE_ID"));
                    pstmtInsert.setBoolean(6, resultSetCache.getBoolean("HAS_CONTROL_PHASING"));
                    pstmtInsert.setInt(7, resultSetCache.getInt("HEIGHT"));
                    pstmtInsert.setLong(8, resultSetCache.getLong("FROZEN_BALANCE"));
                    pstmtInsert.executeUpdate();
                }

            }catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    };

    private static final VersionedEntityDbTable<Account> accountCacheTable = new VersionedEntityDbTable<Account>("account_cache", accountDbKeyFactory) {

        @Override
        protected Account load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Account(rs, dbKey);
        }

        @Override
        protected void save(Connection con, Account account) throws SQLException {
            account.save(con);
        }

        @Override
        public void trim(int height) {
            if(Constants.SYNC_BUTTON) {
                return;
            }
            _trim("account_cache", height, false);
        }

        @Override
        public void rollback(int height) {
            //super.rollback(height);
        }

        @Override
        public void truncate() {}

    };

    private static final VersionedEntityDbTable<Account> accountHistoryTable = new VersionedEntityDbTable<Account>("account_history", accountDbKeyFactory) {

        @Override
        protected Account load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Account(rs, dbKey);
        }

        @Override
        protected void save(Connection con, Account account) throws SQLException {
            account.save(con);
        }

        @Override
        public void trim(int height) {
            if(Constants.SYNC_BUTTON) {
                return;
            }
            _trim("account_history", height, false);
        }

        @Override
        public void rollback(int height) {
            //super.rollback(height);
        }

        @Override
        public void truncate() {
        }

    };

    private static final DbKey.LongKeyFactory<AccountInfo> accountInfoDbKeyFactory = new DbKey.LongKeyFactory<AccountInfo>("account_id") {

        @Override
        public DbKey newKey(AccountInfo accountInfo) {
            return accountInfo.dbKey;
        }

    };

    private static final DbKey.LongKeyFactory<AccountLease> accountLeaseDbKeyFactory = new DbKey.LongKeyFactory<AccountLease>("lessor_id") {

        @Override
        public DbKey newKey(AccountLease accountLease) {
            return accountLease.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountLease> accountLeaseTable = new VersionedEntityDbTable<AccountLease>("account_lease",
            accountLeaseDbKeyFactory) {

        @Override
        protected AccountLease load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountLease(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountLease accountLease) throws SQLException {
            accountLease.save(con);
        }

        @Override
        public void trim(int height) {
            _trim("account_lease", height);
        }

    };

    private static final VersionedEntityDbTable<AccountInfo> accountInfoTable = new VersionedEntityDbTable<AccountInfo>("account_info",
            accountInfoDbKeyFactory, "name,description") {

        @Override
        protected AccountInfo load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountInfo(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountInfo accountInfo) throws SQLException {
            accountInfo.save(con);
        }

        @Override
        public void trim(int height) {
            _trim("account_info",height);
        }

    };

    private static final DbKey.LongKeyFactory<PublicKey> publicKeyDbKeyFactory = new DbKey.LongKeyFactory<PublicKey>("account_id") {

        @Override
        public DbKey newKey(PublicKey publicKey) {
            return publicKey.dbKey;
        }

        @Override
        public PublicKey newEntity(DbKey dbKey) {
            return new PublicKey(((DbKey.LongKey)dbKey).getId(), null);
        }

    };

    private static final VersionedPersistentDbTable<PublicKey> publicKeyTable = new VersionedPersistentDbTable<PublicKey>("public_key", publicKeyDbKeyFactory) {

        @Override
        protected PublicKey load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new PublicKey(rs, dbKey);
        }

        @Override
        protected void save(Connection con, PublicKey publicKey) throws SQLException {
            publicKey.save(con);
        }

    };

    private static final DbKey.LinkKeyFactory<AccountAsset> accountAssetDbKeyFactory = new DbKey.LinkKeyFactory<AccountAsset>("account_id", "asset_id") {

        @Override
        public DbKey newKey(AccountAsset accountAsset) {
            return accountAsset.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountAsset> accountAssetTable = new VersionedEntityDbTable<AccountAsset>("account_asset", accountAssetDbKeyFactory) {

        @Override
        protected AccountAsset load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountAsset(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountAsset accountAsset) throws SQLException {
            accountAsset.save(con);
        }

        @Override
        public void trim(int height) {
            _trim("account_asset", height - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK);
        }

        @Override
        public void checkAvailable(int height) {
            if (height + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK < Conch.getBlockchainProcessor().getMinRollbackHeight()) {
                throw new IllegalArgumentException("Historical data as of height " + height +" not available.");
            }
            if (height > Conch.getBlockchain().getHeight()) {
                throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + Conch.getBlockchain().getHeight());
            }
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY quantity DESC, account_id, asset_id ";
        }

    };

    private static final DbKey.LinkKeyFactory<AccountCurrency> accountCurrencyDbKeyFactory = new DbKey.LinkKeyFactory<AccountCurrency>("account_id", "currency_id") {

        @Override
        public DbKey newKey(AccountCurrency accountCurrency) {
            return accountCurrency.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountCurrency> accountCurrencyTable = new VersionedEntityDbTable<AccountCurrency>("account_currency", accountCurrencyDbKeyFactory) {

        @Override
        protected AccountCurrency load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountCurrency(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountCurrency accountCurrency) throws SQLException {
            accountCurrency.save(con);
        }

        @Override
        public void trim(int height) {
            _trim("account_currency", height);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY units DESC, account_id, currency_id ";
        }

    };

    private static final DerivedDbTable accountGuaranteedBalanceTable = new DerivedDbTable("account_guaranteed_balance") {

        @Override
        public void trim(int height) {
            if(Constants.SYNC_BUTTON) {
                return;
            }
            _trim("account_guaranteed_balance", height, false);
            _trim("account_guaranteed_balance_cache", height, false);
            _trim("account_guaranteed_balance_history", height, false);
        }

        @Override
        public void rollback(int height) {
            if (!db.isInTransaction()) {
                throw new IllegalStateException("Not in transaction");
            }
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmtSelect = con.prepareStatement("select distinct account_id FROM account_guaranteed_balance WHERE height > ?");
                pstmtSelect.setInt(1, height);
                ResultSet resultSet = pstmtSelect.executeQuery();
                try (PreparedStatement pstmtWorkDelete = con.prepareStatement("DELETE FROM account_guaranteed_balance WHERE height > ?");
                     PreparedStatement pstmtCacheDelete = con.prepareStatement("DELETE FROM account_guaranteed_balance_cache WHERE height > ?");
                     PreparedStatement pstmtHistoryDelete = con.prepareStatement("DELETE FROM account_guaranteed_balance_history WHERE height > ?");
                ) {
                    pstmtWorkDelete.setInt(1, height);
                    pstmtWorkDelete.executeUpdate();
                    pstmtCacheDelete.setInt(1, height);
                    pstmtCacheDelete.executeUpdate();
                    pstmtHistoryDelete.setInt(1, height);
                    pstmtHistoryDelete.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
                while (resultSet.next()) {
                    long accountId = resultSet.getLong("account_id");
                    PreparedStatement workTable = con.prepareStatement("select * FROM account_guaranteed_balance WHERE account_id = ? order by height desc limit 1");
                    workTable.setLong(1, accountId);
                    ResultSet resultSetWork = workTable.executeQuery();
                    if (resultSetWork.next()) {
                        PreparedStatement update = con.prepareStatement("update account_guaranteed_balance set latest = true where db_id = ?");
                        update.setLong(1,resultSetWork.getLong("db_id"));
                        update.executeUpdate();
                        continue;
                    }
                    PreparedStatement cacheTable = con.prepareStatement("select * FROM account_guaranteed_balance_cache WHERE account_id = ? order by height desc limit 1");
                    cacheTable.setLong(1, accountId);
                    ResultSet resultSetCache = cacheTable.executeQuery();
                    if (!resultSetCache.next()) {
                        PreparedStatement historyTable = con.prepareStatement("select * FROM account_guaranteed_balance_history WHERE account_id = ? order by height desc limit 1");
                        historyTable.setLong(1, accountId);
                        resultSetCache = historyTable.executeQuery();
                        if (!resultSetCache.next()) {
                            continue;
                        }
                    }
                    PreparedStatement pstmtUpdate = con.prepareStatement("INSERT INTO ACCOUNT_GUARANTEED_BALANCE (ACCOUNT_ID,"
                            + " ADDITIONS, HEIGHT, LATEST) VALUES (?, ?, ?, ?)");
                    pstmtUpdate.setLong(1, resultSetCache.getLong("ACCOUNT_ID"));
                    pstmtUpdate.setLong(2, resultSetCache.getLong("ADDITIONS"));
                    pstmtUpdate.setInt(3, resultSetCache.getInt("HEIGHT"));
                    pstmtUpdate.setBoolean(4, false);
                    pstmtUpdate.executeUpdate();
                }

            }catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    };

    private static final DbKey.LongKeyFactory<AccountProperty> accountPropertyDbKeyFactory = new DbKey.LongKeyFactory<AccountProperty>("id") {

        @Override
        public DbKey newKey(AccountProperty accountProperty) {
            return accountProperty.dbKey;
        }

    };

    private static final VersionedEntityDbTable<AccountProperty> accountPropertyTable = new VersionedEntityDbTable<AccountProperty>("account_property", accountPropertyDbKeyFactory) {

        @Override
        protected AccountProperty load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AccountProperty(rs, dbKey);
        }

        @Override
        protected void save(Connection con, AccountProperty accountProperty) throws SQLException {
            accountProperty.save(con);
        }

        @Override
        public void trim(int height) {
            _trim("account_property", height);
        }

    };

    private static final ConcurrentMap<DbKey, byte[]> publicKeyCache = Conch.getBooleanProperty("sharder.enablePublicKeyCache") ?
            new ConcurrentHashMap<>() : null;

    private static final Listeners<Account,Event> listeners = new Listeners<>();

    private static final Listeners<AccountAsset,Event> assetListeners = new Listeners<>();

    private static final Listeners<AccountCurrency,Event> currencyListeners = new Listeners<>();

    private static final Listeners<AccountLease,Event> leaseListeners = new Listeners<>();

    private static final Listeners<AccountProperty,Event> propertyListeners = new Listeners<>();

    public static boolean addListener(Listener<Account> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Account> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static boolean addAssetListener(Listener<AccountAsset> listener, Event eventType) {
        return assetListeners.addListener(listener, eventType);
    }

    public static boolean removeAssetListener(Listener<AccountAsset> listener, Event eventType) {
        return assetListeners.removeListener(listener, eventType);
    }

    public static boolean addCurrencyListener(Listener<AccountCurrency> listener, Event eventType) {
        return currencyListeners.addListener(listener, eventType);
    }

    public static boolean removeCurrencyListener(Listener<AccountCurrency> listener, Event eventType) {
        return currencyListeners.removeListener(listener, eventType);
    }

    public static boolean addLeaseListener(Listener<AccountLease> listener, Event eventType) {
        return leaseListeners.addListener(listener, eventType);
    }

    public static boolean removeLeaseListener(Listener<AccountLease> listener, Event eventType) {
        return leaseListeners.removeListener(listener, eventType);
    }

    public static boolean addPropertyListener(Listener<AccountProperty> listener, Event eventType) {
        return propertyListeners.addListener(listener, eventType);
    }

    public static boolean removePropertyListener(Listener<AccountProperty> listener, Event eventType) {
        return propertyListeners.removeListener(listener, eventType);
    }

    public static int getCount() {
        return publicKeyTable.getCount();
    }

    public static int getAssetAccountCount(long assetId) {
        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

    public static int getAssetAccountCount(long assetId, int height) {
        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId), height);
    }

    public static int getAccountAssetCount(long accountId) {
        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId));
    }

    public static int getAccountAssetCount(long accountId, int height) {
        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId), height);
    }

    public static int getCurrencyAccountCount(long currencyId) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("currency_id", currencyId));
    }

    public static int getCurrencyAccountCount(long currencyId, int height) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("currency_id", currencyId), height);
    }

    public static int getAccountCurrencyCount(long accountId) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("account_id", accountId));
    }

    public static int getAccountCurrencyCount(long accountId, int height) {
        return accountCurrencyTable.getCount(new DbClause.LongClause("account_id", accountId), height);
    }

    public static int getAccountLeaseCount() {
        return accountLeaseTable.getCount();
    }

    public static int getActiveLeaseCount() {
        return accountTable.getCount(new DbClause.NotNullClause("active_lessee_id"));
    }

    public static AccountProperty getProperty(long propertyId) {
        return accountPropertyTable.get(accountPropertyDbKeyFactory.newKey(propertyId));
    }

    public static DbIterator<AccountProperty> getProperties(long recipientId, long setterId, String property, int from, int to) {
        if (recipientId == 0 && setterId == 0) {
            throw new IllegalArgumentException("At least one of recipientId and setterId must be specified");
        }
        DbClause dbClause = null;
        if (setterId == recipientId) {
            dbClause = new DbClause.NullClause("setter_id");
        } else if (setterId != 0) {
            dbClause = new DbClause.LongClause("setter_id", setterId);
        }
        if (recipientId != 0) {
            if (dbClause != null) {
                dbClause = dbClause.and(new DbClause.LongClause("recipient_id", recipientId));
            } else {
                dbClause = new DbClause.LongClause("recipient_id", recipientId);
            }
        }
        if (property != null) {
            dbClause = dbClause.and(new DbClause.StringClause("property", property));
        }
        return accountPropertyTable.getManyBy(dbClause, from, to, " ORDER BY property ");
    }

    public static AccountProperty getProperty(long recipientId, String property) {
        return getProperty(recipientId, property, recipientId);
    }

    public static AccountProperty getProperty(long recipientId, String property, long setterId) {
        if (recipientId == 0 || setterId == 0) {
            throw new IllegalArgumentException("Both recipientId and setterId must be specified");
        }
        DbClause dbClause = new DbClause.LongClause("recipient_id", recipientId);
        dbClause = dbClause.and(new DbClause.StringClause("property", property));
        if (setterId != recipientId) {
            dbClause = dbClause.and(new DbClause.LongClause("setter_id", setterId));
        } else {
            dbClause = dbClause.and(new DbClause.NullClause("setter_id"));
        }
        return accountPropertyTable.getBy(dbClause);
    }

    /**
    public static Account getAccount(long id) {
        DbKey dbKey = accountDbKeyFactory.newKey(id);
        Account account = accountTable.get(dbKey);
        if (account == null) {
            PublicKey publicKey = publicKeyTable.get(dbKey);
            if (publicKey != null) {
                account = accountTable.newEntity(dbKey);
                account.publicKey = publicKey;
            }
        }
        return account;
    }

    public static Account getAccount(long id, int height) {
        DbKey dbKey = accountDbKeyFactory.newKey(id);
        Account account = accountTable.get(dbKey, height);
        if (account == null) {
            account = accountCacheTable.get(dbKey, height);
            if (account == null) {
                account = accountHistoryTable.get(dbKey, height);
                if (account == null) {
                    PublicKey publicKey = publicKeyTable.get(dbKey, height);
                    if (publicKey != null) {
                        account = accountTable.newEntity(dbKey);
                        account.publicKey = publicKey;
                    }
                }
            }
        }
        return account;
    }
   **/


    private static final Integer DONT_APPOINT_HEIGHT = -1;

    /**
     * query account according to accountId and height
     * if account data not exist at the height, return the closest height's account data in work,cache and history table
     * if exist cache data, return account data from cache
     * use the cache when the query operations happen in the same transaction
     *
     * @param accountId
     * @param height    null or -1(DONT_APPOINT_HEIGHT) means don't appoint end height
     * @return
     */
    private static Account _getAccount(long accountId, Integer height, boolean cache) {
        // get data from cache
        DbKey dbKey = accountDbKeyFactory.newKey(accountId);
        if (cache && Db.db.isInTransaction()) {
            Account account = (Account) Db.db.getCache("account").get(dbKey);
            if (account != null) {
                return account;
            }
        }

        boolean appointHeight = true;
        if(height == null || (height.intValue() == DONT_APPOINT_HEIGHT.intValue())) {
            appointHeight = false;
        }

        // query height should smaller than current height
        if (appointHeight
                && height > Conch.getHeight()) {
            throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + Conch.getHeight());
        }
        boolean isInTx = Db.db.isInTransaction();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            Account account = null;
            String querySql = "select * FROM %s WHERE id = ?"
                    + (appointHeight ? " and height <= ?" : "")
                    + " order by height desc limit 1";

            String workTableQuerySql = String.format(querySql, "account");
            PreparedStatement accountWorkQuery = con.prepareStatement(workTableQuerySql);
            accountWorkQuery.setLong(1, accountId);
            if (appointHeight) {
                accountWorkQuery.setInt(2, height);
            }

            ResultSet resultSet = accountWorkQuery.executeQuery();
            if (!resultSet.next()) {
                String cacheTableQuerySql = String.format(querySql, "account_cache");
                PreparedStatement accountCacheQuery = con.prepareStatement(cacheTableQuerySql);
                accountCacheQuery.setLong(1, accountId);
                if (appointHeight) {
                    accountCacheQuery.setInt(2, height);
                }
                resultSet = accountCacheQuery.executeQuery();
                if (!resultSet.next()) {
                    String historyTableQuerySql = String.format(querySql, "account_history");
                    PreparedStatement accountHistoryQuery = con.prepareStatement(historyTableQuerySql);
                    accountHistoryQuery.setLong(1, accountId);
                    if (appointHeight) {
                        accountHistoryQuery.setInt(2, height);
                    }
                    resultSet = accountHistoryQuery.executeQuery();
                    // if database doesn't exist data return new entity
                    if (!resultSet.next()) {
                        PublicKey publicKey = publicKeyTable.get(dbKey);
                        if (publicKey != null) {
                            account = accountTable.newEntity(dbKey);
                            account.publicKey = publicKey;
                        }
                        return account;
                    }
                }
            }
            // set data to cache and return
            if (Db.db.isInTransaction()) {
                DbKey dbKey1 = accountDbKeyFactory.newKey(resultSet);
                account = (Account) Db.db.getCache("account").get(dbKey1);
                if (account == null) {
                    account = new Account(resultSet, dbKey1);
                    Db.db.getCache("account").put(dbKey1, account);
                }
            } else {
                account = new Account(resultSet, dbKey);
            }
            return account;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }

    }

    public static Account getAccount(long id) {
        return _getAccount(id, DONT_APPOINT_HEIGHT, true);
    }

    public static Account getAccount(long id, int height) {
        // if param height illegal or setting not allowed, get the latest data
        boolean doesNotExceed = Conch.getHeight() <= height
                && !(accountTable.isPersistent() && Conch.getBlockchainProcessor().isScanning());

        if (height < 0 || doesNotExceed) {
            return getAccount(id);
        }
        return _getAccount(id, height, false);
    }

    public static Account getAccount(byte[] publicKey) {
        long accountId = getId(publicKey);
        Account account = getAccount(accountId);
        if (account == null) {
            return null;
        }
        if (account.publicKey == null) {
            account.publicKey = publicKeyTable.get(accountDbKeyFactory.newKey(account));
        }
        if (account.publicKey == null || account.publicKey.publicKey == null || Arrays.equals(account.publicKey.publicKey, publicKey)) {
            return account;
        }
        throw new RuntimeException("DUPLICATE KEY for account " + Long.toUnsignedString(accountId)
                + " existing key " + Convert.toHexString(account.publicKey.publicKey) + " new key " + Convert.toHexString(publicKey));
    }

    public static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    public static long getId(String secretPhrase) {
       return getId(Crypto.getPublicKey(secretPhrase));
    }

    public static byte[] getPublicKey(long id) {
        DbKey dbKey = publicKeyDbKeyFactory.newKey(id);
        byte[] key = null;
        if (publicKeyCache != null) {
            key = publicKeyCache.get(dbKey);
        }
        if (key == null) {
            PublicKey publicKey = publicKeyTable.get(dbKey);
            if (publicKey == null || (key = publicKey.publicKey) == null) {
                return null;
            }
            if (publicKeyCache != null) {
                publicKeyCache.put(dbKey, key);
            }
        }
        return key;
    }

    public static long rsAccountToId(String rsAccount) {
        if (rsAccount == null || (rsAccount = rsAccount.trim()).isEmpty()) {
            return 0;
        }
        rsAccount = rsAccount.toUpperCase();
        if (rsAccount.startsWith(Constants.ACCOUNT_PREFIX)) {
            return Crypto.rsDecode(rsAccount.substring(4));
        } else {
            return Long.parseUnsignedLong(rsAccount);
        }
    }

    public static String rsAccount(long accountId) {
        return Constants.ACCOUNT_PREFIX + Crypto.rsEncode(accountId);
    }

    public static String rsAccount(String secretPhrase) {
        return rsAccount(getId(secretPhrase));
    }
    

    public static Account addOrGetAccount(long id) {
        if (id == 0) {
            throw new IllegalArgumentException("Invalid accountId 0");
        }
        DbKey dbKey = accountDbKeyFactory.newKey(id);
        Account account = accountTable.get(dbKey);
        if (account == null) {
            account = accountTable.newEntity(dbKey);
            PublicKey publicKey = publicKeyTable.get(dbKey);
            if (publicKey == null) {
                publicKey = publicKeyTable.newEntity(dbKey);
                publicKeyTable.insert(publicKey);
            }
            account.publicKey = publicKey;
        }
        return account;
    }

    private static DbIterator<AccountLease> getLeaseChangingAccounts(final int height) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                    "SELECT * FROM account_lease WHERE current_leasing_height_from = ? AND latest = TRUE "
                            + "UNION ALL SELECT * FROM account_lease WHERE current_leasing_height_to = ? AND latest = TRUE "
                            + "ORDER BY current_lessee_id, lessor_id");
            int i = 0;
            pstmt.setInt(++i, height);
            pstmt.setInt(++i, height);
            return accountLeaseTable.getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<AccountAsset> getAccountAssets(long accountId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<AccountAsset> getAccountAssets(long accountId, int height, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", accountId), height, from, to);
    }

    public static AccountAsset getAccountAsset(long accountId, long assetId) {
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId));
    }

    public static AccountAsset getAccountAsset(long accountId, long assetId, int height) {
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId), height);
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to, " ORDER BY quantity DESC, account_id ");
    }

    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), height, from, to, " ORDER BY quantity DESC, account_id ");
    }

    public static AccountCurrency getAccountCurrency(long accountId, long currencyId) {
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
    }

    public static AccountCurrency getAccountCurrency(long accountId, long currencyId, int height) {
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId), height);
    }

    public static DbIterator<AccountCurrency> getAccountCurrencies(long accountId, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<AccountCurrency> getAccountCurrencies(long accountId, int height, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", accountId), height, from, to);
    }

    public static DbIterator<AccountCurrency> getCurrencyAccounts(long currencyId, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
    }

    public static DbIterator<AccountCurrency> getCurrencyAccounts(long currencyId, int height, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), height, from, to);
    }

    public static long getAssetBalanceQNT(long accountId, long assetId, int height) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId), height);
        return accountAsset == null ? 0 : accountAsset.quantityQNT;
    }

    public static long getAssetBalanceQNT(long accountId, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId));
        return accountAsset == null ? 0 : accountAsset.quantityQNT;
    }

    public static long getUnconfirmedAssetBalanceQNT(long accountId, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId));
        return accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
    }

    public static long getCurrencyUnits(long accountId, long currencyId, int height) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId), height);
        return accountCurrency == null ? 0 : accountCurrency.units;
    }

    public static long getCurrencyUnits(long accountId, long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.units;
    }

    public static long getUnconfirmedCurrencyUnits(long accountId, long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
    }

    public static DbIterator<AccountInfo> searchAccounts(String query, int from, int to) {
        return accountInfoTable.search(query, DbClause.EMPTY_CLAUSE, from, to);
    }

    static {

        Conch.getBlockchainProcessor().addListener(block -> {
            int height = block.getHeight();

            List<AccountLease> changingLeases = new ArrayList<>();
            DbIterator<AccountLease> leases = null;
            try {
                leases = getLeaseChangingAccounts(height);
                while (leases.hasNext()) {
                    changingLeases.add(leases.next());
                }
            }finally {
                DbUtils.close(leases);
            }
            for (AccountLease lease : changingLeases) {
                Account lessor = Account.getAccount(lease.lessorId);
                if (height == lease.currentLeasingHeightFrom) {
                    lessor.activeLesseeId = lease.currentLesseeId;
                    leaseListeners.notify(lease, Event.LEASE_STARTED);
                } else if (height == lease.currentLeasingHeightTo) {
                    leaseListeners.notify(lease, Event.LEASE_ENDED);
                    lessor.activeLesseeId = 0;
                    if (lease.nextLeasingHeightFrom == 0) {
                        lease.currentLeasingHeightFrom = 0;
                        lease.currentLeasingHeightTo = 0;
                        lease.currentLesseeId = 0;
                        accountLeaseTable.delete(lease);
                    } else {
                        lease.currentLeasingHeightFrom = lease.nextLeasingHeightFrom;
                        lease.currentLeasingHeightTo = lease.nextLeasingHeightTo;
                        lease.currentLesseeId = lease.nextLesseeId;
                        lease.nextLeasingHeightFrom = 0;
                        lease.nextLeasingHeightTo = 0;
                        lease.nextLesseeId = 0;
                        accountLeaseTable.insert(lease);
                        if (height == lease.currentLeasingHeightFrom) {
                            lessor.activeLesseeId = lease.currentLesseeId;
                            leaseListeners.notify(lease, Event.LEASE_STARTED);
                        }
                    }
                }
                lessor.save();
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);

        if (publicKeyCache != null) {

            Conch.getBlockchainProcessor().addListener(block -> {
                publicKeyCache.remove(accountDbKeyFactory.newKey(block.getGeneratorId()));
                block.getTransactions().forEach(transaction -> {
                    publicKeyCache.remove(accountDbKeyFactory.newKey(transaction.getSenderId()));
                    if (!transaction.getAppendages(appendix -> (appendix instanceof Appendix.PublicKeyAnnouncement), false).isEmpty()) {
                        publicKeyCache.remove(accountDbKeyFactory.newKey(transaction.getRecipientId()));
                    }
                    if (transaction.getType() == ShufflingTransaction.SHUFFLING_RECIPIENTS) {
                        Attachment.ShufflingRecipients shufflingRecipients = (Attachment.ShufflingRecipients) transaction.getAttachment();
                        for (byte[] publicKey : shufflingRecipients.getRecipientPublicKeys()) {
                            publicKeyCache.remove(accountDbKeyFactory.newKey(Account.getId(publicKey)));
                        }
                    }
                });
            }, BlockchainProcessor.Event.BLOCK_POPPED);

            Conch.getBlockchainProcessor().addListener(block -> publicKeyCache.clear(), BlockchainProcessor.Event.RESCAN_BEGIN);

        }

    }

    public static void init() {}


    private final long id;
    private final DbKey dbKey;
    private PublicKey publicKey;
    private long balanceNQT;
    private long unconfirmedBalanceNQT;
    private long forgedBalanceNQT;
    private long frozenBalanceNQT;
    private long activeLesseeId;
    private Set<ControlType> controls;

    private Account(long id) {
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            Logger.logMessage("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
        }
        this.id = id;
        this.dbKey = accountDbKeyFactory.newKey(this.id);
        this.controls = Collections.emptySet();
    }

    private Account(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.balanceNQT = rs.getLong("balance");
        this.unconfirmedBalanceNQT = rs.getLong("unconfirmed_balance");
        this.forgedBalanceNQT = rs.getLong("forged_balance");
        this.frozenBalanceNQT = rs.getLong("frozen_balance");
        this.activeLesseeId = rs.getLong("active_lessee_id");
        if (rs.getBoolean("has_control_phasing")) {
            controls = Collections.unmodifiableSet(EnumSet.of(ControlType.PHASING_ONLY));
        } else {
            controls = Collections.emptySet();
        }
    }

    private void save(Connection con) throws SQLException {
        boolean insertNew = true;
        if(Constants.updateHistoryRecord()){
                PreparedStatement pstmt = con.prepareStatement("SELECT db_id, height FROM account"
                    + " WHERE id=? AND latest = TRUE ORDER BY height DESC LIMIT 1");
                pstmt.setLong(1, this.id);

                ResultSet queryRS = pstmt.executeQuery();
                if(queryRS != null && queryRS.next()){
                    Long dbid = queryRS.getLong("db_id");
                    pstmt = con.prepareStatement("UPDATE account "
                            + "SET height = ?"
                            + ",balance = ?"
                            + ",unconfirmed_balance = ?"
                            + ",forged_balance = ?"
                            + ",frozen_balance = ?"
                            + ",active_lessee_id = ?"
                            + ",has_control_phasing = ?"
                            + ",latest = ?"
                            + " WHERE DB_ID = ?"
                    );
                    pstmt.setInt(1, Conch.getHeight());
                    pstmt.setLong(2, this.balanceNQT);
                    pstmt.setLong(3, this.unconfirmedBalanceNQT);
                    pstmt.setLong(4, this.forgedBalanceNQT);
                    pstmt.setLong(5, this.frozenBalanceNQT);
                    DbUtils.setLongZeroToNull(pstmt, 6, this.activeLesseeId);
                    pstmt.setBoolean(7, controls.contains(ControlType.PHASING_ONLY));
                    pstmt.setBoolean(8, true);
                    pstmt.setLong(9, dbid);
                    pstmt.executeUpdate();
                    insertNew = false;
                }
        }

        if(insertNew){
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO account (id, "
                    + "balance, unconfirmed_balance, forged_balance, frozen_balance,"
                    + "active_lessee_id, has_control_phasing, height, latest) "
                    + "KEY (id, height) VALUES (?, ?, ?, ?,?, ?, ?, ?, TRUE)");
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.balanceNQT);
            pstmt.setLong(++i, this.unconfirmedBalanceNQT);
            pstmt.setLong(++i, this.forgedBalanceNQT);
            pstmt.setLong(++i, this.frozenBalanceNQT);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.activeLesseeId);
            pstmt.setBoolean(++i, controls.contains(ControlType.PHASING_ONLY));
            pstmt.setInt(++i, Conch.getHeight());
            pstmt.executeUpdate();
        }
    }

    private void save() {
        if (balanceNQT == 0 && unconfirmedBalanceNQT == 0 && forgedBalanceNQT == 0 && frozenBalanceNQT == 0 && activeLesseeId == 0 && controls.isEmpty()) {
            accountTable.delete(this, true);
        } else {
            accountTable.insert(this);
        }
    }

    /**
     * Don't use this method if you don't know this method's usage:
     * - reset balance of account
     * - reset the additions of guarant balance
     * @param balance
     * @param unconfirmedBalance
     * @param forgedBalance
     * @param frozenBalance
     */
    public void reset(Connection con,long balance,long unconfirmedBalance, long forgedBalance, long frozenBalance) throws SQLException {
        this.balanceNQT = balance;
        this.unconfirmedBalanceNQT = unconfirmedBalance;
        this.forgedBalanceNQT = forgedBalance;
        this.frozenBalanceNQT = frozenBalance;
        save(con);

        resetGuaranteedBalance(con, this.balanceNQT);
    }

    public long getId() {
        return id;
    }
    
    public String getRsAddress() {
        return rsAccount(id);
    }

    public AccountInfo getAccountInfo() {
        return accountInfoTable.get(accountDbKeyFactory.newKey(this));
    }

    public void setAccountInfo(String name, String description) {
        name = Convert.emptyToNull(name.trim());
        description = Convert.emptyToNull(description.trim());
        AccountInfo accountInfo = getAccountInfo();
        if (accountInfo == null) {
            accountInfo = new AccountInfo(id, name, description);
        } else {
            accountInfo.name = name;
            accountInfo.description = description;
        }
        accountInfo.save();
    }

    public AccountLease getAccountLease() {
        return accountLeaseTable.get(accountDbKeyFactory.newKey(this));
    }

    public EncryptedData encryptTo(byte[] data, String senderSecretPhrase, boolean compress) {
        byte[] key = getPublicKey(this.id);
        if (key == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return Account.encryptTo(key, data, senderSecretPhrase, compress);
    }

    public static EncryptedData encryptTo(byte[] publicKey, byte[] data, String senderSecretPhrase, boolean compress) {
        if (compress && data.length > 0) {
            data = Convert.compress(data);
        }
        return EncryptedData.encrypt(data, senderSecretPhrase, publicKey);
    }

    public byte[] decryptFrom(EncryptedData encryptedData, String recipientSecretPhrase, boolean uncompress) {
        byte[] key = getPublicKey(this.id);
        if (key == null) {
            throw new IllegalArgumentException("Sender account doesn't have a public key set");
        }
        return Account.decryptFrom(key, encryptedData, recipientSecretPhrase, uncompress);
    }

    public static byte[] decryptFrom(byte[] publicKey, EncryptedData encryptedData, String recipientSecretPhrase, boolean uncompress) {
        byte[] decrypted = encryptedData.decrypt(recipientSecretPhrase, publicKey);
        if (uncompress && decrypted.length > 0) {
            decrypted = Convert.uncompress(decrypted);
        }
        return decrypted;
    }

    public long getBalanceNQT() {
        return balanceNQT;
    }

    public long getUnconfirmedBalanceNQT() {
        return unconfirmedBalanceNQT;
    }

    public long getForgedBalanceNQT() {
        return forgedBalanceNQT;
    }

    public long getFrozenBalanceNQT() {
        return frozenBalanceNQT;
    }

    public long getCurrentEffectiveBalanceSS() {
        return getEffectiveBalanceSS(Conch.getHeight());
    }
    
    /**
     * return the effective balance in the unit
     * @param height
     * @return
     */
    public long getEffectiveBalanceSS(int height) {
        return getEffectiveBalanceNQT(height) / Constants.ONE_SS;
    }

    /**
     * return the effective balance in the unit NQT (10 decimals)
     * @param height
     * @return
     */
    public long getEffectiveBalanceNQT(int height) {
        if (this.publicKey == null) {
            this.publicKey = publicKeyTable.get(accountDbKeyFactory.newKey(this));
        }

        // adding height judgment logic, not check the account publicKey
        if (height <= Constants.NONE_PUBLICKEY_ACTIVE_HEIGHT && (this.publicKey == null || this.publicKey.publicKey == null)) {
            return 0;
        }

        try {
            Conch.getBlockchain().readLock();
            long effectiveBalanceNQT = getLessorsGuaranteedBalanceNQT(height);
            if (activeLesseeId == 0 || Constants.SYNC_BUTTON) {
                effectiveBalanceNQT += getGuaranteedBalanceNQT(Constants.GUARANTEED_BALANCE_CONFIRMATIONS, height);
            }
            return  effectiveBalanceNQT;
        } finally {
            Conch.getBlockchain().readUnlock();
        }
    }

    private long getLessorsGuaranteedBalanceNQT(int height) {
        if (!Constants.isOpenLessorMode) {
            return 0;
        }
        boolean inInTx = Db.db.isInTransaction();
        List<Account> lessors = new ArrayList<>();
        DbIterator<Account> iterator = null;
        try {
            iterator = getLessors(height);
            while (iterator.hasNext()) {
                lessors.add(iterator.next());
            }
        }finally {
            if (!inInTx) {
                DbUtils.close(iterator);
            }
        }
        if (lessors.isEmpty()) {
            return 0;
        }
        Long[] lessorIds = new Long[lessors.size()];
        long[] balances = new long[lessors.size()];
        for (int i = 0; i < lessors.size(); i++) {
            lessorIds[i] = lessors.get(i).getId();
            balances[i] = lessors.get(i).getBalanceNQT();
        }
        int blockchainHeight = Conch.getBlockchain().getHeight();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT account_id, SUM (additions) AS additions "
                    + "FROM account_guaranteed_balance, TABLE (id BIGINT=?) T WHERE account_id = T.id AND height > ? "
                    + (height < blockchainHeight ? " AND height <= ? " : "")
                    + " GROUP BY account_id ORDER BY account_id");
            
            pstmt.setObject(1, lessorIds);
            pstmt.setInt(2, height - Constants.GUARANTEED_BALANCE_CONFIRMATIONS);
            if (height < blockchainHeight) {
                pstmt.setInt(3, height);
            }
            long total = 0;
            int i = 0;
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long accountId = rs.getLong("account_id");
                    while (lessorIds[i] < accountId && i < lessorIds.length) {
                        total += balances[i++];
                    }
                    if (lessorIds[i] == accountId) {
                        total += Math.max(balances[i++] - rs.getLong("additions"), 0);
                    }
                }
            }
            while (i < balances.length) {
                total += balances[i++];
            }
            return total;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            if (!inInTx) {
                DbUtils.close(con);
            }
        }
    }

    public DbIterator<Account> getLessors() {
        return accountTable.getManyBy(new DbClause.LongClause("active_lessee_id", id), 0, -1, " ORDER BY id ASC ");
    }

    public DbIterator<Account> getLessors(int height) {
        return accountTable.getManyBy(new DbClause.LongClause("active_lessee_id", id), height, 0, -1, " ORDER BY id ASC ");
    }

    private void resetGuaranteedBalance(Connection con, final long balance) {
        long currentGuarantBalance = getGuaranteedBalanceNQT();
        if(currentGuarantBalance == balance) {
            return;
        }

        try {
            Conch.getBlockchain().readLock();
            boolean isInTx = Db.db.isInTransaction();
            Connection connDel = Db.db.getConnection();
            try{
                // delete records which the height larger than the current height
                PreparedStatement pstmtDel = connDel.prepareStatement("DELETE FROM ACCOUNT_GUARANTEED_BALANCE WHERE ACCOUNT_ID = ? AND HEIGHT >= ?");
                pstmtDel.setLong(1, this.id);
                pstmtDel.setInt(2, Conch.getHeight());
                pstmtDel.executeUpdate();
            }catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (!isInTx) {
                    DbUtils.close(connDel);
                }
            }


            if(balance > currentGuarantBalance) {
                PreparedStatement pstmtUpdate = con.prepareStatement("INSERT INTO ACCOUNT_GUARANTEED_BALANCE (ACCOUNT_ID,"
                        + " ADDITIONS, HEIGHT) VALUES (?, ?, ?)");

                long additions = balance - currentGuarantBalance;
                pstmtUpdate.setLong(1, this.id);
                pstmtUpdate.setLong(2, additions);
                pstmtUpdate.setInt(3, Conch.getHeight());
                pstmtUpdate.executeUpdate();
            }else {
                PreparedStatement pstmt = con.prepareStatement("SELECT DB_ID, ADDITIONS, min(height) minHeight"
                        + "FROM ACCOUNT_GUARANTEED_BALANCE WHERE ACCOUNT_ID = ? AND HEIGHT < ? ORDER BY HEIGHT DESC");
                int queryHeight = Conch.getHeight() + 1;
                pstmt.setLong(1, this.id);
                pstmt.setInt(2, queryHeight);
                long afterSub = currentGuarantBalance;
                String delIds = "";
                Set<Long> delList = Sets.newHashSet();
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        long tmp = Math.subtractExact(afterSub, rs.getLong("ADDITIONS"));
                        queryHeight = rs.getInt("minHeight");
                        Long dbId = rs.getLong("DB_ID");
                        delList.add(dbId);
                        delIds += dbId + ",";
                        if(tmp > balance) {
                            afterSub = tmp;
                        }else{
                            afterSub = Math.subtractExact(afterSub,balance);
                            break;
                        }
                    }
                }

                PreparedStatement cachePstmt = con.prepareStatement("SELECT DB_ID, ADDITIONS, min(height) minHeight"
                        + "FROM ACCOUNT_GUARANTEED_BALANCE_CACHE WHERE ACCOUNT_ID = ? AND HEIGHT < ? ORDER BY HEIGHT DESC");
                cachePstmt.setLong(1, this.id);
                cachePstmt.setInt(2, queryHeight);

                try (ResultSet rs = cachePstmt.executeQuery()) {
                    while (rs.next()) {
                        long tmp = Math.subtractExact(afterSub, rs.getLong("ADDITIONS"));
                        queryHeight = rs.getInt("minHeight");
                        Long dbId = rs.getLong("DB_ID");
                        delList.add(dbId);
                        delIds += dbId + ",";
                        if(tmp > balance) {
                            afterSub = tmp;
                        }else{
                            afterSub = Math.subtractExact(afterSub,balance);
                            break;
                        }
                    }
                }

                PreparedStatement historyPstmt = con.prepareStatement("SELECT DB_ID, ADDITIONS "
                        + "FROM ACCOUNT_GUARANTEED_BALANCE_HISTORY WHERE ACCOUNT_ID = ? AND HEIGHT < ? ORDER BY HEIGHT DESC");
                historyPstmt.setLong(1, this.id);
                historyPstmt.setInt(2, queryHeight);

                try (ResultSet rs = historyPstmt.executeQuery()) {
                    while (rs.next()) {
                        long tmp = Math.subtractExact(afterSub, rs.getLong("ADDITIONS"));

                        Long dbId = rs.getLong("DB_ID");
                        delList.add(dbId);
                        delIds += dbId + ",";
                        if(tmp > balance) {
                            afterSub = tmp;
                        }else{
                            afterSub = Math.subtractExact(afterSub,balance);
                            break;
                        }
                    }
                }

                if(delIds.length() > 1) {
                    delIds = delIds.substring(0, delIds.length() - 1);
                    PreparedStatement pstmtUpdate = con.prepareStatement("DELETE FROM ACCOUNT_GUARANTEED_BALANCE WHERE DB_ID IN (" + delIds + ")");
                    pstmtUpdate.executeUpdate();
                    PreparedStatement cachePstmtUpdate = con.prepareStatement("DELETE FROM ACCOUNT_GUARANTEED_BALANCE_CACHE WHERE DB_ID IN (" + delIds + ")");
                    cachePstmtUpdate.executeUpdate();
                    PreparedStatement historyPstmtUpdate = con.prepareStatement("DELETE FROM ACCOUNT_GUARANTEED_BALANCE_HISTORY WHERE DB_ID IN (" + delIds + ")");
                    historyPstmtUpdate.executeUpdate();
                }

                if(afterSub > 0) {
                    PreparedStatement pstmtUpdate = con.prepareStatement("INSERT INTO ACCOUNT_GUARANTEED_BALANCE (ACCOUNT_ID,"
                            + " ADDITIONS, HEIGHT) VALUES (?, ?, ?)");

                    pstmtUpdate.setLong(1, this.id);
                    pstmtUpdate.setLong(2, afterSub);
                    pstmtUpdate.setInt(3, Conch.getHeight());
                    pstmtUpdate.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Conch.getBlockchain().readUnlock();
        }
    }

    public long getGuaranteedBalanceNQT() {
        return getGuaranteedBalanceNQT(Constants.GUARANTEED_BALANCE_CONFIRMATIONS, Conch.getHeight());
    }

    public long getGuaranteedBalanceNQT(final int numberOfConfirmations,final int currentHeight) {

        try {
            Conch.getBlockchain().readLock();

            int fromHeight = currentHeight - numberOfConfirmations;
            if(fromHeight < 0){
                fromHeight = 0;
            }
//            if (fromHeight + Constants.GUARANTEED_BALANCE_CONFIRMATIONS < Conch.getBlockchainProcessor().getMinRollbackHeight()
//                    || fromHeight > Conch.getBlockchain().getHeight()) {
//                throw new IllegalArgumentException("Height " + fromHeight + " not available for guaranteed balance calculation");
//            }
            boolean isInTx = Db.db.isInTransaction();
            Connection con = null;
            try {
                con = Db.db.getConnection();
                Long additions = 0L;
                int toHeight = currentHeight + 1;
                PreparedStatement pstmt = con.prepareStatement("SELECT SUM (additions) AS additions, min(height) as height "
                        + "FROM account_guaranteed_balance WHERE account_id = ? AND height > ? AND height < ?");
                pstmt.setLong(1, this.id);
                pstmt.setInt(2, fromHeight);
                pstmt.setInt(3, toHeight);
                ResultSet workRs = pstmt.executeQuery();
                String cacheSql;
                if (workRs.next()) {
                    toHeight = workRs.getInt("height");
                    additions += workRs.getLong("additions");
                }
                PreparedStatement cachePstmt = con.prepareStatement("SELECT SUM (additions) AS additions, min(height) as height "
                        + "FROM account_guaranteed_balance_cache WHERE account_id = ? AND height > ? AND height < ?");
                cachePstmt.setLong(1, this.id);
                cachePstmt.setInt(2, fromHeight);
                cachePstmt.setInt(3, toHeight);
                ResultSet cacheRs = cachePstmt.executeQuery();
                String historySql;
                if (cacheRs.next()) {
                    toHeight = cacheRs.getInt("height");
                    additions += cacheRs.getLong("additions");
                }
                PreparedStatement historyPstmt = con.prepareStatement("SELECT SUM (additions) AS additions "
                        + "FROM account_guaranteed_balance_history WHERE account_id = ? AND height > ? AND height < ?");
                historyPstmt.setLong(1, this.id);
                historyPstmt.setInt(2, fromHeight);
                historyPstmt.setInt(3, toHeight);
                ResultSet historyRs = historyPstmt.executeQuery();
                return Math.max(Math.subtractExact(balanceNQT, additions + (historyRs.next() ? historyRs.getLong("additions") : 0)), 0);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }finally {
                if (!isInTx) {
                    DbUtils.close(con);
                }
            }
        } finally {
            Conch.getBlockchain().readUnlock();
        }
    }

    public long getGuaranteedBalanceNQT(final int numberOfConfirmations) {

        try {
            Conch.getBlockchain().readLock();

            int fromHeight = Conch.getHeight() - numberOfConfirmations;
            if(fromHeight < 0){
                fromHeight = 0;
            }
//            if (fromHeight + Constants.GUARANTEED_BALANCE_CONFIRMATIONS < Conch.getBlockchainProcessor().getMinRollbackHeight()
//                    || fromHeight > Conch.getBlockchain().getHeight()) {
//                throw new IllegalArgumentException("Height " + fromHeight + " not available for guaranteed balance calculation");
//            }
            boolean isInTx = Db.db.isInTransaction();
            Connection con = null;
            try {
                con = Db.db.getConnection();
                Long additions = 0L;
                int toHeight = Conch.getHeight() + 1;
                PreparedStatement pstmt = con.prepareStatement("SELECT SUM (additions) AS additions, min(height) as height "
                        + "FROM account_guaranteed_balance WHERE account_id = ? AND height > ? AND height < ?");
                pstmt.setLong(1, this.id);
                pstmt.setInt(2, fromHeight);
                pstmt.setInt(3, toHeight);
                ResultSet workRs = pstmt.executeQuery();
                return Math.max(Math.subtractExact(balanceNQT, additions), 0);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }finally {
                if (!isInTx) {
                    DbUtils.close(con);
                }
            }
        } finally {
            Conch.getBlockchain().readUnlock();
        }
    }

    public DbIterator<AccountAsset> getAssets(int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", this.id), from, to);
    }

    public DbIterator<AccountAsset> getAssets(int height, int from, int to) {
        return accountAssetTable.getManyBy(new DbClause.LongClause("account_id", this.id), height, from, to);
    }

    public DbIterator<Trade> getTrades(int from, int to) {
        return Trade.getAccountTrades(this.id, from, to);
    }

    public DbIterator<AssetTransfer> getAssetTransfers(int from, int to) {
        return AssetTransfer.getAccountAssetTransfers(this.id, from, to);
    }

    public DbIterator<CurrencyTransfer> getCurrencyTransfers(int from, int to) {
        return CurrencyTransfer.getAccountCurrencyTransfers(this.id, from, to);
    }

    public DbIterator<Exchange> getExchanges(int from, int to) {
        return Exchange.getAccountExchanges(this.id, from, to);
    }

    public AccountAsset getAsset(long assetId) {
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
    }

    public AccountAsset getAsset(long assetId, int height) {
        return accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId), height);
    }

    public long getAssetBalanceQNT(long assetId) {
        return getAssetBalanceQNT(this.id, assetId);
    }

    public long getAssetBalanceQNT(long assetId, int height) {
        return getAssetBalanceQNT(this.id, assetId, height);
    }

    public long getUnconfirmedAssetBalanceQNT(long assetId) {
        return getUnconfirmedAssetBalanceQNT(this.id, assetId);
    }

    public AccountCurrency getCurrency(long currencyId) {
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
    }

    public AccountCurrency getCurrency(long currencyId, int height) {
        return accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId), height);
    }

    public DbIterator<AccountCurrency> getCurrencies(int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", this.id), from, to);
    }

    public DbIterator<AccountCurrency> getCurrencies(int height, int from, int to) {
        return accountCurrencyTable.getManyBy(new DbClause.LongClause("account_id", this.id), height, from, to);
    }

    public long getCurrencyUnits(long currencyId) {
        return getCurrencyUnits(this.id, currencyId);
    }

    public long getCurrencyUnits(long currencyId, int height) {
        return getCurrencyUnits(this.id, currencyId, height);
    }

    public long getUnconfirmedCurrencyUnits(long currencyId) {
        return getUnconfirmedCurrencyUnits(this.id, currencyId);
    }

    public Set<ControlType> getControls() {
        return controls;
    }

    public void leaseEffectiveBalance(long lesseeId, int period) {
        int height = Conch.getBlockchain().getHeight();
        AccountLease accountLease = accountLeaseTable.get(accountDbKeyFactory.newKey(this));
        if (accountLease == null) {
            accountLease = new AccountLease(id,
                    height + Constants.LEASING_DELAY,
                    height + Constants.LEASING_DELAY + period,
                    lesseeId);
        } else if (accountLease.currentLesseeId == 0) {
            accountLease.currentLeasingHeightFrom = height + Constants.LEASING_DELAY;
            accountLease.currentLeasingHeightTo = height + Constants.LEASING_DELAY + period;
            accountLease.currentLesseeId = lesseeId;
        } else {
            accountLease.nextLeasingHeightFrom = height + Constants.LEASING_DELAY;
            if (accountLease.nextLeasingHeightFrom < accountLease.currentLeasingHeightTo) {
                accountLease.nextLeasingHeightFrom = accountLease.currentLeasingHeightTo;
            }
            accountLease.nextLeasingHeightTo = accountLease.nextLeasingHeightFrom + period;
            accountLease.nextLesseeId = lesseeId;
        }
        accountLeaseTable.insert(accountLease);
        leaseListeners.notify(accountLease, Event.LEASE_SCHEDULED);
    }

    public void addControl(ControlType control) {
        if (controls.contains(control)) {
            return;
        }
        EnumSet<ControlType> newControls = EnumSet.of(control);
        newControls.addAll(controls);
        controls = Collections.unmodifiableSet(newControls);
        accountTable.insert(this);
    }

    public void removeControl(ControlType control) {
        if (!controls.contains(control)) {
            return;
        }
        EnumSet<ControlType> newControls = EnumSet.copyOf(controls);
        newControls.remove(control);
        controls = Collections.unmodifiableSet(newControls);
        save();
    }

    public void setProperty(Transaction transaction, Account setterAccount, String property, String value) {
        value = Convert.emptyToNull(value);
        AccountProperty accountProperty = getProperty(this.id, property, setterAccount.id);
        if (accountProperty == null) {
            accountProperty = new AccountProperty(transaction.getId(), this.id, setterAccount.id, property, value);
        } else {
            accountProperty.value = value;
        }
        accountPropertyTable.insert(accountProperty);
        listeners.notify(this, Event.SET_PROPERTY);
        propertyListeners.notify(accountProperty, Event.SET_PROPERTY);
    }

    public void deleteProperty(long propertyId) {
        AccountProperty accountProperty = accountPropertyTable.get(accountPropertyDbKeyFactory.newKey(propertyId));
        if (accountProperty == null) {
            return;
        }
        if (accountProperty.getSetterId() != this.id && accountProperty.getRecipientId() != this.id) {
            throw new RuntimeException("Property " + Long.toUnsignedString(propertyId) + " cannot be deleted by " + Long.toUnsignedString(this.id));
        }
        accountPropertyTable.delete(accountProperty);
        listeners.notify(this, Event.DELETE_PROPERTY);
        propertyListeners.notify(accountProperty, Event.DELETE_PROPERTY);
    }

    public static boolean setOrVerify(long accountId, byte[] key) {
        DbKey dbKey = publicKeyDbKeyFactory.newKey(accountId);
        PublicKey publicKey = publicKeyTable.get(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.publicKey == null) {
            publicKey.publicKey = key;
            publicKey.height = Conch.getBlockchain().getHeight();
            return true;
        }
        return Arrays.equals(publicKey.publicKey, key);
    }

   public void apply(byte[] key) {
        PublicKey publicKey = publicKeyTable.get(dbKey);
        if (publicKey == null) {
            publicKey = publicKeyTable.newEntity(dbKey);
        }
        if (publicKey.publicKey == null) {
            publicKey.publicKey = key;
            publicKeyTable.insert(publicKey);
        } else if (! Arrays.equals(publicKey.publicKey, key)) {
            throw new IllegalStateException("Public key mismatch");
        } else if (publicKey.height >= Conch.getBlockchain().getHeight() - 1) {
            PublicKey dbPublicKey = publicKeyTable.get(dbKey, false);
            if (dbPublicKey == null || dbPublicKey.publicKey == null) {
                publicKeyTable.insert(publicKey);
            }
        }
        if (publicKeyCache != null) {
            publicKeyCache.put(dbKey, key);
        }
        this.publicKey = publicKey;
    }

    public void addToAssetBalanceQNT(AccountLedger.LedgerEvent event, long eventId, long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityQNT;
        assetBalance = Math.addExact(assetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, assetBalance, 0);
        } else {
            accountAsset.quantityQNT = assetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, false)) {
            AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id, AccountLedger.LedgerHolding.ASSET_BALANCE, assetId,
                    quantityQNT, assetBalance));
        }
    }

    public void addToUnconfirmedAssetBalanceQNT(AccountLedger.LedgerEvent event, long eventId, long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, 0, unconfirmedAssetBalance);
        } else {
            accountAsset.unconfirmedQuantityQNT = unconfirmedAssetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, true)) {
            AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id,
                    AccountLedger.LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityQNT, unconfirmedAssetBalance));
        }
    }

    public void addToAssetAndUnconfirmedAssetBalanceQNT(AccountLedger.LedgerEvent event, long eventId, long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(this.id, assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityQNT;
        assetBalance = Math.addExact(assetBalance, quantityQNT);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(this.id, assetId, assetBalance, unconfirmedAssetBalance);
        } else {
            accountAsset.quantityQNT = assetBalance;
            accountAsset.unconfirmedQuantityQNT = unconfirmedAssetBalance;
        }
        accountAsset.save();
        listeners.notify(this, Event.ASSET_BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Event.UNCONFIRMED_ASSET_BALANCE);
        if (event == null) {
            return; // do not try to log ledger entry for FXT distribution
        }
        if (AccountLedger.mustLogEntry(this.id, true)) {
            AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id,
                    AccountLedger.LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityQNT, unconfirmedAssetBalance));
        }
        if (AccountLedger.mustLogEntry(this.id, false)) {
            AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id,
                    AccountLedger.LedgerHolding.ASSET_BALANCE, assetId,
                    quantityQNT, assetBalance));
        }
    }

    public void addToCurrencyUnits(AccountLedger.LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.units;
        currencyUnits = Math.addExact(currencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(this.id, currencyId, currencyUnits, 0);
        } else {
            accountCurrency.units = currencyUnits;
        }
        accountCurrency.save();
        listeners.notify(this, Event.CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.CURRENCY_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, false)) {
            AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id, AccountLedger.LedgerHolding.CURRENCY_BALANCE, currencyId,
                    units, currencyUnits));
        }
    }

    public void addToUnconfirmedCurrencyUnits(AccountLedger.LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
        unconfirmedCurrencyUnits = Math.addExact(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(this.id, currencyId, 0, unconfirmedCurrencyUnits);
        } else {
            accountCurrency.unconfirmedUnits = unconfirmedCurrencyUnits;
        }
        accountCurrency.save();
        listeners.notify(this, Event.UNCONFIRMED_CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.UNCONFIRMED_CURRENCY_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, true)) {
            AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id,
                    AccountLedger.LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
                    units, unconfirmedCurrencyUnits));
        }
    }

    public void addToCurrencyAndUnconfirmedCurrencyUnits(AccountLedger.LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(this.id, currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.units;
        currencyUnits = Math.addExact(currencyUnits, units);
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
        unconfirmedCurrencyUnits = Math.addExact(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(this.id, currencyId, currencyUnits, unconfirmedCurrencyUnits);
        } else {
            accountCurrency.units = currencyUnits;
            accountCurrency.unconfirmedUnits = unconfirmedCurrencyUnits;
        }
        accountCurrency.save();
        listeners.notify(this, Event.CURRENCY_BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.CURRENCY_BALANCE);
        currencyListeners.notify(accountCurrency, Event.UNCONFIRMED_CURRENCY_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, true)) {
            AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id,
                    AccountLedger.LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
                    units, unconfirmedCurrencyUnits));
        }
        if (AccountLedger.mustLogEntry(this.id, false)) {
            AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id,
                    AccountLedger.LedgerHolding.CURRENCY_BALANCE, currencyId,
                    units, currencyUnits));
        }
    }
    
    private long frozenAmountValidation(long amountNQT){
        if(amountNQT >= 0) {
            return amountNQT;
        }
        
        long absAmount = Math.abs(amountNQT);
        
        // freeze amount is 0
        if(this.frozenBalanceNQT == 0){
            Logger.logWarningMessage("[Ledger] Want to subtract the Account %s [id=%d]'s freeze amount %d, but current freeze amount is 0, don't subtract anything at height %d"
                    , Account.rsAccount(this.id), this.id, absAmount, Conch.getHeight());
            return 0;
        }

        // not enough freeze amount
        if(this.frozenBalanceNQT <= absAmount){
            Logger.logWarningMessage("[Ledger] Want to subtract the Account %s [id=%d]'s freeze amount %d, but current freeze amount isn't enough, just subtract the current freeze amount %d at height %d" 
                    , Account.rsAccount(this.id), this.id, absAmount, this.frozenBalanceNQT, Conch.getHeight());
            return -this.frozenBalanceNQT;
        }

        return amountNQT;
    }

    /**
     * - add frozen balance
     * - sub balance
     * - sub unconfirmed balance
     * - add guaranteed balance 
     * @param event
     * @param eventId
     * @param amountNQT
     */
    public void addFrozenSubBalanceSubUnconfirmed(AccountLedger.LedgerEvent event, long eventId, long amountNQT) {
        frozen(event,eventId,amountNQT,true);
    }

    /**
     * - add frozen balance
     * - sub balance
     * - add guaranteed balance 
     * @param event
     * @param eventId
     * @param amountNQT
     */
    public void addFrozenSubBalance(AccountLedger.LedgerEvent event, long eventId, long amountNQT) {
        frozen(event,eventId,amountNQT,false);
    }

    /**
     * - add frozen balance only 
     * @param event
     * @param eventId
     * @param amountNQT
     */
    public void addFrozen(AccountLedger.LedgerEvent event, long eventId, long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.frozenBalanceNQT = Math.addExact(this.frozenBalanceNQT, amountNQT);
        //addToGuaranteedBalanceNQT(amountNQT);
        checkAndSave();
    }

    /**
     * internal method used to record frozen, unconfirmed, guaranteed balance
     * @param event
     * @param eventId
     * @param amountNQT
     * @param changeUnconfirmed
     */
    private void frozen(AccountLedger.LedgerEvent event, long eventId, long amountNQT, boolean changeUnconfirmed){
        if (amountNQT == 0) {
            return;
        }

        amountNQT = frozenAmountValidation(amountNQT);
        
        this.balanceNQT = Math.subtractExact(this.balanceNQT, amountNQT);
        if(changeUnconfirmed) {
            this.unconfirmedBalanceNQT = Math.subtractExact(this.unconfirmedBalanceNQT, amountNQT);
        }
//        addGuaranteedBalance(-amountNQT);
        
        this.frozenBalanceNQT = Math.addExact(this.frozenBalanceNQT, amountNQT);
       
        checkAndSave();
    }
    
    private void checkAndSave() throws DoubleSpendingException {
        try{
            checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
            if (this.frozenBalanceNQT < 0) {
                throw new DoubleSpendingException("Negative frozen balance or quantity: ", this.id, this.balanceNQT, this.frozenBalanceNQT);
            }
            save();
        }catch(Account.DoubleSpendingException e) {
            if(!CheckSumValidator.isDirtyPoolTx(Conch.getHeight(), id)) {
                throw e;
            }
        }
    }

    public void addBalance(AccountLedger.LedgerEvent event, long eventId, long amountNQT) {
        addToBalanceNQT(event, eventId, amountNQT, 0);
    }

    public void addToBalanceNQT(AccountLedger.LedgerEvent event, long eventId, long amountNQT, long feeNQT) {
        addBalance(event,eventId,amountNQT,feeNQT,Event.BALANCE);
    }

    public void addToUnconfirmedNQT(AccountLedger.LedgerEvent event, long eventId, long amountNQT) {
        addUnconfirmed(event, eventId, amountNQT, 0);
    }
    
    public void pocChanged(){
        listeners.notify(this, Event.POC);
    }


    private long balanceAmountValidation(long amountNQT){
        if(!Constants.isTestnet()) {
            return amountNQT;
        }

        /* this.balanceNQT is null or 0 */
        if(amountNQT >= 0) {
            return amountNQT;
        }
        
        long absAmount = Math.abs(amountNQT);
        if(this.balanceNQT <= absAmount){
            Logger.logWarningMessage("[Ledger] Want to subtract the Account %s [id=%d]'s confirmed balance amount %d, but current confirmed balance isn't enough, just subtract the current balanceNQT %d at height %d"
                    , Account.rsAccount(this.id), this.id, absAmount, this.balanceNQT, Conch.getHeight());
            return -this.balanceNQT;
        }

        return amountNQT;
    }
    
    /**
     * internal method to add balance or unconfirmed balance.
     * different logic between balance and unconfirmed balance only is: whether add the guaranteed balance
     * @param event 
     * @param eventId 
     * @param amountNQT
     * @param feeNQT
     * @param balanceEvent Event.BALANCE or Event.UNCONFIRMED_BALANCE
     */
    private void addBalance(AccountLedger.LedgerEvent event, long eventId, long amountNQT, long feeNQT, Event balanceEvent){
        if (amountNQT == 0 && feeNQT == 0) {
            return;
        }

        amountNQT = balanceAmountValidation(amountNQT);
        
        long totalAmountNQT = Math.addExact(amountNQT, feeNQT);
        if(Event.BALANCE == balanceEvent){
            this.balanceNQT = Math.addExact(this.balanceNQT, totalAmountNQT);
            //add the guaranteed balance
            addGuaranteedBalance(totalAmountNQT);
        }else if(Event.UNCONFIRMED_BALANCE == balanceEvent){
            this.unconfirmedBalanceNQT = Math.addExact(this.unconfirmedBalanceNQT, totalAmountNQT);
        }
        checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
        save();

        listeners.notify(this, balanceEvent);

        boolean isUnconfirmed = false;
        // balance before accept the transfer amount
        long preBalance = 0;
        // balance after accept the transfer amount
        long postBalance = 0;
        AccountLedger.LedgerHolding holdingType = null;
        if(Event.BALANCE == balanceEvent){
            preBalance = this.balanceNQT - amountNQT;
            postBalance = this.balanceNQT;
            holdingType = AccountLedger.LedgerHolding.CONCH_BALANCE;
            isUnconfirmed = false;
        }else if(Event.UNCONFIRMED_BALANCE == balanceEvent){
            preBalance = this.unconfirmedBalanceNQT - amountNQT;
            postBalance = this.unconfirmedBalanceNQT;
            holdingType = AccountLedger.LedgerHolding.UNCONFIRMED_CONCH_BALANCE;
            isUnconfirmed = true;
        }
        if (AccountLedger.mustLogEntry(this.id, isUnconfirmed)) {
            if (feeNQT != 0) {
                AccountLedger.logEntry(new AccountLedger.LedgerEntry(AccountLedger.LedgerEvent.TRANSACTION_FEE, eventId, this.id,
                        holdingType, null, feeNQT, preBalance));
            }
            if (amountNQT != 0) {
                AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id, holdingType, null, amountNQT, postBalance));
            }
        }
    }

    public void addUnconfirmed(AccountLedger.LedgerEvent event, long eventId, long amountNQT, long feeNQT) {
        addBalance(event,eventId,amountNQT,feeNQT,Event.UNCONFIRMED_BALANCE);
    }

    public void addBalanceAddUnconfirmed(AccountLedger.LedgerEvent event, long eventId, long amountNQT) {
        addBalanceAddUnconfirmed(event, eventId, amountNQT, 0);
    }

    public void addBalanceAddUnconfirmed(AccountLedger.LedgerEvent event, long eventId, long amountNQT, long feeNQT) {
        if (amountNQT == 0 && feeNQT == 0) {
            return;
        }
        amountNQT = balanceAmountValidation(amountNQT);
        
        long totalAmountNQT = Math.addExact(amountNQT, feeNQT);
        this.balanceNQT = Math.addExact(this.balanceNQT, totalAmountNQT);
        this.unconfirmedBalanceNQT = Math.addExact(this.unconfirmedBalanceNQT, totalAmountNQT);
        addGuaranteedBalance(totalAmountNQT);
        checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
        save();
        listeners.notify(this, Event.BALANCE);
        listeners.notify(this, Event.UNCONFIRMED_BALANCE);
        if (AccountLedger.mustLogEntry(this.id, true)) {
            if (feeNQT != 0) {
                AccountLedger.logEntry(new AccountLedger.LedgerEntry(AccountLedger.LedgerEvent.TRANSACTION_FEE, eventId, this.id,
                        AccountLedger.LedgerHolding.UNCONFIRMED_CONCH_BALANCE, null, feeNQT, this.unconfirmedBalanceNQT - amountNQT));
            }
            if (amountNQT != 0) {
                AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id,
                        AccountLedger.LedgerHolding.UNCONFIRMED_CONCH_BALANCE, null, amountNQT, this.unconfirmedBalanceNQT));
            }
        }
        if (AccountLedger.mustLogEntry(this.id, false)) {
            if (feeNQT != 0) {
                AccountLedger.logEntry(new AccountLedger.LedgerEntry(AccountLedger.LedgerEvent.TRANSACTION_FEE, eventId, this.id,
                        AccountLedger.LedgerHolding.CONCH_BALANCE, null, feeNQT, this.balanceNQT - amountNQT));
            }
            if (amountNQT != 0) {
                AccountLedger.logEntry(new AccountLedger.LedgerEntry(event, eventId, this.id,
                        AccountLedger.LedgerHolding.CONCH_BALANCE, null, amountNQT, this.balanceNQT));
            }
        }
    }

    public void addMintedBalance(long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        this.forgedBalanceNQT = Math.addExact(this.forgedBalanceNQT, amountNQT);
        save();
    }

    private static void checkBalance(long accountId, long confirmed, long unconfirmed) {
        if (accountId == SharderGenesis.CREATOR_ID) {
            return;
        }
        if (confirmed < 0) {
            throw new DoubleSpendingException("Negative balance or quantity: ", accountId, confirmed, unconfirmed);
        }
        if (unconfirmed < 0) {
            throw new DoubleSpendingException("Negative unconfirmed balance or quantity: ", accountId, confirmed, unconfirmed);
        }
        if (unconfirmed > confirmed) {
            throw new DoubleSpendingException("Unconfirmed exceeds confirmed balance or quantity: ", accountId, confirmed, unconfirmed);
        }
    }

    private void addGuaranteedBalance(long amountNQT) {
        if (amountNQT <= 0) {
            return;
        }
        int blockchainHeight = Conch.getHeight();
        boolean isInTx = Db.db.isInTransaction();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmtSelect = con.prepareStatement("SELECT additions FROM account_guaranteed_balance "
                    + "WHERE account_id = ? and height = ?");
            PreparedStatement pstmtUpdate = con.prepareStatement("MERGE INTO account_guaranteed_balance (account_id, "
                    + " additions, height) KEY (account_id, height) VALUES(?, ?, ?)");

//            PreparedStatement pstmtUpdateLatest = null;
//            if(Constants.TRIM_AT_INSERT){
//                pstmtUpdateLatest = con.prepareStatement("DELETE FROM account_guaranteed_balance " +
//                        "WHERE account_id = ? AND height < ?");
//            }else{
//                pstmtUpdateLatest = con.prepareStatement("UPDATE account_guaranteed_balance SET "
//                        + "latest = false WHERE account_id = ? AND height < ?");
//            }

            pstmtSelect.setLong(1, this.id);
            pstmtSelect.setInt(2, blockchainHeight);

            try (ResultSet rs = pstmtSelect.executeQuery()) {
                long additions = amountNQT;
                if (rs.next()) {
                    additions = Math.addExact(additions, rs.getLong("additions"));
                }
                pstmtUpdate.setLong(1, this.id);
                pstmtUpdate.setLong(2, additions);
                pstmtUpdate.setInt(3, blockchainHeight);
                pstmtUpdate.executeUpdate();

//                pstmtUpdateLatest.setLong(1, this.id);
//                pstmtUpdateLatest.setInt(2, blockchainHeight);
//                pstmtUpdateLatest.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
    }

    public void payDividends(final long transactionId, Attachment.ColoredCoinsDividendPayment attachment) {
        long totalDividend = 0;
        List<AccountAsset> accountAssets = new ArrayList<>();
        boolean isInTx = Db.db.isInTransaction();
        DbIterator<AccountAsset> iterator = null;
        try {
            iterator = getAssetAccounts(attachment.getAssetId(), attachment.getHeight(), 0, -1);
            while (iterator.hasNext()) {
                accountAssets.add(iterator.next());
            }
        }finally {
            if (!isInTx) {
                DbUtils.close(iterator);
            }
        }
        final long amountNQTPerQNT = attachment.getAmountNQTPerQNT();
        long numAccounts = 0;
        for (final AccountAsset accountAsset : accountAssets) {
            if (accountAsset.getAccountId() != this.id && accountAsset.getQuantityQNT() != 0) {
                long dividend = Math.multiplyExact(accountAsset.getQuantityQNT(), amountNQTPerQNT);
                Account.getAccount(accountAsset.getAccountId())
                        .addBalanceAddUnconfirmed(AccountLedger.LedgerEvent.ASSET_DIVIDEND_PAYMENT, transactionId, dividend);
                totalDividend += dividend;
                numAccounts += 1;
            }
        }
        this.addBalance(AccountLedger.LedgerEvent.ASSET_DIVIDEND_PAYMENT, transactionId, -totalDividend);
        AssetDividend.addAssetDividend(transactionId, attachment, totalDividend, numAccounts);
    }

    private static final String AUTO_POC_TX_ADDR = Conch.getStringProperty("sharder.autoTransactionAddress");
    public static void checkApiAutoTxAccount(String address) throws ConchException.AccountControlException {
        String correctRs = Optional.ofNullable(AUTO_POC_TX_ADDR)
                .orElseThrow(() -> new ConchException.AccountControlException("auto transaction address not configured! please set sharder.autoTransactionAddress configuration"));
        if (!correctRs.equals(address)) {
            throw new ConchException.AccountControlException("Auto Poc Tx account address does not match! please reset sharder.autoTransactionAddress configuration");
        }
//        Logger.logInfoMessage("[ OK ] Auto Poc Tx account address (" + correctRs + ") is correct!");
    }

    @Override
    public String toString() {
        return "Account " + Long.toUnsignedString(getId());
    }


    public static void syncAccountTable(Connection con, String sourceTable, String targetTable, int dif) {
        boolean closeCon = false;
        try {
            if (con == null) {
                con = Db.db.getConnection();
                closeCon = true;
            }
            long t1 = System.currentTimeMillis();
            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String idQuerySql = "SELECT distinct id" + " FROM " + sourceTable;
            ResultSet accountIdRs = statement.executeQuery(idQuerySql);
            while (accountIdRs.next()) {
                long accountId = accountIdRs.getLong("id");
                PreparedStatement pstmtSelectWork =
                 con.prepareStatement("SELECT max(HEIGHT) height FROM " + sourceTable + " where id = " + accountId);
                PreparedStatement pstmtSelectHistory =
                 con.prepareStatement("SELECT max(HEIGHT) height FROM " + targetTable + " where id = " + accountId);
                PreparedStatement pstmtSelect = con.prepareStatement("SELECT DB_ID,ID,BALANCE,UNCONFIRMED_BALANCE," +
                 "FORGED_BALANCE," +
                        "ACTIVE_LESSEE_ID,HAS_CONTROL_PHASING,HEIGHT,LATEST,FROZEN_BALANCE FROM " + sourceTable
                        + " WHERE height > ? and height < ? and id = ?");

    /*
                PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account (ID,BALANCE,UNCONFIRMED_BALANCE,FORGED_BALANCE," +
                        "ACTIVE_LESSEE_ID,HAS_CONTROL_PHASING,HEIGHT,LATEST,FROZEN_BALANCE) KEY (account_id, height) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
    */

                ResultSet workHeightRs = pstmtSelectWork.executeQuery();
                ResultSet heightRs = pstmtSelectHistory.executeQuery();
                if (!workHeightRs.next()) {
                    throw new RuntimeException("table " + sourceTable + " data not exist!");
                }
                int workHeight = workHeightRs.getInt("height");
                int floorHeight = heightRs.next() ? heightRs.getInt("height") : 0;
                //Logger.logDebugMessage("table " + targetTable + " sync block height:" + floorHeight);
                int ceilingHeight = workHeight - dif;
//                if (workHeight - floorHeight > Constants.SYNC_BLOCK_NUM) {
//                    ceilingHeight = floorHeight + Constants.SYNC_BLOCK_NUM;
//                }

                if (workHeight - floorHeight < dif) {
//                    return;
                    continue;
                }
                pstmtSelect.setInt(1, floorHeight);
                pstmtSelect.setInt(2, ceilingHeight);
                pstmtSelect.setLong(3, accountId);
                ResultSet resultSet = pstmtSelect.executeQuery();
                if (!resultSet.next()) {
                    continue;
                }
                StringBuilder sb = new StringBuilder("INSERT INTO " + targetTable + " (DB_ID,ID,BALANCE,UNCONFIRMED_BALANCE,FORGED_BALANCE," +
                        "ACTIVE_LESSEE_ID,HAS_CONTROL_PHASING,HEIGHT,LATEST,FROZEN_BALANCE) VALUES");
                do {
                    sb.append("(").append(resultSet.getLong("db_id")).append(",");
                    sb.append(resultSet.getLong("id")).append(",");
                    sb.append(resultSet.getLong("balance")).append(",");
                    sb.append(resultSet.getLong("unconfirmed_balance")).append(",");
                    sb.append(resultSet.getLong("forged_balance")).append(",");
                    sb.append(resultSet.getLong("active_lessee_id")).append(",");
                    sb.append(resultSet.getBoolean("has_control_phasing")).append(",");
                    sb.append(resultSet.getInt("height")).append(",");
                    sb.append(resultSet.getBoolean("latest")).append(",");
                    sb.append(resultSet.getLong("frozen_balance")).append(")").append(",");
                } while (resultSet.next());
                sb.deleteCharAt(sb.length() - 1);
                PreparedStatement pstmtInsert = con.prepareStatement(sb.toString());
                pstmtInsert.execute();
                PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + sourceTable + " "
                        + "WHERE height <= ? and id = ?");
                pstmtDelete.setInt(1, ceilingHeight);
                //pstmtDelete.setBoolean(2, false);
                pstmtDelete.setLong(2, accountId);
                pstmtDelete.execute();
            }
            Logger.logDebugMessage("sync time:" + (System.currentTimeMillis() - t1));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (closeCon) {
                DbUtils.close(con);
            }
        }
    }


    public static void syncAccountGuaranteedBalanceTable(Connection con, String sourceTable, String targetTable,
     int dif) {
        boolean closeCon = false;
        try {
            if (con == null) {
                con = Db.db.getConnection();
                closeCon = true;
            }
            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String idQuerySql = "SELECT distinct ACCOUNT_ID" + " FROM " + sourceTable;
            ResultSet accountIdRs = statement.executeQuery(idQuerySql);
            while (accountIdRs.next()) {
                long accountId = accountIdRs.getLong("ACCOUNT_ID");
                PreparedStatement pstmtSelectWork =
                 con.prepareStatement("SELECT max(HEIGHT) height FROM " + sourceTable + " where account_id = " + accountId);
                PreparedStatement pstmtSelectHistory =
                 con.prepareStatement("SELECT max(HEIGHT) height FROM " + targetTable + " where account_id = " + accountId);
                PreparedStatement pstmtSelect = con.prepareStatement("SELECT DB_ID,ACCOUNT_ID,ADDITIONS,HEIGHT,LATEST" +
                        " FROM " + sourceTable + " WHERE height > ? and height < ? and account_id = ?");

/*
            PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account (ID,BALANCE,UNCONFIRMED_BALANCE,FORGED_BALANCE," +
                    "ACTIVE_LESSEE_ID,HAS_CONTROL_PHASING,HEIGHT,LATEST,FROZEN_BALANCE) KEY (account_id, height) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
*/

                ResultSet workHeightRs = pstmtSelectWork.executeQuery();
                ResultSet heightRs = pstmtSelectHistory.executeQuery();
                if (!workHeightRs.next()) {
                    throw new RuntimeException("table " + sourceTable + " data not exist!");
                }
                int workHeight = workHeightRs.getInt("height");
                int floorHeight = heightRs.next() ? heightRs.getInt("height") : 0;
                //Logger.logDebugMessage("table " + targetTable + " sync block height:" + floorHeight);

                int ceilingHeight = workHeight - dif;
//                if (workHeight - floorHeight > Constants.SYNC_BLOCK_NUM) {
//                    ceilingHeight = floorHeight + Constants.SYNC_BLOCK_NUM - dif;
//                }
                if (workHeight - floorHeight < dif) {
//                    return;
                    continue;
                }
                pstmtSelect.setInt(1, floorHeight);
                pstmtSelect.setInt(2, ceilingHeight);
                pstmtSelect.setLong(3, accountId);
                ResultSet resultSet = pstmtSelect.executeQuery();
                if (!resultSet.next()) {
                    continue;
                }
                StringBuilder sb = new StringBuilder("INSERT INTO " + targetTable + " (DB_ID,ACCOUNT_ID,ADDITIONS,HEIGHT,LATEST) VALUES");
                do {
                    sb.append("(").append(resultSet.getLong("db_id")).append(",");
                    sb.append(resultSet.getLong("account_id")).append(",");
                    sb.append(resultSet.getLong("additions")).append(",");
                    sb.append(resultSet.getInt("height")).append(",");
                    sb.append(resultSet.getBoolean("latest")).append(")").append(",");
                } while (resultSet.next());
                sb.deleteCharAt(sb.length() - 1);
                PreparedStatement pstmtInsert = con.prepareStatement(sb.toString());
                pstmtInsert.execute();
                PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + sourceTable
                        + " WHERE height <= ? and account_id = ?");
                pstmtDelete.setInt(1, Math.min(ceilingHeight, Conch.getHeight() - Constants.GUARANTEED_BALANCE_CONFIRMATIONS));
                //pstmtDelete.setBoolean(2, false);
                pstmtDelete.setLong(2, accountId);
                pstmtDelete.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (closeCon) {
                DbUtils.close(con);
            }
        }
    }


    public static void syncAccountLedgerTable(Connection con, String sourceTable, String targetTable, int dif) {
        boolean closeCon = false;
        try {
            if (con == null) {
                con = Db.db.getConnection();
                closeCon = true;
            }
            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String idQuerySql = "SELECT distinct ACCOUNT_ID" + " FROM " + sourceTable;
            ResultSet accountIdRs = statement.executeQuery(idQuerySql);
            while (accountIdRs.next()) {
                long accountId = accountIdRs.getLong("ACCOUNT_ID");
                PreparedStatement pstmtSelectWork =
                 con.prepareStatement("SELECT max(HEIGHT) height FROM " + sourceTable + " where account_id = " + accountId);
                PreparedStatement pstmtSelectHistory =
                 con.prepareStatement("SELECT max(HEIGHT) height FROM " + targetTable + " where account_id = " + accountId);
                PreparedStatement pstmtSelect = con.prepareStatement("SELECT DB_ID,ACCOUNT_ID,EVENT_TYPE,EVENT_ID," +
                 "HOLDING_TYPE," +
                        "HOLDING_ID,CHANGE,BALANCE,BLOCK_ID,HEIGHT,TIMESTAMP,LATEST FROM " + sourceTable
                        + " WHERE height > ? and height < ? and account_id = ?");

/*
            PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account (ID,BALANCE,UNCONFIRMED_BALANCE,FORGED_BALANCE," +
                    "ACTIVE_LESSEE_ID,HAS_CONTROL_PHASING,HEIGHT,LATEST,FROZEN_BALANCE) KEY (account_id, height) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
*/

                ResultSet workHeightRs = pstmtSelectWork.executeQuery();
                ResultSet heightRs = pstmtSelectHistory.executeQuery();
                if (!workHeightRs.next()) {
                    throw new RuntimeException("table " + sourceTable + " data not exist!");
                }
                int workHeight = workHeightRs.getInt("height");
                int floorHeight = heightRs.next() ? heightRs.getInt("height") : 0;
                //Logger.logDebugMessage("table " + targetTable + " sync block height:" + floorHeight);

                int ceilingHeight = workHeight - dif;
//                if (workHeight - floorHeight > Constants.SYNC_BLOCK_NUM) {
//                    ceilingHeight = floorHeight + Constants.SYNC_BLOCK_NUM;
//                }

                if (workHeight - floorHeight < dif) {
//                    return;
                    continue;
                }
                pstmtSelect.setInt(1, floorHeight);
                pstmtSelect.setInt(2, ceilingHeight);
                pstmtSelect.setLong(3, accountId);
                ResultSet resultSet = pstmtSelect.executeQuery();
                if (!resultSet.next()) {
                    continue;
                }
                StringBuilder sb = new StringBuilder("INSERT INTO " + targetTable + " (DB_ID,ACCOUNT_ID,EVENT_TYPE,EVENT_ID,HOLDING_TYPE," +
                        "HOLDING_ID,CHANGE,BALANCE,BLOCK_ID,HEIGHT,TIMESTAMP,LATEST) VALUES");
                do {
                    sb.append("(").append(resultSet.getLong("DB_ID")).append(",");
                    sb.append(resultSet.getLong("ACCOUNT_ID")).append(",");
                    sb.append(resultSet.getInt("EVENT_TYPE")).append(",");
                    sb.append(resultSet.getLong("EVENT_ID")).append(",");
                    sb.append(resultSet.getInt("HOLDING_TYPE")).append(",");
                    sb.append(resultSet.getLong("HOLDING_ID")).append(",");
                    sb.append(resultSet.getLong("CHANGE")).append(",");
                    sb.append(resultSet.getLong("BALANCE")).append(",");
                    sb.append(resultSet.getLong("BLOCK_ID")).append(",");
                    sb.append(resultSet.getInt("HEIGHT")).append(",");
                    sb.append(resultSet.getInt("TIMESTAMP")).append(",");
                    sb.append(resultSet.getBoolean("LATEST")).append(")").append(",");
                } while (resultSet.next());
                sb.deleteCharAt(sb.length() - 1);
                PreparedStatement pstmtInsert = con.prepareStatement(sb.toString());
                pstmtInsert.execute();
                PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + sourceTable
                        + " WHERE height <= ? and account_id = ?");
                pstmtDelete.setInt(1, ceilingHeight);
                //pstmtDelete.setBoolean(2, false);
                pstmtDelete.setLong(2, accountId);
                pstmtDelete.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (closeCon) {
                DbUtils.close(con);
            }
        }
    }


    public static void syncAccountPocScoreTable(Connection con, String sourceTable, String targetTable, int dif) {
        boolean closeCon = false;
        try {
            if (con == null) {
                con = Db.db.getConnection();
                closeCon = true;
            }
            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            String idQuerySql = "SELECT distinct ACCOUNT_ID" + " FROM " + sourceTable;
            ResultSet accountIdRs = statement.executeQuery(idQuerySql);
            while (accountIdRs.next()) {
                long accountId = accountIdRs.getLong("ACCOUNT_ID");
                PreparedStatement pstmtSelectWork =
                 con.prepareStatement("SELECT max(HEIGHT) height FROM " + sourceTable + " where account_id = " + accountId);
                PreparedStatement pstmtSelectHistory =
                 con.prepareStatement("SELECT max(HEIGHT) height FROM " + targetTable + " where account_id = " + accountId);
                PreparedStatement pstmtSelect = con.prepareStatement("SELECT DB_ID,ACCOUNT_ID,POC_SCORE,HEIGHT,POC_DETAIL,LATEST" +
                        " FROM " + sourceTable
                        + " WHERE height > ? and height < ? and account_id = ?");

/*
            PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account (ID,BALANCE,UNCONFIRMED_BALANCE,FORGED_BALANCE," +
                    "ACTIVE_LESSEE_ID,HAS_CONTROL_PHASING,HEIGHT,LATEST,FROZEN_BALANCE) KEY (account_id, height) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
*/

                ResultSet workHeightRs = pstmtSelectWork.executeQuery();
                ResultSet heightRs = pstmtSelectHistory.executeQuery();
                if (!workHeightRs.next()) {
                    throw new RuntimeException("table " + sourceTable + " data not exist!");
                }
                int workHeight = workHeightRs.getInt("height");
                int floorHeight = heightRs.next() ? heightRs.getInt("height") : 0;
                //Logger.logDebugMessage("table " + targetTable + " sync block height:" + floorHeight);
                int ceilingHeight = workHeight - dif;
//                if (workHeight - floorHeight > Constants.SYNC_BLOCK_NUM) {
//                    ceilingHeight = floorHeight + Constants.SYNC_BLOCK_NUM;
//                }

                if (workHeight - floorHeight < dif) {
//                    return;
                    continue;
                }
                pstmtSelect.setInt(1, floorHeight);
                pstmtSelect.setInt(2, ceilingHeight);
                pstmtSelect.setLong(3, accountId);
                ResultSet resultSet = pstmtSelect.executeQuery();
                if (!resultSet.next()) {
                    continue;
                }
                StringBuilder sb = new StringBuilder("INSERT INTO " + targetTable + " (DB_ID,ACCOUNT_ID,POC_SCORE,HEIGHT,POC_DETAIL,LATEST) VALUES");
                do {
                    sb.append("(").append(resultSet.getLong("DB_ID")).append(",");
                    sb.append(resultSet.getLong("ACCOUNT_ID")).append(",");
                    sb.append(resultSet.getLong("POC_SCORE")).append(",");
                    sb.append(resultSet.getInt("HEIGHT")).append(",");
                    sb.append("'").append(resultSet.getString("POC_DETAIL")).append("',");
                    sb.append(resultSet.getBoolean("LATEST")).append(")").append(",");
                } while (resultSet.next());
                sb.deleteCharAt(sb.length() - 1);
                PreparedStatement pstmtInsert = con.prepareStatement(sb.toString());
                pstmtInsert.execute();
                PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + sourceTable
                        + " WHERE height <= ? and account_id = ?");
                pstmtDelete.setInt(1, ceilingHeight);
                //pstmtDelete.setBoolean(2, false);
                pstmtDelete.setLong(2, accountId);
                pstmtDelete.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (closeCon) {
                DbUtils.close(con);
            }
        }
    }

    public static void trimHistoryData(int height){
        DbTrimUtils.trimTables(height,"ACCOUNT_HISTORY", "ACCOUNT_GUARANTEED_BALANCE_HISTORY", "ACCOUNT_POC_SCORE_HISTORY");
    }

    public static boolean needCompact = true;
    public static void truncateHistoryData(){
        if(!Constants.HISTORY_RECORD_CLEAR) {
            return;
        }
        long clearStartMS = System.currentTimeMillis();

        try (Connection con = Db.db.beginTransaction()){
            Statement stmt = con.createStatement();
            Logger.logMessage("[HistoryRecords] Truncate tables [ACCOUNT_LEDGER]");
            stmt.executeUpdate("TRUNCATE TABLE ACCOUNT_LEDGER");

//            Logger.logMessage("[HistoryRecords] Truncate tables [ACCOUNT_POC_SCORE_HISTORY,]");
//            stmt.executeUpdate("TRUNCATE TABLE ACCOUNT_POC_SCORE_HISTORY");
//
//            Logger.logMessage("[HistoryRecords] Truncate tables [ACCOUNT_GUARANTEED_BALANCE_HISTORY]");
//            stmt.executeUpdate("TRUNCATE TABLE ACCOUNT_GUARANTEED_BALANCE_HISTORY");
//
//            Logger.logMessage("[HistoryRecords] Truncate tables [ACCOUNT_HISTORY]");
//            stmt.executeUpdate("TRUNCATE TABLE ACCOUNT_HISTORY");

            stmt.executeUpdate("ALTER TABLE ACCOUNT_GUARANTEED_BALANCE_HISTORY ADD COLUMN IF NOT EXISTS latest BOOLEAN NOT NULL DEFAULT false");
            Db.db.commitTransaction();
        } catch(Exception e){
            Db.db.rollbackTransaction();
            Logger.logWarningMessage("[HistoryRecords] Truncate occur error[%s], rollback and break", e.getMessage());
        }finally {
            Db.db.endTransaction();
        }
        Logger.logMessage(String.format("[HistoryRecords] Finished to clear history records, used %d S",(System.currentTimeMillis() - clearStartMS) / 1000));

//        if(needCompact) {
//            Logger.logMessage("[HistoryRecords] Compact the current db");
//            int code = CompactDatabase.compactAndRestoreDB();
//            if(code != 2) {
//                Logger.logInfoMessage("[HistoryRecords] You need restart the client to finish the compact & restore db");
//                Conch.shutdown();
//            }
//        }
    }

    /**
     * Migrate the all account's records to working table and cache table
     * latest height record to working table
     * others to cache table
     */
    public static void migrateHistoryData(){
//        String[] dataArr = {"ACCOUNT", "ACCOUNT_LEDGER", "ACCOUNT_GUARANTEED_BALANCE", "ACCOUNT_POC_SCORE"};
        String[] dataArr = {"ACCOUNT", "ACCOUNT_GUARANTEED_BALANCE", "ACCOUNT_POC_SCORE"};
        Logger.logInfoMessage("[HistoryRecords] Migrate history data to working and cache table " + Arrays.toString(dataArr) + ", it will take a few minutes...");

        long startMS = System.currentTimeMillis();
        int migrationSize = 18; //Constants.MAX_ROLLBACK;
        try (Connection con = Db.db.beginTransaction()){
            for (String table : dataArr) {
                String historyTable = table + "_HISTORY";
                String cacheTable = table + "_CACHE";
                String idColumn;
                if ("ACCOUNT".equalsIgnoreCase(table)) {
                    idColumn = "ID";
                } else {
                    idColumn = "ACCOUNT_ID";
                }
                Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                String idQuerySql = "SELECT distinct " + idColumn + " FROM " + historyTable;
                Logger.logDebugMessage("[HistoryRecords] %s", idQuerySql);

                ResultSet accountIdRs = statement.executeQuery(idQuerySql);
                accountIdRs.last();
                int totalAccountCount = accountIdRs.getRow();
                accountIdRs.beforeFirst();
                Logger.logInfoMessage("[HistoryRecords] Migrate %d unique account's records from %s to %s", totalAccountCount, historyTable, table);

                int accountMigrateCount = 0;
                int totalMigrateCount = 0;
                while (accountIdRs.next()) {
                    // migrate all account's records from history table to working & cache tables
                    long accountId = accountIdRs.getLong(idColumn);
                    try{
                        PreparedStatement maxHeightStmt = con.prepareStatement("SELECT max(HEIGHT) maxHeight FROM " + historyTable + " WHERE " + idColumn + " = ?");
                        maxHeightStmt.setLong(1, accountId);
                        ResultSet idSet = maxHeightStmt.executeQuery();
                        if (idSet.next()) {
                            int maxHeight = idSet.getInt("maxHeight");
                            int migrationStartHeight = maxHeight - migrationSize;

                            //TODO 增加目标表里记录的检查逻辑，如果目标表的记录超过了 Constants.SYNC_CACHE_BLOCK_NUM，就无需进行数据迁移和同步


                            Statement selectStmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                            String recordsQuerySql = String.format("SELECT * FROM %s WHERE HEIGHT >= %d and %s = %d ORDER BY HEIGHT DESC", historyTable, migrationStartHeight, idColumn, accountId);

                            ResultSet data = selectStmt.executeQuery(recordsQuerySql);
                            data.last();
                            int totalRecordsCount = data.getRow();
                            data.beforeFirst();
//                            Logger.logDebugMessage("[HistoryRecords] Migrate account[%d] %d records from %s to %s where height >= %d", accountId, totalRecordsCount, historyTable, table, migrationStartHeight);

                            // migrate single account's records: first to working table, others to cache table
                            int historyDataMigrateCount = 0;
                            while (data != null && data.next()) {
                                historyDataMigrateCount++;
                                String migrationTable = (historyDataMigrateCount == 1) ? table : cacheTable;

                                try{
                                    ResultSetMetaData metaData = data.getMetaData();
                                    int columnCount = metaData.getColumnCount();
                                    StringBuilder insert = new StringBuilder();
                                    StringBuilder values = new StringBuilder();
                                    for (int i = 1; i <= columnCount; i++) {
                                        if (i == 1) {
                                            insert.append("insert into " + migrationTable + " (");
                                            insert.append(metaData.getColumnName(i)).append(",");
                                            values.append("values (").append("?,");
                                        } else if (1 < i && i < columnCount) {
                                            insert.append(metaData.getColumnName(i)).append(",");
                                            values.append("?,");
                                        } else {
                                            insert.append(metaData.getColumnName(i)).append(")");
                                            values.append("?)");
                                        }
                                    }
                                    PreparedStatement insertStmt = con.prepareStatement(insert.append(values).toString());
                                    for (int i = 1; i <= columnCount; i++) {
                                        if ("latest".equalsIgnoreCase(metaData.getColumnName(i))) {
                                            insertStmt.setObject(i, true);
                                        } else {
                                            insertStmt.setObject(i, data.getObject(i));
                                        }
                                    }
                                    insertStmt.executeUpdate();
                                } catch(Exception e){
                                    Logger.logWarningMessage("[HistoryRecords] Migration account[%d]'s records[from %s to %s] occur error[%s], ignore and process next", accountId, historyTable, migrationTable, e.getMessage());
                                }
                            }
                            // single account's history records migration finished
                            totalMigrateCount += historyDataMigrateCount;
                            Logger.logInfoMessage("[HistoryRecords] Migrate account[%d]'s records %d finished [from %s to %s]", accountId, historyDataMigrateCount,  historyTable, table);
                        }

                        if(accountMigrateCount++ % 1000 == 0) {
                            Logger.logDebugMessage("[HistoryRecords] Migrated %d records and progress is %d/%d accounts [from %s to %s]", totalMigrateCount, accountMigrateCount, totalAccountCount, historyTable, table);
                        }
                    }catch(Exception e){
                        Logger.logWarningMessage("[HistoryRecords] Migration account[%d] occur error[%s], ignore and process next", accountId,  e.getMessage());
                    }
                }
            }
            Db.db.commitTransaction();
        } catch (SQLException throwable) {
            Db.db.rollbackTransaction();
            Logger.logMessage(String.format("[HistoryRecords] Migrate history records occur error %s", throwable.getMessage()));
        }finally {
            Db.db.endTransaction();
        }
        long usedS= (System.currentTimeMillis() - startMS) / 1000;
        long usedM= usedS / 60;
        Logger.logMessage(String.format("[HistoryRecords] Migrate history records[%d height] used %d Minutes(%d S)", migrationSize, usedM, usedS));
    }

}
