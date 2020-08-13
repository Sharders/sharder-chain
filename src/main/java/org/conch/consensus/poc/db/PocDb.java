package org.conch.consensus.poc.db;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.consensus.poc.PocScore;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.db.DerivedDbTable;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.util.Convert;
import org.conch.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-05-17
 */
public class PocDb  {

    /**
     * poc score table
     */
    private static class PocScoreTable extends DerivedDbTable {
        public PocScoreTable() {
            super("account_poc_score");
        }

        public int countAndRollback(int height) {
            if (!Db.db.isInTransaction()) {
                throw new IllegalStateException("Not in transaction");
            }
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM ACCOUNT_POC_SCORE WHERE height > ?")) {
                pstmtDelete.setInt(1, height);
                return pstmtDelete.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        public Map<Long,PocScore> listAll() {
            Map<Long,PocScore> scoreMap = Maps.newHashMap();
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT poc_detail AS detail FROM account_poc_score ORDER BY height DESC");

                ResultSet rs = pstmt.executeQuery();
                while(rs.next()){
                    try{
                        PocScore pocScore = new PocScore(rs.getString("detail"));

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
            return scoreMap;
        }

        public String get(long accountId, int height, boolean loadHistory) {
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
            return null;
        }

        public void saveOrUpdate(Connection con, PocScore pocScore) throws SQLException {
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
        }

        public int insert(Connection con, PocScore pocScore) throws SQLException {
            if(con == null) return 0;

            PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO account_poc_score(account_id, "
                    + " poc_score, height, poc_detail) VALUES(?, ?, ?, ?)");

            pstmtInsert.setLong(1, pocScore.getAccountId());
            pstmtInsert.setLong(2, pocScore.total().longValue());
            pstmtInsert.setInt(3, pocScore.getHeight());
            pstmtInsert.setString(4, pocScore.toSimpleJson());
            return pstmtInsert.executeUpdate();
        }

        public int update(Connection con, PocScore pocScore) throws SQLException {
            String detail = pocScore.toSimpleJson();
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

        @Override
        public void rollback(int height) {
            countAndRollback(height);
        }

        /**
         * Trim the poc score table
         *
         * @param   height                  Trim height
         */
        @Override
        public void trim(int height) {
            try (Connection con = db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("DELETE FROM account_poc_score WHERE height <= ?")) {
                pstmt.setInt(1, height);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }
    private static final PocScoreTable pocScoreTable = new PocScoreTable();


    /**
     * certified peer table
     */
    private static class CertifiedPeerTable extends DerivedDbTable {
        public CertifiedPeerTable() {
            super("certified_peer");
        }

        public int countAndRollback(int height) {
            if (!Db.db.isInTransaction()) {
                throw new IllegalStateException("Not in transaction");
            }
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM certified_peer WHERE height > ?")) {
                pstmtDelete.setInt(1, height);
                return pstmtDelete.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        public Map<Long, CertifiedPeer> listAll() {
            Map<Long,CertifiedPeer> peerMap = Maps.newHashMap();
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT * FROM certified_peer ORDER BY height DESC");

                ResultSet rs = pstmt.executeQuery();
                while(rs.next()){
                    try{
                        CertifiedPeer certifiedPeer = _parse(rs);
                        // compare the height
                        if(peerMap.containsKey(certifiedPeer.getBoundAccountId())){
                            CertifiedPeer oldPeer = peerMap.get(certifiedPeer.getBoundAccountId());
                            if(oldPeer.getHeight() < certifiedPeer.getHeight()){
                                peerMap.put(certifiedPeer.getBoundAccountId(), certifiedPeer);
                            }
                        }else{
                            peerMap.put(certifiedPeer.getBoundAccountId(), certifiedPeer);
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
            return peerMap;
        }

        private CertifiedPeer _parse(ResultSet rs){
            CertifiedPeer certifiedPeer = null;
            try{
                String host = rs.getString("host");
                Long linkedAccountId= rs.getLong("account_id");
                Peer.Type type = Peer.Type.getByCode(rs.getInt("type"));
                int height = rs.getInt("height");
                int lastUpdateEpochTime = rs.getInt("last_updated");
                certifiedPeer = new CertifiedPeer(type, host, linkedAccountId, Convert.fromEpochTime(lastUpdateEpochTime));
                certifiedPeer.setHeight(height);
            }catch (Exception e) {
                // continue to fetch next
                System.err.println(e);
            }
            return certifiedPeer;
        }

        public CertifiedPeer get(long accountId, int height, boolean loadHistory) {
            try {
                Conch.getBlockchain().readLock();
                if(height < 0) return null;

                // close the start height in query
                int appointStart = -1;

                Connection con = null;
                try {
                    con = Db.db.getConnection();
                    PreparedStatement pstmt = con.prepareStatement("SELECT * FROM certified_peer"
                            + " WHERE account_id = ?"
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
                        return _parse(rs);
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

        public CertifiedPeer get(String host, int height, boolean loadHistory) {
            try {
                Conch.getBlockchain().readLock();
                if(height < 0) return null;

                // close the start height in query
                int appointStart = -1;

                Connection con = null;
                try {
                    con = Db.db.getConnection();
                    PreparedStatement pstmt = con.prepareStatement("SELECT * FROM certified_peer"
                            + " WHERE host = ?"
                            + (loadHistory ? " AND height <= ?" : " AND height = ?")
                            + (appointStart != -1 ? " AND height > ?" : "")
                            + " ORDER BY height DESC LIMIT 1");

                    pstmt.setString(1, host);
                    pstmt.setInt(2, height);

                    if(appointStart != -1) {
                        pstmt.setInt(3, appointStart);
                    }

                    ResultSet rs = pstmt.executeQuery();
                    if(rs !=null && rs.next()){
                        return _parse(rs);
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

        public void saveOrUpdate(Connection con, CertifiedPeer certifiedPeer) throws SQLException {
            PreparedStatement pstmtCount = con.prepareStatement("SELECT db_id FROM certified_peer"
                    + " WHERE account_id = ? AND height = ?");
            pstmtCount.setLong(1, certifiedPeer.getBoundAccountId());
            pstmtCount.setInt(2, certifiedPeer.getHeight());

            ResultSet rs = pstmtCount.executeQuery();
            boolean exist = (rs != null) && rs.next();

            if(exist){
                update(con, certifiedPeer);
            }else{
                insert(con, certifiedPeer);
            }
        }

        public int insert(Connection con, CertifiedPeer certifiedPeer) throws SQLException {
            if(con == null) return 0;

            PreparedStatement pstmtInsert = con.prepareStatement("INSERT INTO certified_peer(host, "
                    + " account_id, type, height, last_updated) VALUES(?, ?, ?, ?, ?)");

            pstmtInsert.setString(1, certifiedPeer.getHost());
            pstmtInsert.setLong(2, certifiedPeer.getBoundAccountId());
            pstmtInsert.setInt(3, certifiedPeer.getTypeCode());
            pstmtInsert.setInt(4, certifiedPeer.getHeight());
            pstmtInsert.setInt(5, certifiedPeer.getUpdateTimeInEpochFormat());
            return pstmtInsert.executeUpdate();
        }

        public int update(Connection con, CertifiedPeer certifiedPeer) throws SQLException {
            if(con == null || certifiedPeer.getBoundAccountId() == -1 || certifiedPeer.getHeight() < 0 ){
                return 0;
            }

            PreparedStatement pstmtUpdate = con.prepareStatement("UPDATE certified_peer SET host=?, type=?, last_updated=? WHERE account_id = ? AND height = ?");

            pstmtUpdate.setString(1, certifiedPeer.getHost());
            pstmtUpdate.setInt(2, certifiedPeer.getTypeCode());
            pstmtUpdate.setInt(3, certifiedPeer.getUpdateTimeInEpochFormat());
            pstmtUpdate.setLong(4, certifiedPeer.getBoundAccountId());
            pstmtUpdate.setInt(5, certifiedPeer.getHeight());
            return pstmtUpdate.executeUpdate();
        }

        public void delete(long accountId, int height) {
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM certified_peer WHERE account_id = ? AND height = ?");

                pstmtDelete.setLong(1, accountId);
                pstmtDelete.setInt(2, height);
                pstmtDelete.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            } finally {
                DbUtils.close(con);
            }
        }

        @Override
        public void rollback(int height) {
            countAndRollback(height);
        }

        /**
         * Trim the poc score table
         *
         * @param   height                  Trim height
         */
        @Override
        public void trim(int height) {
            try (Connection con = db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("DELETE FROM certified_peer WHERE height <= ?")) {
                pstmt.setInt(1, height);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    }
    private static final CertifiedPeerTable certifiedPeerTable = new CertifiedPeerTable();

    public static int getLastScoreHeight(){
        int lastHeight = -1;
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT height FROM account_poc_score"
                    + " ORDER BY height DESC LIMIT 1");

            ResultSet rs = pstmt.executeQuery();
            if(rs !=null && rs.next()){
                lastHeight = rs.getInt("height");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
        return lastHeight;
    }

    public static void batchUpdateScore(Connection con, List<PocScore> pocScoreList) {
        if (pocScoreList == null || pocScoreList.size() < 0 ) {
            return;
        }

        pocScoreList.forEach(pocScore -> {
            try {
                pocScoreTable.saveOrUpdate(con, pocScore);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void saveOrUpdateScore(PocScore pocScore) {
        if (pocScore == null || pocScore.getAccountId() == -1 || pocScore.getHeight() < 0 ) {
            return;
        }
        Connection con = null;
        try {
            con = Db.db.getConnection();
            pocScoreTable.saveOrUpdate(con,pocScore);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
    }

    public static void deleteScore(PocScore pocScore) {
        pocScoreTable.delete(pocScore.getAccountId(), pocScore.getHeight());
    }

    public static int rollbackScore(int height) {
        return pocScoreTable.countAndRollback(height);
    }

    /**
     * load poc score record at specified height
     * @param accountId
     * @param height
     * @param loadHistory true-load history record when no record at specified height, false-return null if no record at specified height
     * @return
     */
    public static PocScore getPocScore(long accountId, int height, boolean loadHistory) {
        String detail = pocScoreTable.get(accountId, height, loadHistory);
        if(StringUtils.isEmpty(detail)) return null;

        return new PocScore(detail);
    }

    public static Map<Long,PocScore> listAllScore() {
        Logger.logInfoMessage("List all poc txs from DB at height %d", Conch.getHeight());
        return pocScoreTable.listAll();
    }


    public static Map<Long,CertifiedPeer> listAllPeers() {
        return certifiedPeerTable.listAll();
    }

    public static void saveOrUpdatePeer(CertifiedPeer certifiedPeer) {
        if (certifiedPeer == null
                || certifiedPeer.getBoundAccountId() == -1
                || certifiedPeer.getHeight() < 0 ) {
            return;
        }

        Connection con = null;
        try {
            con = Db.db.getConnection();
            certifiedPeerTable.saveOrUpdate(con, certifiedPeer);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
    }

    public static CertifiedPeer getPeer(long accountId, int height, boolean loadHistory) {
        return certifiedPeerTable.get(accountId, height, loadHistory);
    }

    public static CertifiedPeer getPeer(String host, int height, boolean loadHistory) {
        return certifiedPeerTable.get(host, height, loadHistory);
    }

    public static void deletePeer(CertifiedPeer certifiedPeer) {
        certifiedPeerTable.delete(certifiedPeer.getBoundAccountId(), certifiedPeer.getHeight());
    }

    public static int rollbackPeer(int height) {
        return certifiedPeerTable.countAndRollback(height);
    }
}
