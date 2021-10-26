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

package org.conch.chain;

import com.google.common.collect.Lists;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.consensus.burn.BurnCalculator;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.PocScore;
import org.conch.consensus.poc.db.PocDb;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.reward.RewardCalculator;
import org.conch.crypto.Crypto;
import org.conch.db.*;
import org.conch.exchange.ExchangeProcessor;
import org.conch.http.ForceConverge;
import org.conch.mint.Generator;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.security.Guard;
import org.conch.storage.StorageBackup;
import org.conch.storage.tx.StorageTx;
import org.conch.storage.tx.StorageTxProcessorImpl;
import org.conch.tx.*;
import org.conch.util.*;
import org.conch.vote.PhasingPoll;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author ben
 * @date 01/11/2018
 */
public final class BlockchainProcessorImpl implements BlockchainProcessor {

    private static final BlockchainProcessorImpl instance = new BlockchainProcessorImpl();

    public static BlockchainProcessorImpl getInstance() {
        return instance;
    }

    private final BlockchainImpl blockchain = BlockchainImpl.getInstance();

    private final int TYPE_SHARDER_POOL = 8;

    private final ExecutorService networkService = Executors.newCachedThreadPool();
    private final List<DerivedDbTable> derivedTables = new CopyOnWriteArrayList<>();
    private final boolean trimDerivedTables = Conch.getBooleanProperty("sharder.trimDerivedTables");
    private final int defaultNumberOfForkConfirmations =
            Conch.getIntProperty(
                    Constants.isTestnetOrDevnet()
                            ? "sharder.testnetNumberOfForkConfirmations"
                            : "sharder.numberOfForkConfirmations");
    private final boolean simulateEndlessDownload =
            Conch.getBooleanProperty("sharder.simulateEndlessDownload");

    private int initialScanHeight;
    private volatile int lastTrimHeight;
    private volatile int lastRestoreTime = 0;
    private final Set<Long> prunableTransactions = new HashSet<>();

    private final Listeners<Block, Event> blockListeners = new Listeners<>();
    private volatile Peer lastBlockchainFeeder;
    private volatile int lastBlockchainFeederHeight;
    private volatile boolean getMoreBlocks = true;

    private volatile boolean isTrimming;
    private volatile boolean isScanning;
    private volatile boolean isDownloading;
    private volatile boolean isProcessingBlock;
    private volatile boolean isRestoring;
    private volatile boolean alreadyInitialized = false;

    private static long lastDownloadMS = System.currentTimeMillis();
    private static final long MAX_DOWNLOAD_TIME = Constants.isDevnet() ? (1 * 1000L) : (60 * 60 * 1000L);

    private boolean peerHasMore;
    private List<Peer> connectedPublicPeers;
    private List<Long> chainBlockIds;
    private long totalTime = 1;
    private int totalBlocks;

    // auto fork switch
    public static final boolean CLOSE_SWITCH_TO_BOOT_FORK = Conch.getBooleanProperty("sharder.closeSwitchToBootFork", true);

    private static final int FORK_COUNT_RESET_REBOOT = Constants.isDevnet() ? 30 : 50;
    private static final int COUNT_RESTORE_DB = Constants.isDevnet() ? 25 : 50;
    private static final int COUNT_SWITCH_TO_BOOT_NODE = Constants.isDevnet() ? 5 : 20;

    private static final long FORCE_SWITCH_INTERVAL = 30 * 60 * 1000L;
    private static long lastForceSwitchMS = -1L;

    private int forkSwitchFailedCount = 0;
    private int switchToBootNodeFailedCount = 0;

    private boolean forceSwitchToBootNodesFork = false;
    private volatile boolean isRestoringDb = false;
    private volatile int lastBootNodeHeight = -1;

