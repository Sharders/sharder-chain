package org.conch.consensus.poc;

import org.conch.db.DbIterator;
import org.conch.db.DbKey;
import org.conch.db.PersistentDbTable;
import org.conch.tx.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**********************************************************************************
 * @package org.conch.consensus.poc
 * @author Wolf Tian
 * @email twenbin@sharder.org
 * @company Sharder Foundation
 * @website https://www.sharder.org/
 * @creatAt 2018-Dec-17 15:33 Mon
 * @tel 18716387615
 * @comment
 **********************************************************************************/
public class PocInfo {

    private static final DbKey.LongKeyFactory<PocInfo> pocKeyFactory = new DbKey.LongKeyFactory<PocInfo>("poc_id") {
        @Override
        public DbKey newKey(PocInfo poc) {
            return poc.dbKey;
        }
    };

    private static  final PersistentDbTable<PocInfo> pocTable = new PersistentDbTable<PocInfo>("poc_info", pocKeyFactory) {
        @Override
        protected PocInfo load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new PocInfo(rs, dbKey);
        }

        @Override
        protected void save(Connection con, PocInfo poc) throws SQLException {
            poc.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY height DESC, poc_id DESC ";
        }
    };

    private final DbKey dbKey;
    private final long pocId;
    private final long configTransaction;
    private final long templateTransaction;
    private final long onlineRateTransaction;
    private final long blockMissTransaction;
    private final long bcTransaction;
    private int height;

    private PocInfo(ResultSet rs, DbKey dbKey) throws SQLException {
        this.dbKey = dbKey;
        this.pocId = rs.getLong("poc_id");
        this.configTransaction = rs.getLong("config_transaction");
        this.templateTransaction = rs.getLong("template_transaction");
        this.onlineRateTransaction = rs.getLong("online_rate_transaction");
        this.blockMissTransaction = rs.getLong("block_miss_transaction");
        this.bcTransaction = rs.getLong("bc_transaction");
        this.height = rs.getInt("height");
    }

    public PocInfo(Transaction configTransaction, Transaction templateTransaction, Transaction onlineRateTransaction, Transaction blockMissTransaction, Transaction bcTransaction) {
        this.dbKey = pocKeyFactory.newKey(templateTransaction.getId());
        this.pocId = templateTransaction.getSenderId();
        this.configTransaction = configTransaction.getId();
        this.templateTransaction = templateTransaction.getId();
        this.onlineRateTransaction = onlineRateTransaction.getId();
        this.blockMissTransaction = blockMissTransaction.getId();
        this.bcTransaction = bcTransaction.getId();
        this.height = templateTransaction.getHeight();
    }

    public static int getCount() {
        return pocTable.getCount();
    }

    public static DbIterator<PocInfo> getAll(int from, int to) {
        return pocTable.getAll(from, to);
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO poc_info (poc_id, config_transaction, template_transaction, online_rate_transaction, block_miss_transaction, bc_transaction, height) VALUES ( ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.pocId);
            pstmt.setLong(++i, this.configTransaction);
            pstmt.setLong(++i, this.templateTransaction);
            pstmt.setLong(++i, this.onlineRateTransaction);
            pstmt.setLong(++i, this.blockMissTransaction);
            pstmt.setLong(++i, this.bcTransaction);
            pstmt.setInt(++i, this.height);
            pstmt.executeUpdate();
        }
    }

}
