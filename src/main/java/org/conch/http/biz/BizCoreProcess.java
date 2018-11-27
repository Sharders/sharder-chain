package org.conch.http.biz;

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.crypto.Crypto;
import org.conch.http.ParameterException;
import org.conch.http.biz.domain.Transaction;
import org.conch.http.biz.exception.BizParameterException;
import org.conch.tx.Appendix;
import org.conch.tx.Attachment;
import org.conch.tx.PhasingParams;
import org.conch.util.Convert;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.conch.http.JSONResponses.INCORRECT_LINKED_FULL_HASH;
import static org.conch.http.JSONResponses.INCORRECT_WHITELIST;

public class BizCoreProcess {

    public static Transaction createTransaction(HashMap<String, String> params, Account senderAccount, long recipientId,
                                                long amountNQT, Attachment attachment) throws ConchException.NotValidException, ParameterException {
        String deadlineValue = params.get("deadline");
        String referencedTransactionFullHash = Convert.emptyToNull(params.get("referencedTransactionFullHash"));
        String secretPhrase = BizParameterParser.getSecretPhrase(params, false);
        String publicKeyValue = Convert.emptyToNull(params.get("publicKey"));
        boolean broadcast = !"false".equalsIgnoreCase(params.get("broadcast")) && secretPhrase != null;
        Appendix.EncryptedMessage encryptedMessage = null;
        Appendix.PrunableEncryptedMessage prunableEncryptedMessage = null;
        if (attachment.getTransactionType().canHaveRecipient() && recipientId != 0) {
            Account recipient = Account.getAccount(recipientId);
            if ("true".equalsIgnoreCase(params.get("encryptedMessageIsPrunable"))) {
                prunableEncryptedMessage = (Appendix.PrunableEncryptedMessage) BizParameterParser.getEncryptedMessage(params, recipient, true);
            } else {
                encryptedMessage = (Appendix.EncryptedMessage) BizParameterParser.getEncryptedMessage(params, recipient, false);
            }
        }
        Appendix.EncryptToSelfMessage encryptToSelfMessage = BizParameterParser.getEncryptToSelfMessage(params);
        Appendix.Message message = null;
        Appendix.PrunablePlainMessage prunablePlainMessage = null;
        if ("true".equalsIgnoreCase(params.get("messageIsPrunable"))) {
            prunablePlainMessage = (Appendix.PrunablePlainMessage) BizParameterParser.getPlainMessage(params, true);
        } else {
            message = (Appendix.Message) BizParameterParser.getPlainMessage(params, false);
        }
        Appendix.PublicKeyAnnouncement publicKeyAnnouncement = null;
        String recipientPublicKey = Convert.emptyToNull(params.get("recipientPublicKey"));
        if (recipientPublicKey != null) {
            publicKeyAnnouncement = new Appendix.PublicKeyAnnouncement(Convert.parseHexString(recipientPublicKey));
        }

        Appendix.Phasing phasing = null;
        boolean phased = "true".equalsIgnoreCase(params.get("phased"));
        if (phased) {
            phasing = parsePhasing(params);
        }

        if (secretPhrase == null && publicKeyValue == null) {
            throw new BizParameterException("secretPhrase not specified or not submitted to the remote node");
        } else if (deadlineValue == null) {
            throw new BizParameterException("deadline not specified or not submitted to the remote node");
        }

        short deadline;
        try {
            deadline = Short.parseShort(deadlineValue);
            if (deadline < 1) {
                throw new BizParameterException("deadline Incorrect");
            }
        } catch (NumberFormatException e) {
            throw new BizParameterException("deadline Incorrect");
        }

        long feeNQT = BizParameterParser.getFeeNQT(params);
        int ecBlockHeight = BizParameterParser.getInt(params, "ecBlockHeight", 0, Integer.MAX_VALUE, false);
        long ecBlockId = BizParameterParser.getUnsignedLong(params, "ecBlockId", false);
        if (ecBlockId != 0 && ecBlockId != Conch.getBlockchain().getBlockIdAtHeight(ecBlockHeight)) {
            throw new BizParameterException("ecBlockId does not match the block id at ecBlockHeight");
        }
        if (ecBlockId == 0 && ecBlockHeight > 0) {
            ecBlockId = Conch.getBlockchain().getBlockIdAtHeight(ecBlockHeight);
        }
        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        byte[] publicKey = secretPhrase != null ? Crypto.getPublicKey(secretPhrase) : Convert.parseHexString(publicKeyValue);
        Transaction biz_transaction = new Transaction();
        org.conch.tx.Transaction.Builder builder = Conch.newTransactionBuilder(publicKey, amountNQT, feeNQT,
                deadline, attachment).referencedTransactionFullHash(referencedTransactionFullHash);
        if (attachment.getTransactionType().canHaveRecipient()) {
            builder.recipientId(recipientId);
        }
        builder.appendix(encryptedMessage);
        builder.appendix(message);
        builder.appendix(publicKeyAnnouncement);
        builder.appendix(encryptToSelfMessage);
        builder.appendix(phasing);
        builder.appendix(prunablePlainMessage);
        builder.appendix(prunableEncryptedMessage);
        if (ecBlockId != 0) {
            builder.ecBlockId(ecBlockId);
            builder.ecBlockHeight(ecBlockHeight);
        }
        org.conch.tx.Transaction transaction = builder.build(secretPhrase);
        try {
            if (Math.addExact(amountNQT, transaction.getFeeNQT()) > senderAccount.getUnconfirmedBalanceNQT()) {
                throw new BizParameterException("Not enough funds");
            }
        } catch (ArithmeticException e) {
            throw new BizParameterException("Not enough funds");
        }
        biz_transaction.setTransactionId(transaction.getStringId());
        biz_transaction.setHash(transaction.getFullHash());
        biz_transaction.setIndex(transaction.getIndex());
        biz_transaction.setHeight(transaction.getHeight());
        biz_transaction.setAmount(BigDecimal.valueOf(transaction.getAmountNQT()));
        biz_transaction.setFee(BigDecimal.valueOf(transaction.getFeeNQT()));
        biz_transaction.setRecipient(String.valueOf(transaction.getRecipientId()));
        biz_transaction.setConfirmations(0);
        return biz_transaction;
    }

