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

import org.conch.Conch;
import org.conch.chain.Block;
import org.conch.chain.Blockchain;
import org.conch.chain.BlockchainProcessor;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.db.DerivedDbTable;
import org.conch.util.Listener;
import org.conch.util.Listeners;
import org.conch.util.Logger;

import java.sql.*;
import java.util.*;

/**
 * Maintain a ledger of changes to selected accounts
 */
public class AccountLedger {

    /** Account ledger is enabled */
    private static final boolean ledgerEnabled;

    /** Track all accounts */
    private static final boolean trackAllAccounts;

    /** Accounts to track */
    private static final SortedSet<Long> trackAccounts = new TreeSet<>();

    /** Unconfirmed logging */
    private static final int logUnconfirmed;

    /** Number of blocks to keep when trimming */
    public static final int trimKeep = Conch.getIntProperty("sharder.ledgerTrimKeep", 30000);

    /** Blockchain */
    private static final Blockchain blockchain = Conch.getBlockchain();

    /** Blockchain processor */
    private static final BlockchainProcessor blockchainProcessor = Conch.getBlockchainProcessor();

    /** Pending ledger entries */
    private static final List<LedgerEntry> pendingEntries = new ArrayList<>();

    /**
     * Process org.conch.ledgerAccounts
     */
    static {
        List<String> ledgerAccounts = Conch.getStringListProperty("sharder.ledgerAccounts");
        ledgerEnabled = !ledgerAccounts.isEmpty();
        trackAllAccounts = ledgerAccounts.contains("*");
        if (ledgerEnabled) {
            if (trackAllAccounts) {
                Logger.logInfoMessage("Account ledger is tracking all accounts");
            } else {
                for (String account : ledgerAccounts) {
                    try {
                        trackAccounts.add(Account.rsAccountToId(account));
                        Logger.logInfoMessage("Account ledger is tracking account " + account);
                    } catch (RuntimeException e) {
                        Logger.logErrorMessage("Account " + account + " is not valid; ignored");
                    }
                }
            }
        } else {
            Logger.logInfoMessage("Account ledger is not enabled");
        }
        int temp = Conch.getIntProperty("sharder.ledgerLogUnconfirmed", 1);
        logUnconfirmed = (temp >= 0 && temp <= 2 ? temp : 1);
    }

    /**
     * Account ledger table
     */
    private static class AccountLedgerTable extends DerivedDbTable {

        /**
         * Create the account ledger table
         */
        public AccountLedgerTable() {
            super("account_ledger");
        }

