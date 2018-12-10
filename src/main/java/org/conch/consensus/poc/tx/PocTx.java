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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.common.ConchException;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.Https;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class PocTx extends TransactionType {

    private static final byte SUBTYPE_POC_NODE_CONFIGURATION = 0; // 节点配置
    private static final byte SUBTYPE_POC_WEIGHT = 1; // 权重
    private static final byte SUBTYPE_POC_ONLINE_RATE = 2; // 在线率
    private static final byte SUBTYPE_POC_BLOCKING_MISS = 3; // 出块丢失
    private static final byte SUBTYPE_POC_BIFURACTION_OF_CONVERGENCE = 4; // 分叉收敛

    private static final String API_SERVER = "https://api.sharder.io";

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
            //TODO node certify
            String url = API_SERVER + "/SC/getCredibleNode.ss?networkType=" + Conch.getStringProperty("sharder.network");
            String result = Https.httpRequest(url,"GET", null);
            // result 示例：// [{"id":21,"downloadedVolume":"20653840","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"20872748","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72869017,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72822036,"state":1,"shareAddress":true,"networkType":"alpha"},{"id":57,"downloadedVolume":"20724904","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"20941479","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72872622,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72872622,"state":1,"shareAddress":true,"networkType":"alpha"},{"id":92,"downloadedVolume":"21526090","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"21678153","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72930417,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72926800,"state":1,"shareAddress":true,"networkType":"alpha"},{"id":129,"downloadedVolume":"21773860","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"21893284","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72948477,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72944864,"state":1,"shareAddress":true,"networkType":"alpha"},{"id":165,"downloadedVolume":"21780900","address":"47.107.183.179","inbound":true,"blockChainState":"UP_TO_DATE","weight":0,"uploadedVolume":"21897924","services":null,"servicesObject":"API,CORS,BAPI","version":"0.1.0","platform":"Foundation Node","inboundWebSocket":true,"lastUpdated":72952099,"blackListed":0,"announcedAddress":"47.107.183.179","apiPort":8215,"application":"COS","port":8218,"outBoundWebSocket":true,"peerLoad":null,"lastConnectAttempt":72952099,"state":1,"shareAddress":true,"networkType":"alpha"}]
            if (StringUtils.isNoneBlank(result)) {
                List<NodeInfo> nodeInfos = new Gson().fromJson(result,  new TypeToken<List<NodeInfo>>(){}.getType());
                if (nodeInfos == null || nodeInfos.isEmpty()) {
                    throw new ConchException.NotValidException("Invalid certify nodes: null or empty");
                }
            }
            // TODO 怎样判断是否是可信节点创建的交易？
            Account account = Account.getAccount(transaction.getSenderId());

            // TODO 需要某种关系将Account / Transaction 和 可信节点信息进行对应，否则没有办法check是否通过
            Attachment.PocNodeConfiguration configuration = (Attachment.PocNodeConfiguration) transaction.getAttachment();
            if (configuration == null) {
                throw new ConchException.NotValidException("Invalid pocNodeConfiguration: null");
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocNodeConfiguration pocNodeConfiguration = (Attachment.PocNodeConfiguration) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl

            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_NODE_CONFIGURATION, transaction.getId(), -transaction.getAmountNQT());
            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
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
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocWeight pocWeight = (Attachment.PocWeight) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl

            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_WEIGHT, transaction.getId(), -transaction.getAmountNQT());
            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
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

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocOnlineRate pocOnlineRate = (Attachment.PocOnlineRate) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl

            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_ONLINE_RATE, transaction.getId(), -transaction.getAmountNQT());
            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
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
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocBlockingMiss pocBlockingMiss = (Attachment.PocBlockingMiss) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl

            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_BLOCKING_MISS, transaction.getId(), -transaction.getAmountNQT());
            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
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

        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            Attachment.PocBifuractionOfConvergence pocBifuractionOfConvergence = (Attachment.PocBifuractionOfConvergence) transaction.getAttachment();
            // TODO to add task 2 PocProcessorImpl

            senderAccount.frozenBalanceAndUnconfirmedBalanceNQT(AccountLedger.LedgerEvent.POC_BIFURACTION_OF_CONVERGENCE, transaction.getId(), -transaction.getAmountNQT());
            senderAccount.addToForgedBalanceNQT(transaction.getAmountNQT());
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

    public class NodeInfo implements Serializable {
        private static final long serialVersionUID = 3165561011642807413L;

        private Long id;
        private String downloadedVolume;
        private String address;
        private Boolean inbound;
        private String blockChainState;
        private Integer weight;
        private String uploadedVolume;
        private Object services;
        private String servicesObject;
        private String version;
        private String platform;
        private Boolean inboundWebSocket;
        private Integer lastUpdated;
        private Integer blackListed;
        private String announcedAddress;
        private Integer apiPort;
        private String application;
        private Integer port;
        private Boolean outBoundWebSocket;
        private Object peerLoad;
        private Integer lastConnectAttempt;
        private Integer state;
        private Boolean shareAddress;
        private String networkType;
    }
}
