package org.conch.base;

import org.conch.account.Account;
import org.conch.crypto.Crypto;
import org.conch.util.Convert;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-28
 */
public class BaseTest {
    
    protected static String getSpFromConsole(){
        String secretPhrase = "";
        System.out.println("Input the secret phrase >>");
        Console console = System.console();
        if (console == null) {
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    secretPhrase = reader.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            secretPhrase = new String(console.readPassword("Secret phrase: "));
        }
        
        return secretPhrase;
    }

    protected static void accountInfoPrint(String secretPhrase){
        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        long accountId = Account.getId(secretPhrase);
        System.out.println("account summary >>");
        System.out.println("accountId=" + accountId);
        System.out.println("rsAccount=" + Account.rsAccount(accountId));
        System.out.println("publicKey[byte]=" + Arrays.toString(publicKey));
        System.out.println("publicKey[hex]=" + Convert.toHexString(publicKey));

    }
}
