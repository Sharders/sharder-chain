package org.conch.consensus;

import com.google.common.collect.Lists;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.tx.Attachment;
import org.conch.tx.TransactionImpl;
import org.conch.util.Logger;

import java.util.List;

/**
 * 海螺链的创世节点,按照如下方式分配初始资金=> 以下为测试数据.
 * @author  xy@ichaoj.com
 * @version 16/7/1
 */
public class ConchGenesis {

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
            50000000
    };

    public static final long[] GENESIS_RECIPIENTS = {
            Long.parseUnsignedLong("6219247923802955552"),
            Long.parseUnsignedLong("3790328149872734783")
    };

    public static final byte[][] GENESIS_RECIPIENTS_PK = {
            {-71, -79, -127, -96, -9, -33, -46, -124, 122, 42, -79, 20, -45, -4, 72, 83, -69, 78, 65, 64, 31, 71, 33, -22, -80, 68, -12, -10, -115, 22, -40, 118},
            {30, 5, 14, 84, -15, -59, 81, 105, 88, -43, 119, 56, 10, 34, 62, -93, 23, -122, 95, -14, 21, 35, 8, 58, -62, 6, -114, 52, -72, 35, 120, 17}
    };


    public static final byte[][] GENESIS_RECIPIENTS_SIGNATURES = {
            {32, 95, 92, 36, 9, 53, 79, 86, -66, 35, -1, -62, 10, -13, 105, -9, -76, -113, 69, 93, -22, 108, 14, -7, -82, 22, -30, 11, 66, -72, 12, 87},
            {63, -14, 83, -53, 94, -13, -103, 52, -38, -125, -28, -123, 80, 90, -64, 121, 113, 27, 0, -84, -10, 117, 110, -20, -17, 14, 39, 67, 110, 79, 96, 121}
    };

    private static boolean enableGenesisAccount = false;
    public static final void enableGenesisAccount(){
        if(enableGenesisAccount) return;

        Logger.logDebugMessage("Enable genesis account[size=" + (GENESIS_RECIPIENTS.length + 1) + "]");

        Account.addOrGetAccount(CREATOR_ID).apply(CREATOR_PUBLIC_KEY);

        for(int i = 0 ; i < GENESIS_RECIPIENTS.length; i ++) {
            Account.addOrGetAccount(GENESIS_RECIPIENTS[i]).apply(GENESIS_RECIPIENTS_PK[i]);
        }

        enableGenesisAccount = true;
    }

    private ConchGenesis() {}

    /**
     * original coinbase, initial supply of ss
     * @return coinbase txs
     */
    public static List<TransactionImpl> coinbase(){
        List<TransactionImpl> transactions = Lists.newArrayList();
        //TODO 
        return transactions;
    }

    /**
     * default poc weight table
     * @return
     * @throws ConchException.NotValidException
     */
    public static TransactionImpl defaultPocWeightTableTx() throws ConchException.NotValidException {
        Attachment.AbstractAttachment attachment = PocTxBody.PocWeightTable.defaultPocWeightTable();
        return new TransactionImpl.BuilderImpl(
                (byte) 0,
                ConchGenesis.CREATOR_PUBLIC_KEY,
                0,
                0,
                (short) 0,
                attachment)
                .timestamp(0)
                .signature(ConchGenesis.CREATOR_SIGNATURES)
                .height(0)
                .ecBlockHeight(0)
                .ecBlockId(0)
                .build();
    }
}

