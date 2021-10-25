package org.conch.exchange;

import org.conch.chain.Block;
import org.conch.common.Constants;
import org.conch.tx.TransactionType;
import org.conch.util.Listener;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 处理跨链请求
 * @Author peifeng
 * @Date 2021/5/6 16:56
 */
public final class ExchangeProcessor implements Listener<Block> {

    // close dividend distribution
    final static boolean exchangeOpen = Constants.EXCHANGE_OPEN_BUTTON;

    static int debugCount = 0;

    @Override
    public void notify(Block block) {
        if(!exchangeOpen){
            Logger.logDebugMessage("Exchange Assets Close!");
            return;
        }
        /**
         * heco chain accossChain
         *
         * check Transactions if there is Transaction to the lockAddress, send to gateway
         *
         * @Author peifeng
         */
        if(block.getHeight() >= Constants.EXCHANGE_HEIGHT){
            String rightCode = "200";
            Map<String,String> chainIds = Constants.chainIds;
            if(block.getTransactions().size() > 0){
                block.getTransactions().forEach(transaction -> {
                    if(transaction.getType().isType(TransactionType.TYPE_PAYMENT)){
                        String url = Constants.MGR_URL;
                        RestfulHttpClient.HttpResponse response = null;
                        try {
                            response = RestfulHttpClient.getClient(url+"getExchangeAddress").get().request();
                            String content = response.getContent();
                            String code = (String)com.alibaba.fastjson.JSON.parseObject(content).get("code");
                            com.alibaba.fastjson.JSONObject contentObj = (com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSON.parseObject(content).get("body");
                            if(code.equals(rightCode)){
                                com.alibaba.fastjson.JSONObject chainObj;
                                Map<String,String> params = new HashMap<>(6);
                                Boolean flag = false;

                                for(Map.Entry<String, String> entry : chainIds.entrySet()){
                                    if(!flag){
                                        chainObj = contentObj.getJSONObject(entry.getValue());
                                        if(chainObj!=null && (transaction.getRecipientId()+"").equals(chainObj.getString("CosRecipient"))){
                                            params.put("chainId",entry.getKey());
                                            flag = true;
                                        }
                                    }
                                }
                                if(flag){
                                    params.put("accountId",transaction.getSenderId()+"");
                                    params.put("recordType","1");
                                    params.put("amount",transaction.getAmountNQT()+"");
                                    params.put("createDate",new Date().toString());
                                    params.put("SourceTransactionHash",transaction.getFullHash());
                                    try {
                                        response = RestfulHttpClient.getClient(url+"saveRecord").post().postParams(params).request();
                                        content = response.getContent();
                                        contentObj = com.alibaba.fastjson.JSON.parseObject(content);
                                        code = (String)contentObj.get("code");
                                        if(code.equals(rightCode)){
                                            Logger.logInfoMessage("Heco chain: Record save success");
                                        }else{
                                            com.alibaba.fastjson.JSONObject body = com.alibaba.fastjson.JSON.parseObject((String)contentObj.get("body"));;
                                            Logger.logInfoMessage("Heco chain: "+(String)body.get("status"));
                                        }
                                    }catch (IOException e) {
                                        Logger.logDebugMessage("Heco chain:can't sendTransactin in hecoChain"+e.getMessage());
                                    }
                                }
                            }
                        } catch (IOException e) {
                            Logger.logDebugMessage("Heco chain: can't connect " + Constants.MGR_URL + " caused by: " + e.getMessage());
                        }
                    }
                });
            }
        }
    }
}
