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
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.chain.CheckSumValidator;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.consensus.poc.PocScore;
import org.conch.consensus.poc.db.PocDb;
import org.conch.consensus.poc.db.PoolDb;
import org.conch.consensus.reward.RewardCalculator;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.mint.Generator;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peers;
import org.conch.tools.ClientUpgradeTool;
import org.conch.tx.Transaction;
import org.conch.util.FileUtil;
import org.conch.util.Logger;
import org.conch.util.RestfulHttpClient;
import org.conch.util.ThreadPool;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class ForceConverge extends APIServlet.APIRequestHandler {

    static final ForceConverge INSTANCE = new ForceConverge();

    private ForceConverge() {
        super(new APITag[] {APITag.DEBUG});
    }
    
    enum Command {
        TO_HEIGHT("toHeight")
        ,KEEP_TX("keepTx")
        ,PAUSE_SYNC("pauseSyn")
        ,UPGRADE_COS("upgradeCos")
        ,UPGRADE_DB("upgradeDb")
        ,RESET("reset")
        ,RESTART("restart")
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
                        _manualReset();
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
                    Logger.logDebugMessage("start to upgrade...");
                    ClientUpgradeTool.upgradeCos(true);
                }
            }

            //upgrade db
            if(cmdObj.containsKey(Command.UPGRADE_DB.val())){
                String upgradeDbHeight = cmdObj.getString(Command.UPGRADE_DB.val());
                Logger.logDebugMessage("received upgradeDb command and upgrade db height is %s ", upgradeDbHeight);
                if(StringUtils.isNotEmpty(upgradeDbHeight)){
                    Logger.logDebugMessage("start to fetch archived db file from oss and upgrade local db...");
                    ClientUpgradeTool.restoreDbAtHeight(upgradeDbHeight);
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
    
    private static void _manualReset(){
       try{
            Conch.pause();

            Logger.logInfoMessage("[ManualReset] start to manual reset...");
            FileUtil.deleteDbFolder();
            FileUtil.clearAllLogs();
            Logger.logInfoMessage("[ManualReset] manual reset finished");

            Logger.logDebugMessage("[ManualReset] set the manualReset setting to false");
            Conch.storePropertieToFile(PROPERTY_MANUAL_RESET, "false");
        }catch (RuntimeException | FileNotFoundException e) {
            Logger.logErrorMessage("reset failed", e);
        }finally {
            Conch.unpause();
        }
    }

    public static void checkOrManualReset(){
        // manual reset
        String resetStr = Conch.getStringProperty(PROPERTY_MANUAL_RESET, null);
        boolean needManualReset = StringUtils.isEmpty(resetStr) ? true : Boolean.valueOf(resetStr);
        if(!needManualReset
        || Generator.isBootNode) {
            return;
        }
        _manualReset();
    }

    public static final String PROPERTY_FORK_NAME = "sharder.forkName";
    public static final String PROPERTY_MANUAL_RESET = "sharder.manualReset";
    public static String currentFork = Conch.getStringProperty(PROPERTY_FORK_NAME);
    public static void forceSwitchForkAccordingToCmdTool(){
        if(Conch.versionCompare("0.1.6") > 0 || Generator.isBootNode) return;
        
        Logger.logInfoMessage("Start to switch the fork to Giant");
 
        if(StringUtils.isEmpty(currentFork) || !"Giant".equals(currentFork)) {
            Logger.logDebugMessage("pause the blockchain till fork switched...");
            Conch.pause();
        }else{
            Logger.logInfoMessage("Current node stay on the " + currentFork + " already, no needs to switch");
            return;
        }
        
        Peers.checkOrConnectBootNodeRandom(false);

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
            // writeForkNameIntoPropertiesFile
            Conch.storePropertieToFile(PROPERTY_FORK_NAME, "Giant");
            Logger.logInfoMessage("Switch to fork Giant successfully, start to syncing blocks...");
        }
    }

    static final String PROPERTY_CLOSE_AUTO_UPGRADE = "sharder.closeAutoUpgrade";
    public static void autoUpgrade(){
        try {
            com.alibaba.fastjson.JSONObject cosVerObj = ClientUpgradeTool.fetchLastCosVersion();
            String version = cosVerObj.getString("version");
            String updateTime = cosVerObj.getString("updateTime");
            
            boolean foundNewVersion = Conch.versionCompare(version, updateTime) == -1;
            if(!foundNewVersion) return;
            
            Logger.logInfoMessage("[AutoUpgrade] Found a new version %s release date %s, auto upgrade current COS version %s to it"
                    , version, updateTime, Conch.getVersion());
            ClientUpgradeTool.upgradePackageThread(cosVerObj,true);
            
        } catch (Exception e) {
            Logger.logErrorMessage("autoUpgrade occur unknown exception", e);
        }
    }
    
    // remove the checkOrForceDeleteBakFolder function in the v0.1.9 and later COS versions
    private static void checkOrForceDeleteBakFolder(){
        try {
            String version = "0.1.8";
            String updateTime = "2019-08-20 19:19:19";

            boolean forceDelBakFolder = Conch.versionCompare(version, updateTime) <= 0;
            if(!forceDelBakFolder) return;

            Logger.logInfoMessage("Force delete the bak folder when the COS version is %s %s", Conch.getFullVersion(), Conch.getCosUpgradeDate());
            FileUtil.deleteDirectory(Paths.get(".","bak"));

        } catch (Exception e) {
            Logger.logErrorMessage("forceDeleteBakFolder occur unknown exception", e);
        }
    }

    public static final String PROPERTY_RESET_FOR_OLD_NETWORK = "sharder.resetForOldClient";
    public static final boolean resetForOldClient = Conch.getBooleanProperty(PROPERTY_RESET_FOR_OLD_NETWORK, false);
    /**
     * New Testnet is start form version v0.0.5
     */
    private static void checkOrResetOldClients(){
        if(!resetForOldClient) {
            return;
        }

        try {
            String version = "0.0.5";
            String updateTime = "2020-11-03 01:01:01";
            boolean forceReset = Conch.versionCompare(version, updateTime) <= 0;

            if(forceReset) {
                _manualReset();
                Conch.restartApplication(null);
            }

        } catch (Exception e) {
            Logger.logErrorMessage("checkOrResetOldClients occur unknown exception", e);
        }
    }

    public static final boolean forcePause = false;
    private static void forcePauseAndWait(){
        if(!forcePause) {
            return;
        }

        try {
            String version = "0.0.5";
            String updateTime = "2020-11-04 20:01:01";
            boolean execution = Conch.versionCompare(version, updateTime) <= 0;

        } catch (Exception e) {
            Logger.logErrorMessage("[ForcePause] checkOrResetOldClients occur unknown exception", e);
        }
    }
    private static final Runnable forcePauseThread = () -> {
        try {
            forcePauseAndWait();
        } catch (Exception e) {
            Logger.logErrorMessage("[ForcePause] Force pause thread interrupted caused by %s", e.getMessage());
        } catch (Throwable t) {
            Logger.logErrorMessage("[ForcePause] CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }
    };

    public static void init() {
        // auto upgrade
        boolean closeAutoUpgrade = Conch.getBooleanProperty(PROPERTY_CLOSE_AUTO_UPGRADE);
        if(!closeAutoUpgrade) {
            int intervalInMinutes = Constants.isDevnet() ? 1 : 60;
            Logger.logInfoMessage("[AutoUpgrade] Open the auto upgrade on this node, check interval is %d minutes", intervalInMinutes);
            ThreadPool.scheduleThread("cosAutoUpgradeThread", autoUpgradeThread, intervalInMinutes, TimeUnit.MINUTES);
        }

//       checkOrForceDeleteBakFolder();
        checkOrManualReset();
        checkOrResetOldClients();
//        // switch fork
//        if(StringUtils.isEmpty(currentFork) || !"Giant".equals(currentFork)){
//            forceSwitchForkAccordingToCmdTool(); // execute immediately once
//            ThreadPool.scheduleThread("switchForkThread", switchForkThread, 5, TimeUnit.MINUTES);  
//        }
        
        // correct the blockchain of Testnet
        // Conch.getBlockchainProcessor().addListener(block -> resetPoolAndAccounts(block), BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
    }


    /**
     * Reset the blockchain to correct the account balance of Testnet
     */
    public static void resetPoolAndAccounts(Block block){
        if(Constants.POC_LEDGER_RESET_HEIGHT == -1){
            Logger.logInfoMessage("Constants.POC_LEDGER_RESET_HEIGHT is -1, don't resetPoolAndAccounts");
            return;
        }

        boolean reachHeight = (block.getHeight() == Constants.POC_LEDGER_RESET_HEIGHT);
        
        if(!Constants.isTestnet() || !reachHeight) return;
        String logPrefix = "[Reset-Height " + block.getHeight() + "]";
        
        try{
            Conch.pause();
         
            Logger.logInfoMessage(logPrefix + " start to reset the pools and accounts to avoid the balance and block generator validation error");

            List<SharderPoolProcessor> poolProcessors = PoolDb.list(SharderPoolProcessor.State.DESTROYED.ordinal(), false);
            int successCount = 0;
            for(SharderPoolProcessor pool : poolProcessors){
                try{
                    pool.setState(SharderPoolProcessor.State.DESTROYED);
//                    pool.destroy(Conch.getHeight());
                    successCount++;
                }catch (Exception e) {
                    Logger.logWarningMessage(logPrefix + "can't destroy pool, ignore it[" + pool.toJsonStr() + "]", e);
                }
            }
            PoolDb.saveOrUpdate(null, poolProcessors);
            SharderPoolProcessor.instFromDB();
            Logger.logInfoMessage(logPrefix + " all pools be destroyed[size=%d,succeed=%d,failed=%d]",poolProcessors.size(),successCount,poolProcessors.size()-successCount);
            
            // get all accounts
            Map<Long, String> accountMinedBalanceMap = Maps.newHashMap();
            Connection con = null;
            try {
                con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ACCOUNT WHERE LATEST=TRUE ORDER BY HEIGHT ASC");

                ResultSet rs = pstmt.executeQuery();
               
                while(rs.next()){
                    String ba = rs.getLong("BALANCE") + "," + rs.getLong("FORGED_BALANCE");
                    accountMinedBalanceMap.put(rs.getLong("ID"), ba);
                }

            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            } finally {
                DbUtils.close(con);
            }
            
            // reset the all accounts balance
            List<PocScore> pocScoreList = Lists.newArrayList();
            String scoreRecalAccounts = "";
            try {
                con = Db.db.getConnection();
                if(accountMinedBalanceMap.size() > 0) {
                   Set<Long> accountIds = accountMinedBalanceMap.keySet();
                   for(long accountId : accountIds){
                       try {
                           Account account = Account.getAccount(accountId);

                           String ba = accountMinedBalanceMap.get(accountId);
                           if(StringUtils.isEmpty(ba) || !ba.contains(",")) continue;
                           String[] baArray = ba.split(",");
                           long balance = new Long(baArray[0]);
                           long minedBalance = new Long(baArray[1]);
                           long frozenBalance = 0;
                           if(block.getGeneratorId() == accountId) {
                               frozenBalance = 12800000000L;
                           }
                      
                           account.reset(con, balance, balance, minedBalance, frozenBalance);
                       } catch (SQLException e) {
                           e.printStackTrace();
                       } 
                   }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            } finally {
                DbUtils.close(con);
            }
            Logger.logInfoMessage(logPrefix + " accounts balance and guaranteed balance reset finished, size is " + accountMinedBalanceMap.size());

            // re-cal the all accounts poc score
            try {
                if(pocScoreList.size() > 0) {
                    con = Db.db.getConnection();
                    PocDb.batchUpdateScore(con, pocScoreList);
                    Logger.logInfoMessage(logPrefix + " accounts poc score re-calculate finished, size is " + pocScoreList.size() + ", accounts[" + scoreRecalAccounts + "]");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            } finally {
                DbUtils.close(con);
            }
           
            try {
                // block generator frozen balance calculate
                Db.db.beginTransaction();
                
                int i = Constants.SHARDER_REWARD_DELAY;
                while(i > 0){
                    Block pastBlock = Conch.getBlockchain().getBlockAtHeight(block.getHeight() - i);
    
                    for (Transaction tx : pastBlock.getTransactions()) {
                        if(!RewardCalculator.isBlockRewardTx(tx.getAttachment())) {
                            continue;
                        }

                        Constants.rewardCalculatorInstance.blockRewardDistribution(tx,false);
                    }
                    i--;
                }
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw new RuntimeException(e.toString(), e);
            } finally {
                Db.db.endTransaction();
            }
            Logger.logInfoMessage(logPrefix + " block generator and ref consignors's frozen balance update finished");

            Logger.logInfoMessage(logPrefix + " write the manual reset property to false into properties file");
            Conch.storePropertieToFile(PROPERTY_MANUAL_RESET, "false");
        }catch (RuntimeException e) {
            Logger.logErrorMessage(logPrefix + " reset pools and accounts failed", e);
        }finally {
            Conch.unpause();
        }
    }

    private static final Runnable switchForkThread = () -> {
        try {
            forceSwitchForkAccordingToCmdTool();
        } catch (Exception e) {
            Logger.logErrorMessage("Switch fork thread interrupted caused by %s", e.getMessage());
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }
    };

    private static final Runnable autoUpgradeThread = () -> {
        try {
            autoUpgrade();
        } catch (Exception e) {
            Logger.logErrorMessage("Auto upgrade thread interrupted caused by %s", e.getMessage());
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
