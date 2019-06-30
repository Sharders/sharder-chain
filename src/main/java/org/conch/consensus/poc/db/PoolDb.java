package org.conch.consensus.poc.db;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-05-25
 */
public class PoolDb {

    public static void saveOrUpdate(List<SharderPoolProcessor> list) {
        if(list == null || list.size() <=0 ) return;
        Connection con = null;
        try {
            con = Db.db.getConnection();
            for(SharderPoolProcessor poolProcessor : list){
                saveOrUpdate(con, poolProcessor);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
    }
    
    public static void saveOrUpdate(Connection con, SharderPoolProcessor poolProcessor) {
        if (poolProcessor == null || poolProcessor.getCreatorId() == -1 || poolProcessor.getPoolId() == -1) {
            return;
        }
        
        boolean ctlCon = (con == null);
        try {
            if(ctlCon)  con = Db.db.getConnection();

            PreparedStatement pstmtCount = con.prepareStatement("SELECT db_id from account_pool " 
                                                                + "WHERE pool_id = ?");
            pstmtCount.setLong(1, poolProcessor.getPoolId());
            
            ResultSet rs = pstmtCount.executeQuery();
            boolean exist = (rs != null) && rs.next();
            
            if(exist){
                update(con, poolProcessor);
            }else{
                insert(con, poolProcessor);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            if(ctlCon)  DbUtils.close(con);
        }
    }

    public static int countByAccountId(Long creatorId, int state) {
        Connection con = null;
        try {
            con = Db.db.getConnection();

            PreparedStatement pstmtCount = con.prepareStatement("SELECT COUNT(db_id) as num from account_pool " + "WHERE creator_id = ? and state = ?");
            pstmtCount.setLong(1, creatorId);
            pstmtCount.setInt(2, state);

            ResultSet rs = pstmtCount.executeQuery();
            if(rs != null && rs.next()){
                return rs.getInt("num");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
        return 0;
    }
    
    private static int insert(Connection con, SharderPoolProcessor poolProcessor) throws SQLException {
        if(con == null) return 0;
        
        PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account_pool(pool_id, "
                + " creator_id, state, pool_detail) VALUES(?, ?, ?, ?)");

        pstmtInsert.setLong(1, poolProcessor.getPoolId());
        pstmtInsert.setLong(2, poolProcessor.getCreatorId());
        pstmtInsert.setInt(3, poolProcessor.getState().ordinal());
        String poolJsonStr = poolProcessor.toJsonStr();
        pstmtInsert.setString(4, poolJsonStr);
        Logger.logDebugMessage("Insert Pool Json String: " + poolJsonStr);
        return pstmtInsert.executeUpdate();
    }

    private static int update(Connection con, SharderPoolProcessor poolProcessor) throws SQLException {
        String detail = poolProcessor.toJsonStr();
        if(con == null || StringUtils.isEmpty(detail) || poolProcessor.getCreatorId() == -1 || poolProcessor.getPoolId() == -1 ){
            return 0;
        }
        
        PreparedStatement pstmtUpdate = con.prepareStatement("UPDATE account_pool SET creator_id=?, state=?, pool_detail=? WHERE pool_id = ?");

        pstmtUpdate.setLong(1,poolProcessor.getCreatorId());
        pstmtUpdate.setInt(2, poolProcessor.getState().ordinal());
        pstmtUpdate.setString(3, detail);
        pstmtUpdate.setLong(4, poolProcessor.getPoolId());
//        Logger.logDebugMessage("Update Pool Json String: " + detail);
        return pstmtUpdate.executeUpdate();
    }

    public static void delete(SharderPoolProcessor poolProcessor) {
        delete(poolProcessor.getPoolId());
    }

    public static void delete(long poolId) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM account_pool WHERE pool_id = ?");

            pstmtDelete.setLong(1, poolId);
            pstmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(con);
        }
    }

    public static List<SharderPoolProcessor> list(int state,boolean equal){
        List<SharderPoolProcessor> list = Lists.newArrayList();
        try {
            Conch.getBlockchain().readLock();
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT pool_detail AS detail FROM account_pool where "
                        + (equal ? "state = ?" : "state != ?"));

                pstmt.setInt(1, state);
                ResultSet rs = pstmt.executeQuery();
                while(rs.next()){
                    try{
                        String detail = rs.getString("detail");
                        list.add(JSON.parseObject(detail, SharderPoolProcessor.class));
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
        return list; 
    }
    
    private static String get(long poolId) {
        try {
            Conch.getBlockchain().readLock();
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT pool_detail AS detail "
                        + "FROM account_pool WHERE pool_id = ?");

                pstmt.setLong(1, poolId);

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
     * @return
     */
    public static SharderPoolProcessor getPool(long poolId) {
        String detail = get(poolId);
        if(StringUtils.isEmpty(detail)) return null;
        
        return JSON.parseObject(detail, SharderPoolProcessor.class);
    }
}
