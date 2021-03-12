package org.conch.consensus.genesis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.BlockImpl;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.crypto.Crypto;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.util.Logger;

import java.security.MessageDigest;
import java.util.*;

import static org.conch.util.JSON.readJsonFile;

/**
 * Sharder Genesis 
 * @author  xy@sharder.org 
 * @date 01/19/2019
 */
public class SharderGenesis {
    public static final long GENESIS_BLOCK_ID = 6840612405442242020L;
    public static final long CREATOR_ID = 1492434941236553746L;
    public static final long KEEPER_ID = 1868021154578573726L;
    public static final byte[] CREATOR_PUBLIC_KEY = {
            -31, -17, -44, -121, 32, -95, -97, 40, -38, 117, -114, 80, -94, 25, -96,
            -102, 1, 109, 125, -99, 125, -16, 56, 109, 4, 48, -46, -41, 12, -81, 111, 10
    };
    public static final byte[] CREATOR_SIGNATURES = {
            18, 40, -55, -6, -85, 49, -74, 20, -81, -20, -16, -120, -97, -92, -119,
            -77, -71, 0, 105, 12, -69, -72, -109, 45, -3, 55, -22, -6, 67, 52, -91, -3
    };

    public static final byte[] GENESIS_BLOCK_SIGNATURE = new byte[]{
            24, -119, -8, -17, -14, 105, -64, -7, -17, -31, 94, -111, 30, 18, -84, 15, -96, 84, 1, -35, 77, -44, 89,
            -36, 77, 76, 118, 61, -82, -112, 19, -34
    };
    public static final byte[] GENESIS_PAYLOAD_HASH = new byte[]{
            -80, -59, -99, 2, -10, -128, -43, 20, -70, 55, -55, 63, 15, 33, -26, -50, -90, 77, 104, 109, 6, 57, -99,
            -3, 111, 84, -5, -78, -10, -5, -84, -11
    };

    private static boolean enableGenesisAccount = false;
    public static final void enableGenesisAccount(){
        if(enableGenesisAccount) {
            return;
        }

        Logger.logDebugMessage("Enable genesis account[size=" + (GenesisRecipient.getAll().size() + 1) + "]");

        Account.addOrGetAccount(CREATOR_ID).apply(CREATOR_PUBLIC_KEY);
        
        for(GenesisRecipient genesisRecipient : GenesisRecipient.getAll()){
            Account.addOrGetAccount(genesisRecipient.id).apply(genesisRecipient.publicKey);
        }
        enableGenesisAccount = true;
    }

    protected static final JSONObject genesisJsonObj = loadGenesisSettings();
    private static JSONObject loadGenesisSettings() {
        String pathName = Conch.getStringProperty("sharder.genesis.pathName", "conf/genesis.json");
        String jsonStr = readJsonFile(pathName);
        return JSON.parseObject(jsonStr);
    }

    public static class GenesisPeer {
        public String domain;
        public Peer.Type type;
        public byte typeCode = -1;
        public long accountId;
        public long diskCapacity;

        private GenesisPeer(String domain,Peer.Type type, long accountId){
            this.domain = domain;
            this.type = type;
            this.accountId = accountId;
        }

        private GenesisPeer(String domain, Peer.Type type, long accountId, long diskCapacity){
            this.domain = domain;
            this.type = type;
            this.accountId = accountId;
            this.diskCapacity = diskCapacity;
        }

        public GenesisPeer(){}

        private void mappingType(){
            if(this.typeCode == -1) {
                return;
            }
            this.type = Peer.Type.getByCode(this.typeCode);
        }

        static final Map<Constants.Network, List<GenesisPeer>> genesisPeers = loadGenesisPeers();
        static Map<Constants.Network, List<GenesisPeer>> loadGenesisPeers() {
            Map<Constants.Network, List<GenesisPeer>> genesisPeers = Maps.newHashMap();
            JSONArray jsonArrayDev = SharderGenesis.genesisJsonObj.getJSONArray("devnetPeers");
            List<GenesisPeer> devnetPeers = JSONObject.parseArray(jsonArrayDev.toJSONString(), GenesisPeer.class);
            JSONArray jsonArrayTest = SharderGenesis.genesisJsonObj.getJSONArray("testnetPeers");
            List<GenesisPeer> testnetPeers = JSONObject.parseArray(jsonArrayTest.toJSONString(), GenesisPeer.class);
            JSONArray jsonArrayMain = SharderGenesis.genesisJsonObj.getJSONArray("mainnetPeers");
            List<GenesisPeer> mainnetPeers = JSONObject.parseArray(jsonArrayMain.toJSONString(), GenesisPeer.class);
            devnetPeers.forEach(peer -> peer.mappingType());
            testnetPeers.forEach(peer -> peer.mappingType());
            mainnetPeers.forEach(peer -> peer.mappingType());
            genesisPeers.put(Constants.Network.DEVNET, devnetPeers);
            genesisPeers.put(Constants.Network.TESTNET, testnetPeers);
            genesisPeers.put(Constants.Network.MAINNET, mainnetPeers);

            return genesisPeers;
        }

