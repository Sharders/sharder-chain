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

import org.conch.util.JSON;
import org.conch.util.Logger;
import org.conch.util.JSON;
import org.conch.util.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.AsyncMiddleManServlet;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

public final class ShapeShiftProxyServlet extends AsyncMiddleManServlet {

    static final String SHAPESHIFT_TARGET = "/shapeshift";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            super.service(new ClientRequestWrapper(request), response);
        } catch(Exception e) {
            JSONObject errorJson = new JSONObject();
            errorJson.put("errorDescription", e.getMessage());
            try {
                try (Writer writer = response.getWriter()) {
                    JSON.writeJSONString(JSON.prepare(errorJson), writer);
                }
            } catch(IOException ioe) {
                Logger.logInfoMessage("Failed to write response to client", ioe);
            }
        }
    }

    @Override
    protected HttpClient newHttpClient() {
        return HttpClientFactory.newHttpClient();
    }

    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {
        return "https://shapeshift.io" + clientRequest.getRequestURI();
    }

    @Override
    protected void onClientRequestFailure(HttpServletRequest clientRequest, Request proxyRequest,
                                          HttpServletResponse proxyResponse, Throwable failure) {
        super.onClientRequestFailure(clientRequest, proxyRequest, proxyResponse, failure);
    }

    @Override
    protected Response.Listener newProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
        return new APIProxyResponseListener(request, response);
    }

    private class APIProxyResponseListener extends ProxyResponseListener {

        APIProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }

        @Override
        public void onFailure(Response response, Throwable failure) {
            super.onFailure(response, failure);
            Logger.logErrorMessage("shape shift proxy failed", failure);
        }
    }

    private static class ClientRequestWrapper extends HttpServletRequestWrapper {

        private final HttpServletRequest request;

        ClientRequestWrapper(HttpServletRequest request) {
            super(request);
            this.request = request;
        }

        @Override
        public String getRequestURI() {
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith(SHAPESHIFT_TARGET)) {
                uri = uri.replaceFirst(SHAPESHIFT_TARGET, "");
            }
            return uri;
        }
    }
}