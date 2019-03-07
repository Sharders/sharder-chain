package org.conch.http.handler.impl;

import org.conch.common.ConchException;
import org.conch.http.handler.QueryTransactionsHandler;
import org.conch.tx.TransactionType;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * @author CloudSen
 */
public class QueryTransactionsViaAddress implements QueryTransactionsHandler {

    private QueryTransactionsViaAddress() {

    }

    private static class InstanceHolder {
        private static final QueryTransactionsViaAddress INSTANCE = new QueryTransactionsViaAddress();
    }

    public static QueryTransactionsViaAddress getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void filter(JSONObject transaction, List<JSONObject> dataJsons, QueryTransactionsCondition condition) throws ConchException.NotValidException {
        boolean correctType = TransactionType.TYPE_POC == condition.getType()
                && QueryTransactionsHandler.Factory.getSubTypeByHandleType(condition.getHandleType()) == condition.getSubType();
        if (correctType) {
            org.json.simple.JSONObject attachment = (org.json.simple.JSONObject) transaction.get("attachment");
            String attachmentIp = String.valueOf(attachment.get("ip"));
            String attachmentPort = String.valueOf(attachment.get("port"));
            String expectIp = condition.getIp();
            String expectPort = condition.getPort();
            boolean getAll = expectIp == null && expectPort == null;
            boolean getClearlyAddress = expectIp != null && expectPort != null && expectIp.equalsIgnoreCase(attachmentIp) && expectPort.equalsIgnoreCase(attachmentPort);
            boolean getByIp = expectIp != null && expectPort == null && expectIp.equalsIgnoreCase(attachmentIp);
            boolean getByPort = expectIp == null && expectPort != null && expectPort.equalsIgnoreCase(attachmentPort);
            if (getAll || getClearlyAddress || getByIp || getByPort) {
                attachment.put("fullHash", transaction.get("fullHash"));
                attachment.put("transaction", transaction.get("transaction"));
                dataJsons.add(attachment);
            }
        }
    }
}
