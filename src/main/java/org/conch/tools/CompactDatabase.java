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

package org.conch.tools;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.util.Logger;
import org.h2.tools.RunScript;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Compact and reorganize the database.
 *
 * <p>To run the database compact tool on Linux or Mac:
 *
 * <p>java -cp "classes:lib/*:conf" org.conch.tools.CompactDatabase
 *
 * <p>To run the database compact tool on Windows:
 *
 * <p>java -cp "classes;lib/*;conf" -Dsharder.runtime.mode=desktop org.conch.tools.CompactDatabase
 */
public class CompactDatabase {

    /**
     * Compact the NRS database
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        //
        // Initialize Conch properties and logging
        //
        Logger.init();
        //
        // Compact the database
        //
        int exitCode = compactAndRestoreDB();
        //
        // Shutdown the logger and exit
        //
        Logger.shutdown();
        System.exit(exitCode);
    }


    static String dbPrefix;
    static String dbType;
    static String dbUrl;
    static String dbParams;
    static String dbUsername;
    static String dbPassword;
    static String dbDir;

    private static void initDb() throws Exception{
        //
        // Get the database URL
        //
        dbPrefix = Constants.isTestnetOrDevnet() ? "sharder.testDb" : "sharder.db";
        dbType = Conch.getStringProperty(dbPrefix + "Type");
        if (!"h2".equals(dbType)) {
            Logger.logErrorMessage("Database type must be 'h2'");
            throw new RuntimeException("Database type must be 'h2'");
        }
        dbUrl = Conch.getStringProperty(dbPrefix + "Url");
        if (dbUrl == null) {
            String dbPath = Conch.getDbDir(Conch.getStringProperty(dbPrefix + "Dir"));
            dbUrl = String.format("jdbc:%s:%s", dbType, dbPath);
        }
        dbParams = Conch.getStringProperty(dbPrefix + "Params");
        dbUrl += ";" + dbParams;
        if (!dbUrl.contains("MV_STORE=")) {
            dbUrl += ";MV_STORE=FALSE";
        }
        dbUsername = Conch.getStringProperty(dbPrefix + "Username", "sa");
        dbPassword = Conch.getStringProperty(dbPrefix + "Password", "sa", true);

        //
        // Get the database path.  This is the third colon-separated operand and is
        // terminated by a semi-colon or by the end of the string.
        //
        int pos = dbUrl.indexOf(':');
        if (pos >= 0) {
            pos = dbUrl.indexOf(':', pos + 1);
        }
        if (pos < 0) {
            Logger.logErrorMessage("Malformed database URL: " + dbUrl);
            throw new RuntimeException("Malformed database URL: " + dbUrl);
        }

        int startPos = pos + 1;
        int endPos = dbUrl.indexOf(';', startPos);
        if (endPos < 0) {
            dbDir = dbUrl.substring(startPos);
        } else {
            dbDir = dbUrl.substring(startPos, endPos);
        }
        //
        // Remove the optional 'file' operand
        //
        if (dbDir.startsWith("file:")) {
            dbDir = dbDir.substring(5);
        }
        //
        // Remove the database prefix from the end of the database path.  The path
        // separator can be either '/' or '\' (Windows will accept either separator
        // so we can't rely on the system property).
        //
        endPos = dbDir.lastIndexOf('\\');
        pos = dbDir.lastIndexOf('/');
        if (endPos >= 0) {
            if (pos >= 0) {
                endPos = Math.max(endPos, pos);
            }
        } else {
            endPos = pos;
        }
        if (endPos < 0) {
            Logger.logErrorMessage("Malformed database URL: " + dbUrl);
            throw new RuntimeException("Malformed database URL: " + dbUrl);
        }
        dbDir = dbDir.substring(0, endPos);
        Logger.logInfoMessage("Database directory is '" + dbDir + '"');
    }

    public static boolean checkAndRestore(){
        String sqlFilePath = null;
        try {
            initDb();
            File sqlFile = new File(dbDir, "backup.sql.gz");
            sqlFilePath = sqlFile.getPath();
            if(!sqlFile.exists()){
                Logger.logInfoMessage("No restore sql %s found, no needs to restore db " + sqlFilePath);
                return false;
            }
            restore(dbUrl, dbUsername, dbPassword, sqlFile, true);
        } catch (Throwable exc) {
            Logger.logErrorMessage("Unable to restore the database from " + sqlFilePath, exc);
            return false;
        }
        Account.needCompact = false;
        return true;
    }

    public static boolean restore(String dbUrl, String dbUsername, String dbPassword, File sqlFile, boolean delAfter) throws Exception {
        initDb();
        Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
//        Statement s = conn.createStatement();
//        Logger.logInfoMessage("Restore the '%s' from SQL script '%s'", dbUrl, sqlPath);
//        s.execute("RUNSCRIPT FROM '" + sqlFile.getPath() + "' COMPRESSION GZIP CHARSET 'UTF-8'");
//        s.execute("ANALYZE");

        RunScript.execute(conn, new FileReader(sqlFile));

        File dbFile = new File(dbDir, "mw.h2.db");
        File oldDbFile = new File(dbFile.getPath() + ".bak");
        if(delAfter) {
            delOldFiles(dbFile, oldDbFile);
        }

        return true;
    }

    private static void delOldFiles(File... files){
        for (int i = 0 ; i < files.length; i++) {
            if (files[i].exists()
                && !files[i].delete()) {
                Logger.logErrorMessage(String.format("Unable to delete '%s'", files[i].getPath()));
            }
        }
    }

    /**
     * Compact the database
     */
    public static int compactAndRestoreDB() {
        int exitCode = 0;

        try{
           initDb();
        }catch(Exception e){
            e.printStackTrace();
            return 1;
        }

        //
        // Create our files
        //
        int phase = 0;
        File sqlFile = new File(dbDir, "backup.sql.gz");
        File dbFile = new File(dbDir, "mw.h2.db");
        if (!dbFile.exists()) {
            dbFile = new File(dbDir, "mw.mv.db");
            if (!dbFile.exists()) {
                Logger.logErrorMessage("Database %s not found", dbFile.getName());
                return 1;
            }
        }
        File oldFile = new File(dbFile.getPath() + ".bak");
        try {
            //
            // Create the SQL script
            //
            Logger.logInfoMessage("Creating the SQL script %s", sqlFile.getName());
            if (sqlFile.exists()) {
                if (!sqlFile.delete()) {
                    throw new IOException(String.format("Unable to delete '%s'", sqlFile.getPath()));
                }
            }
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                 Statement s = conn.createStatement()) {
                 s.execute("SCRIPT TO '" + sqlFile.getPath() + "' COMPRESSION GZIP CHARSET 'UTF-8'");
            }

            //
            // Create the new database
            //
            Logger.logInfoMessage("Creating the new database and rename the current db %s to %s", dbFile.getName(), oldFile.getName());
            if (!dbFile.renameTo(oldFile)) {
                throw new IOException(
                        String.format("Unable to rename '%s' to '%s'", dbFile.getPath(), oldFile.getPath()));
            }

            phase = 1;
            restore(dbUrl, dbUsername, dbPassword, sqlFile, false);
            //
            // New database has been created
            //
            phase = 2;
            Logger.logInfoMessage("Database successfully compacted");
        } catch (Throwable exc) {
            Logger.logErrorMessage("Unable to compact the database", exc);
            exitCode = 1;
        } finally {
            switch (phase) {
                case 0:
                    //
                    // We failed while creating the SQL file
                    //
                    if (sqlFile.exists()) {
                        if (!sqlFile.delete()) {
                            Logger.logErrorMessage(String.format("Unable to delete '%s'", sqlFile.getPath()));
                        }
                    }
                    break;
                case 1:
                    //
                    // We failed while creating the new database
                    //
                    File newFile = new File(dbDir, "mw.h2.db");
                    if (newFile.exists()) {
                        if (!newFile.delete()) {
                            Logger.logErrorMessage(String.format("Unable to delete '%s'", newFile.getPath()));
                        }
                    } else {
                        newFile = new File(dbDir, "mw.mv.db");
                        if (newFile.exists()) {
                            if (!newFile.delete()) {
                                Logger.logErrorMessage(String.format("Unable to delete '%s'", newFile.getPath()));
                            }
                        }
                    }
//                    if (!oldFile.renameTo(dbFile)) {
//                        Logger.logErrorMessage(
//                                String.format(
//                                        "Unable to rename '%s' to '%s'", oldFile.getPath(), dbFile.getPath()));
//                    }
                    break;
                case 2:
                    //
                    // New database created
                    //
                    delOldFiles(dbFile, oldFile);
                    break;
            }
        }
        return exitCode;
    }
}
