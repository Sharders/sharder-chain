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

package org.conch.user;

import org.conch.Conch;
import org.conch.ConchException;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class UserServlet extends HttpServlet  {

    abstract static class UserRequestHandler {
        abstract JSONStreamAware processRequest(HttpServletRequest request, User user) throws ConchException, IOException;
        boolean requirePost() {
            return false;
        }
    }

    private static final boolean enforcePost = Conch.getBooleanProperty("sharder.uiServerEnforcePOST");

    private static final Map<String,UserRequestHandler> userRequestHandlers;

    static {
        Map<String,UserRequestHandler> map = new HashMap<>();
        map.put("generateAuthorizationToken", GenerateAuthorizationToken.instance);
        map.put("getInitialData", GetInitialData.instance);
        map.put("getNewData", GetNewData.instance);
        map.put("lockAccount", LockAccount.instance);
        map.put("removeActivePeer", RemoveActivePeer.instance);
        map.put("removeBlacklistedPeer", RemoveBlacklistedPeer.instance);
        map.put("removeKnownPeer", RemoveKnownPeer.instance);
        map.put("sendMoney", SendMoney.instance);
        map.put("unlockAccount", UnlockAccount.instance);
        userRequestHandlers = Collections.unmodifiableMap(map);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        User user = null;

        try {

            String userPasscode = req.getParameter("user");
            if (userPasscode == null) {
                return;
            }
            user = Users.getUser(userPasscode);

            if (Users.allowedUserHosts != null && ! Users.allowedUserHosts.contains(req.getRemoteHost())) {
                user.enqueue(JSONResponses.DENY_ACCESS);
                return;
            }

            String requestType = req.getParameter("requestType");
            if (requestType == null) {
                user.enqueue(JSONResponses.INCORRECT_REQUEST);
                return;
            }

            UserRequestHandler userRequestHandler = userRequestHandlers.get(requestType);
            if (userRequestHandler == null) {
                user.enqueue(JSONResponses.INCORRECT_REQUEST);
                return;
            }

            if (enforcePost && userRequestHandler.requirePost() && ! "POST".equals(req.getMethod())) {
                user.enqueue(JSONResponses.POST_REQUIRED);
                return;
            }

            JSONStreamAware response = userRequestHandler.processRequest(req, user);
            if (response != null) {
                user.enqueue(response);
            }

        } catch (RuntimeException|ConchException e) {

            Logger.logMessage("Error processing GET request", e);
            if (user != null) {
                JSONObject response = new JSONObject();
                response.put("response", "showMessage");
                response.put("message", e.toString());
                user.enqueue(response);
            }

        } finally {

            if (user != null) {
                user.processPendingResponses(req, resp);
            }

        }

    }

}
