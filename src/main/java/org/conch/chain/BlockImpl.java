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

package org.conch.chain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.poc.PocScore;
import org.conch.crypto.Crypto;
import org.conch.mint.Generator;
import org.conch.tx.TransactionDb;
import org.conch.tx.TransactionImpl;
import org.conch.tx.TransactionType;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.util.SizeUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.*;

public final class BlockImpl implements Block {

    private final int version;
    private final int timestamp;
    private final long previousBlockId;
    private volatile byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final long totalAmountNQT;
    private final long totalFeeNQT;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private volatile List<TransactionImpl> blockTransactions;

    private byte[] blockSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private long baseTarget = Constants.INITIAL_BASE_TARGET;
    private volatile long nextBlockId;
    private int height = -1;
    private volatile long id;
    private volatile String stringId = null;
    private volatile long generatorId;
    private volatile byte[] bytes = null;
    private byte[] extension = null;
    private com.alibaba.fastjson.JSONObject extensionJson = null;
     
    public enum ExtensionEnum {
        CONTAIN_POC("h-poc",Boolean.class),
        CONTAIN_POOL("h-pool",Boolean.class);
        
        private String name;
        private Class clazz;
        
        ExtensionEnum(String name, Class clazz){
          this.name = name;  
          this.clazz = clazz;  
        }
        
        public static Class getClazz(String key) {
            for (ExtensionEnum _enum : values()) {
                if (_enum.name.equals(key)) {
                    return _enum.clazz;
                }
            }
            return null;
        }
    }

    /**
     * allow the 12 extension pair, the max length of the one pair is 20.
     * the pair is under the json format: "key":value, the length of key + the length of value <= 20
     */
    private static final int EXTENSION_MAX_SIZE = 276;
    private static final int EXTENSION_ITEM_MAX_SIZE = 10;
    private static final int EXTENSION_PAIR_MAX_SIZE = EXTENSION_ITEM_MAX_SIZE * 2 + 3;
    private boolean _exceedMaxSize(String key, Object value){
        if(key.length() > EXTENSION_ITEM_MAX_SIZE ) {
            Logger.logWarningMessage("the pair-key[" + key + ",size=" + key.length() + "] is larger than allowed max size=" + EXTENSION_ITEM_MAX_SIZE + "], can't save it to extension area");
            return true;
        }
        
        if(SizeUtil.sizeOf(value) > EXTENSION_ITEM_MAX_SIZE ) {
            Logger.logWarningMessage("the pair-value[" + key + ",size=" + SizeUtil.sizeOf(value) + "] is larger than allowed max size=" + EXTENSION_ITEM_MAX_SIZE + "], can't save it to extension area");
            return true;
        }

        int pairSize = key.length() + SizeUtil.sizeOf(value);
        if(this.extension.length < (EXTENSION_MAX_SIZE - EXTENSION_PAIR_MAX_SIZE)) {
            Logger.logWarningMessage("extension area is full[current size=" +  this.extension.length + "allowed max size=" + EXTENSION_MAX_SIZE + "], the extension[key=" + key + ",value=" + value + ",size=" + pairSize + "] can't be append to tail");
            return true;
        }
        
        return false;
    }

    public String getExtensionStr() {
        return String.valueOf(extension);
    }
    
    public byte[] getExtension() {
        return extension;
    }
    
    private BlockImpl addExtension(String key, Object value){
        if(this.extensionJson == null) this.extensionJson = new com.alibaba.fastjson.JSONObject();
        this.extensionJson.put(key,value);
        this.extension = this.extensionJson.toJSONString().getBytes();
        return this;
    }
    

    public BlockImpl addExtension(ExtensionEnum extensionEnum, Object value){
        return addExtension(extensionEnum.name,value);
    }

    /**
     *
     * @param extensions String use the org.conch.chain.BlockImpl.ExtensionEnum.name
     */
    public BlockImpl addExtensions(Map<String,Object> extensions){
        for (Map.Entry<String, Object> entry : extensions.entrySet()) {
            addExtension(entry.getKey(),entry.getValue());
        }
        return this;
    }

