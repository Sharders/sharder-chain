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

package org.conch.poc.tx;

import org.conch.ConchException;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
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
                ;
            case SUBTYPE_POC_WEIGHT:
                ;
            case SUBTYPE_POC_ONLINE_RATE:
                ;
            case SUBTYPE_POC_BLOCKING_MISS:
                ;
            case SUBTYPE_POC_BIFURACTION_OF_CONVERGENCE:
                ;
            default:
                return null;
        }
    }


    private PocTx() {}

    public static final TransactionType POC_NODE_CONFIGURATION = new PocTx() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_NODE_CONFIGURATION;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
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

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

        }

        @Override
        public String getName() {
            return "nodeConfiguration";
        }
    };

    public static final TransactionType POC_WEIGHT = new PocTx() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_WEIGHT;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return new Attachment.PocNodeConfiguration(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return new Attachment.PocNodeConfiguration(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

        }

        @Override
        public String getName() {
            return "weight";
        }
    };

    public static final TransactionType POC_ONLINE_RATE = new PocTx() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_ONLINE_RATE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return null;
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

        }

        @Override
        public String getName() {
            return "onlineRate";
        }
    };

    public static final TransactionType POC_BLOCKING_MISS = new PocTx() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BLOCKING_MISS;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return null;
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

        }

        @Override
        public String getName() {
            return "blockingMiss";
        }
    };

    public static final TransactionType POC_BIFURACTION_OF_CONVERGENCE = new PocTx() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BIFURACTION_OF_CONVERGENCE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws ConchException.NotValidException {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws ConchException.NotValidException {
            return null;
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

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
