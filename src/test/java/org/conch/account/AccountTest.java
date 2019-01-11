package org.conch.account;

import com.google.common.collect.Lists;
import org.conch.chain.BlockImpl;
import org.conch.common.ConchException;
import org.conch.consensus.SharderGenesis;
import org.conch.crypto.Crypto;
import org.conch.tx.TransactionImpl;
import org.conch.util.Convert;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/10
 */
public class AccountTest {
    static BlockImpl previousBlock = null;
    static List<TransactionImpl> transactions;
    
    static {
        try {
            transactions = Lists.newArrayList(SharderGenesis.defaultPocWeightTableTx());
        } catch (ConchException.NotValidException e) {
            e.printStackTrace();
        }
    }
    
    
    static void accountInfo(String secretPhrase){
        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        MessageDigest digest = Crypto.sha256();

        for (TransactionImpl transaction : transactions) {
            digest.update(transaction.bytes());
        }
      
        
        byte[] payloadHash = digest.digest();
        if(previousBlock != null){
            byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.bytes());
            digest.update(previousBlock.getGenerationSignature());
        }
       
        byte[] generationSignature = digest.digest(publicKey);

        System.out.println("account summary >>");
        System.out.println("publicKey[byte]=" + Arrays.toString(publicKey));
        System.out.println("publicKey[hex]=" + Convert.toHexString(publicKey));
        System.out.println("generationSignature[byte]=" + Arrays.toString(generationSignature));
        System.out.println("generationSignature[hex]=" + Convert.toHexString(generationSignature));
        System.out.println("payloadHash=" + payloadHash);
    }

    public static void main(String[] args) throws IOException {
        String secretPhrase = "";
        
        System.out.println("Input the secret phrase >>");
        Console console = System.console();
        if (console == null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                secretPhrase = reader.readLine();
            }
        } else {
            secretPhrase = new String(console.readPassword("Secret phrase: "));
        }
        
        accountInfo(secretPhrase);
    }
}
