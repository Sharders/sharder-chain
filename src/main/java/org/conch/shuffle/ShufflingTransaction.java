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

package org.conch.shuffle;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.asset.Asset;
import org.conch.asset.HoldingType;
import org.conch.asset.MonetaryTx;
import org.conch.asset.token.Currency;
import org.conch.asset.token.CurrencyType;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.crypto.Crypto;
import org.conch.tx.*;
import org.conch.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ShufflingTransaction extends TransactionType {

    private static final byte SUBTYPE_SHUFFLING_CREATION = 0;
    private static final byte SUBTYPE_SHUFFLING_REGISTRATION = 1;
    private static final byte SUBTYPE_SHUFFLING_PROCESSING = 2;
    private static final byte SUBTYPE_SHUFFLING_RECIPIENTS = 3;
    private static final byte SUBTYPE_SHUFFLING_VERIFICATION = 4;
    private static final byte SUBTYPE_SHUFFLING_CANCELLATION = 5;

    public static TransactionType findTransactionType(byte subtype) {
        switch (subtype) {
            case SUBTYPE_SHUFFLING_CREATION:
                return SHUFFLING_CREATION;
            case SUBTYPE_SHUFFLING_REGISTRATION:
                return SHUFFLING_REGISTRATION;
            case SUBTYPE_SHUFFLING_PROCESSING:
                return SHUFFLING_PROCESSING;
            case SUBTYPE_SHUFFLING_RECIPIENTS:
                return SHUFFLING_RECIPIENTS;
            case SUBTYPE_SHUFFLING_VERIFICATION:
                return SHUFFLING_VERIFICATION;
            case SUBTYPE_SHUFFLING_CANCELLATION:
                return SHUFFLING_CANCELLATION;
            default:
                return null;
        }
    }

    private final static Fee SHUFFLING_PROCESSING_FEE = new Fee.ConstantFee(10 * Constants.ONE_SS);
    private final static Fee SHUFFLING_RECIPIENTS_FEE = new Fee.ConstantFee(11 * Constants.ONE_SS);


    private ShufflingTransaction() {}

    @Override
    public final byte getType() {
        return TYPE_SHUFFLING;
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }


    public static final TransactionType SHUFFLING_CREATION = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_CREATION;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.SHUFFLING_REGISTRATION;
        }

        @Override
        public String getName() {
            return "ShufflingCreation";
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return new Attachment.ShufflingCreation(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new Attachment.ShufflingCreation(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.ShufflingCreation attachment = (Attachment.ShufflingCreation) transaction.getAttachment();
            HoldingType holdingType = attachment.getHoldingType();
            long amount = attachment.getAmount();
            if (holdingType == HoldingType.SS) {
                if (amount < Constants.SHUFFLING_DEPOSIT_NQT || amount > Constants.MAX_BALANCE_NQT) {
                    throw new ConchException.NotValidException("Invalid NQT amount " + amount
                            + ", minimum is " + Constants.SHUFFLING_DEPOSIT_NQT);
                }
            } else if (holdingType == HoldingType.ASSET) {
                Asset asset = Asset.getAsset(attachment.getHoldingId());
                if (asset == null) {
                    throw new ConchException.NotCurrentlyValidException("Unknown asset " + Long.toUnsignedString(attachment.getHoldingId()));
                }
                if (amount <= 0 || amount > asset.getInitialQuantityQNT()) {
                    throw new ConchException.NotValidException("Invalid asset quantity " + amount);
                }
            } else if (holdingType == HoldingType.CURRENCY) {
                Currency currency = Currency.getCurrency(attachment.getHoldingId());
                CurrencyType.validate(currency, transaction);
                if (!currency.isActive()) {
                    throw new ConchException.NotCurrentlyValidException("Currency is not active: " + currency.getCode());
                }
                if (amount <= 0 || amount > Constants.MAX_CURRENCY_TOTAL_SUPPLY) {
                    throw new ConchException.NotValidException("Invalid currency amount " + amount);
                }
            } else {
                throw new RuntimeException("Unsupported holding type " + holdingType);
            }
            if (attachment.getParticipantCount() < Constants.MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS
                    || attachment.getParticipantCount() > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS) {
                throw new ConchException.NotValidException(String.format("Number of participants %d is not between %d and %d",
                        attachment.getParticipantCount(), Constants.MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS, Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS));
            }
            if (attachment.getRegistrationPeriod() < 1 || attachment.getRegistrationPeriod() > Constants.MAX_SHUFFLING_REGISTRATION_PERIOD) {
                throw new ConchException.NotValidException("Invalid registration period: " + attachment.getRegistrationPeriod());
            }
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.ShufflingCreation attachment = (Attachment.ShufflingCreation) transaction.getAttachment();
            HoldingType holdingType = attachment.getHoldingType();
            if (holdingType != HoldingType.SS) {
                if (holdingType.getUnconfirmedBalance(senderAccount, attachment.getHoldingId()) >= attachment.getAmount()
                        && senderAccount.getUnconfirmedBalanceNQT() >= Constants.SHUFFLING_DEPOSIT_NQT) {
                    holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getHoldingId(), -attachment.getAmount());
                    senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), -Constants.SHUFFLING_DEPOSIT_NQT);
                    return true;
                }
            } else {
                if (senderAccount.getUnconfirmedBalanceNQT() >= attachment.getAmount()) {
                    senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), -attachment.getAmount());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.ShufflingCreation attachment = (Attachment.ShufflingCreation) transaction.getAttachment();
            Shuffling.addShuffling(transaction, attachment);
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.ShufflingCreation attachment = (Attachment.ShufflingCreation) transaction.getAttachment();
            HoldingType holdingType = attachment.getHoldingType();
            if (holdingType != HoldingType.SS) {
                holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getHoldingId(), attachment.getAmount());
                senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), Constants.SHUFFLING_DEPOSIT_NQT);
            } else {
                senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), attachment.getAmount());
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.ShufflingCreation attachment = (Attachment.ShufflingCreation) transaction.getAttachment();
            if (attachment.getHoldingType() != HoldingType.CURRENCY) {
                return false;
            }
            Currency currency = Currency.getCurrency(attachment.getHoldingId());
            String nameLower = currency.getName().toLowerCase();
            String codeLower = currency.getCode().toLowerCase();
            boolean isDuplicate = isDuplicate(MonetaryTx.CURRENCY_ISSUANCE, nameLower, duplicates, false);
            if (! nameLower.equals(codeLower)) {
                isDuplicate = isDuplicate || isDuplicate(MonetaryTx.CURRENCY_ISSUANCE, codeLower, duplicates, false);
            }
            return isDuplicate;
        }

    };

    public static final TransactionType SHUFFLING_REGISTRATION = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_REGISTRATION;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.SHUFFLING_REGISTRATION;
        }

        @Override
        public String getName() {
            return "ShufflingRegistration";
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return new Attachment.ShufflingRegistration(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new Attachment.ShufflingRegistration(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.ShufflingRegistration attachment = (Attachment.ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new ConchException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            byte[] shufflingStateHash = shuffling.getStateHash();
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new ConchException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }
            if (shuffling.getStage() != Shuffling.Stage.REGISTRATION) {
                throw new ConchException.NotCurrentlyValidException("Shuffling registration has ended for " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            if (shuffling.getParticipant(transaction.getSenderId()) != null) {
                throw new ConchException.NotCurrentlyValidException(String.format("Account %s is already registered for shuffling %s",
                        Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (Conch.getBlockchain().getHeight() + shuffling.getBlocksRemaining() <= attachment.getFinishValidationHeight(transaction)) {
                throw new ConchException.NotCurrentlyValidException("Shuffling registration finishes in " + shuffling.getBlocksRemaining() + " blocks");
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.ShufflingRegistration attachment = (Attachment.ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            return isDuplicate(SHUFFLING_REGISTRATION,
                    Long.toUnsignedString(shuffling.getId()) + "." + Long.toUnsignedString(transaction.getSenderId()), duplicates, true)
                    || isDuplicate(SHUFFLING_REGISTRATION,
                    Long.toUnsignedString(shuffling.getId()), duplicates, shuffling.getParticipantCount() - shuffling.getRegistrantCount());
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.ShufflingRegistration attachment = (Attachment.ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            HoldingType holdingType = shuffling.getHoldingType();
            if (holdingType != HoldingType.SS) {
                if (holdingType.getUnconfirmedBalance(senderAccount, shuffling.getHoldingId()) >= shuffling.getAmount()
                        && senderAccount.getUnconfirmedBalanceNQT() >= Constants.SHUFFLING_DEPOSIT_NQT) {
                    holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getHoldingId(), -shuffling.getAmount());
                    senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), -Constants.SHUFFLING_DEPOSIT_NQT);
                    return true;
                }
            } else {
                if (senderAccount.getUnconfirmedBalanceNQT() >= shuffling.getAmount()) {
                    senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), -shuffling.getAmount());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.ShufflingRegistration attachment = (Attachment.ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            shuffling.addParticipant(transaction.getSenderId());
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            Attachment.ShufflingRegistration attachment = (Attachment.ShufflingRegistration) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            HoldingType holdingType = shuffling.getHoldingType();
            if (holdingType != HoldingType.SS) {
                holdingType.addToUnconfirmedBalance(senderAccount, getLedgerEvent(), transaction.getId(), shuffling.getHoldingId(), shuffling.getAmount());
                senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), Constants.SHUFFLING_DEPOSIT_NQT);
            } else {
                senderAccount.addToUnconfirmedNQT(getLedgerEvent(), transaction.getId(), shuffling.getAmount());
            }
        }

    };

    public static final TransactionType SHUFFLING_PROCESSING = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_PROCESSING;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.SHUFFLING_PROCESSING;
        }

        @Override
        public String getName() {
            return "ShufflingProcessing";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return SHUFFLING_PROCESSING_FEE;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.ShufflingProcessing(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return new Attachment.ShufflingProcessing(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.ShufflingProcessing attachment = (Attachment.ShufflingProcessing)transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new ConchException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            if (shuffling.getStage() != Shuffling.Stage.PROCESSING) {
                throw new ConchException.NotCurrentlyValidException(String.format("Shuffling %s is not in processing stage",
                        Long.toUnsignedString(attachment.getShufflingId())));
            }
            ShufflingParticipant participant = shuffling.getParticipant(transaction.getSenderId());
            if (participant == null) {
                throw new ConchException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                        Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (!participant.getState().canBecome(ShufflingParticipant.State.PROCESSED)) {
                throw new ConchException.NotCurrentlyValidException(String.format("Participant %s processing already complete",
                        Long.toUnsignedString(transaction.getSenderId())));
            }
            if (participant.getAccountId() != shuffling.getAssigneeAccountId()) {
                throw new ConchException.NotCurrentlyValidException(String.format("Participant %s is not currently assigned to process shuffling %s",
                        Long.toUnsignedString(participant.getAccountId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (participant.getNextAccountId() == 0) {
                throw new ConchException.NotValidException(String.format("Participant %s is last in shuffle",
                        Long.toUnsignedString(transaction.getSenderId())));
            }
            byte[] shufflingStateHash = shuffling.getStateHash();
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new ConchException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }
            byte[][] data = attachment.getData();
            if (data == null && Conch.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                throw new ConchException.NotCurrentlyValidException("Data has been pruned prematurely");
            }
            if (data != null) {
                if (data.length != participant.getIndex() + 1 && data.length != 0) {
                    throw new ConchException.NotValidException(String.format("Invalid number of encrypted data %d for participant number %d",
                            data.length, participant.getIndex()));
                }
                byte[] previous = null;
                for (byte[] bytes : data) {
                    if (bytes.length != 32 + 64 * (shuffling.getParticipantCount() - participant.getIndex() - 1)) {
                        throw new ConchException.NotValidException("Invalid encrypted data length " + bytes.length);
                    }
                    if (previous != null && Convert.byteArrayComparator.compare(previous, bytes) >= 0) {
                        throw new ConchException.NotValidException("Duplicate or unsorted encrypted data");
                    }
                    previous = bytes;
                }
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.ShufflingProcessing attachment = (Attachment.ShufflingProcessing) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            return isDuplicate(SHUFFLING_PROCESSING, Long.toUnsignedString(shuffling.getId()), duplicates, true);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.ShufflingProcessing attachment = (Attachment.ShufflingProcessing)transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            shuffling.updateParticipantData(transaction, attachment);
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

        @Override
        public boolean isPhasable() {
            return false;
        }

        @Override
        public boolean isPruned(long transactionId) {
            Transaction transaction = TransactionDb.findTransaction(transactionId);
            Attachment.ShufflingProcessing attachment = (Attachment.ShufflingProcessing)transaction.getAttachment();
            return ShufflingParticipant.getData(attachment.getShufflingId(), transaction.getSenderId()) == null;
        }

    };

    public static final TransactionType SHUFFLING_RECIPIENTS = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_RECIPIENTS;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.SHUFFLING_PROCESSING;
        }

        @Override
        public String getName() {
            return "ShufflingRecipients";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return SHUFFLING_RECIPIENTS_FEE;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.ShufflingRecipients(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new Attachment.ShufflingRecipients(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.ShufflingRecipients attachment = (Attachment.ShufflingRecipients)transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new ConchException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            if (shuffling.getStage() != Shuffling.Stage.PROCESSING) {
                throw new ConchException.NotCurrentlyValidException(String.format("Shuffling %s is not in processing stage",
                        Long.toUnsignedString(attachment.getShufflingId())));
            }
            ShufflingParticipant participant = shuffling.getParticipant(transaction.getSenderId());
            if (participant == null) {
                throw new ConchException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                        Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (participant.getNextAccountId() != 0) {
                throw new ConchException.NotValidException(String.format("Participant %s is not last in shuffle",
                        Long.toUnsignedString(transaction.getSenderId())));
            }
            if (!participant.getState().canBecome(ShufflingParticipant.State.PROCESSED)) {
                throw new ConchException.NotCurrentlyValidException(String.format("Participant %s processing already complete",
                        Long.toUnsignedString(transaction.getSenderId())));
            }
            if (participant.getAccountId() != shuffling.getAssigneeAccountId()) {
                throw new ConchException.NotCurrentlyValidException(String.format("Participant %s is not currently assigned to process shuffling %s",
                        Long.toUnsignedString(participant.getAccountId()), Long.toUnsignedString(shuffling.getId())));
            }
            byte[] shufflingStateHash = shuffling.getStateHash();
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new ConchException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }
            byte[][] recipientPublicKeys = attachment.getRecipientPublicKeys();
            if (recipientPublicKeys.length != shuffling.getParticipantCount() && recipientPublicKeys.length != 0) {
                throw new ConchException.NotValidException(String.format("Invalid number of recipient public keys %d", recipientPublicKeys.length));
            }
            Set<Long> recipientAccounts = new HashSet<>(recipientPublicKeys.length);
            for (byte[] recipientPublicKey : recipientPublicKeys) {
                if (!Crypto.isCanonicalPublicKey(recipientPublicKey)) {
                    throw new ConchException.NotValidException("Invalid recipient public key " + Convert.toHexString(recipientPublicKey));
                }
                if (!recipientAccounts.add(Account.getId(recipientPublicKey))) {
                    throw new ConchException.NotValidException("Duplicate recipient accounts");
                }
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.ShufflingRecipients attachment = (Attachment.ShufflingRecipients) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            return isDuplicate(SHUFFLING_PROCESSING, Long.toUnsignedString(shuffling.getId()), duplicates, true);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.ShufflingRecipients attachment = (Attachment.ShufflingRecipients)transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            shuffling.updateRecipients(transaction, attachment);
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

        @Override
        public boolean isPhasable() {
            return false;
        }

    };

    public static final TransactionType SHUFFLING_VERIFICATION = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_VERIFICATION;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.SHUFFLING_PROCESSING;
        }

        @Override
        public String getName() {
            return "ShufflingVerification";
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return new Attachment.ShufflingVerification(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new Attachment.ShufflingVerification(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.ShufflingVerification attachment = (Attachment.ShufflingVerification) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new ConchException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            if (shuffling.getStage() != Shuffling.Stage.VERIFICATION) {
                throw new ConchException.NotCurrentlyValidException("Shuffling not in verification stage: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            ShufflingParticipant participant = shuffling.getParticipant(transaction.getSenderId());
            if (participant == null) {
                throw new ConchException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                        Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (!participant.getState().canBecome(ShufflingParticipant.State.VERIFIED)) {
                throw new ConchException.NotCurrentlyValidException(String.format("Shuffling participant %s in state %s cannot become verified",
                        Long.toUnsignedString(attachment.getShufflingId()), participant.getState()));
            }
            if (participant.getIndex() == shuffling.getParticipantCount() - 1) {
                throw new ConchException.NotValidException("Last participant cannot submit verification transaction");
            }
            byte[] shufflingStateHash = shuffling.getStateHash();
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new ConchException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.ShufflingVerification attachment = (Attachment.ShufflingVerification) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            return isDuplicate(SHUFFLING_VERIFICATION,
                    Long.toUnsignedString(shuffling.getId()) + "." + Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.ShufflingVerification attachment = (Attachment.ShufflingVerification) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            shuffling.verify(transaction.getSenderId());
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public boolean isPhasable() {
            return false;
        }

    };

    public static final TransactionType SHUFFLING_CANCELLATION = new ShufflingTransaction() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_SHUFFLING_CANCELLATION;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.SHUFFLING_PROCESSING;
        }

        @Override
        public String getName() {
            return "ShufflingCancellation";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return SHUFFLING_PROCESSING_FEE;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.ShufflingCancellation(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new Attachment.ShufflingCancellation(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.ShufflingCancellation attachment = (Attachment.ShufflingCancellation) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            if (shuffling == null) {
                throw new ConchException.NotCurrentlyValidException("Shuffling not found: " + Long.toUnsignedString(attachment.getShufflingId()));
            }
            long cancellingAccountId = attachment.getCancellingAccountId();
            if (cancellingAccountId == 0 && !shuffling.getStage().canBecome(Shuffling.Stage.BLAME)) {
                throw new ConchException.NotCurrentlyValidException(String.format("Shuffling in state %s cannot be cancelled", shuffling.getStage()));
            }
            if (cancellingAccountId != 0 && cancellingAccountId != shuffling.getAssigneeAccountId()) {
                throw new ConchException.NotCurrentlyValidException(String.format("Shuffling %s is not currently being cancelled by account %s",
                        Long.toUnsignedString(shuffling.getId()), Long.toUnsignedString(cancellingAccountId)));
            }
            ShufflingParticipant participant = shuffling.getParticipant(transaction.getSenderId());
            if (participant == null) {
                throw new ConchException.NotCurrentlyValidException(String.format("Account %s is not registered for shuffling %s",
                        Long.toUnsignedString(transaction.getSenderId()), Long.toUnsignedString(shuffling.getId())));
            }
            if (!participant.getState().canBecome(ShufflingParticipant.State.CANCELLED)) {
                throw new ConchException.NotCurrentlyValidException(String.format("Shuffling participant %s in state %s cannot submit cancellation",
                        Long.toUnsignedString(attachment.getShufflingId()), participant.getState()));
            }
            if (participant.getIndex() == shuffling.getParticipantCount() - 1) {
                throw new ConchException.NotValidException("Last participant cannot submit cancellation transaction");
            }
            byte[] shufflingStateHash = shuffling.getStateHash();
            if (shufflingStateHash == null || !Arrays.equals(shufflingStateHash, attachment.getShufflingStateHash())) {
                throw new ConchException.NotCurrentlyValidException("Shuffling state hash doesn't match");
            }
            Transaction dataProcessingTransaction = TransactionDb.findTransactionByFullHash(participant.getDataTransactionFullHash(), Conch.getBlockchain().getHeight());
            if (dataProcessingTransaction == null) {
                throw new ConchException.NotCurrentlyValidException("Invalid data transaction full hash");
            }
            Attachment.ShufflingProcessing shufflingProcessing = (Attachment.ShufflingProcessing) dataProcessingTransaction.getAttachment();
            if (!Arrays.equals(shufflingProcessing.getHash(), attachment.getHash())) {
                throw new ConchException.NotValidException("Blame data hash doesn't match processing data hash");
            }
            byte[][] keySeeds = attachment.getKeySeeds();
            if (keySeeds.length != shuffling.getParticipantCount() - participant.getIndex() - 1) {
                throw new ConchException.NotValidException("Invalid number of revealed keySeeds: " + keySeeds.length);
            }
            for (byte[] keySeed : keySeeds) {
                if (keySeed.length != 32) {
                    throw new ConchException.NotValidException("Invalid keySeed: " + Convert.toHexString(keySeed));
                }
            }
        }

        @Override
        public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
            Attachment.ShufflingCancellation attachment = (Attachment.ShufflingCancellation) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            return isDuplicate(SHUFFLING_VERIFICATION, // use VERIFICATION for unique type
                    Long.toUnsignedString(shuffling.getId()) + "." + Long.toUnsignedString(transaction.getSenderId()), duplicates, true);
        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.ShufflingCancellation attachment = (Attachment.ShufflingCancellation) transaction.getAttachment();
            Shuffling shuffling = Shuffling.getShuffling(attachment.getShufflingId());
            ShufflingParticipant participant = ShufflingParticipant.getParticipant(shuffling.getId(), senderAccount.getId());
            shuffling.cancelBy(participant, attachment.getBlameData(), attachment.getKeySeeds());
        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {}

        @Override
        public boolean isPhasable() {
            return false;
        }

    };

}