        /**
         * Insert an entry into the table
         *
         * @param   ledgerEntry             Ledger entry
         */
        public void insert(LedgerEntry ledgerEntry) {
            try (Connection con = db.getConnection()) {
                ledgerEntry.save(con);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        /**
         * Trim the account ledger table
         *
         * @param   height                  Trim height
         */
        @Override
        public void trim(int height) {
            if(Constants.SYNC_BUTTON){
                return;
            }

            if (trimKeep <= 0) {
                return;
            }
            int trimHeight = Math.max(height - trimKeep, 0);
            _trim("account_ledger", trimHeight, false);
        }

        @Override
        public void rollback(int height) {
            super.rollback(height);
        }
    }
    private static final AccountLedgerTable accountLedgerTable = new AccountLedgerTable();

    /**
     * Initialization
     *
     * We don't do anything but we need to be called from Conch.init() in order to
     * register our table
     */
    public static void init() {
    }

    /**
     * Account ledger listener events
     */
    public enum Event {
        ADD_ENTRY
    }

    /**
     * Account ledger listeners
     */
    private static final Listeners<LedgerEntry, Event> listeners = new Listeners<>();

    /**
     * Add a listener
     *
     * @param   listener                    Listener
     * @param   eventType                   Event to listen for
     * @return                              True if the listener was added
     */
    public static boolean addListener(Listener<LedgerEntry> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    /**
     * Remove a listener
     *
     * @param   listener                    Listener
     * @param   eventType                   Event to listen for
     * @return                              True if the listener was removed
     */
    public static boolean removeListener(Listener<LedgerEntry> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static boolean mustLogEntry(long accountId, boolean isUnconfirmed) {
        //
        // Must be tracking this account
        //
        if (!ledgerEnabled || (!trackAllAccounts && !trackAccounts.contains(accountId))) {
            return false;
        }
        // confirmed changes only occur while processing block, and unconfirmed changes are
        // only logged while processing block
        if (!blockchainProcessor.isProcessingBlock()) {
            return false;
        }
        //
        // Log unconfirmed changes only when processing a block and logUnconfirmed does not equal 0
        // Log confirmed changes unless logUnconfirmed equals 2
        //
        if (isUnconfirmed && logUnconfirmed == 0) {
            return false;
        }
        if (!isUnconfirmed && logUnconfirmed == 2) {
            return false;
        }
        if (trimKeep > 0 && blockchain.getHeight() <= Constants.LAST_KNOWN_BLOCK - trimKeep) {
            return false;
        }
        //
        // Don't log account changes if we are scanning the blockchain and the current height
        // is less than the minimum account_ledger trim height
        //
        if (blockchainProcessor.isScanning() && trimKeep > 0 &&
                blockchain.getHeight() <= blockchainProcessor.getInitialScanHeight() - trimKeep) {
            return false;
        }
        return true;
    }

    /**
     * Log an event in the account_ledger table
     *
     * @param   ledgerEntry                 Ledger entry
     */
    public static void logEntry(LedgerEntry ledgerEntry) {
        //
        // Must be in a database transaction
        //
        if (!Db.db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        //
        // Combine multiple ledger entries
        //
        int index = pendingEntries.indexOf(ledgerEntry);
        if (index >= 0) {
            LedgerEntry existingEntry = pendingEntries.remove(index);
            ledgerEntry.updateChange(existingEntry.getChange());
            long adjustedBalance = existingEntry.getBalance() - existingEntry.getChange();
            for (; index < pendingEntries.size(); index++) {
                existingEntry = pendingEntries.get(index);
                if (existingEntry.getAccountId() == ledgerEntry.getAccountId() &&
                        existingEntry.getHolding() == ledgerEntry.getHolding() &&
                        ((existingEntry.getHoldingId() == null && ledgerEntry.getHoldingId() == null) ||
                        (existingEntry.getHoldingId() != null && existingEntry.getHoldingId().equals(ledgerEntry.getHoldingId())))) {
                    adjustedBalance += existingEntry.getChange();
                    existingEntry.setBalance(adjustedBalance);
                }
            }
        }
        pendingEntries.add(ledgerEntry);
    }

    public static void clearAllHistoryEntries(){
        try {
            Db.db.beginTransaction();
            accountLedgerTable.truncate();
            Db.db.commitTransaction();
        } catch (Exception e) {
            Logger.logErrorMessage(e.toString(), e);
            Db.db.rollbackTransaction();
            throw e;
        } finally {
            Db.db.endTransaction();
        }
    }

    /**
     * Commit pending ledger entries
     */
    public static void commitEntries() {
        for (LedgerEntry ledgerEntry : pendingEntries) {
            accountLedgerTable.insert(ledgerEntry);
            listeners.notify(ledgerEntry, Event.ADD_ENTRY);
        }
        pendingEntries.clear();
    }

    /**
     * Clear pending ledger entries
     */
    public static void clearEntries() {
        pendingEntries.clear();
    }

    /**
     * Return a single entry identified by the ledger entry identifier
     *
     * @param   ledgerId                    Ledger entry identifier
     * @return                              Ledger entry or null if entry not found
     */
    public static LedgerEntry getEntry(long ledgerId) {
        if (!ledgerEnabled)
            return null;
        LedgerEntry entry;
        try (Connection con = Db.db.getConnection();
                PreparedStatement stmt = con.prepareStatement("SELECT * FROM account_ledger WHERE db_id = ?")) {
            stmt.setLong(1, ledgerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    entry = new LedgerEntry(rs);
                else
                    entry = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return entry;
    }

    /**
     * Return the ledger entries sorted in descending insert order
     *
     *
     * @param   accountId                   Account identifier or zero if no account identifier
     * @param   event                       Ledger event or null
     * @param   eventId                     Ledger event identifier or zero if no event identifier
     * @param   holding                     Ledger holding or null
     * @param   holdingId                   Ledger holding identifier or zero if no holding identifier
     * @param   firstIndex                  First matching entry index, inclusive
     * @param   lastIndex                   Last matching entry index, inclusive
     * @return                              List of ledger entries
     */
    public static List<LedgerEntry> getEntries(long accountId, LedgerEvent event, long eventId,
                                                LedgerHolding holding, long holdingId,
                                                int firstIndex, int lastIndex) {
        if (!ledgerEnabled) {
            return Collections.emptyList();
        }
        List<LedgerEntry> entryList = new ArrayList<>();
        //
        // Build the SELECT statement to search the entries
        StringBuilder sb = new StringBuilder(128);
        sb.append("SELECT * FROM account_ledger ");
        if (accountId != 0 || event != null || holding != null) {
            sb.append("WHERE ");
        }
        if (accountId != 0) {
            sb.append("account_id = ? ");
        }
        if (event != null) {
            if (accountId != 0) {
                sb.append("AND ");
            }
            sb.append("event_type = ? ");
            if (eventId != 0)
                sb.append("AND event_id = ? ");
        }
        if (holding != null) {
            if (accountId != 0 || event != null) {
                sb.append("AND ");
            }
            sb.append("holding_type = ? ");
            if (holdingId != 0)
                sb.append("AND holding_id = ? ");
        }
        sb.append("ORDER BY db_id DESC ");
        sb.append(DbUtils.limitsClause(firstIndex, lastIndex));
        //
        // Get the ledger entries
        //
      
        Connection con = null;
        try {
            blockchain.readLock();
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sb.toString());
            
            int i = 0;
            if (accountId != 0) {
                pstmt.setLong(++i, accountId);
            }
            if (event != null) {
                pstmt.setByte(++i, (byte)event.getCode());
                if (eventId != 0) {
                    pstmt.setLong(++i, eventId);
                }
            }
            if (holding != null) {
                pstmt.setByte(++i, (byte)holding.getCode());
                if (holdingId != 0) {
                    pstmt.setLong(++i, holdingId);
                }
            }
            DbUtils.setLimits(++i, pstmt, firstIndex, lastIndex);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entryList.add(new LedgerEntry(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            blockchain.readUnlock();
            DbUtils.close(con);
        }
        return entryList;
    }

    /**
     * Ledger events
     *
     * There must be a ledger event defined for each transaction (type,subtype) pair.  When adding
     * a new event, do not change the existing code assignments since these codes are stored in
     * the event_type field of the account_ledger table.
     */
    public enum LedgerEvent {
        // Block and Transaction
            BLOCK_GENERATED(1, false),
            REJECT_PHASED_TRANSACTION(2, true),
            TRANSACTION_FEE(50, true),
        // TYPE_PAYMENT
            ORDINARY_PAYMENT(3, true),
            COIN_BASE(58, true),
            //TODO sub type of coinbase definitions
        // TYPE_MESSAGING
            ACCOUNT_INFO(4, true),
            ALIAS_ASSIGNMENT(5, true),
            ALIAS_BUY(6, true),
            ALIAS_DELETE(7, true),
            ALIAS_SELL(8, true),
            ARBITRARY_MESSAGE(9, true),
            HUB_ANNOUNCEMENT(10, true),
            PHASING_VOTE_CASTING(11, true),
            POLL_CREATION(12, true),
            VOTE_CASTING(13, true),
            ACCOUNT_PROPERTY(56, true),
            ACCOUNT_PROPERTY_DELETE(57, true),
        // TYPE_COLORED_COINS
            ASSET_ASK_ORDER_CANCELLATION(14, true),
            ASSET_ASK_ORDER_PLACEMENT(15, true),
            ASSET_BID_ORDER_CANCELLATION(16, true),
            ASSET_BID_ORDER_PLACEMENT(17, true),
            ASSET_DIVIDEND_PAYMENT(18, true),
            ASSET_ISSUANCE(19, true),
            ASSET_TRADE(20, true),
            ASSET_TRANSFER(21, true),
            ASSET_DELETE(49, true),
        // TYPE_DIGITAL_GOODS
            DIGITAL_GOODS_DELISTED(22, true),
            DIGITAL_GOODS_DELISTING(23, true),
            DIGITAL_GOODS_DELIVERY(24, true),
            DIGITAL_GOODS_FEEDBACK(25, true),
            DIGITAL_GOODS_LISTING(26, true),
            DIGITAL_GOODS_PRICE_CHANGE(27, true),
            DIGITAL_GOODS_PURCHASE(28, true),
            DIGITAL_GOODS_PURCHASE_EXPIRED(29, true),
            DIGITAL_GOODS_QUANTITY_CHANGE(30, true),
            DIGITAL_GOODS_REFUND(31, true),
        // TYPE_ACCOUNT_CONTROL
            ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING(32, true),
            ACCOUNT_CONTROL_PHASING_ONLY(55, true),
        // TYPE_CURRENCY
            CURRENCY_DELETION(33, true),
            CURRENCY_DISTRIBUTION(34, true),
            CURRENCY_EXCHANGE(35, true),
            CURRENCY_EXCHANGE_BUY(36, true),
            CURRENCY_EXCHANGE_SELL(37, true),
            CURRENCY_ISSUANCE(38, true),
            CURRENCY_MINTING(39, true),
            CURRENCY_OFFER_EXPIRED(40, true),
            CURRENCY_OFFER_REPLACED(41, true),
            CURRENCY_PUBLISH_EXCHANGE_OFFER(42, true),
            CURRENCY_RESERVE_CLAIM(43, true),
            CURRENCY_RESERVE_INCREASE(44, true),
            CURRENCY_TRANSFER(45, true),
            CURRENCY_UNDO_CROWDFUNDING(46, true),
        // TYPE_DATA
            TAGGED_DATA_UPLOAD(47, true),
            TAGGED_DATA_EXTEND(48, true),
        // TYPE_SHUFFLING
            SHUFFLING_REGISTRATION(51, true),
            SHUFFLING_PROCESSING(52, true),
            SHUFFLING_CANCELLATION(53, true),
            SHUFFLING_DISTRIBUTION(54, true),
        // TYPE_SHARDER_POOL
            FORGE_POOL_CREATE(61, true),
            FORGE_POOL_DESTROY(62, true),
            FORGE_POOL_JOIN(63, true),
            FORGE_POOL_QUIT(64, true),
        // TYPE_STORAGE
            STORAGE_UPLOAD(71, true),
            STORAGE_BACKUP(72, true),
            STORAGE_EXTEND(73, true),
        //  TYPE_BURN
            BURN(75, true),
        //  SAVE_HASH
            SAVE_HASH(76, true);

        /** Event code mapping */
        private static final Map<Integer, LedgerEvent> eventMap = new HashMap<>();
        static {
            for (LedgerEvent event : values()) {
                if (eventMap.put(event.code, event) != null) {
                    throw new RuntimeException("LedgerEvent code " + event.code + " reused");
                }
            }
        }

        /** Event code */
        private final int code;

        /** Event identifier is a transaction */
        private final boolean isTransaction;

        /**
         * Create the ledger event
         *
         * @param   code                    Event code
         * @param   isTransaction           Event identifier is a transaction
         */
        LedgerEvent(int code, boolean isTransaction) {
            this.code = code;
            this.isTransaction = isTransaction;
        }

        /**
         * Check if the event identifier is a transaction
         *
         * @return                          TRUE if the event identifier is a transaction
         */
        public boolean isTransaction() {
            return isTransaction;
        }

        /**
         * Return the event code
         *
         * @return                          Event code
         */
        public int getCode() {
            return code;
        }

        /**
         * Get the event from the event code
         *
         * @param   code                    Event code
         * @return                          Event
         */
        public static LedgerEvent fromCode(int code) {
            LedgerEvent event = eventMap.get(code);
            if (event == null) {
                throw new IllegalArgumentException("LedgerEvent code " + code + " is unknown");
            }
            return event;
        }
    }

    /**
     * Ledger holdings
     *
     * When adding a new holding, do not change the existing code assignments since
     * they are stored in the holding_type field of the account_ledger table.
     */
    public enum LedgerHolding {
        UNCONFIRMED_CONCH_BALANCE(1, true),
        CONCH_BALANCE(2, false),
        UNCONFIRMED_ASSET_BALANCE(3, true),
        ASSET_BALANCE(4, false),
        UNCONFIRMED_CURRENCY_BALANCE(5, true),
        CURRENCY_BALANCE(6, false);

        /** Holding code mapping */
        private static final Map<Integer, LedgerHolding> holdingMap = new HashMap<>();
        static {
            for (LedgerHolding holding : values()) {
                if (holdingMap.put(holding.code, holding) != null) {
                    throw new RuntimeException("LedgerHolding code " + holding.code + " reused");
                }
            }
        }

        /** Holding code */
        private final int code;

        /** Unconfirmed holding */
        private final boolean isUnconfirmed;

        /**
         * Create the holding event
         *
         * @param   code                    Holding code
         * @param   isUnconfirmed           TRUE if the holding is unconfirmed
         */
        LedgerHolding(int code, boolean isUnconfirmed) {
            this.code = code;
            this.isUnconfirmed = isUnconfirmed;
        }

        /**
         * Check if the holding is unconfirmed
         *
         * @return                          TRUE if the holding is unconfirmed
         */
        public boolean isUnconfirmed() {
            return this.isUnconfirmed;
        }

        /**
         * Return the holding code
         *
         * @return                          Holding code
         */
        public int getCode() {
            return code;
        }

        /**
         * Get the holding from the holding code
         *
         * @param   code                    Holding code
         * @return                          Holding
         */
        public static LedgerHolding fromCode(int code) {
            LedgerHolding holding = holdingMap.get(code);
            if (holding == null) {
                throw new IllegalArgumentException("LedgerHolding code " + code + " is unknown");
            }
            return holding;
        }
    }

    /**
     * Ledger entry
     */
    public static class LedgerEntry {

        /** Ledger identifier */
        private long ledgerId = -1;

        /** Ledger event */
        private final LedgerEvent event;

        /** Associated event identifier */
        private final long eventId;

        /** Account identifier */
        private final long accountId;

        /** Holding */
        private final LedgerHolding holding;

        /** Holding identifier */
        private final Long holdingId;

        /** Change in balance */
        private long change;

        /** New balance */
        private long balance;

        /** Block identifier */
        private final long blockId;

        /** Blockchain height */
        private final int height;

        /** Block timestamp */
        private final int timestamp;

        /**
         * Create a ledger entry
         *
         * @param   event                   Event
         * @param   eventId                 Event identifier
         * @param   accountId               Account identifier
         * @param   holding                 Holding or null
         * @param   holdingId               Holding identifier or null
         * @param   change                  Change in balance
         * @param   balance                 New balance
         */
        public LedgerEntry(LedgerEvent event, long eventId, long accountId, LedgerHolding holding, Long holdingId,
                                            long change, long balance) {
            this.event = event;
            this.eventId = eventId;
            this.accountId = accountId;
            this.holding = holding;
            this.holdingId = holdingId;
            this.change = change;
            this.balance = balance;
            Block block = blockchain.getLastBlock();
            this.blockId = block.getId();
            this.height = block.getHeight();
            this.timestamp = block.getTimestamp();
        }

        /**
         * Create a ledger entry
         *
         * @param   event                   Event
         * @param   eventId                 Event identifier
         * @param   accountId               Account identifier
         * @param   change                  Change in balance
         * @param   balance                 New balance
         */
        public LedgerEntry(LedgerEvent event, long eventId, long accountId, long change, long balance) {
            this(event, eventId, accountId, null, null, change, balance);
        }

        /**
         * Create a ledger entry from a database entry
         *
         * @param   rs                      Result set
         * @throws  SQLException            Database error occurred
         */
        private LedgerEntry(ResultSet rs) throws SQLException {
            ledgerId = rs.getLong("db_id");
            event = LedgerEvent.fromCode(rs.getByte("event_type"));
            eventId = rs.getLong("event_id");
            accountId = rs.getLong("account_id");
            int holdingType = rs.getByte("holding_type");
            if (holdingType >= 0) {
                holding = LedgerHolding.fromCode(holdingType);
            } else {
                holding = null;
            }
            long id = rs.getLong("holding_id");
            if (rs.wasNull()) {
                holdingId = null;
            } else {
                holdingId = id;
            }
            change = rs.getLong("change");
            balance = rs.getLong("balance");
            blockId = rs.getLong("block_id");
            height = rs.getInt("height");
            timestamp = rs.getInt("timestamp");
        }

        /**
         * Return the ledger identifier
         *
         * @return                          Ledger identifier or -1 if not set
         */
        public long getLedgerId() {
            return ledgerId;
        }

        /**
         * Return the ledger event
         *
         * @return                          Ledger event
         */
        public LedgerEvent getEvent() {
            return event;
        }

        /**
         * Return the associated event identifier
         *
         * @return                          Event identifier
         */
        public long getEventId() {
            return eventId;
        }

        /**
         * Return the account identifier
         *
         * @return                          Account identifier
         */
        public long getAccountId() {
            return accountId;
        }

        /**
         * Return the holding
         *
         * @return                          Holding or null if there is no holding
         */
        public LedgerHolding getHolding() {
            return holding;
        }

        /**
         * Return the holding identifier
         *
         * @return                          Holding identifier or null if there is no holding identifier
         */
        public Long getHoldingId() {
            return holdingId;
        }

        /**
         * Update the balance change
         *
         * @param   amount                  Change amount
         */
        private void updateChange(long amount) {
            change += amount;
        }

        /**
         * Return the balance change
         *
         * @return                          Balance changes
         */
        public long getChange() {
            return change;
        }

        /**
         * Set the new balance
         *
         * @param balance                   New balance
         */
        private void setBalance(long balance) {
            this.balance = balance;
        }

        /**
         * Return the new balance
         *
         * @return                          New balance
         */
        public long getBalance() {
            return balance;
        }

        /**
         * Return the block identifier
         *
         * @return                          Block identifier
         */
        public long getBlockId() {
            return blockId;
        }

        /**
         * Return the height
         *
         * @return                          Height
         */
        public int getHeight() {
            return height;
        }

        /**
         * Return the timestamp
         *
         * @return                          Timestamp
         */
        public int getTimestamp() {
            return timestamp;
        }

        /**
         * Return the hash code
         *
         * @return                          Hash code
         */
        @Override
        public int hashCode() {
            return (Long.hashCode(accountId) ^ event.getCode() ^ Long.hashCode(eventId) ^
                    (holding != null ? holding.getCode() : 0) ^ (holdingId != null ? Long.hashCode(holdingId) : 0));
        }

        /**
         * Check if two ledger events are equal
         *
         * @param   obj                     Ledger event to check
         * @return                          TRUE if the ledger events are the same
         */
        @Override
        public boolean equals(Object obj) {
            return (obj != null && (obj instanceof LedgerEntry) && accountId == ((LedgerEntry)obj).accountId &&
                    event == ((LedgerEntry)obj).event && eventId == ((LedgerEntry)obj).eventId &&
                    holding == ((LedgerEntry)obj).holding &&
                    (holdingId != null ? holdingId.equals(((LedgerEntry)obj).holdingId) : ((LedgerEntry)obj).holdingId == null));
        }

        /**
         * Save the ledger entry
         *
         * @param   con                     Database connection
         * @throws  SQLException            Database error occurred
         */
        private void save(Connection con) throws SQLException {
            boolean insertNew = true;
            if(Constants.updateHistoryRecord()){
                PreparedStatement pstmt = con.prepareStatement("SELECT db_id, height FROM account_ledger"
                        + " WHERE account_id=? ORDER BY height DESC LIMIT 1");
                pstmt.setLong(1, this.accountId);

                ResultSet queryRS = pstmt.executeQuery();
                if(queryRS != null && queryRS.next()){
                    Long dbid = queryRS.getLong("db_id");
                    pstmt = con.prepareStatement("UPDATE account_ledger "
                            + "SET height = ?"
                            + ",event_type = ?"
                            + ",event_id = ?"
                            + ",holding_type = ?"
                            + ",holding_id = ?"
                            + ",change = ?"
                            + ",balance = ?"
                            + ",block_id = ?"
                            + ",timestamp = ?"
                            + " WHERE DB_ID = ?"
                    );
                    int i=0;
                    pstmt.setInt(++i, this.height);
                    pstmt.setByte(++i, (byte) this.event.getCode());
                    pstmt.setLong(++i, this.eventId);
                    if (holding != null) {
                        pstmt.setByte(++i, (byte)holding.getCode());
                    } else {
                        pstmt.setByte(++i, (byte)-1);
                    }
                    DbUtils.setLong(pstmt, ++i, holdingId);
                    pstmt.setLong(++i, change);
                    pstmt.setLong(++i, balance);
                    pstmt.setLong(++i, blockId);
                    pstmt.setInt(++i, timestamp);
                    pstmt.setLong(++i, dbid);
                    pstmt.executeUpdate();
                    insertNew = false;
                }
            }

            if(insertNew){
                PreparedStatement stmt = con.prepareStatement("INSERT INTO account_ledger "
                        + "(account_id, event_type, event_id, holding_type, holding_id, change, balance, "
                        + "block_id, height, timestamp) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                int i=0;
                stmt.setLong(++i, accountId);
                stmt.setByte(++i, (byte) event.getCode());
                stmt.setLong(++i, eventId);
                if (holding != null) {
                    stmt.setByte(++i, (byte)holding.getCode());
                } else {
                    stmt.setByte(++i, (byte)-1);
                }
                DbUtils.setLong(stmt, ++i, holdingId);
                stmt.setLong(++i, change);
                stmt.setLong(++i, balance);
                stmt.setLong(++i, blockId);
                stmt.setInt(++i, height);
                stmt.setInt(++i, timestamp);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        ledgerId = rs.getLong(1);
                    }
                }
            }

        }
    }
}
