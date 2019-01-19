package org.conch.consensus;

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
 * 海螺链的创世节点,按照如下方式分配初始资金=> 以下为测试数据.
 * @author  xy@ichaoj.com
 * @version 16/7/1
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
    public static final byte[] GENESIS_BLOCK_SIGNATURE = new byte[]{
            58, 75, 72, 28, -115, 20, 91, 112, 87, 33, -23, 20, -40, -74, -108, 73, 52, 111, 94, 0, 87, 23, 22, 86, -91, 89, -37, 84, 29,
            48, 18, 15, -125, 97, -103, 106, -104, -125, -104, -33, 110, 99, -1, -79, -116, 25, 6, 73, 64, 34, 108, -33, 56, 107, -73, -60,
            17, 91, 104, -115, 67, -94, 3, -92
    };

    public static final int[] GENESIS_AMOUNTS = {
            50000000,
            //
            50000000,
            100000000
    };

    public static final long[] GENESIS_RECIPIENTS = {
            Long.parseUnsignedLong("6219247923802955552"),
            //
            Long.parseUnsignedLong("3790328149872734783"),
            Long.parseUnsignedLong("9011521658538046719")
    };

    public static final byte[][] GENESIS_RECIPIENTS_PK = {
            {-71, -79, -127, -96, -9, -33, -46, -124, 122, 42, -79, 20, -45, -4, 72, 83, -69, 78, 65, 64, 31, 71, 33, -22, -80, 68, -12, -10, -115, 22, -40, 118},
            //
            {30, 5, 14, 84, -15, -59, 81, 105, 88, -43, 119, 56, 10, 34, 62, -93, 23, -122, 95, -14, 21, 35, 8, 58, -62, 6, -114, 52, -72, 35, 120, 17},
            {45, -47, 43, 69, 124, 115, -15, -34, -45, -65, 5, 101, 3, 76, 24, 67, -20, -128, 72, -93, -39, -106, 78, -22, 41, -34, 85, -118, -16, 50, 8, 89}
    };


    public static final byte[][] GENESIS_RECIPIENTS_SIGNATURES = {
            {32, 95, 92, 36, 9, 53, 79, 86, -66, 35, -1, -62, 10, -13, 105, -9, -76, -113, 69, 93, -22, 108, 14, -7, -82, 22, -30, 11, 66, -72, 12, 87},
            //
            {63, -14, 83, -53, 94, -13, -103, 52, -38, -125, -28, -123, 80, 90, -64, 121, 113, 27, 0, -84, -10, 117, 110, -20, -17, 14, 39, 67, 110, 79, 96, 121},
            {-1, 52, -26, 14, 52, 91, 15, 125, 99, 38, 30, 60, -54, 90, 112, 75, -16, 16, 127, 58, -40, -21, -40, -7, -4, -49, -122, 21, 69, 41, 101, -53}
    };

    private static boolean enableGenesisAccount = false;
    public static final void enableGenesisAccount(){
        if(enableGenesisAccount) {
            return;
        }

        Logger.logDebugMessage("Enable genesis account[size=" + (GENESIS_RECIPIENTS.length + 1) + "]");

        Account.addOrGetAccount(CREATOR_ID).apply(CREATOR_PUBLIC_KEY);

        for(int i = 0 ; i < GENESIS_RECIPIENTS.length; i ++) {
            Account.addOrGetAccount(GENESIS_RECIPIENTS[i]).apply(GENESIS_RECIPIENTS_PK[i]);
        }
        enableGenesisAccount = true;
    }

    public static boolean isGenesisRecipients(long accountId){
        for(int i = 0 ; i < GENESIS_RECIPIENTS.length; i ++) {
            if(GENESIS_RECIPIENTS[i] == accountId){
                return true;
            }
        }
        return false;
    }
    
    public static boolean isGenesisCreator(long accountId){
        return CREATOR_ID == accountId ? true : false;
    }

    private SharderGenesis() {}


    
    /**
     * original coinbase, initial supply of ss
     * @return coinbase txs
     */
    private static List<TransactionImpl> coinbaseTxs(){
        List<TransactionImpl> transactions = Lists.newArrayList();

        // coinbase txs
        try{
            long genesisCreatorId = Account.getId(SharderGenesis.CREATOR_PUBLIC_KEY);
            for (int i = 0; i < SharderGenesis.GENESIS_RECIPIENTS.length; i++) {
                TransactionImpl transaction =
                new TransactionImpl.BuilderImpl(
                        (byte) 1,
                        SharderGenesis.GENESIS_RECIPIENTS_PK[i],
                        SharderGenesis.GENESIS_AMOUNTS[i] * Constants.ONE_SS,
                        0,
                        (short) 0,
                        new Attachment.CoinBase(
                            Attachment.CoinBase.CoinBaseType.GENESIS, genesisCreatorId, SharderGenesis.GENESIS_RECIPIENTS[i], Maps.newHashMap()))
                    .timestamp(0)
                    .recipientId(SharderGenesis.GENESIS_RECIPIENTS[i])
                    .signature(SharderGenesis.GENESIS_RECIPIENTS_SIGNATURES[i])
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
     * genesis transactions: 
     * 1. coinbase tx for the genesis account
     * 2. default poc weight table tx
     * @return
     */
    public static List<TransactionImpl> genesisTransactions() throws ConchException.NotValidException {
        List<TransactionImpl> transactions = Lists.newArrayList();
        transactions.addAll(coinbaseTxs());
        transactions.add(defaultPocWeightTableTx());
        return transactions;
    }

    /**
     * genesis block
     * @return
     */
    public static BlockImpl genesisBlock() throws ConchException.NotValidException {
        List<TransactionImpl> transactions = genesisTransactions();

        Collections.sort(transactions, Comparator.comparingLong(Transaction::getId));
        MessageDigest digest = Crypto.sha256();
        for (TransactionImpl transaction : transactions) {
            digest.update(transaction.bytes());
        }

        BlockImpl genesisBlock =
                new BlockImpl(
                        SharderGenesis.GENESIS_BLOCK_ID,
                        -1,
                        0,
                        0,
                        Constants.MAX_BALANCE_NQT,
                        0,
                        transactions.size() * 128,
                        digest.digest(),
                        SharderGenesis.CREATOR_PUBLIC_KEY,
                        new byte[64],
                        SharderGenesis.GENESIS_BLOCK_SIGNATURE,
                        null,
                        transactions);
        genesisBlock.setPrevious(null);
        
        return genesisBlock;
    }
}

