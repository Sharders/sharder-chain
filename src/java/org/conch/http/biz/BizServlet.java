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

package org.conch.http.biz;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.conch.http.biz.exception.BizErrorResolver;
import org.conch.http.biz.service.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class BizServlet extends HttpServlet {

    private JsonRpcServer jsonRpcServer;

    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        try {
            jsonRpcServer.handle(req, resp);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        List<Service> services = new ArrayList<>();
        List<Class> classes = new ArrayList<>();

        for (BizServiceEnum api : BizServiceEnum.values()) {
            if (api.getServiceImpl() != null) {
                services.add(api.getServiceImpl());
                classes.add(api.getServiceInterface());
            }
        }

        Object compositeService = ProxyUtil.createCompositeServiceProxy(
                this.getClass().getClassLoader(), services.toArray(), classes.toArray(new Class[classes.size()]), true);
        jsonRpcServer = new JsonRpcServer(compositeService);
        jsonRpcServer.setErrorResolver(new BizErrorResolver());
        jsonRpcServer.getRequestInterceptor();
    }

}
