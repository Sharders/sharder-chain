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

package org.conch.http;

import org.conch.Conch;
import org.conch.chain.CheckSumValidator;
import org.conch.common.ConchException;
import org.conch.common.UrlManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ForceConverge extends APIServlet.APIRequestHandler {

    static final ForceConverge instance = new ForceConverge();

    private ForceConverge() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try {
            if(!UrlManager.validFoundationHost(req)){
                response.put("error", "Not valid request sender");
                return response;
            }

            CheckSumValidator.updateKnownIgnoreBlocks();
            
            int height = Conch.getBlockchain().getHeight();
            try {
                height = Integer.parseInt(req.getParameter("height"));
            } catch (NumberFormatException ignored) {}

            
//            List<? extends Block> blocks;
//            try {
//                Conch.getBlockchainProcessor().setGetMoreBlocks(false);
//                if (numBlocks > 0) {
//                    blocks = Conch.getBlockchainProcessor().popOffTo(Conch.getBlockchain().getHeight() - numBlocks);
//                } else if (height > 0) {
//                    blocks = Conch.getBlockchainProcessor().popOffTo(height);
//                } else {
//                    return JSONResponses.missing("numBlocks", "height");
//                }
//            } finally {
//                Conch.getBlockchainProcessor().setGetMoreBlocks(true);
//            }
//            JSONArray blocksJSON = new JSONArray();
//            blocks.forEach(block -> blocksJSON.add(JSONData.block(block, true, false)));
//            JSONObject response = new JSONObject();
//            response.put("blocks", blocksJSON);
//            if (keepTransactions) {
//                blocks.forEach(block -> Conch.getTransactionProcessor().processLater(block.getTransactions()));
//            }
//            
            
            req.getParameter("includeLessors");
          
            
            
            response.put("done", true);
            
            
        } catch (ConchException.NotValidException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            JSONData.putException(response, e);
        }
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
