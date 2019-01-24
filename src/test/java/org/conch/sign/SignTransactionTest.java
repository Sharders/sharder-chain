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

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.BlockImpl;
import org.conch.common.ConchException;
import org.conch.consensus.SharderGenesis;
import org.conch.crypto.Crypto;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public final class SignTransactionTest {
    
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
    
    public static void signTranscationFile(String[] args){
        try {
            Logger.setLevel(Logger.Level.ERROR);
            if (args.length == 0 || args.length > 2) {
                System.out.println("Usage: SignTransactionJSON <unsigned transaction json file> <signed transaction json file>");
                System.exit(1);
            }
            File unsigned = new File(args[0]);
            if (!unsigned.exists()) {
                System.out.println("File not found: " + unsigned.getAbsolutePath());
                System.exit(1);
            }
            File signed;
            if (args.length == 2) {
                signed = new File(args[1]);
            } else if (unsigned.getName().startsWith("unsigned.")) {
                signed = new File(unsigned.getParentFile(), unsigned.getName().substring(2));
            } else {
                signed = new File(unsigned.getParentFile(), "signed." + unsigned.getName());
            }
            if (signed.exists()) {
                System.out.println("File already exists, delete it: " + signed.getAbsolutePath());
                signed.delete();
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(unsigned));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(signed))) {
                JSONObject json = (JSONObject) JSONValue.parseWithException(reader);
                byte[] publicKeyHash = Crypto.sha256().digest(Convert.parseHexString((String) json.get("senderPublicKey")));
                String senderRS = Account.rsAccount(Convert.fullHashToId(publicKeyHash));
                String secretPhrase;
                Console console = System.console();
                if (console == null) {
                    try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in))) {
                        secretPhrase = inputReader.readLine();
                    }
                } else {
                    secretPhrase = new String(console.readPassword("Secret phrase for account " + senderRS + ": "));
                }
                Transaction.Builder builder = Conch.newTransactionBuilder(json);
                Transaction transaction = builder.build(secretPhrase);
                writer.write(transaction.getJSONObject().toJSONString());
                writer.newLine();
                System.out.println("Signed transaction JSON saved as: " + signed.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws ConchException.NotValidException {
    //        System.out.println("genesis1=>");
    //        blockHash(SharderGenesis.genesisBlock());

        signTranscationImpls();
        signTranscationImpls();
        signTranscationImpls();
        
        SharderGenesis.genesisBlock();
        SharderGenesis.genesisBlock();
        
        byte[] digestBytes = new byte[]{9, 16, 0, 0, 0, 0, 0, 0, 45, -47, 43, 69, 124, 115, -15, -34, -45, -65, 5, 101, 3, 76, 24, 67, -20, -128, 72, -93, -39, -106, 78, -22, 41, -34, 85, -118, -16, 50, 8, 89, -89, 89, 114, -48, 3, -95, -69, 106, 0, 0, -63, 111, -14, -122, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 52, -26, 14, 52, 91, 15, 125, 99, 38, 30, 60, -54, 90, 112, 75, -16, 16, 127, 58, -40, -21, -40, -7, -4, -49, -122, 21, 69, 41, 101, -53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 7, 71, 69, 78, 69, 83, 73, 83, -89, 89, 114, -48, 3, -95, -69, 106, -1, 52, -26, 14, 52, 91, 15, 125, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 2, 0, 0, 0, 0, 0, 0, -36, 27, -52, -114, -28, 115, -4, -120, 50, -66, -107, 70, -54, -95, 61, -14, 79, 123, -18, -57, -99, 10, -34, 75, -48, -72, -25, 96, -53, -63, -1, 43, -89, 89, 114, -48, 3, -95, -69, 106, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -36, 27, -52, -114, -28, 115, -4, -120, 50, -66, -107, 70, -54, -95, 61, -14, 79, 123, -18, -57, -99, 10, -34, 75, -48, -72, -25, 96, -53, -63, -1, 43, -84, 19, 52, 1, 0, 0, 0, 0, 94, 0, 0, 0, 123, 34, 110, 111, 100, 101, 34, 58, 50, 53, 44, 34, 115, 101, 114, 118, 101, 114, 79, 112, 101, 110, 34, 58, 50, 48, 44, 34, 104, 97, 114, 100, 119, 97, 114, 101, 67, 111, 110, 102, 105, 103, 34, 58, 53, 44, 34, 110, 101, 116, 119, 111, 114, 107, 67, 111, 110, 102, 105, 103, 34, 58, 53, 44, 34, 115, 115, 72, 111, 108, 100, 34, 58, 52, 48, 44, 34, 116, 120, 80, 101, 114, 102, 111, 114, 109, 97, 110, 99, 101, 34, 58, 53, 125, 22, 0, 0, 0, 123, 49, 58, 49, 48, 44, 50, 58, 56, 44, 51, 58, 51, 44, 52, 58, 54, 44, 53, 58, 54, 125, 29, 0, 0, 0, 123, 49, 50, 56, 58, 52, 44, 51, 50, 58, 52, 44, 50, 53, 54, 58, 52, 44, 54, 52, 58, 52, 44, 53, 49, 50, 58, 52, 125, 14, 0, 0, 0, 123, 49, 58, 51, 44, 50, 58, 54, 44, 51, 58, 49, 48, 125, 18, 0, 0, 0, 123, 48, 58, 48, 44, 49, 58, 51, 44, 50, 58, 54, 44, 51, 58, 49, 48, 125, 14, 0, 0, 0, 123, 49, 58, 51, 44, 50, 58, 54, 44, 51, 58, 49, 48, 125, 123, 0, 0, 0, 123, 34, 72, 85, 66, 34, 58, 123, 52, 58, 45, 53, 44, 53, 58, 53, 44, 54, 58, 51, 125, 44, 34, 70, 79, 85, 78, 68, 65, 84, 73, 79, 78, 34, 58, 123, 48, 58, 45, 50, 44, 49, 58, 45, 53, 44, 51, 58, 45, 49, 48, 125, 44, 34, 67, 79, 77, 77, 85, 78, 73, 84, 89, 34, 58, 123, 49, 58, 45, 50, 44, 50, 58, 45, 53, 44, 52, 58, 45, 49, 48, 125, 44, 34, 66, 79, 88, 34, 58, 123, 52, 58, 45, 53, 44, 53, 58, 53, 44, 54, 58, 51, 125, 44, 34, 78, 79, 82, 77, 65, 76, 34, 58, 123, 54, 58, 53, 44, 55, 58, 51, 125, 125, 17, 0, 0, 0, 123, 49, 58, 45, 49, 48, 44, 50, 58, 45, 54, 44, 51, 58, 45, 51, 125, 17, 0, 0, 0, 123, 48, 58, 45, 49, 48, 44, 49, 58, 45, 51, 44, 50, 58, 45, 54, 125, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 16, 0, 0, 0, 0, 0, 0, 30, 5, 14, 84, -15, -59, 81, 105, 88, -43, 119, 56, 10, 34, 62, -93, 23, -122, 95, -14, 21, 35, 8, 58, -62, 6, -114, 52, -72, 35, 120, 17, -89, 89, 114, -48, 3, -95, -69, 106, 0, -128, -32, 55, 121, -61, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 63, -14, 83, -53, 94, -13, -103, 52, -38, -125, -28, -123, 80, 90, -64, 121, 113, 27, 0, -84, -10, 117, 110, -20, -17, 14, 39, 67, 110, 79, 96, 121, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 7, 71, 69, 78, 69, 83, 73, 83, -89, 89, 114, -48, 3, -95, -69, 106, 63, -14, 83, -53, 94, -13, -103, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 16, 0, 0, 0, 0, 0, 0, -71, -79, -127, -96, -9, -33, -46, -124, 122, 42, -79, 20, -45, -4, 72, 83, -69, 78, 65, 64, 31, 71, 33, -22, -80, 68, -12, -10, -115, 22, -40, 118, -89, 89, 114, -48, 3, -95, -69, 106, 0, -128, -32, 55, 121, -61, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 95, 92, 36, 9, 53, 79, 86, -66, 35, -1, -62, 10, -13, 105, -9, -76, -113, 69, 93, -22, 108, 14, -7, -82, 22, -30, 11, 66, -72, 12, 87, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 7, 71, 69, 78, 69, 83, 73, 83, -89, 89, 114, -48, 3, -95, -69, 106, 32, 95, 92, 36, 9, 53, 79, 86, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        System.out.println(Arrays.toString(Crypto.sha256().digest(digestBytes)));
        System.out.println(Arrays.toString(Crypto.sha256().digest(digestBytes)));
        System.out.println(Arrays.toString(Crypto.sha256().digest(digestBytes)));
    }

}
