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

import com.google.common.collect.Lists;
import org.conch.Conch;
import org.conch.chain.Block;
import org.conch.chain.CheckSumValidator;
import org.conch.common.ConchException;
import org.conch.common.UrlManager;
import org.conch.mint.Generator;
import org.conch.tools.ClientUpgradeTool;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public final class ForceConverge extends APIServlet.APIRequestHandler {

    static final ForceConverge INSTANCE = new ForceConverge();

    private ForceConverge() {
        super(new APITag[] {APITag.DEBUG}, "height", "keepTx", "upgradeCos");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try {
            if(!UrlManager.validFoundationHost(req)){
                response.put("error", "Not valid request sender");
                return response;
            }
            Generator.pause(true);
            Logger.logDebugMessage("start to syn known ignore blocks...");
            // syn ignore blocks
            CheckSumValidator.updateKnownIgnoreBlocks();
            
            int currentHeight = Conch.getBlockchain().getHeight();
            int toHeight = currentHeight;
            try {
                toHeight = Integer.parseInt(req.getParameter("height"));
            } catch (NumberFormatException ignored) {}
            
            Logger.logDebugMessage("received toHeight is %d ", toHeight);
            // pop-off to specified height
            List<? extends Block> blocks = Lists.newArrayList();
            try {
                Conch.getBlockchainProcessor().setGetMoreBlocks(false);
                if(toHeight < currentHeight) {
                    Logger.logDebugMessage("start to pop-off to height %d", toHeight);
                    blocks = Conch.getBlockchainProcessor().popOffTo(toHeight);
                }
            } finally {
                Conch.getBlockchainProcessor().setGetMoreBlocks(true);
            }
            
            // tx process
            boolean keepTx = "true".equalsIgnoreCase(req.getParameter("keepTx"));
            Logger.logDebugMessage("received keepTx is %s ", keepTx);
            
            if (keepTx) {
                Logger.logDebugMessage("start to put the txs into delay process pool");
                blocks.forEach(block -> Conch.getTransactionProcessor().processLater(block.getTransactions()));
            }

            boolean upgradeCos = "true".equalsIgnoreCase(req.getParameter("upgradeCos"));
            Logger.logDebugMessage("received upgradeCos is %s ",upgradeCos);
            if(upgradeCos){
                Logger.logDebugMessage("start to auto upgrade...");
                ClientUpgradeTool.autoUpgrade(true);
            }
            
            response.put("done", true);
            
        } catch (ConchException.NotValidException | IOException e) {
            JSONData.putException(response, e);
        } catch (RuntimeException e) {
            JSONData.putException(response, e);
        }finally {
            Generator.pause(false);
        }
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return false;
    }

    @Override
    protected boolean requirePassword() {
        return false;
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
