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

package org.conch.consensus.poc.tx;

import org.apache.commons.lang3.StringUtils;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.chain.BlockchainImpl;
import org.conch.common.ConchException;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.tx.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public abstract class PocTx extends TransactionType {

    private static final byte SUBTYPE_POC_NODE_CONFIGURATION = 0; // 节点配置
    private static final byte SUBTYPE_POC_WEIGHT = 1; // 权重
    private static final byte SUBTYPE_POC_ONLINE_RATE = 2; // 在线率
    private static final byte SUBTYPE_POC_BLOCKING_MISS = 3; // 出块丢失
    private static final byte SUBTYPE_POC_BIFURACTION_OF_CONVERGENCE = 4; // 分叉收敛

    public static TransactionType findTxType(byte subtype) {
        switch (subtype) {
            case SUBTYPE_POC_NODE_CONFIGURATION:
                return POC_NODE_CONFIGURATION;
            case SUBTYPE_POC_WEIGHT:
                return POC_WEIGHT;
            case SUBTYPE_POC_ONLINE_RATE:
                return POC_ONLINE_RATE;
            case SUBTYPE_POC_BLOCKING_MISS:
                return POC_BLOCKING_MISS;
            case SUBTYPE_POC_BIFURACTION_OF_CONVERGENCE:
                return POC_BIFURACTION_OF_CONVERGENCE;
            default:
                return null;
        }
    }


    private PocTx() {}

    public static final TransactionType POC_NODE_CONFIGURATION = new PocTx() {
        @Override
        public boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount) {

        }

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_NODE_CONFIGURATION;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.POC_NODE_CONFIGURATION;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.PocNodeConfiguration(buffer,transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return new Attachment.PocNodeConfiguration(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.PocNodeConfiguration pocNodeConfiguration = (Attachment.PocNodeConfiguration) transaction.getAttachment();
            if (pocNodeConfiguration == null) {
                throw new ConchException.NotValidException("Invalid pocNodeConfiguration: null");
            }
            if (pocNodeConfiguration.getNodeId() == null) {
                throw new ConchException.NotValidException("Invalid nodeId: null");
            }
            if (StringUtils.isBlank(pocNodeConfiguration.getDevice())) {
                throw new ConchException.NotValidException("Invalid device: null or empty");
            }
            if (pocNodeConfiguration.getConfiguration() == null || pocNodeConfiguration.getConfiguration().isEmpty()) {
                throw new ConchException.NotValidException("Invalid configuration: null or empty");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocNodeConfiguration pocNodeConfiguration = (Attachment.PocNodeConfiguration) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl
            long pocFee = transaction.getFeeNQT() - ((TransactionImpl) transaction).getMinimumFeeNQT(BlockchainImpl.getInstance().getHeight());
            Account account = Account.getAccount(transaction.getSenderId());
            account.frozenNQT(AccountLedger.LedgerEvent.POC_NODE_CONFIGURATION, transaction.getId(), pocFee);
        }

        @Override
        public String getName() {
            return "nodeConfiguration";
        }
    };

    public static final TransactionType POC_WEIGHT = new PocTx() {
        @Override
        public boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount) {

        }

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_WEIGHT;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.POC_WEIGHT;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.PocWeight(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return new Attachment.PocWeight(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.PocWeight pocWeight = (Attachment.PocWeight) transaction.getAttachment();
            if (pocWeight == null) {
                throw new ConchException.NotValidException("Invalid pocWeight: null");
            }
            if (pocWeight.getConfigWeight() == null) {
                throw new ConchException.NotValidException("Invalid configWeight: null");
            }
            if (pocWeight.getNetworkWeight() == null) {
                throw new ConchException.NotValidException("Invalid networkWeight: null");
            }
            if (pocWeight.getNodeId() == null) {
                throw new ConchException.NotValidException("Invalid nodeId: null");
            }
            if (pocWeight.getNodeWeight() == null) {
                throw new ConchException.NotValidException("Invalid nodeWeight: null");
            }
            if (pocWeight.getServerWeight() == null) {
                throw new ConchException.NotValidException("Invalid serverWeight: null");
            }
            if (pocWeight.getSsHoldWeight() == null) {
                throw new ConchException.NotValidException("Invalid ssHoldWeight: null");
            }
            if (pocWeight.getTpWeight() == null) {
                throw new ConchException.NotValidException("Invalid tpWeight: null");
            }
            if (StringUtils.isBlank(pocWeight.getDevice())) {
                throw new ConchException.NotValidException("Invalid device: null or empty");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocWeight pocWeight = (Attachment.PocWeight) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl
            long pocFee = transaction.getFeeNQT() - ((TransactionImpl) transaction).getMinimumFeeNQT(BlockchainImpl.getInstance().getHeight());
            Account account = Account.getAccount(transaction.getSenderId());
            account.frozenNQT(AccountLedger.LedgerEvent.POC_WEIGHT, transaction.getId(), pocFee);
        }

        @Override
        public String getName() {
            return "weight";
        }
    };

    public static final TransactionType POC_ONLINE_RATE = new PocTx() {
        @Override
        public boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount) {

        }

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_ONLINE_RATE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.POC_ONLINE_RATE;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.PocOnlineRate(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return new Attachment.PocOnlineRate(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.PocOnlineRate pocOnlineRate = (Attachment.PocOnlineRate) transaction.getAttachment();
            if (pocOnlineRate == null) {
                throw new ConchException.NotValidException("Invalid pocOnlineRate: null");
            }
            if (pocOnlineRate.getNodeId() == null) {
                throw new ConchException.NotValidException("Invalid nodeId: null");
            }
            if (StringUtils.isBlank(pocOnlineRate.getDevice())) {
                throw new ConchException.NotValidException("Invalid device: null or empty");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocOnlineRate pocOnlineRate = (Attachment.PocOnlineRate) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl
            long pocFee = transaction.getFeeNQT() - ((TransactionImpl) transaction).getMinimumFeeNQT(BlockchainImpl.getInstance().getHeight());
            Account account = Account.getAccount(transaction.getSenderId());
            account.frozenNQT(AccountLedger.LedgerEvent.POC_ONLINE_RATE, transaction.getId(), pocFee);
        }

        @Override
        public String getName() {
            return "onlineRate";
        }
    };

    public static final TransactionType POC_BLOCKING_MISS = new PocTx() {
        @Override
        public boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount) {

        }

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BLOCKING_MISS;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.POC_BLOCKING_MISS;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.PocBlockingMiss(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return new Attachment.PocBlockingMiss(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.PocBlockingMiss pocBlockingMiss = (Attachment.PocBlockingMiss) transaction.getAttachment();
            if (pocBlockingMiss == null) {
                throw new ConchException.NotValidException("Invalid pocBlockingMiss: null");
            }
            if (StringUtils.isBlank(pocBlockingMiss.getDevice())) {
                throw new ConchException.NotValidException("Invalid device: null or empty");
            }
            if (pocBlockingMiss.getNodeId() == null) {
                throw new ConchException.NotValidException("Invalid nodeId: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocBlockingMiss pocBlockingMiss = (Attachment.PocBlockingMiss) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl
            long pocFee = transaction.getFeeNQT() - ((TransactionImpl) transaction).getMinimumFeeNQT(BlockchainImpl.getInstance().getHeight());
            Account account = Account.getAccount(transaction.getSenderId());
            account.frozenNQT(AccountLedger.LedgerEvent.POC_BLOCKING_MISS, transaction.getId(), pocFee);
        }

        @Override
        public String getName() {
            return "blockingMiss";
        }
    };

    public static final TransactionType POC_BIFURACTION_OF_CONVERGENCE = new PocTx() {
        @Override
        public boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount) {

        }

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BIFURACTION_OF_CONVERGENCE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.POC_BIFURACTION_OF_CONVERGENCE;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.PocBifuractionOfConvergence(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return new Attachment.PocBifuractionOfConvergence(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            Attachment.PocBifuractionOfConvergence pocBifuractionOfConvergence = (Attachment.PocBifuractionOfConvergence) transaction.getAttachment();
            if (pocBifuractionOfConvergence == null) {
                throw new ConchException.NotValidException("Invalid pocBifuractionConvergence: null");
            }
            if (StringUtils.isBlank(pocBifuractionOfConvergence.getDevice())) {
                throw new ConchException.NotValidException("Invalid device: null or empty");
            }
            if (pocBifuractionOfConvergence.getNodeId() == null) {
                throw new ConchException.NotValidException("Invalid nodeId: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocBifuractionOfConvergence pocBifuractionOfConvergence = (Attachment.PocBifuractionOfConvergence) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl
            long pocFee = transaction.getFeeNQT() - ((TransactionImpl) transaction).getMinimumFeeNQT(BlockchainImpl.getInstance().getHeight());
            Account account = Account.getAccount(transaction.getSenderId());
            account.frozenNQT(AccountLedger.LedgerEvent.POC_BIFURACTION_OF_CONVERGENCE, transaction.getId(), pocFee);
        }

        @Override
        public String getName() {
            return "bifuractionOfConvergence";
        }
    };

    public abstract boolean attachmentApplyUnconfirmed(Transaction transaction, Account senderAccount);
    public abstract void attachmentUndoUnconfirmed(Transaction transaction, Account senderAccount);

    @Override
    final public byte getType() {
        return TransactionType.TYPE_POC;
    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return attachmentApplyUnconfirmed(transaction, senderAccount);
    }

    @Override
    public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        attachmentUndoUnconfirmed(transaction, senderAccount);
    }

    @Override
    final public boolean canHaveRecipient() {
        return false;
    }

    @Override
    final public boolean isPhasingSafe() {
        return true;
    }

}
