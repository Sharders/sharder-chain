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

package org.conch.mint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.*;
import org.conch.common.Constants;
import org.conch.consensus.poc.PocCalculator;
import org.conch.consensus.poc.PocScore;
import org.conch.crypto.Crypto;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.env.RuntimeEnvironment;
import org.conch.http.ForceConverge;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.tx.TransactionProcessorImpl;
import org.conch.util.*;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author ben 
 * @date 01/11/2018
 */
public class Generator implements Comparable<Generator> {

    public enum Event {
        GENERATION_DEADLINE, START_MINING, STOP_MINING
    }
    
    public static final int MAX_MINERS = Conch.getIntProperty("sharder.maxNumberOfMiners");
    private static final byte[] fakeMiningPublicKey = Conch.getBooleanProperty("sharder.enableFakeMining") ?
            Account.getPublicKey(Account.rsAccountToId(Conch.getStringProperty("sharder.fakeMiningAccount"))) : null;

    private static final Listeners<Generator,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, Generator> generators = new ConcurrentHashMap<>();
    private static final Collection<Generator> allGenerators = Collections.unmodifiableCollection(generators.values());
    private static volatile List<Generator> sortedMiners = null;
    private static volatile List<Long> generationMissingMinerIds = Lists.newArrayList();
    private static volatile Set<Long> blackedGenerators = Sets.newConcurrentHashSet();
    private static long lastBlockId;
    private static int delayTime = Constants.MINING_DELAY;
    private static Generator linkedGenerator = null;
    
    private static class MinerPrinter {
        static int count = 0;
        static boolean debug = true;
        private static String generatorSummary = reset();
        private static final int MAX_COUNT = Constants.isDevnet() ? 1 : 100; 
        private static final String splitter = "\n\r";

        static private String appendSplitter(String str, boolean appendEnd) {
            str = splitter + "[ DEBUG ]"  + str ;
            if(appendEnd) {
                str += splitter;
            }
            return str;
        }
        static String reset(){
            String generatorSummary = appendSplitter("--------------Active Miners-------------",false);
            generatorSummary += appendSplitter("Local Account[linked rs=" + HUB_BIND_ADDRESS + " | autoMint rs=" + AUTO_MINT_ADDRESS + "]",false);
            count=0;
            return generatorSummary;
        }
        static void putin(){
            if(sortedMiners != null && sortedMiners.size() > 0){
                for(Generator generator : sortedMiners){
                    generatorSummary += appendSplitter(Account.rsAccount(generator.accountId) + "[id=" + generator.accountId + ",poc score=" + generator.pocScore
                            + ",deadline=" + generator.deadline + ",hit=" + generator.hit + ",hitTime=" + generator.hitTime,false) + "]";
                }
            }
        }

        static void print(){
            if(!debug || (count++  <= MAX_COUNT)) return;
            putin();
            Logger.logDebugMessage(generatorSummary);
            generatorSummary = reset();
        }
        
    }

 
    private static final boolean dontWait = Conch.getBooleanProperty("sharder.stillWait");

    private static boolean forcePause = false;
    public static void pause(boolean pause){
        forcePause = pause;
    }
    
    public static boolean isPauseMining(){
        return forcePause;
    }
    
