package org.conch.consensus.poc.db;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.consensus.poc.PocScore;
import org.conch.db.Db;
import org.conch.db.DbUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * 
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-05-17
 */
public class PocDb {

    public static void saveOrUpdate(PocScore pocScore) {
        if (pocScore == null || pocScore.getAccountId() == -1 || pocScore.getHeight() < 0 ) {
            return;
        }
        Connection con = null;
        try {
            con = Db.db.getConnection();

            PreparedStatement pstmtCount = con.prepareStatement("SELECT db_id from account_poc_score " 
                                                                + "WHERE account_id = ? AND height = ?");
            pstmtCount.setLong(1, pocScore.getAccountId());
            pstmtCount.setInt(2, pocScore.getHeight());
            
            ResultSet rs = pstmtCount.executeQuery();
            boolean exist = (rs != null) && rs.next();
            
            if(exist){
                update(con, pocScore);
            }else{
                insert(con, pocScore);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
    }
    
    private static int insert(Connection con, PocScore pocScore) throws SQLException {
        if(con == null) return 0;
        
        PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account_poc_score(account_id, "
                + " poc_score, height, poc_detail) VALUES(?, ?, ?, ?)");

        pstmtInsert.setLong(1, pocScore.getAccountId());
        pstmtInsert.setLong(2, pocScore.total().longValue());
        pstmtInsert.setInt(3, pocScore.getHeight());
        pstmtInsert.setString(4, pocScore.toJsonString());
        return pstmtInsert.executeUpdate();
    }

    private static int update(Connection con, PocScore pocScore) throws SQLException {
        String detail = pocScore.toJsonString();
        if(con == null || StringUtils.isEmpty(detail) || pocScore.getAccountId() == -1 || pocScore.getHeight() < 0 ){
            return 0;
        }
        
        PreparedStatement pstmtUpdate = con.prepareStatement("UPDATE account_poc_score SET poc_score=?, poc_detail=? WHERE account_id = ? AND height = ?");

        pstmtUpdate.setLong(1, pocScore.total().longValue());
        pstmtUpdate.setString(2, detail);
        pstmtUpdate.setLong(3, pocScore.getAccountId());
        pstmtUpdate.setInt(4, pocScore.getHeight());
        return pstmtUpdate.executeUpdate();
    }

    public static void delete(PocScore pocScore) {
        delete(pocScore.getAccountId(), pocScore.getHeight());
    }

    public static void delete(long accountId, int height) {
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

    private static String get(long accountId, int height, boolean loadHistory) {
        try {
            Conch.getBlockchain().readLock();
            if(height < 0) return null;
            
            // close the start height in query 
            int appointStart = -1;
            
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT poc_detail AS detail "
                        + "FROM account_poc_score WHERE account_id = ?"
                        + (loadHistory ? " AND height <= ?" : " AND height = ?")
                        + (appointStart != -1 ? " AND height > ?" : "")  
                        + " ORDER BY height DESC LIMIT 1");

                pstmt.setLong(1, accountId);
                pstmt.setInt(2, height);
                
                if(appointStart != -1) {
                    pstmt.setInt(3, appointStart);
                }

                ResultSet rs = pstmt.executeQuery();
                if(rs !=null && rs.next()){
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

    /**
     * load poc score record at specified height
     * @param accountId
     * @param height
     * @param loadHistory true-load history record when no record at specified height, false-return null if no record at specified height
     * @return
     */
    public static PocScore getPocScore(long accountId, int height, boolean loadHistory) {
        String detail = get(accountId, height, loadHistory);
        if(StringUtils.isEmpty(detail)) return null;
        
        return JSON.parseObject(detail, PocScore.class);
    }


    public static Map<Long,PocScore>  listAll() {
        Map<Long,PocScore> scoreMap = Maps.newHashMap();
        
        try {
            Conch.getBlockchain().readLock();
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT poc_detail AS detail FROM account_poc_score ORDER BY height DESC");

                ResultSet rs = pstmt.executeQuery();
                while(rs.next()){
                    try{
                        String detail = rs.getString("detail");
                        PocScore pocScore = JSON.parseObject(detail, PocScore.class);
                        
                        // compare the height
                        if(scoreMap.containsKey(pocScore.getAccountId())){
                            PocScore oldScore = scoreMap.get(pocScore.getAccountId());
                            if(oldScore.getHeight() < pocScore.getHeight()){
                                scoreMap.put(pocScore.getAccountId(), pocScore);
                            }
                        }else{
                            scoreMap.put(pocScore.getAccountId(), pocScore);
                        }
                    }catch (Exception e) {
                        // continue to fetch next
                        System.err.println(e);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }finally {
                DbUtils.close(con);
            }
        } finally {
            Conch.getBlockchain().readUnlock();
        }
        return scoreMap;
    }
}
