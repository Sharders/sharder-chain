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
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.chain.Block;
import org.conch.chain.CheckSumValidator;
import org.conch.common.UrlManager;
import org.conch.mint.Generator;
import org.conch.peer.Peers;
import org.conch.tools.ClientUpgradeTool;
import org.conch.util.FileUtil;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.conch.util.ThreadPool;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ForceConverge extends APIServlet.APIRequestHandler {

    static final ForceConverge INSTANCE = new ForceConverge();

    private ForceConverge() {
        super(new APITag[] {APITag.DEBUG});
    }
    
    enum Command {
        TO_HEIGHT("toHeight")       // from v0.1.5
        ,KEEP_TX("keepTx")          // from v0.1.5
        ,PAUSE_SYNC("pauseSyn")     // from v0.1.5
        ,UPGRADE_COS("upgradeCos")  // from v0.1.5
        ,UPGRADE_DB("upgradeDb")
        ,RESET("reset")             // from v0.1.6
        ,RESTART("restart")         // from v0.1.6
        ;

        private String value;

        Command(String value) {
            this.value = value;
        }

        public String val() {
            return value;
        }
    }


    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try {
            com.alibaba.fastjson.JSONObject cmdObj = getCmdTools();
            if(cmdObj == null) {
                response.put("current node[" + Peers.getMyAddress() + "] needn't to process", true);
                return response;
            }else{
                Logger.logDebugMessage("received force converge command: " + cmdObj.toJSONString());
            }

            // syn ignore blocks
            try {
                Generator.pause(true);
                Logger.logDebugMessage("update known ignore blocks and txs...");
                CheckSumValidator.updateKnownIgnoreBlocks();
            } finally {
                Generator.pause(false);
            }
            
            //pop-off command
            if(cmdObj.containsKey(Command.TO_HEIGHT.val())) {
                try {
                    Conch.pause();

                    int currentHeight = Conch.getBlockchain().getHeight();
                    int toHeight = cmdObj.getInteger(Command.TO_HEIGHT.val());
                    Logger.logDebugMessage("received toHeight is %d ", toHeight);
                    
                    if(toHeight == -1){
                        // reset the current db
                        Logger.logDebugMessage("toHeight is -1, reset(delete db folder from disk) and restart the block chain ");
                        manualReset();
                        new Thread(() -> Conch.restartApplication(null)).start();
                        response.put("done", true);
                    }else if(toHeight == 0){
                        Logger.logDebugMessage("toHeight is 0, full reset the block chain");
                        Conch.getBlockchainProcessor().fullReset();
                    }else{
                        // pop-off to specified height
                        List<? extends Block> blocks = Lists.newArrayList();
                        if (toHeight < currentHeight) {
                            Logger.logDebugMessage("start to pop-off to height %d", toHeight);
                            blocks = Conch.getBlockchainProcessor().popOffTo(toHeight);
                        }

                        // tx process
                        boolean keepTx = cmdObj.getBooleanValue(Command.KEEP_TX.val());
                        Logger.logDebugMessage("received keepTx is %s ", keepTx);

                        if (keepTx) {
                            Logger.logDebugMessage("start to put the txs into delay process pool");
                            blocks.forEach(block -> Conch.getTransactionProcessor().processLater(block.getTransactions()));
                        }
                    }
                 
                } finally {
                    Conch.unpause();
                }
            }

            //upgrade cos
            if(cmdObj.containsKey(Command.UPGRADE_COS.val())){
                boolean upgradeCos = cmdObj.getBooleanValue(Command.UPGRADE_COS.val());
                Logger.logDebugMessage("received upgradeCos is %s ",upgradeCos);
                if(upgradeCos){
                    Logger.logDebugMessage("start to auto upgrade...");
                    ClientUpgradeTool.autoUpgrade(true);
                }
            }

            //upgrade db
            if(cmdObj.containsKey(Command.UPGRADE_DB.val())){
                String upgradeDbHeight = cmdObj.getString(Command.UPGRADE_DB.val());
                Logger.logDebugMessage("received upgradeDb command and upgrade db height is %s ", upgradeDbHeight);
                if(StringUtils.isNotEmpty(upgradeDbHeight)){
                    Logger.logDebugMessage("start to fetch archived db file from oss and upgrade local db...");
                    ClientUpgradeTool.upgradeDbFile(upgradeDbHeight);
                }
            }

            // reset command process
            if(cmdObj.containsKey(Command.RESET.val())){
                boolean needReset = cmdObj.getBooleanValue(Command.RESET.val());
                if(needReset) Conch.getBlockchainProcessor().fullReset();;
            }
            
            // restart command process
            if(cmdObj.containsKey(Command.RESTART.val())){
                boolean needRestart = cmdObj.getBooleanValue(Command.RESTART.val());
                if(needRestart){
                    new Thread(() -> Conch.restartApplication(null)).start();
                }
            }

            // pause command process
            if(cmdObj.containsKey(Command.PAUSE_SYNC.val())){
                boolean pauseSyn = cmdObj.getBooleanValue(Command.PAUSE_SYNC.val());
                if(pauseSyn){
                    Conch.pause();
                }else{
                    Conch.unpause();
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
    
    public static void manualReset(){
       try{
            Conch.pause();
            
            Logger.logDebugMessage("start to reset the blockchain");
//            Conch.getBlockchainProcessor().fullReset();
            FileUtil.deleteDbFolder();
            FileUtil.clearAllLogs();
            
        }catch (RuntimeException | FileNotFoundException e) {
            Logger.logErrorMessage("reset failed", e);
        }finally {
            Conch.unpause();
        }
    
    }
    
    static boolean reset = false;
    static final String PROPERTY_FORK_NAME = "sharder.forkName";
    public static void writeForkNameIntoPropertiesFile(){
        HashMap<String, String> parameters = Maps.newHashMap();
        parameters.put(PROPERTY_FORK_NAME, "Giant");
        Conch.storePropertiesToFile(parameters);
    }
    
    public static void switchFork(){
//        if(!Constants.isTestnet() || Conch.versionCompare("0.1.6") > 0 || Generator.isBootNode) return;
        if(Conch.versionCompare("0.1.6") > 0 || Generator.isBootNode) return;
        
        Logger.logInfoMessage("Start to switch the fork to Giant");
        String forkName = Conch.getStringProperty(PROPERTY_FORK_NAME);
        if(StringUtils.isEmpty(forkName) || !"Giant".equals(forkName)) {
            if(!reset) {
                manualReset();
                Logger.logDebugMessage("pause the blockchain till fork switched...");
                Conch.pause();
                reset = true;
            }
        }else{
            Logger.logInfoMessage("Current node stay on the " + forkName + " already, no needs to switch");
            return;
        }
        
        Peers.checkOrConnectBootNode();

        Logger.logInfoMessage("start to check converge command and finish the fork switch");
        com.alibaba.fastjson.JSONObject cmdObj = getCmdTools();
        if(cmdObj == null) {
            Logger.logInfoMessage("can't found own [%s] converge command, wait for next round check", Generator.getAutoMiningRS());
            return;
        }
            
        Logger.logDebugMessage("force converge command is: " + cmdObj.toJSONString());
        // check and unpause
        if(!cmdObj.containsKey(Command.PAUSE_SYNC.val())) {
            Logger.logInfoMessage("can't found command [%s] to resume syncing, wait for next round check", Command.PAUSE_SYNC.val());
            return;
        }
        
        boolean unpause = !cmdObj.getBooleanValue(Command.PAUSE_SYNC.val());
        if(unpause) {
            Conch.unpause();
            writeForkNameIntoPropertiesFile();
            Logger.logInfoMessage("Switch to fork Giant successfully, start to syncing blocks...");
        }
    }
    
    public static void init() {
        String forkName = Conch.getStringProperty(PROPERTY_FORK_NAME);
        if(StringUtils.isEmpty(forkName) || !"Giant".equals(forkName)){
            switchFork(); // execute immediately once
            ThreadPool.scheduleThread("switchForkThread", switchForkThread, 5, TimeUnit.MINUTES);  
        }
    }

    private static final Runnable switchForkThread = () -> {
        try {
            switchFork();
        } catch (Exception e) {
            Logger.logErrorMessage("Switch fork thread interrupted caused by %s", e.getMessage());
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }
    };
    
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
