/*
 * Copyright © 2017 sharder.org.
 * Copyright © 2014-2017 ichaoj.com.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with ichaoj.com,
 * no part of the COS software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package org.conch.http;

import org.conch.AccountLedger;
import org.conch.Block;
import org.conch.BlockchainProcessor;
import org.conch.Constants;
import org.conch.Conch;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

public final class GetBlockchainStatus extends APIServlet.APIRequestHandler {

    static final GetBlockchainStatus instance = new GetBlockchainStatus();

    private GetBlockchainStatus() {
        super(new APITag[] {APITag.BLOCKS, APITag.INFO});
    }

    @Override
    protected JSONObject processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        response.put("application", Conch.APPLICATION);
        response.put("version", Conch.VERSION);
        response.put("time", Conch.getEpochTime());
        Block lastBlock = Conch.getBlockchain().getLastBlock();
        response.put("lastBlock", lastBlock.getStringId());
        response.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
        response.put("numberOfBlocks", lastBlock.getHeight() + 1);
        BlockchainProcessor blockchainProcessor = Conch.getBlockchainProcessor();
        Peer lastBlockchainFeeder = blockchainProcessor.getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("lastBlockchainFeederHeight", blockchainProcessor.getLastBlockchainFeederHeight());
        response.put("isScanning", blockchainProcessor.isScanning());
        response.put("isDownloading", blockchainProcessor.isDownloading());
        response.put("maxRollback", Constants.MAX_ROLLBACK);
        response.put("currentMinRollbackHeight", Conch.getBlockchainProcessor().getMinRollbackHeight());
        response.put("isTestnet", Constants.isTestnet);
        response.put("maxPrunableLifetime", Constants.MAX_PRUNABLE_LIFETIME);
        response.put("includeExpiredPrunable", Constants.INCLUDE_EXPIRED_PRUNABLE);
        response.put("correctInvalidFees", Constants.correctInvalidFees);
        response.put("ledgerTrimKeep", AccountLedger.trimKeep);
        JSONArray servicesArray = new JSONArray();
        Peers.getServices().forEach(service -> servicesArray.add(service.name()));
        response.put("services", servicesArray);
        if (APIProxy.isActivated()) {
            String servingPeer = APIProxy.getInstance().getMainPeerAnnouncedAddress();
            response.put("apiProxy", true);
            response.put("apiProxyPeer", servingPeer);
        } else {
            response.put("apiProxy", false);
        }
        response.put("isLightClient", Constants.isLightClient);
        response.put("maxAPIRecords", API.maxRecords);
        response.put("blockchainState", Peers.getMyBlockchainState());
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