        public static List<GenesisPeer> getAll(){
            return genesisPeers.get(Constants.getNetwork());
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static boolean isGenesisPeerAccount(long accountId){
        for(GenesisPeer genesisPeer : GenesisPeer.getAll()){
            if(genesisPeer.accountId == accountId) return true;
        }
        return false;
    }

    public static boolean isGenesisRecipients(long accountId){
        GenesisRecipient recipient = GenesisRecipient.getByAccountId(accountId);
        return recipient == null ? false : true;
    }
    
    public static boolean isGenesisCreator(long accountId){
        return CREATOR_ID == accountId ? true : false;
    }

    private static long genesisBlockAmount(){
        long total = 0;
        for(GenesisRecipient genesisRecipient : GenesisRecipient.getAll()){
            total += genesisRecipient.amount * Constants.ONE_SS;
        }
        return total;
    }
    
    private SharderGenesis() {}

    /**
     * all genesis txs
     * @return
     * @throws ConchException.NotValidException
     */
    public static List<TransactionImpl> genesisTxs() throws ConchException.NotValidException {
        List<TransactionImpl> transactions = coinbaseTxs();
        transactions.add(defaultPocWeightTableTx());
        transactions.addAll(nodeTypeTxs());
        Collections.sort(transactions, Comparator.comparingLong(Transaction::getId));
        return transactions;
    }

    /**
     * genesis block:
     * @param fixedPayloadHash
     * @return
     * @throws ConchException.NotValidException
     */
    private static BlockImpl genesisBlock(boolean fixedPayloadHash) throws ConchException.NotValidException {
        byte[] payloadHash = SharderGenesis.GENESIS_PAYLOAD_HASH;
        List<TransactionImpl> transactions = genesisTxs();
        if(!fixedPayloadHash) {
            MessageDigest digest = Crypto.sha256();
            for (TransactionImpl transaction : transactions) {
                digest.update(transaction.bytes());
            }
            payloadHash = digest.digest();
        }
        
        int blockVersion = -1;
        BlockImpl genesisBlock = BlockImpl.newGenesisBlock(
                        SharderGenesis.GENESIS_BLOCK_ID,
                        blockVersion,
                        0,
                        0,
                        genesisBlockAmount(),
                        0,
                        transactions.size() * 128,
                        payloadHash,
                        SharderGenesis.CREATOR_PUBLIC_KEY,
                        new byte[64],
                        SharderGenesis.GENESIS_BLOCK_SIGNATURE,
                        null,
                        transactions);
        genesisBlock.calAndSetByPreviousBlock(null);

        return genesisBlock;
    }
    
    /**
     * original coinbase, initial ss supply
     * @return coinbase txs
     */
    private static List<TransactionImpl> coinbaseTxs(){
        List<TransactionImpl> transactions = Lists.newArrayList();

        // coinbase txs
        long genesisCreatorId = Account.getId(SharderGenesis.CREATOR_PUBLIC_KEY);
        GenesisRecipient.getAll().forEach(recipient -> {
            try {
                transactions.add(new TransactionImpl.BuilderImpl(
                        recipient.publicKey,
                        recipient.amount * Constants.ONE_SS,
                        0,
                        (short) 0,
                        new Attachment.CoinBase(Attachment.CoinBase.CoinBaseType.GENESIS, genesisCreatorId, recipient.id, Maps.newHashMap()))
                        .timestamp(0)
                        .recipientId(recipient.id)
                        .signature(recipient.signature)
                        .height(0)
                        .ecBlockHeight(0)
                        .ecBlockId(0)
                        .build());
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }
        });
            
        return transactions;
    }

    
    /**
     * default node type tx for known peers
     * @return node-type txs
     */
    public static List<TransactionImpl> nodeTypeTxs() {
        List<TransactionImpl> transactions = Lists.newArrayList();

        GenesisPeer.getAll().forEach(genesisPeer -> {
//            Attachment.AbstractAttachment attachment = new PocTxBody.PocNodeTypeV2(genesisPeer.domain,genesisPeer.type,genesisPeer.accountId);
            Attachment.AbstractAttachment attachment = new PocTxBody.PocNodeTypeV3(genesisPeer.domain,genesisPeer.type,genesisPeer.accountId,genesisPeer.diskCapacity);
            try {
                transactions.add(new TransactionImpl.BuilderImpl(
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
                        .build());
            } catch (ConchException.NotValidException e) {
                e.printStackTrace();
            }
        });
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
     * genesis block that include genesis transactions:
     * 1. coinbase tx for the genesis account
     * 2. default poc weight table tx
     * @return genesis block
     */
    public static BlockImpl genesisBlock() throws ConchException.NotValidException {
        return genesisBlock(true);
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(GenesisPeer.genesisPeers.get(Constants.Network.MAINNET).toArray()));
        System.out.println(Arrays.toString(GenesisPeer.genesisPeers.get(Constants.Network.DEVNET).toArray()));
        System.out.println(Arrays.toString(GenesisPeer.genesisPeers.get(Constants.Network.TESTNET).toArray()));
    }
    
}

