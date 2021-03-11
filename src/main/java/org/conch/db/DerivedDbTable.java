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

package org.conch.db;

import org.conch.Conch;
import org.conch.util.Logger;

import java.sql.*;

public abstract class DerivedDbTable {

    protected static final TransactionalDb db = Db.db;

    protected final String table;

    protected DerivedDbTable(String table) {
        this.table = table;
        Conch.getBlockchainProcessor().registerDerivedTable(this);
    }

    public void rollback(int height) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + " WHERE height > ?")) {
            pstmtDelete.setInt(1, height);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void truncate() {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = db.getConnection();
             Statement stmt = con.createStatement()) {
             stmt.executeUpdate("TRUNCATE TABLE " + table);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void trim(int height) {
        //nothing to trim
    }

    public void createSearchIndex(Connection con) throws SQLException {
        //implemented in EntityDbTable only
    }

    public boolean isPersistent() {
        return false;
    }

    private static int countOfCycleDelMode = 100000;

    protected static void _trim(String tableName, int height){
        _trim(tableName, height, true);
    }

    protected static void _trim(String tableName, int height, boolean containLatestField) {

        boolean isInTx = db.isInTransaction();
        Connection con = null;
        try {
            con = db.getConnection();
            // check the deletion count
            PreparedStatement countStatement = con.prepareStatement(
                    "SELECT count(1) as deletion_count FROM " + tableName
                            + " WHERE height < ?"
                            + (containLatestField ? " AND latest <> TRUE" : "")
            );
            countStatement.setInt(1, height);

            ResultSet rs = countStatement.executeQuery();
            boolean cycleDelMode = false;
            if(rs != null && rs.next()) {
                int deletionCount = rs.getInt("deletion_count");
                if(deletionCount > 100000) {
                    cycleDelMode = true;
                }
            }

            if(cycleDelMode) {
//                cycleDeleteByDbId(con, tableName, height, containLatestField);
                cycleDeleteByHeightRange(con, tableName, height, containLatestField);
            }else{
                // direct del mode
                String trimSql = "DELETE FROM " + tableName
                        + " WHERE height < ?"
                        + (containLatestField ? " AND latest <> TRUE" : "");
                Logger.logDebugMessage("trim sql: " + trimSql);
                PreparedStatement deleteStatement = con.prepareStatement(trimSql);
                deleteStatement.setInt(1, height);
                deleteStatement.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
    }

    private static int delCountPerTurn = 144;
    private static void cycleDeleteByDbId(Connection con, String tableName, int height, boolean containLatestField) throws SQLException {
        boolean needDeleting = true;
        while(needDeleting) {
            PreparedStatement queryStatement = con.prepareStatement("SELECT db_id FROM " + tableName
                    + " WHERE height < ?"
                    + (containLatestField ? " AND latest <> TRUE" : "")
                    + " ORDER BY height ASC limit " + delCountPerTurn);
            queryStatement.setInt(1, height);

            ResultSet rs = queryStatement.executeQuery();
            if(rs != null) {
                while (rs.next()) {
                    Long existDbId = rs.getLong("db_id");
                    String trimSql = "DELETE FROM " + tableName + " WHERE db_id = ?";
                    Logger.logDebugMessage("DELETE FROM %s WHERE db_id = %d" , tableName, existDbId);
                    PreparedStatement deleteStatement = con.prepareStatement(trimSql);
                    deleteStatement.setLong(1, existDbId);
                    deleteStatement.executeUpdate();
                }
                Logger.logInfoMessage("deleted %d %s records in cycle delete in the table trimming", delCountPerTurn, tableName);
            }else{
                needDeleting = false;
            }
            con.commit();
        }
    }

    private static void cycleDeleteByHeightRange(Connection con, String tableName, int height, boolean containLatestField) throws SQLException {
        boolean needDeleting = true;
        int startHeight = height;
        while(needDeleting) {
            int endHeight = startHeight - delCountPerTurn;
            if(endHeight < 0) {
                endHeight = 0;
            }

            String trimSql = "DELETE FROM " + tableName
                    + " WHERE height < " + startHeight
                    + " AND height >= " + endHeight
                    + (containLatestField ? " AND latest <> TRUE" : "");
            Logger.logDebugMessage(trimSql);
            PreparedStatement deleteStatement = con.prepareStatement(trimSql);
            int count = deleteStatement.executeUpdate();
            Logger.logDebugMessage("DELETE " + count + " records after execute [" + trimSql + "]" );
            startHeight = endHeight;
            if(startHeight == 0) {
                needDeleting = false;
            }

        }
    }

    public void _rollback(int height, String... tables) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        if(tables == null || tables.length <= 0){
            return;
        }

        for(String table : tables){
            try (Connection con = db.getConnection();
                 PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + " WHERE height > ?")) {
                pstmtDelete.setInt(1, height);
                pstmtDelete.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }

    @Override
    public final String toString() {
        return table;
    }

//    protected static void rollbackAndPush(String tableName, int height, boolean push) {
//        if (!Db.db.isInTransaction()) {
//            throw new IllegalStateException("Not in transaction");
//        }
//        try (Connection con = Db.db.getConnection()) {
//            String idColumn;
//            if ("ACCOUNT".equalsIgnoreCase(tableName)
//            || "ACCOUNT_CACHE".equalsIgnoreCase(tableName)
//            || "ACCOUNT_HISTORY".equalsIgnoreCase(tableName)) {
//                idColumn = "ID";
//            } else {
//                idColumn = "ACCOUNT_ID";
//            }
//            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
//            String idQuerySql = "SELECT distinct " + idColumn + " FROM " + tableName;
//            ResultSet accountIdRs = statement.executeQuery(idQuerySql);
//            PreparedStatement pstmtDeleteWork = con.prepareStatement("DELETE FROM " + tableName + " WHERE height > ?");
//            pstmtDeleteWork.setInt(1, height);
//            pstmtDeleteWork.executeUpdate();
//            PreparedStatement pstmtDeleteCache = con.prepareStatement("DELETE FROM " + tableName + "_cache" + " WHERE height > ?");
//            pstmtDeleteCache.setInt(1, height);
//            pstmtDeleteCache.executeUpdate();
//            PreparedStatement pstmtDeleteHistory = con.prepareStatement("DELETE FROM " + tableName + "_history" + " WHERE height > ?");
//            pstmtDeleteHistory.setInt(1, height);
//            pstmtDeleteHistory.executeUpdate();
//
//            while (accountIdRs.next()) {
//                long accountId = accountIdRs.getLong(idColumn);
//
//                PreparedStatement wordQueryState = con.prepareStatement("select DB_ID from " + tableName + " where " + idColumn + " = ? order by height desc limit 1");
//                wordQueryState.setLong(1, accountId);
//                ResultSet resultSet = wordQueryState.executeQuery();
//                if (resultSet != null && resultSet.next()) {
//
//                    PreparedStatement pstmtUpdate = con.prepareStatement("update " + tableName + " set latest = true where DB_ID = ?");
//                    pstmtUpdate.setLong(1, resultSet.getLong("DB_ID"));
//                    pstmtUpdate.executeUpdate();
//                } else {
//                    PreparedStatement cacheQueryState = con.prepareStatement("select * from " + tableName + "_cache" + " where " + idColumn + " = ? order by height desc limit 1");
//                    cacheQueryState.setLong(1, accountId);
//                    ResultSet cacheResult = cacheQueryState.executeQuery();
//                    if (cacheResult == null || !cacheResult.next()) {
//                        PreparedStatement historyQueryState = con.prepareStatement("select * from " + tableName + "_history" + " where " + idColumn + " = ? order by height desc limit 1");
//                        historyQueryState.setLong(1, accountId);
//                        cacheResult = historyQueryState.executeQuery();
//                    }
//                    if (cacheResult != null && cacheResult.next()) {
//                        do {
//                            ResultSetMetaData metaData = cacheResult.getMetaData();
//                            int columnCount = metaData.getColumnCount();
//                            StringBuilder insert = new StringBuilder();
//                            StringBuilder values = new StringBuilder();
//                            for (int i = 1; i <= columnCount; i++) {
//                                if (i == 1) {
//                                    insert.append("insert into " + tableName + " (");
//                                    insert.append(metaData.getColumnName(i)).append(",");
//                                    values.append("values (").append("?,");
//                                } else if (1 < i && i < columnCount) {
//                                    insert.append(metaData.getColumnName(i)).append(",");
//                                    values.append("?,");
//                                } else {
//                                    insert.append(metaData.getColumnName(i)).append(")");
//                                    values.append("?)");
//                                }
//                            }
//                            PreparedStatement preparedStatement = con.prepareStatement(insert.append(values).toString());
//                            for (int i = 1; i <= columnCount; i++) {
//                                if ("latest".equalsIgnoreCase(metaData.getColumnName(i))) {
//                                    preparedStatement.setObject(i, true);
//                                } else {
//                                    preparedStatement.setObject(i, cacheResult.getObject(i));
//                                }
//                            }
//                            preparedStatement.executeUpdate();
//                        } while (cacheResult.next());
//                    }
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e.toString(), e);
//        }
//    }
}
