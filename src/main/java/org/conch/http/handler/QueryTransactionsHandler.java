package org.conch.http.handler;

import org.conch.common.ConchException;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.http.handler.impl.QueryTransactionsCondition;
import org.conch.http.handler.impl.QueryTransactionsViaAddress;
import org.conch.http.handler.impl.QueryTransactionsViaPocVersion;
import org.conch.tx.TransactionType;
import org.conch.util.Convert;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * handler for querying transactions
 *
 * @author CloudSen
 */
public interface QueryTransactionsHandler {

    String HANDLE_TYPE_NOT_FOUND = "Can not found handle type named {}";

    enum HandleType {
        /**
         * type of node configuration performance
         */
        NODE_CONFIG,
        /**
         * node type
         */
        NODE_TYPE,
        /**
         * type of PoC template weight table
         */
        POC_TEMPLATE,
        /**
         * type of node online rate
         */
        ONLINE_RATE
    }

    enum Factory {
        /**
         * node config transactions queried by ip and port
         */
        NODE_CONFIG_HANDLER(HandleType.NODE_CONFIG, TransactionType.TYPE_POC, PocTxWrapper.SUBTYPE_POC_NODE_CONF, QueryTransactionsViaAddress.getInstance()),
        /**
         * PoC template transactions queried by poc template version
         */
        POC_TEMPLATE_HANDLER(HandleType.POC_TEMPLATE, TransactionType.TYPE_POC, PocTxWrapper.SUBTYPE_POC_WEIGHT_TABLE, QueryTransactionsViaPocVersion.getInstance()),
        /**
         * node online rate transactions queried by ip and port
         */
        ONLINE_RATE_HANDLER(HandleType.ONLINE_RATE, TransactionType.TYPE_POC, PocTxWrapper.SUBTYPE_POC_ONLINE_RATE, QueryTransactionsViaAddress.getInstance()),
        ;


        /**
         * PoC operation type
         */
        private HandleType handleType;
        private byte txType;
        private byte subType;
        private QueryTransactionsHandler queryTransactionsHandler;

        Factory(HandleType handleType, byte txType, byte subType, QueryTransactionsHandler queryTransactionsHandler) {
            this.handleType = handleType;
            this.txType = txType;
            this.subType = subType;
            this.queryTransactionsHandler = queryTransactionsHandler;
        }

        public QueryTransactionsHandler getQueryTransactionsHandler() {
            return queryTransactionsHandler;
        }

        public HandleType getHandleType() {
            return handleType;
        }

        public byte getTxType() {
            return txType;
        }

        public byte getSubType() {
            return subType;
        }

        public static QueryTransactionsHandler getHandlerByType(HandleType handleType) throws ConchException.NotValidException {
            return Arrays.stream(values())
                    .filter(factory -> factory.getHandleType().equals(handleType))
                    .findFirst().map(Factory::getQueryTransactionsHandler)
                    .orElseThrow(() -> new ConchException.NotValidException(Convert.stringTemplate(HANDLE_TYPE_NOT_FOUND, handleType)));
        }

        public static byte getTxTypeByHandleType(HandleType handleType) throws ConchException.NotValidException {
            return Arrays.stream(values())
                    .filter(factory -> factory.getHandleType().equals(handleType))
                    .findFirst().map(Factory::getTxType)
                    .orElseThrow(() -> new ConchException.NotValidException(Convert.stringTemplate(HANDLE_TYPE_NOT_FOUND, handleType)));
        }

        public static byte getSubTypeByHandleType(HandleType handleType) throws ConchException.NotValidException {
            return Arrays.stream(values())
                    .filter(factory -> factory.getHandleType().equals(handleType))
                    .findFirst().map(Factory::getSubType)
                    .orElseThrow(() -> new ConchException.NotValidException(Convert.stringTemplate(HANDLE_TYPE_NOT_FOUND, handleType)));
        }
    }

    /**
     * Filtering the corresponding data according to the conditions
     *
     * @param transaction current transaction
     * @param dataJsons   collection of corresponding data
     * @param condition   query conditions
     * @throws ConchException.NotValidException
     */
    default void filter(JSONObject transaction, List<JSONObject> dataJsons, QueryTransactionsCondition condition) throws ConchException.NotValidException {

    }
}
