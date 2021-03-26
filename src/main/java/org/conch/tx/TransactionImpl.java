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

package org.conch.tx;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountRestrictions;
import org.conch.chain.*;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.reward.RewardCalculator;
import org.conch.crypto.Crypto;
import org.conch.db.DbKey;
import org.conch.util.Convert;
import org.conch.util.Filter;
import org.conch.util.Logger;
import org.conch.vote.PhasingPoll;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.*;

final public class TransactionImpl implements Transaction {

    static final public class BuilderImpl implements Builder {

        private final short deadline;
        private final byte[] senderPublicKey;
        private final long amountNQT;
        private final long feeNQT;
        private final TransactionType type;
        private final byte version;
        private Attachment.AbstractAttachment attachment;

        private long recipientId;
        private byte[] referencedTransactionFullHash;
        private byte[] signature;
        private Attachment.SaveHash saveHash;
        private Appendix.Message message;
        private Appendix.EncryptedMessage encryptedMessage;
        private Appendix.EncryptToSelfMessage encryptToSelfMessage;
        private Appendix.PublicKeyAnnouncement publicKeyAnnouncement;
        private Appendix.Phasing phasing;
        private Appendix.PrunablePlainMessage prunablePlainMessage;
        private Appendix.PrunableEncryptedMessage prunableEncryptedMessage;
        private long blockId;
        private int height = Integer.MAX_VALUE;
        private long id;
        private long senderId;
        private int timestamp = Integer.MAX_VALUE;
        private int blockTimestamp = -1;
        private byte[] fullHash;
        private boolean ecBlockSet = false;
        private int ecBlockHeight;
        private long ecBlockId;
        private short index = -1;

        public BuilderImpl(byte version, byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline,
                    Attachment.AbstractAttachment attachment) {
            this.version = version;
            this.deadline = deadline;
            this.senderPublicKey = senderPublicKey;
            this.amountNQT = amountNQT;
            this.feeNQT = feeNQT;
            this.attachment = attachment;
            this.type = attachment.getTransactionType();
        }

        public BuilderImpl(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline,
                           Attachment.AbstractAttachment attachment) {
            this(defaultTxVersion(),senderPublicKey,amountNQT,feeNQT,deadline,attachment);
        }

        @Override
        public TransactionImpl build(String secretPhrase) throws ConchException.NotValidException {
            if (timestamp == Integer.MAX_VALUE) {
                timestamp = Conch.getEpochTime();
            }
            if (!ecBlockSet) {
                Block ecBlock = BlockchainImpl.getInstance().getECBlock(timestamp);
                this.ecBlockHeight = ecBlock.getHeight();
                this.ecBlockId = ecBlock.getId();
            }
            return new TransactionImpl(this, secretPhrase);
        }

        @Override
        public TransactionImpl build() throws ConchException.NotValidException {
            return build(null);
        }

