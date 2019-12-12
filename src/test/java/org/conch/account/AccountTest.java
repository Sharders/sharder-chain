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

import java.io.IOException;
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
        getAccountInfo(Account.rsAccountToId(bindRs));
    }
    
    public static Account getAccountInfo(long accountId) {
        Account account = Account.getAccount(accountId);
        System.out.println("account info is :" + JSONObject.toJSONString(account));
        System.out.println("public key is:" + Convert.toString(Account.getPublicKey(accountId), false));
        System.out.println("rs address:" + Account.rsAccount(accountId));
        return account;
    }

    public static Account getAccountInfoBySecretPhrase(String secretPhrase) {
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        Account account = Account.getAccount(publicKey);
        System.out.println("account info is :" + JSONObject.toJSONString(account));
        System.out.println("public key is:" + Convert.toString(Account.getPublicKey(account.getId()), false));
        System.out.println("rs address:" + Account.rsAccount(account.getId()));
        return account;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("rs address:" + Account.rsAccount(-90778548339644322L));

//        String secretPhrase = getSpFromConsole();
//        accountInfoPrint(secretPhrase);
//        accountSignInfo(secretPhrase);
//        getAccountInfoViaBindRS("CDW-EF9Z-8J9G-LLHC-9VU5U");
         Object[] arr = {};
         int[] ia1 = {1,2,3};
         int[] ia2 = {1,2};

         int r= 0;
         for (int i:ia1){
             boolean f = false;
             try {
                 for (int j:ia2){
                     if (j==i){
                         f=true;
                         break;
                     }
                 }
             }catch (Exception e){
                 System.out.println("catch:"+e);
             }finally {
                 if (f){
                     continue;
                 }
                 r++;
                 System.out.println("r:"+r);
             }

             System.out.println("rl:"+r);
         }


    }
}
