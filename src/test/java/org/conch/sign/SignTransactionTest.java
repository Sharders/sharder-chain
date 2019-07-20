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

package org.conch.sign;

import com.google.common.collect.Maps;
import org.conch.account.Account;
import org.conch.base.BaseTest;
import org.conch.chain.BlockImpl;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.genesis.GenesisRecipient;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.crypto.Crypto;
import org.conch.tools.SignTransactionJSON;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.Convert;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public final class SignTransactionTest extends BaseTest {
    
    public static void calHash(){
        byte[] array = new byte[]{};
        for(int i = 0 ; i < 10 ; i++) {
            System.out.println(i + "=> " + Arrays.toString(Crypto.sha256().digest(array)));
        }
    } 
    
    
    public static void blockHash(BlockImpl block) throws ConchException.NotValidException {
        System.out.println("block id=" + block.getId());
        System.out.println("blockHash: " + Arrays.toString(Crypto.sha256().digest(block.bytes())));
        System.out.println("block bytes: " + Arrays.toString(block.bytes()));
        System.out.println("block payload hash: " + Arrays.toString(block.getPayloadHash()));
    }
   
    public static void signTranscations(List<Transaction> transactions){
        MessageDigest digest = Crypto.sha256();
        digest.reset();
        for(Transaction transaction : transactions){
            digest.update(transaction.getBytes());
        }
        byte[] checksum = digest.digest();
        System.out.println("checksum is " + Arrays.toString(checksum));
    }

    public static void signTranscationImpls() throws ConchException.NotValidException {
        List<TransactionImpl> transactions = SharderGenesis.genesisTxs();
        MessageDigest digest = Crypto.sha256();
        for (TransactionImpl transaction : transactions) {
            System.out.println("tx bytes: " + Arrays.toString(transaction.bytes()));
            digest.update(transaction.bytes());
        }
        byte[] checksum = digest.digest();
        System.out.println("checksum is " + Arrays.toString(checksum));
    }
    
    public static void signCoinbaseTx(String secretPhrase) throws ConchException.NotValidException {
//        Db.init();
        long accountId = Account.getId(secretPhrase);
        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        accountInfoPrint(secretPhrase);
        long genesisCreatorId = Account.getId(SharderGenesis.CREATOR_PUBLIC_KEY);
            TransactionImpl transaction =
                    new TransactionImpl.BuilderImpl(
                            (byte) 1,
                            publicKey,
                            GenesisRecipient.getByAccountId(accountId).amount * Constants.ONE_SS,
                            0,
                            (short) 0,
                            new Attachment.CoinBase(Attachment.CoinBase.CoinBaseType.GENESIS, genesisCreatorId, accountId, Maps.newHashMap()))
                            .timestamp(0)
                            .recipientId(accountId)
                            .height(0)
                            .ecBlockHeight(0)
                            .ecBlockId(0)
                            .build(secretPhrase);
        System.out.println(transaction);
    }
    
    public static void signPocWeightTx(String secretPhrase) throws ConchException.NotValidException {
//        Db.init();
        accountInfoPrint(secretPhrase);
        Attachment.AbstractAttachment attachment = PocTxBody.PocWeightTable.defaultPocWeightTable();
        TransactionImpl  transaction = new TransactionImpl.BuilderImpl(
                (byte) 0,
                SharderGenesis.CREATOR_PUBLIC_KEY,
                0,
                0,
                (short) 0,
                attachment)
                .timestamp(0)
                .height(0)
                .ecBlockHeight(0)
                .ecBlockId(0)
                .build(secretPhrase);
        System.out.println(transaction);
    }
    
    
    public static void newTx() throws ConchException.NotValidException {
        byte[] bytes = Convert.toBytes("0c30c0ce7f050a00561112d186285166f709240fc8521f51468aad3271123f5d4c09a91a8e2dc71fa75972d003a1bb6a000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000f35b7484f8e4b23adbac5ee28694d2f2eac2e4daf23344f979696e9e0d2af30751413997146fdd7ed1034732ddaa000bd1d0c55b18ebbf54d7842d205c0bce160000000006290000ff43bc54bb4b671901040000002e004144324631414230354645413236394437443430303231333342423545413736422e61737573636f6d6d2e636f6d6a5395f0b0c5ded8");
        TransactionImpl.newTransactionBuilder(bytes);
    }
    
    public static void signTranscationFile(String[] args){
        SignTransactionJSON.signTranscationFile(args);
    }

    /**
     * - blockHash method used to generate the genesis block hash
     * - signCoinbaseTx method used to generate the account info and tx sign for GenesisRecipient
     * @param args
     * @throws ConchException.NotValidException
     */
    public static void main(String[] args) throws ConchException.NotValidException {
        newTx();
    //        System.out.println("genesis1=>");
//            blockHash(SharderGenesis.genesisBlock());
//        signPocWeightTx(getSpFromConsole());

//        signCoinbaseTx(getSpFromConsole());
//        exit(0);
    }

}