    private final Runnable getMoreBlocksThread = new Runnable() {
        @Override
        public void run() {
            try {
                //
                // Download blocks until we are up-to-date
                //
                while (true) {

                    if (!getMoreBlocks) {
                        if (Logger.printNow(Logger.BlockchainProcessor_getMoreBlocks)) {
                            Logger.logDebugMessage("Don't synchronize blocks when the getMoreBlocks is set to false");
                        }

                        // re-connect all peers when download can't finish long time
                        if (isExceedUnfinishedDownload(MAX_DOWNLOAD_TIME)) {
                            Logger.logInfoMessage("Can't finish the block synchronization in the %d Minutes, re-connect all peers", MAX_DOWNLOAD_TIME/1000/60);
                            Peers.checkOrReConnectAllPeers();
                        }
                    }

                    if (Conch.hasSerialNum() && !Constants.hubLinked) {
                        if (Logger.printNow(Logger.BlockchainProcessor_getMoreBlocks)) {
                            Logger.logDebugMessage("Don't synchronize blocks till the client initialization is completed");
                        }
                        return;
                    }

                    if (!Conch.getPocProcessor().pocTxsProcessed(Conch.getHeight())) {
                        if (Logger.printNow(Logger.BlockchainProcessor_oldPocTxsProcessingCheck, 1000)) {
                            Logger.logDebugMessage("Don't synchronize blocks till delayed or old poc txs[ height <=  %d ] be processed", Conch.getHeight());
                        }
                        return;
                    }

                    int chainHeight = blockchain.getHeight();
                    long downloadStartMS = System.currentTimeMillis();
                    downloadPeer();
                    if (blockchain.getHeight() == chainHeight) {
                        if (isDownloading && !simulateEndlessDownload) {
                            isDownloading = false;
                            lastDownloadMS = System.currentTimeMillis();
                            Peers.checkAndUpdateBlockchainState(null);
                            Block lastBlock = blockchain.getLastBlock();
                            long downloadUsedMS= System.currentTimeMillis() - downloadStartMS;
                            Logger.logInfoMessage("Finished blockchain downloaded %d blocks from %s[%s] used %d Min(%d S), sync last block[miner=%s, id=%d], " +
                                    "current chain[height=%d, state=%s]" ,
                                    totalBlocks, lastBlockchainFeeder.getAnnouncedAddress(), lastBlockchainFeeder.getHost(),
                                    downloadUsedMS/1000/60, downloadUsedMS/1000,
                                    Account.rsAccount(lastBlock.getGeneratorId()), lastBlock.getId(),
                                    blockchain.getHeight(), Peers.getMyBlockchainStateName());
                            bootNodeForkSwitchCheck(lastBlockchainFeeder);
                        }
                        break;
                    }
                }
                //
                // Restore prunable data
                //
                int now = Conch.getEpochTime();
                if (!isRestoring
                    && !prunableTransactions.isEmpty()
                    && now - lastRestoreTime > 60 * 60) {
                    isRestoring = true;
                    lastRestoreTime = now;
                    networkService.submit(new RestorePrunableDataTask());
                }
            } catch (InterruptedException e) {
                Logger.logDebugMessage("Blockchain download thread interrupted");
            } catch (Throwable t) {
                Logger.logErrorMessage(
                        "CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
                System.exit(1);
            }
        }

    };

    private static long lastForceConnectMS = System.currentTimeMillis();
    /**
     * Synchronize blocks from the feeder peer
     *
     * @throws InterruptedException
     */
    private void downloadPeer() throws InterruptedException {
        try {
            long startTime = System.currentTimeMillis();
            int limitConnectedSize = Math.min(1, defaultNumberOfForkConfirmations);

            List<Peer> bootNodes = Lists.newArrayList();
            if (Guard.needConnectBoot(lastForceConnectMS)) {
                boolean needConnectNow = (System.currentTimeMillis() - lastForceConnectMS) > (MAX_DOWNLOAD_TIME / 2);
                bootNodes = Peers.checkOrConnectAllGuideNodes(needConnectNow);
                if (bootNodes.size() > 0) {
                    lastForceConnectMS = System.currentTimeMillis();
                }
            }
            connectedPublicPeers = Peers.getPublicPeers(Peer.State.CONNECTED, true);
            int connectedSize = connectedPublicPeers.size();
            if (!Generator.isBootNode
                    && connectedSize < limitConnectedSize) {
                if (Logger.printNow(Logger.BlockchainProcessor_downloadPeer_sizeCheck)) {
                    Logger.logInfoMessage("No enough connected peers[limit size=" + (limitConnectedSize) + ",current " +
                            "connected size=" + connectedSize + "], break syn blocks...");
                }

                if (isExceedUnfinishedDownload(MAX_DOWNLOAD_TIME)) {
                    Peers.checkOrReConnectAllPeers();
                }
                return;
            }

            if(ForceConverge.forcePause){
                List<Peer> needRemovePeers = Lists.newArrayList();
                for(Peer peer : connectedPublicPeers) {
                    if(!Constants.isValidBootNode(peer)){
                        needRemovePeers.add(peer);
                    }
                }
                connectedPublicPeers.removeAll(needRemovePeers);
            }

            peerHasMore = true;
            final Peer peer = Peers.getWeightedPeer(connectedPublicPeers);
            if (peer == null
                && Logger.printNow(Logger.BlockchainProcessor_downloadPeer_getWeightedPeer)) {
                String bootNodesDetail = "";
                for(Peer bootPeer : bootNodes){
                    bootNodesDetail += String.format("[DEBUG] %s[%s] state is %s, blockchain state is %s\n",
                            bootPeer.getAnnouncedAddress(), bootPeer.getHost(),
                            bootPeer.getState(), bootPeer.getBlockchainState());
                }

                Logger.logDebugMessage("\n[DEBUG] Can't find a weighted peer to sync the blocks:\n" +
                        "[DEBUG] a) current peer's version '%s' is larger than other peers.\n" +
                        "[DEBUG] b) can't connect to boot nodes or other peers which have the public IP.\n" +
                        "[DEBUG] >>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" + "%s",
                        Conch.getVersionWithBuild(), bootNodesDetail);
                return;
            }

            JSONObject response = getPeersDifficulty(peer);
            // can't get the mining difficulty of remote peer
            BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();
            Object remoteDifficultyObj = response != null ? response.get("cumulativeDifficulty") : null;
            String peerDifficultyStr = (remoteDifficultyObj != null) ? (String) remoteDifficultyObj : null;
            if (peerDifficultyStr == null) {
                return;
            }

            // the mining difficulty of the feeder peer is smaller than the current peer
            BigInteger peerCumulativeDifficulty = new BigInteger(peerDifficultyStr);
            if (peerCumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {
                return;
            }

            // the mining difficulty of the feeder peer is same with the current peer
            if (response.get("blockchainHeight") != null) {
                lastBlockchainFeeder = peer;
                lastBlockchainFeederHeight = ((Long) response.get("blockchainHeight")).intValue();
            }
            if (peerCumulativeDifficulty.equals(curCumulativeDifficulty)) {
                return;
            }

            // the cos version of the feeder peer is smaller than current peer
            if (Conch.versionCompare(peer.getVersion()) > 0){
                Logger.logDebugMessage("Current peer's version %s is larger than remote peer %s[%s]'s version, ABORT the block sync... ", Conch.getFullVersion(), peer.getAnnouncedAddress(), peer.getHost());
                return;
            }

            // milestone block and block number check
            long commonMilestoneBlockId = SharderGenesis.GENESIS_BLOCK_ID;

            if (blockchain.getLastBlock().getId() != SharderGenesis.GENESIS_BLOCK_ID) {
                commonMilestoneBlockId = getCommonMilestoneBlockId(peer);
            }
            if (commonMilestoneBlockId == 0 || !peerHasMore) {
                return;
            }

            chainBlockIds = getBlockIdsAfterCommon(peer, commonMilestoneBlockId, false);
            if (chainBlockIds.size() < 2 || !peerHasMore) {
                return;
            }

            final long commonBlockId = chainBlockIds.get(0);
            final Block commonBlock = blockchain.getBlock(commonBlockId);
            if (commonBlock == null || blockchain.getHeight() - commonBlock.getHeight() >= 720) {
                if (commonBlock != null) {
                    Logger.logDebugMessage(peer + " stay at fork with better difficulty, but the last common block is at height " + commonBlock.getHeight() + ", ABORT the block sync...");
                }
                return;
            }
            if (simulateEndlessDownload) {
                isDownloading = true;
                return;
            }

            int heightDiffCount = lastBlockchainFeederHeight - commonBlock.getHeight();
            // fetch the db archive and restart
            if (isExceedUnfinishedDownload(MAX_DOWNLOAD_TIME)) {
                Logger.logWarningMessage("Can't finish the block synchronization in the %d hours"
                        + ", try to RESET and RESTART this client manually to fix problem!!"
                        , (MAX_DOWNLOAD_TIME/1000/60/60), heightDiffCount);
//                ClientUpgradeTool.restoreDbToLastArchive(true, true);
            }

            if (!isDownloading && heightDiffCount > 6) {
                Logger.logMessage("Blockchain download in progress[height is from " + blockchain.getHeight() + " to " + lastBlockchainFeederHeight + "]");
                isDownloading = true;
            }

            blockchain.updateLock();
            try {
                if (peerCumulativeDifficulty.compareTo(blockchain.getLastBlock().getCumulativeDifficulty()) <= 0) {
                    return;
                }
                long lastBlockId = blockchain.getLastBlock().getId();
                int syncHeightCount = lastBlockchainFeederHeight - blockchain.getHeight();
                long estimatedSyncSeconds = syncHeightCount * 1;
                Logger.logInfoMessage("Synchronize the blocks from feeder %s[%s], current height %d -> feeder's height %d, sync %d blocks estimated processing time is %d Min(%d S) ...",
                        lastBlockchainFeeder.getAnnouncedAddress(), lastBlockchainFeeder.getHost(),
                        blockchain.getHeight(), lastBlockchainFeederHeight, syncHeightCount,
                        estimatedSyncSeconds / 60 , estimatedSyncSeconds);
                downloadBlockchain(peer, commonBlock, commonBlock.getHeight());
                if (blockchain.getHeight() - commonBlock.getHeight() <= 10) {
                    checkAndSwitchToBootNodesFork();
                    return;
                }

                int confirmations = 0;
                for (Peer otherPeer : connectedPublicPeers) {
                    if (confirmations >= limitConnectedSize) {
                        break;
                    }
                    if (peer.getHost().equals(otherPeer.getHost())) {
                        continue;
                    }
                    chainBlockIds = getBlockIdsAfterCommon(otherPeer, commonBlockId, true);
                    if (chainBlockIds.isEmpty()) {
                        continue;
                    }
                    long otherPeerCommonBlockId = chainBlockIds.get(0);
                    if (otherPeerCommonBlockId == blockchain.getLastBlock().getId()) {
                        confirmations++;
                        continue;
                    }
                    Block otherPeerCommonBlock = blockchain.getBlock(otherPeerCommonBlockId);
                    if (blockchain.getHeight() - otherPeerCommonBlock.getHeight() >= 720) {
                        continue;
                    }
                    String otherPeerCumulativeDifficulty;
                    JSONObject otherPeerResponse = getPeersDifficulty(peer);
                    if (otherPeerResponse == null
                            || (otherPeerCumulativeDifficulty =
                            (String) response.get("cumulativeDifficulty"))
                            == null) {
                        continue;
                    }
                    if (new BigInteger(otherPeerCumulativeDifficulty)
                            .compareTo(blockchain.getLastBlock().getCumulativeDifficulty())
                            <= 0) {
                        continue;
                    }
                    Logger.logInfoMessage("Found a peer %s[%s] with better difficulty[%s], synchronize the blocks from height %d",
                            otherPeer.getAnnouncedAddress(), otherPeer.getHost(), otherPeerCumulativeDifficulty, commonBlock.getHeight());
                    lastBlockchainFeeder = otherPeer;
                    downloadBlockchain(otherPeer, otherPeerCommonBlock, commonBlock.getHeight());
                }
                Logger.logDebugMessage("Got " + confirmations + " confirmations");

                if (blockchain.getLastBlock().getId() != lastBlockId) {
                    long time = System.currentTimeMillis() - startTime;
                    totalTime += time;
                    int numBlocks = blockchain.getHeight() - commonBlock.getHeight();
                    totalBlocks += numBlocks;
                    Logger.logInfoMessage(
                            "Downloaded "
                                    + numBlocks
                                    + " blocks from "
                                    + lastBlockchainFeeder.getAnnouncedAddress() + "[" + lastBlockchainFeeder.getHost() + "]"
                                    + " at height "
                                    + blockchain.getHeight()
                                    + " in "
                                    + time / 1000
                                    + " s, "
                                    + (totalBlocks * 1000) / totalTime
                                    + " per s, "
                                    + totalTime
                                    * (lastBlockchainFeederHeight - blockchain.getHeight())
                                    / ((long) totalBlocks * 1000 * 60)
                                    + " min left"
                    );
                } else {
//                    checkAndSwitchToBootNodesFork();
                    Logger.logDebugMessage("Did not accept peer's blocks, back to our own fork"
                            + ", fork switch failed count " + forkSwitchFailedCount);
                }

            } finally {
                blockchain.updateUnlock();
            }

        } catch (ConchException.StopException e) {
            Logger.logMessage("Blockchain download stopped: " + e.getMessage());
            throw new InterruptedException("Blockchain download stopped");
        } catch (Exception e) {
            Logger.logMessage("Error in blockchain download thread", e);
        }
    }


    /**
     * common milestone block id: org.conch.peer.GetMilestoneBlockIds
     *
     * @param peer
     * @return
     */
    private long getCommonMilestoneBlockId(Peer peer) {

        String lastMilestoneBlockId = null;

        while (true) {
            JSONObject milestoneBlockIdsRequest = new JSONObject();
            milestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
            // [NAT] inject useNATService property to the request params
            milestoneBlockIdsRequest.putAll(Peers.getNatAndAddressMap());
            if (lastMilestoneBlockId == null) {
                milestoneBlockIdsRequest.put("lastBlockId", blockchain.getLastBlock().getStringId());
            } else {
                milestoneBlockIdsRequest.put("lastMilestoneBlockId", lastMilestoneBlockId);
            }

            JSONObject response = peer.send(JSON.prepareRequest(milestoneBlockIdsRequest));
            if (response == null) {
                return 0;
            }

            JSONArray milestoneBlockIds = (JSONArray) response.get("milestoneBlockIds");
            if (milestoneBlockIds == null) {
                return 0;
            }
            if (milestoneBlockIds.isEmpty()) {
                return SharderGenesis.GENESIS_BLOCK_ID;
            }

            // prevent overloading with blockIds
            if (milestoneBlockIds.size() > 20) {
                Logger.logDebugMessage("Obsolete or rogue peer " + peer.getHost() + " sends too many milestoneBlockIds, blacklisting");
                peer.blacklist("Too many milestoneBlockIds");
                return 0;
            }

            if (Boolean.TRUE.equals(response.get("last"))) {
                peerHasMore = false;
            }

            for (Object milestoneBlockId : milestoneBlockIds) {
                long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
                if (BlockDb.hasBlock(blockId)) {
                    if (lastMilestoneBlockId == null && milestoneBlockIds.size() > 1) {
                        peerHasMore = false;
                    }
                    return blockId;
                }
                lastMilestoneBlockId = (String) milestoneBlockId;
            }
        }
    }

    private List<Long> getBlockIdsAfterCommon(final Peer peer, final long startBlockId, final boolean countFromStart) {
        long matchId = startBlockId;
        List<Long> blockList = new ArrayList<>(720);
        boolean matched = false;
        int limit = countFromStart ? 720 : 1440;
        while (true) {
            JSONObject request = new JSONObject();
            // [NAT] inject useNATService property to the request params
            request.put("requestType", "getNextBlockIds");
            request.put("blockId", Long.toUnsignedString(matchId));
            request.put("limit", limit);
            request.putAll(Peers.getNatAndAddressMap());
            JSONObject response = peer.send(JSON.prepareRequest(request));
            if (response == null) {
                return Collections.emptyList();
            }
            JSONArray nextBlockIds = (JSONArray) response.get("nextBlockIds");
            if (nextBlockIds == null || nextBlockIds.size() == 0) {
                break;
            }
            // prevent overloading with blockIds
            if (nextBlockIds.size() > limit) {
                Logger.logDebugMessage("Obsolete or rogue peer "
                        + peer.getHost()
                        + " sends too many nextBlockIds, blacklisting");
                peer.blacklist("Too many nextBlockIds");
                return Collections.emptyList();
            }
            boolean matching = true;
            int count = 0;
            for (Object nextBlockId : nextBlockIds) {
                long blockId = Convert.parseUnsignedLong((String) nextBlockId);
                if (matching) {
                    if (BlockDb.hasBlock(blockId)) {
                        matchId = blockId;
                        matched = true;
                    } else {
                        blockList.add(matchId);
                        blockList.add(blockId);
                        matching = false;
                    }
                } else {
                    blockList.add(blockId);
                    if (blockList.size() >= 720) {
                        break;
                    }
                }
                if (countFromStart && ++count >= 720) {
                    break;
                }
            }
            if (!matching || countFromStart) {
                break;
            }
        }
        if (blockList.isEmpty() && matched) {
            blockList.add(matchId);
        }
        return blockList;
    }

    /**
     * Download the block chain
     *
     * @param feederPeer  Peer supplying the blocks list
     * @param commonBlock Common block
     * @throws InterruptedException Download interrupted
     */
    private void downloadBlockchain(final Peer feederPeer, final Block commonBlock, final int startHeight) throws InterruptedException {
        Map<Long, PeerBlock> blockMap = new HashMap<>();
        //
        // Break the download into multiple segments.  The first block in each segment
        // is the common block for that segment.
        //
        List<GetNextBlocks> getList = new ArrayList<>();
        int segSize = 36;
        int stop = chainBlockIds.size() - 1;
        for (int start = 0; start < stop; start += segSize) {
            getList.add(new GetNextBlocks(chainBlockIds, start, Math.min(start + segSize, stop)));
        }
        int nextPeerIndex = ThreadLocalRandom.current().nextInt(connectedPublicPeers.size());
        long maxResponseTime = 0;
        Peer slowestPeer = null;
        //
        // Issue the getNextBlocks requests and get the results.  We will repeat
        // a request if the peer didn't respond or returned a partial block list.
        // The download will be aborted if we are unable to get a segment after
        // retrying with different peers.
        //
        download:
        while (!getList.isEmpty()) {
            //
            // Submit threads to issue 'getNextBlocks' requests.  The first segment
            // will always be sent to the feeder peer.  Subsequent segments will
            // be sent to the feeder peer if we failed trying to download the blocks
            // from another peer.  We will stop the download and process any pending
            // blocks if we are unable to download a segment from the feeder peer.
            //
            for (GetNextBlocks nextBlocks : getList) {
                Peer peer;
                if (nextBlocks.getRequestCount() > 1) {
                    break download;
                }
                if (nextBlocks.getStart() == 0
                    || nextBlocks.getRequestCount() != 0) {
                    peer = feederPeer;
                } else {
                    if (forceSwitchToBootNodesFork) {
                        peer = Peers.checkOrConnectBootNodeRandom(false);
                    } else {
                        if (nextPeerIndex >= connectedPublicPeers.size()) {
                            nextPeerIndex = 0;
                        }
                        peer = connectedPublicPeers.get(nextPeerIndex++);
                        if (peer.isBlacklisted()) {
                            break download;
                        }
                    }
                }
                if (nextBlocks.getPeer() == peer) {
                    break download;
                }

                nextBlocks.setPeer(peer);
                Future<List<BlockImpl>> future = networkService.submit(nextBlocks);
                nextBlocks.setFuture(future);
            }
            //
            // Get the results.  A peer is on a different fork if a returned
            // block is not in the block identifier list.
            //
            Iterator<GetNextBlocks> it = getList.iterator();
            while (it.hasNext()) {
                GetNextBlocks nextBlocks = it.next();
                List<BlockImpl> blockList;
                try {
                    // todo A {@code Future} represents the result of an asynchronous computation, possible execute to here blockList is null, should wait Future is done
                    blockList = nextBlocks.getFuture().get();
                } catch (ExecutionException exc) {
                    throw new RuntimeException(exc.getMessage(), exc);
                }
                if (blockList == null) {
                    nextBlocks.getPeer().deactivate();
                    continue;
                }
                Peer peer = nextBlocks.getPeer();
                int index = nextBlocks.getStart() + 1;
                for (BlockImpl block : blockList) {
                    if (block.getId() != chainBlockIds.get(index)) {
                        break;
                    }
                    blockMap.put(block.getId(), new PeerBlock(peer, block));
                    index++;
                }
                if (index > nextBlocks.getStop()) {
                    it.remove();
                } else {
                    nextBlocks.setStart(index - 1);
                }
                if (nextBlocks.getResponseTime() > maxResponseTime) {
                    maxResponseTime = nextBlocks.getResponseTime();
                    slowestPeer = nextBlocks.getPeer();
                }
            }
        }
        if (slowestPeer != null
                && connectedPublicPeers.size() >= Peers.maxNumberOfConnectedPublicPeers
                && chainBlockIds.size() > 360) {
            Logger.logDebugMessage(slowestPeer.getHost() + " took " + maxResponseTime + " ms, disconnecting");
            slowestPeer.deactivate();
        }
        //
        // Add the new blocks to the blockchain.  We will stop if we encounter
        // a missing block (this will happen if an invalid block is encountered
        // when downloading the blocks)
        //
        blockchain.writeLock();
        try {
            List<BlockImpl> forkBlocks = new ArrayList<>();
            Logger.logInfoMessage("Will push %d blocks into current chain...", chainBlockIds.size());
            for (int index = 1; index < chainBlockIds.size() && blockchain.getHeight() - startHeight < 720; index++) {
                if(!getMoreBlocks) break;

                PeerBlock peerBlock = blockMap.get(chainBlockIds.get(index));
                if (peerBlock == null) {
                    break;
                }
                BlockImpl block = peerBlock.getBlock();
                if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
                    try {
                        Logger.logInfoMessage("Pushing block%s into current chain at height %d ...", block.toSummary(), Conch.getHeight());
                        pushBlock(block);
                    } catch (GeneratorNotAcceptedException e) {
                        Generator.blackGenerator(e.getGeneratorId());
                    } catch (BlockNotAcceptedException e) {
                        peerBlock.getPeer().blacklist(e);
                    } catch (ConchException.StopException e) {
                        throw e;
                    }
                } else {
                    forkBlocks.add(block);
                }
            }
            //
            // Process a fork
            //
            int myForkSize = blockchain.getHeight() - startHeight;
            if (!forkBlocks.isEmpty() && myForkSize < 720) {
                Logger.logDebugMessage("Will process a fork of %d blocks from start height %d, " +
                                "current block chain have %d fork blocks",
                        forkBlocks.size(), startHeight, myForkSize);
                processFork(feederPeer, forkBlocks, commonBlock);
            }

        } finally {
            blockchain.writeUnlock();
        }
    }

    private void processFork(final Peer peer, final List<BlockImpl> forkBlocks, final Block commonBlock) throws ConchException.StopException {
        // record the current difficulty and pop-off the chain to common block(genesis block or last known block)
        BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();
        List<BlockImpl> myPoppedOffBlocks = popOffTo(commonBlock);

        String peerAddress = peer != null ? peer.getAnnouncedAddress() : "AddressUndefined";
        String peerHost = peer != null ? peer.getHost() : "HostUndefined";

        // push the fork blocks into chain
        int pushedForkBlocks = 0;
        if (blockchain.getLastBlock().getId() == commonBlock.getId()) {
            for (BlockImpl block : forkBlocks) {
                if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
                    try {
                        pushBlock(block);
                        pushedForkBlocks += 1;
                    } catch (BlockNotAcceptedException e) {
                        peer.blacklist(e);
                        break;
                    } catch (GeneratorNotAcceptedException e) {
                        Generator.blackGenerator(e.getGeneratorId());
                        break;
                    } catch (ConchException.StopException e) {
                        throw e;
                    }
                }
            }
        }

        boolean isSmallerDifficultyOnOtherFork = blockchain
                .getLastBlock()
                .getCumulativeDifficulty()
                .compareTo(curCumulativeDifficulty)
                < 0;

        if (forceSwitchToBootNodesFork
            && Constants.isValidBootNode(peer)) {
            // in the boot node switching processing, don't check the CumulativeDifficulty
        } else {
            // check original difficulty(before pushed fork blocks) of chain with difficulty of pushed chain 
            if (pushedForkBlocks > 0
            && isSmallerDifficultyOnOtherFork) {
                Logger.logDebugMessage("Pop off caused by peer %s[%s] blacklisting", peerAddress, peerHost);
                peer.blacklist("Pop off");
                List<BlockImpl> peerPoppedOffBlocks = popOffTo(commonBlock);
                pushedForkBlocks = 0;
                for (BlockImpl block : peerPoppedOffBlocks) {
                    TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                }
            }
        }

        // put the pop-off blocks back to chain 
        if (pushedForkBlocks == 0) {
            Logger.logDebugMessage("Didn't accept any blocks, pushing back my previous blocks");
            for (int i = myPoppedOffBlocks.size() - 1; i >= 0; i--) {
                BlockImpl block = myPoppedOffBlocks.remove(i);
                try {
                    pushBlock(block);
                } catch (BlockNotAcceptedException e) {
                    Logger.logErrorMessage("Popped off block no longer acceptable: " + block.getJSONObject().toJSONString(), e);
                    break;
                } catch (GeneratorNotAcceptedException e) {
                    Generator.blackGenerator(e.getGeneratorId());
                    break;
                }
            }
            checkAndSwitchToBootNodesFork();
        } else {
            Logger.logDebugMessage("Switched to peer %s[%s]'s fork", peerAddress, peerHost);
            for (BlockImpl block : myPoppedOffBlocks) {
                TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
            }
        }
    }

