package org.conch.http.handler.impl;

import org.conch.common.ConchException;
import org.conch.http.handler.QueryTransactionsHandler;
import org.conch.tx.TransactionType;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * @author CloudSen
 */
public class QueryTransactionsViaPocVersion implements QueryTransactionsHandler {

    private QueryTransactionsViaPocVersion() {

    }

    private static class InstanceHolder {
        private static final QueryTransactionsViaPocVersion INSTANCE = new QueryTransactionsViaPocVersion();
    }

    public static QueryTransactionsViaPocVersion getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void filter(JSONObject transaction, List<JSONObject> dataJsons, QueryTransactionsCondition condition) throws ConchException.NotValidException {
        boolean correctType = TransactionType.TYPE_POC == condition.getType()
                && QueryTransactionsHandler.Factory.getSubTypeByHandleType(condition.getHandleType()) == condition.getSubType();
        if (correctType) {
            org.json.simple.JSONObject attachment = (org.json.simple.JSONObject) transaction.get("attachment");
            Long templateVersion = attachment.get("templateVersion") == null ? 0 : Long.parseLong(String.valueOf(attachment.get("templateVersion")));
            Long expectVersion = condition.getTemplateVersion();
            // is poc template tx, but no version query condition
            boolean getAllVersion = templateVersion != 0 && expectVersion == 0;
            // is poc template tx, and querying by expect version
            boolean getClearlyVersion = templateVersion != 0 && expectVersion != 0 && templateVersion.equals(expectVersion);
            if (getAllVersion || getClearlyVersion) {
                attachment.put("fullHash", transaction.get("fullHash"));
                attachment.put("transaction", transaction.get("transaction"));
                dataJsons.add(attachment);
            }
        }
    }

}