    // obsolete time delay, default value is 60 minutes
    public static final int OBSOLETE_DELAY = Constants.isDevnet() ? 1 : Conch.getIntProperty("sharder.obsoleteDelay", 60);
    private static final boolean WAIT_WHEN_OBSOLETE = true;
    /**
     * check current blockchain state:
     * - mining height
     * - block synchronization state check
     * - boot node and obsolete state check
     * - poc txns processing state check
     * @param lastBlock
     * @return
     */
    private static boolean miningConditionReached(Block lastBlock, int generationLimit){
        // boot node check before the last known height
        if(isBootNode && Conch.getHeight() < Constants.LAST_KNOWN_BLOCK) {
            if(Logger.isLevel(Logger.Level.DEBUG)) {
                Logger.logInfoMessage("[BootNode] Start to mining directly at height %d", lastBlock.getHeight());
            }else if(Logger.printNow(Logger.Generator_isMintHeightReached)) {
                Logger.logInfoMessage("[BootNode] Start to mining directly at height %d", lastBlock.getHeight());
            }
            return true;
        }

        if(Constants.isOffline && isBootNode){
            Logger.logInfoMessage("[BootNode] Keep mining in the offline mode at height %d", lastBlock.getHeight());
            return true;
        }

        // wait till Conch initialized finished
        if(!Conch.isInitialized()) {
            if(Logger.printNow(Logger.Generator_isMintHeightReached)) {
                Logger.logDebugMessage("Don't start mining till client is initialized...");
            }
            return false;
        }
        
        // last known block check for the normal nodes
        if (lastBlock == null || lastBlock.getHeight() < Constants.LAST_KNOWN_BLOCK) {
            if(Logger.printNow(Logger.Generator_isMintHeightReached)) {
                Logger.logInfoMessage("[Tip] Last known block height is " + Constants.LAST_KNOWN_BLOCK
                        + ", and current height is " + lastBlock.getHeight()
                        + ", don't mining till block sync finished");
            }
            return false;
        }
        
        if(dontWait) {
            return true;
        }

        if(Conch.getBlockchainProcessor().isDownloading()) {
            Logger.logDebugMessage("Current blockchain is downloading blocks, wait for blocks synchronizing finished...");
            return false;
        }

        if(Conch.getBlockchainProcessor().isScanning()) {
            Logger.logDebugMessage("Current blockchain is scanning, wait for scanning finished...");
            return false;
        }

        if(linkedGenerator == null) {
            String miningPR = getAutoMiningPR();
            if (StringUtils.isNotEmpty(miningPR)) {
                linkedGenerator = new Generator(miningPR.trim());
            }
        }

        if (linkedGenerator == null) {
            Logger.logDebugMessage("No linked miner, please finish the client initial or miner address linking " +
                    "firstly ...");
            return false;
        }

        if (linkedGenerator.pocScore == null) {
            linkedGenerator.setLastBlock(lastBlock);
        }

        int miningTime = linkedGenerator.getTimestamp(generationLimit);
        boolean hitMatched = verifyHit(linkedGenerator.hit, linkedGenerator.pocScore, lastBlock, miningTime);
        long secondsSinceLastBlock = Conch.getEpochTime() - Conch.getBlockchain().getLastBlockTimestamp();
        long minutesSinceLastBlock = secondsSinceLastBlock / 60;
        if (!Conch.getBlockchainProcessor().isUpToDate()) {
            String nodeType = isBootNode ? "Boot" : "Normal";
            if (hitMatched) {
                if (!WAIT_WHEN_OBSOLETE || isBootNode) {
                    if (Logger.printNow(Logger.Generator_isBlockStuck)) {
                        Logger.logInfoMessage("Current node is %s node and blockchain state[%s] isn't " +
                                        "UP_TO_DATE[sinceLastBlock=%d minutes, mining trigger=%d min delay], " +
                                        "still mining when the miner[%s] hit is matched at height %d, its mining time is %s",
                                nodeType, Peers.getMyBlockchainStateName(), minutesSinceLastBlock, OBSOLETE_DELAY,
                                linkedGenerator.rsAddress, lastBlock.getHeight(),
                                Convert.dateFromEpochTime(linkedGenerator.hitTime));
                    }
                } else {
                    if (Logger.printNow(Logger.Generator_isBlockStuck)) {
                        Logger.logInfoMessage("Current blockchain state[%s] isn't UP_TO_DATE[sinceLastBlock=%d minutes], " +
                                        "DON'T MINING when the miner[%s] hit is matched at height %d, its original estimated mining time is %s",
                                Peers.getMyBlockchainStateName(), minutesSinceLastBlock,
                                linkedGenerator.rsAddress, lastBlock.getHeight(),
                                Convert.dateFromEpochTime(linkedGenerator.hitTime));
                    }
                    return false;
                }
            } else {
                if (Logger.printNow(Logger.Generator_isBlockStuck)) {
                    Logger.logInfoMessage("Current node is %s node and blockchain state[%s] isn't " +
                                    "UP_TO_DATE[sinceLastBlock=%d minutes, trigger=%d min delay], " +
                                    "but miner[%s] hit didn't matched at height %d, its mining time is %s",
                            nodeType, Peers.getMyBlockchainStateName(), minutesSinceLastBlock, OBSOLETE_DELAY,
                            linkedGenerator.rsAddress, lastBlock.getHeight(),
                            Convert.dateFromEpochTime(linkedGenerator.hitTime));
                }
                return false;
            }
        }

        if(!Conch.getPocProcessor().pocTxsProcessed(lastBlock.getHeight())) {
            if(Logger.printNow(Logger.Generator_isPocTxsProcessed)) {
                Logger.logDebugMessage("[Tip] Delayed or old poc txs haven't processed, " +
                        "don't mining till poc txs be processed before height %d...", lastBlock.getHeight());
            }
            return false;
        }
        
        return true;
    }
    
    
    private static final Runnable generateBlocksThread = new Runnable() {

        private volatile boolean logged;

        @Override
        public void run() {
            try {
                try {
                    Conch.getBlockchain().updateLock();
                    try {
                        if(forcePause) {
                            return;
                        }

                        if(ForceConverge.forcePause){
                            return;
                        }

                        final int generationLimit = Conch.getEpochTime() - delayTime;
                        Block lastBlock = Conch.getBlockchain().getLastBlock();
                        if(!miningConditionReached(lastBlock, generationLimit)) {
                            return;
                        }
                        
                        checkOrStartAutoMining();
                       
                        if (lastBlock.getId() != lastBlockId
                            || sortedMiners == null
                            || sortedMiners.size() == 0) {
                            lastBlockId = lastBlock.getId();
                            // drop current last block, and use the previous block as the last block
                            boolean lastBlockGeneratedInGap = lastBlock.getTimestamp() > (Conch.getEpochTime() - Constants.GAP_SECONDS);
                            if (lastBlockGeneratedInGap
                                && !isBootNode) {
                                Block previousBlock = Conch.getBlockchain().getBlock(lastBlock.getPreviousBlockId());
                                for (Generator generator : generators.values()) {
                                    generator.setLastBlock(previousBlock);
                                    int miningTime = generator.getTimestamp(generationLimit);
                                    if (miningTime != generationLimit
                                        && generator.getHitTime() > 0
                                        && miningTime < lastBlock.getTimestamp()) {
                                        Logger.logDebugMessage("Mining time is missed, pop off last block [height=%d, miner=%s, id=%d] for %s"
                                            , lastBlock.getHeight(), Account.rsAccount(lastBlock.getGeneratorId()), lastBlock.getId()
                                            , generator.toString()
                                        );
                                        List<BlockImpl> poppedOffBlock = BlockchainProcessorImpl.getInstance().popOffTo(previousBlock);
                                        for (BlockImpl block : poppedOffBlock) {
                                            TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                                        }
                                        lastBlock = previousBlock;
                                        lastBlockId = previousBlock.getId();
                                        break;
                                    }
                                }
                            }

                            // active miners and trigger re-cal
                            List<Generator> forgers = new ArrayList<>();
                            for (Generator generator : generators.values()) {
                                generator.setLastBlock(lastBlock);

                                if(generator.pocScore == null) {
                                    continue;
                                }

                                if (generator.pocScore.signum() > 0
                                || isBootDirectlyMiningPhase(Conch.getHeight())) {
                                    forgers.add(generator);
                                }
                            }

                            Collections.sort(forgers);
                            sortedMiners = Collections.unmodifiableList(forgers);
                            logged = false;
                        }

                        if (!logged) {
                            for (Generator generator : sortedMiners) {
                                if (generator.getHitTime() - generationLimit > 60) {
                                    break;
                                }
//                                Logger.logDebugMessage(generator.toString());
                                logged = true;
                            }
                        }

                        MinerPrinter.print();
                        // generationMissingMinerIds.clear();

                        // mint
                        for (Generator generator : sortedMiners) {
                            if(generator.getHitTime() > generationLimit) {
                                return;
                            }
                            if(generator.mint(lastBlock, generationLimit)) {
                                return;
                            }
                            generationMissingMinerIds.add(generator.getAccountId());
                        }
                        
                    } finally {
                        Conch.getBlockchain().updateUnlock();
                    }
                } catch (Exception e) {
                    Logger.logErrorMessage("Error in block generation thread, ignore it and continue to next round", e);
                }
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS. EXIT NOW!\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };
    
    public static void blackGenerator(long generatorId){
       // temporary closed, no needs to black the generator. - 2019.07.13
       // blackedGenerators.add(generatorId);
    }

    public static boolean hasGenerationMissingAccount(){
        return generationMissingMinerIds.size() > 0;
    }
    
    public synchronized static List<Long> getAndResetMissingMiners(){
        List<Long> missingAccounts = Collections.unmodifiableList(generationMissingMinerIds);
        generationMissingMinerIds.clear();
        return missingAccounts;
    }
    
    public static final boolean isBootNode;
    static {
        if (!Constants.isLightClient) {
            ThreadPool.scheduleThread("GenerateBlocks", generateBlocksThread, 10000, TimeUnit.MILLISECONDS);
        }
        isBootNode = bootNodeCheck();
    }

    public static final boolean isBootDirectlyMiningPhase(int height) {
        return isBootNode && height <= Constants.LAST_KNOWN_BLOCK;
    }

    private static final boolean bootNodeCheck() {
        String isBootNode = System.getProperty(RuntimeEnvironment.GUIDE_ARG);
        if (StringUtils.isEmpty(isBootNode) || StringUtils.isBlank(isBootNode)) {
            return false;
        }

        return Boolean.valueOf(isBootNode);
    }

    public static void init() {
        // active generator listener for block pushed
        Conch.getBlockchainProcessor().addListener(block -> {
            long generatorId = block.getGeneratorId();
            synchronized(activeGeneratorMp) {
                if (!activeGeneratorMp.containsKey(generatorId)) {
                    activeGeneratorMp.put(generatorId,new ActiveGenerator(generatorId));
                }
            }
        }, BlockchainProcessor.Event.BLOCK_PUSHED);
    }

    public static boolean addListener(Listener<Generator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Generator> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static boolean isBlackedMiner(long minerId){
        return blackedGenerators.contains(minerId);
    }

    /**
     * the miner whether is valid
     * @param minerId account id of miner
     * @param height validation height
     * @return true-valid miner
     */
    public static boolean isValidMiner(long minerId, int height){
        if(LocalDebugTool.isLocalDebugAndBootNodeMode) {
            return true;
        }

        Account minerAccount = Account.getAccount(minerId, height);
        String minerRs = minerAccount != null ? minerAccount.getRsAddress() : "null";
        if(minerAccount == null) {
            if(Logger.printNow(Logger.Generator_startMining)
                || Logger.isLevel(Logger.Level.DEBUG)) {
                Logger.logWarningMessage("Current miner[addr=%s, id=%d] can't start auto mining or mint block " +
                        "when it's a new account at this height %d. " +
                        "Please CREATE tx by yourself or RECEIVE tx from other declared accounts",
                        minerRs, minerId, height);
            }
            return false;
        }

        if(isBlackedMiner(minerId)) {
            if(Logger.printNow(Logger.Generator_startMining)
                || Logger.isLevel(Logger.Level.DEBUG)) {
                Logger.logWarningMessage("Invalid miner [addr=%s, id=%d] can't start auto mining or mint block " +
                        "when it's in the black list! ",
                        minerRs, minerId,Conch.getHeight());
            }
            return false;
        }

        //check the peer statement
        boolean isCertifiedPeer = Conch.getPocProcessor().isCertifiedPeerBind(minerId, height);
        if(!isCertifiedPeer) {
            if(Logger.printNow(Logger.Generator_startMining)
                || Logger.isLevel(Logger.Level.DEBUG)) {
                Logger.logWarningMessage("Invalid miner %s(it didn't linked to a certified peer before the height %d) " +
                                "can't start auto mining or mint block. Maybe it didn't create a PocNodeTypeTx statement. please INIT or RESET the client firstly! ",
                        minerRs,
                        Conch.getHeight());
            }
            return false;
        }

        long accountBalanceNQT = (minerAccount != null) ? minerAccount.getEffectiveBalanceNQT(Conch.getHeight()) : 0L;
        if(accountBalanceNQT < Constants.MINING_HOLDING_LIMIT
        && height > Constants.LAST_KNOWN_BLOCK) {
            if(Logger.printNow(Logger.Generator_startMining)
                || Logger.isLevel(Logger.Level.DEBUG)) {
                Logger.logWarningMessage("Invalid miner %s can't start auto mining or mint block. Because the "+ Conch.COIN_UNIT +" holding limit of the mining is %d and current balance is %d",
                        minerRs,
                        (Constants.MINING_HOLDING_LIMIT / Constants.ONE_SS),
                        (accountBalanceNQT / Constants.ONE_SS));
            }
            return false;
        }
        return true;
    }

    public static Generator startMining(String secretPhrase) {
        if(StringUtils.isEmpty(secretPhrase)) {
            return null;
        }

        // check the mining max count if miner is not the owner of this node
        boolean isOwner = secretPhrase.equalsIgnoreCase(getAutoMiningPR());
        if(!isOwner && generators.size() >= MAX_MINERS) {
            throw new RuntimeException("The limit miners of this node is " + MAX_MINERS + ", can't allow more miners!");
        }

        // mining condition: holding limit check
        long minerId = Account.getId(secretPhrase);
        String minerRs = Account.rsAccount(minerId);
        if(!isValidMiner(minerId, Conch.getHeight())) {
            if(Logger.printNow(Logger.Generator_startMining)) {
                Logger.logWarningMessage("Current miner account[rs=%s, id=%d] isn't a valid miner, so can't start auto mining at this height %d",
                        minerRs, minerId,
                        Conch.getHeight());
            }
            return null;
        }

        /**
        // whether own the pool
        if(!SharderPoolProcessor.checkOwnPoolState(accountId, SharderPoolProcessor.State.WORKING)) {
            throw new RuntimeException("current node did't own the pool, please create pool firstly!");
        }
        
        if(!Conch.getPocProcessor().isCertifiedPeerBind(accountId)){
            throw new RuntimeException("current node type isn't the valid node");
        }
        **/

        Generator generator = new Generator(secretPhrase);
        Generator old = generators.putIfAbsent(secretPhrase, generator);
        if (old != null) {
            Logger.logDebugMessage(old + " is already mining");
            return old;
        }
        if(sortedMiners != null) {
            sortedMiners.add(generator);
        }

        listeners.notify(generator, Event.START_MINING);
        Logger.logDebugMessage(generator + " started");
        return generator;
    }

    public static Generator stopMining(String secretPhrase) {
        Generator generator = generators.remove(secretPhrase);
        if (generator != null) {
            Conch.getBlockchain().updateLock();
            try {
                sortedMiners = null;
            } finally {
                Conch.getBlockchain().updateUnlock();
            }
            Logger.logDebugMessage(generator + " stopped");
            listeners.notify(generator, Event.STOP_MINING);
        }
        return generator;
    }

    public static int stopMining() {
        int count = generators.size();
        Iterator<Generator> iterator = generators.values().iterator();
        while (iterator.hasNext()) {
            Generator generator = iterator.next();
            iterator.remove();
            Logger.logDebugMessage(generator + " stopped");
            listeners.notify(generator, Event.STOP_MINING);
        }
        Conch.getBlockchain().updateLock();
        try {
            sortedMiners = null;
        } finally {
            Conch.getBlockchain().updateUnlock();
        }
        return count;
    }

    public static Generator getGenerator(String secretPhrase) {
        return generators.get(secretPhrase);
    }

    public static int getGeneratorCount() {
        return generators.size();
    }

    public static Collection<Generator> getAllGenerators() {
        return allGenerators;
    }
    
    public static boolean containMiner(long minerId){
        for(Generator generator : allGenerators) {
            if(generator.accountId == minerId) {
                return true;
            }
        }
        return false;
    }

    /**
     * add cuurent miner into active miner list
     * @param minerId
     */
    public static void addMiner(long minerId){
        if(containMiner(minerId)) return;
        
        // add into active genera
        activeGeneratorMp.put(minerId,new ActiveGenerator(minerId));
    }
    
    public static List<Generator> getSortedMiners() {
        List<Generator> forgers = sortedMiners;
        return forgers == null ? Collections.emptyList() : forgers;
    }

    public static long getNextHitTime(long lastBlockId, int curTime) {
        try {
            BlockchainImpl.getInstance().readLock();
            
            if (lastBlockId == Generator.lastBlockId && sortedMiners != null) {
                for (Generator generator : sortedMiners) {
                    if (generator.getHitTime() >= curTime - Constants.MINING_DELAY) {
                        return generator.getHitTime();
                    }
                }
            }
            return 0;
        } finally {
            BlockchainImpl.getInstance().readUnlock();
        }
    }

    public static void setDelay(int delay) {
        Generator.delayTime = delay;
    }


    private static void reCalculateScore(int currentTime, int miningTime, Block previousBlock){
        if(currentTime > miningTime
            && currentTime > (previousBlock.getTimestamp() + Constants.GAP_SECONDS * 2)
            && linkedGenerator != null) {
            Logger.logDebugMessage("Set last block again to re-calculate the miner[%s]'s poc score to avoid stuck",
                    linkedGenerator.rsAddress);
            linkedGenerator.setLastBlock(previousBlock);
        }
    }
    /**
     * check the generate turn
     * @param hit
     * @param pocScore
     * @param previousBlock
     * @param miningTime
     * @return
     */
    public static boolean verifyHit(BigInteger hit, BigInteger pocScore, Block previousBlock, int miningTime) {
        //        if(isBootDirectlyMiningPhase(previousBlock.getHeight()+1)){
        //            return true;
        //        }
        if (pocScore == null || previousBlock == null) {
            return false;
        }

        int currentTime = Conch.getEpochTime();
        int elapsedTime = miningTime - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            if (Generator.isBootNode) {
                if (linkedGenerator != null) {
                    Logger.logDebugMessage("Set last block again to re-calculate the miner[%s]'s poc score " +
                            "and continue to validate the hit when the Boot Node's elapsed time[%d] <=0 " +
                            "and stuck on the boot node", linkedGenerator.rsAddress, elapsedTime);
                    linkedGenerator.setLastBlock(previousBlock);
                    elapsedTime = linkedGenerator.getTimestamp(Conch.getEpochTime()-delayTime) - previousBlock.getTimestamp();
                }else{
                    return false;
                }
            }else {
                Logger.logDebugMessage("Verify hit failed caused by this generator missing the mining turn " +
                        "when the elapsed time[%d] <=0", elapsedTime);
                reCalculateScore(currentTime, miningTime, previousBlock);
                return false;
            }
        }else if(elapsedTime < Constants.GAP_SECONDS){
            Logger.logDebugMessage("Verify hit failed caused by this generator's elapsed time[%d] < block gap[%d]",
                    elapsedTime, Constants.GAP_SECONDS);
            reCalculateScore(currentTime, miningTime, previousBlock);
            return false;
        }


        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(pocScore);
        int ratio = elapsedTime - Constants.GAP_SECONDS - 1;
        if(ratio <= 0) {
            ratio = 1;
        }
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(ratio));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        // check the elapsed time(in second) after previous block generated
        boolean elapsed = elapsedTime > Constants.GAP_SECONDS;

        // 3 right situations: a) last hit < current hit < current target, b) this block is elapsed, c) in offline mode
        boolean validHit = hit.compareTo(target) < 0 && (hit.compareTo(prevTarget) >= 0 || elapsed || Constants.isOffline);
        if(!validHit) {
            Logger.logDebugMessage("Verify hit failed, hit should smaller than target [hit=%d, target=%d, poc score=%d, previous target=%d, elapsed time=%d]",
                    hit, target, pocScore, prevTarget, elapsedTime);
        }
        return validHit;
    }

    public static boolean allowsFakeMining(byte[] publicKey) {
        return Constants.isTestnetOrDevnet() && publicKey != null && Arrays.equals(publicKey, fakeMiningPublicKey);
    }

    /**
     * calculate the hit of generator
     * @param publicKey the public key of the generator
     * @param block the last block
     * @return
     */
    public static BigInteger getHit(byte[] publicKey, Block block) {
        if (allowsFakeMining(publicKey)) {
            return BigInteger.ZERO;
        }

        MessageDigest digest = Crypto.sha256();
        digest.update(block.getGenerationSignature());
        byte[] generationSignatureHash = digest.digest(publicKey);
        return new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
    }

//    static long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block) {
//        return block.getTimestamp()
//                + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(effectiveBalance)).longValue();
//    }

    /**
     * calculate the hit time of the generator
     * @param pocScore poc score of the generator. you can see the: org.conch.consensus.poc.PocScore.PocCalculator
     * @param hit the hit of the generator
     * @param block  the last block
     * @return
     */
    public static long getHitTime(BigInteger pocScore, BigInteger hit, Block block) {
        return block.getTimestamp() + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(pocScore)).longValue() + Constants.GAP_SECONDS;
    }


