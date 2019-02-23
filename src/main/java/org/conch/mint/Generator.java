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
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.consensus.poc.PocScore;
import org.conch.crypto.Crypto;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.tx.TransactionProcessorImpl;
import org.conch.util.*;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

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
    
    
    private static class MinerPrinter {
        static int count = 0;
        static boolean debug = true;
        private static String generatorSummary = reset();

        private static final String splitter = "\n\r";

        static private String appendSplitter(String str, boolean appendEnd) {
            str = splitter + str ;
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
        static void putin(Generator generator){
            generatorSummary += appendSplitter(Account.rsAccount(generator.accountId) + "[id=" + generator.accountId + ",poc score=" + generator.pocScore 
                    + ",deadline=" + generator.deadline + ",hit=" + generator.hit + ",hitTime=" + generator.hitTime,false);
        }

        static void print(){
            if(!debug || (count++  <= 100)) return;
           
            Logger.logDebugMessage(generatorSummary);
            generatorSummary = reset();
        }
        
    }
    
    private static final Runnable generateBlocksThread = new Runnable() {

        private volatile boolean logged;

        @Override
        public void run() {
            try {
                try {
                    autoMining();
                    
                    BlockchainImpl.getInstance().updateLock();
                    try {
                        Block lastBlock = Conch.getBlockchain().getLastBlock();
                        //wait for last known block
                        if (lastBlock == null || lastBlock.getHeight() < Constants.LAST_KNOWN_BLOCK) {
                            return;
                        }

                        final int generationLimit = Conch.getEpochTime() - delayTime;
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
                                if (generator.pocScore.signum() > 0) {
                                    forgers.add(generator);
                                    MinerPrinter.putin(generator);
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
                    Logger.logErrorMessage("Error in block generation thread", e);
                }
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };
    
    public static void blackGenerator(long generatorId){
        blackedGenerators.add(generatorId);
    }

    /**
     * generator is not in the black list and it be bind to a certified node
     * @param generatorId
     * @return
     */
    public static boolean isValid(long generatorId){
        return PocProcessorImpl.isCertifiedPeerBind(generatorId) && !blackedGenerators.contains(generatorId);
    }

    public static boolean hasGenerationMissingAccount(){
        return generationMissingMinerIds.size() > 0;
    }
    
    public synchronized static List<Long> getAndResetMissingMiners(){
        List<Long> missingAccounts = Collections.unmodifiableList(generationMissingMinerIds);
        generationMissingMinerIds.clear();
        return missingAccounts;
    }

    static {
        if (!Constants.isLightClient) {
            ThreadPool.scheduleThread("GenerateBlocks", generateBlocksThread, 500, TimeUnit.MILLISECONDS);
        }
    }

    public static void init() {}

    public static boolean addListener(Listener<Generator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Generator> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    /**
     * the owner of node start mining
     * @param secretPhrase
     * @return
     */
    public static Generator ownerMining(String secretPhrase) {
        return startMining(secretPhrase,true);
    }

    /**
     * normal accountS start mining
     * @param secretPhrase
     * @return
     */
    public static Generator startMining(String secretPhrase) {
        return startMining(secretPhrase,false);
    }

    private static Generator startMining(String secretPhrase, boolean isOwner) {
        // if miner is not the owner of the node
        if(!isOwner) {
            if(!Peers.isOpenService(Peer.Service.MINER)) {
                throw new RuntimeException("the proxy mint service of this node isn't open, can't allow miners to mining!");
            }else if(generators.size() >= MAX_MINERS) {
                throw new RuntimeException("the limit miners of this node is setting to " + MAX_MINERS + ", can't allow more miners!");
            }
//            long accountId = Account.getId(secretPhrase);
//            if(!PocProcessorImpl.isHubBind(accountId)) {
//                Logger.logInfoMessage("Account[id=" + accountId  + "] is not be bind to hub");
//            }
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

    public static List<Generator> getSortedMiners() {
        List<Generator> forgers = sortedMiners;
        return forgers == null ? Collections.emptyList() : forgers;
    }

    public static long getNextHitTime(long lastBlockId, int curTime) {
        BlockchainImpl.getInstance().readLock();
        try {
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
            Logger.logDebugMessage("this generator missing the generation turn because the elapsed time <=0");
            return false;
        }else if(elapsedTime < Constants.getBlockGapSeconds()){
            Logger.logDebugMessage("this generator is in the block gap because the elapsed time < block gap[" + Constants.getBlockGapSeconds() + "]");
            return false;
        }
        
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(pocScore);
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - Constants.getBlockGapSeconds() - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        // check the elapsed time(in second) after previous block generated
        boolean elapsed = Constants.isTestnetOrDevnet() ? elapsedTime > 300 : elapsedTime > 3600;
        return hit.compareTo(target) < 0 && (hit.compareTo(prevTarget) >= 0 || elapsed || Constants.isOffline);
        
//        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(effectiveBalance);
//        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - 421));
//        BigInteger target = prevTarget.add(effectiveBaseTarget);
//        return hit.compareTo(target) < 0
//                && (previousBlock.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_8
//                || hit.compareTo(prevTarget) >= 0
////                || (Constants.isTestnet ? elapsedTime > 300 : elapsedTime > 3600)
//                || (Constants.isTestnet ? elapsedTime > 300 : elapsedTime > 300)
//                || Constants.isOffline);
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
    protected byte[] publicKey;
    protected volatile long hitTime;
    protected volatile BigInteger hit;
    protected volatile BigInteger effectiveBalance;
    protected volatile BigInteger pocScore;

    private String secretPhrase;
    private volatile long deadline;

    protected Generator() {}
    
    private Generator(String secretPhrase) {
        this.secretPhrase = secretPhrase;
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.accountId = Account.getId(publicKey);
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
        int i = this.hit.multiply(g.pocScore).compareTo(g.hit.multiply(this.pocScore));
        return i != 0 ? i : Long.compare(accountId, g.accountId);
    }

    @Override
    public String toString() {
        return "Miner[id=" + Long.toUnsignedString(accountId) + ", poc score=" + pocScore + "] deadline " + getDeadline() + " hit " + hitTime;
    }

    public JSONObject toJson(boolean loadPoolInfo) {
        int elapsedTime = Conch.getEpochTime() - Conch.getBlockchain().getLastBlock().getTimestamp();
        JSONObject json = new JSONObject();
        json.put("account", Long.toUnsignedString(accountId));
        json.put("accountRS", Account.rsAccount(accountId));
        json.put("effectiveBalanceSS",  effectiveBalance);
        json.put("pocScore", pocScore);
        json.put("deadline", deadline);
        json.put("hitTime", hitTime);
        json.put("remaining", Math.max(deadline - elapsedTime, 0));
        json.put("bindPeerType", PocProcessorImpl.bindPeerType(accountId).getName());
        if(loadPoolInfo) {
            json.put("bindPeerType", SharderPoolProcessor.getPoolJSON(accountId));
        }
        return json;
    }

    /**
     * calculate the poc score and set the hit
     * @param lastBlock
     */
    protected void calAndSetHit(Block lastBlock) {
        int lastHeight = lastBlock.getHeight();
        Account account = Account.getAccount(accountId, lastHeight);

        effectiveBalance = PocScore.calEffectiveBalance(account,lastHeight);

        pocScore = PocProcessorImpl.instance.calPocScore(account,lastHeight);

        if (pocScore.signum() <= 0) {
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
     * mint the block
     * @param lastBlock
     * @param generationLimit
     * @return
     * @throws BlockchainProcessor.BlockNotAcceptedException
     * @throws BlockchainProcessor.GeneratorNotAcceptedException
     */
    boolean mint(Block lastBlock, int generationLimit) throws BlockchainProcessor.BlockNotAcceptedException, BlockchainProcessor.GeneratorNotAcceptedException {
        int timestamp = getTimestamp(generationLimit);
        if (!verifyHit(hit, pocScore, lastBlock, timestamp)) {
            Logger.logDebugMessage(this.toString() + " failed to mint at " + timestamp + " height " + lastBlock.getHeight() + " last timestamp " + lastBlock.getTimestamp());
            return false;
        }
        int start = Conch.getEpochTime();
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
    
    /** 3days */
    private static final int MAX_ACTIVE_GENERATOR_LIFECYCLE = 615;
    /**
     * Return a list of generators for the next block.  The caller must hold the blockchain
     * read lock to ensure the integrity of the returned list.
     *
     * @return List of generator account identifiers
     */
    public static List<ActiveGenerator> getNextGenerators() {
        List<ActiveGenerator> generatorList;
        Blockchain blockchain = Conch.getBlockchain();
        synchronized(activeGeneratorMp) {
            if (!generatorsInitialized) {
                Set<Long> generatorIds = BlockDb.getBlockGenerators(Math.max(1, blockchain.getHeight() - MAX_ACTIVE_GENERATOR_LIFECYCLE));
                generatorIds.forEach(generatorId -> activeGeneratorMp.put(generatorId,new ActiveGenerator(generatorId)));
                Logger.logDebugMessage(activeGeneratorMp.size() + " generators found");
                
                // Active generator listener for block pushed
                Conch.getBlockchainProcessor().addListener(block -> {
                    long generatorId = block.getGeneratorId();
                    synchronized(activeGeneratorMp) {
                        if (!activeGeneratorMp.containsKey(generatorId)) {
                            activeGeneratorMp.put(generatorId,new ActiveGenerator(generatorId));
                        }
                    }
                }, BlockchainProcessor.Event.BLOCK_PUSHED);
                
                generatorsInitialized = true;
            }

            Block lastBlock = blockchain.getLastBlock();
            List<ActiveGenerator> curMiners = new ArrayList<>();
            //add active miners of local node 
            for(Generator generator : sortedMiners){
                if(activeGeneratorMp.containsKey(generator.getAccountId())) {
                    continue;
                }
                ActiveGenerator activeMiner = new ActiveGenerator(generator.accountId,generator.hitTime,generator.hit);
                curMiners.add(activeMiner);
            }

            if (lastBlock.getId() != activeBlockId) {
                activeBlockId = lastBlock.getId();
                curMiners.forEach(generator -> generator.setLastBlock(lastBlock));
                activeGeneratorMp.forEach((id, generator)-> generator.setLastBlock(lastBlock));
            }

            generatorList = Lists.newArrayList(curMiners);
            generatorList.addAll(activeGeneratorMp.values());
            Collections.sort(generatorList);
        }
        return generatorList;
    }
    

    /**
     * Active generator
     */
    public static class ActiveGenerator extends Generator {

        public ActiveGenerator(long accountId) {
            this.accountId = accountId;
            this.hitTime = Long.MAX_VALUE;
            this.hit = BigInteger.ZERO;;
        }
        
        public ActiveGenerator(long accountId, long hitTime, BigInteger hit) {
            this.accountId = accountId;
            this.hitTime = hitTime;
            this.hit = hit;
        }

        public long getEffectiveBalance() {
            return effectiveBalance.longValue();
        }

        public long getPocScore() { return pocScore.longValue(); }

        private void setLastBlock(Block lastBlock) {
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
            return (hitTime < obj.hitTime ? -1 : (hitTime > obj.hitTime ? 1 : 0));
        }
    }


    // Hub setting
    public static final String HUB_BIND_ADDRESS = Conch.getStringProperty("sharder.HubBindAddress");
    public static final Boolean HUB_IS_BIND = Conch.getBooleanProperty("sharder.HubBind");
    public static final String HUB_BIND_PR = Conch.getStringProperty("sharder.HubBindPassPhrase", "", true).trim();
    public static final String AUTO_MINT_ADDRESS = autoMintRs();
    static boolean autoMintRunning = false;
    static String autoMintRs(){
        String autoMintPR = Convert.emptyToNull(Conch.getStringProperty("sharder.autoMint.secretPhrase", "", true));
        return StringUtils.isEmpty(autoMintPR) ? null : Account.rsAccount(Account.getId(autoMintPR));
    }
    /**
     * Auto mining of Hub or Miner, just execute once
     */
    public static void autoMining(){
        if(autoMintRunning) {
            return;
        }
        
        if(Conch.getBlockchain().getHeight() < 0) {
            Logger.logWarningMessage("!!! current height < 0, need syn blocks or wait genesis block be saved into db");
            Logger.logWarningMessage("!!! you can restart the client after genesis block created");
            return;
        }

        // [Hub Miner] if owner bind the passphrase then start mine automatic
        if (HUB_IS_BIND && StringUtils.isNotEmpty(HUB_BIND_PR)) {
            Generator hubGenerator = ownerMining(HUB_BIND_PR);
            if(hubGenerator != null && (hubGenerator.getAccountId() != Account.rsAccountToId(HUB_BIND_ADDRESS))) {
                stopMining(HUB_BIND_PR);
                Logger.logInfoMessage("Account" + HUB_BIND_ADDRESS + " is not same with Generator's passphrase");
            } else {
                Logger.logInfoMessage("Account " + HUB_BIND_ADDRESS + " started mining...");
            }
        }else {
            // [Normal Miner] if owner set the passphrase of mint then start mining
            String autoMintPR = Convert.emptyToNull(Conch.getStringProperty("sharder.autoMint.secretPhrase", "", true));
            if(autoMintPR != null) {
                Generator bindGenerator = ownerMining(autoMintPR.trim());
                Logger.logInfoMessage("Account " + Account.rsAccount(bindGenerator.getAccountId()) + "started mining...");
            }
        }

        if(MAX_MINERS > 0) {
            // open miner service
            Peers.checkAndAddOpeningServices(Lists.newArrayList(Peer.Service.MINER));
        }
        autoMintRunning = true;
    }
}
