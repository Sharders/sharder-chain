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

package org.conch.tools;

import org.conch.Conch;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.crypto.Crypto;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.util.Convert;

import java.io.*;

public final class SignTransactions {

    public static void main(String[] args) {
        _signTxAndSaveItToBytesFile(args);
    }
    
    static void _signTxBytesFile(String[] args){
        try {
            if (args.length != 2) {
                System.out.println("Usage: SignTransactions <unsigned transaction bytes file> <signed transaction bytes file>");
                System.exit(1);
            }
            File unsigned = new File(args[0]);
            if (!unsigned.exists()) {
                System.out.println("File not found: " + unsigned.getAbsolutePath());
                System.exit(1);
            }
            File signed = new File(args[1]);
            if (signed.exists()) {
                System.out.println("File already exists, delete it: " + signed.getAbsolutePath());
                signed.delete();
            }
            String secretPhrase;
            System.out.println("Input the secret phrase >>");
            Console console = System.console();
            if (console == null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    secretPhrase = reader.readLine();
                }
            } else {
                secretPhrase = new String(console.readPassword("Secret phrase: "));
            }
            int n = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(unsigned));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(signed))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    byte[] transactionBytes = Convert.parseHexString(line);
                    Transaction.Builder builder = Conch.newTransactionBuilder(transactionBytes);
                    Transaction transaction = builder.build(secretPhrase);
                    writer.write(Convert.toHexString(transaction.getBytes()));
                    writer.newLine();
                    n += 1;
                }
            }
            System.out.println("Signed " + n + " transactions");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static void _signTxAndSaveItToBytesFile(String[] args){
        try {
            File signed = new File(args[0]);
            if (signed.exists()) {
                System.out.println("File already exists, delete it: " + signed.getAbsolutePath());
                signed.delete();
            }
            String secretPhrase;
            System.out.println("Input the secret phrase >>");
            Console console = System.console();
            if (console == null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    secretPhrase = reader.readLine();
                }
            } else {
                secretPhrase = new String(console.readPassword("Secret phrase: "));
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(signed))) {
                Attachment attachment = PocTxBody.PocWeightTable.defaultPocWeightTable();
                
                Transaction.Builder builder = Conch.newTransactionBuilder(Crypto.getPublicKey(secretPhrase), 0, 0,
                      (short) 0, attachment).timestamp(0).ecBlockHeight(0).ecBlockId(0);
                Transaction transaction = builder.build(secretPhrase);
                writer.write(Convert.toHexString(transaction.getBytes()));
                writer.newLine();
            }
            System.out.println("Signed transactions: " + signed.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
