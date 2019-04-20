package org.conch.consensus.genesis;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.account.Account;
import org.conch.chain.BlockImpl;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.crypto.Crypto;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.Logger;

import java.security.MessageDigest;
import java.util.*;

/**
 * Sharder Genesis 
 * @author  xy@sharder.org 
 * @date 01/19/2019
 */
public class SharderGenesis {

    public static final long GENESIS_BLOCK_ID = 6840612405442242239L;
    public static final long CREATOR_ID = 7690917826419382695L;
    public static final long KEEPER_ID = -4542396882408079631L;
    public static final byte[] CREATOR_PUBLIC_KEY = {
            -36, 27, -52, -114, -28, 115, -4, -120, 50, -66, -107, 70, -54, -95, 61, -14,
            79, 123, -18, -57, -99, 10, -34, 75, -48, -72, -25, 96, -53, -63, -1, 43
    };
    public static final byte[] CREATOR_SIGNATURES = {
            -79, 103, -74, -56, -6, 72, -57, -20, 59, 14, 92, 111, -116, 61, 7, -106, 38, 43, -105, 82, -112, -30, 55, -111, 3, 81, -15, 89, 5, -5, 20, 14, 58, -44, 
            122, 99, 123, 119, 54, 66, -19, -107, 71, -115, -89, -55, -27, 121, -122, 12, 31, -126, -98, -91, 92, -88, 48, 30, 43, 80, 94, 90, 98, -109
    };

    public static final byte[] GENESIS_BLOCK_SIGNATURE = new byte[]{
            58, 75, 72, 28, -115, 20, 91, 112, 87, 33, -23, 20, -40, -74, -108, 73, 52, 111, 94, 0, 87, 23, 22, 86, -91, 89, -37, 84, 29,
            48, 18, 15, -125, 97, -103, 106, -104, -125, -104, -33, 110, 99, -1, -79, -116, 25, 6, 73, 64, 34, 108, -33, 56, 107, -73, -60,
            17, 91, 104, -115, 67, -94, 3, -92
    };
    public static final byte[] GENESIS_PAYLOAD_HASH = new byte[]{
            -68, 29, 41, -120, -78, -7, -86, -93, -10, -89, -77, -46, 109, -49, 30, 72, -115, 77, 73, -19, -85, 125, -43, -13, -3, -44, -124, -62, 123, -68, 69, -81
    };
    
    private static boolean enableGenesisAccount = false;
    public static final void enableGenesisAccount(){
        if(enableGenesisAccount) {
            return;
        }

        Logger.logDebugMessage("Enable genesis account[size=" + (GenesisRecipient.getAll().size() + 1) + "]");

        Account.addOrGetAccount(CREATOR_ID).apply(CREATOR_PUBLIC_KEY);
        
        for(GenesisRecipient genesisRecipient : GenesisRecipient.getAll()){
            Account.addOrGetAccount(genesisRecipient.id).apply(genesisRecipient.publicKey);
        }
        enableGenesisAccount = true;
    }


    public static class GenesisPeer {
        public String domain;
        public Peer.Type type ;

        private GenesisPeer(String domain,Peer.Type type){
            this.domain = domain;
            this.type = type;
        }

        static Map<Constants.Network, List<GenesisPeer>> genesisPeers = new HashMap<>();
        static {
            List<GenesisPeer> devnetPeers = Lists.newArrayList(
                    new GenesisPeer("devboot.sharder.io",Peer.Type.FOUNDATION),
                    new GenesisPeer("devna.sharder.io",Peer.Type.FOUNDATION),
                    new GenesisPeer("devnb.sharder.io",Peer.Type.FOUNDATION)
            );

            List<GenesisPeer> testnetPeers = Lists.newArrayList(
                    new GenesisPeer("testboot.sharder.io",Peer.Type.FOUNDATION),
                    new GenesisPeer("testna.sharder.io",Peer.Type.COMMUNITY),
                    new GenesisPeer("testnb.sharder.io",Peer.Type.HUB)
            );

            List<GenesisPeer> mainnetPeers = Lists.newArrayList(

            );
            genesisPeers.put(Constants.Network.DEVNET,devnetPeers);
            genesisPeers.put(Constants.Network.TESTNET,testnetPeers);
            genesisPeers.put(Constants.Network.MAINNET,mainnetPeers);
        }

