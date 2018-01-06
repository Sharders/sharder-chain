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

import org.conch.Conch;
import org.conch.ConchException;
import org.conch.util.JSON;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessTransactions extends PeerServlet.PeerRequestHandler {

    static final ProcessTransactions instance = new ProcessTransactions();

    private ProcessTransactions() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        try {
            Conch.getTransactionProcessor().processPeerTransactions(request);
            return JSON.emptyJSON;
        } catch (RuntimeException | ConchException.ValidationException e) {
            Logger.logDebugMessage("Failed to parse peer transactions: " + request.toJSONString());
            peer.blacklist(e);
            return PeerServlet.error(e);
        }

    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
