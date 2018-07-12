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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StorageBackup {

    private static final DbKey.LongKeyFactory<StorageBackup> storageKeyFactory = new DbKey.LongKeyFactory<StorageBackup>("id") {

        @Override
        public DbKey newKey(StorageBackup storage) {
            return storage.dbKey;
        }

    };

    private static final VersionedPersistentDbTable<StorageBackup> storageTable = new VersionedPersistentDbTable<StorageBackup>(
            "storage", storageKeyFactory, "name,description,tags") {

        @Override
        protected StorageBackup load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new StorageBackup(rs, dbKey);
        }

        @Override
        protected void save(Connection con, StorageBackup storage) throws SQLException {
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

    public static DbIterator<StorageBackup> getAll(int from, int to) {
        return storageTable.getAll(from, to);
    }

    public static StorageBackup getDataStorage(long transactionId) {
        return storageTable.get(storageKeyFactory.newKey(transactionId));
    }

    public static DbIterator<StorageBackup> getDataStorage(String channel, long accountId, int from, int to) {
        if (channel == null && accountId == 0) {
            throw new IllegalArgumentException("Either channel, or accountId, or both, must be specified");
        }
        return storageTable.getManyBy(getDbClause(channel, accountId), from, to);
    }

    public static DbIterator<StorageBackup> searchData(String query, String channel, long accountId, int from, int to) {
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

    private final DbKey dbKey;
    private final long storerId;
    private final long storeTransaction;
    private final long backupTransaction;
    private int height;

    public StorageBackup(Transaction upload,Transaction backup) {
        this.dbKey = storageKeyFactory.newKey(backup.getId());
        this.storerId = upload.getSenderId();
        this.storeTransaction = upload.getId();
        this.backupTransaction = backup.getId();
        this.height = height;
    }

    private StorageBackup(ResultSet rs, DbKey dbKey) throws SQLException {
        this.dbKey = dbKey;
        this.storerId = rs.getLong("storer_id");
        this.storeTransaction = rs.getLong("store_transaction");
        this.backupTransaction = rs.getLong("backup_transaction");
        this.height = rs.getInt("height");
    }

    public static void init(){}

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO storage_backup (storer_id, store_transaction, backup_transaction,height) "
                + "VALUES ( ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.storerId);
            pstmt.setLong(++i, this.storeTransaction);
            pstmt.setLong(++i, this.backupTransaction);
            pstmt.setInt(++i, height);
            pstmt.executeUpdate();
        }
    }

    static void add(Transaction upload,Transaction backup) {
        StorageBackup storage = storageTable.get(storageKeyFactory.newKey(backup.getId()));
        if (storage == null) {
            storage = new StorageBackup(upload, backup);
            storageTable.insert(storage);
            //TODO off-chain storage
        }
        // why need save the timestamp
//        Timestamp timestamp = new Timestamp(transaction.getId(), transaction.getTimestamp());
//        timestampTable.insert(timestamp);
    }

    static int getCurrentBackupNum(long id){
        try{
           Connection connection =  Db.db.getConnection();
            PreparedStatement st = connection.prepareStatement("SELECT COUNT(*) FROM STORAGE_BACKUP WHERE store_transaction = ?");
            st.setLong(1,id);
            ResultSet resultSet = st.executeQuery();
        }catch (SQLException e){

        }
        return 0;
    }
}
