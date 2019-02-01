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
import org.conch.chain.Block;
import org.conch.chain.Blockchain;
import org.conch.common.ConchException;
import org.conch.mint.Generator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * The GetNextBlockGenerators API will return the next block generators ordered by the
 * hit time.  The list of active miners is initialized using the block generators
 * with at least 2 blocks generated within the previous 10,000 blocks.  Accounts without
 * a public key will not be included.  The list is
 * updated as new blocks are processed.  This means the results will not be 100%
 * correct since previously active generators may no longer be running and new generators
 * won't be known until they generate a block.  This API will be replaced when transparent
 * forging is activated.
 * <p>
 * Request parameters:
 * <ul>
 * <li>limit - The number of miners to return and defaults to 1.
 * </ul>
 * <p>
 * Return fields:
 * <ul>
 * <li>activeCount - The number of active generators
 * <li>height - The last block height
 * <li>lastBlock - The last block identifier
 * <li>timestamp - The last block timestamp
 * <li>generators - The next block generators
 * <ul>
 * <li>account - The account identifier
 * <li>accountRS - The account RS identifier
 * <li>deadline - The difference between the generation time and the last block timestamp
 * <li>effectiveBalanceSS - The account effective balance
 * <li>hitTime - The generation time for the account
 * </ul>
 * </ul>
 */
public final class GetNextBlockGenerators extends APIServlet.APIRequestHandler {

    static final GetNextBlockGenerators instance = new GetNextBlockGenerators();

    private GetNextBlockGenerators() {
        super(new APITag[] {APITag.FORGING}, "limit");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        boolean loadPoolInfo = ParameterParser.getBoolean(req, "loadPoolInfo");
        JSONObject response = new JSONObject();
        int limit = Math.max(1, ParameterParser.getInt(req, "limit", 1, Integer.MAX_VALUE, false));
        Blockchain blockchain = Conch.getBlockchain();
        blockchain.readLock();
        try {
            Block lastBlock = blockchain.getLastBlock();
            response.put("timestamp", lastBlock.getTimestamp());
            response.put("height", lastBlock.getHeight());
            response.put("lastBlock", Long.toUnsignedString(lastBlock.getId()));
            List<Generator.ActiveGenerator> activeGenerators = Generator.getNextGenerators();
            response.put("activeCount", activeGenerators.size());
            JSONArray generators = new JSONArray();
            for (Generator.ActiveGenerator generator : activeGenerators) {
                if (generator.getHitTime() > Integer.MAX_VALUE) {
                    break;
                }
                generators.add(generator.toJson(loadPoolInfo));
                if (generators.size() == limit) {
                    break;
                }
            }
            response.put("generators", generators);
        } finally {
            blockchain.readUnlock();
        }
        return response;
    }

    /**
     * No required block parameters
     *
     * @return                      FALSE to disable the required block parameters
     */
    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
