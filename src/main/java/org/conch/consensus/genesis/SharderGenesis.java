package org.conch.consensus.genesis;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.conch.account.Account;
import org.conch.chain.BlockImpl;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.crypto.Crypto;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.Logger;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Sharder Genesis 
 * @author  xy@sharder.org 
 * @date 01/19/2019
 */
public class SharderGenesis {

    public static final long GENESIS_BLOCK_ID = 6840612405442242239L;
    public static final long CREATOR_ID = 7690917826419382695L;
    public static final byte[] CREATOR_PUBLIC_KEY = {
            -36, 27, -52, -114, -28, 115, -4, -120, 50, -66, -107, 70, -54, -95, 61, -14,
            79, 123, -18, -57, -99, 10, -34, 75, -48, -72, -25, 96, -53, -63, -1, 43
    };
    public static final byte[] CREATOR_SIGNATURES = {
            -36, 27, -52, -114, -28, 115, -4, -120, 50, -66, -107, 70, -54, -95, 61, -14,
            79, 123, -18, -57, -99, 10, -34, 75, -48, -72, -25, 96, -53, -63, -1, 43
    };
    //    poc weight tx signature
    //    {
    //            -31,-20,-91,-48,-18,-86,94,-100,-98,93,-47,61,60,-72,-45,91,85,-98,88,36,19,-128,46,-58,-124,81,96,84,21,121,-101,9,107,-7,41,
    //            -93,61,23,55,-64,109,127,67,-44,-44,43,-114,-82,-47,-111,45,123,-48,115,11,50,86,-79,127,-125,-16,49,-73,-36
    //    };
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

        Logger.logDebugMessage("Enable genesis account[size=" + (GenesisRecipient.recipients.size() + 1) + "]");

        Account.addOrGetAccount(CREATOR_ID).apply(CREATOR_PUBLIC_KEY);
        
        for(GenesisRecipient genesisRecipient : GenesisRecipient.recipients){
            Account.addOrGetAccount(genesisRecipient.id).apply(genesisRecipient.publicKey);
        }
        enableGenesisAccount = true;
    }

    public static boolean isGenesisRecipients(long accountId){
        for(GenesisRecipient genesisRecipient : GenesisRecipient.recipients){
            if(genesisRecipient.id == accountId){
                return true;
            }
        }
        return false;
    }
    
    public static boolean isGenesisCreator(long accountId){
        return CREATOR_ID == accountId ? true : false;
    }

    private static long genesisBlockAmount(){
        long total = 0;
        for(GenesisRecipient genesisRecipient : GenesisRecipient.recipients){
            total += genesisRecipient.amount * Constants.ONE_SS;
        }
        return total;
    }
    
    private SharderGenesis() {}


    public static List<TransactionImpl> genesisTxs() throws ConchException.NotValidException {
        List<TransactionImpl> transactions = coinbaseTxs();
        transactions.add(defaultPocWeightTableTx());
        Collections.sort(transactions, Comparator.comparingLong(Transaction::getId));
        return transactions;
    }

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

        BlockImpl genesisBlock = BlockImpl.newGenesisBlock(
                        SharderGenesis.GENESIS_BLOCK_ID,
                        -1,
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
     * original coinbase, initial supply of ss
     * @return coinbase txs
     */
    private static List<TransactionImpl> coinbaseTxs(){
        List<TransactionImpl> transactions = Lists.newArrayList();

        // coinbase txs
        try{
            long genesisCreatorId = Account.getId(SharderGenesis.CREATOR_PUBLIC_KEY);
            for(GenesisRecipient genesisRecipient : GenesisRecipient.recipients){
                TransactionImpl transaction =
                new TransactionImpl.BuilderImpl(
                        (byte) 1,
                        genesisRecipient.publicKey,
                        genesisRecipient.amount * Constants.ONE_SS,
                        0,
                        (short) 0,
                        new Attachment.CoinBase(
                            Attachment.CoinBase.CoinBaseType.GENESIS, genesisCreatorId, genesisRecipient.id, Maps.newHashMap()))
                    .timestamp(0)
                    .recipientId(genesisRecipient.id)
                    .signature(genesisRecipient.signature)
                    .height(0)
                    .ecBlockHeight(0)
                    .ecBlockId(0)
                    .build();
                transactions.add(transaction);
            }
        }catch (ConchException.NotValidException e) {
            e.printStackTrace();
        }

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
     * genesis block that include genesis transacations:`
     * 1. coinbase tx for the genesis account
     * 2. default poc weight table tx
     * @return genesis block
     */
    public static BlockImpl genesisBlock() throws ConchException.NotValidException {
        return genesisBlock(true);
    }
    
}

