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
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.common.ConchException;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.Https;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public abstract class PocTx extends TransactionType {

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

    private PocTx() {}

    public static final TransactionType POC_NODE_TYPE = new PocTx() {

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
            
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocNodeType pocNodeType = (PocTxBody.PocNodeType) transaction.getAttachment();

            PocProcessorImpl.addOrUpdateNodeType(transaction.getHeight(),pocNodeType.getIp());
        }

        @Override
        public String getName() {
            return "nodeConfiguration";
        }
    };
    
    public static final TransactionType POC_NODE_CONFIGURATION = new PocTx() {

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
            //TODO node certify
            String url = API_SERVER + "/SC/getCredibleNode.ss?networkType=" + Conch.getStringProperty("sharder.network");
            String result = Https.httpRequest(url,"GET", null);
            // result 示例：// [{"id":21,"downloadedVolume":"20653840","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"20872748","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72869017,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72822036,"state":1,"shareAddress":true,"networkType":"alpha"},{"id":57,"downloadedVolume":"20724904","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"20941479","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72872622,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72872622,"state":1,"shareAddress":true,"networkType":"alpha"},{"id":92,"downloadedVolume":"21526090","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"21678153","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72930417,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72926800,"state":1,"shareAddress":true,"networkType":"alpha"},{"id":129,"downloadedVolume":"21773860","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"21893284","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72948477,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72944864,"state":1,"shareAddress":true,"networkType":"alpha"},{"id":165,"downloadedVolume":"21780900","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"21897924","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72952099,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72952099,"state":1,"shareAddress":true,"networkType":"alpha"}]
            if (StringUtils.isNoneBlank(result)) {
                
//                List<NodeInfo> nodeInfos = new Gson().fromJson(result,  new TypeToken<List<NodeInfo>>(){}.getType());
//                if (nodeInfos == null || nodeInfos.isEmpty()) {
//                    throw new ConchException.NotValidException("Invalid certify nodes: null or empty");
//                }
            }
            // TODO 怎样判断是否是可信节点创建的交易？
            Account account = Account.getAccount(transaction.getSenderId());

            // TODO 需要某种关系将Account / Transaction 和 可信节点信息进行对应，否则没有办法check是否通过
            PocTxBody.PocNodeConf configuration = (PocTxBody.PocNodeConf) transaction.getAttachment();
            if (configuration == null) {
                throw new ConchException.NotValidException("Invalid pocNodeConfiguration: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocNodeConf pocNodeConfiguration = (PocTxBody.PocNodeConf) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl
//            PocProcessorImpl.setPocConfiguration(senderAccount, transaction);
//            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_NODE_CONF, transaction.getId(), -transaction.getAmountNQT());
//            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
        }

        @Override
        public String getName() {
            return "nodeConfiguration";
        }
    };

    public static final TransactionType POC_WEIGHT_TABLE = new PocTx() {

        @Override
        public byte getSubtype() {
            return SUBTYPE_POC_WEIGHT;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocWeightTable.class, buffer, transactionVersion);
        }

        @Override
        public Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return Attachment.TxBodyBase.newObj(PocTxBody.PocWeightTable.class, attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws ConchException.ValidationException {
            PocTxBody.PocWeightTable pocWeight = (PocTxBody.PocWeightTable) transaction.getAttachment();
            if (pocWeight == null) {
                throw new ConchException.NotValidException("Invalid PocWeightTable: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            PocTxBody.PocWeightTable pocWeight = (PocTxBody.PocWeightTable) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl

//            PocProcessorImpl.setPocWeight(senderAccount, transaction);
//            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_WEIGHT, transaction.getId(), -transaction.getAmountNQT());
//            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
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
            // TODO to add task 2 PocProcessorImpl

//            PocProcessorImpl.setPocOnlineRate(senderAccount, transaction);
//            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_ONLINE_RATE, transaction.getId(), -transaction.getAmountNQT());
//            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
        }

        @Override
        public String getName() {
            return "onlineRate";
        }
    };

    public static final TransactionType POC_BLOCKING_MISS = new PocTx() {

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
            // TODO to add task 2 PocProcessorImpl

//            PocProcessorImpl.setPocBlockingMiss(senderAccount, transaction);
//            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_BLOCKING_MISS, transaction.getId(), -transaction.getAmountNQT());
//            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
        }

        @Override
        public String getName() {
            return "blockingMiss";
        }
    };

    public static final TransactionType POC_BC = new PocTx() {

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
