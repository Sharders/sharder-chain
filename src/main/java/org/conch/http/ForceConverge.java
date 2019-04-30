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

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.chain.Block;
import org.conch.chain.CheckSumValidator;
import org.conch.common.UrlManager;
import org.conch.mint.Generator;
import org.conch.peer.Peers;
import org.conch.tools.ClientUpgradeTool;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public final class ForceConverge extends APIServlet.APIRequestHandler {

    static final ForceConverge INSTANCE = new ForceConverge();

    private ForceConverge() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try {
            com.alibaba.fastjson.JSONObject cmdObj = getCmdTools();
            if(cmdObj == null) {
                response.put("current node[" + Peers.getMyAddress() + "] needn't to process", true);
                return response;
            }
            
            try{
                Generator.pause(true);
                Logger.logDebugMessage("start to syn known ignore blocks and txs...");
                // syn ignore blocks
                CheckSumValidator.updateKnownIgnoreBlocks();

                int currentHeight = Conch.getBlockchain().getHeight();
                int toHeight = cmdObj.containsKey("toHeight") ? cmdObj.getInteger("toHeight") : currentHeight;
                Logger.logDebugMessage("received toHeight is %d ", toHeight);
                // pop-off to specified height
                List<? extends Block> blocks = Lists.newArrayList();
                try {
                    Conch.getBlockchainProcessor().setGetMoreBlocks(false);
                    if(toHeight < currentHeight) {
                        Logger.logDebugMessage("start to pop-off to height %d", toHeight);
                        blocks = Conch.getBlockchainProcessor().popOffTo(toHeight);
                    }
                } finally {
                    Conch.getBlockchainProcessor().setGetMoreBlocks(true);
                }

                // tx process
                boolean keepTx = cmdObj.getBooleanValue("keepTx");
                Logger.logDebugMessage("received keepTx is %s ", keepTx);

                if (keepTx) {
                    Logger.logDebugMessage("start to put the txs into delay process pool");
                    blocks.forEach(block -> Conch.getTransactionProcessor().processLater(block.getTransactions()));
                }
            }finally {
                Generator.pause(false);
            }

            // pause command process
            if(cmdObj.containsKey("pauseSyn")){
                boolean pauseSyn = cmdObj.getBooleanValue("pauseSyn");
                if(pauseSyn){
                    Conch.getBlockchainProcessor().setGetMoreBlocks(false);
                    Generator.pause(true);
                }else{
                    Conch.getBlockchainProcessor().setGetMoreBlocks(true);
                    Generator.pause(false);
                }
            }
            
            //upgrade
            if(cmdObj.containsKey("upgradeCos")){
                boolean upgradeCos = cmdObj.getBooleanValue("upgradeCos");
                Logger.logDebugMessage("received upgradeCos is %s ",upgradeCos);
                if(upgradeCos){
                    Logger.logDebugMessage("start to auto upgrade...");
                    ClientUpgradeTool.autoUpgrade(true);
                }
            }
            
            response.put("done", true);
            
        } catch (IOException e) {
            JSONData.putException(response, e);
        } catch (RuntimeException e) {
            JSONData.putException(response, e);
        }
        return response;
    }
    
    private static boolean matchOwn(com.alibaba.fastjson.JSONObject cmdObj){
        boolean accountMatched = false;
        if(cmdObj.containsKey("rsAccount")){
            accountMatched =  cmdObj.getString("rsAccount").equals(Generator.getAutoMiningRS());
        }

        boolean serialMatched = false;
        if(cmdObj.containsKey("serialNum")){
            serialMatched = StringUtils.isNotEmpty(Conch.getSerialNum()) 
                    && cmdObj.getString("serialNum").equals(Conch.getSerialNum());
        }
        
        return accountMatched || serialMatched;
    }
    
    public static com.alibaba.fastjson.JSONObject getCmdTools(){
        String url = UrlManager.CMD_TOOLS;
        try {
            RestfulHttpClient.HttpResponse response = RestfulHttpClient.getClient(url).get().request();
            if(response == null) return null;
            
            String content = response.getContent();
            Logger.logDebugMessage("cmd tools => \n\r" + content);
            if(content.startsWith("[")) {
                com.alibaba.fastjson.JSONArray array = JSON.parseArray(content);
                for(int i = 0; i < array.size(); i++) {
                    com.alibaba.fastjson.JSONObject cmdObj = array.getJSONObject(i);
                    
                    if(matchOwn(cmdObj)) return cmdObj;
                }
            }else if(content.startsWith("{")){
                com.alibaba.fastjson.JSONObject cmdObj = JSON.parseObject(content);
                if(matchOwn(cmdObj)) return cmdObj;
            }
            
        } catch (IOException e) {
            Logger.logErrorMessage("Can't get cmd tools from " + url + " caused by " + e.getMessage());
        }
        return null;
    }

    @Override
    protected final boolean requirePost() {
        return false;
    }

    @Override
    protected boolean requirePassword() {
        return false;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