    private JSONObject getPeersDifficulty(Peer peer) {
        if (peer == null) {
            return null;
        }

        JSONObject request = new JSONObject();
        request.put("requestType", "getCumulativeDifficulty");
        // [NAT] inject useNATService property to the request params
        request.putAll(Peers.getNatAndAddressMap());
        return peer.send(JSON.prepareRequest(request));
    }

    private boolean isExceedUnfinishedDownload(long maxTimeSeconds){
        return (System.currentTimeMillis() - lastDownloadMS) > maxTimeSeconds;
    }

    public boolean checkAndSwitchToBootNodesFork() {
        try {
            if (Generator.isBootNode
            || CLOSE_SWITCH_TO_BOOT_FORK
            || forkSwitchFailedCount++ < COUNT_SWITCH_TO_BOOT_NODE) {
                return false;
            }

            forceSwitchToBootNodesFork = true;
            // connect to the boot nodes
            boolean needConnectNow = isExceedUnfinishedDownload(MAX_DOWNLOAD_TIME / 2);
            Peer peer = Peers.checkOrConnectBootNodeRandom(needConnectNow);
            if (peer == null) {
                Logger.logWarningMessage("Can't connect to boot nodes, break and wait for next round. ForkSwitchingFailed=%d, SwitchToBootNodeFailedCount=%d",
                        forkSwitchFailedCount, switchToBootNodeFailedCount);
                return false;
            }
            Logger.logInfoMessage("Start to switch to BootNode %s[%s]'s fork. ForkSwitchingFailed=%d, SwitchToBootNodeFailedCount=%d",
                    peer.getAnnouncedAddress(), peer.getHost(), forkSwitchFailedCount, switchToBootNodeFailedCount);

            JSONObject response = getPeersDifficulty(peer);
            if (response == null) return false;
            Integer bootNodeHeight = response.get("blockchainHeight") != null ? ((Long) response.get("blockchainHeight")).intValue() : 0;
            int startHeight = Conch.getHeight();

            if (switchToBootNodeFailedCount++ < COUNT_RESTORE_DB) {
                // rollback the local blockchain then fetch the blocks once
                if (bootNodeHeight < Conch.getHeight()
                    && bootNodeHeight > Constants.LAST_KNOWN_BLOCK) {
                    startHeight = bootNodeHeight;
                } else if (bootNodeHeight > Conch.getHeight()) {
                    startHeight = Conch.getHeight() - 12;
                }

                // rollback 
                Block startBlock = blockchain.getBlockAtHeight(startHeight);
                try {
                    Conch.pause();
                    if (startHeight < Conch.getHeight()) {
                        popOffTo(startBlock);
                    }
                } finally {
                    Conch.unpause();
                }

            } else {
                // restore to the check point(known db archive)
                /**
                if (isRestoringDb) {
                    return false;
                }
                isRestoringDb = true;
                ClientUpgradeTool.restoreDbToLastArchive(true, true);
                return false;
                **/
            }

        } catch (Exception e) {
            Logger.logErrorMessage("Switch to boot nodes fork failed", e);
        }
        return false;
    }

    private void bootNodeForkSwitchCheck(Peer lastFeeder) {
        if (isUpToDate() && forceSwitchToBootNodesFork) {
            Logger.logInfoMessage("Switched to BootNode %s[%s]'s fork at height %d",
                lastFeeder.getAnnouncedAddress(), lastFeeder.getHost(), Conch.getHeight());
            switchToBootNodeFailedCount = 0;
            forkSwitchFailedCount = 0;
            forceSwitchToBootNodesFork = false;
        }
    }

    private void bootNodeHeightCompare() {
        Peer bootNode = Peers.getPeer(Constants.bootNodeHost, true);

        // boot node processing
        if (Constants.bootNodeHost.equalsIgnoreCase(Conch.getMyAddress())) {
            List<String> remainBootNodes = Lists.newArrayList(Constants.bootNodesHost);
            remainBootNodes.remove(Conch.getMyAddress());
            if (remainBootNodes.size() > 0) {
                bootNode = Peers.getPeer(remainBootNodes.get(0), true);
            } else {
                return;
            }
        }

        JSONObject response = getPeersDifficulty(bootNode);
        if (response == null || response.get("blockchainHeight") == null) {
            return;
        } else {
            lastBootNodeHeight = ((Long) response.get("blockchainHeight")).intValue();
        }

        // can't get the mining difficulty of remote peer
        BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();
        Object remoteDifficultyObj = response != null ? response.get("cumulativeDifficulty") : null;
        String peerCumulativeDifficulty = remoteDifficultyObj != null ? (String) remoteDifficultyObj : null;
        if (peerCumulativeDifficulty == null) {
            return;
        }

        // the mining difficulty of the feeder peer is smaller than the current peer
        BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
        if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {
            return;
        }

        if (lastBootNodeHeight == blockchain.getHeight()) {
            Logger.logDebugMessage("Reach the BootNode %s[%s]'s last height %d, update the blockchain state to UpToDate"
                    , bootNode.getAnnouncedAddress(), bootNode.getHost(), lastBootNodeHeight);
            Peers.checkAndUpdateBlockchainState(true);
        }
    }

    /**
     * Callable method to get the next block segment from the selected peer
     */
    private static class GetNextBlocks implements Callable<List<BlockImpl>> {

        /**
         * Callable future
         */
        private Future<List<BlockImpl>> future;

        /**
         * Peer
         */
        private Peer peer;

        /**
         * Block identifier list
         */
        private final List<Long> blockIds;

        /**
         * Start index
         */
        private int start;

        /**
         * Stop index
         */
        private int stop;

        /**
         * Request count
         */
        private int requestCount;

        /**
         * Time it took to return getNextBlocks
         */
        private long responseTime;

        /**
         * Create the callable future
         *
         * @param blockIds Block identifier list
         * @param start    Start index within the list
         * @param stop     Stop index within the list
         */
        public GetNextBlocks(List<Long> blockIds, int start, int stop) {
            this.blockIds = blockIds;
            this.start = start;
            this.stop = stop;
            this.requestCount = 0;
        }

        /**
         * Return the result
         *
         * @return List of blocks or null if an error occurred
         */
        @Override
        public List<BlockImpl> call() {
            requestCount++;
            //
            // Build the block request list
            //
            JSONArray idList = new JSONArray();
            for (int i = start + 1; i <= stop; i++) {
                idList.add(Long.toUnsignedString(blockIds.get(i)));
            }
            // [NAT] inject useNATService property to the request params
            JSONObject request = new JSONObject();
            request.put("requestType", "getNextBlocks");
            request.put("blockIds", idList);
            request.put("blockId", Long.toUnsignedString(blockIds.get(start)));
            request.putAll(Peers.getNatAndAddressMap());
            long startTime = System.currentTimeMillis();
            JSONObject response = peer.send(JSON.prepareRequest(request), Peers.MAX_RESPONSE_SIZE);
            responseTime = System.currentTimeMillis() - startTime;
            if (response == null) {
                return null;
            }
            //
            // Get the list of blocks.  We will stop parsing blocks if we encounter
            // an invalid block.  We will return the valid blocks and reset the stop
            // index so no more blocks will be processed.
            //
            List<JSONObject> nextBlocks = (List<JSONObject>) response.get("nextBlocks");
            if (nextBlocks == null) {
                return null;
            }
            if (nextBlocks.size() > 36) {
                Logger.logDebugMessage(
                        "Obsolete or rogue peer "
                                + peer.getHost()
                                + " sends too many nextBlocks, blacklisting");
                peer.blacklist("Too many nextBlocks");
                return null;
            }
            List<BlockImpl> blockList = new ArrayList<>(nextBlocks.size());
            try {
                int count = stop - start;
                for (JSONObject blockData : nextBlocks) {
                    blockList.add(BlockImpl.parseBlock(blockData));
                    if (--count <= 0) {
                        break;
                    }
                }
            } catch (RuntimeException | ConchException.NotValidException e) {
                Logger.logDebugMessage("Failed to parse block: " + e.toString(), e);
                peer.blacklist(e);
                stop = start + blockList.size();
            }
            return blockList;
        }

        /**
         * Return the callable future
         *
         * @return Callable future
         */
        public Future<List<BlockImpl>> getFuture() {
            return future;
        }

        /**
         * Set the callable future
         *
         * @param future Callable future
         */
        public void setFuture(Future<List<BlockImpl>> future) {
            this.future = future;
        }

        /**
         * Return the peer
         *
         * @return Peer
         */
        public Peer getPeer() {
            return peer;
        }

        /**
         * Set the peer
         *
         * @param peer Peer
         */
        public void setPeer(Peer peer) {
            this.peer = peer;
        }

        /**
         * Return the start index
         *
         * @return Start index
         */
        public int getStart() {
            return start;
        }

        /**
         * Set the start index
         *
         * @param start Start index
         */
        public void setStart(int start) {
            this.start = start;
        }

        /**
         * Return the stop index
         *
         * @return Stop index
         */
        public int getStop() {
            return stop;
        }

        /**
         * Return the request count
         *
         * @return Request count
         */
        public int getRequestCount() {
            return requestCount;
        }

        /**
         * Return the response time
         *
         * @return Response time
         */
        public long getResponseTime() {
            return responseTime;
        }
    }

    /**
     * Block returned by a peer
     */
    private static class PeerBlock {

        /**
         * Peer
         */
        private final Peer peer;

        /**
         * Block
         */
        private final BlockImpl block;

        /**
         * Create the peer block
         *
         * @param peer  Peer
         * @param block Block
         */
        public PeerBlock(Peer peer, BlockImpl block) {
            this.peer = peer;
            this.block = block;
        }

        /**
         * Return the peer
         *
         * @return Peer
         */
        public Peer getPeer() {
            return peer;
        }

        /**
         * Return the block
         *
         * @return Block
         */
        public BlockImpl getBlock() {
            return block;
        }
    }

    /**
     * Task to restore prunable data for downloaded blocks
     */
    private class RestorePrunableDataTask implements Runnable {

