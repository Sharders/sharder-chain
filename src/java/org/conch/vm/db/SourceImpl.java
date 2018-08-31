package org.conch.vm.db;

import org.conch.Db;
import org.conch.util.Logger;
import org.conch.vm.util.ByteUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SourceImpl implements SourceI<byte[], byte[]> {

    @Override
    public void put(byte[] key, byte[] val) {
        try (PreparedStatement pstmt =
                     Db.db.getConnection()
                             .prepareStatement("MERGE INTO CONTRACT (KEY, VALUE) VALUES (?, ?)")) {
            int i = 0;
            pstmt.setString(++i, ByteUtil.toHexString(key));
            pstmt.setString(++i, ByteUtil.toHexString(val));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.logErrorMessage("Contract save value to database failed!");
        }
    }

    @Override
    public byte[] get(byte[] key) {
        byte[] result = null;
        try (PreparedStatement pstmt =
                     Db.db.getConnection()
                             .prepareStatement("SELECT * FROM CONTRACT where KEY = ?")) {
            int i = 0;
            pstmt.setString(++i, ByteUtil.toHexString(key));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                result = ByteUtil.hexStringToBytes(rs.getString("value"));
        } catch (SQLException e) {
            Logger.logErrorMessage("load the value of " + ByteUtil.toHexString(key) + " from database error ", e);
        }
        return result;
    }

    @Override
    public void delete(byte[] key) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM CONTRACT WHERE KEY = ")) {
            pstmt.setString(1, ByteUtil.toHexString(key));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public boolean flush() {
        return false;
    }
}
