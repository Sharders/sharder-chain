package org.conch.http;

import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.crypto.Crypto;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class AuthorizationLogin extends APIServlet.APIRequestHandler {
    static final AuthorizationLogin instance = new AuthorizationLogin();
    /**
     * 授权有效期(毫秒)
     */
    private static final int AUTH_TIME = 120 * 1000;
    /**
     * 授权码长度
     */
    private static final int AUTH_CODE_NUM = 128;

    /**
     * 用于保存token的队列;
     */
    public static HashMap<String, Object> hashMap = new HashMap<>();

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
        ACCOUNT("account"),
        /**
         * 账户列表
         */
        ACCOUNT_LIST("accountList"),
        /**
         * secretPhrase 信息
         */
        SECRET_PHRASE("secretPhrase"),
        /**
         * 获得账户列表
         */
        GET_ACCOUNT_LIST("getAccountList"),
        /**
         * 绑定账户
         */
        BINDING_ACCOUNT("bindingAccount"),

        /**
         * 获得绑定账户
         */
        GET_BINDING_ACCOUNT("getBindingAccount");

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
       /* if (!Constants.isDevnet() && !allowRequest(request.getServerName())) {
            JSONObject json = new JSONObject();
            json.put("memo", "This interface only allows access by specific users");
            json.put("success", false);
            return json;
        }*/
        String shell = request.getParameter("shell");
        if (Shell.BINDING_ACCOUNT.getValue().equalsIgnoreCase(shell)) {
            return bindingAccount(request);
        }
        if (Shell.GET_BINDING_ACCOUNT.getValue().equalsIgnoreCase(shell)) {
            return getBindingAccount(request);
        }
        if (Shell.ACCOUNT_LIST.getValue().equalsIgnoreCase(shell)) {
            return accountList(request);
        }
        if (Shell.GET_ACCOUNT_LIST.getValue().equalsIgnoreCase(shell)) {
            return getAccountList(request);
        }
        if (Shell.AUTH.getValue().equalsIgnoreCase(shell)) {
            return generateAuthorization(request);
        }
        if (Shell.VALIDATION.getValue().equalsIgnoreCase(shell)) {
            return validationAccount(request);
        }
        if (Shell.ACCOUNT.getValue().equalsIgnoreCase(shell)) {
            return tokenToAccount(request);
        }
        if (Shell.SECRET_PHRASE.getValue().equalsIgnoreCase(shell)) {
            return secretPhrase(request);
        }
        return null;
    }

    /**
     * 判断请求者的域名
     *
     * @param serverName
     * @return
     */
    private boolean allowRequest(String serverName) {
        ArrayList<String> serverNameList = new ArrayList<>();
        serverNameList.add("mw.run");
        serverNameList.add("test.mw.run");
        serverNameList.add("testboot.mw.run");
        serverNameList.add("mwfs.io");
        serverNameList.add("test.mwfs.io");
        serverNameList.add("devboot.sharder.io");
        serverNameList.add("testboot.sharder.io");
        serverNameList.add("testna.sharder.io");
        serverNameList.add("testnb.sharder.io");
        return serverNameList.contains(serverName);
    }

    private JSONObject secretPhrase(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        String token = request.getParameter("token");
        String secretPhrase = request.getParameter("secretPhrase");
        if (token == null || secretPhrase == null) {
            json.put("memo", "token or secretPhrase cannot be null");
            json.put("success", false);
            return json;
        }
        try {
            HashMap<String, Object> map = (HashMap<String, Object>) hashMap.get(token);
            if (System.currentTimeMillis() - (long) map.get("time") > AUTH_TIME) {
                throw new RuntimeException("token invalid");
            }
            hashMap.remove(token);
        } catch (Exception e) {
            json.put("memo", "token overdue");
            json.put("success", false);
            return json;
        }
        String tt = generateToken(AUTH_CODE_NUM);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("secretPhrase", secretPhrase);
        hashMap.put("time", System.currentTimeMillis());
        hashMapAdd(tt, hashMap);
        json.put("token", tt);
        json.put("success", true);
        return json;
    }


    private JSONObject getBindingAccount(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        String token = request.getParameter("token");
        if (token == null) {
            json.put("memo", "token cannot be null");
            json.put("success", false);
            return json;
        }
        HashMap<String, Object> map = new HashMap<>();
        try {
            map = (HashMap<String, Object>) hashMap.get(token);
            if (System.currentTimeMillis() - (long) map.get("time") > AUTH_TIME) {
                throw new RuntimeException("token invalid");
            }
            hashMap.remove(token);
        } catch (Exception e) {
            json.put("memo", "token overdue");
            json.put("success", false);
            return json;
        }
        json.put("data", map);
        json.put("success", true);
        return json;
    }

    private JSONObject bindingAccount(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        String token = request.getParameter("token");
        String account = request.getParameter("account");
        if (token == null || account == null) {
            json.put("memo", "token and account cannot be null");
            json.put("success", false);
            return json;
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("account", account);
        map.put("time", System.currentTimeMillis());
        hashMapAdd(token, map);
        json.put("success", true);
        return json;
    }

    /**
     * 账户列表
     *
     * @return
     */
    private JSONObject accountList(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        String token = "";
        try {
            Map<String, String[]> parameterMap = request.getParameterMap();
            HashMap<String, Object> map = new HashMap<>();
            for (String key : parameterMap.keySet()) {
                if (key.contains("accountList") && key.contains("[account]")) {
                    map.put(key, parameterMap.get(key));
                }
                if (key.contains("accountList") && key.contains("[assets]")) {
                    map.put(key, parameterMap.get(key));
                }
            }
            token = generateToken(AUTH_CODE_NUM);
            map.put("time", System.currentTimeMillis());
            hashMapAdd(token, map);
        } catch (Exception e) {
            json.put("success", false);
            json.put("memo", "data error");
            return json;
        }
        json.put("token", token);
        json.put("success", true);
        return json;
    }

    /**
     * 获得账户列表
     *
     * @return
     */
    private JSONObject getAccountList(HttpServletRequest request) {
        JSONObject json = new JSONObject();
        String token = request.getParameter("token");
        if (token == null) {
            json.put("memo", "token cannot be null");
            json.put("success", false);
            return json;
        }
        if (validationAccountToken(token, json).containsKey("error")) {
            json.put("memo", "token overdue");
            json.put("success", false);
            return json;
        }
        json.put("success", true);
        return json;
    }

    private JSONObject validationAccountToken(String token, JSONObject json) {
        if (!hashMap.containsKey(token)) {
            json.put("error", true);
            return json;
        }
        HashMap<String, Object> map = new HashMap<>();
        try {
            map = (HashMap<String, Object>) hashMap.get(token);
            if (System.currentTimeMillis() - (long) map.get("time") > AUTH_TIME) {
                throw new RuntimeException("token invalid");
            }
            for (String key : map.keySet()) {
                if (!"time".equals(key)) {
                    String[] str = (String[]) map.get(key);
                    map.put(key, str[0]);
                }
            }
            hashMap.remove(token);
            json.put("data", map);
        } catch (Exception e) {
            json.put("error", true);
            return json;
        }
        return json;
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
            long value = Account.rsAccountToId(account);
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
            if (!user.containsKey(account) || System.currentTimeMillis() - user.get(account) > AUTH_TIME) {
                throw new RuntimeException("validation overdue");
            }
            hashMap.remove(validation);
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
        String publicKey = request.getParameter("publicKey");
        String signature = request.getParameter("validation");
        try {
            //验证签名得到用户,公钥和时间存入json;
            if (Crypto.verify(signature.getBytes(), message.getBytes(), publicKey.getBytes(), true)) {
                throw new RuntimeException();
            }
            HashMap<String, Object> map = new HashMap<>();
            map.put("time", System.currentTimeMillis());
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
            if (System.currentTimeMillis() - (long) map.get("time") > AUTH_TIME) {
                throw new RuntimeException("token invalid");
            }
            hashMap.remove(token);
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
