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

import org.conch.db.*;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.Filter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public interface Blockchain {

    void readLock();

    void readUnlock();

    void updateLock();

    void updateUnlock();

    Block getLastBlock();

    Block getLastBlock(int timestamp);

    int getHeight();

    int getLastBlockTimestamp();

    Block getBlock(long blockId);

    Block getBlockAtHeight(int height);

    boolean hasBlock(long blockId);

    DbIterator<? extends Block> getAllBlocks();

    /**
     * get blocks follow the order conditions
     * @param orderPair String[2]: orderPair[0] - sort field, orderPair[0] - sort direction, value should be 'ASC' or 'DESC'
     *                  e.g. orderPair[0]="height", orderPair[1]="ASC"
     * @return
     */
    DbIterator<? extends Block> getBlocks(int from, int to, String[] orderPair);

    DbIterator<? extends Block> getBlocks(int from, int to);

    DbIterator<? extends Block> getBlocks(long accountId, int timestamp);

    DbIterator<? extends Block> getBlocks(long accountId, int timestamp, int from, int to);

    int getBlockCount(long accountId);

    DbIterator<? extends Block> getBlocks(Connection con, PreparedStatement pstmt);

    List<Long> getBlockIdsAfter(long blockId, int limit);

    List<? extends Block> getBlocksAfter(long blockId, int limit);

    List<? extends Block> getBlocksAfter(long blockId, List<Long> blockList);

    long getBlockIdAtHeight(int height);

    Block getECBlock(int timestamp);

    Transaction getTransaction(long transactionId);

    Transaction getTransactionByFullHash(String fullHash);

    boolean hasTransaction(long transactionId);

    boolean hasTransactionByFullHash(String fullHash);

    int getTransactionCount();

    int getTransactionCountByType(int type, Connection con);

    int getTransactionCountByAccount(long accountId, byte type, byte subtype, boolean isRecipient, boolean isSender);

    int getTransactionCountByAccount(long accountId, byte type, byte subtype,String recipientRS,String senderRS);

    DbIterator<? extends Transaction> getAllTransactions();

    DbIterator<TransactionImpl> getTransactions(long accountId, byte type, boolean isFrom, int from, int to);

    DbIterator<TransactionImpl> getTransactions(long senderId, long receiverId, byte type, int from, int to);

    DbIterator<TransactionImpl> getTransactions(long accountId, byte type, boolean isFrom, int limitFrom, int limitTo, int endHeight);

    DbIterator<? extends Transaction> getTransactions(long accountId, byte type, byte subtype, int blockTimestamp,
                                                      boolean includeExpiredPrunable);

    DbIterator<? extends Transaction> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                                      int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                                      int from, int to, boolean includeExpiredPrunable, boolean executedOnly);

    DbIterator<? extends Transaction> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                                      int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                                      int from, int to, boolean includeExpiredPrunable, boolean executedOnly,String recipientRS,String senderRS);

    DbIterator<? extends Transaction> getTransactions(Connection con, PreparedStatement pstmt);

    List<? extends Transaction> getExpectedTransactions(Filter<Transaction> filter);

    DbIterator<? extends Transaction> getReferencingTransactions(long transactionId, int from, int to);

    long countIncludeTypeBlocks(List<String> includeType);

    DbIterator<BlockImpl> getBlocksByHeight(int from, int to, String[] orderPair);

    void setLastBlock(BlockImpl block);
}