    private static Appendix.Phasing parsePhasing(HashMap<String, String> params) throws ParameterException {
        int finishHeight = BizParameterParser.getInt(params, "phasingFinishHeight",
                Conch.getBlockchain().getHeight() + 1,
                Conch.getBlockchain().getHeight() + Constants.MAX_PHASING_DURATION + 1,
                true);

        PhasingParams phasingParams = parsePhasingParams(params, "phasing");

        byte[][] linkedFullHashes = null;
        String[] linkedFullHashesValues = params.get("phasingLinkedFullHash").split("");
        if (linkedFullHashesValues != null && linkedFullHashesValues.length > 0) {
            linkedFullHashes = new byte[linkedFullHashesValues.length][];
            for (int i = 0; i < linkedFullHashes.length; i++) {
                linkedFullHashes[i] = Convert.parseHexString(linkedFullHashesValues[i]);
                if (Convert.emptyToNull(linkedFullHashes[i]) == null || linkedFullHashes[i].length != 32) {
                    throw new ParameterException(INCORRECT_LINKED_FULL_HASH);
                }
            }
        }

        byte[] hashedSecret = Convert.parseHexString(Convert.emptyToNull(params.get("phasingHashedSecret")));
        byte algorithm = BizParameterParser.getByte(params, "phasingHashedSecretAlgorithm", (byte) 0, Byte.MAX_VALUE, false);

        return new Appendix.Phasing(finishHeight, phasingParams, linkedFullHashes, hashedSecret, algorithm);
    }

    static final PhasingParams parsePhasingParams(HashMap<String, String> params, String parameterPrefix) throws ParameterException {
        byte votingModel = BizParameterParser.getByte(params, parameterPrefix + "VotingModel", (byte)-1, (byte)5, true);
        long quorum = BizParameterParser.getLong(params, parameterPrefix + "Quorum", 0, Long.MAX_VALUE, false);
        long minBalance = BizParameterParser.getLong(params, parameterPrefix + "MinBalance", 0, Long.MAX_VALUE, false);
        byte minBalanceModel = BizParameterParser.getByte(params, parameterPrefix + "MinBalanceModel", (byte)0, (byte)3, false);
        long holdingId = BizParameterParser.getUnsignedLong(params, parameterPrefix + "Holding", false);
        long[] whitelist = null;
        String[] whitelistValues = params.get(parameterPrefix + "Whitelisted").split("&");
        if (whitelistValues != null && whitelistValues.length > 0) {
            whitelist = new long[whitelistValues.length];
            for (int i = 0; i < whitelistValues.length; i++) {
                whitelist[i] = Convert.parseAccountId(whitelistValues[i]);
                if (whitelist[i] == 0) {
                    throw new ParameterException(INCORRECT_WHITELIST);
                }
            }
        }
        return new PhasingParams(votingModel, holdingId, quorum, minBalance, minBalanceModel, whitelist);
    }
}
