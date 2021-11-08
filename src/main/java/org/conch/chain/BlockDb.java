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

package org.conch.chain;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.tx.TransactionDb;
import org.conch.tx.TransactionImpl;
import org.conch.util.Logger;

public final class BlockDb {

    /** Block cache */
    static final int BLOCK_CACHE_SIZE = 10;
    public static final Map<Long, BlockImpl> blockCache = new HashMap<>();
    public static final SortedMap<Integer, BlockImpl> heightMap = new TreeMap<>();
    public static final Map<Long, TransactionImpl> transactionCache = new HashMap<>();
    static final Blockchain blockchain = Conch.getBlockchain();
    static {
        Conch.getBlockchainProcessor().addListener((block) -> {
            synchronized (blockCache) {
                int height = block.getHeight();
                Iterator<BlockImpl> it = blockCache.values().iterator();
                while (it.hasNext()) {
                    Block cacheBlock = it.next();
                    int cacheHeight = cacheBlock.getHeight();
                    if (cacheHeight <= height - BLOCK_CACHE_SIZE || cacheHeight >= height) {
                        cacheBlock.getTransactions().forEach((tx) -> transactionCache.remove(tx.getId()));
                        heightMap.remove(cacheHeight);
                        it.remove();
                    }
                }
                block.getTransactions().forEach((tx) -> transactionCache.put(tx.getId(), (TransactionImpl)tx));
                heightMap.put(height, (BlockImpl)block);
                blockCache.put(block.getId(), (BlockImpl)block);
            }
        }, BlockchainProcessor.Event.BLOCK_PUSHED);
    }

    private static void clearBlockCache() {
        synchronized (blockCache) {
            blockCache.clear();
            heightMap.clear();
            transactionCache.clear();
        }
    }

