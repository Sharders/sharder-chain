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

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.poc.PocCalculator;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.Logger;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * poc tx series wrapper: validate , parse , apply
 */
public abstract class PocTxWrapper extends TransactionType {

    public static final byte SUBTYPE_POC_NODE_TYPE = 0; // 节点类型
    public static final byte SUBTYPE_POC_NODE_CONF = 1; // 节点配置
    public static final byte SUBTYPE_POC_WEIGHT_TABLE = 2; // 权重
    public static final byte SUBTYPE_POC_ONLINE_RATE = 3; // 在线率
    public static final byte SUBTYPE_POC_BLOCK_MISSING = 4; // 出块丢失
    public static final byte SUBTYPE_POC_BC_SPEED = 5; // 分叉收敛


    public static TransactionType findTxType(byte subtype) {
        switch (subtype) {
            case SUBTYPE_POC_NODE_TYPE:
                return POC_NODE_TYPE;
            case SUBTYPE_POC_NODE_CONF:
                return POC_NODE_CONF;
            case SUBTYPE_POC_WEIGHT_TABLE:
                return POC_WEIGHT_TABLE;
            case SUBTYPE_POC_ONLINE_RATE:
                return POC_ONLINE_RATE;
            case SUBTYPE_POC_BLOCK_MISSING:
                return POC_BLOCK_MISSING;
            case SUBTYPE_POC_BC_SPEED:
                return POC_BC_SPEED;
            default:
                return null;
        }
    }

    private PocTxWrapper() {}

  public static final TransactionType POC_WEIGHT_TABLE =
      new PocTxWrapper() {

        @Override
        public byte getSubtype() {
          return SUBTYPE_POC_WEIGHT_TABLE;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
          return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
          try {
            return new PocTxBody.PocWeightTable(buffer, transactionVersion);
          } catch (ConchException.NotValidException e) {
            Logger.logErrorMessage("Can't new PocTxBody.PocWeightTable instance",e);
          }
          return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return  new PocTxBody.PocWeightTable(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
          PocTxBody.PocWeightTable pocWeight = (PocTxBody.PocWeightTable) transaction.getAttachment();
          if (pocWeight == null) {
            throw new ConchException.NotValidException("Invalid PocTxBody.PocWeightTable: null");
          }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
          PocTxBody.PocWeightTable pocWeight = (PocTxBody.PocWeightTable) transaction.getAttachment();
          PocCalculator.setCurWeightTable(pocWeight, transaction.getHeight());
        }

        @Override
        public String getName() {
          return "pocWeightTable";
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
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion)  {
            try{
                if(Conch.getBlockchain().getHeight() > Constants.POC_NODETYPE_V2_HEIGHT) {
                    return new PocTxBody.PocNodeTypeV2(buffer, transactionVersion);
                } 
            }catch(Exception e){
                return new PocTxBody.PocNodeType(buffer, transactionVersion);
            }
            return new PocTxBody.PocNodeType(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject data) {
            return data.containsKey("accountId") ? new PocTxBody.PocNodeTypeV2(data) : new PocTxBody.PocNodeType(data);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocNodeType nodeType = null;
            Attachment attachment = transaction.getAttachment();
            if(attachment instanceof PocTxBody.PocNodeType) {
                nodeType = (PocTxBody.PocNodeType) attachment;
            }else if(attachment instanceof PocTxBody.PocNodeTypeV2){
                nodeType = (PocTxBody.PocNodeTypeV2)attachment;
            }
            
            if (nodeType == null) {
                throw new ConchException.NotValidException("Invalid PocTxBody.PocNodeType: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Conch.getPocProcessor().pocTxProcess(transaction);
        }

        @Override
        public String getName() {
            return "pocNodeType";
        }
    };
    
    public static final TransactionType POC_NODE_CONF = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_NODE_CONF;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return new PocTxBody.PocNodeConf(buffer,transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new PocTxBody.PocNodeConf(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            // CONF tx need be created by official site
            PocTxBody.PocNodeConf configuration = (PocTxBody.PocNodeConf) transaction.getAttachment();
            if (configuration == null) {
                throw new ConchException.NotValidException("Invalid PocTxBody.PocNodeConf: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Conch.getPocProcessor().pocTxProcess(transaction);
        }

        @Override
        public String getName() {
            return "pocNodeConf";
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
            return new PocTxBody.PocOnlineRate(buffer,transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new PocTxBody.PocOnlineRate(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocOnlineRate pocOnlineRate = (PocTxBody.PocOnlineRate) transaction.getAttachment();
            if (pocOnlineRate == null) {
                throw new ConchException.NotValidException("Invalid PocTxBody.PocOnlineRate: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Conch.getPocProcessor().pocTxProcess(transaction);
        }

        @Override
        public String getName() {
            return "pocOnlineRate";
        }
    };

    public static final TransactionType POC_BLOCK_MISSING = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BLOCK_MISSING;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            try {
                return new PocTxBody.PocGenerationMissing(buffer,transactionVersion);
            } catch (ConchException.NotValidException e) {
                Logger.logErrorMessage("Can't new PocTxBody.PocBlockMissing instance",e);
            }
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new PocTxBody.PocGenerationMissing(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocGenerationMissing pocBlockingMiss = (PocTxBody.PocGenerationMissing) transaction.getAttachment();
            if (pocBlockingMiss == null) {
                throw new ConchException.NotValidException("Invalid PocTxBody.PocBlockMissing: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Conch.getPocProcessor().pocTxProcess(transaction);
        }

        @Override
        public String getName() {
            return "pocBlockMissing";
        }
    };

    public static final TransactionType POC_BC_SPEED = new PocTxWrapper() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_BC_SPEED;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return new PocTxBody.PocBcSpeed(buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new PocTxBody.PocGenerationMissing(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocBcSpeed pocBcSpeed = (PocTxBody.PocBcSpeed) transaction.getAttachment();
            if (pocBcSpeed == null) {
                throw new ConchException.NotValidException("Invalid PocTxBody.PocBcSpeed: null");
            }

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            
        }

        @Override
        public String getName() {
            return "pocBcSpeed";
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
