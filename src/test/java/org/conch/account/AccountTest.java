package org.conch.account;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.conch.base.BaseTest;
import org.conch.chain.BlockImpl;
import org.conch.common.ConchException;
import org.conch.consensus.genesis.SharderGenesis;
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

import static java.lang.System.exit;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/10
 */
public class AccountTest extends BaseTest {
    static BlockImpl previousBlock = null;
    static List<TransactionImpl> transactions;
    
    
    static void accountSignInfo(String secretPhrase){
        // init genesis txs
        if(transactions == null){
            try {
                transactions = Lists.newArrayList(SharderGenesis.genesisBlock().getTransactions());
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }
        }
        
        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        MessageDigest digest = Crypto.sha256();

        for (TransactionImpl transaction : transactions) {
            digest.update(transaction.bytes());
        }
      
        byte[] payloadHash = digest.digest();
        if(previousBlock != null){
            digest.update(previousBlock.getGenerationSignature());
        }
       
        byte[] generationSignature = digest.digest(publicKey);
        System.out.println("accountsign summary >>");
        System.out.println("generationSignature[byte]=" + Arrays.toString(generationSignature));
        System.out.println("generationSignature[hex]=" + Convert.toHexString(generationSignature));
        System.out.println("payloadHash=" + payloadHash);
        exit(0);
    }

    public static void getAccountInfoViaBindRS(String bindRs) {
        long accountId = Account.rsAccountToId(bindRs);
        Account account = Account.getAccount(accountId);
        System.out.println("account info is :" + JSONObject.toJSONString(account));
        System.out.println("public key is:" + Convert.toString(Account.getPublicKey(accountId), false));
    }

    public static void main(String[] args) throws IOException {
        String secretPhrase = getSpFromConsole();
        accountInfoPrint(secretPhrase);
//        accountSignInfo(secretPhrase);
        getAccountInfoViaBindRS("SSA-EF9Z-8J9G-LLHC-9VU5U");
    }
}
