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
import org.conch.consensus.poc.PocScore;
import org.conch.crypto.Crypto;
import org.conch.env.RuntimeEnvironment;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.tx.TransactionProcessorImpl;
import org.conch.util.*;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
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
            generatorSummary += appendSplitter("Local bind account[ hub rs=" + HUB_BIND_ADDRESS + " | autoMint rs=" + AUTO_MINT_ADDRESS + " ]",false);
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
                Logger.logInfoMessage("[BootNode] Start to mining directly at height[%d].", lastBlock.getHeight());
            }else if(Logger.printNow(Logger.Generator_isMintHeightReached)) {
                Logger.logInfoMessage("[BootNode] Start to mining directly at height[%d].", lastBlock.getHeight());
            }
            return true;
        }

        if(Constants.isOffline && isBootNode){
            Logger.logInfoMessage("[BootNode] Keep mining in the offline mode at height[%d].", lastBlock.getHeight());
            return true;
        }

            // wait till Conch initialized finished
        if(!Conch.isInitialized()) {
            if(Logger.printNow(Logger.Generator_isMintHeightReached)) {
                Logger.logDebugMessage("Wait for Conch initialized...");
            }
            return false;
        }
        
        // last known block check for the normal nodes
        if (lastBlock == null || lastBlock.getHeight() < Constants.LAST_KNOWN_BLOCK) {
            if(Logger.printNow(Logger.Generator_isMintHeightReached)) {
                Logger.logWarningMessage("last known block height is " + Constants.LAST_KNOWN_BLOCK
                        + ", and current height is " + lastBlock.getHeight()
                        + ", don't mining till blocks sync finished...");
            }
            return false;
        }
        
        if(dontWait) return true;
        
        // blockchain is synchronizing blocks or stuck
        if(!Conch.getBlockchainProcessor().isUpToDate()) {
            // when blockchain be blocked and last block is obsolete, boot node need mining the block
            long secondsSinceLastBlock = Conch.getEpochTime() - 600 - Conch.getBlockchain().getLastBlockTimestamp();
            long minutesSinceLastBlock = secondsSinceLastBlock/60;
            boolean isObsoleteTime =  secondsSinceLastBlock > (60 * OBSOLETE_DELAY); // default block mining delay > 1h
            boolean foundBlockStuckOnBootNode = Conch.getBlockchainProcessor().isObsolete() && isObsoleteTime && isBootNode;
            
            if(linkedGenerator == null) {
                String miningPR = getAutoMiningPR();
                if(StringUtils.isNotEmpty(miningPR)) {
                    linkedGenerator = new Generator(miningPR.trim());
                }
            }
            
            if(foundBlockStuckOnBootNode && linkedGenerator != null) {
                int timestamp = linkedGenerator.getTimestamp(generationLimit);
                if (verifyHit(linkedGenerator.hit, linkedGenerator.pocScore, lastBlock, timestamp)) {
                    Logger.logInfoMessage("[BootNode] Current blockchain was stuck[sinceLastBlock=%d minutes], but boot node should keep mining when the miner[%s]' hit is matched at height[%d].", minutesSinceLastBlock,linkedGenerator.rsAddress, lastBlock.getHeight());
                }else{
                    Logger.logDebugMessage("[BootNode] Current blockchain was stuck[sinceLastBlock=%d minutes], but boot node miner[%s]'s hit[%d] didn't matched now at height[%d], wait for next round check.", minutesSinceLastBlock,linkedGenerator.rsAddress,linkedGenerator.hit, lastBlock.getHeight());
                    return false;
                }
            }else{
                if(Logger.printNow(Logger.Generator_isBlockStuckOnBootNode)) {
                    String nodeType = isBootNode ? "Boot" : "Normal";
                    Logger.logInfoMessage("[ TIPS ] Current node is %s node and block chain state isn't UP_TO_DATE, maybe it is downloading blocks or stuck at height[%d]. wait for blocks synchronizing finished...", nodeType, lastBlock.getHeight());
                }
                return false;
            }
            
        }
        
        if(!Conch.getPocProcessor().pocTxsProcessed(lastBlock.getHeight())) {
            if(Logger.printNow(Logger.Generator_isPocTxsProcessed)) {
                Logger.logDebugMessage("[ TIPS ] Delayed or old poc txs haven't processed, don't mining till poc txs be processed before height[%d]...", lastBlock.getHeight());
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
                    BlockchainImpl.getInstance().updateLock();
                    try {
                        if(forcePause) return;

                        final int generationLimit = Conch.getEpochTime() - delayTime;
                        Block lastBlock = Conch.getBlockchain().getLastBlock();
                        if(!miningConditionReached(lastBlock, generationLimit)) return;
                        
                        checkOrStartAutoMining();
                       
                        if (lastBlock.getId() != lastBlockId || sortedMiners == null || sortedMiners.size() == 0) {
                            lastBlockId = lastBlock.getId();
                            if (lastBlock.getTimestamp() > Conch.getEpochTime() - 600) {
                                Block previousBlock = Conch.getBlockchain().getBlock(lastBlock.getPreviousBlockId());
                                for (Generator generator : generators.values()) {
                                    generator.setLastBlock(previousBlock);
                                    int timestamp = generator.getTimestamp(generationLimit);
                                    if (timestamp != generationLimit && generator.getHitTime() > 0 && timestamp < lastBlock.getTimestamp()) {
                                        Logger.logDebugMessage("Pop off: " + generator.toString() + " will pop off last block " + lastBlock.getStringId());
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

                            List<Generator> forgers = new ArrayList<>();
                            for (Generator generator : generators.values()) {
                                generator.setLastBlock(lastBlock);

                                if(generator.pocScore == null) continue;

                                if (generator.pocScore.signum() > 0) {
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
                        BlockchainImpl.getInstance().updateUnlock();
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

    /**
     * generator is not in the black list and it be bind to a certified node
     * @param generatorId
     * @return
     */
    public static boolean isValid(long generatorId, int height){
        return !blackedGenerators.contains(generatorId);
//        return Conch.getPocProcessor().isCertifiedPeerBind(generatorId) && !blackedGenerators.contains(generatorId);
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
    
    private static final boolean bootNodeCheck() {
        String isBootNode = System.getProperty(RuntimeEnvironment.BOOTNODE_ARG);
        if (StringUtils.isEmpty(isBootNode) || StringUtils.isBlank(isBootNode)) return false;

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
        if(LocalDebugTool.isLocalDebugAndBootNodeMode) return true;

        Account minerAccount = Account.getAccount(minerId, height);
        if(minerAccount == null) {
            if(Logger.printNow(Logger.Generator_startMining)) {
                Logger.logWarningMessage("Current miner account[id=%d] can't start auto mining or mint block. Because it is a new account at this height %d, please create some txs or receive some coins from other accounts", minerId, height);
            }
            return false;
        }

        if(isBlackedMiner(minerId)) {
            if(Logger.printNow(Logger.Generator_startMining)) {
                Logger.logWarningMessage("Invalid miner account %s can't start auto mining or mint block. Because this account is in the black list! ",
                        minerAccount.getRsAddress(),
                        Conch.getHeight());
            }
            return false;
        }

        //check the peer statement
        boolean isCertifiedPeer = Conch.getPocProcessor().isCertifiedPeerBind(minerId, height);
        if(!isCertifiedPeer) {
            if(Logger.printNow(Logger.Generator_startMining)) {
                Logger.logWarningMessage("Invalid miner account %s(it didn't linked to a certified peer before the height %d) can't start auto mining or mint block. " +
                                "Maybe it didn't create a PocNodeTypeTx to statement. please INIT or RESET the client firstly! ",
                        minerAccount.getRsAddress(),
                        Conch.getHeight());
            }
            return false;
        }

        long accountBalanceNQT = (minerAccount != null) ? minerAccount.getEffectiveBalanceNQT(Conch.getHeight()) : 0L;
        if(accountBalanceNQT < Constants.MINING_HOLDING_LIMIT) {
            if(Logger.printNow(Logger.Generator_startMining)) {
                Logger.logWarningMessage("Invalid miner account %s can't start auto mining or mint block. Because the coin holding limit of the mining is %d and current balance is %d",
                        minerAccount.getRsAddress(),
                        (Constants.MINING_HOLDING_LIMIT / Constants.ONE_SS),
                        (accountBalanceNQT / Constants.ONE_SS));
            }
            return false;
        }
        return true;
    }

    public static Generator startMining(String secretPhrase) {
        if(StringUtils.isEmpty(secretPhrase)) return null;

        // check the mining max count if miner is not the owner of this node
        boolean isOwner = secretPhrase.equalsIgnoreCase(getAutoMiningPR());
        if(!isOwner && generators.size() >= MAX_MINERS) {
            throw new RuntimeException("The limit miners of this node is " + MAX_MINERS + ", can't allow more miners!");
        }

        // mining condition: holding limit check
        long minerAccountId = Account.getId(secretPhrase);
        if(!isValidMiner(minerAccountId, Conch.getHeight())) {
            if(Logger.printNow(Logger.Generator_startMining)) {
                Logger.logWarningMessage("Current miner account[id=%d] isn't a valid miner, so can't start auto mining at this height %d",
                        minerAccountId,
                        Conch.getHeight());
            }
            return null;
        }

        Generator generator = new Generator(secretPhrase);
        Generator old = generators.putIfAbsent(secretPhrase, generator);
        if (old != null) {
            Logger.logDebugMessage(old + " is already mining");
            return old;
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

    /**
     * check the generate turn
     * @param hit
     * @param pocScore
     * @param previousBlock
     * @param timestamp
     * @return
     */
    public static boolean verifyHit(BigInteger hit, BigInteger pocScore, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            if(Generator.isBootNode) {
                Logger.logWarningMessage("continue to validate the hit when the Boot Node's elapsed time[%d] <=0 to avoid the block stuck in the single boot node situation", elapsedTime);
            }else {
                Logger.logWarningMessage("verify hit failed caused by this generator missing the turn to generate when the elapsed time[%d] <=0", elapsedTime);
                return false;
            }
        }else if(elapsedTime < Constants.getBlockGapSeconds()){
            Logger.logWarningMessage("verify hit failed caused by this generator's elapsed time[%d] is in the block gap[%d]", elapsedTime,Constants.getBlockGapSeconds() );
            return false;
        }
        
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(pocScore);
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - Constants.getBlockGapSeconds() - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        // check the elapsed time(in second) after previous block generated
        boolean elapsed = elapsedTime > Constants.getBlockGapSeconds();
        
        // 3 right situations: a) last hit < current hit < current target, b) this block is elapsed, c) in offline mode
        boolean validHit = hit.compareTo(target) < 0 && (hit.compareTo(prevTarget) >= 0 || elapsed || Constants.isOffline);
        if(!validHit) {
            Logger.logDebugMessage("verify hit failed, hit should smaller than target [hit=%d, target=%d, poc score=%d, previous target=%d, elapsed time=%d]",hit, target, pocScore, prevTarget, elapsedTime);
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
        return block.getTimestamp() + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(pocScore)).longValue() + Constants.getBlockGapSeconds();
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
            if (Conch.getBlockchain().getHeight() >= Constants.LAST_KNOWN_BLOCK) {
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
        return "Miner[id=" + Long.toUnsignedString(accountId) + ", rs=" + rsAddress + ", poc score=" + pocScore + "] deadline " + getDeadline() + " hit " + hitTime;
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
     * calculate the poc score and set the hit
     * @param lastBlock
     */
    protected void calAndSetHit(Block lastBlock) {
        if(lastBlock == null) {
            Logger.logWarningMessage("last block is null, can't calculate the poc score and hit of account[" + rsAddress + ",id=" + accountId + "]");
            return;
        }
        
        int lastHeight = lastBlock.getHeight();
        Account account = Account.getAccount(accountId, lastHeight);
        if(account == null) {
            Logger.logWarningMessage("current account %s [id=%d] is a new account, please create some txs or receive some SS from other accounts", rsAddress, accountId);
            return;
        }
        /**
        // if the miner dose not be public to the network yet, new a account locally
        if (!Db.db.isInTransaction()) {
            try {
                Db.db.beginTransaction();
                if(account == null) {
                    Account.addOrGetAccount(accountId).apply(getPublicKey());
                }
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
        }
        **/
    
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
        if(!isValidMiner(accountId, lastBlock.getHeight())){
            Logger.logWarningMessage("%s failed to mint at height %d last timestamp %d, because this account is invalid miner.", this.toString(), lastBlock.getHeight(), lastBlock.getTimestamp());
            return false;
        }

        int timestamp = getTimestamp(generationLimit);
        if (!verifyHit(hit, pocScore, lastBlock, timestamp)) {
            Logger.logInfoMessage(this.toString() + " failed to mint at " + timestamp + " height " + lastBlock.getHeight() + " last timestamp " + lastBlock.getTimestamp());
            return false;
        }
        
        int start = Conch.getEpochTime();
        if(isBootNode 
        && isAutoMiningAccount(accountId)
        && Conch.getBlockchainProcessor().isObsolete()){
            Logger.logInfoMessage("[BootNode] Current blockchain was stuck, so use the current system time %d to replace the original block generation time %d."
                    , start, timestamp);
            timestamp = start;
        }
        
        while (true) {
            try {
                BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, timestamp);
                setDelay(Constants.MINING_DELAY);
                return true;
            } catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                // the bad transaction has been expunged, try again
                if (Conch.getEpochTime() - start > 10) { // give up after trying for 10 s
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
        return 30L * (60L*60L*24L / (long)Constants.getBlockGapSeconds());
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
        }
        return generatorList;
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
    private static final String HUB_BIND_ADDRESS = Conch.getStringProperty("sharder.HubBindAddress");
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
     * sequence: hub bound account > local auto mint account
     *
     * @return pr of auto mining account
     */
    private static String getAutoMiningPR() {
        // [Hub Miner] if owner bind the passphrase then start mine automatic
        if (HUB_IS_BIND && StringUtils.isNotEmpty(HUB_BIND_PR)) {
            return HUB_BIND_PR;
        }
        // [Normal Miner] if owner set the passphrase of autoMint then start mining
        return Convert.emptyToNull(Conch.getStringProperty("sharder.autoMint.secretPhrase", "", true));
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

            if(linkedGenerator == null) return;

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
