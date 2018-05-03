package org.conch.http.biz;

import org.conch.http.ParameterException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import static org.conch.http.JSONResponses.BIZ_MISSING_CLIENT;

public class BizParameterRequestWrapper extends HttpServletRequestWrapper {
    private Map<String, String[]> params;

    private static final String UPLOAD_TEXTDATA = "uploadTextData";
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

        String queryString = req.getQueryString();
        String requestType = req.getParameter("requestType");
        switch (requestType){
            case UPLOAD_TEXTDATA :
                if (req.getParameter("deadline") == null)
                    this.params.put("deadline", new String[]{"60"});
                if (req.getParameter("feeNQT") == null)
                    this.params.put("feeNQT", new String[]{"0"});
                this.params.put("channel", new String[]{req.getParameter("clientAccount")});
                this.params.put("secretPhrase", new String[]{req.getParameter("passPhrase")});
                this.params.put("name", new String[]{req.getParameter("fileName")});
                this.params.put("type", new String[]{req.getParameter("fileType")});
                break;
        }
    }
}
