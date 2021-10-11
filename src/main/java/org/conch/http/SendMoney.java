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

package org.conch.http;

import com.alibaba.fastjson.JSONObject;
import org.conch.account.Account;
import org.conch.chain.BlockchainImpl;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class SendMoney extends CreateTransaction {

    static final SendMoney instance = new SendMoney();

    private final BlockchainImpl blockchain = BlockchainImpl.getInstance();

    private SendMoney() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "recipient", "amountNQT");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        long recipient = ParameterParser.getAccountId(req, "recipient", true);
        long amountNQT = ParameterParser.getAmountNQT(req);
        Account account = ParameterParser.getSenderAccount(req);
        String chainId = req.getParameter("chainId");
        if(Constants.EXCHANGE_OPEN_BUTTON){
             /*
              限制锁仓地址每天的转出量
             */
            String rightCode = "200";
            if(blockchain.getHeight()>Constants.EXCHANGE_HEIGHT && chainId != null){
                String url = Constants.MGR_URL;
                RestfulHttpClient.HttpResponse response;
                try {
                    response = RestfulHttpClient.getClient(url+"getExchangeAddress").get().request();
                    String content = response.getContent();
                    String code = (String)com.alibaba.fastjson.JSON.parseObject(content).get("code");
                    if(code.equals(rightCode)){
                        com.alibaba.fastjson.JSONObject contentObj =
                                (JSONObject) com.alibaba.fastjson.JSON.parseObject(content).get("body");
                        String chainStr = null;
                        switch (chainId){
                            case "1":
                                chainStr = contentObj.getString("Heco");
                                break;
                            case "2":
                                chainStr = contentObj.getString("OKEx");
                                break;
                            case "3":
                                chainStr = contentObj.getString("ETH");
                                break;
                            case "4":
                                chainStr = contentObj.getString("Tron");
                                break;
                            case "5":
                                chainStr = contentObj.getString("BSC");
                                break;
                            default:
                                break;
                        }

                        assert chainStr != null;
                        String CosRecipient = (String) JSONObject.parseObject(chainStr).get("CosRecipient");
                        if(CosRecipient.equals(account.getId()+"")) {
                            try {
                                response = RestfulHttpClient.getClient(url+"getDailyExchangeQuantity").get().request();
                                content = response.getContent();
                                contentObj = (JSONObject) com.alibaba.fastjson.JSON.parseObject(content).get("body");
                                code = (String) com.alibaba.fastjson.JSON.parseObject(content).get("code");
                                JSONObject chainObj = null;
                                if(code.equals(rightCode)){
                                    switch (chainId){
                                        case "1":
                                            chainObj = (JSONObject) contentObj.get("Heco");
                                            break;
                                        case "2":
                                            chainObj = (JSONObject) contentObj.get("OKEx");
                                            break;
                                        case "3":
                                            chainObj = (JSONObject) contentObj.get("ETH");
                                            break;
                                        case "4":
                                            chainObj = (JSONObject) contentObj.get("Tron");
                                            break;
                                        case "5":
                                            chainObj = (JSONObject) contentObj.get("BSC");
                                            break;
                                        default:
                                            break;
                                    }
                                    assert chainObj != null;
                                    Long dailyExchangeQuantity = (Long) chainObj.get("DailyExchangeQuantity");
                                    BigDecimal bigDecimal = new BigDecimal(dailyExchangeQuantity);
                                    BigDecimal bigDecimal1 = new BigDecimal(amountNQT);
                                    if(bigDecimal1.compareTo(bigDecimal) > 0){
                                        amountNQT = dailyExchangeQuantity;
                                    }
                                    String dailyExchangeQuantity1 = bigDecimal.subtract(new BigDecimal(amountNQT)).stripTrailingZeros().toPlainString();

                                    Map<String,String> params = new HashMap<>(2);
                                    params.put("chainId",chainId);
                                    params.put("dailyExchangeQuantity",dailyExchangeQuantity1);
                                    RestfulHttpClient.getClient(url+"setDailyExchangeQuantity").post().postParams(params).request();
                                }
                            }catch (IOException e) {
                                Logger.logDebugMessage("Assets cross chain: can't connect " + Constants.MGR_URL + e.getMessage());
                                return createTransaction(req, account, recipient, amountNQT);
                            }
                        }
                    }
                }catch (IOException e) {
                    Logger.logDebugMessage("Assets cross chain: can't connect " + Constants.MGR_URL + " caused by: " + e.getMessage());
                    return createTransaction(req, account, recipient, amountNQT);
                }
            }
        }
        return createTransaction(req, account, recipient, amountNQT);
    }

}
