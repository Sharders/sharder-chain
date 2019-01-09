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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

public class BizParameterRequestWrapper extends HttpServletRequestWrapper {
    private Map<String, String[]> params;

    private static final String UPLOAD_TEXTDATA = "uploadTextData";
    private static final String CREATE_CLIENT_ACCOUNT = "createClientAccount";
    public BizParameterRequestWrapper(HttpServletRequest request,
                                   Map<String, String[]> newParams) {
        super(request);
        this.params = newParams;
        // RequestDispatcher.forward parameter
        renewParameterMap(request);
    }

    @Override
    public String getParameter(String name) {
        String result = "";

        Object v = params.get(name);
        if (v == null) {
            result = null;
        } else if (v instanceof String[]) {
            String[] strArr = (String[]) v;
            if (strArr.length > 0) {
                result =  strArr[0];
            } else {
                result = null;
            }
        } else if (v instanceof String) {
            result = (String) v;
        } else {
            result =  v.toString();
        }

        return result;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return params;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return new Vector<String>(params.keySet()).elements();
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] result = null;

        Object v = params.get(name);
        if (v == null) {
            result =  null;
        } else if (v instanceof String[]) {
            result =  (String[]) v;
        } else if (v instanceof String) {
            result =  new String[] { (String) v };
        } else {
            result =  new String[] { v.toString() };
        }

        return result;
    }

    private void renewParameterMap(HttpServletRequest req) {

        String requestType = req.getParameter("requestType");
        switch (requestType){
            case UPLOAD_TEXTDATA :
                if (req.getParameter("deadline") == null)
                    this.params.put("deadline", new String[]{"3600"});
                if (req.getParameter("feeNQT") == null)
                    this.params.put("feeNQT", new String[]{"0"});
                this.params.put("channel", new String[]{req.getParameter("clientAccount")});
                this.params.put("secretPhrase", new String[]{req.getParameter("passPhrase")});
                this.params.put("name", new String[]{req.getParameter("fileName")});
                this.params.put("type", new String[]{req.getParameter("fileType")});
                break;
            case CREATE_CLIENT_ACCOUNT :
                this.params.put("secretPhrase", new String[]{req.getParameter("passPhrase")});
                this.params.put("deadline", new String[]{"3600"});
                this.params.put("message", new String[]{"account created and broadcast to the network"});
                this.params.put("feeNQT",new String[]{"0"});
                break;
        }
    }
}