    protected long accountId;
    protected String rsAddress;
    protected byte[] publicKey;
    protected volatile long hitTime;
    protected volatile BigInteger hit;
    protected volatile BigInteger effectiveBalance;
    protected volatile BigInteger pocScore;
    protected volatile com.alibaba.fastjson.JSONObject detailedPocScore;

    private String secretPhrase;
    private volatile long deadline;

    protected Generator() {}
    
    private Generator(String secretPhrase) {
        this.secretPhrase = secretPhrase;
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.accountId = Account.getId(publicKey);
        this.rsAddress = Account.rsAccount(accountId);
        Conch.getBlockchain().updateLock();
        try {
            if (Conch.getHeight() >= Constants.LAST_KNOWN_BLOCK) {
                setLastBlock(Conch.getBlockchain().getLastBlock());
            }
            sortedMiners = null;
        } finally {
            Conch.getBlockchain().updateUnlock();
        }
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getDeadline() {
        return deadline;
    }

    public long getHitTime() {
        return hitTime;
    }
    
    @Override
    public int compareTo(Generator g) {
        try{
            int i = this.hit.multiply(g.pocScore).compareTo(g.hit.multiply(this.pocScore));
            return i != 0 ? i : Long.compare(accountId, g.accountId);
        }catch(Exception e){
            Logger.logErrorMessage("Generator compare failed",e);
        }
       return 0;
    }

    @Override
    public String toString() {
        return "Miner[id=" + Long.toUnsignedString(accountId) + ", rs=" + rsAddress + ", poc=" + pocScore + ", deadline=" + getDeadline() + ", hit=" + hitTime + "] ";
    }

    public JSONObject toJson(boolean loadPoolInfo) {
        int elapsedTime = Conch.getEpochTime() - Conch.getBlockchain().getLastBlock().getTimestamp();
        JSONObject json = new JSONObject();
        json.put("account", Long.toUnsignedString(accountId));
        json.put("accountRS", StringUtils.isNotEmpty(rsAddress) ? rsAddress : Account.rsAccount(accountId));
        json.put("effectiveBalanceSS",  effectiveBalance);
        json.put("pocScore", pocScore);
        json.put("detailedPocScore", detailedPocScore);
        json.put("deadline", deadline);
        json.put("hitTime", hitTime);
        json.put("remaining", Math.max(deadline - elapsedTime, 0));
        CertifiedPeer boundedPeer = Conch.getPocProcessor().getBoundedPeer(accountId, Conch.getHeight());
        Peer.Type type = (boundedPeer != null) ? boundedPeer.getType() : Peer.Type.NORMAL;
        json.put("bindPeerType", type.getName());
        if(loadPoolInfo) {
            json.put("poolInfo", SharderPoolProcessor.getPoolJSON(accountId));
        }
        return json;
    }
    
    public JSONObject toBlockExplorerJson() {
        int elapsedTime = Conch.getEpochTime() - Conch.getBlockchain().getLastBlock().getTimestamp();
        JSONObject json = new JSONObject();
        json.put("account", Long.toUnsignedString(accountId));
        json.put("accountRS", StringUtils.isNotEmpty(rsAddress) ? rsAddress : Account.rsAccount(accountId));
        json.put("effectiveBalanceSS",  effectiveBalance);
        json.put("pocScore", pocScore);
        json.put("detailedPocScore", detailedPocScore);
        json.put("deadline", deadline);
        json.put("hitTime", hitTime);
        json.put("remaining", Math.max(deadline - elapsedTime, 0));
        CertifiedPeer boundedPeer = Conch.getPocProcessor().getBoundedPeer(accountId, Conch.getHeight());
        Peer.Type type = (boundedPeer != null) ? boundedPeer.getType() : Peer.Type.NORMAL;
        json.put("bindPeerType", type.getName());
        json.put("totalBlockCount", Conch.getHeight());
        int amount = BlockDb.getAmountByGenerator(accountId);
        json.put("minerBlockCount", amount);
        json.put("percentage", new DecimalFormat("0.00").format((float) (amount*100)/Conch.getHeight()).toString() + "%");
        return json;
    }

    public static void updatePocScore(PocScore pocScore){
        if(sortedMiners != null) {
            synchronized (sortedMiners) {
                sortedMiners.forEach(generator -> {
                    if (pocScore.getAccountId() == generator.getAccountId()) {
                        generator.detailedPocScore = pocScore.toJsonObject();
                        generator.pocScore = pocScore.total();
                    }
                });
            }
        } 
        
        synchronized (activeGeneratorMp) {
            if (activeGeneratorMp.containsKey(pocScore.getAccountId())) {
                Generator generator = activeGeneratorMp.get(pocScore.getAccountId());
                generator.detailedPocScore = pocScore.toJsonObject();
                generator.pocScore = pocScore.total();
            }
        }
    }

    /**
     * Need test and finish the logic
     * @param accountId
     * @param height
     * @return
     */
    private long CheckOrDeclareNewAccount(long accountId, int height){
         Account newAccount = null;
         // if the miner dose not be public to the network yet, new a account locally
         if (!Db.db.isInTransaction()) {
             try {
                 Db.db.beginTransaction();
                 Account account = Account.getAccount(accountId, height);
                 if(account == null) {
                     account = Account.addOrGetAccount(accountId);
                     account.apply(Account.getPublicKey(accountId));
                 }
                 Db.db.commitTransaction();
             } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
             } finally {
                Db.db.endTransaction();
             }
         }
         return -1;
    }
    /**
     * calculate the poc score and set the hit
     * @param lastBlock
     */
    protected void calAndSetHit(Block lastBlock) {
        if(lastBlock == null) {
            Logger.logWarningMessage("Last block is null, can't calculate the poc score and hit of account[" + rsAddress + ",id=" + accountId + "]");
            return;
        }
        
        int lastHeight = lastBlock.getHeight();
        Account account = Account.getAccount(accountId, lastHeight);
        if(account == null) {
            Logger.logWarningMessage("Current account %s [id=%d] is a new account, please create some txs or receive some money from other accounts", rsAddress, accountId);
            return;
        }

        PocScore pocScoreObj = Conch.getPocProcessor().calPocScore(account,lastHeight);
        effectiveBalance = pocScoreObj.getEffectiveBalance();
        detailedPocScore = pocScoreObj.toJsonObject();
        pocScore = pocScoreObj.total();

        if (!pocScoreObj.qualifiedMiner()) {
            hitTime = 0;
            hit = BigInteger.ZERO;
            return;
        }

        hit = getHit(publicKey, lastBlock);
        hitTime = getHitTime(pocScore, hit, lastBlock); 
    }
    
