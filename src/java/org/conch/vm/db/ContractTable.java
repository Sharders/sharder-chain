package org.conch.vm.db;

import org.conch.Db;
import org.conch.util.Logger;
import org.conch.vm.util.ByteUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContractTable {
    private final String key;
    private final String value;
    //state root only has one key, data base can't cache it when transaction has more than one state
    //so it can't rollback when something is wrong
    private final static Map<String, String> stateCache = new ConcurrentHashMap<>();
    //if the table doesn"t have the value, it saves the "null"
    private final static Map<String, String> oldState = new ConcurrentHashMap<>();

    public ContractTable(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static void init() {
    }

    public void save() {
        if (key == null || "".equals(key))
            return;

        if (stateCache.containsKey(key) && !"null".equals(stateCache.get(key))) {
            stateCache.put(key, value);
            return;
        }

        ContractTable table = getInstance(key);
        if (table == null) {
            oldState.put(key, "null");
        } else {
            oldState.put(key, table.value);
        }
        stateCache.put(key, value);
    }

    public static void rollback() {
        if (oldState.isEmpty())
            return;
        for (String key : oldState.keySet()) {
            if ("null".equals(oldState.get(key))) {
                deleteDb(key);
            } else {
                saveDb(key, oldState.get(key));
            }
        }

        oldState.clear();
        stateCache.clear();
    }

    public static void commit() {
        if (stateCache.isEmpty())
            return;
        for (String key : stateCache.keySet()) {
            if ("null".equals(stateCache.get(key))) {
                deleteDb(key);
            } else {
                saveDb(key, stateCache.get(key));
            }
        }

        oldState.clear();
        stateCache.clear();
    }

    private static void deleteDb(String key) {
        String sql = "DELETE FROM CONTRACT WHERE KEY = ?";
        try (PreparedStatement pstmt =
                     Db.db.getConnection()
                             .prepareStatement(sql)) {
            int i = 0;
            pstmt.setString(++i, key);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.logErrorMessage("rollback state root error", e);
        }
    }

    private static void saveDb(String key, String value) {
        String sql = "MERGE INTO CONTRACT (KEY, VALUE) VALUES (?, ?)";
        try (PreparedStatement pstmt =
                     Db.db.getConnection()
                             .prepareStatement(sql)) {
            int i = 0;
            pstmt.setString(++i, key);
            pstmt.setString(++i, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.logErrorMessage("rollback state root error", e);
        }
    }

    public static byte[] getValueByKey(String key) {
        ContractTable table = getInstance(key);
        if (table == null)
            return null;
        return ByteUtil.hexStringToBytes(table.getValue());
    }

    public static ContractTable getInstance(String key) {
        if (stateCache.containsKey(key) && !"null".equals(stateCache.get(key)))
            return new ContractTable(key, stateCache.get(key));

        try (PreparedStatement pstmt =
                     Db.db.getConnection()
                             .prepareStatement("select * FROM CONTRACT WHERE KEY = ?")) {
            int i = 0;
            pstmt.setString(++i, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return new ContractTable(rs.getString("key"), rs.getString("value"));
        } catch (SQLException e) {
            Logger.logErrorMessage("commit state root error", e);
        }
        return null;
    }

    public void delete() {
        if (stateCache.containsKey(key) && !"null".equals(stateCache.get(key))) {
            stateCache.put(key, "null");
            return;
        }

        ContractTable table = getInstance(key);
        if (table == null) {
            oldState.put(key, "null");
            Logger.logDebugMessage("try to delete the key without value!");
        } else {
            oldState.put(key, table.value);
        }
        stateCache.put(key, "null");
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
