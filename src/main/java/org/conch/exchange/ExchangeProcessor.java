package org.conch.exchange;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.conch.chain.Block;
import org.conch.common.Constants;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.Listener;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;

/**
 * NOTE: 需要检查该逻辑，为什么每个节点都需要该笔交易，会不会出现跨链交易被重复发送CosMgr进行处理? - Ben 2021.11.11
 *
 * @Description: 处理跨链请求
 * @Author peifeng
 * @Date 2021/5/6 16:56
 */
public final class ExchangeProcessor implements Listener<Block> {
    private static final String CODE_OK = "200";
    @Override
    public void notify(Block block) {
        if(!Constants.ACROSS_CHAIN_EXCHANGE_ENABLE){
            Logger.logDebugMessage("Across Exchange Assets Close!");
            return;
        }
        /**
         * heco chain acrossChain
         * check Transactions if there is Transaction to the lockAddress, send to gateway
         *
         * @Author peifeng
         */
        boolean exceedHeight = block.getHeight() >= Constants.ACROSS_CHAIN_EXCHANGE_HEIGHT;
        boolean emptyTxs = block.getTransactions().size() <= 0;
        if(!exceedHeight || emptyTxs){
            return;
        }

        for(Transaction tx : block.getTransactions()){
            if(!tx.getType().isType(TransactionType.TYPE_PAYMENT)){
                continue;
            }
            acrossChainExchangeProcess(tx);
        }
    }

    private void acrossChainExchangeProcess(Transaction tx){
        try {
            RestfulHttpClient.HttpResponse getExchangeAddrRsp = RestfulHttpClient.getClient(Constants.ACROSS_EXCHANGE_MGR_URL + "getExchangeAddress").get().request();
            String content = getExchangeAddrRsp.getContent();
            String code = (String)com.alibaba.fastjson.JSON.parseObject(content).get("code");
            JSONObject exchangeDef = (JSONObject) JSON.parseObject(content).get("body");
            if(!StringUtils.equals(CODE_OK, code)){
                return;
            }

            String chainId = findMatchedChainId(tx, exchangeDef);
            if(StringUtils.isEmpty(chainId)){
                return;
            }

            Map<String,String> params = new HashMap<>(6);
            params.put("chainId", chainId);
            params.put("accountId",tx.getSenderId()+"");
            params.put("recordType","1");
            params.put("amount",tx.getAmountNQT()+"");
            params.put("createDate",new Date().toString());
            params.put("SourceTransactionHash",tx.getFullHash());

            RestfulHttpClient.HttpResponse saveRsp = RestfulHttpClient.getClient(Constants.ACROSS_EXCHANGE_MGR_URL + "saveRecord").post().postParams(params).request();
            content = saveRsp.getContent();
            JSONObject saveResult = JSON.parseObject(content);
            code = (String)saveResult.get("code");
            if(CODE_OK.equals(code)){
                Logger.logInfoMessage("Heco chain: Record save success");
            }else{
                JSONObject body = JSON.parseObject((String)saveResult.get("body"));;
                Logger.logInfoMessage("Heco chain: " + (String)body.get("status"));
            }
        } catch (IOException e) {
            Logger.logDebugMessage("Heco chain: can't connect " + Constants.ACROSS_EXCHANGE_MGR_URL + " caused by: " + e.getMessage());
        } catch (Exception e) {
            Logger.logWarningMessage("Across chain exchange failed, caused by: " + e.getMessage());
        }
    }

    private String findMatchedChainId(Transaction tx, JSONObject exchangeDef){
        for(Map.Entry<String, String> chainDef : Constants.SUPPORT_ACROSS_CHAINS.entrySet()){
            String chainName = chainDef.getValue();
            if(!exchangeDef.containsKey(chainName)){
                continue;
            }
            JSONObject chainObj = exchangeDef.getJSONObject(chainName);
            if(StringUtils.equals(String.valueOf(tx.getRecipientId()), chainObj.getString("CosRecipient"))){
                return chainDef.getKey();
            }
        }
        return null;
    }
}