        public BuilderImpl recipientId(long recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        @Override
        public BuilderImpl referencedTransactionFullHash(String referencedTransactionFullHash) {
            this.referencedTransactionFullHash = Convert.parseHexString(referencedTransactionFullHash);
            return this;
        }

        BuilderImpl referencedTransactionFullHash(byte[] referencedTransactionFullHash) {
            this.referencedTransactionFullHash = referencedTransactionFullHash;
            return this;
        }

        BuilderImpl appendix(Attachment.AbstractAttachment attachment) {
            this.attachment = attachment;
            return this;
        }

        @Override
        public BuilderImpl appendix(Appendix.Message message) {
            this.message = message;
            return this;
        }

        @Override
        public BuilderImpl appendix(Appendix.EncryptedMessage encryptedMessage) {
            this.encryptedMessage = encryptedMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(Appendix.EncryptToSelfMessage encryptToSelfMessage) {
            this.encryptToSelfMessage = encryptToSelfMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(Appendix.PublicKeyAnnouncement publicKeyAnnouncement) {
            this.publicKeyAnnouncement = publicKeyAnnouncement;
            return this;
        }

        @Override
        public BuilderImpl appendix(Appendix.PrunablePlainMessage prunablePlainMessage) {
            this.prunablePlainMessage = prunablePlainMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(Appendix.PrunableEncryptedMessage prunableEncryptedMessage) {
            this.prunableEncryptedMessage = prunableEncryptedMessage;
            return this;
        }

        @Override
        public BuilderImpl appendix(Appendix.Phasing phasing) {
            this.phasing = phasing;
            return this;
        }

        @Override
        public BuilderImpl timestamp(int timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @Override
        public BuilderImpl ecBlockHeight(int height) {
            this.ecBlockHeight = height;
            this.ecBlockSet = true;
            return this;
        }

        @Override
        public BuilderImpl ecBlockId(long blockId) {
            this.ecBlockId = blockId;
            this.ecBlockSet = true;
            return this;
        }

        BuilderImpl id(long id) {
            this.id = id;
            return this;
        }

        public BuilderImpl signature(byte[] signature) {
            this.signature = signature;
            return this;
        }

        BuilderImpl blockId(long blockId) {
            this.blockId = blockId;
            return this;
        }

        public BuilderImpl height(int height) {
            this.height = height;
            return this;
        }

        BuilderImpl senderId(long senderId) {
            this.senderId = senderId;
            return this;
        }

        BuilderImpl fullHash(byte[] fullHash) {
            this.fullHash = fullHash;
            return this;
        }

        BuilderImpl blockTimestamp(int blockTimestamp) {
            this.blockTimestamp = blockTimestamp;
            return this;
        }

        BuilderImpl index(short index) {
            this.index = index;
            return this;
        }

    }

    private final short deadline;
    private volatile byte[] senderPublicKey;
    private final long recipientId;
    private final long amountNQT;
    private final long feeNQT;
    private final byte[] referencedTransactionFullHash;
    private final TransactionType type;
    private final int ecBlockHeight;
    private final long ecBlockId;
    private final byte version;
    private final int timestamp;
    private final byte[] signature;
    private final Attachment.SaveHash saveHash;
    private final Attachment.AbstractAttachment attachment;
    private final Appendix.Message message;
    private final Appendix.EncryptedMessage encryptedMessage;
    private final Appendix.EncryptToSelfMessage encryptToSelfMessage;
    private final Appendix.PublicKeyAnnouncement publicKeyAnnouncement;
    private final Appendix.Phasing phasing;
    private final Appendix.PrunablePlainMessage prunablePlainMessage;
    private final Appendix.PrunableEncryptedMessage prunableEncryptedMessage;

    private final List<Appendix.AbstractAppendix> appendages;
    private final int appendagesSize;

    private volatile int height = Integer.MAX_VALUE;
    private volatile long blockId;
    private volatile BlockImpl block;
    private volatile int blockTimestamp = -1;
    private volatile short index = -1;
    private volatile long id;
    private volatile String stringId;
    private volatile long senderId;
    private volatile byte[] fullHash;
    private volatile DbKey dbKey;
    private volatile byte[] bytes = null;


    private TransactionImpl(BuilderImpl builder, String secretPhrase) throws ConchException.NotValidException {

        this.timestamp = builder.timestamp;
        this.deadline = builder.deadline;
        this.senderPublicKey = builder.senderPublicKey;
        this.recipientId = builder.recipientId;
        this.amountNQT = builder.amountNQT;
        this.referencedTransactionFullHash = builder.referencedTransactionFullHash;
        this.type = builder.type;
        this.version = builder.version;
        this.blockId = builder.blockId;
        this.height = builder.height;
        this.index = builder.index;
        this.id = builder.id;
        this.senderId = builder.senderId;
        this.blockTimestamp = builder.blockTimestamp;
        this.fullHash = builder.fullHash;
		this.ecBlockHeight = builder.ecBlockHeight;
        this.ecBlockId = builder.ecBlockId;

        List<Appendix.AbstractAppendix> list = new ArrayList<>();
        if ((this.attachment = builder.attachment) != null) {
            list.add(this.attachment);
        }
        if ((this.saveHash  = builder.saveHash) != null) {
            list.add(this.saveHash);
        }
        if ((this.message  = builder.message) != null) {
            list.add(this.message);
        }
        if ((this.encryptedMessage = builder.encryptedMessage) != null) {
            list.add(this.encryptedMessage);
        }
        if ((this.publicKeyAnnouncement = builder.publicKeyAnnouncement) != null) {
            list.add(this.publicKeyAnnouncement);
        }
        if ((this.encryptToSelfMessage = builder.encryptToSelfMessage) != null) {
            list.add(this.encryptToSelfMessage);
        }
        if ((this.phasing = builder.phasing) != null) {
            list.add(this.phasing);
        }
        if ((this.prunablePlainMessage = builder.prunablePlainMessage) != null) {
            list.add(this.prunablePlainMessage);
        }
        if ((this.prunableEncryptedMessage = builder.prunableEncryptedMessage) != null) {
            list.add(this.prunableEncryptedMessage);
        }
        this.appendages = Collections.unmodifiableList(list);
        int appendagesSize = 0;
        for (Appendix appendage : appendages) {
            if (secretPhrase != null && appendage instanceof Appendix.Encryptable) {
                ((Appendix.Encryptable)appendage).encrypt(secretPhrase);
            }
            appendagesSize += appendage.getSize();
        }
        this.appendagesSize = appendagesSize;
        if (builder.feeNQT <= 0 || (Constants.correctInvalidFees && builder.signature == null)) {
            int effectiveHeight = (height < Integer.MAX_VALUE ? height : Conch.getBlockchain().getHeight());
            long minFee = getMinimumFeeNQT(effectiveHeight);
            feeNQT = Math.max(minFee, builder.feeNQT);
        } else {
            feeNQT = builder.feeNQT;
        }

        if (builder.signature != null && secretPhrase != null) {
            throw new ConchException.NotValidException("Transaction is already signed");
        } else if (builder.signature != null) {
            this.signature = builder.signature;
        } else if (secretPhrase != null) {
            if (getSenderPublicKey() != null && ! Arrays.equals(senderPublicKey, Crypto.getPublicKey(secretPhrase))) {
                throw new ConchException.NotValidException("Secret phrase doesn't match transaction sender public key");
            }
            signature = Crypto.sign(bytes(), secretPhrase);
            bytes = null;
        } else {
            signature = null;
        }

    }
    
    public static byte defaultTxVersion(){
//        if(Conch.versionCompare("0.1.6") >=0 ) {
        if(Conch.versionCompare("0.0.1") >=0 ) {
            return 3;
        }
        return 1;
    }
    
    public boolean checkVersion(){
//        if(Conch.versionCompare("0.1.6/") >=0 ) {
        if(Conch.versionCompare("0.0.1") >=0 ) {
            return 3 <= this.version;
        }
        return 1 <= this.version;
    }

    @Override
    public short getDeadline() {
        return deadline;
    }

    @Override
    public byte[] getSenderPublicKey() {
        if (senderPublicKey == null) {
            senderPublicKey = Account.getPublicKey(senderId);
        }
        return senderPublicKey;
    }

    @Override
    public long getRecipientId() {
        return recipientId;
    }

    @Override
    public long getAmountNQT() {
        return amountNQT;
    }

    @Override
    public long getFeeNQT() {
        return feeNQT;
    }

    public long[] getBackFees() {
        return type.getBackFees(this);
    }

    @Override
    public String getReferencedTransactionFullHash() {
        return Convert.toHexString(referencedTransactionFullHash);
    }

    public byte[] referencedTransactionFullHash() {
        return referencedTransactionFullHash;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }

    @Override
    public TransactionType getType() {
        return type;
    }

    @Override
    public byte getVersion() {
        return version;
    }

    @Override
    public long getBlockId() {
        return blockId;
    }

    @Override
    public BlockImpl getBlock() {
        if (block == null && blockId != 0) {
            block = BlockchainImpl.getInstance().getBlock(blockId);
        }
        return block;
    }

    public void setBlock(BlockImpl block) {
        this.block = block;
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.blockTimestamp = block.getTimestamp();
    }

    public void unsetBlock() {
        this.block = null;
        this.blockId = 0;
        this.blockTimestamp = -1;
        this.index = -1;
        // must keep the height set, as transactions already having been included in a popped-off block before
        // get priority when sorted for inclusion in a new block
    }

    @Override
    public short getIndex() {
        if (index == -1) {
            throw new IllegalStateException("Transaction index has not been set");
        }
        return index;
    }

    public void setIndex(int index) {
        this.index = (short)index;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    @Override
    public int getExpiration() {
        return timestamp + deadline * 60;
    }

    @Override
    public Attachment.AbstractAttachment getAttachment() {
        attachment.loadPrunable(this);
        return attachment;
    }

    @Override
    public List<Appendix.AbstractAppendix> getAppendages() {
        return getAppendages(false);
    }

    @Override
    public List<Appendix.AbstractAppendix> getAppendages(boolean includeExpiredPrunable) {
        for (Appendix.AbstractAppendix appendage : appendages) {
            appendage.loadPrunable(this, includeExpiredPrunable);
        }
        return appendages;
    }

    @Override
    public List<Appendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable) {
        List<Appendix> result = new ArrayList<>();
        appendages.forEach(appendix -> {
            if (filter.ok(appendix)) {
                appendix.loadPrunable(this, includeExpiredPrunable);
                result.add(appendix);
            }
        });
        return result;
    }

    @Override
    public long getId() {
        if (id == 0) {
            if (signature == null) {
                throw new IllegalStateException("Transaction is not signed yet");
            }
            
            byte[] data = zeroSignature(getBytes());
            byte[] signatureHash = Crypto.sha256().digest(signature);
            MessageDigest digest = Crypto.sha256();
            digest.update(data);
            fullHash = digest.digest(signatureHash);
            
            //[NQT]
//            if (useNQT()) {
//                byte[] data = zeroSignature(getBytes());
//                byte[] signatureHash = Crypto.sha256().digest(signature);
//                MessageDigest digest = Crypto.sha256();
//                digest.update(data);
//                fullHash = digest.digest(signatureHash);
//            } else {
//                fullHash = Crypto.sha256().digest(bytes());
//            }
            BigInteger bigInteger = new BigInteger(1, new byte[] {fullHash[7], fullHash[6], fullHash[5], fullHash[4], fullHash[3], fullHash[2], fullHash[1], fullHash[0]});
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
    public String getFullHash() {
        return Convert.toHexString(fullHash());
    }

    public byte[] fullHash() {
        if (fullHash == null) {
            getId();
        }
        return fullHash;
    }

    @Override
    public long getSenderId() {
        if (senderId == 0) {
            senderId = Account.getId(getSenderPublicKey());
        }
        return senderId;
    }

    public DbKey getDbKey() {
        if (dbKey == null) {
            dbKey = TransactionProcessorImpl.getInstance().unconfirmedTransactionDbKeyFactory.newKey(getId());
        }
        return dbKey;
    }

    @Override
    public Appendix.Message getMessage() {
        return message;
    }

    @Override
    public Appendix.EncryptedMessage getEncryptedMessage() {
        return encryptedMessage;
    }

    @Override
    public Appendix.EncryptToSelfMessage getEncryptToSelfMessage() {
        return encryptToSelfMessage;
    }

    @Override
    public Appendix.Phasing getPhasing() {
        return phasing;
    }

    public boolean attachmentIsPhased() {
        return attachment.isPhased(this);
    }

    Appendix.PublicKeyAnnouncement getPublicKeyAnnouncement() {
        return publicKeyAnnouncement;
    }

    @Override
    public Appendix.PrunablePlainMessage getPrunablePlainMessage() {
        if (prunablePlainMessage != null) {
            prunablePlainMessage.loadPrunable(this);
        }
        return prunablePlainMessage;
    }

    public boolean hasPrunablePlainMessage() {
        return prunablePlainMessage != null;
    }

    @Override
    public Appendix.PrunableEncryptedMessage getPrunableEncryptedMessage() {
        if (prunableEncryptedMessage != null) {
            prunableEncryptedMessage.loadPrunable(this);
        }
        return prunableEncryptedMessage;
    }

    @Override
    public Appendix.SaveHash getSaveHash() {
        return saveHash;
    }

    public boolean hasPrunableEncryptedMessage() {
        return prunableEncryptedMessage != null;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes(), bytes.length);
    }

    public byte[] bytes() {
        if (bytes == null) {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(getSize());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put(type.getType());
                buffer.put((byte) ((version << 4) | type.getSubtype()));
                buffer.putInt(timestamp);
                buffer.putShort(deadline);
                buffer.put(getSenderPublicKey());
                buffer.putLong(type.canHaveRecipient() ? recipientId : SharderGenesis.CREATOR_ID);
                
                buffer.putLong(amountNQT);
                buffer.putLong(feeNQT);
                if (referencedTransactionFullHash != null) {
                    buffer.put(referencedTransactionFullHash);
                } else {
                    buffer.put(new byte[32]);
                }
                
                //[NQT]
//                if (useNQT()) {
//                    buffer.putLong(amountNQT);
//                    buffer.putLong(feeNQT);
//                    if (referencedTransactionFullHash != null) {
//                        buffer.put(referencedTransactionFullHash);
//                    } else {
//                        buffer.put(new byte[32]);
//                    }
//                } else {
//                    buffer.putInt((int) (amountNQT / Constants.ONE_SS));
//                    buffer.putInt((int) (feeNQT / Constants.ONE_SS));
//                    if (referencedTransactionFullHash != null) {
//                        buffer.putLong(Convert.fullHashToId(referencedTransactionFullHash));
//                    } else {
//                        buffer.putLong(0L);
//                    }
//                }
                buffer.put(signature != null ? signature : new byte[64]);
                if (version > 0) {
                    buffer.putInt(getFlags());
                    buffer.putInt(ecBlockHeight);
                    buffer.putLong(ecBlockId);
                }
                for (Appendix appendage : appendages) {
                    appendage.putBytes(buffer);
                }
                bytes = buffer.array();
            } catch (RuntimeException e) {
                if (signature != null) {
                    Logger.logDebugMessage("Failed to get transaction bytes for transaction: " + getJSONObject().toJSONString());
                }
                throw e;
            }
        }
        return bytes;
    }

    public static BuilderImpl newTransactionBuilder(byte[] bytes) throws ConchException.NotValidException {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            byte type = buffer.get();
            byte subtype = buffer.get();
            byte version = (byte) ((subtype & 0xF0) >> 4);
            subtype = (byte) (subtype & 0x0F);
            int timestamp = buffer.getInt();
            short deadline = buffer.getShort();
            byte[] senderPublicKey = new byte[32];
            buffer.get(senderPublicKey);
            long recipientId = buffer.getLong();
            long amountNQT = buffer.getLong();
            long feeNQT = buffer.getLong();
            byte[] referencedTransactionFullHash = new byte[32];
            buffer.get(referencedTransactionFullHash);
            referencedTransactionFullHash = Convert.emptyToNull(referencedTransactionFullHash);
            byte[] signature = new byte[64];
            buffer.get(signature);
            signature = Convert.emptyToNull(signature);
            int flags = 0;
            int ecBlockHeight = 0;
            long ecBlockId = 0;
            if (version > 0) {
                flags = buffer.getInt();
                ecBlockHeight = buffer.getInt();
                ecBlockId = buffer.getLong();
            }
            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            BuilderImpl builder = new BuilderImpl(version, senderPublicKey, amountNQT, feeNQT,
                    deadline, transactionType.parseAttachment(buffer, version))
                    .timestamp(timestamp)
                    .referencedTransactionFullHash(referencedTransactionFullHash)
                    .signature(signature)
                    .ecBlockHeight(ecBlockHeight)
                    .ecBlockId(ecBlockId);
            if (transactionType.canHaveRecipient()) {
                builder.recipientId(recipientId);
            }
            int position = 1;
            if ((flags & position) != 0 || (version == 0 && transactionType == TransactionType.Messaging.ARBITRARY_MESSAGE)) {
                builder.appendix(new Appendix.Message(buffer, version));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new Appendix.EncryptedMessage(buffer, version));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new Appendix.PublicKeyAnnouncement(buffer, version));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new Appendix.EncryptToSelfMessage(buffer, version));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new Appendix.Phasing(buffer, version));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new Appendix.PrunablePlainMessage(buffer, version));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new Appendix.PrunableEncryptedMessage(buffer, version));
            }
            if (buffer.hasRemaining()) {
                throw new ConchException.NotValidException("Transaction bytes too long, " + buffer.remaining() + " extra bytes");
            }
            return builder;
        } catch (ConchException.NotValidException|RuntimeException e) {
            Logger.logDebugMessage("Failed to parse transaction bytes: " + Convert.toHexString(bytes));
            throw e;
        }
    }

    public static BuilderImpl newTransactionBuilder(byte[] bytes, JSONObject prunableAttachments) throws ConchException.NotValidException {
        BuilderImpl builder = newTransactionBuilder(bytes);
        if (prunableAttachments != null) {
            Attachment.ShufflingProcessing shufflingProcessing = Attachment.ShufflingProcessing.parse(prunableAttachments);
            if (shufflingProcessing != null) {
                builder.appendix(shufflingProcessing);
            }
            Attachment.TaggedDataUpload taggedDataUpload = Attachment.TaggedDataUpload.parse(prunableAttachments);
            if (taggedDataUpload != null) {
                builder.appendix(taggedDataUpload);
            }
            Attachment.TaggedDataExtend taggedDataExtend = Attachment.TaggedDataExtend.parse(prunableAttachments);
            if (taggedDataExtend != null) {
                builder.appendix(taggedDataExtend);
            }
            Appendix.PrunablePlainMessage prunablePlainMessage = Appendix.PrunablePlainMessage.parse(prunableAttachments);
            if (prunablePlainMessage != null) {
                builder.appendix(prunablePlainMessage);
            }
            Appendix.PrunableEncryptedMessage prunableEncryptedMessage = Appendix.PrunableEncryptedMessage.parse(prunableAttachments);
            if (prunableEncryptedMessage != null) {
                builder.appendix(prunableEncryptedMessage);
            }
        }
        return builder;
    }

    public byte[] getUnsignedBytes() {
        return zeroSignature(getBytes());
    }

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("type", type.getType());
        json.put("subtype", type.getSubtype());
        json.put("timestamp", timestamp);
        json.put("deadline", deadline);
        json.put("senderId", senderId);
        json.put("senderPublicKey", Convert.toHexString(getSenderPublicKey()));
        if (type.canHaveRecipient()) {
            json.put("recipient", Long.toUnsignedString(recipientId));
        }
        json.put("amountNQT", amountNQT);
        json.put("feeNQT", feeNQT);
        if (referencedTransactionFullHash != null) {
            json.put("referencedTransactionFullHash", Convert.toHexString(referencedTransactionFullHash));
        }
        json.put("ecBlockHeight", ecBlockHeight);
        json.put("height", height);
        json.put("ecBlockId", Long.toUnsignedString(ecBlockId));
        json.put("signature", Convert.toHexString(signature));
        JSONObject attachmentJSON = new JSONObject();
        for (Appendix.AbstractAppendix appendage : appendages) {
            appendage.loadPrunable(this);
            attachmentJSON.putAll(appendage.getJSONObject());
        }
        if (! attachmentJSON.isEmpty()) {
            json.put("attachment", attachmentJSON);
        }
        json.put("version", version);
        return json;
    }

    public String toPrintString(){
        JSONObject json =  getJSONObject();
        // check and remove overlong 'crowdMiners & consignors'
        if(json.containsKey("attachment")){
            JSONObject attachmentJson = (JSONObject) json.get("attachment");
            if(attachmentJson.containsKey("crowdMiners")){
                HashMap<Long, Long> map = JSON.parseObject((String) attachmentJson.get("crowdMiners"), new TypeReference<HashMap<Long, Long>>() {});
                if(map != null && map.size() > 50){
                    attachmentJson.put("crowdMinerSize", map.size());
                    attachmentJson.remove("crowdMiners");
                }
            }
            if(attachmentJson.containsKey("consignors")){
                HashMap<Long, Long> map = JSON.parseObject((String) attachmentJson.get("consignors"), new TypeReference<HashMap<Long, Long>>() {});
                if(map != null && map.size() > 50){
                    attachmentJson.put("consignorSize", map.size());
                    attachmentJson.remove("consignors");
                }
            }
            json.put("attachment", attachmentJson);
        }
        return json.toJSONString();
    }

    @Override
    public JSONObject getPrunableAttachmentJSON() {
        JSONObject prunableJSON = null;
        for (Appendix.AbstractAppendix appendage : appendages) {
            if (appendage instanceof Appendix.Prunable) {
                appendage.loadPrunable(this);
                if (prunableJSON == null) {
                    prunableJSON = appendage.getJSONObject();
                } else {
                    prunableJSON.putAll(appendage.getJSONObject());
                }
            }
        }
        return prunableJSON;
    }

    public static TransactionImpl parseTransaction(JSONObject transactionData) throws ConchException.NotValidException {
        TransactionImpl transaction = newTransactionBuilder(transactionData).build();
        if (transaction.getSignature() != null && !transaction.checkSignature()) {
            throw new ConchException.NotValidException("Invalid transaction signature for transaction " + transaction.getJSONObject().toJSONString());
        }
        return transaction;
    }

    public static BuilderImpl newTransactionBuilder(JSONObject transactionData) throws ConchException.NotValidException {
        try {
            byte type = ((Long) transactionData.get("type")).byteValue();
            byte subtype = ((Long) transactionData.get("subtype")).byteValue();
            int timestamp = ((Long) transactionData.get("timestamp")).intValue();
            short deadline = ((Long) transactionData.get("deadline")).shortValue();
            byte[] senderPublicKey = Convert.parseHexString((String) transactionData.get("senderPublicKey"));
            long amountNQT = Convert.parseLong(transactionData.get("amountNQT"));
            long feeNQT = Convert.parseLong(transactionData.get("feeNQT"));
            String referencedTransactionFullHash = (String) transactionData.get("referencedTransactionFullHash");
            byte[] signature = Convert.parseHexString((String) transactionData.get("signature"));
            Long versionValue = (Long) transactionData.get("version");
            byte version = versionValue == null ? 0 : versionValue.byteValue();
            JSONObject attachmentData = (JSONObject) transactionData.get("attachment");
            int ecBlockHeight = 0;
            long ecBlockId = 0;
            if (version > 0) {
                ecBlockHeight = ((Long) transactionData.get("ecBlockHeight")).intValue();
                ecBlockId = Convert.parseUnsignedLong((String) transactionData.get("ecBlockId"));
            }

            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            if (transactionType == null) {
                throw new ConchException.NotValidException("Invalid transaction type: " + type + ", " + subtype);
            }
            BuilderImpl builder = new BuilderImpl(version, senderPublicKey,
                    amountNQT, feeNQT, deadline,
                    transactionType.parseAttachment(attachmentData))
                    .timestamp(timestamp)
                    .referencedTransactionFullHash(referencedTransactionFullHash)
                    .signature(signature)
                    .ecBlockHeight(ecBlockHeight)
                    .ecBlockId(ecBlockId);
            if (transactionType.canHaveRecipient()) {
                long recipientId = Convert.parseUnsignedLong((String) transactionData.get("recipient"));
                builder.recipientId(recipientId);
            }
            if (attachmentData != null) {
                builder.appendix(Appendix.Message.parse(attachmentData));
                builder.appendix(Appendix.EncryptedMessage.parse(attachmentData));
                builder.appendix((Appendix.PublicKeyAnnouncement.parse(attachmentData)));
                builder.appendix(Appendix.EncryptToSelfMessage.parse(attachmentData));
                builder.appendix(Appendix.Phasing.parse(attachmentData));
                builder.appendix(Appendix.PrunablePlainMessage.parse(attachmentData));
                builder.appendix(Appendix.PrunableEncryptedMessage.parse(attachmentData));
            }
            return builder;
        } catch (ConchException.NotValidException|RuntimeException e) {
            Logger.logDebugMessage("Failed to parse transaction: " + transactionData.toJSONString());
            throw e;
        }
    }


    @Override
    public int getECBlockHeight() {
        return ecBlockHeight;
    }

    @Override
    public long getECBlockId() {
        return ecBlockId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TransactionImpl && this.getId() == ((Transaction)o).getId();
    }

    @Override
    public int hashCode() {
        return (int)(getId() ^ (getId() >>> 32));
    }

    public boolean verifySignature() {
        return checkSignature() && Account.setOrVerify(getSenderId(), getSenderPublicKey());
    }

    private volatile boolean hasValidSignature = false;

    private boolean checkSignature() {
        if(RewardCalculator.isBlockCrowdRewardTx(getAttachment())){
            if(RewardCalculator.closeValidationForCrowdCoinbaseTx) {
                return true;
            }
        }

        if (!hasValidSignature) {
            hasValidSignature = signature != null && Crypto.verify(signature, zeroSignature(getBytes()), getSenderPublicKey(), true);
        }
        return hasValidSignature;
    }

    private int getSize() {
        return signatureOffset() + 64  + (version > 0 ? 4 + 4 + 8 : 0) + appendagesSize;
    }

    @Override
    public int getFullSize() {
        int fullSize = getSize() - appendagesSize;
        for (Appendix.AbstractAppendix appendage : getAppendages()) {
            fullSize += appendage.getFullSize();
        }
        return fullSize;
    }

    //[NQT]
