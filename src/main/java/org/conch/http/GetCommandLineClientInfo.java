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
 *  GNU General Public License for more content.
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

import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.Block;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class GetCommandLineClientInfo extends APIServlet.APIRequestHandler {

    static final GetCommandLineClientInfo instance = new GetCommandLineClientInfo();

    private GetCommandLineClientInfo() {
        super(new APITag[]{APITag.BLOCKS, APITag.DEBUG}, "firstIndex", "lastIndex", "timestamp", "includeTransactions", "includeExecutedPhased", "type", "subtype", "numberOfConfirmations", "withMessage", "phasedOnly", "nonPhasedOnly",
                "includeExpiredPrunable", "includePhasingResult", "executedOnly");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        if (ParameterParser.getLastIndex(req) > 9) {
            lastIndex = 9;
        }
        final int timestamp = ParameterParser.getTimestamp(req);
        boolean includeTransactions = "true".equalsIgnoreCase(req.getParameter("includeTransactions"));
        boolean includeExecutedPhased = "true".equalsIgnoreCase(req.getParameter("includeExecutedPhased"));
        String prefix = "\n[DEBUG] ";
        String splitLine = "\n[DEBUG] ----------------------------";
        JSONObject response = new JSONObject();

        getCurrentPeerMiningInfo(prefix, firstIndex, lastIndex, timestamp, splitLine);

        getBlockcontent(prefix, firstIndex, lastIndex, timestamp, includeTransactions, includeExecutedPhased, splitLine);

        getGuidePeerInfo(prefix, splitLine);
        return response;
    }

    /**
     * Boot peers info
     */
    private void getGuidePeerInfo(String prefix, String splitLine) {
        List<Peer> peerList = Peers.getPeers(chkPeer -> chkPeer.getAnnouncedAddress().contains("mw.run"));
        String header = prefix + "Boot peers connection info: ";
        String title = String.format(prefix + "%-20s%-20s%-20s%-10s%-40s ", "Address", "State", "Host", "Port", "URI");
        String contentFormat = prefix + "%-20s%-20s%-20s%-10d%-40s ";
        String content = "";
        for (Peer peer : peerList) {
            content += String.format(contentFormat, peer.getAddress(), peer.getState(), peer.getHost(), peer.getPort(), peer.getPeerLoad().getUri());
        }
        Logger.logDebugMessage("%s%s%s%s", header, title, content, splitLine);
    }

    /**
     * Block content
     */
    private void getBlockcontent(String prefix, int firstIndex, int lastIndex, int timestamp, boolean includeTransactions, boolean includeExecutedPhased, String splitLine) {
        JSONArray blocks = new JSONArray();
        DbIterator<? extends Block> iterator = null;
        String content = "";
        String contentFormat = prefix + "%-24d%-70s%-24d%-70s%-30s%-20d%-20d%-20d%-20d ";
        String header = String.format(prefix + "Details of the last %d-%d blocks: ", firstIndex+1, lastIndex+1);
        String title = String.format(prefix + "%-24s%-70s%-24s%-70s%-30s%-20s%-20s%-20s%-20s ", "PreviousBlockId", "PreviousBlockHash", "BlockId", "BlockHash", "BlockGenerator", "BlockTimestamp", "PayloadLength", "TransactionCount", "BlockHeight");
        try {
            iterator = Conch.getBlockchain().getBlocks(firstIndex, lastIndex);
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (block.getTimestamp() < timestamp) {
                    break;
                }
                blocks.add(JSONData.block(block, includeTransactions, includeExecutedPhased));
                content += String.format(contentFormat, block.getPreviousBlockId(), Convert.toHexString(block.getPreviousBlockHash()), block.getId(), Convert.toHexString(block.getPayloadHash()), Account.rsAccount(block.getGeneratorId()), block.getTimestamp(), block.getPayloadLength(), block.getTransactions().size(), block.getHeight());
            }
        } finally {
            DbUtils.close(iterator);
            Logger.logDebugMessage("%s%s%s%s", header, title, content, splitLine);
        }
    }

    /**
     * Current peer mining info
     */
    private void getCurrentPeerMiningInfo(String prefix, int firstIndex, int lastIndex, int timestamp, String splitLine) {
        JSONObject myPeerInfoJson = Peers.generateMyPeerJson();

        // Get current peer`s account info
        String account = Convert.emptyToNull((String) myPeerInfoJson.get("bindRsAccount"));
        // test
        /*if (null == account) {
            account = "CDW-FUYN-QQ24-BNU6-8D2EU";
        }*/
        // current peer info
        String header = String.format(prefix + "Current peer info:");
        String title = String.format(prefix + "%-20s%-10s%-30s%-30s", "AnnouncedAddress", "ApiPort", "Account", "Platform");
        String contentFormat = prefix + "%-20s%-10s%-30s%-30s";
        String content = String.format(contentFormat, myPeerInfoJson.get("announcedAddress"), myPeerInfoJson.get("apiPort"), account, myPeerInfoJson.get("platform"));
        Logger.logDebugMessage("%s%s%s%s", header, title, content, splitLine);


        String blockListHeader = String.format(prefix + "List of blocks last %d-%d packaged by the current peer: ", firstIndex+1, lastIndex+1);
        String blockListTitle = String.format(prefix + "%-30s%-20s%-20s%-20s%-20s%-20s", "BlockId", "TotalAmount("+ Conch.COIN_UNIT +")", "TotalFee("+ Conch.COIN_UNIT +")", "TransactionCount", "BlockHeight", "BlockTimestamp");
        String blockListContentFormat = prefix + "%-30d%-20f%-20f%-20d%-20d%-20s";
        String blockListContent = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long accountId = Account.rsAccountToId(account);
        int blockCount = 0;
        long blockTotalAmount = 0;
        long blockTotalFee = 0;

        // Get current peer`s generate blocks info
        if (null == account) {
            Logger.logDebugMessage(prefix + "The Account is null and block information cannot be obtained" + splitLine);
        } else {
            DbIterator<? extends Block> iterator = null;
            try {
                iterator = Conch.getBlockchain().getBlocks(accountId, timestamp);
                int count = 0;
                while (iterator.hasNext()) {
                    Block block = iterator.next();
                    if (count < (lastIndex - firstIndex)) {
                        blockListContent += String.format(blockListContentFormat, block.getId(), (double) block.getTotalAmountNQT() / Constants.ONE_SS, (double) block.getTotalFeeNQT() / Constants.ONE_SS, block.getTransactions().size(), block.getHeight(), sdf.format(new Date(Long.parseLong(String.valueOf(Convert.fromEpochTime(block.getTimestamp()))))));
                    }
                    // Calculate total amount of all blocks
                    blockTotalAmount += block.getTotalAmountNQT();
                    blockTotalFee += block.getTotalFeeNQT();
                    count++;
                }
                // The total number of blocks
                blockCount = Conch.getBlockchain().getBlockCount(accountId);

            } finally {
                DbUtils.close(iterator);
                Logger.logDebugMessage("%s%s%s", blockListHeader, blockListTitle, blockListContent);
                Logger.logDebugMessage(splitLine + prefix + "Total count: " + prefix + "%-20s%-20s%-20s" + prefix + "%-20d%-20f%-20f", "BlockCounts", "BlockTotalAmount", "BlockTotalFee", blockCount, (double) blockTotalAmount / Constants.ONE_SS, (double) blockTotalFee / Constants.ONE_SS);
            }
        }
    }
}
