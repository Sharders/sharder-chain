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

import org.conch.peer.Peers;
import org.conch.storage.Ssid;
import org.conch.storage.ipfs.IpfsService;
import org.conch.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageProcessorImpl implements StorageProcessor {

    private static final StorageProcessorImpl instance = new StorageProcessorImpl();

    public static StorageProcessorImpl getInstance() {
        return instance;
    }
    private final ExecutorService storageService = Executors.newCachedThreadPool();
    private static final Map<Long,Map<String,Integer>> backupTask = new HashMap<>();

    public static void init(){

    }

    @Override
    public Transaction createBackupTransaction(Transaction transaction) {
        //TODO
        if (Storer.getStorer() != null) {
            Storer storer = Storer.getStorer();
                Attachment.DataStorageBackup attachment = new Attachment.DataStorageBackup(transaction.getId(), storer.getAccountId());
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
        Transaction storeTransaction = Conch.getBlockchain().getTransaction(transactionId);
        Attachment.DataStorageUpload storeAttachment = (Attachment.DataStorageUpload) storeTransaction.getAttachment();
        return IpfsService.retrieve(Ssid.decode(storeAttachment.getSsid()));
    }

    @Override
    public boolean backup(Transaction transaction) {
        Attachment.DataStorageBackup attachment = (Attachment.DataStorageBackup) transaction.getAttachment();
        Transaction storeTransaction = Conch.getBlockchain().getTransaction(attachment.getUploadTransaction());
        Attachment.DataStorageUpload storeAttachment = (Attachment.DataStorageUpload) transaction.getAttachment();
        try{
            IpfsService.pin(Ssid.decode(storeAttachment.getSsid()));
        }catch (IOException e){
            Logger.logErrorMessage(transaction.getId() + " backup failed " ,e);
            return false;
        }
        StorageBackup.add(storeTransaction,transaction);
        return true;
    }

    public static void addTask(long id,int num){
        if(backupTask.containsKey(id)){
            Logger.logDebugMessage("backup task list already contains " + id);
            return;
        }
        Map<String,Integer> map = new HashMap<>();
        map.put("need",num);
        map.put("current",0);
        backupTask.put(id,map);
    }

    public static void updateTaskList(long storeId){
        if(backupTask.containsKey(storeId)){
            int num = backupTask.get(storeId).get("current") +1;
            backupTask.get(storeId).put("current",num);
            if(backupTask.get(storeId).get("current") == backupTask.get(storeId).get("need")){
                backupTask.remove(storeId);
            }
        }
    }

    static {
        if (Constants.isStorageClient) {
            Conch.getBlockchainProcessor().addListener(block -> {
                if(!Conch.getBlockchainProcessor().isDownloading() && !backupTask.isEmpty()){
                    for(long id : backupTask.keySet()){
                        Transaction storeTransaction = Conch.getBlockchain().getTransaction(id);
                        if(storeTransaction == null){
                            storeTransaction = Conch.getTransactionProcessor().getUnconfirmedTransaction(id);
                        }
                        if(storeTransaction == null){
                            for(Transaction transaction : block.getTransactions()){
                                if(transaction.getId() == id){
                                    storeTransaction = transaction;
                                    break;
                                }
                            }
                        }

                        Transaction backupTransaction =  StorageProcessorImpl.getInstance().createBackupTransaction(storeTransaction);
                        try{
                            Conch.getTransactionProcessor().broadcast(backupTransaction);
                        }catch (ConchException.ValidationException e){
                            Logger.logErrorMessage("backup transaction validate failed ",e);
                        }
                    }
                }
            }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
        }
    }
}