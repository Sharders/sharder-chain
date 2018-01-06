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

package org.conch.peer;

import org.conch.Block;
import org.conch.Constants;
import org.conch.Conch;
import org.conch.util.Convert;
import org.conch.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetMilestoneBlockIds extends PeerServlet.PeerRequestHandler {

    static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();

    private GetMilestoneBlockIds() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        try {

            JSONArray milestoneBlockIds = new JSONArray();

            String lastBlockIdString = (String) request.get("lastBlockId");
            if (lastBlockIdString != null) {
                long lastBlockId = Convert.parseUnsignedLong(lastBlockIdString);
                long myLastBlockId = Conch.getBlockchain().getLastBlock().getId();
                if (myLastBlockId == lastBlockId || Conch.getBlockchain().hasBlock(lastBlockId)) {
                    milestoneBlockIds.add(lastBlockIdString);
                    response.put("milestoneBlockIds", milestoneBlockIds);
                    if (myLastBlockId == lastBlockId) {
                        response.put("last", Boolean.TRUE);
                    }
                    return response;
                }
            }

            long blockId;
            int height;
            int jump;
            int limit = 10;
            int blockchainHeight = Conch.getBlockchain().getHeight();
            String lastMilestoneBlockIdString = (String) request.get("lastMilestoneBlockId");
            if (lastMilestoneBlockIdString != null) {
                Block lastMilestoneBlock = Conch.getBlockchain().getBlock(Convert.parseUnsignedLong(lastMilestoneBlockIdString));
                if (lastMilestoneBlock == null) {
                    throw new IllegalStateException("Don't have block " + lastMilestoneBlockIdString);
                }
                height = lastMilestoneBlock.getHeight();
                jump = Math.min(Constants.GUARANTEED_BALANCE_CONFIRMATIONS, Math.max(blockchainHeight - height, 1));
                height = Math.max(height - jump, 0);
            } else if (lastBlockIdString != null) {
                height = blockchainHeight;
                jump = 10;
            } else {
                peer.blacklist("Old getMilestoneBlockIds request");
                response.put("error", "Old getMilestoneBlockIds protocol not supported, please upgrade");
                return response;
            }
            blockId = Conch.getBlockchain().getBlockIdAtHeight(height);

            while (height > 0 && limit-- > 0) {
                milestoneBlockIds.add(Long.toUnsignedString(blockId));
                blockId = Conch.getBlockchain().getBlockIdAtHeight(height);
                height = height - jump;
            }
            response.put("milestoneBlockIds", milestoneBlockIds);

        } catch (RuntimeException e) {
            Logger.logDebugMessage(e.toString());
            return PeerServlet.error(e);
        }

        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
