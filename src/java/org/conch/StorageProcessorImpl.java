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

import org.conch.peer.Peer;
import org.conch.storage.Ssid;
import org.conch.storage.ipfs.IpfsService;
import org.conch.util.*;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageProcessorImpl implements StorageProcessor {

    private static final StorageProcessorImpl instance = new StorageProcessorImpl();

    public static StorageProcessorImpl getInstance() {
        return instance;
    }
    private final ExecutorService storageService = Executors.newCachedThreadPool();

    @Override
    public Transaction createBackupTransaction(Transaction transaction) {
        //TODO
        if (Storer.getStorer() != null) {
            Storer storer = Storer.getStorer();
            Attachment.DataStorageBackup attachment = new Attachment.DataStorageBackup(transaction.getId(), storer.getAccountId(), "");
            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(
                    (byte) 1, storer.getPublicKey(), 0, 0, transaction.getDeadline(),
                    attachment
                    );
            try {
                transaction =  builder.build(Storer.getStorer().getSecretPhrase());
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }

            return transaction;
        } else {
            return null;
        }
    }

    @Override
    public boolean isStorageUploadTransaction(Transaction transaction) {
        if (transaction.getType().getType() == StorageTransaction.TYPE_STORAGE
                && transaction.getType().getSubtype() == StorageTransaction.STORAGE_UPLOAD.getSubtype()){
            return true;
        } else
            return false;
    }

    public void setBackupStatus(Transaction transaction) {
        Attachment.DataStorageBackup attachment =  (Attachment.DataStorageBackup)transaction.getAttachment();
        storageService.submit(new backupStatusTask(attachment));
    }

    private class backupStatusTask implements Runnable {
        private Attachment.DataStorageBackup attachment;
        public backupStatusTask(Attachment.DataStorageBackup attachment) {
            this.attachment = attachment;
        }

        @Override
        public void run() {
            long uploadTransaction = attachment.getUploadTransaction();
            long storerId = attachment.getStorerId();
            //TODO storage add backup record into database table
            try {
                Db.db.beginTransaction();

                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
        }
    }

    public byte[] getData(long transactionId) throws IOException {
        String ssid = Storage.getDataStorage(transactionId).getSsid();
        return IpfsService.retrieve(Ssid.decode(ssid));
    }

    @Override
    public String backup(Transaction transaction) {
        return null;
    }


}