package org.conch.http;

import org.conch.common.ConchException;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Random;

public final class AuthorizationLogin extends APIServlet.APIRequestHandler {
    static final AuthorizationLogin instance = new AuthorizationLogin();
    /**
     * 授权有效期(毫秒)
     */
    private static final int AUTH_TIME = 10 * 1000;
    /**
     * 授权码长度
     */
    private static final int AUTH_CODE_NUM = 32;

    enum Shell {
        /**
         * 请求授权
         */
        AUTH("auth"),
        /**
         * 签名验证
         */
        VALIDATION("validation"),
        /**
         * 获得token
         */
        TOKEN("token"),
        /**
         * 获得用户
         */
        ACCOUNT("account");
        private String value;

        Shell(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private AuthorizationLogin() {
        super(new APITag[]{APITag.DEBUG});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        String shell = request.getParameter("shell");
        if (Shell.AUTH.getValue().equalsIgnoreCase(shell)) {
            return generateAuthorization(request);
        }
        if (Shell.VALIDATION.getValue().equalsIgnoreCase(shell)) {
            return generateAuthorization(request);
        }
        if (Shell.TOKEN.getValue().equalsIgnoreCase(shell)) {
            return generateAuthorization(request);
        }
        if (Shell.ACCOUNT.getValue().equalsIgnoreCase(shell)) {
            return generateAuthorization(request);
        }
        return null;
    }

    /**
     * 生成请求请求授权数据
     *
     * @param request
     * @return
     */
    private JSONObject generateAuthorization(HttpServletRequest request) {
        JSONObject json = requestAuthValidation(request);
        if (json.containsKey("error")) {
            return json;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < AUTH_CODE_NUM; i++) {
            sb.append(Integer.toHexString(new Random().nextInt(16)));
        }
        json.put("token", sb.toString());
        //添加token时效性
        request.setAttribute(sb.toString(), System.currentTimeMillis());
        return json;
    }

    /**
     * 请求授权验证
     *
     * @param request
     * @return
     */
    private JSONObject requestAuthValidation(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        json.put("Authorization", false);
        String account = request.getParameter("account");
        if (account == null) {
            json.put("memo", "account cannot be null");
            json.put("error", true);
            return json;
        }
        //验证账户是否有效
        try {
            long value = Convert.parseAccountId(account);
            if (value == 0) {
                throw new RuntimeException("account does not exist");
            }
        } catch (RuntimeException e) {
            json.put("memo", "account does not exist");
            json.put("error", true);
            return json;
        }
        json.put("Authorization", true);
        return json;
    }

    /**
     * 验证签名和时效性
     *
     * @param request
     * @return
     */
    private JSONObject validationAccount(HttpServletRequest request) {
        JSONObject json = new JSONObject();

        return json;
    }

    /**
     * 生成授权token
     *
     * @param request
     * @return
     */
    private JSONObject generateAuthToken(HttpServletRequest request) {
        JSONObject json = new JSONObject();

        return json;
    }

    /**
     * 通过token获得账户信息
     *
     * @param request
     * @return
     */
    private JSONObject tokenToAccount(HttpServletRequest request) {
        JSONObject json = new JSONObject();

        return json;
    }
}