        public static List<GenesisPeer> getAll(){
            return genesisPeers.get(Constants.getNetwork());
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
    
    public static boolean isGenesisRecipients(long accountId){
        GenesisRecipient recipient = GenesisRecipient.getByAccountId(accountId);
        return recipient == null ? false : true;
    }
    
    public static boolean isGenesisCreator(long accountId){
        return CREATOR_ID == accountId ? true : false;
    }

    private static long genesisBlockAmount(){
        long total = 0;
        for(GenesisRecipient genesisRecipient : GenesisRecipient.getAll()){
            total += genesisRecipient.amount * Constants.ONE_SS;
        }
        return total;
    }
    
    private SharderGenesis() {}

    /**
     * all genesis txs
     * @return
     * @throws ConchException.NotValidException
     */
    public static List<TransactionImpl> genesisTxs() throws ConchException.NotValidException {
        List<TransactionImpl> transactions = coinbaseTxs();
        transactions.add(defaultPocWeightTableTx());
        transactions.addAll(nodeTypeTxs());
        Collections.sort(transactions, Comparator.comparingLong(Transaction::getId));
        return transactions;
    }

    /**
     * genesis block:
     * @param fixedPayloadHash
     * @return
     * @throws ConchException.NotValidException
     */
    private static BlockImpl genesisBlock(boolean fixedPayloadHash) throws ConchException.NotValidException {
        byte[] payloadHash = SharderGenesis.GENESIS_PAYLOAD_HASH;
        List<TransactionImpl> transactions = genesisTxs();
        if(!fixedPayloadHash) {
            MessageDigest digest = Crypto.sha256();
            for (TransactionImpl transaction : transactions) {
                digest.update(transaction.bytes());
            }
            payloadHash = digest.digest();
        }
        
        int blockVersion = -1;
        BlockImpl genesisBlock = BlockImpl.newGenesisBlock(
                        SharderGenesis.GENESIS_BLOCK_ID,
                        blockVersion,
                        0,
                        0,
                        genesisBlockAmount(),
                        0,
                        transactions.size() * 128,
                        payloadHash,
                        SharderGenesis.CREATOR_PUBLIC_KEY,
                        new byte[64],
                        SharderGenesis.GENESIS_BLOCK_SIGNATURE,
                        null,
                        transactions);
        genesisBlock.setPrevious(null);

        return genesisBlock;
    }
    
    /**
     * original coinbase, initial ss supply
     * @return coinbase txs
     */
    private static List<TransactionImpl> coinbaseTxs(){
        List<TransactionImpl> transactions = Lists.newArrayList();

        // coinbase txs
        long genesisCreatorId = Account.getId(SharderGenesis.CREATOR_PUBLIC_KEY);
        GenesisRecipient.getAll().forEach(recipient -> {
            try {
                transactions.add(new TransactionImpl.BuilderImpl(
                        (byte) 1,
                        recipient.publicKey,
                        recipient.amount * Constants.ONE_SS,
                        0,
                        (short) 0,
                        new Attachment.CoinBase(Attachment.CoinBase.CoinBaseType.GENESIS, genesisCreatorId, recipient.id, Maps.newHashMap()))
                        .timestamp(0)
                        .recipientId(recipient.id)
                        .signature(recipient.signature)
                        .height(0)
                        .ecBlockHeight(0)
                        .ecBlockId(0)
                        .build());
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }
        });
            
        return transactions;
    }

    
    /**
     * default node type tx for known peers
     * @return node-type txs
     */
    private static List<TransactionImpl> nodeTypeTxs() {
        List<TransactionImpl> transactions = Lists.newArrayList();

        GenesisPeer.getAll().forEach(genesisPeer -> {
            Attachment.AbstractAttachment attachment = new PocTxBody.PocNodeType(genesisPeer.domain,genesisPeer.type);
            try {
                transactions.add(new TransactionImpl.BuilderImpl(
                        (byte) 0,
                        SharderGenesis.CREATOR_PUBLIC_KEY,
                        0,
                        0,
                        (short) 0,
                        attachment)
                        .timestamp(0)
                        .signature(SharderGenesis.CREATOR_SIGNATURES)
                        .height(0)
                        .ecBlockHeight(0)
                        .ecBlockId(0)
                        .build());
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }
        });
        return transactions;
    }

    /**
     * default poc weight table
     * @return
     * @throws ConchException.NotValidException
     */
    private static TransactionImpl defaultPocWeightTableTx() throws ConchException.NotValidException {
        Attachment.AbstractAttachment attachment = PocTxBody.PocWeightTable.defaultPocWeightTable();
        return new TransactionImpl.BuilderImpl(
                (byte) 0,
                SharderGenesis.CREATOR_PUBLIC_KEY,
                0,
                0,
                (short) 0,
                attachment)
                .timestamp(0)
                .signature(SharderGenesis.CREATOR_SIGNATURES)
                .height(0)
                .ecBlockHeight(0)
                .ecBlockId(0)
                .build();
    }

    
    /**
     * genesis block that include genesis transactions:
     * 1. coinbase tx for the genesis account
     * 2. default poc weight table tx
     * @return genesis block
     */
    public static BlockImpl genesisBlock() throws ConchException.NotValidException {
        return genesisBlock(true);
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(GenesisPeer.genesisPeers.get(Constants.Network.MAINNET).toArray()));
        System.out.println(Arrays.toString(GenesisPeer.genesisPeers.get(Constants.Network.DEVNET).toArray()));
        System.out.println(Arrays.toString(GenesisPeer.genesisPeers.get(Constants.Network.TESTNET).toArray()));
    }
    
}