    /**
     * - cal poc score, hit, hitTime base the last block
     */
    private void setLastBlock(Block lastBlock) {
        calAndSetHit(lastBlock);
        deadline = Math.max(hitTime - lastBlock.getTimestamp(), 0);
        listeners.notify(this, Event.GENERATION_DEADLINE);
    }

    /**
     * mint a new block
     * @param lastBlock
     * @param generationLimit
     * @return
     * @throws BlockchainProcessor.BlockNotAcceptedException
     * @throws BlockchainProcessor.GeneratorNotAcceptedException
     */
    boolean mint(Block lastBlock, int generationLimit) throws BlockchainProcessor.BlockNotAcceptedException, BlockchainProcessor.GeneratorNotAcceptedException {
        int lastHeight = lastBlock.getHeight();
        if(!isValidMiner(accountId, lastHeight)){
            Logger.logWarningMessage("%s failed to mint at height %d last block's timestamp %s, because this account is invalid", this.toString(), lastHeight, Convert.dateFromEpochTime(lastBlock.getTimestamp()));
            return false;
        }

        boolean isDirectlyMiningPhase = isBootDirectlyMiningPhase(lastHeight);
        int miningTime = getTimestamp(generationLimit);
        if (!verifyHit(hit, pocScore, lastBlock, miningTime)) {
            Logger.logInfoMessage("%s failed to mint at height %d last block's timestamp %s, because hit is invalid", this.toString(), lastHeight, Convert.dateFromEpochTime(lastBlock.getTimestamp()));
            return false;
        }
        
        int currentTime = Conch.getEpochTime();
        boolean isStuck = isBootNode
                && isAutoMiningAccount(accountId)
                && Conch.getBlockchainProcessor().isObsolete();
        boolean reachDelayLimit = (currentTime - miningTime) > 30 * 60;
        String phaseStr = isDirectlyMiningPhase ? "in direct mining phase" : "stuck";
        if((isStuck && reachDelayLimit)
            || isDirectlyMiningPhase){
            Logger.logInfoMessage("[BootNode] Current blockchain was %s, use the current system time %s to replace the original generation time %s."
            , phaseStr, Convert.dateFromEpochTime(currentTime), Convert.dateFromEpochTime(miningTime));
            miningTime = currentTime - Constants.MINING_DELAY;
        }
        
        while (true) {
            try {
                BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, miningTime);
                setDelay(Constants.MINING_DELAY);
                return true;
            } catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                // the bad transaction has been expunged, try again
                if (Conch.getEpochTime() - currentTime > 10) { // give up after trying for 10 s
                    throw e;
                }
            }
        }
    }

    /**
     * 
     * @param generationLimit the time when the generator need mint the block
     * @return 
     */
    private int getTimestamp(int generationLimit) {
        return (generationLimit - hitTime > 3600) ? generationLimit : (int)hitTime + 1;
    }
    
    private static Map<Long,ActiveGenerator> activeGeneratorMp = Maps.newConcurrentMap();
    
    /** Active block identifier */
    private static long activeBlockId;

    /** Generator list has been initialized */
    private static boolean generatorsInitialized = false;
    
    /** 1months */
    private static final int MAX_ACTIVE_GENERATOR_LIFECYCLE = calActiveGeneratorCount().intValue();

    private static Long calActiveGeneratorCount(){
        // 1 month
        return 30L * (60L*60L*24L / (long)Constants.GAP_SECONDS);
    }

    /**
     * Return a list of generators for the next block.  The caller must hold the blockchain
     * read lock to ensure the integrity of the returned list.
     * 
     * Generators have 2 parts: history block generator and current node miner
     * - org.conch.mint.Generator#init() add a listener to record block generator
     * - get current node miner from sortedMiners
     * 
     * @return List of generator account identifiers
     */
    public static List<ActiveGenerator> getNextGenerators() {
        List<ActiveGenerator> generatorList;
        Blockchain blockchain = Conch.getBlockchain();
        synchronized(activeGeneratorMp) {
          
            if (!generatorsInitialized) {
                Set<Long> generatorIds = Sets.newHashSet();
                // load history block generators 
                generatorIds.addAll(BlockDb.getBlockGenerators(Math.max(1, blockchain.getHeight() - MAX_ACTIVE_GENERATOR_LIFECYCLE)));
                // load working pool creators 
                generatorIds.addAll(SharderPoolProcessor.getAllCreators());
                        
                generatorIds.forEach(generatorId -> activeGeneratorMp.put(generatorId,new ActiveGenerator(generatorId)));
                generatorsInitialized = true;
            }

            // add active miners of local node 
            List<ActiveGenerator> minersOnCurNode = new ArrayList<>();
            if(sortedMiners != null && sortedMiners.size() > 0) {
                for(Generator generator : sortedMiners){
                    if(activeGeneratorMp.containsKey(generator.getAccountId())) {
                        continue;
                    }
                    ActiveGenerator activeMiner = new ActiveGenerator(generator.accountId,generator.hitTime,generator.hit);
                    minersOnCurNode.add(activeMiner);
                }
            }

            // re-calculate poc score and hit according to last block
            Block lastBlock = blockchain.getLastBlock();
            if (lastBlock.getId() != activeBlockId) {
                activeBlockId = lastBlock.getId();
                minersOnCurNode.forEach(generator -> generator.setLastBlock(lastBlock));
                activeGeneratorMp.forEach((id, generator)-> generator.setLastBlock(lastBlock));
            }

            generatorList = Lists.newArrayList(minersOnCurNode);
            generatorList.addAll(activeGeneratorMp.values());
            Collections.sort(generatorList);
            if(Logger.printNow(Logger.Generator_getNextGenerators)){
                Logger.logDebugMessage(generatorList.size() + " generators found");
            }

            // print the poc score detail of the miner in the local debug mode
            if(LocalDebugTool.isLocalDebug()){
                String generatorDetailStr = "";
                String badHardwareScoreStr = "";
                for(ActiveGenerator generator : generatorList){
                    if(generator.detailedPocScore.getBigInteger("hardwareScore").doubleValue() > 54000){
                        badHardwareScoreStr += generator.toJson(false) + "\n";
                    }
                    generatorDetailStr += generator.toJson(false) + "\n";
                }
                //Logger.logDebugMessage("\n\rTotal Generator detail is \n\r" + generatorDetailStr);
                //Logger.logDebugMessage("\n\rTotal Bad Hardware Score detail is \n\r" + badHardwareScoreStr);
            }

            if(lastBlock.getHeight() >= Constants.MINER_REMOVE_HIGHT){
                for(Iterator<ActiveGenerator> it = generatorList.iterator(); it.hasNext();){
                    ActiveGenerator activeGenerator = it.next();
                    boolean isInTx = Db.db.isInTransaction();
                    Connection con = null;
                    try {
                        con = Db.db.getConnection();
                        PreparedStatement pstmt = con.prepareStatement("SELECT * FROM certified_peer WHERE account_id =? and delete_height != 0 and delete_height <= ?;");
                        pstmt.setString(1,activeGenerator.getAccountId()+"");
                        pstmt.setInt(2,lastBlock.getHeight());
                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) {
                            it.remove();
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }finally {
                        if (!isInTx) {
                            DbUtils.close(con);
                        }
                    }
                }
            }


        }
        return generatorList;
    }

    /**
     * Miner hardware total capacity
     * @return hardware Capacity of all active Miner
     */
    public static String hardwareCapacityActive () {
        List<ActiveGenerator> generators = getNextGenerators();
        Integer scoreTotal = 0;
        for (ActiveGenerator generator : generators) {
            Integer hardwareScore = generator.detailedPocScore.getBigInteger("hardwareScore").intValue();
            if (hardwareScore == 400) {
                continue;
            }
            scoreTotal += hardwareScore;
        }
        return PocCalculator.hardwareCapacity(new BigInteger(scoreTotal.toString()));
    }


    /**
     * Active generator
     */
    public static class ActiveGenerator extends Generator {

        public ActiveGenerator(long accountId) {
            this(accountId,Long.MAX_VALUE,BigInteger.ZERO);
        }
        
        public ActiveGenerator(long accountId, long hitTime, BigInteger hit) {
            this.accountId = accountId;
            this.publicKey = Account.getPublicKey(this.accountId);
            this.rsAddress = Account.rsAccount(this.accountId);
            this.hitTime = hitTime;
            this.hit = hit;
            setLastBlock(Conch.getBlockchain().getLastBlock());
        }

        public long getPocScore() { return pocScore.longValue(); }

        private void setLastBlock(Block lastBlock) {
            if(lastBlock == null) return;
            
            if (publicKey == null) {
                publicKey = Account.getPublicKey(accountId);
                if (publicKey == null) {
                    hitTime = Long.MAX_VALUE;
                    return;
                }
            }
            calAndSetHit(lastBlock);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(accountId);
        }

        @Override
        public boolean equals(Object obj) {
            return (obj != null && (obj instanceof ActiveGenerator) && accountId == ((ActiveGenerator)obj).accountId);
        }

        public int compareTo(ActiveGenerator obj) {
            try{
                return (hitTime < obj.hitTime ? -1 : (hitTime > obj.hitTime ? 1 : 0));
            }catch(Exception e){
                Logger.logErrorMessage("ActiveGenerator compare failed",e);
            }
            return 0;
        }
    }

    // Hub auto mining setting
    public static final String HUB_BIND_ADDRESS = Conch.getStringProperty("sharder.HubBindAddress");
    public static final Boolean HUB_IS_BIND = Conch.getBooleanProperty("sharder.HubBind");
    private static final String HUB_BIND_PR = Conch.getStringProperty("sharder.HubBindPassPhrase", "", true).trim();
    private static final String AUTO_MINT_ADDRESS = autoMintAccountRs();
    static boolean autoMintRunning = false;

    private static String autoMintPR = Convert.emptyToNull(Conch.getStringProperty("sharder.autoMint.secretPhrase", "", true));
    /**
     * local auto mint rs account
     *
     * @return
     */
    private static String autoMintAccountRs() {
        return StringUtils.isEmpty(autoMintPR) ? null : Account.rsAccount(Account.getId(autoMintPR));
    }

    private static long autoMintAccountId() {
        return StringUtils.isEmpty(autoMintPR) ? 0 : Account.getId(autoMintPR);
    }

    /**
     * force to open auto mining once
     */
    public static void forceOpenAutoMining(){
        autoMintRunning = false;
    }
    
    public static boolean isBindAddress(String rsAddress){
        if(!HUB_IS_BIND || StringUtils.isEmpty(HUB_BIND_ADDRESS)) return false;
        
        return HUB_BIND_ADDRESS.equals(rsAddress);
    }

    /**
     * check account is hub bound account or local auto mint account
     *
     * @param accountId
     * @return
     */
    public static boolean isAutoMiningAccount(long accountId){
        long bindAddrId = Account.rsAccountToId(Generator.HUB_BIND_ADDRESS);
        if(bindAddrId != 0 && bindAddrId == accountId) return true;

        long autoMintAccountId = autoMintAccountId();
        if(autoMintAccountId != 0 && autoMintAccountId == accountId) return true;
        
        return false;
    }

    /**
     * sequence: hub bound account > local auto mint account
     *
     * @return auto mint rs account
     */
    public static String getAutoMiningRS() {
        if (HUB_IS_BIND && StringUtils.isNotEmpty(HUB_BIND_PR)) {
            return Account.rsAccount(HUB_BIND_PR);
        }

        return autoMintAccountRs();
    }
    
    /**
     * sequence: cos bound account > local auto mint account
     *
     * @return pr of auto mining account
     */
    public static String getAutoMiningPR() {
        if (HUB_IS_BIND && StringUtils.isNotEmpty(HUB_BIND_PR)) {
            return HUB_BIND_PR;
        }
        return autoMintPR;
    }

    /**
     * Auto mining of Hub or Miner, just execute once
     * sequence: hub bind account > local auto mint account
     */
    public static void checkOrStartAutoMining(){
        if(autoMintRunning) {
            if(Logger.printNow(Logger.Generator_checkOrStartAutoMining, 600)) {
                if(linkedGenerator == null) {
                    Logger.logInfoMessage("Can't start auto mining because no linked account, please finish the client initial firstly ...");
                }else {
                    Logger.logInfoMessage("Account %s is mining [next mining time is %s] ...",
                            linkedGenerator.rsAddress,
                            Convert.dateFromEpochTime(linkedGenerator.hitTime));
                }
            }else{
                if(linkedGenerator == null) {
                    Logger.logDebugMessage("Can't start auto mining because no linked account, please finish the client initial firstly ...");
                }else {
                    Logger.logDebugMessage("Account %s is mining [next mining time is %s] ...",
                            linkedGenerator.rsAddress,
                            Convert.dateFromEpochTime(linkedGenerator.hitTime));
                }
            }
            return;
        }
        
        String miningPR = getAutoMiningPR();
        if(StringUtils.isNotEmpty(miningPR)) {
            linkedGenerator = startMining(miningPR.trim());
            if(linkedGenerator == null) {
                return;
            }

            Logger.logInfoMessage("Account %s start to mining [next mining time is %s] ...", linkedGenerator.rsAddress, Convert.dateFromEpochTime(linkedGenerator.hitTime));
        }
       
        if(MAX_MINERS > 0) {
            // open miner service
            Peers.checkAndAddOpeningServices(Lists.newArrayList(Peer.Service.MINER));
        }
        autoMintRunning = true;
    }

    /**
     * Stop auto mining of Hub or Miner
     */
    public static void stopAutoMining(){
       String stopAccount = null;
        if (HUB_IS_BIND && StringUtils.isNotEmpty(HUB_BIND_PR)) {
            stopAccount = HUB_BIND_PR;
        }else {
            stopAccount = Convert.emptyToNull(Conch.getStringProperty("sharder.autoMint.secretPhrase", "", true));
        }

        Logger.logInfoMessage("account " + stopAccount + " stop mining...");
        stopMining(stopAccount);
        autoMintRunning = false;
    }

    public static void main(String[] args) {
        System.out.println(Account.rsAccount(2792673654720227339L));
    }

}
