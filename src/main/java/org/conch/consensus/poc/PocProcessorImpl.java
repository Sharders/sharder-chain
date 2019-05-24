package org.conch.consensus.poc;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.*;
import org.conch.common.Constants;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionType;
import org.conch.util.DiskStorageUtil;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {

    /**
     * !! Don't use this instance directly,
     * please call org.conch.Conch#getPocProcessor() to get instance
     **/
    public static PocProcessorImpl instance = getOrCreate();

    // execute once when restart the cos application
    private static boolean oldPocTxsProcess = false;

    private static final int peerSynThreadInterval = 600;
    private static final int pocTxSynThreadInterval = 60;

    private static final String LOCAL_STORAGE_POC_HOLDER = "StoredPocHolder";
    private static final String LOCAL_STORAGE_POC_CALCULATOR = "StoredPocCalculator";

    // height : { accountId : account }
    private static Map<Integer, Map<Long, Account>> balanceChangedMap = Maps.newConcurrentMap();


    private PocProcessorImpl() {
    }

    private static synchronized PocProcessorImpl getOrCreate() {
        return instance != null ? instance : new PocProcessorImpl();
    }

    static {
        // new block accepted
        Conch.getBlockchainProcessor().addListener((Block block) -> {
            // ss hold score re-calculate
            // remark: the potential logic is: received Account.Event.BALANCE firstly, then received Event.AFTER_BLOCK_ACCEPT
            boolean someAccountBalanceChanged = balanceChangedMap.containsKey(block.getHeight()) && balanceChangedMap.get(block.getHeight()).size() > 0;
            if (someAccountBalanceChanged) {
                for (Account account : balanceChangedMap.get(block.getHeight()).values()) {
                    balanceChangedProcess(block.getHeight(), account);
                }
                balanceChangedMap.get(block.getHeight()).clear();
            }

//      Boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
//      boolean blockContainPocTxs = containPoc == null ? false : containPoc;
//
//      //save to disk when poc score changed case of contains poc txs in block or account balance changed
//      if(someAccountBalanceChanged || blockContainPocTxs) {
//        //save the poc holder and calculator to disk
//        instance.saveToDisk();
//      }
        }, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);

        // balance changed event
        Account.addListener((Account account) -> {
            putInBalanceChangedAccount(Conch.getBlockchain().getHeight(), account, Account.Event.BALANCE);
        }, Account.Event.BALANCE);

        // unconfirmed balance changed event
        Account.addListener((Account account) -> {
            putInBalanceChangedAccount(Conch.getBlockchain().getHeight(), account, Account.Event.UNCONFIRMED_BALANCE);
        }, Account.Event.UNCONFIRMED_BALANCE);

        instance.loadFromDisk();
    }

    static void putInBalanceChangedAccount(int height, Account account, Account.Event event) {
        if (account == null || account.getId() == -1 || event == null) return;
        long accountId = account.getId();

        // check current height when event is BALANCE changed
        if (Account.Event.BALANCE == event) {
            if (!balanceChangedMap.containsKey(height)) {
                balanceChangedMap.put(height, Maps.newHashMap());
            }

            if (!balanceChangedMap.get(height).containsKey(accountId)) {
                balanceChangedMap.get(height).put(accountId, account);
            }
        }

        // check future confirmed height
        int confirmedHeight = height + Constants.GUARANTEED_BALANCE_CONFIRMATIONS;
        if (!balanceChangedMap.containsKey(confirmedHeight)) {
            balanceChangedMap.put(confirmedHeight, Maps.newHashMap());
        }

        if (!balanceChangedMap.get(confirmedHeight).containsKey(accountId)) {
            balanceChangedMap.get(confirmedHeight).put(accountId, account);
        }
    }

    /**
     * PoC tx process
     *
     * @param tx poc tx
     * @return
     */
    @Override
    public boolean pocTxProcess(Transaction tx) {
        if (TransactionType.TYPE_POC != tx.getType().getType()) {
            return true;
        }

        boolean success = false;
        if (PocTxWrapper.SUBTYPE_POC_WEIGHT_TABLE == tx.getType().getSubtype()) {
            PocTxBody.PocWeightTable weightTable = (PocTxBody.PocWeightTable) tx.getAttachment();
            PocCalculator.inst.setCurWeightTable(weightTable, tx.getHeight());
            success = true;
        } else {
            if (Conch.reachLastKnownBlock()) {
                if (PocTxWrapper.SUBTYPE_POC_NODE_TYPE == tx.getType().getSubtype()) {
                    success = nodeTypeTxProcess(tx.getHeight(), tx);
                } else if (PocTxWrapper.SUBTYPE_POC_NODE_CONF == tx.getType().getSubtype()) {
                    success = nodeConfTxProcess(tx.getHeight(), (PocTxBody.PocNodeConf) tx.getAttachment());
                } else if (PocTxWrapper.SUBTYPE_POC_ONLINE_RATE == tx.getType().getSubtype()) {
                    success = onlineRateTxProcess(tx.getHeight(), (PocTxBody.PocOnlineRate) tx.getAttachment());
                } else if (PocTxWrapper.SUBTYPE_POC_BLOCK_MISSING == tx.getType().getSubtype()) {
                    success = blockMissingTxProcess(tx.getHeight(), (PocTxBody.PocGenerationMissing) tx.getAttachment());
                }
            }
        }

        // process later
        if (!success) {
            PocHolder.addDelayPocTx(tx);
        }
        return success;
    }

    @Override
    public PocScore calPocScore(Account account, int height) {
        PocScore pocScore = PocHolder.getPocScore(height, account.getId());

        //[POLYFILL] polyfill for pre hubs in Testnet which PocNodeType is missing the accountId attribute make this bug 
        PocTxBody.PocNodeTypeV2 hubNodeType = CheckSumValidator.isPreAccountsInTestnet(account.getId(), height);
        if (hubNodeType != null) pocScore.nodeTypeCal(hubNodeType);

        return pocScore;
    }

    @Override
    public PocTxBody.PocWeightTable getPocWeightTable(Long version) {
        return PocHolder.getPocWeightTable();
    }

    @Override
    public void notifySynTxNow() {
        oldPocTxsProcess = true;
    }

//    @Override
//    public void updateBoundPeer(String host, long accountId) {
//        if (StringUtils.isEmpty(host) || accountId == 0) return;
//        PocHolder.updateBoundPeer(host, accountId);
//    }

    /**
     * account whether bound to certified peer
     *
     * @param accountId
     * @return
     */
    @Override
    public boolean isCertifiedPeerBind(long accountId, int height) {
        boolean hubBindAccount = PocHolder.isBoundPeer(Peer.Type.HUB, accountId);
        boolean communityBindAccount = PocHolder.isBoundPeer(Peer.Type.COMMUNITY, accountId);
        boolean foundationBindAccount = PocHolder.isBoundPeer(Peer.Type.FOUNDATION, accountId);
        boolean isGenesisAccount = SharderGenesis.isGenesisCreator(accountId) || SharderGenesis.isGenesisRecipients(accountId);

        // height for certified peers
        return hubBindAccount || communityBindAccount || foundationBindAccount || isGenesisAccount;
    }

    /**
     * check whether a account bounded to a certified peer
     *
     * @param accountId
     * @return
     */
    @Override
    public CertifiedPeer getBoundedPeer(long accountId, int height) {
        return PocHolder.getBoundPeer(accountId, height);
    }

    @Override
    public boolean resetCertifiedPeers() {
        return PocHolder.resetCertifiedPeers();
    }

    @Override
    public boolean pocTxsProcessed(int height) {
        // poc isn't processed: whether contains delayed poc txs or old poc txs need to process
        return !oldPocTxsProcess && PocHolder.countDelayPocTxs(height) <= 0;
    }

    @Override
    public boolean processDelayedPocTxs(int height) {

        if (!Conch.reachLastKnownBlock()) return false;

        // delayed poc txs 
        List<Long> delayedPocTxs = PocHolder.delayPocTxs(height);
        Logger.logDebugMessage("process delayed poc txs[size=%d]", delayedPocTxs.size());
        Set<Long> processedTxs = Sets.newHashSet();
        delayedPocTxs.forEach(txid -> {
            if (instance.pocTxProcess(txid)) {
                processedTxs.add(txid);
            }
        });

        // remove processed txs
        if (processedTxs.size() > 0) {
            Logger.logInfoMessage("success to process delayed poc txs[processed size=%d, wish size=%d]", processedTxs.size(), delayedPocTxs.size());
            Logger.logDebugMessage("processed poc txs detail => " + Arrays.toString(processedTxs.toArray()));
            PocHolder.removeProcessedTxs(processedTxs);
        } else if (processedTxs.size() <= 0 && delayedPocTxs.size() > 0) {
            Logger.logWarningMessage("[WARN] delayed poc txs process failed, wish to process %d poc txs %s", delayedPocTxs.size(), Arrays.toString(delayedPocTxs.toArray()));
        }
        return PocHolder.countDelayPocTxs(height) <= 0;
    }

    @Override
    public boolean removeDelayedPocTxs(Set<Long> txIds) {
        PocHolder.removeProcessedTxs(txIds);
        return true;
    }

//  /**
//   * save the poc holder and calculator to disk,
//   * If be called outside, the caller should be org.conch.Conch#shutdown()
//   */
//  @Override
//  public void saveToDisk() {
//    DiskStorageUtil.saveObjToFile(PocHolder.inst, LOCAL_STORAGE_POC_HOLDER);
//    DiskStorageUtil.saveObjToFile(PocCalculator.inst, LOCAL_STORAGE_POC_CALCULATOR);
//  }

    /**
     * load the poc holder backup from local disk
     */
    private void loadFromDisk() {
        // read the disk backup
        Logger.logInfoMessage("load exist poc holder instance from local disk[" + DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_POC_HOLDER) + "]");
        Object holderObj = DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_POC_HOLDER);
        if (holderObj != null) {
            PocHolder.inst = (PocHolder) holderObj;
        } else {
            PocHolder.inst.lastHeight = -1;
        }

        Logger.logInfoMessage("load exist poc calculator instance from local disk[" + DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_POC_CALCULATOR) + "]");
        Object calcObj = DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_POC_CALCULATOR);
        if (calcObj != null) {
            PocCalculator.inst = (PocCalculator) calcObj;
        }

        //if no disk backup, read the poc txs from history blocks
        if (PocHolder.inst != null && PocHolder.inst.lastHeight <= Conch.getBlockchain().getHeight()) {
            oldPocTxsProcess = true;
        }
    }


    public static void init() {
        ThreadPool.scheduleThread("PocTxSynThread", pocTxSynThread, pocTxSynThreadInterval, TimeUnit.SECONDS);
        ThreadPool.scheduleThread("PeerSynThread", peerSynThread, peerSynThreadInterval, TimeUnit.SECONDS);
    }


    private static final Runnable pocTxSynThread = () -> {
        try {
//      if(!Conch.getBlockchainProcessor().isUpToDate()) {
//        Logger.logDebugMessage("block chain state isn't UP_TO_DATE, don't process delayed poc txs till blocks sync finished...");
//        return;
//      }
            int currentHeight = Conch.getBlockchain().getHeight();
            if (instance.processDelayedPocTxs(currentHeight) && !oldPocTxsProcess) {
                Logger.logDebugMessage("no needs to syn and process poc serial txs now, sleep %d seconds...", pocTxSynThreadInterval);
                return;
            }

            if (!Conch.reachLastKnownBlock()) {
                return;
            }

            try {
                instance.processDelayedPocTxs(currentHeight);
            } catch (Exception e) {
                Logger.logErrorMessage("Process delayed poc txs failed caused by [%s]", e.getMessage());
            }

            if (oldPocTxsProcess) {
                // total poc txs from last height
                int fromHeight = (PocHolder.inst.lastHeight <= -1) ? 0 : PocHolder.inst.lastHeight;
//        int fromHeight = 0;
                int toHeight = BlockchainImpl.getInstance().getHeight();
                Logger.logInfoMessage("process old poc txs from %d to %d ...", fromHeight, toHeight);
                DbIterator<BlockImpl> blocks = null;
                try {
                    blocks = BlockchainImpl.getInstance().getBlocks(fromHeight, toHeight);
                    int count = 0;
                    for (BlockImpl block : blocks) {
                        count += instance.pocSeriesTxProcess(block);
                    }
                    Logger.logInfoMessage("old poc txs processed[from %d to %d] [processed size=%d]", fromHeight, toHeight, count);
                    oldPocTxsProcess = false;
                } finally {
                    DbUtils.close(blocks);
                }
            }

        } catch (Exception e) {
            Logger.logErrorMessage("poc tx syn thread interrupted caused by %s", e.getMessage());
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }
    };

    private static final Runnable peerSynThread = () -> {
        try {

            if (PocHolder.synPeers().size() <= 0) {
                Logger.logInfoMessage("no needs to syn peer, sleep %d seconds...", peerSynThreadInterval);
            }

            Set<String> connectedPeers = Sets.newHashSet();
            for (String peerAddress : PocHolder.synPeers()) {
                try {
                    Peer peer = Peers.findOrCreatePeer(peerAddress, Peers.isUseNATService(peerAddress), true);
                    if (peer != null) {
                        Peers.addPeer(peer, peerAddress);
                        Peers.connectPeer(peer);
                    }
                    peer = Peers.getPeer(peerAddress, true);
//          _updateCertifiedNodes(peer.getHost(), peer.getType(), -1);
                    connectedPeers.add(peer.getHost());
                } catch (Exception e) {
                    if (Logger.printNow(PocProcessorImpl.class.getName(), 200)) {
                        Logger.logDebugMessage("can't connect peer[%s] in peerSynThread, caused by %s", peerAddress, e.getMessage());
                    }
                    continue;
                }
            }

            if (connectedPeers.size() > 0) {
                PocHolder.removeConnectedPeers(connectedPeers);
                DiskStorageUtil.saveObjToFile(PocHolder.inst, LOCAL_STORAGE_POC_HOLDER);
            }

        } catch (Exception e) {
            Logger.logErrorMessage("peer syn thread interrupted caused by %s", e.getMessage());
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }
    };

    /**

     */
    /**
     * process poc txs of block
     *
     * @param block block
     * @return processed count
     */
    private int pocSeriesTxProcess(Block block) {
        int count = 0;
        //@link: org.conch.chain.BlockchainProcessorImpl.autoExtensionAppend update the ext tag
        List<? extends Transaction> txs = block.getTransactions();
        Boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
        if (txs == null || txs.size() <= 0 || containPoc == null || !containPoc) {
            return count;
        }

        //just process poc tx
        for (Transaction tx : txs) {
            if (pocTxProcess(tx)) count++;
        }

        return count;
    }

    /**
     * PoC tx process
     *
     * @param txid poc tx id
     * @return
     */
    private boolean pocTxProcess(Long txid) {
        Transaction tx = Conch.getBlockchain().getTransaction(txid);
        if (tx == null) return false;
        return pocTxProcess(tx);
    }


