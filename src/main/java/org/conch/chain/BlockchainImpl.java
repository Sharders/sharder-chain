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

import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionDb;
import org.conch.tx.TransactionImpl;
import org.conch.tx.TransactionType;
import org.conch.util.Convert;
import org.conch.util.Filter;
import org.conch.util.ReadWriteUpdateLock;
import org.conch.vote.PhasingPoll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class BlockchainImpl implements Blockchain {

    private static final BlockchainImpl instance = new BlockchainImpl();

    public static BlockchainImpl getInstance() {
        return instance;
    }

    private BlockchainImpl() {}

    private final ReadWriteUpdateLock lock = new ReadWriteUpdateLock();
    private final AtomicReference<BlockImpl> lastBlock = new AtomicReference<>();

    @Override
    public void readLock() {
        lock.readLock().lock();
    }

    @Override
    public void readUnlock() {
        lock.readLock().unlock();
    }

    @Override
    public void updateLock() {
        lock.updateLock().lock();
    }

    @Override
    public void updateUnlock() {
        lock.updateLock().unlock();
    }

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    @Override
    public BlockImpl getLastBlock() {
        return lastBlock.get();
    }

    @Override
    public void setLastBlock(BlockImpl block) {
        lastBlock.set(block);
    }

    @Override
    public int getHeight() {
        BlockImpl last = lastBlock.get();
        return last == null ? 0 : last.getHeight();
    }

    @Override
    public long countIncludeTypeBlocks(List<String> includeType){
        Connection con = null;
        try {
            con = Db.db.getConnection();
            return TransactionDb.countByBlockIncludeType(con, includeType);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getLastBlockTimestamp() {
        BlockImpl last = lastBlock.get();
        return last == null ? 0 : last.getTimestamp();
    }

    @Override
    public BlockImpl getLastBlock(int timestamp) {
        BlockImpl block = lastBlock.get();
        if (timestamp >= block.getTimestamp()) {
            return block;
        }
        return BlockDb.findLastBlock(timestamp);
    }

    @Override
    public BlockImpl getBlock(long blockId) {
        BlockImpl block = lastBlock.get();
        if (block.getId() == blockId) {
            return block;
        }
        return BlockDb.findBlock(blockId);
    }

    @Override
    public boolean hasBlock(long blockId) {
        return lastBlock.get().getId() == blockId || BlockDb.hasBlock(blockId);
    }

    @Override
    public DbIterator<BlockImpl> getAllBlocks() {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     *
     * @param from
     * @param to
     * @param orderPair String[2]: orderPair[0] - sort field, orderPair[0] - sort direction
     * @return
     */
    private DbIterator<BlockImpl> _getBlocks(int from, int to, String[] orderPair) {
        String sortField = "height";
        String sortDirection = "DESC";
        if(orderPair != null && orderPair.length == 2) {
            sortField = orderPair[0];
            sortDirection = orderPair[1];
        }
        Connection con;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height <= ? AND height >= ? ORDER BY " + sortField + " " + sortDirection);
            int blockchainHeight = getHeight();
            pstmt.setInt(1, blockchainHeight - from);
            pstmt.setInt(2, blockchainHeight - to);
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     *
     * @param from
     * @param to
     * @param orderPair String[2]: orderPair[0] - sort field, orderPair[0] - sort direction
     * @return
     */
    private DbIterator<BlockImpl> _getBlocksByHeight(int from, int to, String[] orderPair) {
        String sortField = "height";
        String sortDirection = "DESC";
        if(orderPair != null && orderPair.length == 2) {
            sortField = orderPair[0];
            sortDirection = orderPair[1];
        }
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height <= ? AND height >= ? ORDER BY " + sortField + " " + sortDirection);
            pstmt.setInt(1, to);
            pstmt.setInt(2, from);
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<BlockImpl> getBlocksByHeight(int from, int to, String[] orderPair) {
        return _getBlocksByHeight(from, to, orderPair);
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(int from, int to, String[] orderPair) {
        return _getBlocks(from, to, orderPair);
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(int from, int to) {
       return _getBlocks(from, to, null);
    }


    @Override
    public DbIterator<BlockImpl> getBlocks(long accountId, int timestamp) {
        return getBlocks(accountId, timestamp, 0, -1);
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(long accountId, int timestamp, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE generator_id = ? "
                    + (timestamp > 0 ? " AND timestamp >= ? " : " ") + "ORDER BY height DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            if (timestamp > 0) {
                pstmt.setInt(++i, timestamp);
            }
            DbUtils.setLimits(++i, pstmt, from, to);
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getBlockCount(long accountId) {
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM block WHERE generator_id = ?")) {
            pstmt.setLong(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<BlockImpl> getBlocks(Connection con, PreparedStatement pstmt) {
        return new DbIterator<>(con, pstmt, BlockDb::loadBlock);
    }

    @Override
    public List<Long> getBlockIdsAfter(long blockId, int limit) {
        // Check the block cache
        List<Long> result = new ArrayList<>(BlockDb.BLOCK_CACHE_SIZE);
        synchronized(BlockDb.blockCache) {
            BlockImpl block = BlockDb.blockCache.get(blockId);
            if (block != null) {
                Collection<BlockImpl> cacheMap = BlockDb.heightMap.tailMap(block.getHeight() + 1).values();
                for (BlockImpl cacheBlock : cacheMap) {
                    if (result.size() >= limit) {
                        break;
                    }
                    result.add(cacheBlock.getId());
                }
                return result;
            }
        }
        // Search the database
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block "
                    + "WHERE db_id > IFNULL ((SELECT db_id FROM block WHERE id = ?), " + Long.MAX_VALUE + ") "
                    + "ORDER BY db_id ASC LIMIT ?");
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
        return result;
    }

    @Override
    public List<BlockImpl> getBlocksAfter(long blockId, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        // Check the block cache
        List<BlockImpl> result = new ArrayList<>(BlockDb.BLOCK_CACHE_SIZE);
        synchronized(BlockDb.blockCache) {
            BlockImpl block = BlockDb.blockCache.get(blockId);
            if (block != null) {
                Collection<BlockImpl> cacheMap = BlockDb.heightMap.tailMap(block.getHeight() + 1).values();
                for (BlockImpl cacheBlock : cacheMap) {
                    if (result.size() >= limit) {
                        break;
                    }
                    result.add(cacheBlock);
                }
                return result;
            }
        }
        // Search the database
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                    + "WHERE db_id > IFNULL ((SELECT db_id FROM block WHERE id = ?), " + Long.MAX_VALUE + ") "
                    + "ORDER BY db_id ASC LIMIT ?");
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(BlockDb.loadBlock(con, rs, true));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
        return result;
    }

    @Override
    public List<BlockImpl> getBlocksAfter(long blockId, List<Long> blockList) {
        if (blockList.isEmpty()) {
            return Collections.emptyList();
        }
        // Check the block cache
        List<BlockImpl> result = new ArrayList<>(BlockDb.BLOCK_CACHE_SIZE);
        synchronized(BlockDb.blockCache) {
            BlockImpl block = BlockDb.blockCache.get(blockId);
            if (block != null) {
                Collection<BlockImpl> cacheMap = BlockDb.heightMap.tailMap(block.getHeight() + 1).values();
                int index = 0;
                for (BlockImpl cacheBlock : cacheMap) {
                    if (result.size() >= blockList.size() || cacheBlock.getId() != blockList.get(index++)) {
                        break;
                    }
                    result.add(cacheBlock);
                }
                return result;
            }
        }
        // Search the database
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                    + "WHERE db_id > IFNULL ((SELECT db_id FROM block WHERE id = ?), " + Long.MAX_VALUE + ") "
                    + "ORDER BY db_id ASC LIMIT ?");
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, blockList.size());
            try (ResultSet rs = pstmt.executeQuery()) {
                int index = 0;
                while (rs.next()) {
                    BlockImpl block = BlockDb.loadBlock(con, rs, true);
                    if (block.getId() != blockList.get(index++)) {
                        break;
                    }
                    result.add(block);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
        return result;
    }

    @Override
    public long getBlockIdAtHeight(int height) {
        Block block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block.getId();
        }
        return BlockDb.findBlockIdAtHeight(height);
    }

    @Override
    public BlockImpl getBlockAtHeight(int height) {
        BlockImpl block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block;
        }
        return BlockDb.findBlockAtHeight(height);
    }

    @Override
    public BlockImpl getECBlock(int timestamp) {
        Block block = getLastBlock(timestamp);
        if (block == null) {
            return getBlockAtHeight(0);
        }
        return BlockDb.findBlockAtHeight(Math.max(block.getHeight() - 720, 0));
    }

    @Override
    public TransactionImpl getTransaction(long transactionId) {
        return TransactionDb.findTransaction(transactionId);
    }

    @Override
    public TransactionImpl getTransactionByFullHash(String fullHash) {
        return TransactionDb.findTransactionByFullHash(Convert.parseHexString(fullHash));
    }

    @Override
    public boolean hasTransaction(long transactionId) {
        return TransactionDb.hasTransaction(transactionId);
    }

    @Override
    public boolean hasTransactionByFullHash(String fullHash) {
        return TransactionDb.hasTransactionByFullHash(Convert.parseHexString(fullHash));
    }

    @Override
    public int getTransactionCount() {
        Connection con = null;
        boolean isInTx = Db.db.isInTransaction();
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction");
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (!isInTx) {
                DbUtils.close(con);
            }
        }
    }

    @Override
    public int getTransactionCountByType(int type, Connection con) {
        try {
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction WHERE TYPE = ?");
            pstmt.setInt(1, type);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getTransactionCountByAccount(long accountId, byte type,byte subtype){

        StringBuilder buf = new StringBuilder();
        buf.append("SELECT COUNT(*) FROM transaction WHERE (recipient_id = ? or sender_id = ?) ");
        if(type >= 0){
            buf.append("AND type = ? ");
            if(subtype >= 0){
                buf.append("AND subtype = ? ");
            }
        }
        int i = 0;
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(buf.toString());
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            if(type >= 0){
                pstmt.setByte(++i, type);
                if(subtype >= 0){
                    pstmt.setInt(++i, subtype);
                }
            }
            try(ResultSet rs = pstmt.executeQuery()){
                rs.next();
                return rs.getInt(1);
            }catch (Exception e){
                throw new RuntimeException(e.toString(),e);
            }
        }catch (SQLException e){
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getAllTransactions() {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction ORDER BY db_id ASC");
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    /**
     * 查询账户的收款付款交易记录
     * @param accountId
     * @param type
     * @param isFrom accountId是否是发送方
     * @param from
     * @param to
     * @return
     */
    @Override
    public DbIterator<TransactionImpl> getTransactions(long accountId, byte type, boolean isFrom, int from, int to) {
        Connection con = null;
        PreparedStatement pstmt;
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("SELECT transaction.* FROM transaction where type=" + type + " ");
            if (isFrom) {
                buf.append("And sender_id = ? ");
            } else {
                buf.append("And recipient_id = ? ");
            }
            buf.append("ORDER BY block_timestamp DESC, transaction_index DESC");
            buf.append(DbUtils.limitsClause(from, to));
            con = Db.db.getConnection();
            int i = 0;
            pstmt = con.prepareStatement(buf.toString());
            pstmt.setLong(++i,accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return getTransactions(con, pstmt);
        }catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * 查询指定交易集合
     * @return
     */
    @Override
    public DbIterator<TransactionImpl> getTransactions(long accountId, byte type, boolean isFrom, int from, int to, int endHeight) {
        Connection con = null;
        PreparedStatement pstmt;
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("SELECT transaction.* FROM transaction where type=" + type + " ");
            if (isFrom) {
                buf.append("And sender_id = ? ");
            } else {
                buf.append("And recipient_id = ? ");
            }
            buf.append("And height <= ? ");
            buf.append("ORDER BY block_timestamp DESC, transaction_index DESC");
            buf.append(DbUtils.limitsClause(from, to));
            con = Db.db.getConnection();
            int i = 0;
            pstmt = con.prepareStatement(buf.toString());
            pstmt.setLong(++i,accountId);
            pstmt.setInt(++i,endHeight);
            DbUtils.setLimits(++i, pstmt, from, to);
            return getTransactions(con, pstmt);
        }catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getTransactions(long accountId, byte type, byte subtype, int blockTimestamp,
                                                       boolean includeExpiredPrunable) {
        return getTransactions(accountId, 0, type, subtype, blockTimestamp, false, false, false, 0, -1, includeExpiredPrunable, false);
    }

    @Override
    public DbIterator<TransactionImpl> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                                       int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                                       int from, int to, boolean includeExpiredPrunable, boolean executedOnly) {

        if (phasedOnly && nonPhasedOnly) {
            throw new IllegalArgumentException("At least one of phasedOnly or nonPhasedOnly must be false");
        }
        int height = numberOfConfirmations > 0 ? getHeight() - numberOfConfirmations : Integer.MAX_VALUE;
        if (height < 0) {
            throw new IllegalArgumentException("Number of confirmations required " + numberOfConfirmations
                    + " exceeds current blockchain height " + getHeight());
        }
        Connection con = null;
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("SELECT transaction.* FROM transaction ");
            if (executedOnly && !nonPhasedOnly) {
                buf.append(" LEFT JOIN phasing_poll_result ON transaction.id = phasing_poll_result.id ");
            }
            buf.append("WHERE recipient_id = ? AND sender_id <> ? ");
            if (blockTimestamp > 0) {
                buf.append("AND block_timestamp >= ? ");
            }
            if (type >= 0) {
                buf.append("AND type = ? ");
                if (subtype >= 0) {
                    buf.append("AND subtype = ? ");
                }
            }
            if (height < Integer.MAX_VALUE) {
                buf.append("AND transaction.height <= ? ");
            }
            if (withMessage) {
                buf.append("AND (has_message = TRUE OR has_encrypted_message = TRUE ");
                buf.append("OR ((has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE) AND timestamp > ?)) ");
            }
            if (phasedOnly) {
                buf.append("AND phased = TRUE ");
            } else if (nonPhasedOnly) {
                buf.append("AND phased = FALSE ");
            }
            if (executedOnly && !nonPhasedOnly) {
                buf.append("AND (phased = FALSE OR approved = TRUE) ");
            }
            buf.append("UNION ALL SELECT transaction.* FROM transaction ");
            if (executedOnly && !nonPhasedOnly) {
                buf.append(" LEFT JOIN phasing_poll_result ON transaction.id = phasing_poll_result.id ");
            }
            buf.append("WHERE sender_id = ? ");
            if (blockTimestamp > 0) {
                buf.append("AND block_timestamp >= ? ");
            }
            if (type >= 0) {
                buf.append("AND type = ? ");
                if (subtype >= 0) {
                    buf.append("AND subtype = ? ");
                }
            }
            if (height < Integer.MAX_VALUE) {
                buf.append("AND transaction.height <= ? ");
            }
            if (withMessage) {
                buf.append("AND (has_message = TRUE OR has_encrypted_message = TRUE OR has_encrypttoself_message = TRUE ");
                buf.append("OR ((has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE) AND timestamp > ?)) ");
            }
            if (phasedOnly) {
                buf.append("AND phased = TRUE ");
            } else if (nonPhasedOnly) {
                buf.append("AND phased = FALSE ");
            }
            if (executedOnly && !nonPhasedOnly) {
                buf.append("AND (phased = FALSE OR approved = TRUE) ");
            }

            buf.append("ORDER BY block_timestamp DESC, transaction_index DESC");
            buf.append(DbUtils.limitsClause(from, to));
            con = Db.db.getConnection();
            PreparedStatement pstmt;
            int i = 0;
            pstmt = con.prepareStatement(buf.toString());
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            if (blockTimestamp > 0) {
                pstmt.setInt(++i, blockTimestamp);
            }
            if (type >= 0) {
                pstmt.setByte(++i, type);
                if (subtype >= 0) {
                    pstmt.setByte(++i, subtype);
                }
            }
            if (height < Integer.MAX_VALUE) {
                pstmt.setInt(++i, height);
            }
            int prunableExpiration = Math.max(0, Constants.INCLUDE_EXPIRED_PRUNABLE && includeExpiredPrunable ?
                                        Conch.getEpochTime() - Constants.MAX_PRUNABLE_LIFETIME :
                                        Conch.getEpochTime() - Constants.MIN_PRUNABLE_LIFETIME);
            if (withMessage) {
                pstmt.setInt(++i, prunableExpiration);
            }
            pstmt.setLong(++i, accountId);
            if (blockTimestamp > 0) {
                pstmt.setInt(++i, blockTimestamp);
            }
            if (type >= 0) {
                pstmt.setByte(++i, type);
                if (subtype >= 0) {
                    pstmt.setByte(++i, subtype);
                }
            }
            if (height < Integer.MAX_VALUE) {
                pstmt.setInt(++i, height);
            }
            if (withMessage) {
                pstmt.setInt(++i, prunableExpiration);
            }
            DbUtils.setLimits(++i, pstmt, from, to);
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getReferencingTransactions(long transactionId, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT transaction.* FROM transaction, referenced_transaction "
                    + "WHERE referenced_transaction.referenced_transaction_id = ? "
                    + "AND referenced_transaction.transaction_id = transaction.id "
                    + "ORDER BY transaction.block_timestamp DESC, transaction.transaction_index DESC "
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, transactionId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<TransactionImpl> getTransactions(Connection con, PreparedStatement pstmt) {
        return new DbIterator<>(con, pstmt, TransactionDb::loadTransaction);
    }

    @Override
    public List<TransactionImpl> getExpectedTransactions(Filter<Transaction> filter) {
        Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        BlockchainProcessorImpl blockchainProcessor = BlockchainProcessorImpl.getInstance();
        List<TransactionImpl> result = new ArrayList<>();
      
        try {
            readLock();
            
            if (getHeight() >= Constants.PHASING_BLOCK_HEIGHT) {
                DbIterator<TransactionImpl> phasedTransactions = null;
                try {
                    phasedTransactions = PhasingPoll.getFinishingTransactions(getHeight() + 1);
                    for (TransactionImpl phasedTransaction : phasedTransactions) {
                        try {
                            phasedTransaction.validate();
                            if (!phasedTransaction.attachmentIsDuplicate(duplicates, false) && filter.ok(phasedTransaction)) {
                                result.add(phasedTransaction);
                            }
                        } catch (ConchException.ValidationException ignore) {
                        }
                    }
                }finally {
                    DbUtils.close(phasedTransactions);
                }
            }
            blockchainProcessor.selectUnconfirmedTransactions(duplicates, getLastBlock(), -1).forEach(
                    unconfirmedTransaction -> {
                        TransactionImpl transaction = unconfirmedTransaction.getTransaction();
                        if (transaction.getPhasing() == null && filter.ok(transaction)) {
                            result.add(transaction);
                        }
                    }
            );
        } finally {
            readUnlock();
        }
        return result;
    }
}
