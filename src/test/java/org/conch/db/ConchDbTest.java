package org.conch.db;

import org.conch.chain.BlockImpl;
import org.conch.common.ConchException;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.tx.TransactionDb;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-24
 */
public class ConchDbTest {

    public static BlockImpl loadBlock(Connection con, ResultSet rs, boolean loadTransactions) {
        try {
            int version = rs.getInt("version");
            int timestamp = rs.getInt("timestamp");
            long previousBlockId = rs.getLong("previous_block_id");
            long totalAmountNQT = rs.getLong("total_amount");
            long totalFeeNQT = rs.getLong("total_fee");
            int payloadLength = rs.getInt("payload_length");
            long generatorId = rs.getLong("generator_id");
            byte[] previousBlockHash = rs.getBytes("previous_block_hash");
            BigInteger cumulativeDifficulty = new BigInteger(rs.getBytes("cumulative_difficulty"));
            long baseTarget = rs.getLong("base_target");
            long nextBlockId = rs.getLong("next_block_id");
            int height = rs.getInt("height");
            byte[] generationSignature = rs.getBytes("generation_signature");
            byte[] blockSignature = rs.getBytes("block_signature");
            byte[] payloadHash = rs.getBytes("payload_hash");
            byte[] ext = rs.getBytes("ext");
            long id = rs.getLong("id");
            return new BlockImpl(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                    generatorId, generationSignature, blockSignature, previousBlockHash,
                    cumulativeDifficulty, baseTarget, nextBlockId, height, id, ext, loadTransactions ? TransactionDb.findBlockTransactions(con, id) : null);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    static BlockImpl getBlock(long blockId){
        // Search the database
        try (Connection con = Db.db.getConnection(); 
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE id = ?")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block = null;
                if (rs.next()) {
                    block = loadBlock(con, rs, false);
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    static BlockImpl getBlock(int height){
        // Search the database
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block;
                if (rs.next()) {
                    block = loadBlock(con, rs,false);
                } else {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

  public static void main(String[] args) throws ConchException.NotValidException { 
//    Db.init();
//    BlockImpl block0 = getBlock(6840612405442242239L);
//    System.out.println(block0);
//    System.out.println("block-0 block hash be calculated by bytes=>" + Arrays.toString(Crypto.sha256().digest(block0.bytes())));
//    BlockImpl block1 = getBlock(1);
//    System.out.println(block1);
//    System.out.println("block-1 previous block hash=>" + Arrays.toString(block1.getPreviousBlockHash()));
//    System.out.println(getBlock(2));
      SharderGenesis.genesisBlock();
  }
}