    /**
     *
     * @param extensionEnum use the org.conch.chain.BlockImpl.ExtensionEnum
     * @return
     */
    public <T> T getExtValue(ExtensionEnum extensionEnum){
        Class<T> clazz = extensionEnum.clazz;
        if(extensionJson == null) extensionJson = com.alibaba.fastjson.JSONObject.parseObject(getExtensionStr());
        if(extensionJson == null) return null;
        return extensionJson.getObject(extensionEnum.name,clazz);
    }


    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] previousBlockHash, List<TransactionImpl> transactions, String secretPhrase) {
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, null, previousBlockHash, transactions);
        blockSignature = Crypto.sign(bytes(), secretPhrase);
        bytes = null;
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, List<TransactionImpl> transactions) {
        this.version = version;
        this.timestamp = timestamp;
        this.previousBlockId = previousBlockId;
        this.totalAmountNQT = totalAmountNQT;
        this.totalFeeNQT = totalFeeNQT;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;
        this.previousBlockHash = previousBlockHash;
        if (transactions != null) {
            this.blockTransactions = Collections.unmodifiableList(transactions);
        }
    }

    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength,
              byte[] payloadHash, long generatorId, byte[] generationSignature, byte[] blockSignature,
              byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget, long nextBlockId, int height, long id,
              List<TransactionImpl> blockTransactions) {
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                null, generationSignature, blockSignature, previousBlockHash, null);
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.baseTarget = baseTarget;
        this.nextBlockId = nextBlockId;
        this.height = height;
        this.id = id;
        this.generatorId = generatorId;
        this.blockTransactions = blockTransactions;
    }
    
    public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength,
              byte[] payloadHash, long generatorId, byte[] generationSignature, byte[] blockSignature,
              byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget, long nextBlockId, int height, long id, byte[] extension,
              List<TransactionImpl> blockTransactions) {
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                generatorId, generationSignature, blockSignature, previousBlockHash,cumulativeDifficulty,baseTarget,nextBlockId,height,id,blockTransactions);
        this.extension = extension;
    }

    //just for genesis block
    public static BlockImpl newGenesisBlock(long blockId,int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash,
                     byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, List<TransactionImpl> transactions){
        BlockImpl block = new BlockImpl(version,  timestamp,  previousBlockId,  totalAmountNQT,  totalFeeNQT,  payloadLength, payloadHash,
                generatorPublicKey, generationSignature, blockSignature, previousBlockHash, transactions);
        block.id= blockId;
        return block;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public long getPreviousBlockId() {
        return previousBlockId;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        if (generatorPublicKey == null) {
            generatorPublicKey = Account.getPublicKey(generatorId);
        }
        return generatorPublicKey;
    }

    @Override
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public long getTotalAmountNQT() {
        return totalAmountNQT;
    }

    @Override
    public long getTotalFeeNQT() {
        return totalFeeNQT;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public byte[] getPayloadHash() {
        return payloadHash;
    }

    @Override
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    @Override
    public byte[] getBlockSignature() {
        return blockSignature;
    }

    @Override
    public List<TransactionImpl> getTransactions() {
        if (this.blockTransactions == null) {
            List<TransactionImpl> transactions = Collections.unmodifiableList(TransactionDb.findBlockTransactions(getId()));
            for (TransactionImpl transaction : transactions) {
                transaction.setBlock(this);
                this.autoExtensionAppend(transaction);
            }
            this.blockTransactions = transactions;
        }
        return this.blockTransactions;
    }

    public void autoExtensionAppend(TransactionImpl transaction) {
        // auto extension process for isPoc and isPool
        if (TransactionType.TYPE_POC == transaction.getType().getType()) {
            this.addExtension(BlockImpl.ExtensionEnum.CONTAIN_POC, true);
        }

        if (TransactionType.TYPE_SHARDER_POOL == transaction.getType().getType()) {
            this.addExtension(BlockImpl.ExtensionEnum.CONTAIN_POOL, true);
        }
    }

    @Override
    public long getBaseTarget() {
        return baseTarget;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    @Override
    public long getNextBlockId() {
        return nextBlockId;
    }

    void setNextBlockId(long nextBlockId) {
        this.nextBlockId = nextBlockId;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public long getId() {
        if (id == 0) {
            if (blockSignature == null) {
                throw new IllegalStateException("Block is not signed yet");
            }
            byte[] hash = Crypto.sha256().digest(bytes());
            BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Long.toUnsignedString(id);
            }
        }
        return stringId;
    }

    @Override
    public long getGeneratorId() {
        if (generatorId == 0) {
            generatorId = Account.getId(getGeneratorPublicKey());
        }
        return generatorId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId() == ((BlockImpl)o).getId();
    }

    @Override
    public int hashCode() {
        return (int)(getId() ^ (getId() >>> 32));
    }

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("timestamp", timestamp);
        json.put("previousBlock", Long.toUnsignedString(previousBlockId));
        json.put("totalAmountNQT", totalAmountNQT);
        json.put("totalFeeNQT", totalFeeNQT);
        json.put("payloadLength", payloadLength);
        json.put("payloadHash", Convert.toHexString(payloadHash));
        json.put("generatorPublicKey", Convert.toHexString(getGeneratorPublicKey()));
        json.put("generationSignature", Convert.toHexString(generationSignature));
        if (version > 1) {
            json.put("previousBlockHash", Convert.toHexString(previousBlockHash));
        }
        json.put("blockSignature", Convert.toHexString(blockSignature));
        JSONArray transactionsData = new JSONArray();
        getTransactions().forEach(transaction -> transactionsData.add(transaction.getJSONObject()));
        json.put("transactions", transactionsData);
        json.put("extension", extension);
        return json;
    }

    public static BlockImpl parseBlock(JSONObject blockData) throws ConchException.NotValidException {
        try {
            int version = ((Long) blockData.get("version")).intValue();
            int timestamp = ((Long) blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            long totalAmountNQT = Convert.parseLong(blockData.get("totalAmountNQT"));
            long totalFeeNQT = Convert.parseLong(blockData.get("totalFeeNQT"));
            int payloadLength = ((Long) blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
            byte[] previousBlockHash = Convert.parseHexString((String) blockData.get("previousBlockHash"));
            List<TransactionImpl> blockTransactions = new ArrayList<>();
            for (Object transactionData : (JSONArray) blockData.get("transactions")) {
                blockTransactions.add(TransactionImpl.parseTransaction((JSONObject) transactionData));
            }
            BlockImpl block = new BlockImpl(version, timestamp, previousBlock, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash, generatorPublicKey,
                    generationSignature, blockSignature, previousBlockHash, blockTransactions);
            if (!block.checkSignature()) {
                throw new ConchException.NotValidException("Invalid block signature");
            }
            return block;
        } catch (ConchException.NotValidException|RuntimeException e) {
            Logger.logDebugMessage("Failed to parse block: " + blockData.toJSONString());
            throw e;
        }
    }

    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(bytes(), bytes.length);
    }

    public byte[] bytes() {
        if (bytes == null) {
            ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + (version < 3 ? (4 + 4) : (8 + 8)) + 4 + 32 + 32 + (32 + 32) + (blockSignature != null ? 64 : 0));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(version);
            buffer.putInt(timestamp);
            buffer.putLong(previousBlockId);
            buffer.putInt(getTransactions().size());
            if (version < 3) {
                buffer.putInt((int) (totalAmountNQT / Constants.ONE_SS));
                buffer.putInt((int) (totalFeeNQT / Constants.ONE_SS));
            } else {
                buffer.putLong(totalAmountNQT);
                buffer.putLong(totalFeeNQT);
            }
            buffer.putInt(payloadLength);
            buffer.put(payloadHash);
            buffer.put(getGeneratorPublicKey());
            buffer.put(generationSignature);
            if (version > 1) {
                buffer.put(previousBlockHash);
            }
            if (blockSignature != null) {
                buffer.put(blockSignature);
            }
            bytes = buffer.array();
        }
        return bytes;
    }

    public boolean verifyBlockSignature() {
        return checkSignature() && Account.setOrVerify(getGeneratorId(), getGeneratorPublicKey());
    }

    private volatile boolean hasValidSignature = false;

    private boolean checkSignature() {
        if (! hasValidSignature) {
            byte[] data = Arrays.copyOf(bytes(), bytes.length - 64);
            hasValidSignature = blockSignature != null && Crypto.verify(blockSignature, data, getGeneratorPublicKey(), true);
        }
        return hasValidSignature;
    }

    public boolean verifyGenerationSignature() throws BlockchainProcessor.BlockOutOfOrderException {

        try {
//            if(Constants.isTestnet() && Conch.getHeight() <= 1000 && SharderGenesis.isGenesisRecipients(getGeneratorId())){
//                return true;
//            }
            
            BlockImpl previousBlock = BlockchainImpl.getInstance().getBlock(getPreviousBlockId());
            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing", this);
            }
            Account creator = Account.getAccount(getGeneratorId());
            
            PocScore pocScoreObj = Conch.getPocProcessor().calPocScore(creator,previousBlock.getHeight());
            BigInteger pocScore = pocScoreObj.total();
            if (!pocScoreObj.qualifiedMiner()) {
                Logger.logWarningMessage(creator.getRsAddress() + " poc score is less than 0 in this block calculation generation signature verification");
                return false;
            }

            MessageDigest digest = Crypto.sha256();
            byte[] generationSignatureHash;
            digest.update(previousBlock.generationSignature);
            generationSignatureHash = digest.digest(getGeneratorPublicKey());
            if (!Arrays.equals(generationSignature, generationSignatureHash)) {
                Logger.logWarningMessage("current calculate generation signature of previous block is not same with previous block's generation signature");
                return false;
            }

            BigInteger hit = new BigInteger(1, new byte[]{generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
            boolean validHit = Generator.verifyHit(hit, pocScore, previousBlock, timestamp);
            
            boolean isIgnoreBlock = CheckSumValidator.isKnownIgnoreBlock(this.id);
            if(isIgnoreBlock) {
                Logger.logWarningMessage("Known ignore block[id=%d, height=%d] in %s, skip validation", this.getId(), (previousBlock.getHeight()+1), Constants.getNetwork().getName());
            }
            return validHit || isIgnoreBlock;

        } catch (RuntimeException e) {
            Logger.logMessage("Error verifying block generation signature " + toString(), e);
            return false;
        }
    }

    public void apply() {
        Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
        generatorAccount.apply(getGeneratorPublicKey());
        long totalBackFees = 0;
        if (this.height > Constants.SHUFFLING_BLOCK_HEIGHT) {
            long[] backFees = new long[3];
            for (TransactionImpl transaction : getTransactions()) {
                long[] fees = transaction.getBackFees();
                for (int i = 0; i < fees.length; i++) {
                    backFees[i] += fees[i];
                }
            }
            for (int i = 0; i < backFees.length; i++) {
                if (backFees[i] == 0) {
                    break;
                }
                totalBackFees += backFees[i];
                Account previousGeneratorAccount = Account.getAccount(BlockDb.findBlockAtHeight(this.height - i - 1).getGeneratorId());
                Logger.logDebugMessage("Back fees %f CDWH to miner at height %d", ((double)backFees[i])/Constants.ONE_SS, this.height - i - 1);
                previousGeneratorAccount.addBalanceAddUnconfirmed(AccountLedger.LedgerEvent.BLOCK_GENERATED, getId(), backFees[i]);
                previousGeneratorAccount.addMintedBalance(backFees[i]);
            }
        }
        if (totalBackFees != 0) {
            Logger.logDebugMessage("Fee reduced by %f CDWH at height %d", ((double)totalBackFees)/Constants.ONE_SS, this.height);
        }
        generatorAccount.addBalanceAddUnconfirmed(AccountLedger.LedgerEvent.BLOCK_GENERATED, getId(), totalFeeNQT - totalBackFees);
        generatorAccount.addMintedBalance(totalFeeNQT - totalBackFees);
    }

    public void setPrevious(BlockImpl block) {
        if (block != null) {
            if (block.getId() != getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            this.height = block.getHeight() + 1;
            this.calculateBaseTarget(block);
        } else {
            this.height = 0;
        }
        short index = 0;
        for (TransactionImpl transaction : getTransactions()) {
            transaction.setBlock(this);
            transaction.setIndex(index++);
        }
    }

    public void loadTransactions() {
        for (TransactionImpl transaction : getTransactions()) {
            transaction.bytes();
            transaction.getAppendages();
        }
    }

    private void calculateBaseTarget(BlockImpl previousBlock) {
        long prevBaseTarget = previousBlock.baseTarget;
        if (previousBlock.getHeight() <= Constants.SHUFFLING_BLOCK_HEIGHT) {
            baseTarget = BigInteger.valueOf(prevBaseTarget)
                    .multiply(BigInteger.valueOf(this.timestamp - previousBlock.timestamp))
                    .divide(BigInteger.valueOf(60)).longValue();
            if (baseTarget < 0 || baseTarget > Constants.MAX_BASE_TARGET) {
                baseTarget = Constants.MAX_BASE_TARGET;
            }
            if (baseTarget < prevBaseTarget / 2) {
                baseTarget = prevBaseTarget / 2;
            }
            if (baseTarget == 0) {
                baseTarget = 1;
            }
            long twofoldCurBaseTarget = prevBaseTarget * 2;
            if (twofoldCurBaseTarget < 0) {
                twofoldCurBaseTarget = Constants.MAX_BASE_TARGET;
            }
            if (baseTarget > twofoldCurBaseTarget) {
                baseTarget = twofoldCurBaseTarget;
            }
        } else if (previousBlock.getHeight() != 0 && previousBlock.getHeight() % 2 == 0) {
            BlockImpl block = BlockDb.findBlockAtHeight(previousBlock.getHeight() - 2);
            int blockTimeAverage = (this.timestamp - block.timestamp) / 3 - Constants.getBlockGapSeconds();
            if (blockTimeAverage > 60) {
                baseTarget = (prevBaseTarget * Math.min(blockTimeAverage, Constants.MAX_BLOCKTIME_LIMIT)) / 60;
            } else {
                baseTarget = prevBaseTarget - prevBaseTarget * Constants.BASE_TARGET_GAMMA
                        * (60 - Math.max(blockTimeAverage, Constants.MIN_BLOCKTIME_LIMIT)) / 6000;
            }
            if (baseTarget < 0 || baseTarget > Constants.MAX_BASE_TARGET_2) {
                baseTarget = Constants.MAX_BASE_TARGET_2;
            }
            if (baseTarget < Constants.MIN_BASE_TARGET) {
                baseTarget = Constants.MIN_BASE_TARGET;
            }
        } else {
            baseTarget = prevBaseTarget;
        }
        cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(baseTarget)));
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
