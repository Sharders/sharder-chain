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

package org.conch.storage.tx;

import org.conch.Conch;
import org.conch.chain.BlockchainProcessor;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.peer.Peers;
import org.conch.storage.Ssid;
import org.conch.storage.StorageBackup;
import org.conch.storage.Storer;
import org.conch.storage.ipfs.IpfsService;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newCachedThreadPool;

public class StorageTxProcessorImpl implements StorageTxProcessor {

    private static final StorageTxProcessorImpl instance = new StorageTxProcessorImpl();

    public static StorageTxProcessorImpl getInstance() {
        return instance;
    }
    private final ExecutorService storageService = newCachedThreadPool();
    private static final Map<Long,Map<String,Integer>> backupTask = new HashMap<>();

    public static void init(){

    }

    @Override
    public Transaction createBackupTransaction(Transaction transaction) {
        if (Storer.getStorer() != null) {
            Storer storer = Storer.getStorer();
                Attachment.DataStorageBackup attachment = new Attachment.DataStorageBackup(transaction.getId(), storer.getAccountId());
            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(
                     storer.getPublicKey(), 0, 0, transaction.getDeadline(), attachment
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
        if (transaction.getType().getType() == StorageTx.TYPE_STORAGE
                && transaction.getType().getSubtype() == StorageTx.STORAGE_UPLOAD.getSubtype()){
            return true;
        } else {
            return false;
        }
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
            //TODO[storage] storage add backup record into database table
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

    @Override
    public byte[] getData(long transactionId) throws IOException {
        Transaction storeTransaction = Conch.getBlockchain().getTransaction(transactionId);
        Attachment.DataStorageUpload storeAttachment = (Attachment.DataStorageUpload) storeTransaction.getAttachment();
        return IpfsService.retrieve(Ssid.decode(storeAttachment.getSsid()));
    }

    @Override
    public boolean backup(Transaction transaction) {
        Attachment.DataStorageBackup attachment = (Attachment.DataStorageBackup) transaction.getAttachment();
        Transaction storeTransaction = Conch.getBlockchain().getTransaction(attachment.getUploadTransaction());
        Attachment.DataStorageUpload storeAttachment = (Attachment.DataStorageUpload) storeTransaction.getAttachment();
        try{
            IpfsService.pin(Ssid.decode(storeAttachment.getSsid()));
        }catch (IOException e){
            Logger.logErrorMessage(transaction.getId() + " backup failed " ,e);
            return false;
        }
        StorageBackup.add(storeTransaction,transaction, Peers.isMyAddressAnnounced() ? Peers.getMyAddress() : IpfsService.myAddress());
        return true;
    }

    public void syncBackTable(Transaction transaction) {
        Attachment.DataStorageBackup attachment = (Attachment.DataStorageBackup) transaction.getAttachment();
        Transaction storeTransaction = Conch.getBlockchain().getTransaction(attachment.getUploadTransaction());
        StorageBackup.add(storeTransaction,transaction, Peers.isMyAddressAnnounced() ? Peers.getMyAddress() : IpfsService.myAddress());
    }

    public static void addTask(long id,int num){
        if(backupTask.containsKey(id)){
            backupTask.remove(id);
            return;
        }
        Map<String,Integer> map = new HashMap<>();
        map.put("need",num);
        map.put("current",0);
        backupTask.put(id,map);
    }

    public static void recordTask(long id,int num){
        if(backupTask.containsKey(id)){
            Map<String,Integer> map = backupTask.get(id);
            map.put("backup",1);
            return;
        }
        Map<String,Integer> map = new HashMap<>();
        map.put("need",num);
        map.put("current",0);
        map.put("backup",1);
        backupTask.put(id,map);
    }

    /**
     * Check storage exceed the required number, then update the backup numbers by storeId.
     * @param storeId
     */
    public static void checkAndupdateStoreNumbers(long storeId){
        if(backupTask.containsKey(storeId)){
            int num = backupTask.get(storeId).get("current") +1;
            backupTask.get(storeId).put("current",num);
            if(backupTask.get(storeId).get("current").equals(backupTask.get(storeId).get("need"))){
                backupTask.remove(storeId);
            }
        }
    }

    public static void clearBackupTask(){
        backupTask.clear();
    }

    /**
     * Clear first upload file created in org/conch/http/StoreData.java
     * @param transaction
     */
    public static void clearUploadTempFile(Transaction transaction){
        Attachment.DataStorageBackup attachment = (Attachment.DataStorageBackup) transaction.getAttachment();
        Transaction storeTransaction = Conch.getBlockchain().getTransaction(attachment.getUploadTransaction());
        Attachment.DataStorageUpload storeAttachment = (Attachment.DataStorageUpload) storeTransaction.getAttachment();
        if(Storer.getStorer() == null || !StorageBackup.isOwnerOfStorage(Storer.getStorer().getAccountId(),attachment.getUploadTransaction())){
            IpfsService.unpin(Ssid.decode(storeAttachment.getSsid()));
        }
    }

    static {
        if (Constants.isStorageClient) {
            Conch.getBlockchainProcessor().addListener(block -> {
                if(!Conch.getBlockchainProcessor().isDownloading() && !backupTask.isEmpty()){
                    if(Storer.getStorer() == null){
                        return;
                    }
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

                        if(storeTransaction == null || backupTask.get(id).containsKey("backup")){
                            return;
                        }

                        Transaction backupTransaction =  StorageTxProcessorImpl.getInstance().createBackupTransaction(storeTransaction);
                        if(backupTransaction != null){
                            try{
                                Conch.getTransactionProcessor().broadcast(backupTransaction);
                            }catch (ConchException.ValidationException e){
                                Logger.logErrorMessage("backup transaction validate failed ",e);
                            }
                        }
                        backupTask.remove(id);
                    }
                }
            }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
        }
    }
}