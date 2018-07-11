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

import org.conch.db.*;
import org.conch.storage.Ssid;
import org.conch.storage.ipfs.IpfsService;
import org.conch.util.Logger;
import org.conch.util.Search;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Storage {

    private static final DbKey.LongKeyFactory<Storage> storageKeyFactory = new DbKey.LongKeyFactory<Storage>("id") {

        @Override
        public DbKey newKey(Storage storage) {
            return storage.dbKey;
        }

    };

    private static final VersionedPersistentDbTable<Storage> storageTable = new VersionedPersistentDbTable<Storage>(
            "storage", storageKeyFactory, "name,description,tags") {

        @Override
        protected Storage load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Storage(rs, dbKey);
        }

        @Override
        protected void save(Connection con, Storage storage) throws SQLException {
            storage.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY block_timestamp DESC, height DESC, db_id DESC ";
        }

    };

    public static int getCount() {
        return storageTable.getCount();
    }

    public static DbIterator<Storage> getAll(int from, int to) {
        return storageTable.getAll(from, to);
    }

    public static Storage getDataStorage(long transactionId) {
        return storageTable.get(storageKeyFactory.newKey(transactionId));
    }

    public static DbIterator<Storage> getDataStorage(String channel, long accountId, int from, int to) {
        if (channel == null && accountId == 0) {
            throw new IllegalArgumentException("Either channel, or accountId, or both, must be specified");
        }
        return storageTable.getManyBy(getDbClause(channel, accountId), from, to);
    }

    public static DbIterator<Storage> searchData(String query, String channel, long accountId, int from, int to) {
        return storageTable.search(query, getDbClause(channel, accountId), from, to,
                " ORDER BY ft.score DESC, storage.db_id DESC ");
    }

    private static DbClause getDbClause(String channel, long accountId) {
        DbClause dbClause = DbClause.EMPTY_CLAUSE;
        if (channel != null) {
            dbClause = new DbClause.StringClause("channel", channel);
        }
        if (accountId != 0) {
            DbClause accountClause = new DbClause.LongClause("account_id", accountId);
            dbClause = dbClause != DbClause.EMPTY_CLAUSE ? dbClause.and(accountClause) : accountClause;
        }
        return dbClause;
    }

    private final long id;
    private final DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String description;
    private final String type;
    private final String ssid;
    private final String channel;
    private final int existence_height;
    private final int replicated_number;

    private int transactionTimestamp;
    private int blockTimestamp;
    private int height;

    public Storage(Transaction transaction, Attachment.DataStorageUpload attachment) {
        this(transaction, attachment, Conch.getBlockchain().getLastBlockTimestamp(), Conch.getBlockchain().getHeight());
    }

    private Storage(Transaction transaction, Attachment.DataStorageUpload attachment, int blockTimestamp, int height) {
        this.id = transaction.getId();
        this.dbKey = storageKeyFactory.newKey(this.id);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.type = attachment.getType();
        this.ssid = attachment.getSsid();
        this.channel = attachment.getChannel();
        this.existence_height = attachment.getExistence_height();
        this.replicated_number = attachment.getReplicated_number();
        this.blockTimestamp = blockTimestamp;
        this.transactionTimestamp = transaction.getTimestamp();
        this.height = height;
    }

    private Storage(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.type = rs.getString("type");
        this.ssid = rs.getString("ssid");
        this.channel = rs.getString("channel");
        this.existence_height = rs.getInt("existence_height");
        this.replicated_number = rs.getInt("replicated_number");
        this.blockTimestamp = rs.getInt("block_timestamp");
        this.transactionTimestamp = rs.getInt("transaction_timestamp");
        this.height = rs.getInt("height");
    }

    public static void init(){}

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO storage (id, account_id, name, description, "
                + "type, ssid, channel, existence_height, replicated_number, block_timestamp, transaction_timestamp, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.name);
            pstmt.setString(++i, this.description);
            pstmt.setString(++i, this.type);
            pstmt.setString(++i, this.ssid);
            pstmt.setString(++i, this.channel);
            pstmt.setInt(++i, this.existence_height);
            pstmt.setInt(++i, this.replicated_number);
            pstmt.setInt(++i, this.blockTimestamp);
            pstmt.setInt(++i, this.transactionTimestamp);
            pstmt.setInt(++i, height);
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
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

    public String getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public String getSsid() {
        return ssid;
    }

    public int getExistence_height() {
        return existence_height;
    }

    public int getReplicated_number() {
        return replicated_number;
    }

    public int getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    static void add(TransactionImpl transaction, Attachment.DataStorageUpload attachment) {
        Storage storage = storageTable.get(transaction.getDbKey());
        if (storage == null) {
            storage = new Storage(transaction, attachment);
            storageTable.insert(storage);
            //TODO off-chain storage
        }
        // why need save the timestamp
//        Timestamp timestamp = new Timestamp(transaction.getId(), transaction.getTimestamp());
//        timestampTable.insert(timestamp);
    }


}
