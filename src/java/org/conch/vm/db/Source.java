package org.conch.vm.db;

import org.conch.Db;
import org.conch.util.Logger;
import org.conch.vm.DataWord;
import org.conch.vm.util.ByteUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class Source {
    public void flushStorage(HashMap<String, HashMap<DataWord, DataWord>> storageCache) throws SQLException {
        for (String key : storageCache.keySet()) {
            for (DataWord dataKey : storageCache.get(key).keySet()) {
                saveValue(key + dataKey.toString(), storageCache.get(key).get(dataKey).toString());
            }
        }
    }

    public void flushCodeCache(HashMap<String, byte[]> codeCache) throws SQLException {
        for (String key : codeCache.keySet()) {
            saveValue(key, ByteUtil.toHexString(codeCache.get(key)));
        }
    }

    public void flushAccountState(HashMap<String, AccountState> accountStateCache) throws SQLException {
        for (String key : accountStateCache.keySet()) {
            // TODO wj account should save in sharder account table
            saveValue(key, ByteUtil.toHexString(accountStateCache.get(key).getEncoded()));
        }
    }

    private void saveValue(String key, String value) throws SQLException {
        try (PreparedStatement pstmt =
                     Db.db.getConnection()
                             .prepareStatement("MERGE INTO CONTRACT (KEY, VALUE) VALUES (?, ?)")) {
            int i = 0;
            pstmt.setString(++i, key);
            pstmt.setString(++i, value);
            pstmt.executeUpdate();
        }
    }

    public static DataWord getStorage(byte[] addr, DataWord key) {
        DataWord storage = null;
        try (PreparedStatement pstmt =
                     Db.db.getConnection()
                             .prepareStatement("SELECT * FROM CONTRACT where KEY = ?")) {
            int i = 0;
            pstmt.setString(++i, ByteUtil.toHexString(addr) + key.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                storage = new DataWord(ByteUtil.hexStringToBytes(rs.getString("value")));
            }
        } catch (SQLException e) {
            Logger.logErrorMessage("load storage from database error ", e);
        }
        return storage;
    }

    public static AccountState getAccountState(String addr) {
        AccountState accountState = null;
        try (PreparedStatement pstmt =
                     Db.db.getConnection()
                             .prepareStatement("SELECT * FROM CONTRACT where KEY = ?")) {
            int i = 0;
            pstmt.setString(++i, addr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                accountState = new AccountState(ByteUtil.hexStringToBytes(rs.getString("value")));
        } catch (SQLException e) {
            Logger.logErrorMessage("load account state from database error ", e);
        }
        return accountState;
    }

    public static byte[] getBytesValue(String addr) {
        byte[] result = null;
        try (PreparedStatement pstmt =
                     Db.db.getConnection()
                             .prepareStatement("SELECT * FROM CONTRACT where KEY = ?")) {
            int i = 0;
            pstmt.setString(++i, addr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                result = ByteUtil.hexStringToBytes(rs.getString("value"));
        } catch (SQLException e) {
            Logger.logErrorMessage("load the value of " + addr + " from database error ", e);
        }
        return result;
    }
}
