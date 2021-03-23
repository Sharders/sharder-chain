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
import com.google.common.collect.Maps;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.asset.HoldingType;
import org.conch.asset.MonetaryTx;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.crypto.Crypto;
import org.conch.crypto.EncryptedData;
import org.conch.mint.pool.PoolRule;
import org.conch.shuffle.ShufflingParticipant;
import org.conch.shuffle.ShufflingTransaction;
import org.conch.storage.TaggedData;
import org.conch.storage.tx.StorageTx;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.vote.VoteWeighting;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;

public interface Attachment extends Appendix {

    TransactionType getTransactionType();

    /**
     * poc serial txs version must be set to 1
     */
    abstract class TxBodyBase extends AbstractAttachment {
        public TxBodyBase() {
        }

        public TxBodyBase(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, (byte)1);
        }

        public TxBodyBase(JSONObject attachmentData) {
            super(attachmentData);
        }

        public TxBodyBase(int version) {
            super(1);
        }
    }

    abstract class AbstractAttachment extends Appendix.AbstractAppendix implements Attachment {

        public AbstractAttachment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        public AbstractAttachment(JSONObject attachmentData) {
            super(attachmentData);
        }

        public AbstractAttachment(int version) {
            super(version);
        }

        public AbstractAttachment() {
        }

        @Override
        public final String getAppendixName() {
            return getTransactionType().getName();
        }

        @Override
        public final void validate(Transaction tx) throws ConchException.ValidationException {
            getTransactionType().validateAttachment(tx);
        }

        @Override
        public final void apply(Transaction tx, Account senderAccount, Account recipientAccount) {
            getTransactionType().apply((TransactionImpl) tx, senderAccount, recipientAccount);
        }

        @Override
        public final Fee getBaselineFee(Transaction transaction) {
            return getTransactionType().getBaselineFee(transaction);
        }

        @Override
        public final Fee getNextFee(Transaction transaction) {
            return getTransactionType().getNextFee(transaction);
        }

        @Override
        public final int getBaselineFeeHeight() {
            return getTransactionType().getBaselineFeeHeight();
        }

        @Override
        public final int getNextFeeHeight() {
            return getTransactionType().getNextFeeHeight();
        }

        @Override
        public final boolean isPhasable() {
            return !(this instanceof Prunable) && getTransactionType().isPhasable();
        }

        public final int getFinishValidationHeight(Transaction transaction) {
            return isPhased(transaction) ? transaction.getPhasing().getFinishHeight() - 1 : Conch.getBlockchain().getHeight();
        }
    }

    abstract class EmptyAttachment extends AbstractAttachment {

        private EmptyAttachment() {
            super(0);
        }

        @Override
        public final int getMySize() {
            return 0;
        }

        @Override
        public final void putMyBytes(ByteBuffer buffer) {
        }

        @Override
        public final void putMyJSON(JSONObject json) {
        }

        @Override
        public final boolean verifyVersion(byte transactionVersion) {
            return getVersion() == 0;
        }

    }

    EmptyAttachment ORDINARY_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Payment.ORDINARY;
        }

    };

    class CoinBase extends AbstractAttachment {
        private static final int MAX_COINBASE_TYPE_LENGTH = 20;
        public enum CoinBaseType {
            GENESIS,
            BLOCK_REWARD, // support miner and pool rewards distribution
            CROWD_BLOCK_REWARD, // support crowd miner and pool rewards distribution
            FOUNDING_TX,
            SPECIAL_LOGIC;

            public static CoinBaseType getType(String name) {
                for (CoinBaseType type : CoinBaseType.values()) {
                    if (type.name().equals(name)) {
                        return type;
                    }
                }
                return null;
            }
        }

        protected final CoinBaseType coinBaseType;
        //miner id
        protected final long creator;
        //pool id or account id
        protected final long generatorId;
        // account id : investment amount
        protected HashMap<Long, Long> consignors;
        // miner's account id : poc score
        protected HashMap<Long, Long> crowdMiners;

        public CoinBase(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            String typeNameStr = Convert.readString(buffer, buffer.getInt(), MAX_COINBASE_TYPE_LENGTH);
            this.coinBaseType = CoinBaseType.getType(typeNameStr);
            this.creator = buffer.getLong();
            this.generatorId = buffer.getLong();

            // Crowd Miner Map
            if(buffer.hasRemaining()){
                String crowdMinerJson = Convert.readString(buffer, buffer.getInt(), (Integer.MAX_VALUE / 3 - 1));
                crowdMiners = JSON.parseObject(crowdMinerJson, new TypeReference<HashMap<Long, Long>>() {});
            }else {
                crowdMiners = Maps.newHashMap();
            }

            // Consignors Map
            if(buffer.hasRemaining()){
                String consignorJson = Convert.readString(buffer, buffer.getInt(), (Integer.MAX_VALUE / 3 - 1));
                consignors = JSON.parseObject(consignorJson, new TypeReference<HashMap<Long, Long>>(){});
            }else {
                consignors = Maps.newHashMap();
            }
        }

        public CoinBase(JSONObject attachmentData) {
            super(attachmentData);
            this.coinBaseType = CoinBaseType.getType((String) attachmentData.get("coinBaseType"));
            this.creator = (Long) attachmentData.get("creator");
            this.generatorId = (Long) attachmentData.get("generatorId");
            this.consignors = JSON.parseObject((String) attachmentData.get("consignors"), new TypeReference<HashMap<Long, Long>>() {});
            this.crowdMiners = JSON.parseObject((String) attachmentData.get("crowdMiners"), new TypeReference<HashMap<Long, Long>>() {});
        }

        /**
         * The coinbase tx that used to issue the coins when block generated
         * @param creator  account id of creator
         * @param generatorId value is the pool id/ creator id when type is Block Reward; value is the recipient id when type is Genesis.
         * @param consignors pool joiner's map contains joiner id and reward amount
         * @param crowdMiners qualified miners at this height
         */
        public CoinBase(long creator, long generatorId, HashMap<Long, Long> consignors, HashMap<Long, Long> crowdMiners) {
            this.coinBaseType = CoinBaseType.CROWD_BLOCK_REWARD;
            this.creator = creator;
            this.generatorId = generatorId;
            this.consignors = consignors;
            this.crowdMiners = crowdMiners;
        }

        /**
         * The coinbase tx that used to issue the coins when block generated
         * @param coinBaseType see org.conch.tx.Attachment.CoinBase.CoinBaseType
         * @param creator  account id of creator
         * @param generatorId value is the pool id/ creator id when type is Block Reward; value is the recipient id when type is Genesis.
         * @param consignors pool joiner's map contains joiner id and reward amount
         */
        public CoinBase(CoinBaseType coinBaseType, long creator, long generatorId, HashMap<Long, Long> consignors) {
            this.coinBaseType = coinBaseType;
            this.creator = creator;
            this.generatorId = generatorId;
            this.consignors = consignors;
            this.crowdMiners = Maps.newHashMap();
        }

        @Override
        public int getMySize() {
            int size = 4 + coinBaseType.name().getBytes().length
                    + 8 + 8;

            // Crowd Miners
            byte[] crowdMinerBytes = JSON.toJSONBytes(crowdMiners);
            size += 4 + crowdMinerBytes.length;


            // Consignors
            byte[] consignorBytes = JSON.toJSONBytes(consignors);
            size += 4 + consignorBytes.length;

            return size;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] nameBytes = coinBaseType.name().getBytes();
            buffer.putInt(nameBytes.length);
            buffer.put(nameBytes);
            buffer.putLong(creator);
            buffer.putLong(generatorId);

            // Crowd Miner Rewards
            byte[] crowdMinerBytes = JSON.toJSONBytes(crowdMiners);
            buffer.putInt(crowdMinerBytes.length);
            if(crowdMinerBytes.length > 0){
                buffer.put(crowdMinerBytes);
            }

            // Pool Rewards
            byte[] consignorBytes = JSON.toJSONBytes(consignors);
            buffer.putInt(consignorBytes.length);
            if(consignorBytes.length > 0){
                buffer.put(consignorBytes);
            }
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("coinBaseType",  String.valueOf(coinBaseType));
            attachment.put("creator", creator);
            attachment.put("generatorId", generatorId);
            attachment.put("consignors", JSON.toJSONString(consignors));
            attachment.put("crowdMiners", JSON.toJSONString(crowdMiners));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.CoinBase.ORDINARY;
        }

        public CoinBaseType getCoinBaseType() {
            return coinBaseType;
        }

        public HashMap<Long, Long> getConsignors() {
            return consignors;
        }

        public HashMap<Long, Long> getCrowdMiners() {
            return crowdMiners;
        }

        public long getGeneratorId() {
            return generatorId;
        }

        public long getCreator() {
            return creator;
        }

        public boolean isType(CoinBaseType type){
            return (type != null && this.coinBaseType != null) && (type == this.coinBaseType);
        }
    }


    final class BurnDeal extends AbstractAttachment {

        private final long receiver;

        public BurnDeal(long receiver) {
            this.receiver = receiver;
        }

        public BurnDeal(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.receiver = buffer.getLong();
        }

        public BurnDeal(JSONObject attachmentData) {
            super(attachmentData);
            this.receiver = (long) attachmentData.get("receiver");
        }

        @Override
        public int getMySize() {
            return 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.put(Convert.toBytes(receiver));
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("receiver", receiver);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.BurnDeal.ORDINARY;
        }

        public long getReceiver() {
            return receiver;
        }
    }

    // the message payload is in the Appendix
    EmptyAttachment ARBITRARY_MESSAGE = new EmptyAttachment() {
        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ARBITRARY_MESSAGE;
        }

    };

    final class MessagingAliasAssignment extends AbstractAttachment {

        private final String aliasName;
        private final String aliasURI;

        public MessagingAliasAssignment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH).trim();
            aliasURI = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH).trim();
        }

        public MessagingAliasAssignment(JSONObject attachmentData) {
            super(attachmentData);
            aliasName = Convert.nullToEmpty((String) attachmentData.get("alias")).trim();
            aliasURI = Convert.nullToEmpty((String) attachmentData.get("uri")).trim();
        }

        public MessagingAliasAssignment(String aliasName, String aliasURI) {
            this.aliasName = aliasName.trim();
            this.aliasURI = aliasURI.trim();
        }

        @Override
        public int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 2 + Convert.toBytes(aliasURI).length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] alias = Convert.toBytes(this.aliasName);
            byte[] uri = Convert.toBytes(this.aliasURI);
            buffer.put((byte) alias.length);
            buffer.put(alias);
            buffer.putShort((short) uri.length);
            buffer.put(uri);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
            attachment.put("uri", aliasURI);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_ASSIGNMENT;
        }

        public String getAliasName() {
            return aliasName;
        }

        public String getAliasURI() {
            return aliasURI;
        }
    }

    final class MessagingAliasSell extends AbstractAttachment {

        private final String aliasName;
        private final long priceNQT;

        public MessagingAliasSell(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
            this.priceNQT = buffer.getLong();
        }

        public MessagingAliasSell(JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public MessagingAliasSell(String aliasName, long priceNQT) {
            this.aliasName = aliasName;
            this.priceNQT = priceNQT;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_SELL;
        }

        @Override
        public int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
            buffer.putLong(priceNQT);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
            attachment.put("priceNQT", priceNQT);
        }

        public String getAliasName() {
            return aliasName;
        }

        public long getPriceNQT() {
            return priceNQT;
        }
    }

    final class MessagingAliasBuy extends AbstractAttachment {

        private final String aliasName;

        public MessagingAliasBuy(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        }

        public MessagingAliasBuy(JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
        }

        public MessagingAliasBuy(String aliasName) {
            this.aliasName = aliasName;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_BUY;
        }

        @Override
        public int getMySize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
        }

        public String getAliasName() {
            return aliasName;
        }
    }

    final class MessagingAliasDelete extends AbstractAttachment {

        private final String aliasName;

        public MessagingAliasDelete(final ByteBuffer buffer, final byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        }

        public MessagingAliasDelete(final JSONObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
        }

        public MessagingAliasDelete(final String aliasName) {
            this.aliasName = aliasName;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_DELETE;
        }

        @Override
        public int getMySize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        public void putMyBytes(final ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
        }

        @Override
        public void putMyJSON(final JSONObject attachment) {
            attachment.put("alias", aliasName);
        }

        public String getAliasName() {
            return aliasName;
        }
    }

    final class MessagingPollCreation extends AbstractAttachment {

        public final static class PollBuilder {
            private final String pollName;
            private final String pollDescription;
            private final String[] pollOptions;

            private final int finishHeight;
            private final byte votingModel;

            private long minBalance = 0;
            private byte minBalanceModel;

            private final byte minNumberOfOptions;
            private final byte maxNumberOfOptions;

            private final byte minRangeValue;
            private final byte maxRangeValue;

            private long holdingId;

            public PollBuilder(final String pollName, final String pollDescription, final String[] pollOptions,
                               final int finishHeight, final byte votingModel,
                               byte minNumberOfOptions, byte maxNumberOfOptions,
                               byte minRangeValue, byte maxRangeValue) {
                this.pollName = pollName;
                this.pollDescription = pollDescription;
                this.pollOptions = pollOptions;

                this.finishHeight = finishHeight;
                this.votingModel = votingModel;
                this.minNumberOfOptions = minNumberOfOptions;
                this.maxNumberOfOptions = maxNumberOfOptions;
                this.minRangeValue = minRangeValue;
                this.maxRangeValue = maxRangeValue;

                this.minBalanceModel = VoteWeighting.VotingModel.get(votingModel).getMinBalanceModel().getCode();
            }

            public PollBuilder minBalance(byte minBalanceModel, long minBalance) {
                this.minBalanceModel = minBalanceModel;
                this.minBalance = minBalance;
                return this;
            }

            public PollBuilder holdingId(long holdingId) {
                this.holdingId = holdingId;
                return this;
            }

            public MessagingPollCreation build() {
                return new MessagingPollCreation(this);
            }
        }

        private final String pollName;
        private final String pollDescription;
        private final String[] pollOptions;

        private final int finishHeight;

        private final byte minNumberOfOptions;
        private final byte maxNumberOfOptions;
        private final byte minRangeValue;
        private final byte maxRangeValue;
        private final VoteWeighting voteWeighting;

        public MessagingPollCreation(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.pollName = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_NAME_LENGTH);
            this.pollDescription = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_DESCRIPTION_LENGTH);

            this.finishHeight = buffer.getInt();

            int numberOfOptions = buffer.get();
            if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                throw new ConchException.NotValidException("Invalid number of poll options: " + numberOfOptions);
            }

            this.pollOptions = new String[numberOfOptions];
            for (int i = 0; i < numberOfOptions; i++) {
                this.pollOptions[i] = Convert.readString(buffer, buffer.getShort(), Constants.MAX_POLL_OPTION_LENGTH);
            }

            byte votingModel = buffer.get();

            this.minNumberOfOptions = buffer.get();
            this.maxNumberOfOptions = buffer.get();

            this.minRangeValue = buffer.get();
            this.maxRangeValue = buffer.get();

            long minBalance = buffer.getLong();
            byte minBalanceModel = buffer.get();
            long holdingId = buffer.getLong();
            this.voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
        }

        public MessagingPollCreation(JSONObject attachmentData) {
            super(attachmentData);

            this.pollName = ((String) attachmentData.get("name")).trim();
            this.pollDescription = ((String) attachmentData.get("description")).trim();
            this.finishHeight = ((Long) attachmentData.get("finishHeight")).intValue();

            JSONArray options = (JSONArray) attachmentData.get("options");
            this.pollOptions = new String[options.size()];
            for (int i = 0; i < pollOptions.length; i++) {
                this.pollOptions[i] = ((String) options.get(i)).trim();
            }
            byte votingModel = ((Long) attachmentData.get("votingModel")).byteValue();

            this.minNumberOfOptions = ((Long) attachmentData.get("minNumberOfOptions")).byteValue();
            this.maxNumberOfOptions = ((Long) attachmentData.get("maxNumberOfOptions")).byteValue();
            this.minRangeValue = ((Long) attachmentData.get("minRangeValue")).byteValue();
            this.maxRangeValue = ((Long) attachmentData.get("maxRangeValue")).byteValue();

            long minBalance = Convert.parseLong(attachmentData.get("minBalance"));
            byte minBalanceModel = ((Long) attachmentData.get("minBalanceModel")).byteValue();
            long holdingId = Convert.parseUnsignedLong((String) attachmentData.get("holding"));
            this.voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
        }

        private MessagingPollCreation(PollBuilder builder) {
            this.pollName = builder.pollName;
            this.pollDescription = builder.pollDescription;
            this.pollOptions = builder.pollOptions;
            this.finishHeight = builder.finishHeight;
            this.minNumberOfOptions = builder.minNumberOfOptions;
            this.maxNumberOfOptions = builder.maxNumberOfOptions;
            this.minRangeValue = builder.minRangeValue;
            this.maxRangeValue = builder.maxRangeValue;
            this.voteWeighting = new VoteWeighting(builder.votingModel, builder.holdingId, builder.minBalance, builder.minBalanceModel);
        }

        @Override
        public int getMySize() {
            int size = 2 + Convert.toBytes(pollName).length + 2 + Convert.toBytes(pollDescription).length + 1;
            for (String pollOption : pollOptions) {
                size += 2 + Convert.toBytes(pollOption).length;
            }

            size += 4 + 1 + 1 + 1 + 1 + 1 + 8 + 1 + 8;

            return size;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.pollName);
            byte[] description = Convert.toBytes(this.pollDescription);
            byte[][] options = new byte[this.pollOptions.length][];
            for (int i = 0; i < this.pollOptions.length; i++) {
                options[i] = Convert.toBytes(this.pollOptions[i]);
            }

            buffer.putShort((short) name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.putInt(finishHeight);
            buffer.put((byte) options.length);
            for (byte[] option : options) {
                buffer.putShort((short) option.length);
                buffer.put(option);
            }
            buffer.put(this.voteWeighting.getVotingModel().getCode());

            buffer.put(this.minNumberOfOptions);
            buffer.put(this.maxNumberOfOptions);
            buffer.put(this.minRangeValue);
            buffer.put(this.maxRangeValue);

            buffer.putLong(this.voteWeighting.getMinBalance());
            buffer.put(this.voteWeighting.getMinBalanceModel().getCode());
            buffer.putLong(this.voteWeighting.getHoldingId());
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("name", this.pollName);
            attachment.put("description", this.pollDescription);
            attachment.put("finishHeight", this.finishHeight);
            JSONArray options = new JSONArray();
            if (this.pollOptions != null) {
                Collections.addAll(options, this.pollOptions);
            }
            attachment.put("options", options);


            attachment.put("minNumberOfOptions", this.minNumberOfOptions);
            attachment.put("maxNumberOfOptions", this.maxNumberOfOptions);

            attachment.put("minRangeValue", this.minRangeValue);
            attachment.put("maxRangeValue", this.maxRangeValue);

            attachment.put("votingModel", this.voteWeighting.getVotingModel().getCode());

            attachment.put("minBalance", this.voteWeighting.getMinBalance());
            attachment.put("minBalanceModel", this.voteWeighting.getMinBalanceModel().getCode());
            attachment.put("holding", Long.toUnsignedString(this.voteWeighting.getHoldingId()));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.POLL_CREATION;
        }

        public String getPollName() {
            return pollName;
        }

        public String getPollDescription() {
            return pollDescription;
        }

        public int getFinishHeight() {
            return finishHeight;
        }

        public String[] getPollOptions() {
            return pollOptions;
        }

        public byte getMinNumberOfOptions() {
            return minNumberOfOptions;
        }

        public byte getMaxNumberOfOptions() {
            return maxNumberOfOptions;
        }

        public byte getMinRangeValue() {
            return minRangeValue;
        }

        public byte getMaxRangeValue() {
            return maxRangeValue;
        }

        public VoteWeighting getVoteWeighting() {
            return voteWeighting;
        }

    }

    final class MessagingVoteCasting extends AbstractAttachment {

        private final long pollId;
        private final byte[] pollVote;

        public MessagingVoteCasting(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            pollId = buffer.getLong();
            int numberOfOptions = buffer.get();
            if (numberOfOptions > Constants.MAX_POLL_OPTION_COUNT) {
                throw new ConchException.NotValidException("More than " + Constants.MAX_POLL_OPTION_COUNT + " options in a vote");
            }
            pollVote = new byte[numberOfOptions];
            buffer.get(pollVote);
        }

        public MessagingVoteCasting(JSONObject attachmentData) {
            super(attachmentData);
            pollId = Convert.parseUnsignedLong((String) attachmentData.get("poll"));
            JSONArray vote = (JSONArray) attachmentData.get("vote");
            pollVote = new byte[vote.size()];
            for (int i = 0; i < pollVote.length; i++) {
                pollVote[i] = ((Long) vote.get(i)).byteValue();
            }
        }

        public MessagingVoteCasting(long pollId, byte[] pollVote) {
            this.pollId = pollId;
            this.pollVote = pollVote;
        }

        @Override
        public int getMySize() {
            return 8 + 1 + this.pollVote.length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(this.pollId);
            buffer.put((byte) this.pollVote.length);
            buffer.put(this.pollVote);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("poll", Long.toUnsignedString(this.pollId));
            JSONArray vote = new JSONArray();
            if (this.pollVote != null) {
                for (byte aPollVote : this.pollVote) {
                    vote.add(aPollVote);
                }
            }
            attachment.put("vote", vote);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.VOTE_CASTING;
        }

        public long getPollId() {
            return pollId;
        }

        public byte[] getPollVote() {
            return pollVote;
        }
    }

    final class MessagingPhasingVoteCasting extends AbstractAttachment {

        private final List<byte[]> transactionFullHashes;
        private final byte[] revealedSecret;

        public MessagingPhasingVoteCasting(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            byte length = buffer.get();
            transactionFullHashes = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                byte[] hash = new byte[32];
                buffer.get(hash);
                transactionFullHashes.add(hash);
            }
            int secretLength = buffer.getInt();
            if (secretLength > Constants.MAX_PHASING_REVEALED_SECRET_LENGTH) {
                throw new ConchException.NotValidException("Invalid revealed secret length " + secretLength);
            }
            if (secretLength > 0) {
                revealedSecret = new byte[secretLength];
                buffer.get(revealedSecret);
            } else {
                revealedSecret = Convert.EMPTY_BYTE;
            }
        }

        public MessagingPhasingVoteCasting(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray hashes = (JSONArray) attachmentData.get("transactionFullHashes");
            transactionFullHashes = new ArrayList<>(hashes.size());
            hashes.forEach(hash -> transactionFullHashes.add(Convert.parseHexString((String) hash)));
            String revealedSecret = Convert.emptyToNull((String) attachmentData.get("revealedSecret"));
            this.revealedSecret = revealedSecret != null ? Convert.parseHexString(revealedSecret) : Convert.EMPTY_BYTE;
        }

        public MessagingPhasingVoteCasting(List<byte[]> transactionFullHashes, byte[] revealedSecret) {
            this.transactionFullHashes = transactionFullHashes;
            this.revealedSecret = revealedSecret;
        }

        @Override
        public int getMySize() {
            return 1 + 32 * transactionFullHashes.size() + 4 + revealedSecret.length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.put((byte) transactionFullHashes.size());
            transactionFullHashes.forEach(buffer::put);
            buffer.putInt(revealedSecret.length);
            buffer.put(revealedSecret);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            JSONArray jsonArray = new JSONArray();
            transactionFullHashes.forEach(hash -> jsonArray.add(Convert.toHexString(hash)));
            attachment.put("transactionFullHashes", jsonArray);
            if (revealedSecret.length > 0) {
                attachment.put("revealedSecret", Convert.toHexString(revealedSecret));
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.PHASING_VOTE_CASTING;
        }

        public List<byte[]> getTransactionFullHashes() {
            return transactionFullHashes;
        }

        public byte[] getRevealedSecret() {
            return revealedSecret;
        }
    }

    final class MessagingHubAnnouncement extends AbstractAttachment {

        private final long minFeePerByteNQT;
        private final String[] uris;

        public MessagingHubAnnouncement(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.minFeePerByteNQT = buffer.getLong();
            int numberOfUris = buffer.get();
            if (numberOfUris > Constants.MAX_HUB_ANNOUNCEMENT_URIS) {
                throw new ConchException.NotValidException("Invalid number of URIs: " + numberOfUris);
            }
            this.uris = new String[numberOfUris];
            for (int i = 0; i < uris.length; i++) {
                uris[i] = Convert.readString(buffer, buffer.getShort(), Constants.MAX_HUB_ANNOUNCEMENT_URI_LENGTH);
            }
        }

        public MessagingHubAnnouncement(JSONObject attachmentData) throws ConchException.NotValidException {
            super(attachmentData);
            this.minFeePerByteNQT = (Long) attachmentData.get("minFeePerByte");
            try {
                JSONArray urisData = (JSONArray) attachmentData.get("uris");
                this.uris = new String[urisData.size()];
                for (int i = 0; i < uris.length; i++) {
                    uris[i] = (String) urisData.get(i);
                }
            } catch (RuntimeException e) {
                throw new ConchException.NotValidException("Error parsing hub terminal announcement parameters", e);
            }
        }

        public MessagingHubAnnouncement(long minFeePerByteNQT, String[] uris) {
            this.minFeePerByteNQT = minFeePerByteNQT;
            this.uris = uris;
        }

        @Override
        public int getMySize() {
            int size = 8 + 1;
            for (String uri : uris) {
                size += 2 + Convert.toBytes(uri).length;
            }
            return size;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(minFeePerByteNQT);
            buffer.put((byte) uris.length);
            for (String uri : uris) {
                byte[] uriBytes = Convert.toBytes(uri);
                buffer.putShort((short) uriBytes.length);
                buffer.put(uriBytes);
            }
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("minFeePerByteNQT", minFeePerByteNQT);
            JSONArray uris = new JSONArray();
            Collections.addAll(uris, this.uris);
            attachment.put("uris", uris);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.HUB_ANNOUNCEMENT;
        }

        public long getMinFeePerByteNQT() {
            return minFeePerByteNQT;
        }

        public String[] getUris() {
            return uris;
        }

    }

    final class MessagingAccountInfo extends AbstractAttachment {

        private final String name;
        private final String description;

        public MessagingAccountInfo(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH);
        }

        public MessagingAccountInfo(JSONObject attachmentData) {
            super(attachmentData);
            this.name = Convert.nullToEmpty((String) attachmentData.get("name"));
            this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
        }

        public MessagingAccountInfo(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte) name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_INFO;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

    }

    final class MessagingAccountProperty extends AbstractAttachment {

        private final String property;
        private final String value;

        public MessagingAccountProperty(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.property = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH).trim();
            this.value = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH).trim();
        }

        public MessagingAccountProperty(JSONObject attachmentData) {
            super(attachmentData);
            this.property = Convert.nullToEmpty((String) attachmentData.get("property")).trim();
            this.value = Convert.nullToEmpty((String) attachmentData.get("value")).trim();
        }

        public MessagingAccountProperty(String property, String value) {
            this.property = property.trim();
            this.value = Convert.nullToEmpty(value).trim();
        }

        @Override
        public int getMySize() {
            return 1 + Convert.toBytes(property).length + 1 + Convert.toBytes(value).length;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] property = Convert.toBytes(this.property);
            byte[] value = Convert.toBytes(this.value);
            buffer.put((byte) property.length);
            buffer.put(property);
            buffer.put((byte) value.length);
            buffer.put(value);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("property", property);
            attachment.put("value", value);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_PROPERTY;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }

    }

    final class MessagingAccountPropertyDelete extends AbstractAttachment {

        private final long propertyId;

        public MessagingAccountPropertyDelete(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.propertyId = buffer.getLong();
        }

        public MessagingAccountPropertyDelete(JSONObject attachmentData) {
            super(attachmentData);
            this.propertyId = Convert.parseUnsignedLong((String) attachmentData.get("property"));
        }

        public MessagingAccountPropertyDelete(long propertyId) {
            this.propertyId = propertyId;
        }

        @Override
        public int getMySize() {
            return 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(propertyId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("property", Long.toUnsignedString(propertyId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_PROPERTY_DELETE;
        }

        public long getPropertyId() {
            return propertyId;
        }

    }

    final class ColoredCoinsAssetIssuance extends AbstractAttachment {

        private final String name;
        private final String description;
        private final long quantityQNT;
        private final byte decimals;

        public ColoredCoinsAssetIssuance(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ASSET_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_DESCRIPTION_LENGTH);
            this.quantityQNT = buffer.getLong();
            this.decimals = buffer.get();
        }

        public ColoredCoinsAssetIssuance(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
            this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
            this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
        }

        public ColoredCoinsAssetIssuance(String name, String description, long quantityQNT, byte decimals) {
            this.name = name;
            this.description = Convert.nullToEmpty(description);
            this.quantityQNT = quantityQNT;
            this.decimals = decimals;
        }

        @Override
        public int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 8 + 1;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte) name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.putLong(quantityQNT);
            buffer.put(decimals);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("decimals", decimals);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public byte getDecimals() {
            return decimals;
        }
    }

    final class ColoredCoinsAssetTransfer extends AbstractAttachment {

        private final long assetId;
        private final long quantityQNT;
        private final String comment;

        public ColoredCoinsAssetTransfer(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.assetId = buffer.getLong();
            this.quantityQNT = buffer.getLong();
            this.comment = getVersion() == 0 ? Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH) : null;
        }

        public ColoredCoinsAssetTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
            this.comment = getVersion() == 0 ? Convert.nullToEmpty((String) attachmentData.get("comment")) : null;
        }

        public ColoredCoinsAssetTransfer(long assetId, long quantityQNT) {
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.comment = null;
        }

        @Override
        public int getMySize() {
            return 8 + 8 + (getVersion() == 0 ? (2 + Convert.toBytes(comment).length) : 0);
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityQNT);
            if (getVersion() == 0 && comment != null) {
                byte[] commentBytes = Convert.toBytes(this.comment);
                buffer.putShort((short) commentBytes.length);
                buffer.put(commentBytes);
            }
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Long.toUnsignedString(assetId));
            attachment.put("quantityQNT", quantityQNT);
            if (getVersion() == 0) {
                attachment.put("comment", comment);
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_TRANSFER;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public String getComment() {
            return comment;
        }

    }

    final class ColoredCoinsAssetDelete extends AbstractAttachment {

        private final long assetId;
        private final long quantityQNT;

        public ColoredCoinsAssetDelete(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.assetId = buffer.getLong();
            this.quantityQNT = buffer.getLong();
        }

        public ColoredCoinsAssetDelete(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
        }

        public ColoredCoinsAssetDelete(long assetId, long quantityQNT) {
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
        }

        @Override
        public int getMySize() {
            return 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityQNT);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Long.toUnsignedString(assetId));
            attachment.put("quantityQNT", quantityQNT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_DELETE;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

    }

    abstract class ColoredCoinsOrderPlacement extends AbstractAttachment {

        private final long assetId;
        private final long quantityQNT;
        private final long priceNQT;

        private ColoredCoinsOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.assetId = buffer.getLong();
            this.quantityQNT = buffer.getLong();
            this.priceNQT = buffer.getLong();
        }

        private ColoredCoinsOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        private ColoredCoinsOrderPlacement(long assetId, long quantityQNT, long priceNQT) {
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.priceNQT = priceNQT;
        }

        @Override
        public int getMySize() {
            return 8 + 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityQNT);
            buffer.putLong(priceNQT);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Long.toUnsignedString(assetId));
            attachment.put("quantityQNT", quantityQNT);
            attachment.put("priceNQT", priceNQT);
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public long getPriceNQT() {
            return priceNQT;
        }
    }

    final class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {

        public ColoredCoinsAskOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        public ColoredCoinsAskOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsAskOrderPlacement(long assetId, long quantityQNT, long priceNQT) {
            super(assetId, quantityQNT, priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT;
        }

    }

    final class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacement {

        public ColoredCoinsBidOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        public ColoredCoinsBidOrderPlacement(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsBidOrderPlacement(long assetId, long quantityQNT, long priceNQT) {
            super(assetId, quantityQNT, priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_PLACEMENT;
        }

    }

    abstract class ColoredCoinsOrderCancellation extends AbstractAttachment {

        private final long orderId;

        private ColoredCoinsOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.orderId = buffer.getLong();
        }

        private ColoredCoinsOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
            this.orderId = Convert.parseUnsignedLong((String) attachmentData.get("order"));
        }

        private ColoredCoinsOrderCancellation(long orderId) {
            this.orderId = orderId;
        }

        @Override
        public int getMySize() {
            return 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(orderId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("order", Long.toUnsignedString(orderId));
        }

        public long getOrderId() {
            return orderId;
        }
    }

    final class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {

        public ColoredCoinsAskOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        public ColoredCoinsAskOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsAskOrderCancellation(long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
        }

    }

    final class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellation {

        public ColoredCoinsBidOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        public ColoredCoinsBidOrderCancellation(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsBidOrderCancellation(long orderId) {
            super(orderId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
        }

    }

    final class ColoredCoinsDividendPayment extends AbstractAttachment {

        private final long assetId;
        private final int height;
        private final long amountNQTPerQNT;

        public ColoredCoinsDividendPayment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.assetId = buffer.getLong();
            this.height = buffer.getInt();
            this.amountNQTPerQNT = buffer.getLong();
        }

        public ColoredCoinsDividendPayment(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
            this.height = ((Long) attachmentData.get("height")).intValue();
            this.amountNQTPerQNT = Convert.parseLong(attachmentData.get("amountNQTPerQNT"));
        }

        public ColoredCoinsDividendPayment(long assetId, int height, long amountNQTPerQNT) {
            this.assetId = assetId;
            this.height = height;
            this.amountNQTPerQNT = amountNQTPerQNT;
        }

        @Override
        public int getMySize() {
            return 8 + 4 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putInt(height);
            buffer.putLong(amountNQTPerQNT);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("asset", Long.toUnsignedString(assetId));
            attachment.put("height", height);
            attachment.put("amountNQTPerQNT", amountNQTPerQNT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.DIVIDEND_PAYMENT;
        }

        public long getAssetId() {
            return assetId;
        }

        public int getHeight() {
            return height;
        }

        public long getAmountNQTPerQNT() {
            return amountNQTPerQNT;
        }

    }

    final class DigitalGoodsListing extends AbstractAttachment {

        private final String name;
        private final String description;
        private final String tags;
        private final int quantity;
        private final long priceNQT;

        public DigitalGoodsListing(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH);
            this.tags = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_TAGS_LENGTH);
            this.quantity = buffer.getInt();
            this.priceNQT = buffer.getLong();
        }

        public DigitalGoodsListing(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.description = (String) attachmentData.get("description");
            this.tags = (String) attachmentData.get("tags");
            this.quantity = ((Long) attachmentData.get("quantity")).intValue();
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public DigitalGoodsListing(String name, String description, String tags, int quantity, long priceNQT) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
        }

        @Override
        public int getMySize() {
            return 2 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 2
                    + Convert.toBytes(tags).length + 4 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] nameBytes = Convert.toBytes(name);
            buffer.putShort((short) nameBytes.length);
            buffer.put(nameBytes);
            byte[] descriptionBytes = Convert.toBytes(description);
            buffer.putShort((short) descriptionBytes.length);
            buffer.put(descriptionBytes);
            byte[] tagsBytes = Convert.toBytes(tags);
            buffer.putShort((short) tagsBytes.length);
            buffer.put(tagsBytes);
            buffer.putInt(quantity);
            buffer.putLong(priceNQT);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("tags", tags);
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.LISTING;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getTags() {
            return tags;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

    }

    final class DigitalGoodsDelisting extends AbstractAttachment {

        private final long goodsId;

        public DigitalGoodsDelisting(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
        }

        public DigitalGoodsDelisting(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
        }

        public DigitalGoodsDelisting(long goodsId) {
            this.goodsId = goodsId;
        }

        @Override
        public int getMySize() {
            return 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELISTING;
        }

        public long getGoodsId() {
            return goodsId;
        }

    }

    final class DigitalGoodsPriceChange extends AbstractAttachment {

        private final long goodsId;
        private final long priceNQT;

        public DigitalGoodsPriceChange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.priceNQT = buffer.getLong();
        }

        public DigitalGoodsPriceChange(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
        }

        public DigitalGoodsPriceChange(long goodsId, long priceNQT) {
            this.goodsId = goodsId;
            this.priceNQT = priceNQT;
        }

        @Override
        public int getMySize() {
            return 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putLong(priceNQT);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
            attachment.put("priceNQT", priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PRICE_CHANGE;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

    }

    final class DigitalGoodsQuantityChange extends AbstractAttachment {

        private final long goodsId;
        private final int deltaQuantity;

        public DigitalGoodsQuantityChange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.deltaQuantity = buffer.getInt();
        }

        public DigitalGoodsQuantityChange(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
            this.deltaQuantity = ((Long) attachmentData.get("deltaQuantity")).intValue();
        }

        public DigitalGoodsQuantityChange(long goodsId, int deltaQuantity) {
            this.goodsId = goodsId;
            this.deltaQuantity = deltaQuantity;
        }

        @Override
        public int getMySize() {
            return 8 + 4;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(deltaQuantity);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
            attachment.put("deltaQuantity", deltaQuantity);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.QUANTITY_CHANGE;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public int getDeltaQuantity() {
            return deltaQuantity;
        }

    }

    final class DigitalGoodsPurchase extends AbstractAttachment {

        private final long goodsId;
        private final int quantity;
        private final long priceNQT;
        private final int deliveryDeadlineTimestamp;

        public DigitalGoodsPurchase(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.quantity = buffer.getInt();
            this.priceNQT = buffer.getLong();
            this.deliveryDeadlineTimestamp = buffer.getInt();
        }

        public DigitalGoodsPurchase(JSONObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
            this.quantity = ((Long) attachmentData.get("quantity")).intValue();
            this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
            this.deliveryDeadlineTimestamp = ((Long) attachmentData.get("deliveryDeadlineTimestamp")).intValue();
        }

        public DigitalGoodsPurchase(long goodsId, int quantity, long priceNQT, int deliveryDeadlineTimestamp) {
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
            this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
        }

        @Override
        public int getMySize() {
            return 8 + 4 + 8 + 4;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(quantity);
            buffer.putLong(priceNQT);
            buffer.putInt(deliveryDeadlineTimestamp);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("goods", Long.toUnsignedString(goodsId));
            attachment.put("quantity", quantity);
            attachment.put("priceNQT", priceNQT);
            attachment.put("deliveryDeadlineTimestamp", deliveryDeadlineTimestamp);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PURCHASE;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        public int getDeliveryDeadlineTimestamp() {
            return deliveryDeadlineTimestamp;
        }

    }

    class DigitalGoodsDelivery extends AbstractAttachment {

        private final long purchaseId;
        private EncryptedData goods;
        private final long discountNQT;
        private final boolean goodsIsText;

        public DigitalGoodsDelivery(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
            int length = buffer.getInt();
            goodsIsText = length < 0;
            if (length < 0) {
                length &= Integer.MAX_VALUE;
            }
            this.goods = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_DGS_GOODS_LENGTH);
            this.discountNQT = buffer.getLong();
        }

        public DigitalGoodsDelivery(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
            this.goods = new EncryptedData(Convert.parseHexString((String) attachmentData.get("goodsData")),
                    Convert.parseHexString((String) attachmentData.get("goodsNonce")));
            this.discountNQT = Convert.parseLong(attachmentData.get("discountNQT"));
            this.goodsIsText = Boolean.TRUE.equals(attachmentData.get("goodsIsText"));
        }

        public DigitalGoodsDelivery(long purchaseId, EncryptedData goods, boolean goodsIsText, long discountNQT) {
            this.purchaseId = purchaseId;
            this.goods = goods;
            this.discountNQT = discountNQT;
            this.goodsIsText = goodsIsText;
        }

        @Override
        public int getMySize() {
            return 8 + 4 + goods.getSize() + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putInt(goodsIsText ? goods.getData().length | Integer.MIN_VALUE : goods.getData().length);
            buffer.put(goods.getData());
            buffer.put(goods.getNonce());
            buffer.putLong(discountNQT);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Long.toUnsignedString(purchaseId));
            attachment.put("goodsData", Convert.toHexString(goods.getData()));
            attachment.put("goodsNonce", Convert.toHexString(goods.getNonce()));
            attachment.put("discountNQT", discountNQT);
            attachment.put("goodsIsText", goodsIsText);
        }

        @Override
        public final TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELIVERY;
        }

        public final long getPurchaseId() {
            return purchaseId;
        }

        public final EncryptedData getGoods() {
            return goods;
        }

        final void setGoods(EncryptedData goods) {
            this.goods = goods;
        }

        public int getGoodsDataLength() {
            return goods.getData().length;
        }

        public final long getDiscountNQT() {
            return discountNQT;
        }

        public final boolean goodsIsText() {
            return goodsIsText;
        }

    }

    final class UnencryptedDigitalGoodsDelivery extends DigitalGoodsDelivery implements Encryptable {

        private final byte[] goodsToEncrypt;
        private final byte[] recipientPublicKey;

        public UnencryptedDigitalGoodsDelivery(JSONObject attachmentData) {
            super(attachmentData);
            setGoods(null);
            String goodsToEncryptString = (String) attachmentData.get("goodsToEncrypt");
            this.goodsToEncrypt = goodsIsText() ? Convert.toBytes(goodsToEncryptString)
                    : Convert.parseHexString(goodsToEncryptString);
            this.recipientPublicKey = Convert.parseHexString((String) attachmentData.get("recipientPublicKey"));
        }

        public UnencryptedDigitalGoodsDelivery(long purchaseId, byte[] goodsToEncrypt, boolean goodsIsText, long discountNQT, byte[] recipientPublicKey) {
            super(purchaseId, null, goodsIsText, discountNQT);
            this.goodsToEncrypt = goodsToEncrypt;
            this.recipientPublicKey = recipientPublicKey;
        }

        @Override
        public int getMySize() {
            if (getGoods() == null) {
                return 8 + 4 + EncryptedData.getEncryptedSize(getPlaintext()) + 8;
            }
            return super.getMySize();
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            if (getGoods() == null) {
                throw new ConchException.NotYetEncryptedException("Goods not yet encrypted");
            }
            super.putMyBytes(buffer);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            if (getGoods() == null) {
                attachment.put("goodsToEncrypt", goodsIsText() ? Convert.toString(goodsToEncrypt) : Convert.toHexString(goodsToEncrypt));
                attachment.put("recipientPublicKey", Convert.toHexString(recipientPublicKey));
                attachment.put("purchase", Long.toUnsignedString(getPurchaseId()));
                attachment.put("discountNQT", getDiscountNQT());
                attachment.put("goodsIsText", goodsIsText());
            } else {
                super.putMyJSON(attachment);
            }
        }

        @Override
        public void encrypt(String secretPhrase) {
            setGoods(EncryptedData.encrypt(getPlaintext(), secretPhrase, recipientPublicKey));
        }

        @Override
        public int getGoodsDataLength() {
            return EncryptedData.getEncryptedDataLength(getPlaintext());
        }

        private byte[] getPlaintext() {
            return Convert.compress(goodsToEncrypt);
        }

    }

    final class DigitalGoodsFeedback extends AbstractAttachment {

        private final long purchaseId;

        public DigitalGoodsFeedback(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
        }

        public DigitalGoodsFeedback(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
        }

        public DigitalGoodsFeedback(long purchaseId) {
            this.purchaseId = purchaseId;
        }

        @Override
        public int getMySize() {
            return 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Long.toUnsignedString(purchaseId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.FEEDBACK;
        }

        public long getPurchaseId() {
            return purchaseId;
        }

    }

    final class DigitalGoodsRefund extends AbstractAttachment {

        private final long purchaseId;
        private final long refundNQT;

        public DigitalGoodsRefund(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
            this.refundNQT = buffer.getLong();
        }

        public DigitalGoodsRefund(JSONObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
            this.refundNQT = Convert.parseLong(attachmentData.get("refundNQT"));
        }

        public DigitalGoodsRefund(long purchaseId, long refundNQT) {
            this.purchaseId = purchaseId;
            this.refundNQT = refundNQT;
        }

        @Override
        public int getMySize() {
            return 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putLong(refundNQT);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("purchase", Long.toUnsignedString(purchaseId));
            attachment.put("refundNQT", refundNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.REFUND;
        }

        public long getPurchaseId() {
            return purchaseId;
        }

        public long getRefundNQT() {
            return refundNQT;
        }

    }

    final class AccountControlEffectiveBalanceLeasing extends AbstractAttachment {

        private final int period;

        public AccountControlEffectiveBalanceLeasing(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.period = Short.toUnsignedInt(buffer.getShort());
        }

        public AccountControlEffectiveBalanceLeasing(JSONObject attachmentData) {
            super(attachmentData);
            this.period = ((Long) attachmentData.get("period")).intValue();
        }

        public AccountControlEffectiveBalanceLeasing(int period) {
            this.period = period;
        }

        @Override
        public int getMySize() {
            return 2;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putShort((short) period);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("period", period);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AccountControl.EFFECTIVE_BALANCE_LEASING;
        }

        public int getPeriod() {
            return period;
        }
    }

    interface MonetarySystemAttachment {

        long getCurrencyId();

    }

    final class MonetarySystemCurrencyIssuance extends AbstractAttachment {

        private final String name;
        private final String code;
        private final String description;
        private final byte type;
        private final long initialSupply;
        private final long reserveSupply;
        private final long maxSupply;
        private final int issuanceHeight;
        private final long minReservePerUnitNQT;
        private final int minDifficulty;
        private final int maxDifficulty;
        private final byte ruleset;
        private final byte algorithm;
        private final byte decimals;

        public MonetarySystemCurrencyIssuance(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_NAME_LENGTH);
            this.code = Convert.readString(buffer, buffer.get(), Constants.MAX_CURRENCY_CODE_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_CURRENCY_DESCRIPTION_LENGTH);
            this.type = buffer.get();
            this.initialSupply = buffer.getLong();
            this.reserveSupply = buffer.getLong();
            this.maxSupply = buffer.getLong();
            this.issuanceHeight = buffer.getInt();
            this.minReservePerUnitNQT = buffer.getLong();
            this.minDifficulty = buffer.get() & 0xFF;
            this.maxDifficulty = buffer.get() & 0xFF;
            this.ruleset = buffer.get();
            this.algorithm = buffer.get();
            this.decimals = buffer.get();
        }

        public MonetarySystemCurrencyIssuance(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.code = (String) attachmentData.get("code");
            this.description = (String) attachmentData.get("description");
            this.type = ((Long) attachmentData.get("type")).byteValue();
            this.initialSupply = Convert.parseLong(attachmentData.get("initialSupply"));
            this.reserveSupply = Convert.parseLong(attachmentData.get("reserveSupply"));
            this.maxSupply = Convert.parseLong(attachmentData.get("maxSupply"));
            this.issuanceHeight = ((Long) attachmentData.get("issuanceHeight")).intValue();
            this.minReservePerUnitNQT = Convert.parseLong(attachmentData.get("minReservePerUnitNQT"));
            this.minDifficulty = ((Long) attachmentData.get("minDifficulty")).intValue();
            this.maxDifficulty = ((Long) attachmentData.get("maxDifficulty")).intValue();
            this.ruleset = ((Long) attachmentData.get("ruleset")).byteValue();
            this.algorithm = ((Long) attachmentData.get("algorithm")).byteValue();
            this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
        }

        public MonetarySystemCurrencyIssuance(String name, String code, String description, byte type, long initialSupply, long reserveSupply,
                                              long maxSupply, int issuanceHeight, long minReservePerUnitNQT, int minDifficulty, int maxDifficulty,
                                              byte ruleset, byte algorithm, byte decimals) {
            this.name = name;
            this.code = code;
            this.description = description;
            this.type = type;
            this.initialSupply = initialSupply;
            this.reserveSupply = reserveSupply;
            this.maxSupply = maxSupply;
            this.issuanceHeight = issuanceHeight;
            this.minReservePerUnitNQT = minReservePerUnitNQT;
            this.minDifficulty = minDifficulty;
            this.maxDifficulty = maxDifficulty;
            this.ruleset = ruleset;
            this.algorithm = algorithm;
            this.decimals = decimals;
        }

        @Override
        public int getMySize() {
            return 1 + Convert.toBytes(name).length + 1 + Convert.toBytes(code).length + 2 +
                    Convert.toBytes(description).length + 1 + 8 + 8 + 8 + 4 + 8 + 1 + 1 + 1 + 1 + 1;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] code = Convert.toBytes(this.code);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte) name.length);
            buffer.put(name);
            buffer.put((byte) code.length);
            buffer.put(code);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.put(type);
            buffer.putLong(initialSupply);
            buffer.putLong(reserveSupply);
            buffer.putLong(maxSupply);
            buffer.putInt(issuanceHeight);
            buffer.putLong(minReservePerUnitNQT);
            buffer.put((byte) minDifficulty);
            buffer.put((byte) maxDifficulty);
            buffer.put(ruleset);
            buffer.put(algorithm);
            buffer.put(decimals);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("code", code);
            attachment.put("description", description);
            attachment.put("type", type);
            attachment.put("initialSupply", initialSupply);
            attachment.put("reserveSupply", reserveSupply);
            attachment.put("maxSupply", maxSupply);
            attachment.put("issuanceHeight", issuanceHeight);
            attachment.put("minReservePerUnitNQT", minReservePerUnitNQT);
            attachment.put("minDifficulty", minDifficulty);
            attachment.put("maxDifficulty", maxDifficulty);
            attachment.put("ruleset", ruleset);
            attachment.put("algorithm", algorithm);
            attachment.put("decimals", decimals);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetaryTx.CURRENCY_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public byte getType() {
            return type;
        }

        public long getInitialSupply() {
            return initialSupply;
        }

        public long getReserveSupply() {
            return reserveSupply;
        }

        public long getMaxSupply() {
            return maxSupply;
        }

        public int getIssuanceHeight() {
            return issuanceHeight;
        }

        public long getMinReservePerUnitNQT() {
            return minReservePerUnitNQT;
        }

        public int getMinDifficulty() {
            return minDifficulty;
        }

        public int getMaxDifficulty() {
            return maxDifficulty;
        }

        public byte getRuleset() {
            return ruleset;
        }

        public byte getAlgorithm() {
            return algorithm;
        }

        public byte getDecimals() {
            return decimals;
        }
    }

    final class MonetarySystemReserveIncrease extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long amountPerUnitNQT;

        public MonetarySystemReserveIncrease(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.amountPerUnitNQT = buffer.getLong();
        }

        public MonetarySystemReserveIncrease(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.amountPerUnitNQT = Convert.parseLong(attachmentData.get("amountPerUnitNQT"));
        }

        public MonetarySystemReserveIncrease(long currencyId, long amountPerUnitNQT) {
            this.currencyId = currencyId;
            this.amountPerUnitNQT = amountPerUnitNQT;
        }

        @Override
        public int getMySize() {
            return 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(amountPerUnitNQT);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("amountPerUnitNQT", amountPerUnitNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetaryTx.RESERVE_INCREASE;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getAmountPerUnitNQT() {
            return amountPerUnitNQT;
        }

    }

    final class MonetarySystemReserveClaim extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long units;

        public MonetarySystemReserveClaim(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        public MonetarySystemReserveClaim(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemReserveClaim(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        public int getMySize() {
            return 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetaryTx.RESERVE_CLAIM;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

    }

    final class MonetarySystemCurrencyTransfer extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long units;

        public MonetarySystemCurrencyTransfer(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
        }

        public MonetarySystemCurrencyTransfer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        public MonetarySystemCurrencyTransfer(long currencyId, long units) {
            this.currencyId = currencyId;
            this.units = units;
        }

        @Override
        public int getMySize() {
            return 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(units);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetaryTx.CURRENCY_TRANSFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }
    }

    final class MonetarySystemPublishExchangeOffer extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long buyRateNQT;
        private final long sellRateNQT;
        private final long totalBuyLimit;
        private final long totalSellLimit;
        private final long initialBuySupply;
        private final long initialSellSupply;
        private final int expirationHeight;

        public MonetarySystemPublishExchangeOffer(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.buyRateNQT = buffer.getLong();
            this.sellRateNQT = buffer.getLong();
            this.totalBuyLimit = buffer.getLong();
            this.totalSellLimit = buffer.getLong();
            this.initialBuySupply = buffer.getLong();
            this.initialSellSupply = buffer.getLong();
            this.expirationHeight = buffer.getInt();
        }

        public MonetarySystemPublishExchangeOffer(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.buyRateNQT = Convert.parseLong(attachmentData.get("buyRateNQT"));
            this.sellRateNQT = Convert.parseLong(attachmentData.get("sellRateNQT"));
            this.totalBuyLimit = Convert.parseLong(attachmentData.get("totalBuyLimit"));
            this.totalSellLimit = Convert.parseLong(attachmentData.get("totalSellLimit"));
            this.initialBuySupply = Convert.parseLong(attachmentData.get("initialBuySupply"));
            this.initialSellSupply = Convert.parseLong(attachmentData.get("initialSellSupply"));
            this.expirationHeight = ((Long) attachmentData.get("expirationHeight")).intValue();
        }

        public MonetarySystemPublishExchangeOffer(long currencyId, long buyRateNQT, long sellRateNQT, long totalBuyLimit,
                                                  long totalSellLimit, long initialBuySupply, long initialSellSupply, int expirationHeight) {
            this.currencyId = currencyId;
            this.buyRateNQT = buyRateNQT;
            this.sellRateNQT = sellRateNQT;
            this.totalBuyLimit = totalBuyLimit;
            this.totalSellLimit = totalSellLimit;
            this.initialBuySupply = initialBuySupply;
            this.initialSellSupply = initialSellSupply;
            this.expirationHeight = expirationHeight;
        }

        @Override
        public int getMySize() {
            return 8 + 8 + 8 + 8 + 8 + 8 + 8 + 4;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(buyRateNQT);
            buffer.putLong(sellRateNQT);
            buffer.putLong(totalBuyLimit);
            buffer.putLong(totalSellLimit);
            buffer.putLong(initialBuySupply);
            buffer.putLong(initialSellSupply);
            buffer.putInt(expirationHeight);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("buyRateNQT", buyRateNQT);
            attachment.put("sellRateNQT", sellRateNQT);
            attachment.put("totalBuyLimit", totalBuyLimit);
            attachment.put("totalSellLimit", totalSellLimit);
            attachment.put("initialBuySupply", initialBuySupply);
            attachment.put("initialSellSupply", initialSellSupply);
            attachment.put("expirationHeight", expirationHeight);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetaryTx.PUBLISH_EXCHANGE_OFFER;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getBuyRateNQT() {
            return buyRateNQT;
        }

        public long getSellRateNQT() {
            return sellRateNQT;
        }

        public long getTotalBuyLimit() {
            return totalBuyLimit;
        }

        public long getTotalSellLimit() {
            return totalSellLimit;
        }

        public long getInitialBuySupply() {
            return initialBuySupply;
        }

        public long getInitialSellSupply() {
            return initialSellSupply;
        }

        public int getExpirationHeight() {
            return expirationHeight;
        }

    }

    abstract class MonetarySystemExchange extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;
        private final long rateNQT;
        private final long units;

        private MonetarySystemExchange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
            this.rateNQT = buffer.getLong();
            this.units = buffer.getLong();
        }

        private MonetarySystemExchange(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.rateNQT = Convert.parseLong(attachmentData.get("rateNQT"));
            this.units = Convert.parseLong(attachmentData.get("units"));
        }

        private MonetarySystemExchange(long currencyId, long rateNQT, long units) {
            this.currencyId = currencyId;
            this.rateNQT = rateNQT;
            this.units = units;
        }

        @Override
        public int getMySize() {
            return 8 + 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
            buffer.putLong(rateNQT);
            buffer.putLong(units);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("rateNQT", rateNQT);
            attachment.put("units", units);
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getRateNQT() {
            return rateNQT;
        }

        public long getUnits() {
            return units;
        }

    }

    final class MonetarySystemExchangeBuy extends MonetarySystemExchange {

        public MonetarySystemExchangeBuy(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        public MonetarySystemExchangeBuy(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeBuy(long currencyId, long rateNQT, long units) {
            super(currencyId, rateNQT, units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetaryTx.EXCHANGE_BUY;
        }

    }

    final class MonetarySystemExchangeSell extends MonetarySystemExchange {

        public MonetarySystemExchangeSell(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        public MonetarySystemExchangeSell(JSONObject attachmentData) {
            super(attachmentData);
        }

        public MonetarySystemExchangeSell(long currencyId, long rateNQT, long units) {
            super(currencyId, rateNQT, units);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetaryTx.EXCHANGE_SELL;
        }

    }

    final class MonetarySystemCurrencyMinting extends AbstractAttachment implements MonetarySystemAttachment {

        private final long nonce;
        private final long currencyId;
        private final long units;
        private final long counter;

        public MonetarySystemCurrencyMinting(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.nonce = buffer.getLong();
            this.currencyId = buffer.getLong();
            this.units = buffer.getLong();
            this.counter = buffer.getLong();
        }

        public MonetarySystemCurrencyMinting(JSONObject attachmentData) {
            super(attachmentData);
            this.nonce = Convert.parseLong(attachmentData.get("nonce"));
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
            this.units = Convert.parseLong(attachmentData.get("units"));
            this.counter = Convert.parseLong(attachmentData.get("counter"));
        }

        public MonetarySystemCurrencyMinting(long nonce, long currencyId, long units, long counter) {
            this.nonce = nonce;
            this.currencyId = currencyId;
            this.units = units;
            this.counter = counter;
        }

        @Override
        public int getMySize() {
            return 8 + 8 + 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(nonce);
            buffer.putLong(currencyId);
            buffer.putLong(units);
            buffer.putLong(counter);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("nonce", nonce);
            attachment.put("currency", Long.toUnsignedString(currencyId));
            attachment.put("units", units);
            attachment.put("counter", counter);
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetaryTx.CURRENCY_MINTING;
        }

        public long getNonce() {
            return nonce;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }

        public long getUnits() {
            return units;
        }

        public long getCounter() {
            return counter;
        }

    }

    final class MonetarySystemCurrencyDeletion extends AbstractAttachment implements MonetarySystemAttachment {

        private final long currencyId;

        public MonetarySystemCurrencyDeletion(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.currencyId = buffer.getLong();
        }

        public MonetarySystemCurrencyDeletion(JSONObject attachmentData) {
            super(attachmentData);
            this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        }

        public MonetarySystemCurrencyDeletion(long currencyId) {
            this.currencyId = currencyId;
        }

        @Override
        public int getMySize() {
            return 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(currencyId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("currency", Long.toUnsignedString(currencyId));
        }

        @Override
        public TransactionType getTransactionType() {
            return MonetaryTx.CURRENCY_DELETION;
        }

        @Override
        public long getCurrencyId() {
            return currencyId;
        }
    }

    final class ShufflingCreation extends AbstractAttachment {

        private final long holdingId;
        private final HoldingType holdingType;
        private final long amount;
        private final byte participantCount;
        private final short registrationPeriod;

        public ShufflingCreation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.holdingId = buffer.getLong();
            this.holdingType = HoldingType.get(buffer.get());
            this.amount = buffer.getLong();
            this.participantCount = buffer.get();
            this.registrationPeriod = buffer.getShort();
        }

        public ShufflingCreation(JSONObject attachmentData) {
            super(attachmentData);
            this.holdingId = Convert.parseUnsignedLong((String) attachmentData.get("holding"));
            this.holdingType = HoldingType.get(((Long) attachmentData.get("holdingType")).byteValue());
            this.amount = Convert.parseLong(attachmentData.get("amount"));
            this.participantCount = ((Long) attachmentData.get("participantCount")).byteValue();
            this.registrationPeriod = ((Long) attachmentData.get("registrationPeriod")).shortValue();
        }

        public ShufflingCreation(long holdingId, HoldingType holdingType, long amount, byte participantCount, short registrationPeriod) {
            this.holdingId = holdingId;
            this.holdingType = holdingType;
            this.amount = amount;
            this.participantCount = participantCount;
            this.registrationPeriod = registrationPeriod;
        }

        @Override
        public int getMySize() {
            return 8 + 1 + 8 + 1 + 2;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(holdingId);
            buffer.put(holdingType.getCode());
            buffer.putLong(amount);
            buffer.put(participantCount);
            buffer.putShort(registrationPeriod);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("holding", Long.toUnsignedString(holdingId));
            attachment.put("holdingType", holdingType.getCode());
            attachment.put("amount", amount);
            attachment.put("participantCount", participantCount);
            attachment.put("registrationPeriod", registrationPeriod);
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_CREATION;
        }

        public long getHoldingId() {
            return holdingId;
        }

        public HoldingType getHoldingType() {
            return holdingType;
        }

        public long getAmount() {
            return amount;
        }

        public byte getParticipantCount() {
            return participantCount;
        }

        public short getRegistrationPeriod() {
            return registrationPeriod;
        }
    }

    interface ShufflingAttachment extends Attachment {

        long getShufflingId();

        byte[] getShufflingStateHash();

    }

    abstract class AbstractShufflingAttachment extends AbstractAttachment implements ShufflingAttachment {

        private final long shufflingId;
        private final byte[] shufflingStateHash;

        private AbstractShufflingAttachment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.shufflingId = buffer.getLong();
            this.shufflingStateHash = new byte[32];
            buffer.get(this.shufflingStateHash);
        }

        private AbstractShufflingAttachment(JSONObject attachmentData) {
            super(attachmentData);
            this.shufflingId = Convert.parseUnsignedLong((String) attachmentData.get("shuffling"));
            this.shufflingStateHash = Convert.parseHexString((String) attachmentData.get("shufflingStateHash"));
        }

        private AbstractShufflingAttachment(long shufflingId, byte[] shufflingStateHash) {
            this.shufflingId = shufflingId;
            this.shufflingStateHash = shufflingStateHash;
        }

        @Override
        public int getMySize() {
            return 8 + 32;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(shufflingId);
            buffer.put(shufflingStateHash);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("shuffling", Long.toUnsignedString(shufflingId));
            attachment.put("shufflingStateHash", Convert.toHexString(shufflingStateHash));
        }

        @Override
        public final long getShufflingId() {
            return shufflingId;
        }

        @Override
        public final byte[] getShufflingStateHash() {
            return shufflingStateHash;
        }

    }

    final class ShufflingRegistration extends AbstractAttachment implements ShufflingAttachment {

        private final byte[] shufflingFullHash;

        public ShufflingRegistration(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.shufflingFullHash = new byte[32];
            buffer.get(this.shufflingFullHash);
        }

        public ShufflingRegistration(JSONObject attachmentData) {
            super(attachmentData);
            this.shufflingFullHash = Convert.parseHexString((String) attachmentData.get("shufflingFullHash"));
        }

        public ShufflingRegistration(byte[] shufflingFullHash) {
            this.shufflingFullHash = shufflingFullHash;
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_REGISTRATION;
        }

        @Override
        public int getMySize() {
            return 32;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.put(shufflingFullHash);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("shufflingFullHash", Convert.toHexString(shufflingFullHash));
        }

        @Override
        public long getShufflingId() {
            return Convert.fullHashToId(shufflingFullHash);
        }

        @Override
        public byte[] getShufflingStateHash() {
            return shufflingFullHash;
        }

    }

    final class ShufflingProcessing extends AbstractShufflingAttachment implements Prunable {

        private static final byte[] emptyDataHash = Crypto.sha256().digest();

        public static ShufflingProcessing parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(ShufflingTransaction.SHUFFLING_PROCESSING.getName(), attachmentData)) {
                return null;
            }
            return new ShufflingProcessing(attachmentData);
        }

        private volatile byte[][] data;
        private final byte[] hash;

        public ShufflingProcessing(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.hash = new byte[32];
            buffer.get(hash);
            this.data = Arrays.equals(hash, emptyDataHash) ? Convert.EMPTY_BYTES : null;
        }

        public ShufflingProcessing(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray jsonArray = (JSONArray) attachmentData.get("data");
            if (jsonArray != null) {
                this.data = new byte[jsonArray.size()][];
                for (int i = 0; i < this.data.length; i++) {
                    this.data[i] = Convert.parseHexString((String) jsonArray.get(i));
                }
                this.hash = null;
            } else {
                this.hash = Convert.parseHexString(Convert.emptyToNull((String) attachmentData.get("hash")));
                this.data = Arrays.equals(hash, emptyDataHash) ? Convert.EMPTY_BYTES : null;
            }
        }

        public ShufflingProcessing(long shufflingId, byte[][] data, byte[] shufflingStateHash) {
            super(shufflingId, shufflingStateHash);
            this.data = data;
            this.hash = null;
        }

        @Override
        public int getMyFullSize() {
            int size = super.getMySize();
            if (data != null) {
                size += 1;
                for (byte[] bytes : data) {
                    size += 4;
                    size += bytes.length;
                }
            }
            return size / 2; // just lie
        }

        @Override
        public int getMySize() {
            return super.getMySize() + 32;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.put(getHash());
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            if (data != null) {
                JSONArray jsonArray = new JSONArray();
                attachment.put("data", jsonArray);
                for (byte[] bytes : data) {
                    jsonArray.add(Convert.toHexString(bytes));
                }
            }
            attachment.put("hash", Convert.toHexString(getHash()));
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_PROCESSING;
        }

        @Override
        public byte[] getHash() {
            if (hash != null) {
                return hash;
            } else if (data != null) {
                MessageDigest digest = Crypto.sha256();
                for (byte[] bytes : data) {
                    digest.update(bytes);
                }
                return digest.digest();
            } else {
                throw new IllegalStateException("Both hash and data are null");
            }
        }

        public byte[][] getData() {
            return data;
        }

        @Override
        public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
            if (data == null && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
                data = ShufflingParticipant.getData(getShufflingId(), transaction.getSenderId());
            }
        }

        @Override
        public boolean hasPrunableData() {
            return data != null;
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
            ShufflingParticipant.restoreData(getShufflingId(), transaction.getSenderId(), getData(), transaction.getTimestamp(), height);
        }

    }

    final class ShufflingRecipients extends AbstractShufflingAttachment {

        private final byte[][] recipientPublicKeys;

        public ShufflingRecipients(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            int count = buffer.get();
            if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count < 0) {
                throw new ConchException.NotValidException("Invalid data count " + count);
            }
            this.recipientPublicKeys = new byte[count][];
            for (int i = 0; i < count; i++) {
                this.recipientPublicKeys[i] = new byte[32];
                buffer.get(this.recipientPublicKeys[i]);
            }
        }

        public ShufflingRecipients(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray jsonArray = (JSONArray) attachmentData.get("recipientPublicKeys");
            this.recipientPublicKeys = new byte[jsonArray.size()][];
            for (int i = 0; i < this.recipientPublicKeys.length; i++) {
                this.recipientPublicKeys[i] = Convert.parseHexString((String) jsonArray.get(i));
            }
        }

        public ShufflingRecipients(long shufflingId, byte[][] recipientPublicKeys, byte[] shufflingStateHash) {
            super(shufflingId, shufflingStateHash);
            this.recipientPublicKeys = recipientPublicKeys;
        }

        @Override
        public int getMySize() {
            int size = super.getMySize();
            size += 1;
            size += 32 * recipientPublicKeys.length;
            return size;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.put((byte) recipientPublicKeys.length);
            for (byte[] bytes : recipientPublicKeys) {
                buffer.put(bytes);
            }
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            JSONArray jsonArray = new JSONArray();
            attachment.put("recipientPublicKeys", jsonArray);
            for (byte[] bytes : recipientPublicKeys) {
                jsonArray.add(Convert.toHexString(bytes));
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_RECIPIENTS;
        }

        public byte[][] getRecipientPublicKeys() {
            return recipientPublicKeys;
        }

    }

    final class ShufflingVerification extends AbstractShufflingAttachment {

        public ShufflingVerification(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        public ShufflingVerification(JSONObject attachmentData) {
            super(attachmentData);
        }

        public ShufflingVerification(long shufflingId, byte[] shufflingStateHash) {
            super(shufflingId, shufflingStateHash);
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_VERIFICATION;
        }

    }

    final class ShufflingCancellation extends AbstractShufflingAttachment {

        private final byte[][] blameData;
        private final byte[][] keySeeds;
        private final long cancellingAccountId;

        public ShufflingCancellation(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            int count = buffer.get();
            if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count <= 0) {
                throw new ConchException.NotValidException("Invalid data count " + count);
            }
            this.blameData = new byte[count][];
            for (int i = 0; i < count; i++) {
                int size = buffer.getInt();
                if (size > Constants.MAX_PAYLOAD_LENGTH) {
                    throw new ConchException.NotValidException("Invalid data size " + size);
                }
                this.blameData[i] = new byte[size];
                buffer.get(this.blameData[i]);
            }
            count = buffer.get();
            if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count <= 0) {
                throw new ConchException.NotValidException("Invalid keySeeds count " + count);
            }
            this.keySeeds = new byte[count][];
            for (int i = 0; i < count; i++) {
                this.keySeeds[i] = new byte[32];
                buffer.get(this.keySeeds[i]);
            }
            this.cancellingAccountId = buffer.getLong();
        }

        public ShufflingCancellation(JSONObject attachmentData) {
            super(attachmentData);
            JSONArray jsonArray = (JSONArray) attachmentData.get("blameData");
            this.blameData = new byte[jsonArray.size()][];
            for (int i = 0; i < this.blameData.length; i++) {
                this.blameData[i] = Convert.parseHexString((String) jsonArray.get(i));
            }
            jsonArray = (JSONArray) attachmentData.get("keySeeds");
            this.keySeeds = new byte[jsonArray.size()][];
            for (int i = 0; i < this.keySeeds.length; i++) {
                this.keySeeds[i] = Convert.parseHexString((String) jsonArray.get(i));
            }
            this.cancellingAccountId = Convert.parseUnsignedLong((String) attachmentData.get("cancellingAccount"));
        }

        public ShufflingCancellation(long shufflingId, byte[][] blameData, byte[][] keySeeds, byte[] shufflingStateHash, long cancellingAccountId) {
            super(shufflingId, shufflingStateHash);
            this.blameData = blameData;
            this.keySeeds = keySeeds;
            this.cancellingAccountId = cancellingAccountId;
        }

        @Override
        public TransactionType getTransactionType() {
            return ShufflingTransaction.SHUFFLING_CANCELLATION;
        }

        @Override
        public int getMySize() {
            int size = super.getMySize();
            size += 1;
            for (byte[] bytes : blameData) {
                size += 4;
                size += bytes.length;
            }
            size += 1;
            size += 32 * keySeeds.length;
            size += 8;
            return size;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            super.putMyBytes(buffer);
            buffer.put((byte) blameData.length);
            for (byte[] bytes : blameData) {
                buffer.putInt(bytes.length);
                buffer.put(bytes);
            }
            buffer.put((byte) keySeeds.length);
            for (byte[] bytes : keySeeds) {
                buffer.put(bytes);
            }
            buffer.putLong(cancellingAccountId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            JSONArray jsonArray = new JSONArray();
            attachment.put("blameData", jsonArray);
            for (byte[] bytes : blameData) {
                jsonArray.add(Convert.toHexString(bytes));
            }
            jsonArray = new JSONArray();
            attachment.put("keySeeds", jsonArray);
            for (byte[] bytes : keySeeds) {
                jsonArray.add(Convert.toHexString(bytes));
            }
            if (cancellingAccountId != 0) {
                attachment.put("cancellingAccount", Long.toUnsignedString(cancellingAccountId));
            }
        }

        public byte[][] getBlameData() {
            return blameData;
        }

        public byte[][] getKeySeeds() {
            return keySeeds;
        }

        public long getCancellingAccountId() {
            return cancellingAccountId;
        }

        public byte[] getHash() {
            MessageDigest digest = Crypto.sha256();
            for (byte[] bytes : blameData) {
                digest.update(bytes);
            }
            return digest.digest();
        }

    }

    abstract class TaggedDataAttachment extends AbstractAttachment implements Prunable {

        private final String name;
        private final String description;
        private final String tags;
        private final String type;
        private final String channel;
        private final boolean isText;
        private final String filename;
        private final byte[] data;
        private volatile TaggedData taggedData;

        private TaggedDataAttachment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.name = null;
            this.description = null;
            this.tags = null;
            this.type = null;
            this.channel = null;
            this.isText = false;
            this.filename = null;
            this.data = null;
        }

        private TaggedDataAttachment(JSONObject attachmentData) {
            super(attachmentData);
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON != null) {
                this.name = (String) attachmentData.get("name");
                this.description = (String) attachmentData.get("description");
                this.tags = (String) attachmentData.get("tags");
                this.type = (String) attachmentData.get("type");
                this.channel = Convert.nullToEmpty((String) attachmentData.get("channel"));
                this.isText = Boolean.TRUE.equals(attachmentData.get("isText"));
                this.data = isText ? Convert.toBytes(dataJSON) : Convert.parseHexString(dataJSON);
                this.filename = (String) attachmentData.get("filename");
            } else {
                this.name = null;
                this.description = null;
                this.tags = null;
                this.type = null;
                this.channel = null;
                this.isText = false;
                this.filename = null;
                this.data = null;
            }

        }

        private TaggedDataAttachment(String name, String description, String tags, String type, String channel, boolean isText, String filename, byte[] data) {
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.type = type;
            this.channel = channel;
            this.isText = isText;
            this.data = data;
            this.filename = filename;
        }

        @Override
        public final int getMyFullSize() {
            if (getData() == null) {
                return 0;
            }
            return Convert.toBytes(getName()).length + Convert.toBytes(getDescription()).length + Convert.toBytes(getType()).length
                    + Convert.toBytes(getChannel()).length + Convert.toBytes(getTags()).length + Convert.toBytes(getFilename()).length + getData().length;
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            if (taggedData != null) {
                attachment.put("name", taggedData.getName());
                attachment.put("description", taggedData.getDescription());
                attachment.put("tags", taggedData.getTags());
                attachment.put("type", taggedData.getType());
                attachment.put("channel", taggedData.getChannel());
                attachment.put("isText", taggedData.isText());
                attachment.put("filename", taggedData.getFilename());
                attachment.put("data", taggedData.isText() ? Convert.toString(taggedData.getData()) : Convert.toHexString(taggedData.getData()));
            } else if (data != null) {
                attachment.put("name", name);
                attachment.put("description", description);
                attachment.put("tags", tags);
                attachment.put("type", type);
                attachment.put("channel", channel);
                attachment.put("isText", isText);
                attachment.put("filename", filename);
                attachment.put("data", isText ? Convert.toString(data) : Convert.toHexString(data));
            }
        }

        @Override
        public byte[] getHash() {
            if (data == null) {
                return null;
            }
            MessageDigest digest = Crypto.sha256();
            digest.update(Convert.toBytes(name));
            digest.update(Convert.toBytes(description));
            digest.update(Convert.toBytes(tags));
            digest.update(Convert.toBytes(type));
            digest.update(Convert.toBytes(channel));
            digest.update((byte) (isText ? 1 : 0));
            digest.update(Convert.toBytes(filename));
            digest.update(data);
            return digest.digest();
        }

        public final String getName() {
            if (taggedData != null) {
                return taggedData.getName();
            }
            return name;
        }

        public final String getDescription() {
            if (taggedData != null) {
                return taggedData.getDescription();
            }
            return description;
        }

        public final String getTags() {
            if (taggedData != null) {
                return taggedData.getTags();
            }
            return tags;
        }

        public final String getType() {
            if (taggedData != null) {
                return taggedData.getType();
            }
            return type;
        }

        public final String getChannel() {
            if (taggedData != null) {
                return taggedData.getChannel();
            }
            return channel;
        }

        public final boolean isText() {
            if (taggedData != null) {
                return taggedData.isText();
            }
            return isText;
        }

        public final String getFilename() {
            if (taggedData != null) {
                return taggedData.getFilename();
            }
            return filename;
        }

        public final byte[] getData() {
            if (taggedData != null) {
                return taggedData.getData();
            }
            return data;
        }

        @Override
        public void loadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
            if (data == null && taggedData == null && shouldLoadPrunable(transaction, includeExpiredPrunable)) {
                taggedData = TaggedData.getData(getTaggedDataId(transaction));
            }
        }

        @Override
        public boolean hasPrunableData() {
            return (taggedData != null || data != null);
        }

        abstract long getTaggedDataId(Transaction transaction);

    }

    final class TaggedDataUpload extends TaggedDataAttachment {

        public static TaggedDataUpload parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(TransactionType.Data.TAGGED_DATA_UPLOAD.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataUpload(attachmentData);
        }

        private final byte[] hash;

        public TaggedDataUpload(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.hash = new byte[32];
            buffer.get(hash);
        }

        public TaggedDataUpload(JSONObject attachmentData) {
            super(attachmentData);
            String dataJSON = (String) attachmentData.get("data");
            if (dataJSON == null) {
                this.hash = Convert.parseHexString(Convert.emptyToNull((String) attachmentData.get("hash")));
            } else {
                this.hash = null;
            }
        }

        public TaggedDataUpload(String name, String description, String tags, String type, String channel, boolean isText,
                                String filename, byte[] data) throws ConchException.NotValidException {
            super(name, description, tags, type, channel, isText, filename, data);
            this.hash = null;
            if (isText && !Arrays.equals(data, Convert.toBytes(Convert.toString(data)))) {
                throw new ConchException.NotValidException("Data is not UTF-8 text");
            }
        }

        @Override
        public int getMySize() {
            return 32;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.put(getHash());
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("hash", Convert.toHexString(getHash()));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Data.TAGGED_DATA_UPLOAD;
        }

        @Override
        public byte[] getHash() {
            if (hash != null) {
                return hash;
            }
            return super.getHash();
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return transaction.getId();
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
            TaggedData.restore(transaction, this, blockTimestamp, height);
        }

    }

    final class TaggedDataExtend extends TaggedDataAttachment {

        public static TaggedDataExtend parse(JSONObject attachmentData) {
            if (!Appendix.hasAppendix(TransactionType.Data.TAGGED_DATA_EXTEND.getName(), attachmentData)) {
                return null;
            }
            return new TaggedDataExtend(attachmentData);
        }

        private volatile byte[] hash;
        private final long taggedDataId;
        private final boolean jsonIsPruned;

        public TaggedDataExtend(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.taggedDataId = buffer.getLong();
            this.jsonIsPruned = false;
        }

        public TaggedDataExtend(JSONObject attachmentData) {
            super(attachmentData);
            this.taggedDataId = Convert.parseUnsignedLong((String) attachmentData.get("taggedData"));
            this.jsonIsPruned = attachmentData.get("data") == null;
        }

        public TaggedDataExtend(TaggedData taggedData) {
            super(taggedData.getName(), taggedData.getDescription(), taggedData.getTags(), taggedData.getType(),
                    taggedData.getChannel(), taggedData.isText(), taggedData.getFilename(), taggedData.getData());
            this.taggedDataId = taggedData.getId();
            this.jsonIsPruned = false;
        }

        @Override
        public int getMySize() {
            return 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(taggedDataId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            super.putMyJSON(attachment);
            attachment.put("taggedData", Long.toUnsignedString(taggedDataId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Data.TAGGED_DATA_EXTEND;
        }

        public long getTaggedDataId() {
            return taggedDataId;
        }

        @Override
        public byte[] getHash() {
            if (hash == null) {
                hash = super.getHash();
            }
            if (hash == null) {
                TaggedDataUpload taggedDataUpload = (TaggedDataUpload) TransactionDb.findTransaction(taggedDataId).getAttachment();
                hash = taggedDataUpload.getHash();
            }
            return hash;
        }

        @Override
        long getTaggedDataId(Transaction transaction) {
            return taggedDataId;
        }

        public boolean jsonIsPruned() {
            return jsonIsPruned;
        }

        @Override
        public void restorePrunableData(Transaction transaction, int blockTimestamp, int height) {
        }

    }

    final class SetPhasingOnly extends AbstractAttachment {

        private final PhasingParams phasingParams;
        private final long maxFees;
        private final short minDuration;
        private final short maxDuration;

        public SetPhasingOnly(PhasingParams params, long maxFees, short minDuration, short maxDuration) {
            phasingParams = params;
            this.maxFees = maxFees;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
        }

        public SetPhasingOnly(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            phasingParams = new PhasingParams(buffer);
            maxFees = buffer.getLong();
            minDuration = buffer.getShort();
            maxDuration = buffer.getShort();
        }

        public SetPhasingOnly(JSONObject attachmentData) {
            super(attachmentData);
            JSONObject phasingControlParams = (JSONObject) attachmentData.get("phasingControlParams");
            phasingParams = new PhasingParams(phasingControlParams);
            maxFees = Convert.parseLong(attachmentData.get("controlMaxFees"));
            minDuration = ((Long) attachmentData.get("controlMinDuration")).shortValue();
            maxDuration = ((Long) attachmentData.get("controlMaxDuration")).shortValue();
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AccountControl.SET_PHASING_ONLY;
        }

        @Override
        public int getMySize() {
            return phasingParams.getMySize() + 8 + 2 + 2;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            phasingParams.putMyBytes(buffer);
            buffer.putLong(maxFees);
            buffer.putShort(minDuration);
            buffer.putShort(maxDuration);
        }

        @Override
        public void putMyJSON(JSONObject json) {
            JSONObject phasingControlParams = new JSONObject();
            phasingParams.putMyJSON(phasingControlParams);
            json.put("phasingControlParams", phasingControlParams);
            json.put("controlMaxFees", maxFees);
            json.put("controlMinDuration", minDuration);
            json.put("controlMaxDuration", maxDuration);
        }

        public PhasingParams getPhasingParams() {
            return phasingParams;
        }

        public long getMaxFees() {
            return maxFees;
        }

        public short getMinDuration() {
            return minDuration;
        }

        public short getMaxDuration() {
            return maxDuration;
        }

    }

    final class SharderPoolCreate extends AbstractAttachment {

        private final int period;
        private final Map<String, Object> rule;

        public SharderPoolCreate(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.period = Short.toUnsignedInt(buffer.getShort());
            Map<String, Object> map = null;
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.remaining());
                byteBuffer.put(buffer);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(byteBuffer.array()));
                map = (Map<String, Object>) ois.readObject();
                ois.close();
            } catch (Exception e) {
                Logger.logErrorMessage("sharder pool create transaction can't load rule from byte", e);
            }
            this.rule = map;
        }

        public SharderPoolCreate(JSONObject attachmentData) {
            super(attachmentData);
            this.period = ((Long) attachmentData.get("period")).intValue();
            this.rule = PoolRule.jsonObjectToMap((JSONObject) attachmentData.get("rule"));
        }

        public SharderPoolCreate(int period, Map<String, Object> rule) {
            this.period = period;
            this.rule = rule;
        }

        @Override
        public int getMySize() {
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream os = new ObjectOutputStream(bo);
                os.writeObject(rule);
                os.close();
                return 2 + bo.toByteArray().length;
            } catch (Exception e) {
                Logger.logDebugMessage("rule can't turn to byte in sharder pool create", e);
            }
            return 2 + (int) ObjectSizeCalculator.getObjectSize(rule);
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putShort((short) period);
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream os = new ObjectOutputStream(bo);
                os.writeObject(rule);
                os.close();
                buffer.put(ByteBuffer.wrap(bo.toByteArray()));
            } catch (Exception e) {
                Logger.logDebugMessage("rule can't turn to byte in sharder pool create", e);
            }
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("period", period);
            attachment.put("rule", rule);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.SharderPool.SHARDER_POOL_CREATE;
        }

        public int getPeriod() {
            return period;
        }

        public Map<String, Object> getRule() {
            return rule;
        }
    }

    final class SharderPoolDestroy extends AbstractAttachment {
        private final long poolId;

        public SharderPoolDestroy(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.poolId = buffer.getLong();
        }

        public SharderPoolDestroy(JSONObject attachmentData) {
            super(attachmentData);
            this.poolId = (Long) attachmentData.get("poolId");
        }

        public SharderPoolDestroy(long poolId) {
            this.poolId = poolId;
        }

        @Override
        public int getMySize() {
            return 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(poolId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("poolId", poolId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.SharderPool.SHARDER_POOL_DESTROY;
        }

        public long getPoolId() {
            return poolId;
        }
    }

    final class SharderPoolJoin extends AbstractAttachment {

        private final long poolId;
        private final long amount;
        private final int period;

        public SharderPoolJoin(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.poolId = buffer.getLong();
            this.amount = buffer.getLong();
            this.period = Short.toUnsignedInt(buffer.getShort());
        }

        public SharderPoolJoin(JSONObject attachmentData) {
            super(attachmentData);
            this.poolId = (Long) attachmentData.get("poolId");
            this.amount = (Long) attachmentData.get("amount");
            this.period = ((Long) attachmentData.get("period")).intValue();
        }

        public SharderPoolJoin(long poolId, long amount, int period) {
            this.poolId = poolId;
            this.amount = amount;
            this.period = period;
        }

        @Override
        public int getMySize() {
            return 18;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(poolId);
            buffer.putLong(amount);
            buffer.putShort((short) period);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("poolId", poolId);
            attachment.put("amount", amount);
            attachment.put("period", period);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.SharderPool.SHARDER_POOL_JOIN;
        }

        public int getPeriod() {
            return period;
        }

        public long getPoolId() {
            return poolId;
        }

        public long getAmount() {
            return amount;
        }
    }

    final class SharderPoolQuit extends AbstractAttachment {
        private final long txId;
        private final long poolId;

        public SharderPoolQuit(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.txId = buffer.getLong();
            this.poolId = buffer.getLong();
        }

        public SharderPoolQuit(JSONObject attachmentData) {
            super(attachmentData);
            this.txId = (Long) attachmentData.get("txId");
            this.poolId = (Long) attachmentData.get("poolId");
        }

        public SharderPoolQuit(long txId, long poolId) {
            this.txId = txId;
            this.poolId = poolId;
        }

        @Override
        public int getMySize() {
            return 16;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(txId);
            buffer.putLong(poolId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("txId", txId);
            attachment.put("poolId", poolId);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.SharderPool.SHARDER_POOL_QUIT;
        }

        public long getTxId() {
            return txId;
        }

        public long getPoolId() {
            return poolId;
        }
    }

    final class DataStorageUpload extends AbstractAttachment {

        private final String name;
        private final String description;
        private final String type;
        private final String ssid;
        private final String channel;
        private final int existence_height;
        private final int replicated_number;
//        private final String filename;

        public DataStorageUpload(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_TAGGED_DATA_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH);
            this.type = Convert.readString(buffer, buffer.get(), Constants.MAX_TAGGED_DATA_TYPE_LENGTH);
            this.ssid = Convert.readString(buffer, buffer.get(), 200);
            this.channel = Convert.readString(buffer, buffer.get(), Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH);
            this.existence_height = buffer.getInt();
            this.replicated_number = buffer.getInt();
        }

        public DataStorageUpload(JSONObject attachmentData) {
            super(attachmentData);
            this.name = (String) attachmentData.get("name");
            this.description = (String) attachmentData.get("description");
            this.type = (String) attachmentData.get("type");
            this.ssid = (String) attachmentData.get("ssid");
            this.channel = (String) attachmentData.get("channel");
            this.existence_height = ((Long) attachmentData.get("existence_height")).intValue();
            this.replicated_number = ((Long) attachmentData.get("replicated_number")).intValue();
        }

        public DataStorageUpload(String name, String description, String type, String ssid, String channel, int existence_height, int replicated_number) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.ssid = ssid;
            this.channel = channel;
            this.existence_height = existence_height;
            this.replicated_number = replicated_number;
        }

        @Override
        public int getMySize() {
            return 1 + Convert.toBytes(name).length +
                    2 + Convert.toBytes(description).length +
                    1 + Convert.toBytes(type).length +
                    1 + Convert.toBytes(ssid).length +
                    1 + Convert.toBytes(channel).length +
                    4 +
                    4;
//                    4 + Convert.toBytes(channel).length +
//                    5 + Convert.toBytes(filename).length;

        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            byte[] type = Convert.toBytes(this.type);
            byte[] ssid = Convert.toBytes(this.ssid);
            byte[] channel = Convert.toBytes(this.channel);
            buffer.put((byte) name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.put((byte) type.length);
            buffer.put(type);
            buffer.put((byte) ssid.length);
            buffer.put(ssid);
            buffer.put((byte) channel.length);
            buffer.put(channel);
            buffer.putInt(existence_height);
            buffer.putInt(replicated_number);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("name", name);
            attachment.put("description", description);
            attachment.put("type", type);
            attachment.put("ssid", ssid);
            attachment.put("channel", channel);
            attachment.put("existence_height", existence_height);
            attachment.put("replicated_number", replicated_number);
        }

        @Override
        public TransactionType getTransactionType() {
            return StorageTx.STORAGE_UPLOAD;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getSsid() {
            return ssid;
        }

        public String getChannel() {
            return channel;
        }

        public int getExistence_height() {
            return existence_height;
        }

        public int getReplicated_number() {
            return replicated_number;
        }
    }

    final class DataStorageBackup extends AbstractAttachment {

        private final long uploadTransaction;
        private final long storerId;

        public DataStorageBackup(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            super(buffer, transactionVersion);
            this.uploadTransaction = buffer.getLong();
            this.storerId = buffer.getLong();
        }

        public DataStorageBackup(JSONObject attachmentData) {
            super(attachmentData);
            this.uploadTransaction = (Long) attachmentData.get("uploadTransaction");
            this.storerId = (Long) attachmentData.get("storerId");

        }

        public DataStorageBackup(Long uploadTransaction, Long storerId) {
            this.uploadTransaction = uploadTransaction;
            this.storerId = storerId;
        }

        @Override
        public int getMySize() {
            return 8 + 8;
        }

        @Override
        public void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(this.uploadTransaction);
            buffer.putLong(this.storerId);
        }

        @Override
        public void putMyJSON(JSONObject attachment) {
            attachment.put("uploadTransaction", uploadTransaction);
            attachment.put("storerId", storerId);
        }

        @Override
        public TransactionType getTransactionType() {
            return StorageTx.STORAGE_BACKUP;
        }

        public long getUploadTransaction() {
            return uploadTransaction;
        }

        public long getStorerId() {
            return storerId;
        }
    }

    public static void main(String[] args) {

    }
}
