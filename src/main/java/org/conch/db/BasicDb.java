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
import org.conch.common.Constants;
import org.conch.util.LocalDebugTool;
import org.conch.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class BasicDb {

    public static final class DbProperties {

        private long maxCacheSize;
        private String dbUrl;
        private String dbType;
        private String dbDir;
        private String dbParams;
        private String dbUsername;
        private String dbPassword;
        private int maxConnections;
        private int loginTimeout;
        private int defaultLockTimeout;
        private int maxMemoryRows;

        public DbProperties maxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        public DbProperties dbUrl(String dbUrl) {
            this.dbUrl = dbUrl;
            return this;
        }

        public DbProperties dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }

        public DbProperties dbDir(String dbDir) {
            this.dbDir = dbDir;
            return this;
        }

        public DbProperties dbParams(String dbParams) {
            this.dbParams = dbParams;
            return this;
        }

        public DbProperties dbUsername(String dbUsername) {
            this.dbUsername = dbUsername;
            return this;
        }

        public DbProperties dbPassword(String dbPassword) {
            this.dbPassword = dbPassword;
            return this;
        }

        public DbProperties maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public DbProperties loginTimeout(int loginTimeout) {
            this.loginTimeout = loginTimeout;
            return this;
        }

        public DbProperties defaultLockTimeout(int defaultLockTimeout) {
            this.defaultLockTimeout = defaultLockTimeout;
            return this;
        }

        public DbProperties maxMemoryRows(int maxMemoryRows) {
            this.maxMemoryRows = maxMemoryRows;
            return this;
        }

    }

    protected JdbcConnectionPool cp;
    private volatile int maxActiveConnections;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final int maxConnections;
    private final int loginTimeout;
    private final int defaultLockTimeout;
    private final int maxMemoryRows;
    private volatile boolean initialized = false;

    public BasicDb(DbProperties dbProperties) {
        long maxCacheSize = dbProperties.maxCacheSize;
        if (maxCacheSize == 0) {
            maxCacheSize =
                    Math.min(256, Math.max(16, (Runtime.getRuntime().maxMemory() / (1024 * 1024) - 128) / 2)) * 1024;
        }
        String dbUrl = dbProperties.dbUrl;
        if (dbUrl == null) {
            String dbDir = Conch.getDbDir(dbProperties.dbDir);
            dbUrl = String.format("jdbc:%s:%s;%s", dbProperties.dbType, dbDir, dbProperties.dbParams);
        }
        if (!dbUrl.contains("MV_STORE=")) {
            dbUrl += ";MV_STORE=FALSE";
        }
        if (!dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
        this.dbUrl = dbUrl;
        this.dbUsername = dbProperties.dbUsername;
        this.dbPassword = dbProperties.dbPassword;
        this.maxConnections = dbProperties.maxConnections;
        this.loginTimeout = dbProperties.loginTimeout;
        this.defaultLockTimeout = dbProperties.defaultLockTimeout;
        this.maxMemoryRows = dbProperties.maxMemoryRows;
    }

    public void init(DbVersion dbVersion) {
        Logger.logDebugMessage("Database jdbc url set to %s username %s", dbUrl, dbUsername);
        FullTextTrigger.setActive(true);
        cp = JdbcConnectionPool.create(dbUrl, dbUsername, dbPassword);
        cp.setMaxConnections(maxConnections);
        cp.setLoginTimeout(loginTimeout);
        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET DEFAULT_LOCK_TIMEOUT " + defaultLockTimeout);
            stmt.executeUpdate("SET MAX_MEMORY_ROWS " + maxMemoryRows);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        dbVersion.init(this);
        initialized = true;
    }

    /**
     * IMMEDIATELY
     * COMPACT
     * DEFRAG
     *
     * @param mode
     */
    public void shutdown(String mode) {
        if (!initialized) {
            return;
        }
        try {
            FullTextTrigger.setActive(false);
            Connection con = cp.getConnection();
            Statement stmt = con.createStatement();
            stmt.execute("SHUTDOWN " + mode);
            Logger.logShutdownMessage("Database shutdown completed");
        } catch (SQLException e) {
            Logger.logShutdownMessage(e.toString(), e);
        }
    }

    public void analyzeTables() {
        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("ANALYZE SAMPLE_SIZE 0");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection con = getPooledConnection();
        if (con == null && LocalDebugTool.isLocalDebug()) {
            throw new IllegalStateException("Connection pool is overflowed");
        }
        con.setAutoCommit(true);
        return con;
    }

    public int getActiveCount() {
        return cp.getActiveConnections();
    }

    private static int exceedMaxCount = 0;
    private static final int RESTART_COUNT = Constants.isDevnet() ? 10 : 30;
    private static final int MAX_DB_CONNECTIONS = Conch.getIntProperty("sharder.maxDbConnections");
    private static final boolean DEBUG_DETAIL = Conch.getBooleanProperty("sharder.debugStackAtAcquireCon", false);

    protected Connection getPooledConnection() {
        Connection con = null;
        try {
            if (exceedMaxConnections()) {
                Logger.logDebugMessage("Current active db connection pool size is %d larger than max size %d",
                        maxActiveConnections, MAX_DB_CONNECTIONS);

                if(LocalDebugTool.isLocalDebug()){
                    return null;
                }
                checkAndRestart();
            }
            con = cp.getConnection();
            int activeConnections = cp.getActiveConnections();
            if (activeConnections > maxActiveConnections) {
                maxActiveConnections = activeConnections;
                Logger.logDebugMessage("Active db connection pool size is %d after acquire a new connection into pool" +
                                ". You can see stack detail in the 'warn.log'.",
                        maxActiveConnections);
                if (Logger.isLevel(Logger.Level.DEBUG)
                        && DEBUG_DETAIL) {
                    String stacks = String.format("Acquire stacks(active conn size=%d) \n", activeConnections);
                    Logger.logWarningMessage(stacks + Logger.callStack());
                }
            }


        } catch (Exception e) {
            Logger.logErrorMessage("can't get connection from pool caused by " + e.getMessage());
            checkAndRestart();
        }

        return con;
    }

    private boolean exceedMaxConnections() {
        return maxActiveConnections >= MAX_DB_CONNECTIONS;
    }

    private static void checkAndRestart() {
        if (exceedMaxCount++ > RESTART_COUNT) {
            String msg = String.format("Exceed max connections[%d] %d times, restart the COS to temporary " +
                    "fix this problem", MAX_DB_CONNECTIONS, RESTART_COUNT);
            Logger.logWarningMessage(msg);
            Logger.logErrorMessage(msg);
            new Thread(() -> Conch.restartApplication(null)).start();
            exceedMaxCount = 0;
        }
    }

    public String getUrl() {
        return dbUrl;
    }

}
