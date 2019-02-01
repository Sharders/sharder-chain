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

import org.conch.common.ConchException;
import org.conch.db.DerivedDbTable;
import org.conch.mint.Generator;
import org.conch.peer.Peer;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;

public interface BlockchainProcessor extends Observable<Block, BlockchainProcessor.Event> {

    enum Event {
        BLOCK_PUSHED, BLOCK_POPPED, BLOCK_GENERATED, BLOCK_SCANNED,
        RESCAN_BEGIN, RESCAN_END,
        BEFORE_BLOCK_ACCEPT, AFTER_BLOCK_ACCEPT,
        BEFORE_BLOCK_APPLY, AFTER_BLOCK_APPLY
    }

    Peer getLastBlockchainFeeder();

    int getLastBlockchainFeederHeight();

    boolean isScanning();

    boolean isDownloading();

    boolean isProcessingBlock();

    int getMinRollbackHeight();

    int getInitialScanHeight();

    void processPeerBlock(JSONObject request) throws ConchException;

    void fullReset();

    void scan(int height, boolean validate);

    void fullScanWithShutdown();

    void setGetMoreBlocks(boolean getMoreBlocks);

    List<? extends Block> popOffTo(int height);

    void registerDerivedTable(DerivedDbTable table);

    void trimDerivedTables();

    int restorePrunedData();

    Transaction restorePrunedTransaction(long transactionId);

    class BlockNotAcceptedException extends ConchException {

        private final BlockImpl block;

        public BlockNotAcceptedException(String message, BlockImpl block) {
            super(message);
            this.block = block;
        }

        public BlockNotAcceptedException(Throwable cause, BlockImpl block) {
            super(cause);
            this.block = block;
        }

        @Override
        public String getMessage() {
            return block == null ? super.getMessage() : super.getMessage() + ", block " + block.getStringId() + " " + block.getJSONObject().toJSONString();
        }

    }

    class GeneratorNotAcceptedException extends ConchException {

        private final long generatoId;

        public GeneratorNotAcceptedException(String message, long generatoId) {
            super(message);
            this.generatoId = generatoId;
        }

        public GeneratorNotAcceptedException(Throwable cause, long generatoId) {
            super(cause);
            this.generatoId = generatoId;
        }
        
        public long getGeneratoId(){
            return this.generatoId;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", generator[" + generatoId + "]";
        }

    }

    class TransactionNotAcceptedException extends BlockNotAcceptedException {

        private final TransactionImpl transaction;

        public TransactionNotAcceptedException(String message, TransactionImpl transaction) {
            super(message, transaction.getBlock());
            this.transaction = transaction;
        }

        public TransactionNotAcceptedException(Throwable cause, TransactionImpl transaction) {
            super(cause, transaction.getBlock());
            this.transaction = transaction;
        }

        public TransactionImpl getTransaction() {
            return transaction;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", transaction " + transaction.getStringId() + " " + transaction.getJSONObject().toJSONString();
        }
    }

    class BlockOutOfOrderException extends BlockNotAcceptedException {

        public BlockOutOfOrderException(String message, BlockImpl block) {
            super(message, block);
        }

	}

}
