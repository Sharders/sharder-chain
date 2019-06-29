package org.conch.mint.pool;

import com.google.common.collect.Maps;
import org.conch.Conch;
import org.conch.common.Constants;
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

/**
 * TODO bad design and coding, need to refactor - ben 20190411
 */
public class PoolRule implements Serializable {
    private static final long serialVersionUID = 7892310437239078209L;
    private Map<String, Long> rule;
    private static Map<String, Object> rules;
    
    private static int[] lifeCycleRule = null;
    private static Map<Role,float[]> rewardRule = Maps.newHashMap();
    private static Map<Role,long[]> investRule = Maps.newHashMap();

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
        SharderPoolProcessor poolProcessor = SharderPoolProcessor.newPoolFromDestroyed(creatorId);
        if (poolProcessor == null) {
            return 0;
        }
        
        int level = 0;
        for (String key : rules.keySet()) {
            if (key.equals("version")) continue;
            
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
            
            if ((long) value <= max && (long) value >= min) {
                return true;
            } else {
                Logger.logDebugMessage("validate mint pool rule failed, min " + min + "max " + max + ", value" + value);
                return false;
            }
        } else if (value instanceof Integer) {
            long min = (long) map.get("min");
            long max = (long) map.get("max");
            if ((int) value <= max && (int) value >= min) {
                return true;
            } else {
                Logger.logDebugMessage("validate mint pool rule failed, min " + min + "max " + max + ", value" + value);
                return false;
            }
        } else {
            float min = (float) map.get("min");
            float max = (float) map.get("max");
            if ((double) value <= max && (double) value >= min) {
                return true;
            } else {
                Logger.logDebugMessage("validate mint pool rule failed, min " + min + "max " + max + ", value" + value);
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
    
    public static Map<String, Object> jsonObjectToMap(com.alibaba.fastjson.JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (Object key : jsonObject.keySet()) {
            Map<String, Object> temp = null;
            if (jsonObject.get(key) instanceof com.alibaba.fastjson.JSONObject) {
                temp = jsonObjectToMap((com.alibaba.fastjson.JSONObject) jsonObject.get(key));
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
            Map<String, Object> field = (Map<String, Object>) ((Map<String, Object>) ruleMap.get("consignor")).get(o);
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

    /**
     * validate accorfing to specified level
     * @param level
     * @param attachment
     * @param ruleInstance
     * @return
     */
    public static boolean validateConsignor(int level, Attachment attachment, Map<String, Object> ruleInstance) {
        // TODO temporary logic to fix pool tx issue, combine the level0 and level1 to one rule object
        Map<String, Object> ruleMap = (Map<String, Object>) ruleInstance.get("level0");
        if(ruleMap == null) {
            ruleMap =  (Map<String, Object>) ruleInstance.get("level1");
        }
        Map<String, Object> consignorMap = (Map<String, Object>) ruleMap.get("consignor");

        for (Field field : attachment.getClass().getDeclaredFields()) {
            if (consignorMap.containsKey(field.getName())) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(attachment);
                    if (!validate((Map<String, Object>)consignorMap.get(field.getName()),value)) {
                        return false;
                    }
                } catch (IllegalAccessException e) {
                    Logger.logErrorMessage("can't access the attachment field when validate attachment rule ", e);
                    return false;
                }
            }
        }
        return true;
    }


    public static boolean validateConsignor(Long creatorId, Attachment attachment, Map<String, Object> ruleInstance) {
        int level = getLevel(creatorId);
        return validateConsignor(level, attachment, ruleInstance);
    }

    public static Map<String, Object> getRules() {
        return rules;
    }


    /**
     * old rewards distribution calculation method
     * don't use it anymore
     * @return
     */
    @Deprecated
    private static Map<Long, Long> oldCalRewardMapAccordingToRules(Long creator, Long poolId, Long amount, Map<Long, Long> investmentMap) {
        Map<Long, Long> result = new HashMap<>();
        if(investmentMap.size() == 0) return result;

        SharderPoolProcessor forgePool = SharderPoolProcessor.getPoolFromAll(creator, poolId);
        if(forgePool == null) return result;
        int level = forgePool.getLevel();

        Map<String, Object> levelRuleMap = (Map<String, Object>) forgePool.getRule().get("level" + level);
        Map<String, Object> ruleMap = (Map<String, Object>) levelRuleMap.get("forgepool");
        Object rewardRule = ((Map<String, Object>) ruleMap.get("reward")).get("max");

        // reward distribution rate of pool
        Float maxRewardRate = null;
        if (rewardRule instanceof Float) {
            maxRewardRate = (Float) rewardRule;
        } else if (rewardRule instanceof Double) {
            maxRewardRate = ((Double) rewardRule).floatValue();
        } else if(rewardRule instanceof String){
            maxRewardRate = Float.parseFloat((String) rewardRule);
        } else if(rewardRule instanceof BigDecimal){
            maxRewardRate = ((BigDecimal) rewardRule).floatValue();
        } else{
            maxRewardRate = Float.parseFloat(rewardRule.toString());
        }

        if(maxRewardRate == null) return result;

        // calculate the creator amount
        long creatorAmount = Math.round(amount * (1 - maxRewardRate));
        result.put(creator, creatorAmount);
        long leftAmount = amount - creatorAmount;

        // sum the total amount according to investment recording in the map
        long totalInvest = 0;
        for (Long value : investmentMap.values()) {
            totalInvest += value;
        }

        BigDecimal totalInvestAmount = BigDecimal.valueOf(totalInvest);
        BigDecimal remainInvestAmount = new BigDecimal(leftAmount);
        // calculate the single investor's rewards
        long totalReward = 0;
        for (Long id : investmentMap.keySet()) {
            BigDecimal investAmount = BigDecimal.valueOf(investmentMap.get(id));
            BigDecimal investRate = investAmount.divide(totalInvestAmount,4,BigDecimal.ROUND_DOWN);

            long reward = remainInvestAmount.multiply(investRate).longValue();
            if (result.containsKey(id)) {
                result.put(id, result.get(id) + reward);
            }else{
                result.put(id, reward);
            }
            totalReward += reward;
        }

        // remain amount distribute to creator
        long remainReward = (amount > totalReward) ? (amount - totalReward) : 0;
        result.put(creator, result.get(creator) + remainReward);

        return result;
    }
    
    public static Map<Long, Long> calRewardMapAccordingToRules(Long creator, Long poolId, Long amount, Map<Long, Long> investmentMap) {
        // temporary codes to avoid block sync error
        if(Constants.isTestnet() && Conch.getHeight() <= Constants.TESTNET_POC_ALGO_HEIGHT){
            return oldCalRewardMapAccordingToRules(creator, poolId, amount, investmentMap);
        }
        
        Map<Long, Long> result = new HashMap<>();
        if(investmentMap.size() == 0) return result;
        
        SharderPoolProcessor forgePool = SharderPoolProcessor.getPoolFromAll(creator, poolId);
        if(forgePool == null) return result;
        int level = forgePool.getLevel();

        Map<String, Object> levelRuleMap = (Map<String, Object>) forgePool.getRule().get("level" + level);
        Map<String, Object> ruleMap = (Map<String, Object>) levelRuleMap.get("forgepool");
        Object rewardRule = ((Map<String, Object>) ruleMap.get("reward")).get("max");
        
        // reward distribution rate of pool
        Float maxRewardRate = null;
        if (rewardRule instanceof Float) {
            maxRewardRate = (Float) rewardRule;
        } else if (rewardRule instanceof Double) {
            maxRewardRate = ((Double) rewardRule).floatValue();
        } else if(rewardRule instanceof String){
            maxRewardRate = Float.parseFloat((String) rewardRule);
        } else if(rewardRule instanceof BigDecimal){
            maxRewardRate = ((BigDecimal) rewardRule).floatValue();
        } else{
            maxRewardRate = Float.parseFloat(rewardRule.toString());
        }

        if(maxRewardRate == null) return result;
        
        // calculate the creator amount
        long poolFees = Float.valueOf(amount * maxRewardRate).longValue();
        result.put(creator, poolFees);
        long distributionRewards = amount - poolFees;
        
        // sum the total amount according to investment recording in the map
        long totalInvest = 0;
        for (Long value : investmentMap.values()) {
            totalInvest += value;
        }
        
        BigDecimal totalInvestAmount = BigDecimal.valueOf(totalInvest);
        BigDecimal distributionRewardsAmount = new BigDecimal(distributionRewards);
        // calculate the single investor's rewards
        long investorTotalRewards = 0;
        for (Long id : investmentMap.keySet()) {
            BigDecimal investAmount = BigDecimal.valueOf(investmentMap.get(id));
            BigDecimal investRate = investAmount.divide(totalInvestAmount,4,BigDecimal.ROUND_DOWN);
            
            long rewards = distributionRewardsAmount.multiply(investRate).longValue();
            if (result.containsKey(id)) {
                result.put(id, result.get(id) + rewards);
            }else{
                result.put(id, rewards);  
            }
            investorTotalRewards += rewards;
        }

        // remain amount distribute to creator
        long remainRewards = (distributionRewards > investorTotalRewards) ? (distributionRewards - investorTotalRewards) : 0;
        result.put(creator, result.get(creator) + remainRewards);

        return result;
    }
    
    public enum Role {
        MINER,
        USER
    }
    
    private static void checkOrLoadPredefinedRules(Role role){
        if(lifeCycleRule == null) lifeCycleRule = loadLifecycleRule();
        
        if(role != null && !rewardRule.containsKey(role)) {
            rewardRule.put(role, loadRewardRateRule(role));
        }
        
        if(role != null && !investRule.containsKey(role)) {
            investRule.put(role, loadInvestmentRule(role));
        }
    }

    private static String parseLevelByRole(Role role){
        if(Role.MINER == role){
            return "level0";
        }else if(Role.USER == role){
            return "level1";
        }
        return "level0";
    }

    private static int[] loadLifecycleRule() {
        // level0 is pool creator
        Map<String, Object> levelMap = (Map<String, Object>) rules.get("level0");
        Map<String, Object> poolMap = (Map<String, Object>) levelMap.get("rule");
        Map<String, Object> lifeMap = (Map<String, Object>) poolMap.get("totalBlocks");
        Long maxLife = (Long) lifeMap.get("max");
        Long minLife = (Long) lifeMap.get("min");
        
        return new int[]{minLife.intValue(),maxLife.intValue()};
    }

    private static float[] loadRewardRateRule(Role role){
        Map<String, Object> levelMap = (Map<String, Object>) rules.get(parseLevelByRole(role));
        Map<String, Object> poolMap = (Map<String, Object>) levelMap.get("forgepool");
        Map<String, Object> lifeMap = (Map<String, Object>) poolMap.get("reward");
        float maxLife = (float) lifeMap.get("max");
        float minLife = (float) lifeMap.get("min");
        return new float[]{minLife,maxLife};
    }

    private static long[] loadInvestmentRule(Role role) {
        Map<String, Object> levelMap = (Map<String, Object>) rules.get(parseLevelByRole(role));
        Map<String, Object> poolMap = (Map<String, Object>) levelMap.get("consignor");
        Map<String, Object> lifeMap = (Map<String, Object>) poolMap.get("amount");
        long maxAmount = (long) lifeMap.get("max");
        long minAmount = (long) lifeMap.get("min");
        return new long[]{minAmount,maxAmount};
    }
    
    /**
     * get the predefined life cycle in rule file
     * @return long[2] - long[0]: min lifecycle, long[1]: max lifecycle
     */
    public static int[] predefinedLifecycle() {
        checkOrLoadPredefinedRules(null);
        return lifeCycleRule;
    }

    /**
     * get the specified role's predefined reward rate in rule file
     * @param role
     * @return float[2] - float[0]: min reward rate, float[1]: max reward rate
     */
    public static float[] predefinedRewardRate(Role role) {
        checkOrLoadPredefinedRules(role);
        return rewardRule.get(role);
    }
    
    /**
     * get the specified role's investment amount in rule file
     * @param role
     * @return long[2] - float[0]: min investment amount, long[1]: max investment amount
     */
    public static long[] predefinedInvestment(Role role) {
        checkOrLoadPredefinedRules(role);
        return investRule.get(role);
    }
    
    public static void main(String[] args) {
        PoolRule.init();

        System.out.println(Arrays.toString(PoolRule.predefinedLifecycle()));
        System.out.println(Arrays.toString(PoolRule.predefinedRewardRate(Role.MINER)));
        System.out.println(Arrays.toString(PoolRule.predefinedRewardRate(Role.USER)));
        System.out.println(Arrays.toString(PoolRule.predefinedInvestment(Role.MINER)));
        System.out.println(Arrays.toString(PoolRule.predefinedInvestment(Role.USER)));
    }
}
