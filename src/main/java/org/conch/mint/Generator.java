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
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.*;
import org.conch.common.Constants;
import org.conch.consensus.poc.PocProcessorImpl;
import org.conch.consensus.poc.PocScore;
import org.conch.crypto.Crypto;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.tx.TransactionProcessorImpl;
import org.conch.util.Listener;
import org.conch.util.Listeners;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;

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
    private static long lastBlockId;
    private static int delayTime = Constants.FORGING_DELAY;
    
    private static final Runnable generateBlocksThread = new Runnable() {

        private volatile boolean logged;

        @Override
        public void run() {

            try {
                try {
                    BlockchainImpl.getInstance().updateLock();

                    try {
                        Block lastBlock = Conch.getBlockchain().getLastBlock();
                        //等待更新了最新的区块信息才开始锻造
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
//                                if (generator.effectiveBalance.signum() > 0) {
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
                                Logger.logDebugMessage(generator.toString());
                                logged = true;
                            }
                        }

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


    public static boolean hasGenerationMissingAccount(){
        return generationMissingMinerIds.size() > 0;
    }
    
    public synchronized static List<Long> getAndResetGenerationMissingMiners(){
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
            if(!Peers.isOpenService(Peer.Service.MINER) || generators.size() >= MAX_MINERS) {
                throw new RuntimeException("The limit miners of this node is setting to " + MAX_MINERS + ", can't allow more miners!");
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
                    if (generator.getHitTime() >= curTime - Constants.FORGING_DELAY) {
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

    public static boolean verifyHit(BigInteger hit, BigInteger pocScore, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            return false;
        }
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(pocScore);
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - Constants.BLOCK_GAP_SECONDS - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        // check the elapsed time(in second) after previous block generated
        boolean elapsed = Constants.isTestnetOrDevnet() ? elapsedTime > 300 : elapsedTime > 3600;
        return hit.compareTo(target) < 0 && (hit.compareTo(prevTarget) >= 0 || elapsed || Constants.isOffline);
        
        //FIXME[hit]
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

    public static BigInteger getHit(byte[] publicKey, Block block) {
        if (allowsFakeMining(publicKey)) {
            return BigInteger.ZERO;
        }
        if (block.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK) {
            throw new IllegalArgumentException("Not supported below Transparent Forging Block");
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

    public static long getHitTime(BigInteger pocScore, BigInteger hit, Block block) {
        return block.getTimestamp() + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(pocScore)).longValue() + Constants.BLOCK_GAP_SECONDS;
    }


    protected long accountId;
    protected byte[] publicKey;
    protected volatile long hitTime;
    protected volatile BigInteger hit;
    protected volatile BigInteger effectiveBalance;
    protected volatile BigInteger pocScore;

    private String secretPhrase;
    private volatile long deadline;

    protected Generator() {
        
    }
    
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
    
    protected void calAndSetHit(Block lastBlock) {
        int height = lastBlock.getHeight();
        Account account = Account.getAccount(accountId, height);


        effectiveBalance = PocScore.calEffectiveBalance(account,height);

        pocScore = PocProcessorImpl.instance.calPocScore(account,height);
//        if (effectiveBalance.signum() == 0) {
//            hitTime = 0;
//            hit = BigInteger.ZERO;
//            return;
//        }

        if (pocScore.signum() == 0) {
            hitTime = 0;
            hit = BigInteger.ZERO;
            return;
        }

        hit = getHit(publicKey, lastBlock);
        hitTime = getHitTime(pocScore, hit, lastBlock); 
    }
    
    /**
     * 1.设置最后一个区块
     * 2.计算可用余额
     * 3.计算hit和hitTime
     */
    private void setLastBlock(Block lastBlock) {
        calAndSetHit(lastBlock);
        deadline = Math.max(hitTime - lastBlock.getTimestamp(), 0);
        listeners.notify(this, Event.GENERATION_DEADLINE);
    }

    boolean mint(Block lastBlock, int generationLimit) throws BlockchainProcessor.BlockNotAcceptedException {
        int timestamp = getTimestamp(generationLimit);
        if (!verifyHit(hit, pocScore, lastBlock, timestamp)) {
            Logger.logDebugMessage(this.toString() + " failed to mint at " + timestamp + " height " + lastBlock.getHeight() + " last timestamp " + lastBlock.getTimestamp());
            return false;
        }
        int start = Conch.getEpochTime();
        while (true) {
            try {
                BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, timestamp);
                setDelay(Constants.FORGING_DELAY);
                return true;
            } catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                // the bad transaction has been expunged, try again
                if (Conch.getEpochTime() - start > 10) { // give up after trying for 10 s
                    throw e;
                }
            }
        }
    }

    private int getTimestamp(int generationLimit) {
        return (generationLimit - hitTime > 3600) ? generationLimit : (int)hitTime + 1;
    }

    /** Active block generators */
    private static final Set<Long> activeGeneratorIds = new HashSet<>();

    /** Active block identifier */
    private static long activeBlockId;

    /** Sorted list of generators for the next block */
    private static final List<ActiveGenerator> activeGenerators = new ArrayList<>();

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
        synchronized(activeGenerators) {
            if (!generatorsInitialized) {
                activeGeneratorIds.addAll(BlockDb.getBlockGenerators(Math.max(1, blockchain.getHeight() - MAX_ACTIVE_GENERATOR_LIFECYCLE)));
                activeGeneratorIds.forEach(activeGeneratorId -> activeGenerators.add(new ActiveGenerator(activeGeneratorId)));
                Logger.logDebugMessage(activeGeneratorIds.size() + " block generators found");
                
                // Active generator listener for block pushed
                Conch.getBlockchainProcessor().addListener(block -> {
                    long generatorId = block.getGeneratorId();
                    synchronized(activeGenerators) {
                        if (!activeGeneratorIds.contains(generatorId)) {
                            activeGeneratorIds.add(generatorId);
                            activeGenerators.add(new ActiveGenerator(generatorId));
                        }
                    }
                }, BlockchainProcessor.Event.BLOCK_PUSHED);
                
                generatorsInitialized = true;
            }

            //根据最后的区块更新活跃锻造者的锻造信息
            long blockId = blockchain.getLastBlock().getId();
            List<ActiveGenerator> curMiners = new ArrayList<>();

            //添加当前的合格锻造者到活跃锻造者池
            for(Generator generator : sortedMiners){
                if(activeGeneratorIds.contains(generator.getAccountId())) {
                    continue;
                }
                ActiveGenerator activeMiner = new ActiveGenerator(generator.accountId,generator.hitTime,generator.hit);
                curMiners.add(activeMiner);
            }

            if (blockId != activeBlockId) {
                activeBlockId = blockId;
                Block lastBlock = blockchain.getLastBlock();

                for(ActiveGenerator generator : curMiners) {
                    generator.setLastBlock(lastBlock);
                }

                for (ActiveGenerator generator : activeGenerators) {
                    generator.setLastBlock(lastBlock);
                }
            }

            generatorList = new ArrayList<>();
            generatorList.addAll(activeGenerators);
            generatorList.addAll(curMiners);
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
}
