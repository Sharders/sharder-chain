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

package org.conch.peer;

import org.conch.Conch;
import org.conch.chain.Block;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.http.JSONData;
import org.conch.http.ParameterParser;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.ArrayList;
import java.util.List;

final class GetBlocks extends PeerServlet.PeerRequestHandler {

    static final GetBlocks instance = new GetBlocks();
    private GetBlocks() {}

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        try {
            Long latestNum = (Long) request.get("latestNum");
            final int timestamp = 0;
            if (latestNum > 0) {
                JSONArray blocks = new JSONArray();
                DbIterator<? extends Block> iterator = null;
                try {
                    iterator = Conch.getBlockchain().getBlocks(0, Integer.parseInt(latestNum.toString()) - 1);
                    while (iterator.hasNext()) {
                        Block block = iterator.next();
                        if (block.getTimestamp() < timestamp) {
                            break;
                        }
                        blocks.add(JSONData.forkBlock(block));
                    }
                }finally {
                    DbUtils.close(iterator);
                }
                response.put("blocks", blocks);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
        }
        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
