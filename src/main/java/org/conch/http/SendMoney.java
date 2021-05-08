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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.conch.account.Account;
import org.conch.chain.BlockchainImpl;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.json.simple.JSONStreamAware;

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
        /**
         * 限制MW锁仓地址每天的转出量
         */
        if(blockchain.getHeight()>Constants.HECO_HEIGHT){
            String url = Constants.MGR_URL;
            RestfulHttpClient.HttpResponse response = null;
            try {
                response = RestfulHttpClient.getClient(url+"getHecoExchangeAddress").get().request();
                String content = response.getContent();
                com.alibaba.fastjson.JSONObject contentObj = com.alibaba.fastjson.JSON.parseObject(content);
                String code = (String)contentObj.get("code");
                if(code.equals("200")){
                    String recipientId = ((Long)contentObj.get("body")+"");
                    if(recipientId.equals(account.getId()+"")) {
                        try {
                            response = RestfulHttpClient.getClient(url+"getDailyExchangeQuantity").get().request();
                            content = response.getContent();
                            contentObj = com.alibaba.fastjson.JSON.parseObject(content);
                            code = (String)contentObj.get("code");
                            if(code.equals("200")){
                                Long dailyExchangeQuantity = (Long)contentObj.get("body");
                                BigDecimal bigDecimal = new BigDecimal(dailyExchangeQuantity);
                                BigDecimal bigDecimal1 = new BigDecimal(amountNQT);
                                if(bigDecimal1.compareTo(bigDecimal) == 1){
                                    amountNQT = dailyExchangeQuantity;
                                }
                                String dailyExchangeQuantity1 = bigDecimal.subtract(new BigDecimal(amountNQT)).stripTrailingZeros().toPlainString();

                                Map<String,String> params = new HashMap<>();
                                params.put("dailyExchangeQuantity",dailyExchangeQuantity1);
                                RestfulHttpClient.getClient(url+"setDailyExchangeQuantity").post().postParams(params).request();
                            }
                        }catch (IOException e) {
                            Logger.logDebugMessage("Heco chain: can't connect " + Constants.MGR_URL + e.getMessage());
                            return createTransaction(req, account, recipient, amountNQT);
                        }
                    }
                }
            }catch (IOException e) {
                Logger.logDebugMessage("Heco chain: can't connect " + Constants.MGR_URL + " caused by: " + e.getMessage());
                return createTransaction(req, account, recipient, amountNQT);
            }
        }
        return createTransaction(req, account, recipient, amountNQT);
    }

}