        @Override
        public void run() {
            Peer peer = null;
            try {
                //
                // Locate an archive peer
                //
                List<Peer> peers =
                        Peers.getPeers(
                                chkPeer ->
                                        chkPeer.providesService(Peer.Service.PRUNABLE)
                                                && !chkPeer.isBlacklisted()
                                                && chkPeer.getAnnouncedAddress() != null);
                while (!peers.isEmpty()) {
                    Peer chkPeer = peers.get(ThreadLocalRandom.current().nextInt(peers.size()));
                    if (chkPeer.getState() != Peer.State.CONNECTED) {
                        Peers.connectPeer(chkPeer);
                    }
                    if (chkPeer.getState() == Peer.State.CONNECTED) {
                        peer = chkPeer;
                        break;
                    }
                }
                if (peer == null) {
                    Logger.logDebugMessage("Cannot find any archive peers");
                    return;
                }
                Logger.logDebugMessage("Connected to archive peer " + peer.getHost());
                //
                // Make a copy of the prunable transaction list so we can remove entries
                // as we process them while still retaining the entry if we need to
                // retry later using a different archive peer
                //
                Set<Long> processing;
                synchronized (prunableTransactions) {
                    processing = new HashSet<>(prunableTransactions.size());
                    processing.addAll(prunableTransactions);
                }
                Logger.logDebugMessage("Need to restore " + processing.size() + " pruned data");
                //
                // Request transactions in batches of 100 until all transactions have been processed
                //
                while (!processing.isEmpty()) {
                    //
                    // Get the pruned transactions from the archive peer
                    //
                    JSONObject request = new JSONObject();
                    JSONArray requestList = new JSONArray();
                    synchronized (prunableTransactions) {
                        Iterator<Long> it = processing.iterator();
                        while (it.hasNext()) {
                            long id = it.next();
                            requestList.add(Long.toUnsignedString(id));
                            it.remove();
                            if (requestList.size() == 100) {
                                break;
                            }
                        }
                    }
                    // [NAT] inject useNATService property to the request params
                    request.put("requestType", "getTransactions");
                    request.putAll(Peers.getNatAndAddressMap());
                    request.put("transactionIds", requestList);
                    JSONObject response = peer.send(JSON.prepareRequest(request), Peers.MAX_RESPONSE_SIZE);
                    if (response == null) {
                        return;
                    }
                    //
                    // Restore the prunable data
                    //
                    JSONArray transactions = (JSONArray) response.get("transactions");
                    if (transactions == null || transactions.isEmpty()) {
                        return;
                    }
                    List<Transaction> processed =
                            Conch.getTransactionProcessor().restorePrunableData(transactions);
                    //
                    // Remove transactions that have been successfully processed
                    //
                    synchronized (prunableTransactions) {
                        processed.forEach(transaction -> prunableTransactions.remove(transaction.getId()));
                    }
                }
                Logger.logDebugMessage("Done retrieving prunable transactions from " + peer.getHost());
            } catch (ConchException.ValidationException e) {
                Logger.logErrorMessage(
                        "Peer " + peer.getHost() + " returned invalid prunable transaction", e);
                peer.blacklist(e);
            } catch (RuntimeException e) {
                Logger.logErrorMessage("Unable to restore prunable data", e);
            } finally {
                isRestoring = false;
                Logger.logDebugMessage("Remaining " + prunableTransactions.size() + " pruned transactions");
            }
        }
    }

    private BlockchainProcessorImpl() {
        final int trimFrequency = Conch.getIntProperty("sharder.trimFrequency");
        blockListeners.addListener(
                block -> {
                    if (block.getHeight() % 5000 == 0) {
                        Logger.logMessage("processed block " + block.getHeight());
                    }
                    if (block.getHeight() != 0
                        && (trimDerivedTables && block.getHeight() % trimFrequency == 0)) {
                        doTrimDerivedTables();
                    }
                },
                Event.BLOCK_SCANNED);

        blockListeners.addListener(
                block -> {
                    if (trimDerivedTables && block.getHeight() % trimFrequency == 0 && !isTrimming) {
                        isTrimming = true;
                        networkService.submit(
                                () -> {
                                    trimDerivedTables();
                                    isTrimming = false;
                                });
                    }
                    if (block.getHeight() % 5000 == 0) {
                        Logger.logMessage("received block " + block.getHeight());
                        if (!isDownloading || block.getHeight() % 50000 == 0) {
                            networkService.submit(Db.db::analyzeTables);
                        }
                    }
                },
                Event.BLOCK_PUSHED);

        blockListeners.addListener(CheckSumValidator.eventProcessor(), Event.BLOCK_PUSHED);

        blockListeners.addListener(block -> Db.db.analyzeTables(), Event.RESCAN_END);

        /**
         * add ExchangeProcessor
         * @Author peifeng
         */
        blockListeners.addListener(new ExchangeProcessor(), Event.BEFORE_BLOCK_ACCEPT);

        ThreadPool.runBeforeStart(
                () -> {
                    alreadyInitialized = true;
                    if (addGenesisBlock()) {
                        scan(0, false);
                    } else if (Conch.getBooleanProperty("sharder.forceScan")) {
                        scan(0, Conch.getBooleanProperty("sharder.forceValidate"));
                    } else {
                        boolean rescan;
                        boolean validate;
                        int height;
                        try (Connection con = Db.db.getConnection();
                             Statement stmt = con.createStatement();
                             ResultSet rs = stmt.executeQuery("SELECT * FROM scan")) {
                            rs.next();
                            rescan = rs.getBoolean("rescan");
                            validate = rs.getBoolean("validate");
                            height = rs.getInt("height");
                        } catch (SQLException e) {
                            throw new RuntimeException(e.toString(), e);
                        }
                        if (rescan) {
                            scan(height, validate);
                        }
                    }
                },
                false);

        if (!Constants.isLightClient
            && !Constants.isOffline) {
            Logger.logInfoMessage("Current node mode[light client=%s, offline=%s]", Constants.isLightClient, Constants.isOffline);
            Logger.logInfoMessage("Create a thread 'GetMoreBlocks' to sync the blocks from other peers.....");
            ThreadPool.scheduleThread("GetMoreBlocks", getMoreBlocksThread, 1);
        }
    }

    @Override
    public boolean addListener(Listener<Block> listener, Event eventType) {
        return blockListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<Block> listener, Event eventType) {
        return blockListeners.removeListener(listener, eventType);
    }

    @Override
    public void registerDerivedTable(DerivedDbTable table) {
        if (alreadyInitialized) {
            throw new IllegalStateException(
                    "Too late to register table " + table + ", must have done it in Conch.Init");
        }
        derivedTables.add(table);
    }

    @Override
    public void trimDerivedTables() {
        try {
            Db.db.beginTransaction();
            doTrimDerivedTables();
            Db.db.commitTransaction();
        } catch (Exception e) {
            Logger.logMessage(e.toString(), e);
            Db.db.rollbackTransaction();
            throw e;
        } finally {
            Db.db.endTransaction();
        }
    }

    private void doTrimDerivedTables() {
        int trimEndHeight = blockchain.getHeight();
        if(trimEndHeight == 0) {
            try{
                BlockImpl lastBlock = BlockDb.findLastBlock();
                trimEndHeight =  lastBlock != null ? lastBlock.getHeight() : 0 ;
            }catch(Exception e){
                Logger.logErrorMessage("can't get the last block height in the trim processing", e);
            }
        }

        lastTrimHeight = Math.max(trimEndHeight - Constants.MAX_ROLLBACK, 0);
        if (lastTrimHeight > 0) {
            Logger.logInfoMessage("[Trim-%d] Start to trim the tables before the height %d...", trimEndHeight, lastTrimHeight);
            for (DerivedDbTable table : derivedTables) {
                try {
                    blockchain.readLock();
                    long startMS = System.currentTimeMillis();
                    Logger.logDebugMessage("[Trim-%d] Start to trim table %s before the height %d...", trimEndHeight, table.toString(), lastTrimHeight);
                    table.trim(lastTrimHeight);
                    Db.db.commitTransaction();
                    long tableTrimmingMS = System.currentTimeMillis();
                    Logger.logDebugMessage("[Trim-%d] Finish the %s table trimming, used time %d S(≈%d MS)"
                            , trimEndHeight, table.toString(), (tableTrimmingMS-startMS) /1000, (tableTrimmingMS-startMS));
                }catch (Exception e) {
                    Logger.logMessage(e.toString(), e);
                    Db.db.rollbackTransaction();
                } finally {
                    blockchain.readUnlock();
                }
            }
        }
    }

    public List<DerivedDbTable> getDerivedTables() {
        return derivedTables;
    }