//    private static void _updateCertifiedNodes(String host, Peer.Type type, int height) {
//        if (StringUtils.isEmpty(host)) {
//            Logger.logWarningMessage("peer host[" + host + "] is null, can't find peer!");
//            return;
//        }
//
//        Logger.logDebugMessage("update certified peer host=%s, type=%s, height=%d", host, type.getName(), height);
//
//        // local node
//        String localRS = Generator.getAutoMiningRS();
//        if (Conch.matchMyAddress(host)) {
//            if (StringUtils.isNotEmpty(localRS)) {
//                Logger.logWarningMessage("current node[%s] is expected peer, update local miner account", host, localRS);
//                PocHolder.addCertifiedPeer(height, type, host, Account.rsAccountToId(localRS));
//            } else {
//                PocHolder.addSynPeer(host);
//                Logger.logWarningMessage("local linked rs account of peer[host=" + host + "] is null, need syn peer and updated later in Peers.GetCertifiedPeer thread");
//            }
//            return;
//        }
//
//        // connected nodes
//        Peer peer = Peers.getPeer(host, true);
//        peer.setType(type);
//        if (StringUtils.isEmpty(peer.getBindRsAccount())) {
//            // connect peer to get account later
//            PocHolder.addSynPeer(host);
//            Logger.logWarningMessage("linked rs account of peer[host=" + host + "] is null, need syn peer and updated later in Peers.GetCertifiedPeer thread");
//        }
//
//        PocHolder.addCertifiedPeer(height, type, host, );
//        
//        // update certified nodes
//        PocHolder.addCertifiedPeer(height, peer);
//    }



