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

import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.Account.ControlType;
import org.conch.account.AccountLedger;
import org.conch.account.AccountRestrictions;
import org.conch.account.Alias;
import org.conch.asset.Asset;
import org.conch.asset.AssetDividend;
import org.conch.asset.AssetTransfer;
import org.conch.asset.MonetaryTx;
import org.conch.chain.CheckSumValidator;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.market.DigitalGoodsStore;
import org.conch.market.Order;
import org.conch.mint.Hub;
import org.conch.mint.pool.PoolRule;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.shuffle.ShufflingTransaction;
import org.conch.storage.TaggedData;
import org.conch.storage.tx.StorageTx;
import org.conch.tx.Attachment.AbstractAttachment;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.conch.vote.*;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class TransactionType {

    private static final byte TYPE_PAYMENT = 0;
    private static final byte TYPE_MESSAGING = 1;
    private static final byte TYPE_COLORED_COINS = 2;
    private static final byte TYPE_DIGITAL_GOODS = 3;
    private static final byte TYPE_ACCOUNT_CONTROL = 4;
    public static final byte TYPE_MONETARY_SYSTEM = 5;
    public static final byte TYPE_DATA = 6;
    protected static final byte TYPE_SHUFFLING = 7;
    public static final byte TYPE_SHARDER_POOL = 8;
    public static final byte TYPE_COIN_BASE = 9;
    public static final byte TYPE_CONTRACT = 10;
    public static final byte TYPE_STORAGE = 11;
    public static final byte TYPE_POC = 12;

    private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;

    private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    private static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    private static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;
    private static final byte SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT = 4;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_INFO = 5;
    private static final byte SUBTYPE_MESSAGING_ALIAS_SELL = 6;
    private static final byte SUBTYPE_MESSAGING_ALIAS_BUY = 7;
    private static final byte SUBTYPE_MESSAGING_ALIAS_DELETE = 8;
    private static final byte SUBTYPE_MESSAGING_PHASING_VOTE_CASTING = 9;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY = 10;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE = 11;

    private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
    private static final byte SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT = 6;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_DELETE = 7;

    private static final byte SUBTYPE_DIGITAL_GOODS_LISTING = 0;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELISTING = 1;
    private static final byte SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE = 2;
    private static final byte SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE = 3;
    private static final byte SUBTYPE_DIGITAL_GOODS_PURCHASE = 4;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELIVERY = 5;
    private static final byte SUBTYPE_DIGITAL_GOODS_FEEDBACK = 6;
    private static final byte SUBTYPE_DIGITAL_GOODS_REFUND = 7;

    private static final byte SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING = 0;
    private static final byte SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY = 1;

    private static final byte SUBTYPE_DATA_TAGGED_DATA_UPLOAD = 0;
    private static final byte SUBTYPE_DATA_TAGGED_DATA_EXTEND = 1;

    // sharder-pool
    private static final byte SUBTYPE_SHARDER_POOL_CREATE = 0;
    private static final byte SUBTYPE_SHARDER_POOL_DESTROY = 1;
    private static final byte SUBTYPE_SHARDER_POOL_JOIN = 2;
    private static final byte SUBTYPE_SHARDER_POOL_QUIT = 3;

    private static final byte SUBTYPE_COIN_BASE_ORDINARY = 0;

    private static final byte SUBTYPE_CONTRACT_ORDINARY = 0;


    public static TransactionType findTransactionType(byte type, byte subtype) {
        switch (type) {
            case TYPE_PAYMENT:
                switch (subtype) {
                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        return Payment.ORDINARY;
                    default:
                        return null;
                }
            case TYPE_MESSAGING:
                switch (subtype) {
                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                        return Messaging.ARBITRARY_MESSAGE;
                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                        return Messaging.ALIAS_ASSIGNMENT;
                    case SUBTYPE_MESSAGING_POLL_CREATION:
                        return Messaging.POLL_CREATION;
                    case SUBTYPE_MESSAGING_VOTE_CASTING:
                        return Messaging.VOTE_CASTING;
                    case SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT:
                        return Messaging.HUB_ANNOUNCEMENT;
                    case SUBTYPE_MESSAGING_ACCOUNT_INFO:
                        return Messaging.ACCOUNT_INFO;
                    case SUBTYPE_MESSAGING_ALIAS_SELL:
                        return Messaging.ALIAS_SELL;
                    case SUBTYPE_MESSAGING_ALIAS_BUY:
                        return Messaging.ALIAS_BUY;
                    case SUBTYPE_MESSAGING_ALIAS_DELETE:
                        return Messaging.ALIAS_DELETE;
                    case SUBTYPE_MESSAGING_PHASING_VOTE_CASTING:
                        return Messaging.PHASING_VOTE_CASTING;
                    case SUBTYPE_MESSAGING_ACCOUNT_PROPERTY:
                        return Messaging.ACCOUNT_PROPERTY;
                    case SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE:
                        return Messaging.ACCOUNT_PROPERTY_DELETE;
                    default:
                        return null;
                }
            case TYPE_COLORED_COINS:
                switch (subtype) {
                    case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                        return ColoredCoins.ASSET_ISSUANCE;
                    case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                        return ColoredCoins.ASSET_TRANSFER;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                        return ColoredCoins.ASK_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                        return ColoredCoins.BID_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                        return ColoredCoins.ASK_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                        return ColoredCoins.BID_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT:
                        return ColoredCoins.DIVIDEND_PAYMENT;
                    case SUBTYPE_COLORED_COINS_ASSET_DELETE:
                        return ColoredCoins.ASSET_DELETE;
                    default:
                        return null;
                }
            case TYPE_DIGITAL_GOODS:
                switch (subtype) {
                    case SUBTYPE_DIGITAL_GOODS_LISTING:
                        return DigitalGoods.LISTING;
                    case SUBTYPE_DIGITAL_GOODS_DELISTING:
                        return DigitalGoods.DELISTING;
                    case SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE:
                        return DigitalGoods.PRICE_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE:
                        return DigitalGoods.QUANTITY_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_PURCHASE:
                        return DigitalGoods.PURCHASE;
                    case SUBTYPE_DIGITAL_GOODS_DELIVERY:
                        return DigitalGoods.DELIVERY;
                    case SUBTYPE_DIGITAL_GOODS_FEEDBACK:
                        return DigitalGoods.FEEDBACK;
                    case SUBTYPE_DIGITAL_GOODS_REFUND:
                        return DigitalGoods.REFUND;
                    default:
                        return null;
                }
            case TYPE_ACCOUNT_CONTROL:
                switch (subtype) {
                    case SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING:
                        return TransactionType.AccountControl.EFFECTIVE_BALANCE_LEASING;
                    case SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY:
                        return TransactionType.AccountControl.SET_PHASING_ONLY;
                    default:
                        return null;
                }
            case TYPE_MONETARY_SYSTEM:
                return MonetaryTx.findTransactionType(subtype);
            case TYPE_DATA:
                switch (subtype) {
                    case SUBTYPE_DATA_TAGGED_DATA_UPLOAD:
                        return Data.TAGGED_DATA_UPLOAD;
                    case SUBTYPE_DATA_TAGGED_DATA_EXTEND:
                        return Data.TAGGED_DATA_EXTEND;
                    default:
                        return null;
                }
            case TYPE_SHUFFLING:
                return ShufflingTransaction.findTransactionType(subtype);
            case TYPE_SHARDER_POOL:
                switch (subtype) {
                    case SUBTYPE_SHARDER_POOL_CREATE:
                        return SharderPool.SHARDER_POOL_CREATE;
                    case SUBTYPE_SHARDER_POOL_DESTROY:
                        return SharderPool.SHARDER_POOL_DESTROY;
                    case SUBTYPE_SHARDER_POOL_JOIN:
                        return SharderPool.SHARDER_POOL_JOIN;
                    case SUBTYPE_SHARDER_POOL_QUIT:
                        return SharderPool.SHARDER_POOL_QUIT;
                    default:
                        return null;
                }
            case TYPE_COIN_BASE:
                switch (subtype) {
                    case SUBTYPE_COIN_BASE_ORDINARY:
                        return CoinBase.ORDINARY;
                    default:
                        return null;
                }
            case TYPE_CONTRACT:
                switch (subtype) {
                    case SUBTYPE_CONTRACT_ORDINARY:
                        return CoinBase.ORDINARY;
                    default:
                        return null;
                }
            case TYPE_STORAGE:
                return StorageTx.findTxType(subtype);
            case TYPE_POC:
                return PocTxWrapper.findTxType(subtype);
            default:
                return null;
        }
    }


    protected TransactionType() {
    }

    public abstract byte getType();

    public abstract byte getSubtype();

    public abstract AccountLedger.LedgerEvent getLedgerEvent();

    public abstract AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException;

    public abstract Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException;

    public abstract void validateAttachment(Transaction transaction) throws ConchException.ValidationException;

    // return false if double spending
    public final boolean applyUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        long amountNQT = transaction.getAmountNQT();
        long feeNQT = transaction.getFeeNQT();
        if (transaction.getType().getType() != TYPE_COIN_BASE) {
            if (transaction.referencedTransactionFullHash() != null
                    && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
                feeNQT = Math.addExact(feeNQT, Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
            }
            long totalAmountNQT = Math.addExact(amountNQT, feeNQT);
            if (senderAccount.getUnconfirmedBalanceNQT() < totalAmountNQT
                    && !(transaction.getTimestamp() == 0 && Arrays.equals(transaction.getSenderPublicKey(), SharderGenesis.CREATOR_PUBLIC_KEY))) {
                return false;
            }
            senderAccount.addUnconfirmed(getLedgerEvent(), transaction.getId(), -amountNQT, -feeNQT);
        }

        if (!applyAttachmentUnconfirmed(transaction, senderAccount)) {
            senderAccount.addUnconfirmed(getLedgerEvent(), transaction.getId(), amountNQT, feeNQT);
            return false;
        }
        return true;
    }

    public abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    public final void apply(TransactionImpl transaction, Account senderAccount, Account recipientAccount) {
        //transfer processing except coinbase tx (org.conch.tx.TransactionType.CoinBase#applyByType)
        if (transaction.getType().getType() != TYPE_COIN_BASE) {
            long amount = transaction.getAmountNQT();
            long transactionId = transaction.getId();
            if (!transaction.attachmentIsPhased()) {
                senderAccount.addToBalanceNQT(getLedgerEvent(), transactionId, -amount, -transaction.getFeeNQT());
            } else {
                senderAccount.addBalance(getLedgerEvent(), transactionId, -amount);
            }
            if (recipientAccount != null) {
                recipientAccount.addBalanceAddUnconfirmed(getLedgerEvent(), transactionId, amount);
            }
        }
        applyAttachment(transaction, senderAccount, recipientAccount);
    }

    public abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    public final void undoUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        undoAttachmentUnconfirmed(transaction, senderAccount);
        if (transaction.getType().getType() == TYPE_COIN_BASE) {
            return;
        }
        senderAccount.addUnconfirmed(getLedgerEvent(), transaction.getId(),
                transaction.getAmountNQT(), transaction.getFeeNQT());
        if (transaction.referencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            senderAccount.addUnconfirmed(getLedgerEvent(), transaction.getId(), 0,
                    Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
    }

    public abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    // isBlockDuplicate and isDuplicate share the same duplicates map, but isBlockDuplicate check is done first
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    public boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    public static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Map<String, Integer>> duplicates, boolean exclusive) {
        return isDuplicate(uniqueType, key, duplicates, exclusive ? 0 : Integer.MAX_VALUE);
    }

    public static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Map<String, Integer>> duplicates, int maxCount) {
        Map<String, Integer> typeDuplicates = duplicates.get(uniqueType);
        if (typeDuplicates == null) {
            typeDuplicates = new HashMap<>();
            duplicates.put(uniqueType, typeDuplicates);
        }
        Integer currentCount = typeDuplicates.get(key);
        if (currentCount == null) {
            typeDuplicates.put(key, maxCount > 0 ? 1 : 0);
            return false;
        }
        if (currentCount == 0) {
            return true;
        }
        if (currentCount < maxCount) {
            typeDuplicates.put(key, currentCount + 1);
            return false;
        }
        return true;
    }

    public boolean isPruned(long transactionId) {
        return false;
    }

    public abstract boolean canHaveRecipient();

    public boolean mustHaveRecipient() {
        return canHaveRecipient();
    }

    public abstract boolean isPhasingSafe();

    public boolean isPhasable() {
        return true;
    }

    public Fee getBaselineFee(Transaction transaction) {
        return Fee.DEFAULT_FEE;
    }

    public Fee getNextFee(Transaction transaction) {
        return getBaselineFee(transaction);
    }

    public int getBaselineFeeHeight() {
        return Constants.SHUFFLING_BLOCK_HEIGHT;
    }

    public int getNextFeeHeight() {
        return Integer.MAX_VALUE;
    }

    public long[] getBackFees(Transaction transaction) {
        return Convert.EMPTY_LONG;
    }

    public abstract String getName();

    @Override
    public final String toString() {
        return getName() + " type: " + getType() + ", subtype: " + getSubtype();
    }

    public static abstract class Payment extends TransactionType {

        private Payment() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_PAYMENT;
        }

        @Override
        public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            // exception processing
            if (recipientAccount == null) {
                Account.getAccount(SharderGenesis.KEEPER_ID).addBalanceAddUnconfirmed(getLedgerEvent(),
                        transaction.getId(), transaction.getAmountNQT());
            }
        }
        
        @Override
        public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return true;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }

        public static final TransactionType ORDINARY = new Payment() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
            }

            @Override
            public final AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ORDINARY_PAYMENT;
            }

            @Override
            public String getName() {
                return "OrdinaryPayment";
            }

            @Override
            public Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            public Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                if (transaction.getAmountNQT() <= 0 || transaction.getAmountNQT() >= Constants.MAX_BALANCE_NQT) {
                    throw new ConchException.NotValidException("Invalid ordinary payment");
                }
            }

        };

    }

    public static abstract class CoinBase extends TransactionType {

        private CoinBase() {}

        @Override
        public final byte getType() {
            return TransactionType.TYPE_COIN_BASE;
        }

        @Override
        public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }
        
        
        private static void calAndSetMiningReward(Account account, Transaction transaction,long amount, boolean stageTwo){
            if(!stageTwo) {
                account.addBalanceAddUnconfirmed(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), amount);
                account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), amount);
                Logger.logDebugMessage("[Stage One]add mining rewards %d to %s unconfirmed balance and freeze it of tx %d at height %d",
                        amount, account.getRsAddress(), transaction.getId() , transaction.getHeight());
            }else{
                account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), -amount);
                account.addMintedBalance(amount);
                account.pocChanged();
                Logger.logDebugMessage("[Stage Two]unfreeze mining rewards %d of %s and add it in mined amount of tx %d at height %d",
                        amount, account.getRsAddress(), transaction.getId() , transaction.getHeight());
            }
        }
        /**
         * total 2 stages: 
         * stage one is in tx accepted, the rewards need be lock; 
         * stage two is the block confirmations reached, means unlock the rewards and record mined amount
         * @param tx reward tx
         * @param stageTwo true - stage two; false - stage one
         * @return
         */
        public static long mintReward(Transaction tx, boolean stageTwo) {
            Attachment.CoinBase coinBase = (Attachment.CoinBase) tx.getAttachment();
            Account senderAccount = Account.getAccount(tx.getSenderId());
            Map<Long, Long> consignors = coinBase.getConsignors();
            
            if (consignors.size() == 0) {
                calAndSetMiningReward(senderAccount, tx, tx.getAmountNQT(), stageTwo);
            } else {
                Map<Long, Long> rewardList = PoolRule.calRewardMapAccordingToRules(senderAccount.getId(), coinBase.getGeneratorId(), tx.getAmountNQT(), consignors);
                for (long id : rewardList.keySet()) {
                    Account account = Account.getAccount(id);
                    calAndSetMiningReward(account, tx, rewardList.get(id), stageTwo);
                }
            }
            return tx.getAmountNQT();
        }

        public static final TransactionType ORDINARY = new CoinBase() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COIN_BASE_ORDINARY;
            }

            @Override
            public final AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.COIN_BASE;
            }

            @Override
            public String getName() {
                return "OrdinaryCoinBase";
            }

            @Override
            public Attachment.CoinBase parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.CoinBase(buffer, transactionVersion);
            }

            @Override
            public Attachment.CoinBase parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.CoinBase(attachmentData);
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return isDuplicate(CoinBase.ORDINARY, "OrdinaryCoinBase", duplicates, true);
            }

            private void validateByType(Transaction transaction) throws ConchException.NotValidException {
                Attachment.CoinBase coinBase = (Attachment.CoinBase) transaction.getAttachment();
                if (Attachment.CoinBase.CoinBaseType.BLOCK_REWARD == coinBase.getCoinBaseType()) {
                    Map<Long, Long> consignors = coinBase.getConsignors();
                    
                    if(consignors.size() <= 0) return;
                    
                    long id = SharderPoolProcessor.findOwnPoolId(transaction.getSenderId(), Conch.getBlockchain().getHeight());
                    SharderPoolProcessor poolProcessor = (id == -1) ? null : SharderPoolProcessor.getPool(id);
//                    SharderPoolProcessor poolProcessor = SharderPoolProcessor.getPool(id);
//                    if (poolProcessor != null 
//                        && poolProcessor.getState().equals(SharderPoolProcessor.State.WORKING)
//                        && !poolProcessor.validateConsignorsAmount(consignors)) {
//                        throw new ConchException.NotValidException("The pool allocation rule validation failed in BLOCK_REWARD processing[block id=%d, height=%d]", transaction.getBlockId(), transaction.getHeight());
//                    }
                } else if (Attachment.CoinBase.CoinBaseType.GENESIS == coinBase.getCoinBaseType()) {
                    if (!SharderGenesis.isGenesisCreator(coinBase.getCreator())) {
                        throw new ConchException.NotValidException("the Genesis coin base tx is not created by genesis creator");
                    }
                }
            }

            private void applyByType(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.CoinBase coinBase = (Attachment.CoinBase) transaction.getAttachment();
                if (Attachment.CoinBase.CoinBaseType.BLOCK_REWARD == coinBase.getCoinBaseType()) {
                    mintReward(transaction,false);
                } else if (Attachment.CoinBase.CoinBaseType.GENESIS == coinBase.getCoinBaseType()) {
                    if (SharderGenesis.isGenesisCreator(coinBase.getCreator()) && SharderGenesis.isGenesisRecipients(senderAccount.getId())) {
                        if (Constants.isDevnet()) {
                            Logger.logDebugMessage("add balance to genesis account in devnet[account id=" + senderAccount.getId() + ",amount=" + transaction.getAmountNQT() + "]");
                            senderAccount.addBalanceAddUnconfirmed(getLedgerEvent(), transaction.getId(), transaction.getAmountNQT());
                        } else {
                            senderAccount.addBalanceAddUnconfirmed(getLedgerEvent(), transaction.getId(), transaction.getAmountNQT());
                        }
                    }
                }
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                validateByType(transaction);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                applyByType(transaction, senderAccount, recipientAccount);
            }
        };

    }

    public static abstract class Messaging extends TransactionType {

        private Messaging() {}

        @Override
        public final byte getType() {
            return TransactionType.TYPE_MESSAGING;
        }

        @Override
        public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        public final static TransactionType ARBITRARY_MESSAGE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ARBITRARY_MESSAGE;
            }

            @Override
            public String getName() {
                return "ArbitraryMessage";
            }

            @Override
            public Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            public Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment attachment = transaction.getAttachment();
                if (transaction.getAmountNQT() != 0) {
                    throw new ConchException.NotValidException("Invalid arbitrary message: " + attachment.getJSONObject());
                }
                if (transaction.getRecipientId() == SharderGenesis.CREATOR_ID && Conch.getBlockchain().getHeight() > Constants.MONETARY_SYSTEM_BLOCK) {
                    throw new ConchException.NotValidException("Sending messages to Genesis not allowed.");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean mustHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_ASSIGNMENT = new Messaging() {

            private final Fee ALIAS_FEE = new Fee.SizeBasedFee(2 * Constants.ONE_SS, 2 * Constants.ONE_SS, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                    return attachment.getAliasName().length() + attachment.getAliasURI().length();
                }
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ALIAS_ASSIGNMENT;
            }

            @Override
            public String getName() {
                return "AliasAssignment";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return ALIAS_FEE;
            }

            @Override
            public Attachment.MessagingAliasAssignment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingAliasAssignment(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingAliasAssignment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingAliasAssignment(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                Alias.addOrUpdateAlias(transaction, attachment);
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return Conch.getBlockchain().getHeight() > Constants.SHUFFLING_BLOCK_HEIGHT
                        && Alias.getAlias(((Attachment.MessagingAliasAssignment) transaction.getAttachment()).getAliasName()) == null
                        && isDuplicate(Messaging.ALIAS_ASSIGNMENT, "", duplicates, true);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                if (attachment.getAliasName().length() == 0
                        || attachment.getAliasName().length() > Constants.MAX_ALIAS_LENGTH
                        || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
                    throw new ConchException.NotValidException("Invalid alias assignment: " + attachment.getJSONObject());
                }
                String normalizedAlias = attachment.getAliasName().toLowerCase();
                for (int i = 0; i < normalizedAlias.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                        throw new ConchException.NotValidException("Invalid alias name: " + normalizedAlias);
                    }
                }
                Alias alias = Alias.getAlias(normalizedAlias);
                if (alias != null && alias.getAccountId() != transaction.getSenderId()) {
                    throw new ConchException.NotCurrentlyValidException("Alias already owned by another account: " + normalizedAlias);
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_SELL = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_SELL;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ALIAS_SELL;
            }

            @Override
            public String getName() {
                return "AliasSell";
            }

            @Override
            public Attachment.MessagingAliasSell parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingAliasSell(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingAliasSell parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingAliasSell(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
                Alias.sellAlias(transaction, attachment);
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                if (transaction.getAmountNQT() != 0) {
                    throw new ConchException.NotValidException("Invalid sell alias transaction: " +
                            transaction.getJSONObject());
                }
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                if (aliasName == null || aliasName.length() == 0) {
                    throw new ConchException.NotValidException("Missing alias name");
                }
                long priceNQT = attachment.getPriceNQT();
                if (priceNQT < 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
                    throw new ConchException.NotValidException("Invalid alias sell price: " + priceNQT);
                }
                if (priceNQT == 0) {
                    if (SharderGenesis.CREATOR_ID == transaction.getRecipientId()) {
                        throw new ConchException.NotValidException("Transferring aliases to Genesis account not allowed");
                    } else if (transaction.getRecipientId() == 0) {
                        throw new ConchException.NotValidException("Missing alias transfer recipient");
                    }
                }
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new ConchException.NotCurrentlyValidException("No such alias: " + aliasName);
                } else if (alias.getAccountId() != transaction.getSenderId()) {
                    throw new ConchException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
                }
                if (transaction.getRecipientId() == SharderGenesis.CREATOR_ID) {
                    throw new ConchException.NotValidException("Selling alias to Genesis not allowed");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean mustHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_BUY = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_BUY;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ALIAS_BUY;
            }

            @Override
            public String getName() {
                return "AliasBuy";
            }

            @Override
            public Attachment.MessagingAliasBuy parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingAliasBuy(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingAliasBuy parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingAliasBuy(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                Alias.changeOwner(transaction.getSenderId(), aliasName);
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingAliasBuy attachment = (Attachment.MessagingAliasBuy) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new ConchException.NotCurrentlyValidException("No such alias: " + aliasName);
                } else if (alias.getAccountId() != transaction.getRecipientId()) {
                    throw new ConchException.NotCurrentlyValidException("Alias is owned by account other than recipient: "
                            + Long.toUnsignedString(alias.getAccountId()));
                }
                Alias.Offer offer = Alias.getOffer(alias);
                if (offer == null) {
                    throw new ConchException.NotCurrentlyValidException("Alias is not for sale: " + aliasName);
                }
                if (transaction.getAmountNQT() < offer.getPriceNQT()) {
                    String msg = "Price is too low for: " + aliasName + " ("
                            + transaction.getAmountNQT() + " < " + offer.getPriceNQT() + ")";
                    throw new ConchException.NotCurrentlyValidException(msg);
                }
                if (offer.getBuyerId() != 0 && offer.getBuyerId() != transaction.getSenderId()) {
                    throw new ConchException.NotCurrentlyValidException("Wrong buyer for " + aliasName + ": "
                            + Long.toUnsignedString(transaction.getSenderId()) + " expected: "
                            + Long.toUnsignedString(offer.getBuyerId()));
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_DELETE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_DELETE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ALIAS_DELETE;
            }

            @Override
            public String getName() {
                return "AliasDelete";
            }

            @Override
            public Attachment.MessagingAliasDelete parseAttachment(final ByteBuffer buffer, final byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingAliasDelete(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingAliasDelete parseAttachment(final JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingAliasDelete(attachmentData);
            }

            @Override
            public void applyAttachment(final Transaction transaction, final Account senderAccount, final Account recipientAccount) {
                final Attachment.MessagingAliasDelete attachment =
                        (Attachment.MessagingAliasDelete) transaction.getAttachment();
                Alias.deleteAlias(attachment.getAliasName());
            }

            @Override
            public boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingAliasDelete attachment = (Attachment.MessagingAliasDelete) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            public void validateAttachment(final Transaction transaction) throws ConchException.ValidationException {
                final Attachment.MessagingAliasDelete attachment =
                        (Attachment.MessagingAliasDelete) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                if (aliasName == null || aliasName.length() == 0) {
                    throw new ConchException.NotValidException("Missing alias name");
                }
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new ConchException.NotCurrentlyValidException("No such alias: " + aliasName);
                } else if (alias.getAccountId() != transaction.getSenderId()) {
                    throw new ConchException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public final static TransactionType POLL_CREATION = new Messaging() {

            private final Fee POLL_OPTIONS_FEE = new Fee.SizeBasedFee(10 * Constants.ONE_SS, Constants.ONE_SS, 1) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    int numOptions = ((Attachment.MessagingPollCreation) appendage).getPollOptions().length;
                    return numOptions <= 19 ? 0 : numOptions - 19;
                }
            };

            private final Fee POLL_SIZE_FEE = new Fee.SizeBasedFee(0, 2 * Constants.ONE_SS, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) appendage;
                    int size = attachment.getPollName().length() + attachment.getPollDescription().length();
                    for (String option : ((Attachment.MessagingPollCreation) appendage).getPollOptions()) {
                        size += option.length();
                    }
                    return size <= 288 ? 0 : size - 288;
                }
            };

            private final Fee POLL_FEE = (transaction, appendage) ->
                    POLL_OPTIONS_FEE.getFee(transaction, appendage) + POLL_SIZE_FEE.getFee(transaction, appendage);

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.POLL_CREATION;
            }

            @Override
            public String getName() {
                return "PollCreation";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return POLL_FEE;
            }

            @Override
            public Attachment.MessagingPollCreation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingPollCreation(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingPollCreation parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingPollCreation(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
                Poll.addPoll(transaction, attachment);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {

                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();

                int optionsCount = attachment.getPollOptions().length;

                if (attachment.getPollName().length() > Constants.MAX_POLL_NAME_LENGTH
                        || attachment.getPollName().isEmpty()
                        || attachment.getPollDescription().length() > Constants.MAX_POLL_DESCRIPTION_LENGTH
                        || optionsCount > Constants.MAX_POLL_OPTION_COUNT
                        || optionsCount == 0) {
                    throw new ConchException.NotValidException("Invalid poll attachment: " + attachment.getJSONObject());
                }

                if (attachment.getMinNumberOfOptions() < 1
                        || attachment.getMinNumberOfOptions() > optionsCount) {
                    throw new ConchException.NotValidException("Invalid min number of options: " + attachment.getJSONObject());
                }

                if (attachment.getMaxNumberOfOptions() < 1
                        || attachment.getMaxNumberOfOptions() < attachment.getMinNumberOfOptions()
                        || attachment.getMaxNumberOfOptions() > optionsCount) {
                    throw new ConchException.NotValidException("Invalid max number of options: " + attachment.getJSONObject());
                }

                for (int i = 0; i < optionsCount; i++) {
                    if (attachment.getPollOptions()[i].length() > Constants.MAX_POLL_OPTION_LENGTH
                            || attachment.getPollOptions()[i].isEmpty()) {
                        throw new ConchException.NotValidException("Invalid poll options length: " + attachment.getJSONObject());
                    }
                }

                if (attachment.getMinRangeValue() < Constants.MIN_VOTE_VALUE || attachment.getMaxRangeValue() > Constants.MAX_VOTE_VALUE
                        || attachment.getMaxRangeValue() < attachment.getMinRangeValue()) {
                    throw new ConchException.NotValidException("Invalid range: " + attachment.getJSONObject());
                }

                if (attachment.getFinishHeight() <= attachment.getFinishValidationHeight(transaction) + 1
                        || attachment.getFinishHeight() >= attachment.getFinishValidationHeight(transaction) + Constants.MAX_POLL_DURATION) {
                    throw new ConchException.NotCurrentlyValidException("Invalid finishing height" + attachment.getJSONObject());
                }

                if (!attachment.getVoteWeighting().acceptsVotes() || attachment.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
                    throw new ConchException.NotValidException("VotingModel " + attachment.getVoteWeighting().getVotingModel() + " not valid for regular polls");
                }

                attachment.getVoteWeighting().validate();

            }

            @Override
            public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return Conch.getBlockchain().getHeight() > Constants.SHUFFLING_BLOCK_HEIGHT
                        && isDuplicate(Messaging.POLL_CREATION, getName(), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public final static TransactionType VOTE_CASTING = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_VOTE_CASTING;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.VOTE_CASTING;
            }

            @Override
            public String getName() {
                return "VoteCasting";
            }

            @Override
            public Attachment.MessagingVoteCasting parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingVoteCasting(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingVoteCasting parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingVoteCasting(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                Vote.addVote(transaction, attachment);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {

                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                if (attachment.getPollId() == 0 || attachment.getPollVote() == null
                        || attachment.getPollVote().length > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new ConchException.NotValidException("Invalid vote casting attachment: " + attachment.getJSONObject());
                }

                long pollId = attachment.getPollId();

                Poll poll = Poll.getPoll(pollId);
                if (poll == null) {
                    throw new ConchException.NotCurrentlyValidException("Invalid poll: " + Long.toUnsignedString(attachment.getPollId()));
                }

                if (Vote.getVote(pollId, transaction.getSenderId()) != null) {
                    throw new ConchException.NotCurrentlyValidException("Double voting attempt");
                }

                if (poll.getFinishHeight() <= attachment.getFinishValidationHeight(transaction)) {
                    throw new ConchException.NotCurrentlyValidException("Voting for this poll finishes at " + poll.getFinishHeight());
                }

                byte[] votes = attachment.getPollVote();
                int positiveCount = 0;
                for (byte vote : votes) {
                    if (vote != Constants.NO_VOTE_VALUE && (vote < poll.getMinRangeValue() || vote > poll.getMaxRangeValue())) {
                        throw new ConchException.NotValidException(String.format("Invalid vote %d, vote must be between %d and %d",
                                vote, poll.getMinRangeValue(), poll.getMaxRangeValue()));
                    }
                    if (vote != Constants.NO_VOTE_VALUE) {
                        positiveCount++;
                    }
                }

                if (positiveCount < poll.getMinNumberOfOptions() || positiveCount > poll.getMaxNumberOfOptions()) {
                    throw new ConchException.NotValidException(String.format("Invalid num of choices %d, number of choices must be between %d and %d",
                            positiveCount, poll.getMinNumberOfOptions(), poll.getMaxNumberOfOptions()));
                }
            }

            @Override
            public boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                String key = Long.toUnsignedString(attachment.getPollId()) + ":" + Long.toUnsignedString(transaction.getSenderId());
                return isDuplicate(Messaging.VOTE_CASTING, key, duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType PHASING_VOTE_CASTING = new Messaging() {

            private final Fee PHASING_VOTE_FEE = (transaction, appendage) -> {
                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                return attachment.getTransactionFullHashes().size() * Constants.ONE_SS;
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_PHASING_VOTE_CASTING;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.PHASING_VOTE_CASTING;
            }

            @Override
            public String getName() {
                return "PhasingVoteCasting";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return PHASING_VOTE_FEE;
            }

            @Override
            public Attachment.MessagingPhasingVoteCasting parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingPhasingVoteCasting(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingPhasingVoteCasting parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingPhasingVoteCasting(attachmentData);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {

                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                byte[] revealedSecret = attachment.getRevealedSecret();
                if (revealedSecret.length > Constants.MAX_PHASING_REVEALED_SECRET_LENGTH) {
                    throw new ConchException.NotValidException("Invalid revealed secret length " + revealedSecret.length);
                }
                byte[] hashedSecret = null;
                byte algorithm = 0;

                List<byte[]> hashes = attachment.getTransactionFullHashes();
                if (hashes.size() > Constants.MAX_PHASING_VOTE_TRANSACTIONS) {
                    throw new ConchException.NotValidException("No more than " + Constants.MAX_PHASING_VOTE_TRANSACTIONS + " votes allowed for two-phased multi-voting");
                }

                long voterId = transaction.getSenderId();
                for (byte[] hash : hashes) {
                    long phasedTransactionId = Convert.fullHashToId(hash);
                    if (phasedTransactionId == 0) {
                        throw new ConchException.NotValidException("Invalid phased transactionFullHash " + Convert.toHexString(hash));
                    }

                    PhasingPoll poll = PhasingPoll.getPoll(phasedTransactionId);
                    if (poll == null) {
                        throw new ConchException.NotCurrentlyValidException("Invalid phased transaction " + Long.toUnsignedString(phasedTransactionId)
                                + ", or phasing is finished");
                    }
                    if (!poll.getVoteWeighting().acceptsVotes()) {
                        throw new ConchException.NotValidException("This phased transaction does not require or accept voting");
                    }
                    long[] whitelist = poll.getWhitelist();
                    if (whitelist.length > 0 && Arrays.binarySearch(whitelist, voterId) < 0) {
                        throw new ConchException.NotValidException("Voter is not in the phased transaction whitelist");
                    }
                    if (revealedSecret.length > 0) {
                        if (poll.getVoteWeighting().getVotingModel() != VoteWeighting.VotingModel.HASH) {
                            throw new ConchException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " does not accept by-hash voting");
                        }
                        if (hashedSecret != null && !Arrays.equals(poll.getHashedSecret(), hashedSecret)) {
                            throw new ConchException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecret");
                        }
                        if (algorithm != 0 && algorithm != poll.getAlgorithm()) {
                            throw new ConchException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecretAlgorithm");
                        }
                        if (hashedSecret == null && !poll.verifySecret(revealedSecret)) {
                            throw new ConchException.NotValidException("Revealed secret does not match phased transaction hashed secret");
                        }
                        hashedSecret = poll.getHashedSecret();
                        algorithm = poll.getAlgorithm();
                    } else if (poll.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
                        throw new ConchException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " requires revealed secret for approval");
                    }
                    if (!Arrays.equals(poll.getFullHash(), hash)) {
                        throw new ConchException.NotCurrentlyValidException("Phased transaction hash does not match hash in voting transaction");
                    }
                    if (poll.getFinishHeight() <= attachment.getFinishValidationHeight(transaction) + 1) {
                        throw new ConchException.NotCurrentlyValidException(String.format("Phased transaction finishes at height %d which is not after approval transaction height %d",
                                poll.getFinishHeight(), attachment.getFinishValidationHeight(transaction) + 1));
                    }
                }
            }

            @Override
            public final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                List<byte[]> hashes = attachment.getTransactionFullHashes();
                for (byte[] hash : hashes) {
                    PhasingVote.addVote(transaction, senderAccount, Convert.fullHashToId(hash));
                }
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType HUB_ANNOUNCEMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.HUB_ANNOUNCEMENT;
            }

            @Override
            public String getName() {
                return "HubAnnouncement";
            }

            @Override
            public Attachment.MessagingHubAnnouncement parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingHubAnnouncement(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingHubAnnouncement parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingHubAnnouncement(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingHubAnnouncement attachment = (Attachment.MessagingHubAnnouncement) transaction.getAttachment();
                Hub.addOrUpdateHub(transaction, attachment);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                if (Conch.getBlockchain().getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_HUB_ANNOUNCEMENT) {
                    throw new ConchException.NotYetEnabledException("Hub terminal announcement not yet enabled at height " + Conch.getBlockchain().getHeight());
                }
                Attachment.MessagingHubAnnouncement attachment = (Attachment.MessagingHubAnnouncement) transaction.getAttachment();
                if (attachment.getMinFeePerByteNQT() < 0 || attachment.getMinFeePerByteNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getUris().length > Constants.MAX_HUB_ANNOUNCEMENT_URIS) {
                    // cfb: "0" is allowed to show that another way to determine the min fee should be used
                    throw new ConchException.NotValidException("Invalid hub terminal announcement: " + attachment.getJSONObject());
                }
                for (String uri : attachment.getUris()) {
                    if (uri.length() > Constants.MAX_HUB_ANNOUNCEMENT_URI_LENGTH) {
                        throw new ConchException.NotValidException("Invalid URI length: " + uri.length());
                    }
                    //also check URI validity here?
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final Messaging ACCOUNT_INFO = new Messaging() {

            private final Fee ACCOUNT_INFO_FEE = new Fee.SizeBasedFee(Constants.ONE_SS, 2 * Constants.ONE_SS, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                    return attachment.getName().length() + attachment.getDescription().length();
                }
            };

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_INFO;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ACCOUNT_INFO;
            }

            @Override
            public String getName() {
                return "AccountInfo";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return ACCOUNT_INFO_FEE;
            }

            @Override
            public Attachment.MessagingAccountInfo parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingAccountInfo(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingAccountInfo parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingAccountInfo(attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                if (attachment.getName().length() > Constants.MAX_ACCOUNT_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
                    throw new ConchException.NotValidException("Invalid account info issuance: " + attachment.getJSONObject());
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                senderAccount.setAccountInfo(attachment.getName(), attachment.getDescription());
            }

            @Override
            public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return Conch.getBlockchain().getHeight() > Constants.SHUFFLING_BLOCK_HEIGHT
                        && isDuplicate(Messaging.ACCOUNT_INFO, getName(), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final Messaging ACCOUNT_PROPERTY = new Messaging() {

            private final Fee ACCOUNT_PROPERTY_FEE = new Fee.SizeBasedFee(Constants.ONE_SS, Constants.ONE_SS, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingAccountProperty attachment = (Attachment.MessagingAccountProperty) transaction.getAttachment();
                    return attachment.getValue().length();
                }
            };

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ACCOUNT_PROPERTY;
            }

            @Override
            public String getName() {
                return "AccountProperty";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return ACCOUNT_PROPERTY_FEE;
            }

            @Override
            public Attachment.MessagingAccountProperty parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingAccountProperty(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingAccountProperty parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingAccountProperty(attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.MessagingAccountProperty attachment = (Attachment.MessagingAccountProperty) transaction.getAttachment();
                if (attachment.getProperty().length() > Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH
                        || attachment.getProperty().length() == 0
                        || attachment.getValue().length() > Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH) {
                    throw new ConchException.NotValidException("Invalid account property: " + attachment.getJSONObject());
                }
                if (transaction.getAmountNQT() != 0) {
                    throw new ConchException.NotValidException("Account property transaction cannot be used to send SS");
                }
                if (transaction.getRecipientId() == SharderGenesis.CREATOR_ID) {
                    throw new ConchException.NotValidException("Setting Genesis account properties not allowed");
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountProperty attachment = (Attachment.MessagingAccountProperty) transaction.getAttachment();
                recipientAccount.setProperty(transaction, senderAccount, attachment.getProperty(), attachment.getValue());
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final Messaging ACCOUNT_PROPERTY_DELETE = new Messaging() {

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ACCOUNT_PROPERTY_DELETE;
            }

            @Override
            public String getName() {
                return "AccountPropertyDelete";
            }

            @Override
            public Attachment.MessagingAccountPropertyDelete parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.MessagingAccountPropertyDelete(buffer, transactionVersion);
            }

            @Override
            public Attachment.MessagingAccountPropertyDelete parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.MessagingAccountPropertyDelete(attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.MessagingAccountPropertyDelete attachment = (Attachment.MessagingAccountPropertyDelete) transaction.getAttachment();
                Account.AccountProperty accountProperty = Account.getProperty(attachment.getPropertyId());
                if (accountProperty == null) {
                    throw new ConchException.NotCurrentlyValidException("No such property " + Long.toUnsignedString(attachment.getPropertyId()));
                }
                if (accountProperty.getRecipientId() != transaction.getSenderId() && accountProperty.getSetterId() != transaction.getSenderId()) {
                    throw new ConchException.NotValidException("Account " + Long.toUnsignedString(transaction.getSenderId())
                            + " cannot delete property " + Long.toUnsignedString(attachment.getPropertyId()));
                }
                if (accountProperty.getRecipientId() != transaction.getRecipientId()) {
                    throw new ConchException.NotValidException("Account property " + Long.toUnsignedString(attachment.getPropertyId())
                            + " does not belong to " + Long.toUnsignedString(transaction.getRecipientId()));
                }
                if (transaction.getAmountNQT() != 0) {
                    throw new ConchException.NotValidException("Account property transaction cannot be used to send SS");
                }
                if (transaction.getRecipientId() == SharderGenesis.CREATOR_ID) {
                    throw new ConchException.NotValidException("Deleting Genesis account properties not allowed");
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountPropertyDelete attachment = (Attachment.MessagingAccountPropertyDelete) transaction.getAttachment();
                senderAccount.deleteProperty(attachment.getPropertyId());
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

    }

    public static abstract class ColoredCoins extends TransactionType {

        private ColoredCoins() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_COLORED_COINS;
        }

        public static final TransactionType ASSET_ISSUANCE = new ColoredCoins() {

            private final Fee SINGLETON_ASSET_FEE = new Fee.SizeBasedFee(Constants.ONE_SS, Constants.ONE_SS, 32) {
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance) transaction.getAttachment();
                    return attachment.getDescription().length();
                }
            };

            private final Fee ASSET_ISSUANCE_FEE = (transaction, appendage) -> isSingletonIssuance(transaction) ?
                    SINGLETON_ASSET_FEE.getFee(transaction, appendage) : 1000 * Constants.ONE_SS;

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ASSET_ISSUANCE;
            }

            @Override
            public String getName() {
                return "AssetIssuance";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return ASSET_ISSUANCE_FEE;
            }

            @Override
            public long[] getBackFees(Transaction transaction) {
                if (isSingletonIssuance(transaction)) {
                    return Convert.EMPTY_LONG;
                }
                long feeNQT = transaction.getFeeNQT();
                return new long[]{feeNQT * 3 / 10, feeNQT * 2 / 10, feeNQT / 10};
            }

            @Override
            public Attachment.ColoredCoinsAssetIssuance parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAssetIssuance(buffer, transactionVersion);
            }

            @Override
            public Attachment.ColoredCoinsAssetIssuance parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAssetIssuance(attachmentData);
            }

            @Override
            public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance) transaction.getAttachment();
                long assetId = transaction.getId();
                Asset.addAsset(transaction, attachment);
                senderAccount.addToAssetAndUnconfirmedAssetBalanceQNT(getLedgerEvent(), assetId, assetId, attachment.getQuantityQNT());
            }

            @Override
            public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance) transaction.getAttachment();
                if (attachment.getName().length() < Constants.MIN_ASSET_NAME_LENGTH
                        || attachment.getName().length() > Constants.MAX_ASSET_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH
                        || attachment.getDecimals() < 0 || attachment.getDecimals() > 8
                        || attachment.getQuantityQNT() <= 0
                        || attachment.getQuantityQNT() > Constants.MAX_ASSET_QUANTITY_QNT
                ) {
                    throw new ConchException.NotValidException("Invalid asset issuance: " + attachment.getJSONObject());
                }
                String normalizedName = attachment.getName().toLowerCase();
                for (int i = 0; i < normalizedName.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                        throw new ConchException.NotValidException("Invalid asset name: " + normalizedName);
                    }
                }
            }

            @Override
            public boolean isBlockDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
                return Conch.getBlockchain().getHeight() > Constants.SHUFFLING_BLOCK_HEIGHT
                        && !isSingletonIssuance(transaction)
                        && isDuplicate(ColoredCoins.ASSET_ISSUANCE, getName(), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

            private boolean isSingletonIssuance(Transaction transaction) {
                Attachment.ColoredCoinsAssetIssuance attachment = (Attachment.ColoredCoinsAssetIssuance) transaction.getAttachment();
                return attachment.getQuantityQNT() == 1 && attachment.getDecimals() == 0
                        && attachment.getDescription().length() <= Constants.MAX_SINGLETON_ASSET_DESCRIPTION_LENGTH;
            }

        };

        public static final TransactionType ASSET_TRANSFER = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ASSET_TRANSFER;
            }

            @Override
            public String getName() {
                return "AssetTransfer";
            }

            @Override
            public Attachment.ColoredCoinsAssetTransfer parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAssetTransfer(buffer, transactionVersion);
            }

            @Override
            public Attachment.ColoredCoinsAssetTransfer parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAssetTransfer(attachmentData);
            }

            @Override
            public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
                if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(getLedgerEvent(), transaction.getId(),
                            attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
                return false;
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                senderAccount.addToAssetBalanceQNT(getLedgerEvent(), transaction.getId(), attachment.getAssetId(),
                        -attachment.getQuantityQNT());
                if (recipientAccount.getId() == SharderGenesis.CREATOR_ID) {
                    Asset.deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityQNT());
                } else {
                    recipientAccount.addToAssetAndUnconfirmedAssetBalanceQNT(getLedgerEvent(), transaction.getId(),
                            attachment.getAssetId(), attachment.getQuantityQNT());
                    AssetTransfer.addAssetTransfer(transaction, attachment);
                }
            }

            @Override
            public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceQNT(getLedgerEvent(), transaction.getId(),
                        attachment.getAssetId(), attachment.getQuantityQNT());
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.ColoredCoinsAssetTransfer attachment = (Attachment.ColoredCoinsAssetTransfer) transaction.getAttachment();
                if (transaction.getAmountNQT() != 0
                        || attachment.getComment() != null && attachment.getComment().length() > Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH
                        || attachment.getAssetId() == 0) {
                    throw new ConchException.NotValidException("Invalid asset transfer amount or comment: " + attachment.getJSONObject());
                }
                if (transaction.getRecipientId() == SharderGenesis.CREATOR_ID && attachment.getFinishValidationHeight(transaction) > Constants.SHUFFLING_BLOCK_HEIGHT) {
                    throw new ConchException.NotValidException("Asset transfer to Genesis no longer allowed, "
                            + "use asset delete attachment instead");
                }
                if (transaction.getVersion() > 0 && attachment.getComment() != null) {
                    throw new ConchException.NotValidException("Asset transfer comments no longer allowed, use message " +
                            "or encrypted message appendix instead");
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityQNT() <= 0 || (asset != null && attachment.getQuantityQNT() > asset.getInitialQuantityQNT())) {
                    throw new ConchException.NotValidException("Invalid asset transfer asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new ConchException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType ASSET_DELETE = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASSET_DELETE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ASSET_DELETE;
            }

            @Override
            public String getName() {
                return "AssetDelete";
            }

            @Override
            public Attachment.ColoredCoinsAssetDelete parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAssetDelete(buffer, transactionVersion);
            }

            @Override
            public Attachment.ColoredCoinsAssetDelete parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAssetDelete(attachmentData);
            }

            @Override
            public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetDelete attachment = (Attachment.ColoredCoinsAssetDelete) transaction.getAttachment();
                long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
                if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(getLedgerEvent(), transaction.getId(),
                            attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
                return false;
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAssetDelete attachment = (Attachment.ColoredCoinsAssetDelete) transaction.getAttachment();
                senderAccount.addToAssetBalanceQNT(getLedgerEvent(), transaction.getId(), attachment.getAssetId(),
                        -attachment.getQuantityQNT());
                Asset.deleteAsset(transaction, attachment.getAssetId(), attachment.getQuantityQNT());
            }

            @Override
            public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAssetDelete attachment = (Attachment.ColoredCoinsAssetDelete) transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceQNT(getLedgerEvent(), transaction.getId(),
                        attachment.getAssetId(), attachment.getQuantityQNT());
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.ColoredCoinsAssetDelete attachment = (Attachment.ColoredCoinsAssetDelete) transaction.getAttachment();
                if (attachment.getAssetId() == 0) {
                    throw new ConchException.NotValidException("Invalid asset identifier: " + attachment.getJSONObject());
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityQNT() <= 0 || (asset != null && attachment.getQuantityQNT() > asset.getInitialQuantityQNT())) {
                    throw new ConchException.NotValidException("Invalid asset delete asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new ConchException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        abstract static class ColoredCoinsOrderPlacement extends ColoredCoins {

            @Override
            public final void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.ColoredCoinsOrderPlacement attachment = (Attachment.ColoredCoinsOrderPlacement) transaction.getAttachment();
                if (attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getAssetId() == 0) {
                    throw new ConchException.NotValidException("Invalid asset order placement: " + attachment.getJSONObject());
                }
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (attachment.getQuantityQNT() <= 0 || (asset != null && attachment.getQuantityQNT() > asset.getInitialQuantityQNT())) {
                    throw new ConchException.NotValidException("Invalid asset order placement asset or quantity: " + attachment.getJSONObject());
                }
                if (asset == null) {
                    throw new ConchException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) +
                            " does not exist yet");
                }
            }

            @Override
            public final boolean canHaveRecipient() {
                return false;
            }

            @Override
            public final boolean isPhasingSafe() {
                return true;
            }

        }

        public static final TransactionType ASK_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ASSET_ASK_ORDER_PLACEMENT;
            }

            @Override
            public String getName() {
                return "AskOrderPlacement";
            }

            @Override
            public Attachment.ColoredCoinsAskOrderPlacement parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderPlacement(buffer, transactionVersion);
            }

            @Override
            public Attachment.ColoredCoinsAskOrderPlacement parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderPlacement(attachmentData);
            }

            @Override
            public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
                if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(getLedgerEvent(), transaction.getId(),
                            attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
                return false;
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                Order.Ask.addOrder(transaction, attachment);
            }

            @Override
            public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsAskOrderPlacement attachment = (Attachment.ColoredCoinsAskOrderPlacement) transaction.getAttachment();
                senderAccount.addToUnconfirmedAssetBalanceQNT(getLedgerEvent(), transaction.getId(),
                        attachment.getAssetId(), attachment.getQuantityQNT());
            }

        };

        public final static TransactionType BID_ORDER_PLACEMENT = new ColoredCoins.ColoredCoinsOrderPlacement() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ASSET_BID_ORDER_PLACEMENT;
            }

            @Override
            public String getName() {
                return "BidOrderPlacement";
            }

            @Override
            public Attachment.ColoredCoinsBidOrderPlacement parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderPlacement(buffer, transactionVersion);
            }

            @Override
            public Attachment.ColoredCoinsBidOrderPlacement parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderPlacement(attachmentData);
            }

            @Override
            public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= Math.multiplyExact(attachment.getQuantityQNT(), attachment.getPriceNQT())) {
                    senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(),
                            -Math.multiplyExact(attachment.getQuantityQNT(), attachment.getPriceNQT()));
                    return true;
                }
                return false;
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                Order.Bid.addOrder(transaction, attachment);
            }

            @Override
            public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement) transaction.getAttachment();
                senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(),
                        Math.multiplyExact(attachment.getQuantityQNT(), attachment.getPriceNQT()));
            }

        };

        abstract static class ColoredCoinsOrderCancellation extends ColoredCoins {

            @Override
            public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            public boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.ColoredCoinsOrderCancellation attachment = (Attachment.ColoredCoinsOrderCancellation) transaction.getAttachment();
                return TransactionType.isDuplicate(ColoredCoins.ASK_ORDER_CANCELLATION, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
            }

            @Override
            public final boolean canHaveRecipient() {
                return false;
            }

            @Override
            public final boolean isPhasingSafe() {
                return true;
            }

        }

        public static final TransactionType ASK_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ASSET_ASK_ORDER_CANCELLATION;
            }

            @Override
            public String getName() {
                return "AskOrderCancellation";
            }

            @Override
            public Attachment.ColoredCoinsAskOrderCancellation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderCancellation(buffer, transactionVersion);
            }

            @Override
            public Attachment.ColoredCoinsAskOrderCancellation parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsAskOrderCancellation(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation) transaction.getAttachment();
                Order order = Order.Ask.getAskOrder(attachment.getOrderId());
                Order.Ask.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(getLedgerEvent(), transaction.getId(),
                            order.getAssetId(), order.getQuantityQNT());
                }
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.ColoredCoinsAskOrderCancellation attachment = (Attachment.ColoredCoinsAskOrderCancellation) transaction.getAttachment();
                Order ask = Order.Ask.getAskOrder(attachment.getOrderId());
                if (ask == null) {
                    throw new ConchException.NotCurrentlyValidException("Invalid ask order: " + Long.toUnsignedString(attachment.getOrderId()));
                }
                if (ask.getAccountId() != transaction.getSenderId()) {
                    throw new ConchException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account "
                            + Long.toUnsignedString(ask.getAccountId()));
                }
            }

        };

        public static final TransactionType BID_ORDER_CANCELLATION = new ColoredCoins.ColoredCoinsOrderCancellation() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ASSET_BID_ORDER_CANCELLATION;
            }

            @Override
            public String getName() {
                return "BidOrderCancellation";
            }

            @Override
            public Attachment.ColoredCoinsBidOrderCancellation parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderCancellation(buffer, transactionVersion);
            }

            @Override
            public Attachment.ColoredCoinsBidOrderCancellation parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.ColoredCoinsBidOrderCancellation(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation) transaction.getAttachment();
                Order order = Order.Bid.getBidOrder(attachment.getOrderId());
                Order.Bid.removeOrder(attachment.getOrderId());
                if (order != null) {
                    senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(),
                            Math.multiplyExact(order.getQuantityQNT(), order.getPriceNQT()));
                }
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.ColoredCoinsBidOrderCancellation attachment = (Attachment.ColoredCoinsBidOrderCancellation) transaction.getAttachment();
                Order bid = Order.Bid.getBidOrder(attachment.getOrderId());
                if (bid == null) {
                    throw new ConchException.NotCurrentlyValidException("Invalid bid order: " + Long.toUnsignedString(attachment.getOrderId()));
                }
                if (bid.getAccountId() != transaction.getSenderId()) {
                    throw new ConchException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account "
                            + Long.toUnsignedString(bid.getAccountId()));
                }
            }

        };

        public static final TransactionType DIVIDEND_PAYMENT = new ColoredCoins() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ASSET_DIVIDEND_PAYMENT;
            }

            @Override
            public String getName() {
                return "DividendPayment";
            }

            @Override
            public Attachment.ColoredCoinsDividendPayment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
                return new Attachment.ColoredCoinsDividendPayment(buffer, transactionVersion);
            }

            @Override
            public Attachment.ColoredCoinsDividendPayment parseAttachment(JSONObject attachmentData) {
                return new Attachment.ColoredCoinsDividendPayment(attachmentData);
            }

            @Override
            public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment) transaction.getAttachment();
                long assetId = attachment.getAssetId();
                Asset asset = Asset.getAsset(assetId, attachment.getHeight());
                if (asset == null) {
                    return true;
                }
                long quantityQNT = asset.getQuantityQNT() - senderAccount.getAssetBalanceQNT(assetId, attachment.getHeight());
                long totalDividendPayment = Math.multiplyExact(attachment.getAmountNQTPerQNT(), quantityQNT);
                if (senderAccount.getUnconfirmedBalanceNQT() >= totalDividendPayment) {
                    senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), -totalDividendPayment);
                    return true;
                }
                return false;
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment) transaction.getAttachment();
                senderAccount.payDividends(transaction.getId(), attachment);
            }

            @Override
            public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment) transaction.getAttachment();
                long assetId = attachment.getAssetId();
                Asset asset = Asset.getAsset(assetId, attachment.getHeight());
                if (asset == null) {
                    return;
                }
                long quantityQNT = asset.getQuantityQNT() - senderAccount.getAssetBalanceQNT(assetId, attachment.getHeight());
                long totalDividendPayment = Math.multiplyExact(attachment.getAmountNQTPerQNT(), quantityQNT);
                senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), totalDividendPayment);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment) transaction.getAttachment();
                if (attachment.getHeight() > Conch.getBlockchain().getHeight()) {
                    throw new ConchException.NotCurrentlyValidException("Invalid dividend payment height: " + attachment.getHeight()
                            + ", must not exceed current blockchain height " + Conch.getBlockchain().getHeight());
                }
                if (attachment.getHeight() <= attachment.getFinishValidationHeight(transaction) - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK) {
                    throw new ConchException.NotCurrentlyValidException("Invalid dividend payment height: " + attachment.getHeight()
                            + ", must be less than " + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK
                            + " blocks before " + attachment.getFinishValidationHeight(transaction));
                }
                Asset asset;
                if (Conch.getBlockchain().getHeight() > Constants.SHUFFLING_BLOCK_HEIGHT) {
                    asset = Asset.getAsset(attachment.getAssetId(), attachment.getHeight());
                } else {
                    asset = Asset.getAsset(attachment.getAssetId());
                }
                if (asset == null) {
                    throw new ConchException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId())
                            + " for dividend payment doesn't exist yet");
                }
                if (asset.getAccountId() != transaction.getSenderId() || attachment.getAmountNQTPerQNT() <= 0) {
                    throw new ConchException.NotValidException("Invalid dividend payment sender or amount " + attachment.getJSONObject());
                }
                if (Conch.getBlockchain().getHeight() > Constants.FXT_BLOCK) {
                    AssetDividend lastDividend = AssetDividend.getLastDividend(attachment.getAssetId());
                    if (lastDividend != null && lastDividend.getHeight() > Conch.getBlockchain().getHeight() - 60) {
                        throw new ConchException.NotCurrentlyValidException("Last dividend payment for asset " + Long.toUnsignedString(attachment.getAssetId())
                                + " was less than 60 blocks ago at " + lastDividend.getHeight() + ", current height is " + Conch.getBlockchain().getHeight()
                                + ", limit is one dividend per 60 blocks");
                    }
                }
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.ColoredCoinsDividendPayment attachment = (Attachment.ColoredCoinsDividendPayment) transaction.getAttachment();
                return Conch.getBlockchain().getHeight() > Constants.FXT_BLOCK &&
                        isDuplicate(ColoredCoins.DIVIDEND_PAYMENT, Long.toUnsignedString(attachment.getAssetId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

    }

    public static abstract class DigitalGoods extends TransactionType {

        private DigitalGoods() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_DIGITAL_GOODS;
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            if (transaction.getAmountNQT() != 0) {
                throw new ConchException.NotValidException("Invalid digital goods transaction");
            }
            doValidateAttachment(transaction);
        }

        abstract void doValidateAttachment(Transaction transaction) throws ConchException.ValidationException;


        public static final TransactionType LISTING = new DigitalGoods() {

            private final Fee DGS_LISTING_FEE = new Fee.SizeBasedFee(2 * Constants.ONE_SS, 2 * Constants.ONE_SS, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                    return attachment.getName().length() + attachment.getDescription().length();
                }
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_LISTING;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.DIGITAL_GOODS_LISTING;
            }

            @Override
            public String getName() {
                return "DigitalGoodsListing";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return DGS_LISTING_FEE;
            }

            @Override
            public Attachment.DigitalGoodsListing parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsListing(buffer, transactionVersion);
            }

            @Override
            public Attachment.DigitalGoodsListing parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsListing(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                DigitalGoodsStore.listGoods(transaction, attachment);
            }

            @Override
            public void doValidateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                if (attachment.getName().length() == 0
                        || attachment.getName().length() > Constants.MAX_DGS_LISTING_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH
                        || attachment.getTags().length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH
                        || attachment.getQuantity() < 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT) {
                    throw new ConchException.NotValidException("Invalid digital goods listing: " + attachment.getJSONObject());
                }
                Appendix.PrunablePlainMessage prunablePlainMessage = transaction.getPrunablePlainMessage();
                if (prunablePlainMessage != null) {
                    byte[] image = prunablePlainMessage.getMessage();
                    if (image != null) {
                        Tika tika = new Tika();
                        MediaType mediaType = null;
                        try {
                            String mediaTypeName = tika.detect(image);
                            mediaType = MediaType.parse(mediaTypeName);
                        } catch (NoClassDefFoundError e) {
                            Logger.logErrorMessage("Error running Tika parsers", e);
                        }
                        if (mediaType == null || !"image".equals(mediaType.getType())) {
                            throw new ConchException.NotValidException("Only image attachments allowed for DGS listing, media type is " + mediaType);
                        }
                    }
                }
            }

            @Override
            public boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return Conch.getBlockchain().getHeight() > Constants.SHUFFLING_BLOCK_HEIGHT
                        && isDuplicate(DigitalGoods.LISTING, getName(), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType DELISTING = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELISTING;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.DIGITAL_GOODS_DELISTING;
            }

            @Override
            public String getName() {
                return "DigitalGoodsDelisting";
            }

            @Override
            public Attachment.DigitalGoodsDelisting parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(buffer, transactionVersion);
            }

            @Override
            public Attachment.DigitalGoodsDelisting parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.delistGoods(attachment.getGoodsId());
            }

            @Override
            public void doValidateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (goods != null && transaction.getSenderId() != goods.getSellerId()) {
                    throw new ConchException.NotValidException("Invalid digital goods delisting - seller is different: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new ConchException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType PRICE_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.DIGITAL_GOODS_PRICE_CHANGE;
            }

            @Override
            public String getName() {
                return "DigitalGoodsPriceChange";
            }

            @Override
            public Attachment.DigitalGoodsPriceChange parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(buffer, transactionVersion);
            }

            @Override
            public Attachment.DigitalGoodsPriceChange parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.changePrice(attachment.getGoodsId(), attachment.getPriceNQT());
            }

            @Override
            public void doValidateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                    throw new ConchException.NotValidException("Invalid digital goods price change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new ConchException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType QUANTITY_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.DIGITAL_GOODS_QUANTITY_CHANGE;
            }

            @Override
            public String getName() {
                return "DigitalGoodsQuantityChange";
            }

            @Override
            public Attachment.DigitalGoodsQuantityChange parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(buffer, transactionVersion);
            }

            @Override
            public Attachment.DigitalGoodsQuantityChange parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.changeQuantity(attachment.getGoodsId(), attachment.getDeltaQuantity());
            }

            @Override
            public void doValidateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (attachment.getDeltaQuantity() < -Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getDeltaQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                    throw new ConchException.NotValidException("Invalid digital goods quantity change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new ConchException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType PURCHASE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PURCHASE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.DIGITAL_GOODS_PURCHASE;
            }

            @Override
            public String getName() {
                return "DigitalGoodsPurchase";
            }

            @Override
            public Attachment.DigitalGoodsPurchase parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(buffer, transactionVersion);
            }

            @Override
            public Attachment.DigitalGoodsPurchase parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(attachmentData);
            }

            @Override
            public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceNQT())) {
                    senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(),
                            -Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceNQT()));
                    return true;
                }
                return false;
            }

            @Override
            public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(),
                        Math.multiplyExact((long) attachment.getQuantity(), attachment.getPriceNQT()));
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.purchase(transaction, attachment);
            }

            @Override
            public void doValidateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (attachment.getQuantity() <= 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || (goods != null && goods.getSellerId() != transaction.getRecipientId())) {
                    throw new ConchException.NotValidException("Invalid digital goods purchase: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
                    throw new ConchException.NotValidException("Only text encrypted messages allowed");
                }
                if (goods == null || goods.isDelisted()) {
                    throw new ConchException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
                if (attachment.getQuantity() > goods.getQuantity() || attachment.getPriceNQT() != goods.getPriceNQT()) {
                    throw new ConchException.NotCurrentlyValidException("Goods price or quantity changed: " + attachment.getJSONObject());
                }
                if (attachment.getDeliveryDeadlineTimestamp() <= Conch.getBlockchain().getLastBlockTimestamp()) {
                    throw new ConchException.NotCurrentlyValidException("Delivery deadline has already expired: " + attachment.getDeliveryDeadlineTimestamp());
                }
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                if (Conch.getBlockchain().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                    return false;
                }
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, false);
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType DELIVERY = new DigitalGoods() {

            private final Fee DGS_DELIVERY_FEE = new Fee.SizeBasedFee(Constants.ONE_SS, 2 * Constants.ONE_SS, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                    return attachment.getGoodsDataLength() - 16;
                }
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELIVERY;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.DIGITAL_GOODS_DELIVERY;
            }

            @Override
            public String getName() {
                return "DigitalGoodsDelivery";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return DGS_DELIVERY_FEE;
            }

            @Override
            public Attachment.DigitalGoodsDelivery parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsDelivery(buffer, transactionVersion);
            }

            @Override
            public Attachment.DigitalGoodsDelivery parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                if (attachmentData.get("goodsData") == null) {
                    return new Attachment.UnencryptedDigitalGoodsDelivery(attachmentData);
                }
                return new Attachment.DigitalGoodsDelivery(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                DigitalGoodsStore.deliver(transaction, attachment);
            }

            @Override
            public void doValidateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPendingPurchase(attachment.getPurchaseId());
                if (attachment.getGoodsDataLength() > Constants.MAX_DGS_GOODS_LENGTH) {
                    throw new ConchException.NotValidException("Invalid digital goods delivery data length: " + attachment.getGoodsDataLength());
                }
                if (attachment.getGoods() != null) {
                    if (attachment.getGoods().getData().length == 0 || attachment.getGoods().getNonce().length != 32) {
                        throw new ConchException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
                    }
                }
                if (attachment.getDiscountNQT() < 0 || attachment.getDiscountNQT() > Constants.MAX_BALANCE_NQT
                        || (purchase != null &&
                        (purchase.getBuyerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getSellerId()
                                || attachment.getDiscountNQT() > Math.multiplyExact(purchase.getPriceNQT(), (long) purchase.getQuantity())))) {
                    throw new ConchException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
                }
                if (purchase == null || purchase.getEncryptedGoods() != null) {
                    throw new ConchException.NotCurrentlyValidException("Purchase does not exist yet, or already delivered: "
                            + attachment.getJSONObject());
                }
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELIVERY, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType FEEDBACK = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_FEEDBACK;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.DIGITAL_GOODS_FEEDBACK;
            }

            @Override
            public String getName() {
                return "DigitalGoodsFeedback";
            }

            @Override
            public Attachment.DigitalGoodsFeedback parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(buffer, transactionVersion);
            }

            @Override
            public Attachment.DigitalGoodsFeedback parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback) transaction.getAttachment();
                DigitalGoodsStore.feedback(attachment.getPurchaseId(), transaction.getEncryptedMessage(), transaction.getMessage());
            }

            @Override
            public void doValidateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(attachment.getPurchaseId());
                if (purchase != null &&
                        (purchase.getSellerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getBuyerId())) {
                    throw new ConchException.NotValidException("Invalid digital goods feedback: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() == null && transaction.getMessage() == null) {
                    throw new ConchException.NotValidException("Missing feedback message");
                }
                if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
                    throw new ConchException.NotValidException("Only text encrypted messages allowed");
                }
                if (transaction.getMessage() != null && !transaction.getMessage().isText()) {
                    throw new ConchException.NotValidException("Only text public messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null) {
                    throw new ConchException.NotCurrentlyValidException("Purchase does not exist yet or not yet delivered");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType REFUND = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_REFUND;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.DIGITAL_GOODS_REFUND;
            }

            @Override
            public String getName() {
                return "DigitalGoodsRefund";
            }

            @Override
            public Attachment.DigitalGoodsRefund parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsRefund(buffer, transactionVersion);
            }

            @Override
            public Attachment.DigitalGoodsRefund parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.DigitalGoodsRefund(attachmentData);
            }

            @Override
            public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                if (senderAccount.getUnconfirmedBalanceNQT() >= attachment.getRefundNQT()) {
                    senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), -attachment.getRefundNQT());
                    return true;
                }
                return false;
            }

            @Override
            public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), attachment.getRefundNQT());
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.refund(getLedgerEvent(), transaction.getId(), transaction.getSenderId(),
                        attachment.getPurchaseId(), attachment.getRefundNQT(), transaction.getEncryptedMessage());
            }

            @Override
            public void doValidateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(attachment.getPurchaseId());
                if (attachment.getRefundNQT() < 0 || attachment.getRefundNQT() > Constants.MAX_BALANCE_NQT
                        || (purchase != null &&
                        (purchase.getBuyerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getSellerId()))) {
                    throw new ConchException.NotValidException("Invalid digital goods refund: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
                    throw new ConchException.NotValidException("Only text encrypted messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null || purchase.getRefundNQT() != 0) {
                    throw new ConchException.NotCurrentlyValidException("Purchase does not exist or is not delivered or is already refunded");
                }
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                return isDuplicate(DigitalGoods.REFUND, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

    }

    public static abstract class AccountControl extends TransactionType {

        private AccountControl() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_ACCOUNT_CONTROL;
        }

        @Override
        public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        public static final TransactionType EFFECTIVE_BALANCE_LEASING = new AccountControl() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
            }

            @Override
            public String getName() {
                return "EffectiveBalanceLeasing";
            }

            @Override
            public Attachment.AccountControlEffectiveBalanceLeasing parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.AccountControlEffectiveBalanceLeasing(buffer, transactionVersion);
            }

            @Override
            public Attachment.AccountControlEffectiveBalanceLeasing parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.AccountControlEffectiveBalanceLeasing(attachmentData);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.AccountControlEffectiveBalanceLeasing attachment = (Attachment.AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
                Account.getAccount(transaction.getSenderId()).leaseEffectiveBalance(transaction.getRecipientId(), attachment.getPeriod());
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.AccountControlEffectiveBalanceLeasing attachment = (Attachment.AccountControlEffectiveBalanceLeasing) transaction.getAttachment();
                if (transaction.getSenderId() == transaction.getRecipientId()) {
                    throw new ConchException.NotValidException("Account cannot lease balance to itself");
                }
                if (transaction.getAmountNQT() != 0) {
                    throw new ConchException.NotValidException("Transaction amount must be 0 for effective balance leasing");
                }
                if (attachment.getPeriod() < Constants.LEASING_DELAY || attachment.getPeriod() > 65535) {
                    throw new ConchException.NotValidException("Invalid effective balance leasing period: " + attachment.getPeriod());
                }
                byte[] recipientPublicKey = Account.getPublicKey(transaction.getRecipientId());
                if (recipientPublicKey == null && Conch.getBlockchain().getHeight() > Constants.PHASING_BLOCK_HEIGHT) {
                    throw new ConchException.NotCurrentlyValidException("Invalid effective balance leasing: " + " recipient account " + Long.toUnsignedString(transaction.getRecipientId()) + " not found or no public key published");
                }
                if (transaction.getRecipientId() == SharderGenesis.CREATOR_ID) {
                    throw new ConchException.NotValidException("Leasing to Genesis account not allowed");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType SET_PHASING_ONLY = new AccountControl() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.ACCOUNT_CONTROL_PHASING_ONLY;
            }

            @Override
            public AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
                return new Attachment.SetPhasingOnly(buffer, transactionVersion);
            }

            @Override
            public AbstractAttachment parseAttachment(JSONObject attachmentData) {
                return new Attachment.SetPhasingOnly(attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.SetPhasingOnly attachment = (Attachment.SetPhasingOnly) transaction.getAttachment();
                VoteWeighting.VotingModel votingModel = attachment.getPhasingParams().getVoteWeighting().getVotingModel();
                attachment.getPhasingParams().validate();
                if (votingModel == VoteWeighting.VotingModel.NONE) {
                    Account senderAccount = Account.getAccount(transaction.getSenderId());
                    if (senderAccount == null || !senderAccount.getControls().contains(ControlType.PHASING_ONLY)) {
                        throw new ConchException.NotCurrentlyValidException("Phasing only account control is not currently enabled");
                    }
                } else if (votingModel == VoteWeighting.VotingModel.TRANSACTION || votingModel == VoteWeighting.VotingModel.HASH) {
                    throw new ConchException.NotValidException("Invalid voting model " + votingModel + " for account control");
                }
                long maxFees = attachment.getMaxFees();
                long maxFeesLimit = (attachment.getPhasingParams().getVoteWeighting().isBalanceIndependent() ? 3 : 22) * Constants.ONE_SS;
                if (maxFees < 0 || (maxFees > 0 && maxFees < maxFeesLimit) || maxFees > Constants.MAX_BALANCE_NQT) {
                    throw new ConchException.NotValidException(String.format("Invalid max fees %f SS", ((double) maxFees) / Constants.ONE_SS));
                }
                short minDuration = attachment.getMinDuration();
                if (minDuration < 0 || (minDuration > 0 && minDuration < 3) || minDuration >= Constants.MAX_PHASING_DURATION) {
                    throw new ConchException.NotValidException("Invalid min duration " + attachment.getMinDuration());
                }
                short maxDuration = attachment.getMaxDuration();
                if (maxDuration < 0 || (maxDuration > 0 && maxDuration < 3) || maxDuration >= Constants.MAX_PHASING_DURATION) {
                    throw new ConchException.NotValidException("Invalid max duration " + maxDuration);
                }
                if (minDuration > maxDuration) {
                    throw new ConchException.NotValidException(String.format("Min duration %d cannot exceed max duration %d ",
                            minDuration, maxDuration));
                }
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return TransactionType.isDuplicate(SET_PHASING_ONLY, Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.SetPhasingOnly attachment = (Attachment.SetPhasingOnly) transaction.getAttachment();
                AccountRestrictions.PhasingOnly.set(senderAccount, attachment);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public String getName() {
                return "SetPhasingOnly";
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

    }

    public static abstract class Data extends TransactionType {

        private static final Fee TAGGED_DATA_FEE = new Fee.SizeBasedFee(Constants.ONE_SS, Constants.ONE_SS / 10) {
            @Override
            public int getSize(TransactionImpl transaction, Appendix appendix) {
                return appendix.getFullSize();
            }
        };

        private Data() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_DATA;
        }

        @Override
        public final Fee getBaselineFee(Transaction transaction) {
            return TAGGED_DATA_FEE;
        }

        @Override
        public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return false;
        }

        @Override
        public final boolean isPhasable() {
            return false;
        }

        public static final TransactionType TAGGED_DATA_UPLOAD = new Data() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_DATA_TAGGED_DATA_UPLOAD;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.TAGGED_DATA_UPLOAD;
            }

            @Override
            public Attachment.TaggedDataUpload parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.TaggedDataUpload(buffer, transactionVersion);
            }

            @Override
            public Attachment.TaggedDataUpload parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.TaggedDataUpload(attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.TaggedDataUpload attachment = (Attachment.TaggedDataUpload) transaction.getAttachment();
                if (attachment.getData() == null && Conch.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new ConchException.NotCurrentlyValidException("Data has been pruned prematurely");
                }
                if (attachment.getData() != null) {
                    if (attachment.getName().length() == 0 || attachment.getName().length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
                        throw new ConchException.NotValidException("Invalid name length: " + attachment.getName().length());
                    }
                    if (attachment.getDescription().length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
                        throw new ConchException.NotValidException("Invalid description length: " + attachment.getDescription().length());
                    }
                    if (attachment.getTags().length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
                        throw new ConchException.NotValidException("Invalid tags length: " + attachment.getTags().length());
                    }
                    if (attachment.getType().length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
                        throw new ConchException.NotValidException("Invalid type length: " + attachment.getType().length());
                    }
                    if (attachment.getChannel().length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
                        throw new ConchException.NotValidException("Invalid channel length: " + attachment.getChannel().length());
                    }
                    if (attachment.getFilename().length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
                        throw new ConchException.NotValidException("Invalid filename length: " + attachment.getFilename().length());
                    }
                    if (attachment.getData().length == 0 || attachment.getData().length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
                        throw new ConchException.NotValidException("Invalid data length: " + attachment.getData().length);
                    }
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.TaggedDataUpload attachment = (Attachment.TaggedDataUpload) transaction.getAttachment();
                TaggedData.add((TransactionImpl) transaction, attachment);
            }

            @Override
            public String getName() {
                return "TaggedDataUpload";
            }

            @Override
            public boolean isPruned(long transactionId) {
                return TaggedData.isPruned(transactionId);
            }

        };

        public static final TransactionType TAGGED_DATA_EXTEND = new Data() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_DATA_TAGGED_DATA_EXTEND;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.TAGGED_DATA_EXTEND;
            }

            @Override
            public Attachment.TaggedDataExtend parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.TaggedDataExtend(buffer, transactionVersion);
            }

            @Override
            public Attachment.TaggedDataExtend parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.TaggedDataExtend(attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                Attachment.TaggedDataExtend attachment = (Attachment.TaggedDataExtend) transaction.getAttachment();
                if ((attachment.jsonIsPruned() || attachment.getData() == null) && Conch.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new ConchException.NotCurrentlyValidException("Data has been pruned prematurely");
                }
                TransactionImpl uploadTransaction = TransactionDb.findTransaction(attachment.getTaggedDataId(), Conch.getBlockchain().getHeight());
                if (uploadTransaction == null) {
                    throw new ConchException.NotCurrentlyValidException("No such tagged data upload " + Long.toUnsignedString(attachment.getTaggedDataId()));
                }
                if (uploadTransaction.getType() != TAGGED_DATA_UPLOAD) {
                    throw new ConchException.NotValidException("Transaction " + Long.toUnsignedString(attachment.getTaggedDataId())
                            + " is not a tagged data upload");
                }
                if (attachment.getData() != null) {
                    Attachment.TaggedDataUpload taggedDataUpload = (Attachment.TaggedDataUpload) uploadTransaction.getAttachment();
                    if (!Arrays.equals(attachment.getHash(), taggedDataUpload.getHash())) {
                        throw new ConchException.NotValidException("Hashes don't match! Extend hash: " + Convert.toHexString(attachment.getHash())
                                + " upload hash: " + Convert.toHexString(taggedDataUpload.getHash()));
                    }
                }
                TaggedData taggedData = TaggedData.getData(attachment.getTaggedDataId());
                if (taggedData != null && taggedData.getTransactionTimestamp() > Conch.getEpochTime() + 6 * Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new ConchException.NotCurrentlyValidException("Data already extended, timestamp is " + taggedData.getTransactionTimestamp());
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.TaggedDataExtend attachment = (Attachment.TaggedDataExtend) transaction.getAttachment();
                TaggedData.extend(transaction, attachment);
            }

            @Override
            public String getName() {
                return "TaggedDataExtend";
            }

            @Override
            public boolean isPruned(long transactionId) {
                return false;
            }

        };

    }

    public static abstract class SharderPool extends TransactionType {
        public abstract boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount);

        public abstract void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount);

        @Override
        public final byte getType() {
            return TransactionType.TYPE_SHARDER_POOL;
        }

        @Override
        public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            if (attachmentApplyUnconfirmed(transaction, senderAccount)) {
                return true;
            }
            return false;
        }

        @Override
        public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            attachmentUndoUnconfirmed(transaction, senderAccount);
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return false;
        }

        @Override
        public final boolean isPhasable() {
            return false;
        }

        public static final TransactionType SHARDER_POOL_CREATE = new SharderPool() {
            @Override
            public boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            public void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount) {

            }

            @Override
            public byte getSubtype() {
                return SUBTYPE_SHARDER_POOL_CREATE;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.FORGE_POOL_CREATE;
            }

            @Override
            public AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.SharderPoolCreate(buffer, transactionVersion);
            }

            @Override
            public AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.SharderPoolCreate(attachmentData);
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return isDuplicate(SharderPool.SHARDER_POOL_CREATE, Long.toString(transaction.getSenderId()), duplicates, true);
            }

            @Override
            public void validateAttachment(Transaction tx) throws ConchException.ValidationException {
                Attachment.SharderPoolCreate create = (Attachment.SharderPoolCreate) tx.getAttachment();
                if (!PoolRule.validateRule(tx.getSenderId(), PoolRule.mapToJsonObject(create.getRule()))) {
                    throw new ConchException.NotValidException("pool[id=%d, creator id=%s] rule not matched", tx.getId(), Account.rsAccount(tx.getSenderId()));
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                int curHeight = Conch.getBlockchain().getLastBlock().getHeight();
                Attachment.SharderPoolCreate create = (Attachment.SharderPoolCreate) transaction.getAttachment();
                SharderPoolProcessor miningPool = SharderPoolProcessor.createSharderPool(senderAccount.getId(), transaction.getId(), curHeight + Constants.SHARDER_POOL_DELAY,
                        curHeight + Constants.SHARDER_POOL_DELAY + create.getPeriod(),
                        PoolRule.getRuleInstance(senderAccount.getId(), PoolRule.mapToJsonObject(create.getRule())));
                if(miningPool == null) {
                    Logger.logWarningMessage("current pool creation tx[id=%d, height=%d] be ignored, maybe this account already has a working pool or something is wrong", transaction.getId(), transaction.getHeight());
                }
                senderAccount.pocChanged();
            }

            @Override
            public String getName() {
                return "createPool";
            }

        };

        public static final TransactionType SHARDER_POOL_DESTROY = new SharderPool() {
            @Override
            public boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            public void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount) {
            }

            @Override
            public byte getSubtype() {
                return SUBTYPE_SHARDER_POOL_DESTROY;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.FORGE_POOL_DESTROY;
            }

            @Override
            public AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.SharderPoolDestroy(buffer, transactionVersion);
            }

            @Override
            public AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.SharderPoolDestroy(attachmentData);
            }

            @Override
            public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return isDuplicate(SharderPool.SHARDER_POOL_DESTROY, Long.toString(transaction.getSenderId()), duplicates, true);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                //TODO unconfirmed transaction already has this kind of transaction
                Attachment.SharderPoolDestroy destroy = (Attachment.SharderPoolDestroy) transaction.getAttachment();
                SharderPoolProcessor forgePool = SharderPoolProcessor.getPool(destroy.getPoolId());
                if (forgePool == null) {
                    Logger.logWarningMessage("Pool " + destroy.getPoolId() + " doesn't exists, don't execute this tx[id=" + transaction.getId() + ",sender=" + transaction.getSenderId() + "]");
//                    throw new ConchException.NotValidException("Pool " + destroy.getPoolId() + " doesn't exists");
                }
                if (transaction.getSenderId() != SharderPoolProcessor.getPool(destroy.getPoolId()).getCreatorId()) {
                    Logger.logWarningMessage("Transaction creator " + transaction.getSenderId() + "isn't' pool creator " + forgePool.getCreatorId() + "don't execute this tx[id=" + transaction.getId() + "]");
//                    throw new ConchException.NotValidException("Transaction creator " + transaction.getSenderId() + "isn't' pool creator " + forgePool.getCreatorId());
                }
                int curHeight = Conch.getBlockchain().getLastBlock().getHeight();
                int endHeight = forgePool.getEndBlockNo();
                if (curHeight + Constants.SHARDER_POOL_DELAY > endHeight) {
                    Logger.logWarningMessage("Pool will be destroyed at " + endHeight + " before transaction apply at height " + curHeight + ", still to destroy it");
                }
                if (curHeight + Constants.SHARDER_POOL_DELAY - forgePool.getStartBlockNo() > Constants.SHARDER_POOL_MAX_BLOCK_DESTROY) {
                    throw new ConchException.NotValidException("Pool can't be destroyed, because it start at %d and current height %d is out of manual destroy range %d", forgePool.getStartBlockNo(), curHeight, forgePool.getStartBlockNo());
                }

            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                int curHeight = Conch.getBlockchain().getLastBlock().getHeight();
                Attachment.SharderPoolDestroy destroy = (Attachment.SharderPoolDestroy) transaction.getAttachment();
                SharderPoolProcessor miningPool = SharderPoolProcessor.getPool(destroy.getPoolId());
                miningPool.destroySharderPool(curHeight);
                Account.getAccount(miningPool.getCreatorId()).pocChanged();
            }

            @Override
            public String getName() {
                return "destroyPool";
            }

        };

        public static final TransactionType SHARDER_POOL_JOIN = new SharderPool() {
            @Override
            public boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount) {
                long amountNQT = ((Attachment.SharderPoolJoin) transaction.getAttachment()).getAmount();
                boolean isGenesis = transaction.getTimestamp() == 0 
                                    && Arrays.equals(transaction.getSenderPublicKey(), SharderGenesis.CREATOR_PUBLIC_KEY);
                //Balance and genesis creator check
                if (senderAccount.getUnconfirmedBalanceNQT() < amountNQT && !isGenesis) {
                    return false;
                }
                senderAccount.addFrozen(getLedgerEvent(), transaction.getId(), amountNQT);
                return true;
            }

            @Override
            public void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount) {
                long amountNQT = ((Attachment.SharderPoolJoin) transaction.getAttachment()).getAmount();
                senderAccount.addFrozen(getLedgerEvent(), transaction.getId(), -amountNQT);
            }

            @Override
            public byte getSubtype() {
                return SUBTYPE_SHARDER_POOL_JOIN;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.FORGE_POOL_JOIN;
            }

            @Override
            public AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.SharderPoolJoin(buffer, transactionVersion);
            }

            @Override
            public AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.SharderPoolJoin(attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                int curHeight = Conch.getBlockchain().getLastBlock().getHeight();
                Attachment.SharderPoolJoin join = (Attachment.SharderPoolJoin) transaction.getAttachment();
                SharderPoolProcessor pool = SharderPoolProcessor.getPool(join.getPoolId());
                if (pool == null)  {
                    Logger.logWarningMessage("Can't process join tx[tx id=%d] caused by pool doesn't exists[pool id=%d], maybe PoolCreateTx haven't executed" , transaction.getId(),join.getPoolId());
                    return;
//                    throw new ConchException.NotValidException("Can't process join tx[tx id=%d] caused by pool doesn't exists[pool id=%d], maybe PoolCreateTx haven't executed" , transaction.getId(),join.getPoolId());
                }
                
                int poolStartHeight = pool.getStartBlockNo();
                int poolEndHeight = pool.getEndBlockNo();
                if (curHeight + Constants.SHARDER_POOL_DELAY > poolEndHeight) {
                    Logger.logWarningMessage("Pool[%d] will destroyed at height[%d]  before tx[id=%d applied height[%d]", pool.getPoolId(), poolEndHeight, transaction.getId(),  curHeight);
                    return;
                }
                
                if(curHeight >= Constants.SHARDER_POOL_JOIN_CHECK_BLOCK){
                    if(curHeight < poolStartHeight || (curHeight + Constants.SHARDER_POOL_DELAY) < poolStartHeight) {
                        Logger.logWarningMessage("Pool[%d] will start at height[%d] and current tx[id=%d] apply at height[%d] ", pool.getPoolId(), poolStartHeight , transaction.getId(), curHeight);
                        return;
                    }  
                }
                
                // default level is creator
                // 0-pool creator, 1-investor 
                int level = 0;
                if(SharderPoolProcessor.getCreatorIdByPoolId(join.getPoolId()) != transaction.getSenderId()) {
                    level = 1;
                }
                
                if (!PoolRule.validateConsignor(level, join, pool.getRule())) {
                    Logger.logWarningMessage("current condition in the tx[id=%d] is out of rule ", transaction.getId());
                    return;
                }

                com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
                json.putAll(pool.getRootRuleMap());
                long maxCapacity = json.getJSONObject("consignor").getJSONObject("amount").getLong("max");
                if (join.getAmount() + pool.getPower() > maxCapacity) {
                    Logger.logWarningMessage("exceeding total upper limit of pool");
                    return;
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                int height = Conch.getBlockchain().getLastBlock().getHeight() + Constants.SHARDER_POOL_DELAY;
                Attachment.SharderPoolJoin forgePoolJoin = (Attachment.SharderPoolJoin) transaction.getAttachment();
                long amountNQT = forgePoolJoin.getAmount();
                long poolId = forgePoolJoin.getPoolId();
                long txId = transaction.getId();
                senderAccount.addFrozen(getLedgerEvent(), txId, -amountNQT);
                senderAccount.addFrozenSubBalanceSubUnconfirmed(getLedgerEvent(), txId, amountNQT);
                SharderPoolProcessor miningPool = SharderPoolProcessor.getPool(poolId);
                if(miningPool != null) {
                    height = height > miningPool.getStartBlockNo() ? height : miningPool.getStartBlockNo();
                    miningPool.addOrUpdateConsignor(senderAccount.getId(), transaction.getId(), height, height + forgePoolJoin.getPeriod(), amountNQT);
                    Account.getAccount(miningPool.getCreatorId()).pocChanged();
                }
            }

            @Override
            public String getName() {
                return "joinPool";
            }
        };

        public static final TransactionType SHARDER_POOL_QUIT = new SharderPool() {

            @Override
            public boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount) {
                return true;
            }

            @Override
            public void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount) {

            }

            @Override
            public byte getSubtype() {
                return SUBTYPE_SHARDER_POOL_QUIT;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return AccountLedger.LedgerEvent.FORGE_POOL_QUIT;
            }

            @Override
            public AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
                return new Attachment.SharderPoolQuit(buffer, transactionVersion);
            }

            @Override
            public AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
                return new Attachment.SharderPoolQuit(attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
                //FIXME[pool] unconfirmed transaction already has this kind of transaction
                int curHeight = Conch.getBlockchain().getLastBlock().getHeight();
                Attachment.SharderPoolQuit quit = (Attachment.SharderPoolQuit) transaction.getAttachment();
                long poolId = quit.getPoolId();
                SharderPoolProcessor sharderPool = SharderPoolProcessor.getPool(poolId);
                if (sharderPool == null) {
                    Logger.logWarningMessage("sharder pool " + poolId + " doesn't exists, ignore this tx[id=%d]", transaction.getId());
                    return;
                }
                boolean ownerQuit = sharderPool.getCreatorId() == transaction.getSenderId();
                if (!ownerQuit && !sharderPool.hasSenderAndTransaction(transaction.getSenderId(), quit.getTxId())) {
                    Logger.logWarningMessage("sharder pool doesn't have the related join tx, txId:" + quit.getTxId() + ", poolId:" + poolId + ",sender " + transaction.getSenderId());
                    return;
                }
                if (curHeight + Constants.SHARDER_POOL_DELAY > sharderPool.getEndBlockNo()) {
                    Logger.logWarningMessage("sharder pool will be destroyed at " + sharderPool.getEndBlockNo() + " before transaction apply at " + curHeight);
                    return;
                }
                if (!PoolRule.validateConsignor(SharderPoolProcessor.getPool(poolId).getCreatorId(), quit, sharderPool.getRule())) {
                    Logger.logWarningMessage("current condition is out of rule");
                    return;
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.SharderPoolQuit sharderPoolQuit = (Attachment.SharderPoolQuit) transaction.getAttachment();
                long poolId = sharderPoolQuit.getPoolId();
                SharderPoolProcessor miningPool = SharderPoolProcessor.getPool(poolId);
                if (miningPool == null) {
                    Logger.logWarningMessage("sharder pool " + poolId + " doesn't exists, ignore this tx[id=%d]", transaction.getId());
                    return;
                }

                try{
                    long amountNQT = miningPool.quitConsignor(senderAccount.getId(), sharderPoolQuit.getTxId());
                    if (amountNQT > 0) {
                        senderAccount.addFrozenSubBalanceSubUnconfirmed(getLedgerEvent(), transaction.getId(), amountNQT);
                    }
                    Account.getAccount(miningPool.getCreatorId()).pocChanged();
                }catch(Account.DoubleSpendingException e) {
                    if(!CheckSumValidator.isDirtyPoolTx(Conch.getHeight(), senderAccount.getId())) {
                        throw e;
                    }
                }
            }

            @Override
            public String getName() {
                return "quitPool";
            }
        };
    }

}
