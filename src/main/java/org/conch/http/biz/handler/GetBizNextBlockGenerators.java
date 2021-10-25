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

package org.conch.http.biz.handler;

import com.google.common.collect.Lists;
import org.conch.Conch;
import org.conch.chain.Blockchain;
import org.conch.common.ConchException;
import org.conch.common.Constants;
import org.conch.http.APIServlet;
import org.conch.http.APITag;
import org.conch.http.ParameterException;
import org.conch.http.ParameterParser;
import org.conch.mint.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.conch.http.JSONResponses.BIZ_INCORRECT_INDEX;

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
 * <li>firstIndex - The start number of miners to return.
 * <li>lastIndex - The end number of miners to return.
 * </ul>
 * <p>
 * Return fields:
 * <ul>
 * <li>generators - The next block generators
 * <ul>
 * <li>account - The account identifier
 * <li>accountRS - The account RS identifier
 * <li>deadline - The difference between the generation time and the last block timestamp
 * <li>effectiveBalanceSS - The account effective balance
 * <li>hitTime - The generation time for the account
 * <li>totalBlockCount - The total number of block out of the whole network
 * <li>minerBlockCount - Miner block times
 * <li>percentage - Percentage of block output
 * </ul>
 * </ul>
 * @author bowen
 * @date 2021/03/09
 */
public final class GetBizNextBlockGenerators extends APIServlet.APIRequestHandler {

    public static final GetBizNextBlockGenerators instance = new GetBizNextBlockGenerators();

    private GetBizNextBlockGenerators() {
            super(new APITag[] {APITag.FORGING}, "firstIndex", "lastIndex");
    }

    private static JSONObject response = new JSONObject();
    private static long blockId = 0;
    private static List<JSONObject> generators = Lists.newArrayList();

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        Blockchain blockchain = Conch.getBlockchain();
        if (lastIndex - firstIndex > 500) {
            throw new ParameterException(BIZ_INCORRECT_INDEX);
        }
        try {
            blockchain.readLock();
            if (blockId != Conch.getBlockchain().getLastBlock().getId()) {
                generators.clear();
                blockId = Conch.getBlockchain().getLastBlock().getId();
                for (Generator.ActiveGenerator generator : Generator.getNextGenerators()) {
                    if (generator.getHitTime() > Integer.MAX_VALUE) {
                        break;
                    }
                    generators.add(generator.toBlockExplorerJson());
                }
                // generators sort
                Collections.sort(generators, new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject o1, JSONObject o2) {
                        int a = (int) o1.get("minerBlockCount");
                        int b = (int) o2.get("minerBlockCount");
                        if (a < b) {
                            return 1;
                        } else if(a == b) {
                            return 0;
                        } else
                            return -1;
                    }
                });
            }
            List<JSONObject> subList = null;
            if (firstIndex < lastIndex) {
                subList = generators.subList(firstIndex, lastIndex <= generators.size() ? lastIndex : generators.size());
            }

            response.put("generators", subList);
            response.put("generatorCount", generators.size());
        } finally {
            blockchain.readUnlock();
        }
        return response;
    }

    /**
     * No required block parameters
     *
     * @return  FALSE to disable the required block parameters
     */
    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean startDbTransaction() {
        return true;
    }
}
