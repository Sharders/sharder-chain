package org.conch.consensus.poc.db;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.common.Constants;
import org.conch.consensus.poc.PocScore;
import org.conch.db.*;
import org.conch.tx.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-05-17
 */
public class PocDb {

    public void insert(PocScore pocScore) {
        if (pocScore == null) return;
        
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account_poc_score (account_id, "
                    + " poc_score, height, poc_detail) KEY (account_id, height) VALUES(?, ?, ?, ?)");

            pstmtInsert.setLong(1, pocScore.getAccountId());
            pstmtInsert.setLong(2, pocScore.total().longValue());
            pstmtInsert.setInt(3, pocScore.getHeight());
            pstmtInsert.setString(4, pocScore.toJsonString());
            pstmtInsert.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
    }


    public void delete(long accountId, int height) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM account_poc_score WHERE account_id = ? AND height = ?");

            pstmtDelete.setLong(1, accountId);
            pstmtDelete.setInt(2, height);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) { 
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
    }

    private String get(long accountId, int height) {
        Conch.getBlockchain().readLock();
        try {
            if(height < 0) return null;
            
            int startHeight = height;
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT poc_detail AS detail "
                        + "FROM account_poc_score WHERE account_id = ? AND height > ? AND height <= ? ORDER BY height DESC LIMIT 1");

                pstmt.setLong(1, accountId);
                pstmt.setInt(2, startHeight);
                pstmt.setInt(3, height);

                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    return rs.getString("detail");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }finally {
                DbUtils.close(con);
            }
        } finally {
            Conch.getBlockchain().readUnlock();
        }
        return null;
    }

    public PocScore getPocScore(long accountId, int height) {
        String detail = get(accountId, height);
        if(StringUtils.isEmpty(detail)) return null;
        
        return JSON.parseObject(detail, PocScore.class);
    }

}