    public static BlockImpl findBlock(long blockId) {
        // Check the block cache
        synchronized (blockCache) {
            BlockImpl block = blockCache.get(blockId);
            if (block != null) {
                return block;
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE id = ?")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block = null;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static boolean hasBlock(long blockId) {
        return hasBlock(blockId, Integer.MAX_VALUE);
    }

    public static boolean hasBlock(long blockId, int height) {
        // Check the block cache
        synchronized(blockCache) {
            BlockImpl block = blockCache.get(blockId);
            if (block != null) {
                return block.getHeight() <= height;
            }
        }
        // Search the database
        boolean isInTx = Db.db.isInTransaction();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT height FROM block WHERE id = ?");
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("height") <= height;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
    }

    public static long findBlockIdAtHeight(int height) {
        // Check the cache
        synchronized(blockCache) {
            BlockImpl block = heightMap.get(height);
            if (block != null) {
                return block.getId();
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static BlockImpl findBlockAtHeight(int height) {
        // Check the cache
        synchronized(blockCache) {
            BlockImpl block = heightMap.get(height);
            if (block != null) {
                return block;
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                } else {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static BlockImpl findLastBlock() {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY timestamp DESC LIMIT 1")) {
            BlockImpl block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static BlockImpl findLastBlock(int timestamp) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE timestamp <= ? ORDER BY timestamp DESC LIMIT 1")) {
            pstmt.setInt(1, timestamp);
            BlockImpl block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static Set<Long> getBlockGenerators(int startHeight) {
        Set<Long> generators = new HashSet<>();
        Connection con = null;
        boolean isInTx = Db.db.isInTransaction();
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                    "SELECT generator_id, COUNT(generator_id) AS count FROM block WHERE height >= ? GROUP BY generator_id");
            pstmt.setInt(1, startHeight);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("count") > 1) {
                        generators.add(rs.getLong("generator_id"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
        return generators;
    }

    public static BlockImpl loadBlock(Connection con, ResultSet rs) {
        return loadBlock(con, rs, false);
    }

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
            int rewardDistributionHeight = rs.getInt("REWARD_DISTRIBUTION_HEIGHT");
            return new BlockImpl(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                    generatorId, generationSignature, blockSignature, previousBlockHash,
                    cumulativeDifficulty, baseTarget, nextBlockId, height, id, ext, loadTransactions ? TransactionDb.findBlockTransactions(con, id) : null, rewardDistributionHeight);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void saveBlock(Connection con, BlockImpl block) {
        try {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO block (id, version, timestamp, previous_block_id, "
                    + "total_amount, total_fee, payload_length, previous_block_hash, cumulative_difficulty, "
                    + "base_target, height, generation_signature, block_signature, payload_hash, generator_id, ext, REWARD_DISTRIBUTION_HEIGHT) "
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, block.getId());
                pstmt.setInt(++i, block.getVersion());
                pstmt.setInt(++i, block.getTimestamp());
                DbUtils.setLongZeroToNull(pstmt, ++i, block.getPreviousBlockId());
                pstmt.setLong(++i, block.getTotalAmountNQT());
                pstmt.setLong(++i, block.getTotalFeeNQT());
                pstmt.setInt(++i, block.getPayloadLength());
                pstmt.setBytes(++i, block.getPreviousBlockHash());
                pstmt.setBytes(++i, block.getCumulativeDifficulty().toByteArray());
                pstmt.setLong(++i, block.getBaseTarget());
                pstmt.setInt(++i, block.getHeight());
                pstmt.setBytes(++i, block.getGenerationSignature());
                pstmt.setBytes(++i, block.getBlockSignature());
                pstmt.setBytes(++i, block.getPayloadHash());
                pstmt.setLong(++i, block.getGeneratorId());
                pstmt.setBytes(++i, block.getExtension());
                pstmt.setInt(++i, block.getRewardDistributionHeight());
                pstmt.executeUpdate();
                TransactionDb.saveTransactions(con, block.getTransactions());
            }
            if (block.getPreviousBlockId() != 0) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?")) {
                    pstmt.setLong(1, block.getId());
                    pstmt.setLong(2, block.getPreviousBlockId());
                    pstmt.executeUpdate();
                }
                BlockImpl previousBlock;
                synchronized (blockCache) {
                    previousBlock = blockCache.get(block.getPreviousBlockId());
                }
                if (previousBlock != null) {
                    previousBlock.setNextBlockId(block.getId());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void deleteBlocksFromHeight(int height) {
        long blockId;
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                blockId = rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        Logger.logDebugMessage("Deleting blocks starting from height %s", height);
        BlockDb.deleteBlocksFrom(blockId);
    }

    // relying on cascade triggers in the database to delete the transactions and public keys for all deleted blocks
    public static BlockImpl deleteBlocksFrom(long blockId) {
        if (!Db.db.isInTransaction()) {
            BlockImpl lastBlock;
            try {
                Db.db.beginTransaction();
                lastBlock = deleteBlocksFrom(blockId);
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            return lastBlock;
        }
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT db_id FROM block WHERE timestamp >= "
                     + "IFNULL ((SELECT timestamp FROM block WHERE id = ?), " + Integer.MAX_VALUE + ") ORDER BY timestamp DESC");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM block WHERE db_id = ?")) {
            try {
                pstmtSelect.setLong(1, blockId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    Db.db.commitTransaction();
                    while (rs.next()) {
        	            pstmtDelete.setLong(1, rs.getLong("db_id"));
            	        pstmtDelete.executeUpdate();
                        Db.db.commitTransaction();
                    }
	            }
                BlockImpl lastBlock = findLastBlock();
                lastBlock.setNextBlockId(0);
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = NULL WHERE id = ?")) {
                    pstmt.setLong(1, lastBlock.getId());
                    pstmt.executeUpdate();
                }
                Db.db.commitTransaction();
                return lastBlock;
            } catch (SQLException e) {
                Db.db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            clearBlockCache();
        }
    }

    public static void deleteAll(boolean deletePocRefs) {
        if (!Db.db.isInTransaction()) {
            try {
                Db.db.beginTransaction();
                deleteAll(deletePocRefs);
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            return;
        }
        Logger.logMessage("Deleting blockchain...");
        try (Connection con = Db.db.getConnection();
             Statement stmt = con.createStatement()) {
            try {
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
                stmt.executeUpdate("TRUNCATE TABLE transaction");
                stmt.executeUpdate("TRUNCATE TABLE block");
                if(deletePocRefs) {
                    try {
                        stmt.executeUpdate("TRUNCATE TABLE account_pool");
                        stmt.executeUpdate("TRUNCATE TABLE account_poc_socre");
                    } catch (SQLException e) {
                        Logger.logWarningMessage("Ignore the unknown exception when deleting account_pool and account_poc_socre tables. Exception is %s", e.getMessage());
                    }
                }
                BlockchainProcessorImpl.getInstance().getDerivedTables().forEach(table -> {
                    if (table.isPersistent()) {
                        try {
                            stmt.executeUpdate("TRUNCATE TABLE " + table.toString());
                        } catch (SQLException ignore) {}
                    }
                });
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
                Db.db.commitTransaction();
            } catch (SQLException e) {
                Db.db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            clearBlockCache();
        }
    }

    /**
     * if the latest rewardDistributionHeight more than the height over settlement height, return true.
     * @return
     */
    public static boolean reachRewardSettlementHeight(int height) throws ConchException.StopException {
        Connection con = null;
        boolean isIntx = Db.db.isInTransaction();
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("select REWARD_DISTRIBUTION_HEIGHT from BLOCK where HEIGHT <= ? order by REWARD_DISTRIBUTION_HEIGHT desc limit 1");
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int rewardDistributionHeightDifference = height - rs.getInt("REWARD_DISTRIBUTION_HEIGHT");
                    int rewardSettlementHeight = Constants.rewardCalculatorInstance.getRewardSettlementHeight(height);
                    if(rewardSettlementHeight <= 0){
                        return false;
                    }

                    if (rewardDistributionHeightDifference == rewardSettlementHeight) {
                        return true;
                    } else if (rewardDistributionHeightDifference > rewardSettlementHeight) {
                        Block rollbackHeight = blockchain.getBlockAtHeight(rs.getInt("REWARD_DISTRIBUTION_HEIGHT") + rewardSettlementHeight - 1);
                        BlockImpl lastBlock = BlockDb.deleteBlocksFrom(rollbackHeight.getId());
                        blockchain.setLastBlock(lastBlock);
                        BlockchainProcessorImpl.getInstance().popOffTo(lastBlock);
                        Logger.logWarningMessage("current height over the reward distribution height," +
                                "roll back to the reward distribution height %d", lastBlock.getHeight());
                        Conch.restartApplication(null);
                    }
                }
                return false;
            }
        } catch (SQLException | ConchException.StopException e) {
            if (e instanceof ConchException.StopException){
                throw new ConchException.StopException("current height over the reward distribution height," +
                        "roll back to the reward distribution height " + height);
            }
            throw new RuntimeException(e);
        }finally {
            if (!isIntx) {
                DbUtils.close(con);
            }
        }
        //return (height % Constants.SETTLEMENT_INTERVAL_SIZE) == 0;
    }

    /**
     * Load all un-settlement blocks: current turn blocks & missing/un-settlement blocks
     * @param height
     * @return
     */
    public static List<? extends Block> getSettlementBlocks(int height) {
        try (Connection con = Db.db.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE REWARD_DISTRIBUTION_HEIGHT = 0 and Height <= ? order by height asc");
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<BlockImpl> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(loadBlock(con, rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * update the rewardDistributionHeight of the block record according the list of blockId
     * @param blockIds
     * @param height
     */
    public static void updateDistributionState(List<Long> blockIds, int height) {
        if (blockIds == null || blockIds.size() == 0) {
            return;
        }
        Logger.logDebugMessage("Update the REWARD_DISTRIBUTION_HEIGHT of blocks to current height ", Arrays.toString(blockIds.toArray()));

        try (Connection con = Db.db.getConnection()) {
            Statement stmt = con.createStatement();
            StringBuilder sqlStringBuilder = new StringBuilder();
            sqlStringBuilder.append("UPDATE block SET REWARD_DISTRIBUTION_HEIGHT = " + height + " WHERE ID in (");
            for (Long blockId : blockIds) {
                sqlStringBuilder.append(blockId + ",");
            }
            sqlStringBuilder.replace(sqlStringBuilder.length() - 1, sqlStringBuilder.length(), "");
            sqlStringBuilder.append(")");
            stmt.execute(sqlStringBuilder.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    public static int getLatestRewardHeight() {
        boolean isInTx = Db.db.isInTransaction();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement preparedStatement = con.prepareStatement("SELECT REWARD_DISTRIBUTION_HEIGHT from BLOCK order by REWARD_DISTRIBUTION_HEIGHT desc limit 1");
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("REWARD_DISTRIBUTION_HEIGHT");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
    }

    /**
     * roll back the reward distribution height of block to 0
     * @param latestRewardHeight
     */
    public static void rollBackRewardHeight(int latestRewardHeight) {
        boolean isInTx = Db.db.isInTransaction();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement preparedStatement = con.prepareStatement("update BLOCK set REWARD_DISTRIBUTION_HEIGHT = 0 where REWARD_DISTRIBUTION_HEIGHT = ?");
            preparedStatement.setInt(1, latestRewardHeight);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
    }

    public static int getAmountByGenerator(long generatorId) {
        boolean isInTx = Db.db.isInTransaction();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            try (PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) as count from BLOCK where GENERATOR_ID = ?")) {
                pstmt.setLong(1, generatorId);
                ResultSet resultSet = pstmt.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getInt("count");
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
    }
}
