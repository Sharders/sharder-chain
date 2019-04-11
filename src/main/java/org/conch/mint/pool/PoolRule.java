package org.conch.mint.pool;

import org.conch.Conch;
import org.conch.tx.Attachment;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PoolRule implements Serializable {
    private static final long serialVersionUID = 7892310437239078209L;
    private Map<String, Long> rule;
    private static Map<String, Object> rules;

    public static void init() {
        phraseRule();
    }

    public static void phraseRule() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbFactory.newDocumentBuilder();
            Document ruleFile = db.parse(Conch.getConfDir() + File.separator + "rule-default.xml");
            rules = (Map<String, Object>) toMap(ruleFile.getChildNodes()).get("rules");
        } catch (Exception e) {
            Logger.logErrorMessage("Failed to load rule file ", e);
        }
    }

    private static Map<String, Object> toMap(NodeList nodeList) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Map<String, Object> tempMap = null;
            if (nodeList.item(i).getNodeType() == 1
                    && !nodeList.item(i).getChildNodes().item(0).getNodeValue().contains("\n")) {
                Map<String, Object> map = new HashMap<>();
                result.put(
                        nodeList.item(i).getNodeName(),
                        stringToNumber(nodeList.item(i).getChildNodes().item(0).getNodeValue()));
                continue;
            }
            if (nodeList.item(i).hasChildNodes()) {
                NodeList childs = nodeList.item(i).getChildNodes();
                tempMap = toMap(childs);
            }
            if (tempMap != null && nodeList.item(i).getNodeType() == 3) {
                result.put(nodeList.item(i).getParentNode().getNodeName(), tempMap);
                continue;
            }
            if (tempMap != null) {
                result.put(nodeList.item(i).getNodeName(), tempMap);
            }
        }
        return result;
    }

    private static Object stringToNumber(String s) {
        if (s.contains(".")) {
            return Float.parseFloat(s);
        } else {
            return Long.parseLong(s);
        }
    }

    public static int getLevel(long creatorId) {
        SharderPoolProcessor poolProcessor =
                SharderPoolProcessor.newPoolFromDestroyed(creatorId);
        if (poolProcessor == null) {
            return 0;
        }
        int level = 0;
        for (String key : rules.keySet()) {
            if (key.equals("version")) {
                continue;
            }
            Map<String, Object> objectMap = (Map<String, Object>) rules.get(key);
            Map<String, Object> map = (Map<String, Object>) objectMap.get("rule");
            for (String ruleKey : map.keySet()) {
                try {
                    Field field = poolProcessor.getClass().getDeclaredField(ruleKey);
                    field.setAccessible(true);
                    if (!validate((Map<String, Object>) map.get(field.getName()), field.get(poolProcessor))) {
                        break;
                    }
                    if (Integer.parseInt(key.substring(key.length() - 1, key.length())) > level) {
                        level = Integer.parseInt(key.substring(key.length() - 1, key.length()));
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    Logger.logErrorMessage("can't get account sharder pool level,id " + creatorId, e);
                }
            }
        }
        return level;
    }

    private static boolean validate(Map<String, Object> map, Object value) {
        if (value instanceof Long) {
            long min = (long) map.get("min");
            long max = (long) map.get("max");
            if ((long) value < max && (long) value > min) {
                return true;
            } else {
                Logger.logDebugMessage(
                        "validate mint pool rule failed,min " + min + "max " + max + "value" + value);
                return false;
            }
        } else if (value instanceof Integer) {
            long min = (long) map.get("min");
            long max = (long) map.get("max");
            if ((int) value < max && (int) value > min) {
                return true;
            } else {
                Logger.logDebugMessage(
                        "validate mint pool rule failed,min " + min + "max " + max + "value" + value);
                return false;
            }
        } else {
            float min = (float) map.get("min");
            float max = (float) map.get("max");
            if ((double) value < max && (double) value > min) {
                return true;
            } else {
                Logger.logDebugMessage(
                        "validate mint pool rule failed,min " + min + "max " + max + "value" + value);
                return false;
            }
        }
    }

    public static JSONObject mapToJsonObject(Map<String, Object> map) {
        JSONObject jsonObject = new JSONObject();
        for (String key : map.keySet()) {
            JSONObject temp = null;
            if (map.get(key) instanceof Map) {
                temp = mapToJsonObject((Map<String, Object>) map.get(key));
                jsonObject.put(key, temp);
            } else {
                jsonObject.put(key, map.get(key));
            }
        }
        return jsonObject;
    }

    public static Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (Object key : jsonObject.keySet()) {
            Map<String, Object> temp = null;
            if (jsonObject.get(key) instanceof JSONObject) {
                temp = jsonObjectToMap((JSONObject) jsonObject.get(key));
                map.put((String) key, temp);
            } else {
                map.put((String) key, jsonObject.get(key));
            }
        }
        return map;
    }

    public static JSONObject getTemplate(long creatorId) {
        int level = getLevel(creatorId);
        return mapToJsonObject((Map<String, Object>) rules.get("level" + level));
    }

    public static boolean validateRule(long creatorId, JSONObject jsonObject) {
        int level = getLevel(creatorId);
        Map<String, Object> levelRule = new HashMap<>();
        Map<String, Object> ruleMap = (Map<String, Object>) rules.get("level" + level);
        JSONObject forgePool = (JSONObject) jsonObject.get("forgepool");
        JSONObject consignor = (JSONObject) jsonObject.get("consignor");
        Map<String, Object> levelForgePool = new HashMap<>();
        for (Object o : forgePool.keySet()) {
            Map<String, Object> field = (Map<String, Object>) ((Map<String, Object>) ruleMap.get("forgepool")).get(o);
            if (!validate(field, forgePool.get(o))) {
                return false;
            }
            // modify max value
            // field.put("max",forgePool.get(o));
            // levelForgePool.put((String)o,field);
        }
        levelRule.put("forgepool", levelForgePool);
        Map<String, Object> levelConsignor = new HashMap<>();
        for (Object o : consignor.keySet()) {
            Map<String, Object> field =
                    (Map<String, Object>) ((Map<String, Object>) ruleMap.get("consignor")).get(o);
            if (!validate(field, consignor.get(o))) {
                return false;
            }
            // modify max value
            // field.put("max",consignor.get(o));
            // levelConsignor.put((String)o,field);
        }
        /*levelRule.put("consignor",levelConsignor);
        Map<String,Object> levelRules = new HashMap<>();
        levelRules.put("level" + level,levelRule);
        Map<String,Object> tempRules = new HashMap<>();
        tempRules.put("rules",levelRules);
        rules = tempRules;*/
        return true;
    }

    public static Map<String, Object> getRuleInstance(long creatorId, JSONObject jsonObject) {
        int level = getLevel(creatorId);
        Map<String, Object> levelRule = new HashMap<>();
        Map<String, Object> ruleMap = (Map<String, Object>) rules.get("level" + level);
        JSONObject forgePool = (JSONObject) jsonObject.get("forgepool");
        JSONObject consignor = (JSONObject) jsonObject.get("consignor");
        Map<String, Object> levelForgePool = new HashMap<>();
        for (Object o : forgePool.keySet()) {
            Map<String, Object> field =
                    (Map<String, Object>) ((Map<String, Object>) ruleMap.get("forgepool")).get(o);
            if (!validate(field, forgePool.get(o))) {
                return null;
            }
            // modify max value
            Map<String, Object> newField = new HashMap<>();
            newField.put("max", forgePool.get(o));
            newField.put("min", field.get("min"));
            levelForgePool.put((String) o, newField);
        }
        levelRule.put("forgepool", levelForgePool);
        Map<String, Object> levelConsignor = new HashMap<>();
        for (Object o : consignor.keySet()) {
            Map<String, Object> field =
                    (Map<String, Object>) ((Map<String, Object>) ruleMap.get("consignor")).get(o);
            if (!validate(field, consignor.get(o))) {
                return null;
            }
            // modify max value
            Map<String, Object> newField = new HashMap<>();
            newField.put("max", consignor.get(o));
            newField.put("min", field.get("min"));
            levelConsignor.put((String) o, newField);
        }
        levelRule.put("consignor", levelConsignor);
        Map<String, Object> levelRules = new HashMap<>();
        levelRules.put("level" + level, levelRule);
        return levelRules;
    }

    public static boolean validateConsignor(
            Long creatorId, Attachment attachment, Map<String, Object> ruleInstance) {
        int level = getLevel(creatorId);
        Map<String, Object> ruleMap = (Map<String, Object>) ruleInstance.get("level" + level);

        for (Field field : attachment.getClass().getDeclaredFields()) {
            if (((Map<String, Object>) ruleMap.get("consignor")).containsKey(field.getName())) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(attachment);
                    if (!validate(
                            (Map<String, Object>)
                                    ((Map<String, Object>) ruleMap.get("consignor")).get(field.getName()),
                            value)) {
                        return false;
                    }
                } catch (IllegalAccessException e) {
                    Logger.logErrorMessage(
                            "can't access the attachment field when validate attachment rule ", e);
                    return false;
                }
            }
        }
        return true;
    }

    public static Map<String, Object> getRules() {
        return rules;
    }

    public static Map<Long, Long> getRewardMap(Long creator, Long poolId, Long amount, Map<Long, Long> map) {
        Map<Long, Long> result = new HashMap<>();
        SharderPoolProcessor forgePool = SharderPoolProcessor.getPoolFromAll(creator, poolId);
        int level = forgePool.getLevel();
        Map<String, Object> ruleMap =
                (Map<String, Object>)
                        ((Map<String, Object>) forgePool.getRule().get("level" + level)).get("forgepool");
        long creatorAmount = 0;
        if (((Map<String, Object>) ruleMap.get("reward")).get("max") instanceof Float) {
            creatorAmount =
                    Math.round(
                            amount * (1 - (Float) ((Map<String, Object>) ruleMap.get("reward")).get("max")));
        } else {
            creatorAmount =
                    Math.round(
                            amount * (1 - (Double) ((Map<String, Object>) ruleMap.get("reward")).get("max")));
        }
        result.put(creator, creatorAmount);
        long leftAmount = amount - creatorAmount;
        long total = 0;
        for (Long value : map.values()) {
            total += value;
        }
        for (Long id : map.keySet()) {
//            if (result.containsKey(id)) {
//                result.put(id, result.get(id) + leftAmount * map.get(id) / total);
//            }
//            result.put(id, leftAmount * map.get(id) / total);

            //奖励分发(1);
            long reward = new BigDecimal(leftAmount).multiply(new BigDecimal(map.get(id).toString())).divide(new BigDecimal(total),0,BigDecimal.ROUND_DOWN).longValue();
            if (result.containsKey(id)) {
                result.put(id, result.get(id) + reward);
                continue;
            }
            result.put(id, reward);
        }

        //奖励分发(2);
        long poor = amount;
        for(long p : result.values()){
            poor -= p;
        }
        result.put(creator, result.get(creator) + poor);

        return result;
    }
    
    enum PoolRole {
        MINER,
        USER
    }

    /**
     * get the predefined life cycle in rule file
     * @return long[2] - long[0]: min lifecycle, long[1]: max lifecycle
     */
    public static long[] predefinedLifecycle() {
        // level0 is pool creator
        Map<String, Object> levelMap = (Map<String, Object>) rules.get("level0");
        Map<String, Object> poolMap = (Map<String, Object>) levelMap.get("rule");
        Map<String, Object> lifeMap = (Map<String, Object>) poolMap.get("totalBlocks");
        long maxLife = (long) lifeMap.get("max");
        long minLife = (long) lifeMap.get("min");
        return new long[]{minLife,maxLife};
    }

    /**
     * get the specified role's predefined reward rate in rule file
     * @param role
     * @return float[2] - float[0]: min reward rate, float[1]: max reward rate
     */
    public static float[] predefinedRewardRate(PoolRole role) {
        String level = "level0";
        if(PoolRole.MINER == role){
            level = "level0";
        }else if(PoolRole.USER == role){
            level = "level1"; 
        }

        Map<String, Object> levelMap = (Map<String, Object>) rules.get(level);
        Map<String, Object> poolMap = (Map<String, Object>) levelMap.get("forgepool");
        Map<String, Object> lifeMap = (Map<String, Object>) poolMap.get("reward");
        float maxLife = (float) lifeMap.get("max");
        float minLife = (float) lifeMap.get("min");
        return new float[]{minLife,maxLife};
    }


    public static void main(String[] args) {
        PoolRule.init();

        System.out.println(Arrays.toString(PoolRule.predefinedLifecycle()));
        System.out.println(Arrays.toString(PoolRule.predefinedRewardRate(PoolRole.MINER)));
        System.out.println(Arrays.toString(PoolRule.predefinedRewardRate(PoolRole.USER)));
    }
}