//    private int signatureOffset() {
//        return 1 + 1 + 4 + 2 + 32 + 8 + (useNQT() ? 8 + 8 + 32 : 4 + 4 + 8);
//    }
    private int signatureOffset() {
        return 1 + 1 + 4 + 2 + 32 + 8 + (8 + 8 + 32);
    }

//    private boolean useNQT() {
//        return this.height > Constants.NQT_BLOCK
//                && (this.timestamp > (Constants.isTestnetOrDevnet() ? 12908200 : 14271000)
//                || Conch.getBlockchain().getHeight() >= Constants.NQT_BLOCK);
//    }

    private byte[] zeroSignature(byte[] data) {
        int start = signatureOffset();
        for (int i = start; i < start + 64; i++) {
            data[i] = 0;
        }
        return data;
    }

    private int getFlags() {
        int flags = 0;
        int position = 1;
        if (message != null) {
            flags |= position;
        }
        position <<= 1;
        if (encryptedMessage != null) {
            flags |= position;
        }
        position <<= 1;
        if (publicKeyAnnouncement != null) {
            flags |= position;
        }
        position <<= 1;
        if (encryptToSelfMessage != null) {
            flags |= position;
        }
        position <<= 1;
        if (phasing != null) {
            flags |= position;
        }
        position <<= 1;
        if (prunablePlainMessage != null) {
            flags |= position;
        }
        position <<= 1;
        if (prunableEncryptedMessage != null) {
            flags |= position;
        }
        return flags;
    }

    /**
     * 交易校验
     * @throws ConchException.ValidationException
     */
    @Override
    public void validate() throws ConchException.ValidationException {
        //
        if(CheckSumValidator.isKnownIgnoreTx(this.id)){
            Logger.logWarningMessage("Known ignore tx[id=%d, height=%d] in %s, skip validation", this.id, Conch.getBlockchain().getHeight(), Constants.getNetwork().getName());
            return;
        }
        
        if (timestamp == 0 ? (deadline != 0 || feeNQT != 0) : (deadline < 1 || ((feeNQT < 0 && type instanceof TransactionType.CoinBase)
                || feeNQT <= 0 && !(type instanceof TransactionType.CoinBase || type instanceof TransactionType.BurnDeal)))
                || feeNQT > Constants.MAX_BALANCE_NQT
                || amountNQT < 0
                || amountNQT > Constants.MAX_BALANCE_NQT
                || type == null) {
            throw new ConchException.NotValidException("Invalid transaction parameters:\n type: " + type + ", timestamp: " + timestamp
                    + ", deadline: " + deadline + ", fee: " + feeNQT + ", amount: " + amountNQT);
        }

        if (referencedTransactionFullHash != null && referencedTransactionFullHash.length != 32) {
            throw new ConchException.NotValidException("Invalid referenced transaction full hash " + Convert.toHexString(referencedTransactionFullHash));
        }

        if (attachment == null || type != attachment.getTransactionType()) {
            throw new ConchException.NotValidException("Invalid attachment " + attachment + " for transaction of type " + type);
        }

        if (! type.canHaveRecipient()) {
            if (recipientId != 0 || getAmountNQT() != 0) {
                boolean isCoinBase = getAmountNQT() != 0 && type.getType() == TransactionType.TYPE_COIN_BASE;
                if(!isCoinBase){
                    throw new ConchException.NotValidException("Transactions of this type must have recipient == 0, amount == 0");
                }
            }
        }

        if (type.mustHaveRecipient() && version > 0) {
            if (recipientId == 0) {
                throw new ConchException.NotValidException("Transactions of this type must have a valid recipient");
            }
        }

        boolean validatingAtFinish = phasing != null && getSignature() != null && PhasingPoll.getPoll(getId()) != null;
        for (Appendix.AbstractAppendix appendage : appendages) {
            appendage.loadPrunable(this);
            if (! appendage.verifyVersion(this.version)) {
                throw new ConchException.NotValidException("Invalid attachment version " + appendage.getVersion()
                        + " for transaction version " + this.version);
            }
            if (validatingAtFinish) {
                appendage.validateAtFinish(this);
            } else {
                appendage.validate(this);
            }
        }

        if (getFullSize() > Constants.MAX_PAYLOAD_LENGTH) {
            throw new ConchException.NotValidException("Transaction size " + getFullSize() + " exceeds maximum payload size");
        }

        if (!validatingAtFinish) {
            int blockchainHeight = Conch.getBlockchain().getHeight();
            long minimumFeeNQT = getMinimumFeeNQT(blockchainHeight);
            if (feeNQT < minimumFeeNQT) {
                throw new ConchException.NotCurrentlyValidException(String.format("Transaction fee %f less than minimum fee %f at height %d",
                        ((double) feeNQT) / Constants.ONE_SS, ((double) minimumFeeNQT) / Constants.ONE_SS, blockchainHeight));
            }
            if (blockchainHeight > Constants.FXT_BLOCK && ecBlockId != 0) {
                if (blockchainHeight < ecBlockHeight) {
                    throw new ConchException.NotCurrentlyValidException("ecBlockHeight " + ecBlockHeight
                            + " exceeds blockchain height " + blockchainHeight);
                }
                if (BlockDb.findBlockIdAtHeight(ecBlockHeight) != ecBlockId) {
                    throw new ConchException.NotCurrentlyValidException("ecBlockHeight " + ecBlockHeight
                            + " does not match ecBlockId " + Long.toUnsignedString(ecBlockId)
                            + ", transaction was generated on a fork");
                }
            }
        }
        AccountRestrictions.checkTransaction(this, validatingAtFinish);
    }

    // returns false if double spending
    public boolean applyUnconfirmed() {
        Account senderAccount = Account.getAccount(getSenderId());
        return senderAccount != null && type.applyUnconfirmed(this, senderAccount);
    }

    public void apply() throws ConchException.StopException {
        if(CheckSumValidator.isKnownIgnoreTx(id)){
            Logger.logWarningMessage("this tx[id=%d, creator=%s, height=%d] is known ignored tx, don't apply and ignore it", id, Account.rsAccount(senderId), height);
            return;
        }
        
        Account senderAccount = Account.getAccount(getSenderId());
        senderAccount.apply(getSenderPublicKey());
        Account recipientAccount = null;
        if (recipientId != 0) {
            recipientAccount = Account.getAccount(recipientId);
            if (recipientAccount == null) {
                recipientAccount = Account.addOrGetAccount(recipientId);
            }
        }
        if (referencedTransactionFullHash != null
                && timestamp > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            senderAccount.addUnconfirmed(getType().getLedgerEvent(), getId(),
                    0, Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        if (attachmentIsPhased()) {
            senderAccount.addToBalanceNQT(getType().getLedgerEvent(), getId(), 0, -feeNQT);
        }
        for (Appendix.AbstractAppendix appendage : appendages) {
            if (!appendage.isPhased(this)) {
                appendage.loadPrunable(this);
                appendage.apply(this, senderAccount, recipientAccount);
            }
        }
    }

    public void undoUnconfirmed() {
        Account senderAccount = Account.getAccount(getSenderId());
        type.undoUnconfirmed(this, senderAccount);
    }

    public boolean attachmentIsDuplicate(Map<TransactionType, Map<String, Integer>> duplicates, boolean atAcceptanceHeight) {
        if (!attachmentIsPhased() && !atAcceptanceHeight) {
            // can happen for phased transactions having non-phasable attachment
            return false;
        }
        if (atAcceptanceHeight) {
            if (AccountRestrictions.isBlockDuplicate(this, duplicates)) {
                return true;
            }
            // all are checked at acceptance height for block duplicates
            if (type.isBlockDuplicate(this, duplicates)) {
                return true;
            }
            // phased are not further checked at acceptance height
            if (attachmentIsPhased()) {
                return false;
            }
        }
        // non-phased at acceptance height, and phased at execution height
        return type.isDuplicate(this, duplicates);
    }

    public boolean isUnconfirmedDuplicate(Map<TransactionType, Map<String, Integer>> duplicates) {
        return type.isUnconfirmedDuplicate(this, duplicates);
    }

    
    
    private static final String IS_FIXED = "isFixed";
    private static final String FEE = "fee";
    /**
     * define fixed fee txs:
     * TransactionType.TYPE_COIN_BASE -> 0 fee
     * TransactionType.TYPE_COIN_POC -> 1 fee
     * @return
     */
    private static com.alibaba.fastjson.JSONObject isFixedFee(byte transactionType){
        com.alibaba.fastjson.JSONObject feeMap = new com.alibaba.fastjson.JSONObject();
        if(transactionType == TransactionType.TYPE_COIN_BASE ||
                transactionType == TransactionType.TYPE_BURN_DEAL){
            feeMap.put(IS_FIXED,true);
            feeMap.put(FEE,0L);
        }else if(transactionType == TransactionType.TYPE_POC){
            feeMap.put(IS_FIXED,true);
            feeMap.put(FEE,1L);
        } else {
            feeMap.put(IS_FIXED,false);
        }
        return feeMap;
    }

    public long getMinimumFeeNQT(int blockchainHeight) {
        long totalFee = 0;
        byte transactionType = this.getType().getType();
        com.alibaba.fastjson.JSONObject feeMap = isFixedFee(transactionType);
        if(feeMap.getBooleanValue(IS_FIXED)){
            return feeMap.getLongValue(FEE);
        }

        if (transactionType == TransactionType.TYPE_BURN_DEAL) {
            return Constants.configFee.get(transactionType);
        }

        if (transactionType == TransactionType.TYPE_SAVE_HASH) {
            return Constants.configFee.get(transactionType);
        }
        
        if(transactionType != TransactionType.TYPE_DATA && Constants.configFee.get(transactionType) == 0){
            for (Appendix.AbstractAppendix appendage : appendages) {
                appendage.loadPrunable(this);
                if (blockchainHeight < appendage.getBaselineFeeHeight()) {
                    return 0; // No need to validate fees before baseline block
                }
                Fee fee = blockchainHeight >= appendage.getNextFeeHeight() ? appendage.getNextFee(this) : appendage.getBaselineFee(this);
                totalFee = Math.addExact(totalFee, fee.getFee(this, appendage));
            }
            if (referencedTransactionFullHash != null) {
                totalFee = Math.addExact(totalFee, Constants.ONE_SS);
            }
            return totalFee;
        }

        if(transactionType != TransactionType.TYPE_DATA && Constants.configFee.get(transactionType) != 0){
            return Constants.configFee.get(transactionType);
        }

        int size = this.getAttachment().getMyFullSize() / 1024 / 1024 + 8;
        if(size > Constants.configFee.size()){
            Logger.logErrorMessage("calculate the fee error, size:" + size + " configFee size:" + Constants.configFee.size());
            return Constants.ONE_SS;
        }
        return Constants.configFee.get(size);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
