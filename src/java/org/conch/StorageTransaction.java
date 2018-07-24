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

package org.conch;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public abstract class StorageTransaction extends TransactionType {

    private static final byte SUBTYPE_STORAGE_STORE = 0;
    private static final byte SUBTYPE_STORAGE_BACKUP = 1;
    private static final byte SUBTYPE_STORAGE_EXTEND = 2;

    static TransactionType findTransactionType(byte subtype) {
        switch (subtype) {
            case SUBTYPE_STORAGE_STORE:
                return STORAGE_UPLOAD;
            case SUBTYPE_STORAGE_BACKUP:
                return STORAGE_BACKUP;
            default:
                return null;
        }
    }

    //TODO storage fee adjust
    private static final Fee STORAGE_FEE = new Fee.SizeBasedFee(Constants.ONE_SS, Constants.ONE_SS / 10) {
        @Override
        public int getSize(TransactionImpl transaction, Appendix appendix) {
            return appendix.getFullSize();
        }
    };

    private StorageTransaction() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_STORAGE;
    }

    @Override
    final Fee getBaselineFee(Transaction transaction) {
        return STORAGE_FEE;
    }

    @Override
    final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }

    @Override
    public final boolean isPhasable() {
        return false;
    }

    public static final TransactionType STORAGE_UPLOAD = new StorageTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_STORAGE_STORE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.STORAGE_UPLOAD;
        }

        @Override
        Attachment.DataStorageUpload parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.DataStorageUpload(buffer, transactionVersion);
        }

        @Override
        Attachment.DataStorageUpload parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return new Attachment.DataStorageUpload(attachmentData);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.DataStorageUpload attachment = (Attachment.DataStorageUpload) transaction.getAttachment();
            Map<String, Integer> map = new HashMap<>();
            map.put(transaction.getStringId(), attachment.getReplicated_number());
            duplicates.put(STORAGE_UPLOAD, map);
            return false;
        }

        @Override
        void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            // TODO storage add full validate conditions
            Attachment.DataStorageUpload attachment = (Attachment.DataStorageUpload) transaction.getAttachment();
            if (attachment.getName().length() == 0 || attachment.getName().length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
                throw new ConchException.NotValidException("Invalid name length: " + attachment.getName().length());
            }
            if (attachment.getDescription().length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
                throw new ConchException.NotValidException("Invalid description length: " + attachment.getDescription().length());
            }
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.DataStorageUpload attachment = (Attachment.DataStorageUpload) transaction.getAttachment();
            if (Storer.getStorer() != null) {
                StorageProcessorImpl.addTask(transaction.getId(), attachment.getReplicated_number());
            }
            long storeFee = transaction.getFeeNQT() - ((TransactionImpl) transaction).getMinimumFeeNQT(BlockchainImpl.getInstance().getHeight());
            Account account = Account.getAccount(transaction.getSenderId());
            account.frozenNQT(AccountLedger.LedgerEvent.STORAGE_UPLOAD, transaction.getId(), storeFee);
        }

        @Override
        public String getName() {
            return "DataStoreUpload";
        }

        @Override
        boolean isPruned(long transactionId) {
            return TaggedData.isPruned(transactionId);
        }

    };

    public static final TransactionType STORAGE_BACKUP = new StorageTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_STORAGE_BACKUP;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.STORAGE_BACKUP;
        }

        @Override
        Attachment.DataStorageBackup parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.DataStorageBackup(buffer, transactionVersion);
        }

        @Override
        Attachment.DataStorageBackup parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return new Attachment.DataStorageBackup(attachmentData);
        }

        @Override
        boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.DataStorageBackup attachment = (Attachment.DataStorageBackup) transaction.getAttachment();

            Transaction storeTransaction = Conch.getBlockchain().getTransaction(attachment.getUploadTransaction());
            boolean hasStoreTransaction = false;
            if (storeTransaction != null) {
                hasStoreTransaction = true;
            } else {
                storeTransaction = Conch.getTransactionProcessor().getUnconfirmedTransaction(attachment.getUploadTransaction());
            }
            int  replicated_number = 0;
            if (duplicates.containsKey(STORAGE_UPLOAD) &&
                    duplicates.get(STORAGE_UPLOAD).containsKey(Long.toString(attachment.getUploadTransaction()))) {
                hasStoreTransaction = true;
                replicated_number = duplicates.get(STORAGE_UPLOAD).get(Long.toString(attachment.getUploadTransaction()));
            }else {
                Attachment.DataStorageUpload dataStorageUpload = (Attachment.DataStorageUpload) storeTransaction.getAttachment();
                replicated_number = dataStorageUpload.getReplicated_number();
            }
            int num = replicated_number - StorageBackup.getCurrentBackupNum(attachment.getUploadTransaction());
            boolean duplicate = num == 0 || isDuplicate(STORAGE_BACKUP, Long.toString(attachment.getUploadTransaction()), duplicates, num);

            return !hasStoreTransaction || duplicate;
        }

        @Override
        void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            // TODO storage add full validate conditions
            Attachment.DataStorageBackup attachment = (Attachment.DataStorageBackup) transaction.getAttachment();
//            if (!TransactionDb.hasTransaction(attachment.getUploadTransaction())) {
//                throw new ConchException.NotValidException("Transaction not accepted: " + attachment.getUploadTransaction());
//            }
            if (Account.getAccount(attachment.getStorerId()) == null) {
                throw new ConchException.NotValidException("Invalid storer account: " + attachment.getStorerId());
            }
        }

        @Override
        void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.DataStorageBackup attachment = (Attachment.DataStorageBackup) transaction.getAttachment();
            Transaction storeTransaction = Conch.getBlockchain().getTransaction(attachment.getUploadTransaction());
            if (Storer.getStorer() != null && Storer.getStorer().getAccountId() == transaction.getSenderId()) {
                StorageProcessorImpl.getInstance().backup(transaction);
            } else {
                StorageProcessorImpl.getInstance().synsBackTable(transaction);
            }

            if (Storer.getStorer() != null) {
                StorageProcessorImpl.updateTaskList(storeTransaction.getId());
            }

            if (Constants.isStorageClient) {
                StorageProcessorImpl.clearUploadTempFile(transaction);
            }

            long storeFee = storeTransaction.getFeeNQT() - ((TransactionImpl) storeTransaction).getMinimumFeeNQT(storeTransaction.getHeight());
            storeFee /= ((Attachment.DataStorageUpload) storeTransaction.getAttachment()).getReplicated_number();
            storeFee *= 0.1;
            Account account = Account.getAccount(storeTransaction.getSenderId());
            account.frozenNQT(AccountLedger.LedgerEvent.STORAGE_BACKUP, transaction.getId(), -storeFee);

            Account backupAccount = Account.getAccount(transaction.getSenderId());
            backupAccount.addToBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.STORAGE_BACKUP, transaction.getId(), storeFee);
            backupAccount.addToForgedBalanceNQT(storeFee);
        }

        @Override
        public String getName() {
            return "DataStoreBackup";
        }

    };
}
