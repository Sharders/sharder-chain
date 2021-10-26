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
    static Console console = System.console();
    
    protected static String getSpFromConsole(){
        return _getFromConsole("Secret Phrase");
    }

    protected static String _getFromConsole(String displayName){
        String secretPhrase = "";
        System.out.println("Input the " + displayName  +" >>");
     
        if (console == null) {
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    secretPhrase = reader.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            secretPhrase = new String(console.readPassword(displayName + ": "));
        }
        return secretPhrase;
    }

    protected static void accountInfoPrint(String secretPhrase){
        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        long accountId = Account.getId(secretPhrase);
        System.out.println("account summary >>");
        System.out.println("Id=" + accountId);
        System.out.println("Addr=" + Account.rsAccount(accountId));
        System.out.println("publicKey[byte]=" + Arrays.toString(publicKey));
        System.out.println("publicKey[hex]=" + Convert.toHexString(publicKey));

    }
}
