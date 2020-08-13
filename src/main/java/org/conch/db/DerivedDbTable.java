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
        Connection con = null;
        try {
            con = Db.db.getConnection();
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
                if(deletionCount > 100000) cycleDelMode = true;
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
            DbUtils.close(con);
        }
    }

    private static int delCountOneTurn = 10000;
    private static void cycleDeleteByDbId(Connection con, String tableName, int height, boolean containLatestField) throws SQLException {
        boolean needDeleting = true;
        while(needDeleting) {
            PreparedStatement queryStatement = con.prepareStatement("SELECT db_id FROM " + tableName
                    + " WHERE height < ?"
                    + (containLatestField ? " AND latest <> TRUE" : "")
                    + " ORDER BY height ASC limit " + delCountOneTurn);
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
                Logger.logInfoMessage("deleted %d %s records in cycle delete in the table trimming", delCountOneTurn, tableName);
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
            int endHeight = startHeight - delCountOneTurn;
            if(endHeight < 0) endHeight = 0;

            PreparedStatement countStatement = con.prepareStatement(
                    "SELECT count(1) as count FROM " + tableName
                            + " WHERE height < ?"
                            + " AND height >= ?"
                            + (containLatestField ? " AND latest <> TRUE" : "")
            );
            countStatement.setInt(1, startHeight);
            countStatement.setInt(2, endHeight);

            ResultSet rs = countStatement.executeQuery();
            if(rs != null && rs.next()) {
                int count = rs.getInt("count");
                if(count > 0 ) {
                    String trimSql = "DELETE FROM " + tableName
                            + " WHERE height < ?"
                            + " AND height >= ?"
                            + (containLatestField ? " AND latest <> TRUE" : "");
                    Logger.logDebugMessage("DELETE FROM %s WHERE height < %d AND height >= %d", tableName, startHeight, endHeight);
                    PreparedStatement deleteStatement = con.prepareStatement(trimSql);
                    deleteStatement.setInt(1, startHeight);
                    deleteStatement.setInt(2, endHeight);
                    con.commit();

                    startHeight = endHeight;
                }else {
                    needDeleting = false;
                }
            }else{
                needDeleting = false;
            }

        }
    }

    @Override
    public final String toString() {
        return table;
    }

}
