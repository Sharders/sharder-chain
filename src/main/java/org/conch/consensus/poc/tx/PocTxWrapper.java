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

import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.consensus.poc.PocScore;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public abstract class PocTxWrapper extends TransactionType {

    private static final byte SUBTYPE_POC_NODE_TYPE = 0; // 节点类型
    private static final byte SUBTYPE_POC_NODE_CONF = 1; // 节点配置
    private static final byte SUBTYPE_POC_WEIGHT = 2; // 权重
    private static final byte SUBTYPE_POC_ONLINE_RATE = 3; // 在线率
    private static final byte SUBTYPE_POC_BLOCK_MISS = 4; // 出块丢失
    private static final byte SUBTYPE_POC_BC = 5; // 分叉收敛

    private static final String API_SERVER = "https://api.sharder.io";

    public static TransactionType findTxType(byte subtype) {
        switch (subtype) {
            case SUBTYPE_POC_NODE_TYPE:
                return POC_NODE_TYPE;
            case SUBTYPE_POC_NODE_CONF:
                return POC_NODE_CONFIGURATION;
            case SUBTYPE_POC_WEIGHT:
                return POC_WEIGHT_TABLE;
            case SUBTYPE_POC_ONLINE_RATE:
                return POC_ONLINE_RATE;
            case SUBTYPE_POC_BLOCK_MISS:
                return POC_BLOCKING_MISS;
            case SUBTYPE_POC_BC:
                return POC_BC;
            default:
                return null;
        }
    }

    private PocTxWrapper() {}


    public static final TransactionType POC_WEIGHT_TABLE = new PocTxWrapper() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_POC_WEIGHT;
            }

            @Override
            public AccountLedger.LedgerEvent getLedgerEvent() {
                return null;
            }

            @Override
            public Attachment.AbstractAttachment parseAttachment(
                    ByteBuffer buffer, byte transactionVersion) {
                return Attachment.TxBodyBase.newObj(
                        PocTxBody.PocWeightTable.class, buffer, transactionVersion);
            }

            @Override
            public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
                return Attachment.TxBodyBase.newObj(PocTxBody.PocWeightTable.class, attachmentData);
            }

            @Override
            public void validateAttachment(Transaction transaction)
                    throws ConchException.ValidationException {
                PocTxBody.PocWeightTable pocWeight =
                        (PocTxBody.PocWeightTable) transaction.getAttachment();
                if (pocWeight == null) {
                    throw new ConchException.NotValidException("Invalid PocWeightTable: null");
                }
            }

            @Override
            public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                PocTxBody.PocWeightTable pocWeight =  (PocTxBody.PocWeightTable) transaction.getAttachment();
                PocScore.setCurWeightTable(pocWeight);
            }

            @Override
            public String getName() {
                return "weightTable";
            }
    };
    

    public static final TransactionType POC_NODE_TYPE = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_NODE_TYPE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocNodeConf.class, buffer,transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocNodeConf.class,attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocNodeType nodeType = (PocTxBody.PocNodeType) transaction.getAttachment();
            if (nodeType == null) {
                throw new ConchException.NotValidException("Invalid pocNodeType: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocNodeType pocNodeType = (PocTxBody.PocNodeType) transaction.getAttachment();

            PocProcessorImpl.nodeTypeTxProcess(transaction.getHeight(),pocNodeType);
        }

        @Override
        public String getName() {
            return "nodeConfiguration";
        }
    };
    
    public static final TransactionType POC_NODE_CONFIGURATION = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_NODE_CONF;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocNodeConf.class, buffer,transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocNodeConf.class,attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            // CONF tx need be created by official site
            PocTxBody.PocNodeConf configuration = (PocTxBody.PocNodeConf) transaction.getAttachment();
            if (configuration == null) {
                throw new ConchException.NotValidException("Invalid pocNodeConfiguration: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocNodeConf pocNodeConf = (PocTxBody.PocNodeConf) transaction.getAttachment();
            PocProcessorImpl.nodeConfTxProcess(transaction.getHeight(),pocNodeConf);
        }

        @Override
        public String getName() {
            return "nodeConfiguration";
        }
    };

 

    public static final TransactionType POC_ONLINE_RATE = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_ONLINE_RATE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocOnlineRate.class, buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocOnlineRate.class, attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocOnlineRate pocOnlineRate = (PocTxBody.PocOnlineRate) transaction.getAttachment();
            if (pocOnlineRate == null) {
                throw new ConchException.NotValidException("Invalid pocOnlineRate: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocOnlineRate pocOnlineRate = (PocTxBody.PocOnlineRate) transaction.getAttachment();
            PocProcessorImpl.onlineRateTxProcess(transaction.getHeight(),pocOnlineRate);
        }

        @Override
        public String getName() {
            return "onlineRate";
        }
    };

    public static final TransactionType POC_BLOCKING_MISS = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BLOCK_MISS;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocBlockMiss.class, buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocBlockMiss.class, attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocBlockMiss pocBlockingMiss = (PocTxBody.PocBlockMiss) transaction.getAttachment();
            if (pocBlockingMiss == null) {
                throw new ConchException.NotValidException("Invalid pocBlockingMiss: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocBlockMiss pocBlockingMiss = (PocTxBody.PocBlockMiss) transaction.getAttachment();

            PocProcessorImpl.blockMissTxProcess(transaction.getHeight(), pocBlockingMiss);
        }

        @Override
        public String getName() {
            return "blockingMiss";
        }
    };

    public static final TransactionType POC_BC = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BC;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocBC.class, buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocBC.class, attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocBC pocBc = (PocTxBody.PocBC) transaction.getAttachment();
            if (pocBc == null) {
                throw new ConchException.NotValidException("Invalid pocBifuractionConvergence: null");
            }

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocBC pocBC = (PocTxBody.PocBC) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl
//            PocProcessorImpl.setPocBOC(senderAccount, transaction);
//            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_BC, transaction.getId(), -transaction.getAmountNQT());
//            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
        }

        @Override
        public String getName() {
            return "bifuractionOfConvergence";
        }
    };

    @Override
    final public byte getType() {
        return TransactionType.TYPE_POC;
    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
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