//    private static PocScore getPocScoreByPeer(int height, String host) {
//        String rs = null;
//
//        // local node
//        if (Conch.matchMyAddress(host)) {
//            String localRS = Generator.getAutoMiningRS();
//            if (StringUtils.isNotEmpty(localRS)) {
//                rs = localRS;
//            } else {
//                PocHolder.addSynPeer(host);
//                return null;
//            }
//        } else {
//            Peer peer = Peers.getPeer(host, true);
//            if (peer == null || StringUtils.isEmpty(peer.getBindRsAccount())) {
//                PocHolder.addSynPeer(host);
//                return null;
//            } else {
//                rs = peer.getBindRsAccount();
//            }
//        }
//
//        return new PocScore(Account.rsAccountToId(rs), height);
//    }


    /**
     * process the node type tx of poc series
     *
     * @param height      block height that included this tx
     * @param tx          transaction
     * @return
     */
    private static boolean nodeTypeTxProcess(int height, Transaction tx) {
        if (tx == null)  return false;
//        boolean isRightTx = (tx instanceof PocTxBody.PocNodeType) && height < Constants.POC_NODETYPE_V2_HEIGHT
//                            || (tx instanceof PocTxBody.PocNodeTypeV2) && height >= Constants.POC_NODETYPE_V2_HEIGHT;
//        if(!isRightTx) return false;

        PocTxBody.PocNodeTypeV2 nodeTypeV2 = null;
        Attachment attachment = tx.getAttachment();
        if(attachment instanceof PocTxBody.PocNodeType) {
            PocTxBody.PocNodeType nodeType = (PocTxBody.PocNodeType) attachment;
            nodeTypeV2 = CheckSumValidator.isPreAccountsInTestnet(nodeType.getIp(), height);
        }else if(attachment instanceof PocTxBody.PocNodeTypeV2){
            nodeTypeV2 = (PocTxBody.PocNodeTypeV2) attachment;
        }
        if(nodeTypeV2 == null) return false;
        long accountId = nodeTypeV2.getAccountId();
        
//        //TODO check current account linked status
//        PocScore pocScoreToUpdate = getPocScoreByPeer(height, nodeTypeV2.getIp());
//        if (pocScoreToUpdate == null) {
//            return false;
//        }
//        pocScoreToUpdate.nodeTypeCal(nodeTypeV2);

        PocScore pocScoreToUpdate = PocHolder.getPocScore(height, accountId);
        pocScoreToUpdate.nodeTypeCal(nodeTypeV2).saveOrUpdate();
//        PocHolder.scoreMappingAndPersist(pocScoreToUpdate);

        PocHolder.addCertifiedPeer(height, nodeTypeV2.getType(), nodeTypeV2.getIp(), accountId);
        return true;
    }


    /**
     * process the node conf tx of poc series
     *
     * @param height      block height that included this tx
     * @param pocNodeConf PocNodeConf tx
     * @return
     */
    private static boolean nodeConfTxProcess(int height, PocTxBody.PocNodeConf pocNodeConf) {
        CertifiedPeer certifiedPeer = PocHolder.getBoundPeer(pocNodeConf.getHost(), height);
        if(certifiedPeer == null) return false;

        PocScore pocScoreToUpdate = PocHolder.getPocScore(height, certifiedPeer.getBoundAccountId());

        pocScoreToUpdate.nodeConfCal(pocNodeConf).saveOrUpdate();

//        PocHolder.scoreMappingAndPersist(pocScoreToUpdate);
        return true;
    }

    /**
     * process the online rate tx of poc series
     *
     * @param height     block height that included this tx
     * @param onlineRate OnlineRate tx
     * @return
     */
    private static boolean onlineRateTxProcess(int height, PocTxBody.PocOnlineRate onlineRate) {
//        Peer peer = Peers.getPeer(onlineRate.getIp(), true);
//
//        PocScore pocScoreToUpdate = getPocScoreByPeer(height, onlineRate.getIp());
//        if (pocScoreToUpdate == null) {
//            return false;
//        }
        CertifiedPeer certifiedPeer = PocHolder.getBoundPeer(onlineRate.getHost(), height);
        if(certifiedPeer == null) return false;

        PocScore pocScoreToUpdate = PocHolder.getPocScore(height, certifiedPeer.getBoundAccountId());

        pocScoreToUpdate.onlineRateCal(certifiedPeer.getType(), onlineRate).saveOrUpdate();

//        PocHolder.scoreMappingAndPersist(pocScoreToUpdate);
        return true;
    }

    /**
     * process the block miss tx of poc series
     *
     * @param height          block height that included this tx
     * @param pocBlockMissing PocBlockMissing tx
     * @return
     */
    private static boolean blockMissingTxProcess(int height, PocTxBody.PocGenerationMissing pocBlockMissing) {

        List<Long> missAccountIds = pocBlockMissing.getMissingAccountIds();
        for (Long missAccountId : missAccountIds) {
//            PocScore pocScoreToUpdate = new PocScore(missAccountId, height);
            PocScore pocScoreToUpdate = PocHolder.getPocScore(height, missAccountId);
            pocScoreToUpdate.blockMissCal(pocBlockMissing).saveOrUpdate();
//            PocHolder.scoreMappingAndPersist(pocScoreToUpdate);
        }
        return true;
    }

    /**
     * process the balance of account changed
     *
     * @param height  block height that included this tx
     * @param account which balance is changed
     * @return
     */
    private static boolean balanceChangedProcess(int height, Account account) {
        if (account == null) {
            return false;
        }
        long accountId = account.getId();
//        PocScore pocScoreToUpdate = new PocScore(accountId, height);
        PocScore pocScoreToUpdate = PocHolder.getPocScore(height, accountId);
        pocScoreToUpdate.ssCal().saveOrUpdate();
//        PocHolder.scoreMappingAndPersist(pocScoreToUpdate);
        return true;
    }

}