    @Override
    public Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    @Override
    public int getLastBlockchainFeederHeight() {
        return lastBlockchainFeederHeight;
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public int getInitialScanHeight() {
        return initialScanHeight;
    }

    @Override
    public boolean isDownloading() {
        return isDownloading;
    }

    @Override
    public boolean isUpToDate() {
        return Peers.getMyBlockchainState() == Peer.BlockchainState.UP_TO_DATE;
    }

    @Override
    public boolean isObsolete() {
        return Peers.getMyBlockchainState() == Peer.BlockchainState.OBSOLETE;
    }

    @Override
    public boolean isProcessingBlock() {
        return isProcessingBlock;
    }

    @Override
    public boolean isGetMoreBlocks() {
        return getMoreBlocks;
    }

    @Override
    public int getMinRollbackHeight() {
        return trimDerivedTables
                ? (lastTrimHeight > 0
                ? lastTrimHeight
                : Math.max(blockchain.getHeight() - Constants.MAX_ROLLBACK, 0))
                : 0;
    }

    @Override
    public void processPeerBlock(JSONObject request) throws ConchException {
        BlockImpl block = BlockImpl.parseBlock(request);
        BlockImpl lastBlock = blockchain.getLastBlock();
        if (block.getPreviousBlockId() == lastBlock.getId()) {
            pushBlock(block);
        } else if (block.getPreviousBlockId() == lastBlock.getPreviousBlockId()
                && block.getTimestamp() < lastBlock.getTimestamp()) {
            blockchain.writeLock();
            try {
                if (lastBlock.getId() != blockchain.getLastBlock().getId()) {
                    return; // blockchain changed, ignore the block
                }
                BlockImpl previousBlock = blockchain.getBlock(lastBlock.getPreviousBlockId());
                lastBlock = popOffTo(previousBlock).get(0);
                try {
                    pushBlock(block);
                    TransactionProcessorImpl.getInstance().processLater(lastBlock.getTransactions());
                    Logger.logDebugMessage(
                            "Last block " + lastBlock.getStringId() + " was replaced by " + block.getStringId());
                } catch (BlockNotAcceptedException e) {
                    Logger.logDebugMessage("Replacement block failed to be accepted, pushing back our last block");
                    pushBlock(lastBlock);
                    TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                }
            } finally {
                blockchain.writeUnlock();
            }
        } // else ignore the block
    }

    @Override
    public List<BlockImpl> popOffTo(int height) {
        if (height <= 0) {
            fullReset();
        } else if (height < blockchain.getHeight()) {
            return popOffTo(blockchain.getBlockAtHeight(height));
        }
        return Collections.emptyList();
    }

    @Override
    public void fullReset() {
        blockchain.writeLock();
        try {
            try {
                setGetMoreBlocks(false);
                scheduleScan(0, false);
                // BlockDb.deleteBlock(Genesis.GENESIS_BLOCK_ID); // fails with stack overflow in H2
                BlockDb.deleteAll(true);
                if (addGenesisBlock()) {
                    scan(0, false);
                }
            } finally {
                setGetMoreBlocks(true);
            }
        } finally {
            blockchain.writeUnlock();
        }
    }

    @Override
    public void setGetMoreBlocks(boolean getMoreBlocks) {
        this.getMoreBlocks = getMoreBlocks;
    }

    @Override
    public int restorePrunedData() {
        Db.db.beginTransaction();
        try (Connection con = Db.db.getConnection()) {
            int now = Conch.getEpochTime();
            int minTimestamp = Math.max(1, now - Constants.MAX_PRUNABLE_LIFETIME);
            int maxTimestamp = Math.max(minTimestamp, now - Constants.MIN_PRUNABLE_LIFETIME) - 1;
            List<TransactionDb.PrunableTransaction> transactionList =
                    TransactionDb.findPrunableTransactions(con, minTimestamp, maxTimestamp);
            transactionList.forEach(
                    prunableTransaction -> {
                        long id = prunableTransaction.getId();
                        if ((prunableTransaction.hasPrunableAttachment()
                                && prunableTransaction.getTransactionType().isPruned(id))
                                || PrunableMessage.isPruned(
                                id,
                                prunableTransaction.hasPrunablePlainMessage(),
                                prunableTransaction.hasPrunableEncryptedMessage())) {
                            synchronized (prunableTransactions) {
                                prunableTransactions.add(id);
                            }
                        }
                    });
            if (!prunableTransactions.isEmpty()) {
                lastRestoreTime = 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            Db.db.endTransaction();
        }
        synchronized (prunableTransactions) {
            return prunableTransactions.size();
        }
    }

    @Override
    public Transaction restorePrunedTransaction(long transactionId) {
        TransactionImpl transaction = TransactionDb.findTransaction(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        boolean isPruned = false;
        for (Appendix.AbstractAppendix appendage : transaction.getAppendages(true)) {
            if ((appendage instanceof Appendix.Prunable)
                    && !((Appendix.Prunable) appendage).hasPrunableData()) {
                isPruned = true;
                break;
            }
        }
        if (!isPruned) {
            return transaction;
        }
        List<Peer> peers = Peers.getPeers(
                chkPeer -> chkPeer.providesService(Peer.Service.PRUNABLE)
                        && !chkPeer.isBlacklisted()
                        && chkPeer.getAnnouncedAddress() != null);
        if (peers.isEmpty()) {
            Logger.logDebugMessage("Cannot find any archive peers");
            return null;
        }
        JSONObject json = new JSONObject();
        JSONArray requestList = new JSONArray();
        requestList.add(Long.toUnsignedString(transactionId));
        json.put("requestType", "getTransactions");
        json.put("transactionIds", requestList);
        JSONStreamAware request = JSON.prepareRequest(json);
        for (Peer peer : peers) {
            if (peer.getState() != Peer.State.CONNECTED) {
                Peers.connectPeer(peer);
            }
            if (peer.getState() != Peer.State.CONNECTED) {
                continue;
            }
            Logger.logDebugMessage("Connected to archive peer " + peer.getHost());
            JSONObject response = peer.send(request);
            if (response == null) {
                continue;
            }
            JSONArray transactions = (JSONArray) response.get("transactions");
            if (transactions == null || transactions.isEmpty()) {
                continue;
            }
            try {
                List<Transaction> processed =
                        Conch.getTransactionProcessor().restorePrunableData(transactions);
                if (processed.isEmpty()) {
                    continue;
                }
                synchronized (prunableTransactions) {
                    prunableTransactions.remove(transactionId);
                }
                return processed.get(0);
            } catch (ConchException.NotValidException e) {
                Logger.logErrorMessage(
                        "Peer " + peer.getHost() + " returned invalid prunable transaction", e);
                peer.blacklist(e);
            }
        }
        return null;
    }

    public void shutdown() {
        ThreadPool.shutdownExecutor("networkService", networkService, 5);
    }

    private void addBlock(BlockImpl block) {
        try (Connection con = Db.db.getConnection()) {
            BlockDb.saveBlock(con, block);
            blockchain.setLastBlock(block);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private boolean addGenesisBlock() {
        if (BlockDb.hasBlock(SharderGenesis.GENESIS_BLOCK_ID, 0)) {
            Logger.logMessage("Genesis block already in database");
            BlockImpl lastBlock = BlockDb.findLastBlock();
            blockchain.setLastBlock(lastBlock);
            popOffTo(lastBlock);
            Logger.logMessage("Last block height: " + lastBlock.getHeight());
            return false;
        }

        Logger.logMessage("SharderGenesis block not in database, starting from scratch");
        try {
            BlockImpl genesisBlock = SharderGenesis.genesisBlock();
            Logger.logInfoMessage("SharderGenesis block[id=" + genesisBlock.getId() + ",generator=" + genesisBlock.getGeneratorId() + ",baseTarget=" + genesisBlock.getBaseTarget() + ",amount=" + genesisBlock.getTotalAmountNQT() + ",fee=" + genesisBlock.getTotalFeeNQT());
            addBlock(genesisBlock);
            return true;
        } catch (ConchException.ValidationException e) {
            Logger.logMessage(e.getMessage());
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void pushBlock(final BlockImpl block) throws BlockNotAcceptedException, GeneratorNotAcceptedException, ConchException.StopException {

        int curTime = Conch.getEpochTime();
        blockchain.writeLock();
        try {
            boolean delayedOrOldPocTxsProcessed = (Constants.isDevnet() && Generator.isBootNode)
//                    || (!Constants.isDevnet() && Conch.getPocProcessor().processDelayedPocTxs(Conch.getHeight()));
                    || (!Constants.isDevnet() && Conch.getPocProcessor().pocTxsProcessed(Conch.getHeight()));

//            if (Conch.reachLastKnownBlock() && !delayedPocTxsProcessed) {
            if (!delayedOrOldPocTxsProcessed && !Constants.isDevnet()) {
                Logger.logInfoMessage("should process delayed or old poc txs <= [ height %d ] before accepting blocks, break block pushing till poc txs processed ", Conch.getHeight());
                return;
            }

            BlockImpl previousLastBlock = null;
            try {
                Db.db.beginTransaction();
                previousLastBlock = blockchain.getLastBlock();

                validate(block, previousLastBlock, curTime);

                long nextHitTime = Generator.getNextHitTime(previousLastBlock.getId(), curTime);
                if (nextHitTime > 0 && block.getTimestamp() > nextHitTime + 1) {
                    String msg = "Rejecting block " + block.getStringId() + " at height " + previousLastBlock.getHeight()
                            + " block timestamp " + block.getTimestamp() + " next hit time "
                            + nextHitTime + " current time " + curTime;
                    Logger.logInfoMessage(msg);
                    Generator.setDelay(-Constants.MINING_SPEEDUP);
                    throw new BlockOutOfOrderException(msg, block);
                }

                Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
                List<TransactionImpl> validPhasedTransactions = new ArrayList<>();
                List<TransactionImpl> invalidPhasedTransactions = new ArrayList<>();

                validatePhasedTransactions(previousLastBlock.getHeight(), validPhasedTransactions, invalidPhasedTransactions, duplicates);
                validateTransactions(block, previousLastBlock, curTime, duplicates);

                block.calAndSetByPreviousBlock(previousLastBlock);
                blockListeners.notify(block, Event.BEFORE_BLOCK_ACCEPT);
                TransactionProcessorImpl.getInstance().requeueAllUnconfirmedTransactions();
                addBlock(block);
                accept(block, validPhasedTransactions, invalidPhasedTransactions, duplicates);

                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                blockchain.setLastBlock(previousLastBlock);
                if (e instanceof BlockOutOfOrderException) {
                    Logger.logWarningMessage("Push block at height %d failed caused by BlockOutOfOrderException %s",
                            previousLastBlock.getHeight(), block.toSummary());
                } else {
                    Logger.logErrorMessage(String.format("Push block at height %d failed",
                     previousLastBlock.getHeight()), e);
                }
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            blockListeners.notify(block, Event.AFTER_BLOCK_ACCEPT);
        } finally {
            blockchain.writeUnlock();
        }

        // broadcast block to other peers
        if (block.getTimestamp() >= (curTime - Constants.GAP_SECONDS)) {
            Peers.sendToSomePeers(block);
        }

        blockListeners.notify(block, Event.BLOCK_PUSHED);
    }

    private void validatePhasedTransactions(int height, List<TransactionImpl> validPhasedTransactions, List<TransactionImpl> invalidPhasedTransactions, Map<TransactionType, Map<String, Integer>> duplicates) {
        if (height >= Constants.PHASING_BLOCK_HEIGHT) {
            DbIterator<TransactionImpl> phasedTransactions = null;
            try {
                phasedTransactions =
                        PhasingPoll.getFinishingTransactions(height + 1);
                for (TransactionImpl phasedTransaction : phasedTransactions) {
                    if (height > Constants.SHUFFLING_BLOCK_HEIGHT && PhasingPoll.getResult(phasedTransaction.getId()) != null) {
                        continue;
                    }

                    try {
                        phasedTransaction.validate();
                        if (!phasedTransaction.attachmentIsDuplicate(duplicates, false)) {
                            validPhasedTransactions.add(phasedTransaction);
                        } else {
                            Logger.logDebugMessage("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " is duplicate, will not apply");
                            invalidPhasedTransactions.add(phasedTransaction);
                        }
                    } catch (ConchException.ValidationException e) {
                        Logger.logDebugMessage("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " no longer passes validation: " + e.getMessage() + ", will not apply");
                        invalidPhasedTransactions.add(phasedTransaction);
                    }
                }
            } finally {
                DbUtils.close(phasedTransactions);
            }
        }
    }


    private void validate(BlockImpl block, BlockImpl previousLastBlock, int curTime) throws BlockNotAcceptedException, GeneratorNotAcceptedException {
        if (Generator.isBlackedMiner(block.getGeneratorId())) {
            throw new GeneratorNotAcceptedException("Invalid generator", block.getGeneratorId());
        }

        if (previousLastBlock.getId() != block.getPreviousBlockId()) {
            throw new BlockOutOfOrderException("Previous block id doesn't match[previous block id of current chain=" + previousLastBlock.getId()
                    + ", pushing block id=" + block.getId() + "]", block);
        }
        if (block.getVersion() != getBlockVersion(previousLastBlock.getHeight())) {
            throw new BlockNotAcceptedException("Invalid version " + block.getVersion(), block);
        }
        // time valid check
        if (block.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
            Logger.logWarningMessage(
                    "Received block " + block.getStringId() + " from the future, timestamp " + block.getTimestamp() + " generator " + Long.toUnsignedString(block.getGeneratorId())
                            + " current time " + curTime + ", system clock may be off");
            throw new BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp() + " current time is " + curTime, block);
        }
        if (block.getTimestamp() <= previousLastBlock.getTimestamp()) {
            throw new BlockNotAcceptedException("Block timestamp " + block.getTimestamp() + " is before previous block timestamp " + previousLastBlock.getTimestamp(), block);
        }
        // previous block hash check
        if (!Arrays.equals(Crypto.sha256().digest(previousLastBlock.bytes()), block.getPreviousBlockHash())) {
            throw new BlockNotAcceptedException("Previous block hash doesn't match", block);
        }
        if (block.getId() == 0L || BlockDb.hasBlock(block.getId(), previousLastBlock.getHeight())) {
            throw new BlockNotAcceptedException("Duplicate block or invalid id", block);
        }
        if (!block.verifyGenerationSignature() && !Generator.allowsFakeMining(block.getGeneratorPublicKey())) {
            Account generatorAccount = Account.getAccount(block.getGeneratorId());
            PocScore pocScoreObj = Conch.getPocProcessor().calPocScore(generatorAccount, previousLastBlock.getHeight());
            String errorMsg = String.format("Block generation signature verification failed, generator %s(id=%d, last update height=%d)'s poc score is %d at height %d.",
                    generatorAccount.getRsAddress(), generatorAccount.getId(), pocScoreObj.getHeight() , pocScoreObj.total(), (previousLastBlock.getHeight() + 1));
            throw new BlockNotAcceptedException(errorMsg, block);
        }
        if (!block.verifyBlockSignature()) {
            throw new BlockNotAcceptedException("Block signature verification failed", block);
        }
        if (block.getTransactions().size() > Constants.MAX_NUMBER_OF_TRANSACTIONS) {
            throw new BlockNotAcceptedException(
                    "Invalid block transaction count " + block.getTransactions().size(), block);
        }
        if (block.getPayloadLength() > Constants.MAX_PAYLOAD_LENGTH || block.getPayloadLength() < 0) {
            throw new BlockNotAcceptedException(
                    "Invalid block payload length " + block.getPayloadLength(), block);
        }
    }


    private void validateTransactions(BlockImpl block, BlockImpl previousLastBlock, int curTime, Map<TransactionType, Map<String, Integer>> duplicates) throws BlockNotAcceptedException {
        long payloadLength = 0;
        long calculatedTotalAmount = 0;
        long calculatedTotalFee = 0;
        MessageDigest digest = Crypto.sha256();
        boolean hasPrunedTransactions = false;
        int coinBaseNum = 0;
        Map<Long, Transaction> uploadTransactions = new HashMap<>();
        Map<Long, Map<String, Long>> backupNum = new HashMap<>();
        for (TransactionImpl transaction : block.getTransactions()) {

            if (transaction.getAttachment() instanceof Attachment.CoinBase) {
                coinBaseNum++;
            }

            if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
                throw new BlockOutOfOrderException("Invalid transaction timestamp: " + transaction.getTimestamp() + ", current time is " + curTime, block);
            }

            if (!transaction.verifySignature()) {
                throw new TransactionNotAcceptedException("Transaction signature verification failed at height " + previousLastBlock.getHeight(), transaction);
            }

            //full version check
            if (transaction.getTimestamp() > block.getTimestamp() + Constants.MAX_TIMEDRIFT
                    || (transaction.getExpiration() < block.getTimestamp())) {
                throw new TransactionNotAcceptedException(
                        "Invalid transaction timestamp "
                                + transaction.getTimestamp()
                                + ", current time is "
                                + curTime
                                + ", block timestamp is "
                                + block.getTimestamp(),
                        transaction);
            }

            if (TransactionDb.hasTransaction(transaction.getId(), previousLastBlock.getHeight())) {
                throw new TransactionNotAcceptedException("Transaction is already in the blockchain", transaction);
            }
            //FIXME[checksum]
            if (transaction.referencedTransactionFullHash() != null) {
                if ((previousLastBlock.getHeight() < Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                        && !TransactionDb.hasTransaction(
                        Convert.fullHashToId(transaction.referencedTransactionFullHash()),
                        previousLastBlock.getHeight()))
                        || (previousLastBlock.getHeight() >= Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                        && !hasAllReferencedTransactions(transaction, transaction.getTimestamp(), 0))) {
                    throw new TransactionNotAcceptedException("Missing or invalid referenced transaction "
                            + transaction.getReferencedTransactionFullHash(), transaction);
                }
            }

            if (transaction.getVersion() != getTransactionVersion(previousLastBlock.getHeight())) {
                throw new TransactionNotAcceptedException("Invalid transaction version " + transaction.getVersion() + " at height " + previousLastBlock.getHeight(), transaction);
            }

            if (transaction.getId() == 0L) {
                throw new TransactionNotAcceptedException("Invalid transaction id 0", transaction);
            }

            try {
                transaction.validate();
            } catch (ConchException.ValidationException e) {
                throw new TransactionNotAcceptedException(e.getMessage(), transaction);
            }


            if (transaction.attachmentIsDuplicate(duplicates, true)) {
                throw new TransactionNotAcceptedException("Transaction is a duplicate", transaction);
            }

            // backup number validate
            if (transaction.getType() == StorageTx.STORAGE_UPLOAD) {
                uploadTransactions.put(transaction.getId(), transaction);
            }

            if (transaction.getType() == StorageTx.STORAGE_BACKUP) {
                if (!hasUploadTransaction(uploadTransactions, transaction)) {
                    throw new TransactionNotAcceptedException(
                            "Current backup transaction is in the front of upload transaction ", transaction);
                }
                if (hasBackuped(backupNum, transaction)) {
                    throw new TransactionNotAcceptedException(
                            transaction.getSenderId() + " has already backup the upload transaction ",
                            transaction);
                }
                if (isBackupNumberExceed(uploadTransactions, backupNum, transaction)) {
                    throw new TransactionNotAcceptedException("Backup transaction is exceed ", transaction);
                }
            }

            // verify block coinBase tx
            if(Constants.BLOCK_REWARD_VERIFY
                    && blockchain.getHeight()+1 >= Constants.BLOCK_REWARD_VERIFY_HEIGHT
                    && transaction.getType().isType(TransactionType.TYPE_COIN_BASE)
                    && transaction.getAmountNQT() != Constants.rewardCalculatorInstance.blockReward(blockchain.getHeight()+1)) {
                throw new TransactionNotAcceptedException("CoinBaseTx verification failed", transaction);
            }

            if (!hasPrunedTransactions) {
                for (Appendix.AbstractAppendix appendage : transaction.getAppendages()) {
                    if ((appendage instanceof Appendix.Prunable)
                            && !((Appendix.Prunable) appendage).hasPrunableData()) {
                        hasPrunedTransactions = true;
                        break;
                    }
                }
            }

            calculatedTotalAmount += transaction.getAmountNQT();
            if (!StorageTxProcessorImpl.getInstance().isStorageUploadTransaction(transaction)) {
                calculatedTotalFee += transaction.getFeeNQT();
            } else {
                calculatedTotalFee += transaction.getMinimumFeeNQT(blockchain.getHeight());
            }
            payloadLength += transaction.getFullSize();
            digest.update(transaction.bytes());

            block.autoExtensionAppend(transaction);
        }

        if (calculatedTotalAmount != block.getTotalAmountNQT()
                || calculatedTotalFee != block.getTotalFeeNQT()) {
            throw new BlockNotAcceptedException(
                    "Total amount or fee don't match transaction totals", block);
        }

        if(!RewardCalculator.closeValidationForCrowdCoinbaseTx) {
            if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
                throw new BlockNotAcceptedException("Payload hash doesn't match", block);
            }

            if (hasPrunedTransactions
                    ? payloadLength > block.getPayloadLength()
                    : payloadLength != block.getPayloadLength()) {
                throw new BlockNotAcceptedException(
                        "Transaction payload length "
                                + payloadLength
                                + " does not match block payload length "
                                + block.getPayloadLength(),
                        block);
            }
        }

        // coinbase count check
        if (coinBaseNum != 1) {
            throw new BlockNotAcceptedException(
                    "The number of CoinBase transaction doesn't match 1", block);
        }

    }


    private void accept(
            BlockImpl block,
            List<TransactionImpl> validPhasedTransactions,
            List<TransactionImpl> invalidPhasedTransactions,
            Map<TransactionType, Map<String, Integer>> duplicates)
            throws TransactionNotAcceptedException, ConchException.StopException {
        try {
            isProcessingBlock = true;

            if(block.getHeight() >= Constants.MINER_REMOVE_HEIGHT){
                checkMiner(block);
            }

            // unconfirmed balance update
            for (TransactionImpl transaction : block.getTransactions()) {
                if (!transaction.applyUnconfirmed()) {
                    if (CheckSumValidator.isKnownIgnoreTx(transaction.getId())) {
                        Logger.logWarningMessage("this tx[id=%d, creator=%s, height=%d] is known ignored tx, don't apply and ignore it"
                                , transaction.getId(), Account.rsAccount(transaction.getSenderId()), transaction.getHeight());
                    } else if (RewardCalculator.applyUnconfirmedReward(transaction)) {

                    } else {
                        throw new TransactionNotAcceptedException("Double spending", transaction);
                    }
                }
            }
            blockListeners.notify(block, Event.BEFORE_BLOCK_APPLY);
            block.apply();
            validPhasedTransactions.forEach(transaction -> transaction.getPhasing().countVotes(transaction));
            invalidPhasedTransactions.forEach(transaction -> transaction.getPhasing().reject(transaction));
            int fromTimestamp = Conch.getEpochTime() - Constants.MAX_PRUNABLE_LIFETIME;
            for (TransactionImpl transaction : block.getTransactions()) {
                try {
                    try {
                        transaction.apply();
                    } catch (Account.DoubleSpendingException e) {
                        if (CheckSumValidator.isDoubleSpendingIgnoreTx(transaction)) {
                            Logger.logWarningMessage("Ignore the double spending tx => " + transaction.toPrintString());
                            Logger.logErrorMessage("Ignore the double spending tx", e);
                        } else {
                            throw e;
                        }
                    }

                    if (transaction.getTimestamp() > fromTimestamp) {
                        for (Appendix.AbstractAppendix appendage : transaction.getAppendages(true)) {
                            if ((appendage instanceof Appendix.Prunable) && !((Appendix.Prunable) appendage).hasPrunableData()) {
                                synchronized (prunableTransactions) {
                                    prunableTransactions.add(transaction.getId());
                                }
                                lastRestoreTime = 0;
                                break;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    Logger.logErrorMessage(e.toString(), e);
                    throw new TransactionNotAcceptedException(e, transaction);
                }
            }
            if (block.getHeight() > Constants.SHUFFLING_BLOCK_HEIGHT) {
                SortedSet<TransactionImpl> possiblyApprovedTransactions = new TreeSet<>(finishingTransactionsComparator);
                block.getTransactions().forEach(
                        transaction -> {
                            PhasingPoll.getLinkedPhasedTransactions(transaction.fullHash()).forEach(
                                    phasedTransaction -> {
                                        if (phasedTransaction.getPhasing().getFinishHeight() > block.getHeight()) {
                                            possiblyApprovedTransactions.add((TransactionImpl) phasedTransaction);
                                        }
                                    });

                            if (transaction.getType() == TransactionType.Messaging.PHASING_VOTE_CASTING && !transaction.attachmentIsPhased()) {
                                Attachment.MessagingPhasingVoteCasting voteCasting = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                                voteCasting.getTransactionFullHashes().forEach(
                                        hash -> {
                                            PhasingPoll phasingPoll =
                                                    PhasingPoll.getPoll(Convert.fullHashToId(hash));
                                            if (phasingPoll.allowEarlyFinish()
                                                    && phasingPoll.getFinishHeight() > block.getHeight()) {
                                                possiblyApprovedTransactions.add(
                                                        TransactionDb.findTransaction(phasingPoll.getId()));
                                            }
                                        });
                            }

                        });

                validPhasedTransactions.forEach(
                        phasedTransaction -> {
                            if (phasedTransaction.getType() == TransactionType.Messaging.PHASING_VOTE_CASTING) {
                                PhasingPoll.PhasingPollResult result = PhasingPoll.getResult(phasedTransaction.getId());
                                if (result != null && result.isApproved()) {
                                    Attachment.MessagingPhasingVoteCasting phasingVoteCasting = (Attachment.MessagingPhasingVoteCasting) phasedTransaction.getAttachment();
                                    phasingVoteCasting.getTransactionFullHashes().forEach(
                                            hash -> {
                                                PhasingPoll phasingPoll =
                                                        PhasingPoll.getPoll(Convert.fullHashToId(hash));
                                                if (phasingPoll.allowEarlyFinish()
                                                        && phasingPoll.getFinishHeight() > block.getHeight()) {
                                                    possiblyApprovedTransactions.add(
                                                            TransactionDb.findTransaction(phasingPoll.getId()));
                                                }
                                            });
                                }
                            }
                        });
                possiblyApprovedTransactions.forEach(
                        transaction -> {
                            if (PhasingPoll.getResult(transaction.getId()) == null) {
                                try {
                                    transaction.validate();
                                    transaction.getPhasing().tryCountVotes(transaction, duplicates);
                                } catch (ConchException.ValidationException e) {
                                    Logger.logDebugMessage("At height " + block.getHeight() + " phased transaction " + transaction.getId() + " no longer passes validation: " + e.getMessage() + ", cannot finish early");
                                }
                            }
                        });
            }
            blockListeners.notify(block, Event.AFTER_BLOCK_APPLY);
            if (block.getTransactions().size() > 0) {
                TransactionProcessorImpl.getInstance().notifyListeners(block.getTransactions(), TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
            }
            AccountLedger.commitEntries();


        } finally {
            isProcessingBlock = false;
            AccountLedger.clearEntries();
        }
    }

    private static final Comparator<Transaction> finishingTransactionsComparator =
            Comparator.comparingInt(Transaction::getHeight)
                    .thenComparingInt(Transaction::getIndex)
                    .thenComparingLong(Transaction::getId);

    /**
     * Rollback to height of commonBlock, and return blocks list of rolled back
     * @param commonBlock
     * @return Rolled back blocks
     */
    public List<BlockImpl> popOffTo(Block commonBlock) {
        blockchain.writeLock();
        try {
            if (!Db.db.isInTransaction()) {
                try {
                    Db.db.beginTransaction();
                    return popOffTo(commonBlock);
                } finally {
                    Db.db.endTransaction();
                }
            }
            if (commonBlock.getHeight() < getMinRollbackHeight()) {
                Logger.logMessage("Rollback to height " + commonBlock.getHeight() + " not supported, will do a full rescan");
                popOffWithRescan(commonBlock.getHeight() + 1);
                return Collections.emptyList();
            }
            if (!blockchain.hasBlock(commonBlock.getId())) {
                Logger.logDebugMessage("Block " + commonBlock.getStringId() + " not found in blockchain, nothing to pop off");
                return Collections.emptyList();
            }
            List<BlockImpl> poppedOffBlocks = new ArrayList<>();
            try {
                BlockImpl block = blockchain.getLastBlock();
                block.loadTransactions();
                Logger.logDebugMessage("Rollback from height %d[id=%d, miner=%s] to height %d[id=%d, miner=%s]"
                , block.getHeight(), block.getId(), Account.rsAccount(block.getGeneratorId())
                , commonBlock.getHeight(), commonBlock.getId(), Account.rsAccount(commonBlock.getGeneratorId())
                );
                while (block.getId() != commonBlock.getId() && block.getId() != SharderGenesis.GENESIS_BLOCK_ID) {
                    poppedOffBlocks.add(block);
                    block = popLastBlock();
                }
                for (DerivedDbTable table : derivedTables) {
                    table.rollback(commonBlock.getHeight());
                }
                RewardCalculator.rollBackTo(commonBlock.getHeight());
                Conch.getPocProcessor().rollbackTo(commonBlock.getHeight());

                Db.db.clearCache();
                Db.db.commitTransaction();
            } catch (RuntimeException e) {
                Logger.logErrorMessage("Error popping off to " + commonBlock.getHeight() + ", " + e.toString());
                Db.db.rollbackTransaction();
                BlockImpl lastBlock = BlockDb.findLastBlock();
                if(commonBlock.getId() != lastBlock.getId()) {
                    blockchain.setLastBlock(lastBlock);
                    popOffTo(lastBlock);
                }
                throw e;
            }
            return poppedOffBlocks;
        } finally {
            blockchain.writeUnlock();
        }
    }

    private BlockImpl popLastBlock() {
        BlockImpl block = blockchain.getLastBlock();
        if (block.getId() == SharderGenesis.GENESIS_BLOCK_ID) {
            throw new RuntimeException("Cannot pop off genesis block");
        }
        BlockImpl previousBlock = BlockDb.deleteBlocksFrom(block.getId());
        previousBlock.loadTransactions();
        blockchain.setLastBlock(previousBlock);
        blockListeners.notify(block, Event.BLOCK_POPPED);
        return previousBlock;
    }

    private static long QUALIFIED_CROWD_MINER_HOLDING_AMOUNT_MIN = 32*133L; // 1T-133MW

    /**
     * 条件检查的时间点： 在区块确认时，针对转账交易进行检查
     * 转出方是否存在于矿工列表，存在进行最新挖矿持仓量检查，不满足移除；
     * 方案1：
     *   - 矿工数据做逻辑删除，设置检查状态，处于检查状态的逻辑删除矿工数，每个区块高度仍然按照第一条进行条件检测
     *   - 回滚和分叉情况下，放回矿工列表
     * @param block
     */
    private void checkMiner(BlockImpl block){
        //得到矿工列表
        HashMap<Long, Long> crowdMiners = new HashMap<>();
        for (TransactionImpl transaction : block.getTransactions()) {
            if(transaction.getType().isType(TransactionType.TYPE_COIN_BASE)){
                Attachment.CoinBase coinBase = (Attachment.CoinBase) transaction.getAttachment();
                if((coinBase.isType(Attachment.CoinBase.CoinBaseType.CROWD_BLOCK_REWARD)
                        ||coinBase.isType(Attachment.CoinBase.CoinBaseType.BLOCK_REWARD))
                        && coinBase.getCrowdMiners().size() > 0){
                    crowdMiners = coinBase.getCrowdMiners();
                    break;
                }
            }
        }

        for(Long crowdMinerId : crowdMiners.keySet()){
            CertifiedPeer certifiedPeer = Conch.getPocProcessor().getCertifiedPeers().get(crowdMinerId);
            if(certifiedPeer!=null && certifiedPeer.getDeleteHeight() > block.getHeight()){
                certifiedPeer.setDeleteHeight(0);
                PocDb.saveOrUpdatePeer(certifiedPeer);
            }
        }

        for (TransactionImpl transaction : block.getTransactions()) {
            //检查转账交易
            if(transaction.getType().isType(TransactionType.TYPE_PAYMENT)){
                //转出方是否存在于矿工列表
                for(Long crowdMinerId : crowdMiners.keySet()){
                    if(crowdMinerId.equals(transaction.getSenderId())){
                        //存在进行最新挖矿持仓量检查
                        long holdingAmount = 0;
                        try{
                            if(Account.getAccount(crowdMinerId)!=null){
                                holdingAmount = Account.getAccount(crowdMinerId).getEffectiveBalanceSS(block.getHeight());
                            }
                        }catch(Exception e){
                            Logger.logWarningMessage("[QualifiedMiner] not valid miner because can't get balance of account %s at height %d, caused by %s",  Account.getAccount(crowdMinerId).getRsAddress(), block.getHeight(), e.getMessage());
                            holdingAmount = 0;
                        }

                        CertifiedPeer certifiedPeer = null;
                        if(holdingAmount < QUALIFIED_CROWD_MINER_HOLDING_AMOUNT_MIN) {
                            //不满足移除
                            //原表增加deleteHeight字段 查询时判断deleteHeight == 0
                            certifiedPeer = Conch.getPocProcessor().getCertifiedPeers().get(crowdMinerId);
                            if(certifiedPeer!=null){
                                if(certifiedPeer.getDeleteHeight() == 0) {
                                    certifiedPeer.setDeleteHeight(block.getHeight());
                                    PocDb.saveOrUpdatePeer(certifiedPeer);
                                }
                            }
                        }
                    }
                    if(crowdMinerId.equals(transaction.getRecipientId())){
                        CertifiedPeer certifiedPeer = Conch.getPocProcessor().getCertifiedPeers().get(crowdMinerId);
                        if(certifiedPeer!=null){
                            if(certifiedPeer.getDeleteHeight() != 0) {
                                //存在进行最新挖矿持仓量检查
                                long holdingAmount = 0;
                                try{
                                    if(Account.getAccount(crowdMinerId)!=null){
                                        holdingAmount = Account.getAccount(crowdMinerId).getEffectiveBalanceSS(block.getHeight());
                                    }
                                }catch(Exception e){
                                    Logger.logWarningMessage("[QualifiedMiner] not valid miner because can't get balance of account %s at height %d, caused by %s",  Account.getAccount(crowdMinerId).getRsAddress(), block.getHeight(), e.getMessage());
                                    holdingAmount = 0;
                                }

                                if(holdingAmount >= QUALIFIED_CROWD_MINER_HOLDING_AMOUNT_MIN) {
                                    certifiedPeer.setDeleteHeight(0);
                                    PocDb.saveOrUpdatePeer(certifiedPeer);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void popOffWithRescan(int height) {
        blockchain.writeLock();
        try {
            try {
                scheduleScan(0, false);
                BlockImpl lastBLock = BlockDb.deleteBlocksFrom(BlockDb.findBlockIdAtHeight(height));
                blockchain.setLastBlock(lastBLock);
                Logger.logDebugMessage("Deleted blocks starting from height %s", height);
            } finally {
                scan(0, false);
            }
        } finally {
            blockchain.writeUnlock();
        }
    }

    private int getBlockVersion(int previousBlockHeight) {
        // open testnet default version is 3, means poc txs opened
        return (previousBlockHeight < Constants.POC_BLOCK_HEIGHT) ? 2 : 3;
    }

    private int getTransactionVersion(int previousBlockHeight) {
        // tx version be set to 3 after v0.1.6, see: org.conch.tx.TransactionImpl.defaultTxVersion
        return previousBlockHeight < Constants.POC_BLOCK_HEIGHT ? 1 : 3;
    }

    public SortedSet<UnconfirmedTransaction> selectUnconfirmedTransactions(
            Map<TransactionType, Map<String, Integer>> duplicates,
            Block previousBlock,
            int blockTimestamp) {
        List<UnconfirmedTransaction> orderedUnconfirmedTransactions = new ArrayList<>();
        FilteringIterator<UnconfirmedTransaction> unconfirmedTransactions = null;
        try {
            unconfirmedTransactions =
                    new FilteringIterator<>(TransactionProcessorImpl.getInstance().getAllUnconfirmedTransactions(),
                            transaction -> hasAllReferencedTransactions(transaction.getTransaction(), transaction.getTimestamp(), 0));

            for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
                orderedUnconfirmedTransactions.add(unconfirmedTransaction);
            }

        } catch (Exception e) {
            TransactionProcessorImpl.getInstance().processDirtyOrViciousTx(e);
            throw e;
        } finally {
            DbUtils.close(unconfirmedTransactions);
        }

        SortedSet<UnconfirmedTransaction> sortedTransactions =
                new TreeSet<>(transactionArrivalComparator);
        int payloadLength = 0;
        Map<Long, Transaction> uploadTransactions = new HashMap<>();
        Map<Long, Map<String, Long>> backupNum = new HashMap<>();
        while (payloadLength <= Constants.MAX_PAYLOAD_LENGTH
                && sortedTransactions.size() <= Constants.MAX_NUMBER_OF_TRANSACTIONS) {
            int prevNumberOfNewTransactions = sortedTransactions.size();
            for (UnconfirmedTransaction unconfirmedTransaction : orderedUnconfirmedTransactions) {
                int transactionLength = unconfirmedTransaction.getTransaction().getFullSize();
                if (sortedTransactions.contains(unconfirmedTransaction)
                        || payloadLength + transactionLength > Constants.MAX_PAYLOAD_LENGTH) {
                    continue;
                }
                if (unconfirmedTransaction.getVersion() != getTransactionVersion(previousBlock.getHeight())) {
                    continue;
                }
                if (blockTimestamp > 0
                        && (unconfirmedTransaction.getTimestamp() > blockTimestamp + Constants.MAX_TIMEDRIFT
                        || unconfirmedTransaction.getExpiration() < blockTimestamp)) {
                    continue;
                }
                try {
                    unconfirmedTransaction.getTransaction().validate();
                } catch (ConchException.ValidationException e) {
                    continue;
                }
                if (unconfirmedTransaction.getTransaction().attachmentIsDuplicate(duplicates, true)) {
                    continue;
                }

                // storage confirm transaction order and backup transaction number
                if (unconfirmedTransaction.getTransaction().getType() == StorageTx.STORAGE_UPLOAD) {
                    uploadTransactions.put(unconfirmedTransaction.getId(), unconfirmedTransaction);
                }
                if (unconfirmedTransaction.getTransaction().getType() == StorageTx.STORAGE_BACKUP) {
                    if (!hasUploadTransaction(uploadTransactions, unconfirmedTransaction)
                            || hasBackuped(backupNum, unconfirmedTransaction.getTransaction())
                            || isBackupNumberExceed(uploadTransactions, backupNum, unconfirmedTransaction)) {
                        continue;
                    }
                }

                sortedTransactions.add(unconfirmedTransaction);
                payloadLength += transactionLength;
            }
            if (sortedTransactions.size() == prevNumberOfNewTransactions) {
                break;
            }
        }
        return sortedTransactions;
    }

    private boolean hasUploadTransaction(
            Map<Long, Transaction> uploadTransactions, Transaction transaction) {
        Attachment.DataStorageBackup dataStorageBackup =
                (Attachment.DataStorageBackup) transaction.getAttachment();
        Transaction storeTransaction =
                Conch.getBlockchain().getTransaction(dataStorageBackup.getUploadTransaction());
        if (!uploadTransactions.containsKey(dataStorageBackup.getUploadTransaction())
                && storeTransaction == null) {
            return false;
        }
        return true;
    }

    private boolean isBackupNumberExceed(
            Map<Long, Transaction> uploadTransactions,
            Map<Long, Map<String, Long>> backupNum,
            Transaction transaction) {
        Attachment.DataStorageBackup dataStorageBackup =
                (Attachment.DataStorageBackup) transaction.getAttachment();
        Transaction storeTransaction =
                Conch.getBlockchain().getTransaction(dataStorageBackup.getUploadTransaction());

        int replicated_number;
        if (storeTransaction != null) {
            replicated_number =
                    ((Attachment.DataStorageUpload) storeTransaction.getAttachment()).getReplicated_number();
        } else {
            storeTransaction = uploadTransactions.get(dataStorageBackup.getUploadTransaction());
            replicated_number =
                    ((Attachment.DataStorageUpload)
                            uploadTransactions.get(dataStorageBackup.getUploadTransaction()).getAttachment())
                            .getReplicated_number();
        }
        int num =
                replicated_number
                        - StorageBackup.getCurrentBackupNum(dataStorageBackup.getUploadTransaction());
        int backNum =
                backupNum.containsKey(storeTransaction.getId())
                        ? backupNum.get(storeTransaction.getId()).get("num").intValue()
                        : 0;
        if (num - backNum > 0) {
            if (backupNum.containsKey(storeTransaction.getId())) {
                Map<String, Long> info = backupNum.get(storeTransaction.getId());
                info.put("num", info.get("num") + 1);
                backupNum.put(storeTransaction.getId(), info);
            } else {
                Map<String, Long> info = new HashMap<>();
                info.put("num", new Long(1));
                info.put("backuper", transaction.getSenderId());
                backupNum.put(storeTransaction.getId(), info);
            }
            return false;
        }
        return true;
    }

    private boolean hasBackuped(Map<Long, Map<String, Long>> backupNum, Transaction backup) {
        Attachment.DataStorageBackup dataStorageBackup =
                (Attachment.DataStorageBackup) backup.getAttachment();
        return backupNum.containsKey(dataStorageBackup.getUploadTransaction())
                && backupNum.get(dataStorageBackup.getUploadTransaction()).get("backuper")
                == backup.getSenderId();
    }

    private static final Comparator<UnconfirmedTransaction> transactionArrivalComparator =
            Comparator.comparingLong(UnconfirmedTransaction::getArrivalTimestamp)
                    .thenComparingInt(UnconfirmedTransaction::getHeight)
                    .thenComparingLong(UnconfirmedTransaction::getId);

    public void generateBlock(String secretPhrase, int blockTimestamp) throws BlockNotAcceptedException, GeneratorNotAcceptedException {

        Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        if (blockchain.getHeight() >= Constants.PHASING_BLOCK_HEIGHT) {
            DbIterator<TransactionImpl> phasedTransactions = null;
            try {
                phasedTransactions = PhasingPoll.getFinishingTransactions(blockchain.getHeight() + 1);
                for (TransactionImpl phasedTransaction : phasedTransactions) {
                    try {
                        phasedTransaction.validate();
                        // pre-populate duplicates map
                        phasedTransaction.attachmentIsDuplicate(duplicates, false);
                    } catch (ConchException.ValidationException ignore) {

                    }
                }
            } finally {
                DbUtils.close(phasedTransactions);
            }
        }

        BlockImpl previousBlock = blockchain.getLastBlock();
        TransactionProcessorImpl.getInstance().processWaitingTransactions();
        SortedSet<UnconfirmedTransaction> sortedTransactions = selectUnconfirmedTransactions(duplicates, previousBlock, blockTimestamp);
        List<TransactionImpl> blockTransactions = new ArrayList<>();
        MessageDigest digest = Crypto.sha256();
        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        Account creator = Account.getAccount(publicKey);

        long totalAmountNQT = 0;
        long totalFeeNQT = 0;
        int payloadLength = 0;

        // coin base
        TransactionImpl coinBaseTx = null;
        try {
            // transaction version=1, deadline=10,timestamp=blockTimestamp
            coinBaseTx = Constants.rewardCalculatorInstance.generateCoinBaseTxBuilder(publicKey, Conch.getHeight())
                    .timestamp(blockTimestamp)
                    .recipientId(0)
                    .build(secretPhrase);
            sortedTransactions.add(new UnconfirmedTransaction(coinBaseTx, System.currentTimeMillis()));
        } catch (ConchException.NotValidException e) {
            Logger.logErrorMessage("Can't create coin base tx[current miner=" + creator.getRsAddress() + ", id=" + creator.getId() + "]", e);
        }

        // generate missing tx
        try {
            if (Generator.hasGenerationMissingAccount()) {
                TransactionImpl transaction =
                        new TransactionImpl.BuilderImpl(
                                publicKey,
                                0,
                                0,
                                (short) 10,
                                new PocTxBody.PocGenerationMissing(Generator.getAndResetMissingMiners()))
                                .timestamp(blockTimestamp)
                                .recipientId(0)
                                .build(secretPhrase);
                sortedTransactions.add(new UnconfirmedTransaction(transaction, System.currentTimeMillis()));
            }
        } catch (ConchException.NotValidException e) {
            long accountId = Account.getId(publicKey);
            Logger.logErrorMessage("Can't create generation missing transaction[current miner=" + Account.rsAccount(accountId) + ", id=" + accountId + "]", e);
        }

        List<TransactionImpl> tempBlockTransactions = new ArrayList<>();
        List<UnconfirmedTransaction> tempErrorTransactions = new ArrayList<>();
        for (UnconfirmedTransaction unconfirmedTransaction : sortedTransactions) {
            TransactionImpl transaction = unconfirmedTransaction.getTransaction();

            if (transaction.getSenderId() == Constants.BURN_ADDRESS_ID) {
                TransactionProcessorImpl.getInstance().removeUnconfirmedTransaction(transaction);
                tempErrorTransactions.add(unconfirmedTransaction);
            }

            if (transaction.getAttachment().getTransactionType().getType() == TYPE_SHARDER_POOL) {//确认矿池交易
                if (transaction.getAttachment().getJSONObject().get("version.destroyPool") != null) {//判断是否有销毁交易
                    tempBlockTransactions.add(transaction);//将销毁交易加入临时列表
                }
            }
        }
        sortedTransactions.removeAll(tempErrorTransactions);

        for (UnconfirmedTransaction unconfirmedTransaction : sortedTransactions) {
            boolean isJoinDestroyPool = false;
            TransactionImpl transaction = unconfirmedTransaction.getTransaction();
            try {
                if (transaction.getAttachment().getTransactionType().getType() == TYPE_SHARDER_POOL) {//确认矿池交易
                    for (TransactionImpl tr : tempBlockTransactions) {//循环销毁交易临时列表
                        if (transaction.getAttachment().getJSONObject().get("poolId").equals(tr.getAttachment().getJSONObject().get("poolId"))) {//匹配销毁交易
                            SharderPoolProcessor pool = SharderPoolProcessor.getPool((long) transaction.getAttachment().getJSONObject().get("poolId"));//确认销毁交易的pool
                            //判断是否是加入交易
                            if (transaction.getAttachment().getJSONObject().get("version.joinPool") != null || (transaction.getAttachment().getJSONObject().get("version.quitPool") != null && transaction.getSenderId() != pool.getCreatorId())) {
                                TransactionProcessorImpl.getInstance().removeUnconfirmedTransaction(transaction);
                                isJoinDestroyPool = true;
                                break;
                            }

                        }
                    }
                }
            } catch (Exception e) {
                Logger.logErrorMessage("Don't have destroyPool Tx", e);
            } finally {
                if (isJoinDestroyPool) {
                    continue;
                }
                blockTransactions.add(transaction);
                digest.update(transaction.bytes());
                totalAmountNQT += transaction.getAmountNQT();
                if (!StorageTxProcessorImpl.getInstance().isStorageUploadTransaction(transaction)) {
                    totalFeeNQT += transaction.getFeeNQT();
                } else {
                    totalFeeNQT += transaction.getMinimumFeeNQT(blockchain.getHeight());
                }
                payloadLength += transaction.getFullSize();
            }
        }


        try {
            // burn tx
            long burnNQT = BurnCalculator.burnAmount(totalFeeNQT);
            if (burnNQT > 0) {
                TransactionImpl transaction =
                        new TransactionImpl.BuilderImpl(
                                publicKey,
                                burnNQT,
                                0,
                                (short) 10,
                                new Attachment.BurnDeal(Constants.BURN_ADDRESS_ID))
                                .timestamp(blockTimestamp)
                                .recipientId(Constants.BURN_ADDRESS_ID)
                                .build(secretPhrase);
                blockTransactions.add(transaction);
                digest.update(transaction.bytes());
                totalAmountNQT += transaction.getAmountNQT();
                payloadLength += transaction.getFullSize();
                Logger.logDebugMessage("create burn transaction: burn " + burnNQT + " SS");
            }
        } catch (ConchException.ValidationException e) {
            e.printStackTrace();
        }

        byte[] payloadHash = digest.digest();
        digest.update(previousBlock.getGenerationSignature());
        byte[] generationSignature = digest.digest(publicKey);
        byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.bytes());

        BlockImpl block = new BlockImpl(
                        getBlockVersion(previousBlock.getHeight()),
                        blockTimestamp,
                        previousBlock.getId(),
                        totalAmountNQT,
                        totalFeeNQT,
                        payloadLength,
                        payloadHash,
                        publicKey,
                        generationSignature,
                        previousBlockHash,
                        blockTransactions,
                        secretPhrase);

        try {
            pushBlock(block);
            blockListeners.notify(block, Event.BLOCK_GENERATED);
            PocScore generatorScore = Conch.getPocProcessor().calPocScore(creator, previousBlock.getHeight());
            Logger.logInfoMessage("[Mint-%d] Miner[id=%d, RS=%s, PoC=%d] generated block[id=%d, timestamp=%s, crowd miner size=%d] at height %d fee %f",
                    block.getHeight(), creator.getId(), creator.getRsAddress(), generatorScore.total(),
                    block.getId(), Convert.dateFromEpochTime(block.getTimestamp()), RewardCalculator.crowdMinerCount(coinBaseTx.getAttachment()),
                    block.getHeight(), (float) block.getTotalFeeNQT() / Constants.ONE_SS);
            Peers.checkAndUpdateBlockchainState(null);
        } catch (TransactionNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
            TransactionProcessorImpl.getInstance().processWaitingTransactions();
            TransactionImpl transaction = e.getTransaction();
            Logger.logDebugMessage("Removing invalid transaction: " + transaction.getStringId());
            blockchain.writeLock();
            try {
                TransactionProcessorImpl.getInstance().removeUnconfirmedTransaction(transaction);
            } finally {
                blockchain.writeUnlock();
            }
            throw e;
        } catch (BlockNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
            throw e;
        } catch (GeneratorNotAcceptedException e) {
            Generator.blackGenerator(e.getGeneratorId());
            throw e;
        } catch (ConchException.StopException e) {
            throw e;
        }
    }

    public boolean hasAllReferencedTransactions(
            TransactionImpl transaction, int timestamp, int count) {
        if (transaction.referencedTransactionFullHash() == null) {
            return timestamp - transaction.getTimestamp() < Constants.MAX_REFERENCED_TRANSACTION_TIMESPAN
                    && count < 10;
        }
        TransactionImpl referencedTransaction =
                TransactionDb.findTransactionByFullHash(transaction.referencedTransactionFullHash());
        return referencedTransaction != null
                && referencedTransaction.getHeight() < transaction.getHeight()
                && hasAllReferencedTransactions(referencedTransaction, timestamp, count + 1);
    }

    public void scheduleScan(int height, boolean validate) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE scan SET rescan = TRUE, height = ?, validate = ?")) {
            pstmt.setInt(1, height);
            pstmt.setBoolean(2, validate);
            pstmt.executeUpdate();
            Logger.logDebugMessage(
                    "Scheduled scan starting from height " + height + (validate ? ", with validation" : ""));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void scan(int height, boolean validate) {
        try {
            scan(height, validate, false);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ConchException.NotValidException e) {
            e.printStackTrace();
        } catch (BlockNotAcceptedException e) {
            e.printStackTrace();
        } catch (GeneratorNotAcceptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fullScanWithShutdown() {
        try {
            scan(0, true, true);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ConchException.NotValidException e) {
            e.printStackTrace();
        } catch (BlockNotAcceptedException e) {
            e.printStackTrace();
        } catch (GeneratorNotAcceptedException e) {
            e.printStackTrace();
        }
    }

    private void scan(int height, boolean validate, boolean shutdown) throws SQLException, ConchException.NotValidException, BlockNotAcceptedException, GeneratorNotAcceptedException, ConchException.StopException {
        blockchain.writeLock();
        try {
            if (!Db.db.isInTransaction()) {
                try {
                    Db.db.beginTransaction();
                    if (validate) {
                        blockListeners.addListener(CheckSumValidator.eventProcessor(), Event.BLOCK_SCANNED);
                    }
                    scan(height, validate, shutdown);
                    Db.db.commitTransaction();
                } catch (Exception e) {
                    Db.db.rollbackTransaction();
                    throw e;
                } finally {
                    Db.db.endTransaction();
                    blockListeners.removeListener(CheckSumValidator.eventProcessor(), Event.BLOCK_SCANNED);
                }
                return;
            }
            scheduleScan(height, validate);
            if (height > 0 && height < getMinRollbackHeight()) {
                Logger.logMessage(
                        "Rollback to height less than "
                                + getMinRollbackHeight()
                                + " not supported, will do a full scan");
                height = 0;
            }
            if (height < 0) {
                height = 0;
            }
            Logger.logMessage("Scanning blockchain starting from height " + height + "...");
            if (validate) {
                Logger.logDebugMessage("Also verifying signatures and validating transactions...");
            }
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmtSelect =
                         con.prepareStatement(
                                 "SELECT * FROM block WHERE "
                                         + (height > 0 ? "height >= ? AND " : "")
                                         + " db_id >= ? ORDER BY db_id ASC LIMIT 50000");
                 PreparedStatement pstmtDone =
                         con.prepareStatement(
                                 "UPDATE scan SET rescan = FALSE, height = 0, validate = FALSE")) {
                isScanning = true;
                initialScanHeight = blockchain.getHeight();
                if (height > blockchain.getHeight() + 1) {
                    Logger.logMessage(
                            "Rollback height "
                                    + (height - 1)
                                    + " exceeds current blockchain height of "
                                    + blockchain.getHeight()
                                    + ", no scan needed");
                    pstmtDone.executeUpdate();
                    Db.db.commitTransaction();
                    return;
                }
                if (height == 0) {
                    Logger.logDebugMessage("Dropping all full text search indexes");
                    FullTextTrigger.dropAll(con);
                }
                for (DerivedDbTable table : derivedTables) {
                    if (height == 0) {
                        table.truncate();
                    } else {
                        table.rollback(height - 1);
                    }
                }
                Db.db.clearCache();
                Db.db.commitTransaction();
                Logger.logDebugMessage("Rolled back derived tables");
                BlockImpl currentBlock = BlockDb.findBlockAtHeight(height);
                blockListeners.notify(currentBlock, Event.RESCAN_BEGIN);
                long currentBlockId = currentBlock.getId();
                if (height == 0) {
                    blockchain.setLastBlock(currentBlock); // special case to avoid no last block
                    SharderGenesis.enableGenesisAccount();
                } else {
                    blockchain.setLastBlock(BlockDb.findBlockAtHeight(height - 1));
                }
                if (shutdown) {
                    Logger.logMessage("Scan will be performed at next start");
                    new Thread(() -> System.exit(0)).start();
                    return;
                }
                int pstmtSelectIndex = 1;
                if (height > 0) {
                    pstmtSelect.setInt(pstmtSelectIndex++, height);
                }
                long dbId = Long.MIN_VALUE;
                boolean hasMore = true;
                outer:
                while (hasMore) {
                    hasMore = false;
                    pstmtSelect.setLong(pstmtSelectIndex, dbId);
                    try (ResultSet rs = pstmtSelect.executeQuery()) {
                        while (rs.next()) {
                            try {
                                dbId = rs.getLong("db_id");
                                currentBlock = BlockDb.loadBlock(con, rs, true);
                                currentBlock.loadTransactions();
                                if (currentBlock.getId() != currentBlockId || currentBlock.getHeight() > blockchain.getHeight() + 1) {
                                    throw new ConchException.NotValidException("Database blocks in the wrong order!");
                                }
                                Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
                                List<TransactionImpl> validPhasedTransactions = new ArrayList<>();
                                List<TransactionImpl> invalidPhasedTransactions = new ArrayList<>();
                                validatePhasedTransactions(blockchain.getHeight(), validPhasedTransactions, invalidPhasedTransactions, duplicates);

                                if (validate && currentBlockId != SharderGenesis.GENESIS_BLOCK_ID) {
                                    int curTime = Conch.getEpochTime();
                                    validate(currentBlock, blockchain.getLastBlock(), curTime);
                                    byte[] blockBytes = currentBlock.bytes();
                                    JSONObject blockJSON = (JSONObject) JSONValue.parse(currentBlock.getJSONObject().toJSONString());
                                    if (!Arrays.equals(blockBytes, BlockImpl.parseBlock(blockJSON).bytes())) {
                                        throw new ConchException.NotValidException("Block JSON cannot be parsed back to the same block");
                                    }
                                    validateTransactions(currentBlock, blockchain.getLastBlock(), curTime, duplicates);
                                    for (TransactionImpl transaction : currentBlock.getTransactions()) {
                                        byte[] transactionBytes = transaction.bytes();
                                        if (!Arrays.equals(transactionBytes, TransactionImpl.newTransactionBuilder(transactionBytes).build().bytes())) {
                                            throw new ConchException.NotValidException(
                                                    "Transaction bytes cannot be parsed back to the same transaction: " + transaction.toPrintString());
                                        }
                                        JSONObject transactionJSON = (JSONObject) JSONValue.parse(transaction.getJSONObject().toJSONString());
                                        if (!Arrays.equals(transactionBytes, TransactionImpl.newTransactionBuilder(transactionJSON).build().bytes())) {
                                            throw new ConchException.NotValidException("Transaction JSON cannot be parsed back to the same transaction: " + transaction.toPrintString());
                                        }
                                    }
                                }

                                blockListeners.notify(currentBlock, Event.BEFORE_BLOCK_ACCEPT);
                                blockchain.setLastBlock(currentBlock);
                                accept(currentBlock, validPhasedTransactions, invalidPhasedTransactions, duplicates);
                                currentBlockId = currentBlock.getNextBlockId();
                                Db.db.clearCache();
                                Db.db.commitTransaction();
                                blockListeners.notify(currentBlock, Event.AFTER_BLOCK_ACCEPT);
                            } catch (ConchException | RuntimeException e) {
                                Db.db.rollbackTransaction();
                                Logger.logDebugMessage(e.toString(), e);
                                Logger.logDebugMessage("Applying block " + Long.toUnsignedString(currentBlockId) + " at height "
                                        + (currentBlock == null ? 0 : currentBlock.getHeight()) + " failed, deleting from database");
                                if (e instanceof ConchException.StopException) {
                                    throw e;
                                }
                                BlockImpl lastBlock = BlockDb.deleteBlocksFrom(currentBlockId);
                                blockchain.setLastBlock(lastBlock);
                                popOffTo(lastBlock);
                                break outer;
                            }
                            blockListeners.notify(currentBlock, Event.BLOCK_SCANNED);
                            hasMore = true;
                        }
                        dbId = dbId + 1;
                    }
                }
                if (height == 0) {
                    for (DerivedDbTable table : derivedTables) {
                        table.createSearchIndex(con);
                    }
                }
                pstmtDone.executeUpdate();
                Db.db.commitTransaction();
                blockListeners.notify(currentBlock, Event.RESCAN_END);
                Logger.logMessage("...done at height " + blockchain.getHeight());
                if (height == 0 && validate) {
                    Logger.logMessage("SUCCESSFULLY PERFORMED FULL RESCAN WITH VALIDATION");
                }
                lastRestoreTime = 0;
            } catch (SQLException | ConchException.NotValidException | BlockNotAcceptedException | GeneratorNotAcceptedException | ConchException.StopException e) {
                if (e instanceof ConchException.StopException) {
                    throw e;
                }
                throw new RuntimeException(e.toString(), e);
            } finally {
                isScanning = false;
            }
        } finally {
            blockchain.writeUnlock();
        }
    }
}
