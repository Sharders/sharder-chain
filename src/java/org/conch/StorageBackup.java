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
import org.conch.db.PersistentDbTable;
import org.conch.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StorageBackup {

    private static final DbKey.LongKeyFactory<StorageBackup> storageKeyFactory = new DbKey.LongKeyFactory<StorageBackup>("storer_id") {

        @Override
        public DbKey newKey(StorageBackup storage) {
            return storage.dbKey;
        }

    };

    private static final PersistentDbTable<StorageBackup> storageTable = new PersistentDbTable<StorageBackup>(
            "storage_backup", storageKeyFactory, "name,description,tags") {

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
    private final String storeTarget;
    private final long storeTransaction;
    private final long backupTransaction;
    private int height;

    public StorageBackup(Transaction upload,Transaction backup, String target) {
        this.dbKey = storageKeyFactory.newKey(backup.getId());
        this.storerId = backup.getSenderId();
        this.storeTarget = target;
        this.storeTransaction = upload.getId();
        this.backupTransaction = backup.getId();
        this.height = backup.getHeight();
    }

    private StorageBackup(ResultSet rs, DbKey dbKey) throws SQLException {
        this.dbKey = dbKey;
        this.storerId = rs.getLong("storer_id");
        this.storeTarget = rs.getString("backup_target");
        this.storeTransaction = rs.getLong("store_transaction");
        this.backupTransaction = rs.getLong("backup_transaction");
        this.height = rs.getInt("height");
    }

    public static void init(){}

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO storage_backup (storer_id, backup_target, store_transaction, backup_transaction,height) "
                + "VALUES ( ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.storerId);
            pstmt.setString(++i, this.storeTarget);
            pstmt.setLong(++i, this.storeTransaction);
            pstmt.setLong(++i, this.backupTransaction);
            pstmt.setInt(++i, height);
            pstmt.executeUpdate();
        }
    }

    static void add(Transaction upload,Transaction backup, String target) {
        StorageBackup storage = new StorageBackup(upload, backup, target);
        storageTable.insert(storage);
        // why need save the timestamp
//        Timestamp timestamp = new Timestamp(transaction.getId(), transaction.getTimestamp());
//        timestampTable.insert(timestamp);
    }

    static int getCurrentBackupNum(long id){
        try (Connection connection = Db.db.getConnection();
             PreparedStatement st = connection.prepareStatement("SELECT COUNT(*) FROM STORAGE_BACKUP WHERE store_transaction = ?")) {
            st.setLong(1,id);
            ResultSet resultSet = st.executeQuery();
            if (resultSet.next()){
                return resultSet.getInt(1);
            }
        }catch (SQLException e){
            Logger.logErrorMessage("failed to query backup number",e);
        }
        return 0;
    }

    public static boolean isOwnerOfStorage(Long storerId, Long uploadId){
        try (Connection connection = Db.db.getConnection();
             PreparedStatement st = connection.prepareStatement("SELECT COUNT(*) FROM STORAGE_BACKUP WHERE store_transaction = ? AND STORER_ID = ?")) {
            st.setLong(1,uploadId);
            st.setLong(2,storerId);
            ResultSet resultSet = st.executeQuery();
            if (resultSet.next()){
                if(resultSet.getInt(1) == 0){
                    return false;
                }else {
                    return true;
                }
            }
        }catch (SQLException e){
            Logger.logErrorMessage("failed to query backup number",e);
        }
        return false;
    }
}
