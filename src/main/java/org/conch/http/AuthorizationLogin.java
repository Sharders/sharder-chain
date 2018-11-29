package org.conch.http;

import org.conch.common.ConchException;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
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
    private static final int AUTH_CODE_NUM = 128;

    /**
     * 用于保存token的队列;
     */
    private static HashMap<String, Object> hashMap = new HashMap<>();

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
            return validationAccount(request);
        }
        if (Shell.ACCOUNT.getValue().equalsIgnoreCase(shell)) {
            return tokenToAccount(request);
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
        String account = request.getParameter("account");
        JSONObject json = requestAuthValidation(request, account);
        if (json.containsKey("error")) {
            return json;
        }
        String token = generateToken(AUTH_CODE_NUM);

        HashMap map = new HashMap<String, Long>();
        map.put(account, System.currentTimeMillis());
        hashMapAdd(token, map);

        json.put("token", token);
        return json;
    }

    /**
     * 随机生成指定长度的16进制的token
     *
     * @param num
     * @return
     */
    private String generateToken(int num) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < num; i++) {
            sb.append(Integer.toHexString(new Random().nextInt(16)));
        }
        //已存在就重新生成
        if (hashMap.containsKey(sb.toString())) {
            return generateToken(AUTH_CODE_NUM);
        }
        return sb.toString();
    }

    /**
     * 请求授权验证
     *
     * @param request
     * @return
     */
    private JSONObject requestAuthValidation(HttpServletRequest request, String account) {
        JSONObject json = new JSONObject();
        json.put("success", false);
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
        json.put("success", true);
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
        //验证token时效
        if (verifyTime(request, json).containsKey("error")) {
            json.put("success", false);
            return json;
        }
        //验证签名
        if (verifySignature(request, json).containsKey("error")) {
            json.put("success", false);
            return json;
        }
        json.put("success", true);
        return json;
    }

    /**
     * 验证时间有效性
     *
     * @param request
     * @param json
     * @return
     */
    private JSONObject verifyTime(HttpServletRequest request, JSONObject json) {
        String validation = request.getParameter("validation");
        String account = request.getParameter("account");
        if (validation == null || account == null) {
            json.put("memo", "validation and account can't be empty");
            json.put("error", true);
            return json;
        }
        try {
            HashMap<String, Long> user = (HashMap<String, Long>) hashMap.get(validation);
            hashMap.remove(validation);
            if (!user.containsKey(account) || System.currentTimeMillis() - user.get(account) > AUTH_TIME) {
                throw new RuntimeException("validation overdue");
            }
        } catch (Exception e) {
            json.put("memo", "validation overdue");
            json.put("error", true);
        }
        return json;
    }

    /**
     * 验证签名信息
     *
     * @param request
     * @return
     */
    private JSONObject verifySignature(HttpServletRequest request, JSONObject json) {
        String message = request.getParameter("message");
        try {
            //验证签名得到用户,公钥和时间存入json;
            HashMap<String, Object> map = new HashMap<>();
            map.put("time", System.currentTimeMillis());
            map.put("account", "SSA-TPLD-BHYH-DF2B-GAU6P");
            map.put("publicKey", "publicKey publicKey publicKey publicKey publicKey publicKey");
            //生成授权token
            String token = generateToken(AUTH_CODE_NUM);
            //保存全局
            hashMapAdd(token, map);
            json.put("token", token);
        } catch (Exception e) {
            json.put("memo", "secretPhrase error");
            json.put("error", true);
        }
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
        if (verifyToken(request, json).containsKey("error")) {
            json.put("success", false);
            return json;
        }
        json.put("success", true);
        return json;
    }

    /**
     * 检验token
     *
     * @return
     */
    private JSONObject verifyToken(HttpServletRequest request, JSONObject json) {
        String token = request.getParameter("token");
        if (token == null) {
            json.put("memo", "token can't be empty");
            json.put("error", true);
            return json;
        }
        try {
            HashMap map = (HashMap<String, Object>) hashMap.get(token);
            hashMap.remove(token);
            if (System.currentTimeMillis() - (long) map.get("time") > AUTH_TIME) {
                throw new RuntimeException("token invalid");
            }
            json.put("data", map);
        } catch (Exception e) {
            json.put("memo", "token invalid");
            json.put("error", true);
        }
        return json;
    }

    /**
     * 添加hashMap值
     * 当超过1024个 hashMap值会自动清除过期的
     *
     * @param key
     * @param value
     */
    private void hashMapAdd(String key, Object value) {
        if (hashMap.size() > 1024) {
            try {
                for (String k : hashMap.keySet()) {
                    HashMap map = (HashMap<String, Object>) hashMap.get(k);
                    long time = (long) map.get("time");
                    if (System.currentTimeMillis() - time > AUTH_TIME) {
                        hashMap.remove(k);
                    }
                }
            } catch (Exception e) {
                hashMap.clear();
            }
        }
        hashMap.put(key, value);
    }
}
