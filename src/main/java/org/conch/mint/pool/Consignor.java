package org.conch.mint.pool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.json.simple.JSONArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Consignor implements Serializable {
    private static final long serialVersionUID = 1214235652377897690L;
    private long id;
    private final List<JoinTransaction> transactions = new ArrayList<>();

    public Consignor(){}

    public Consignor(long id, long transactionId, int startBlockNo, int endBlockNo, long amount) {
        JoinTransaction joinTransaction = new JoinTransaction(transactionId, startBlockNo, endBlockNo, amount);
        this.id = id;
        this.transactions.add(joinTransaction);
    }
    
    public boolean hasTx(long txId){
        for (JoinTransaction joinTx : transactions) {
            if (joinTx.getTransactionId() == txId) {
                return true;
            }
        }
        return false;
    }

    public void addTransaction(long transactionId, int startBlockNo, int endBlockNo, long amount) {
        JoinTransaction joinTransaction = new JoinTransaction(transactionId, startBlockNo, endBlockNo, amount);
        this.transactions.add(joinTransaction);
    }

    public boolean removeTransaction(long txId) {
        JoinTransaction joinTransaction = new JoinTransaction(txId, 0, 0, 0);
        if (transactions.contains(joinTransaction)) {
            transactions.remove(joinTransaction);
        }
        if (transactions.size() == 0) {
            return true;
        }
        return false;
    }

    public long getTransactionAmount(long txId) {
        JoinTransaction joinTransaction = new JoinTransaction(txId, 0, 0, 0);
        if (transactions.contains(joinTransaction)) {
            int index = transactions.indexOf(joinTransaction);
            return transactions.get(index).getAmount();
        }
        return -1;
    }

    public long getTransactionEndNo(long txId) {
        JoinTransaction joinTransaction = new JoinTransaction(txId, 0, 0, 0);
        if (transactions.contains(joinTransaction)) {
            int index = transactions.indexOf(joinTransaction);
            return transactions.get(index).getEndBlockNo();
        }
        return -1;
    }

    public long validateHeightAndRemove(int height) {
        long amount = 0;
        List<JoinTransaction> timeout = new ArrayList<>();
        for (JoinTransaction joinTransaction : transactions) {
            if (joinTransaction.getEndBlockNo() == height) {
                timeout.add(joinTransaction);
                amount += joinTransaction.getAmount();
            }
        }
        transactions.remove(timeout);
        return amount;
    }

    public long getAmount() {
        long amount = 0;
        for (JoinTransaction joinTransaction : transactions) {
            amount += joinTransaction.getAmount();
        }
        return amount;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<JoinTransaction> getTransactions() {
        return transactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Consignor consignor = (Consignor) o;

        return id == consignor.id;
    }

    public JSONObject toJsonObj(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
//        jsonObject.put("transactions", transactions);
        JSONArray txArray = new JSONArray();
        for(JoinTransaction joinTx : transactions){
            if(SharderPoolProcessor.hasProcessingQuitTx(joinTx.transactionId) != -1){
                continue;
            }
            txArray.add(joinTx.toJsonObj());
        }
        jsonObject.put("txs", txArray);
        return jsonObject;
    }
    

    public String toJsonStr(){
        return toJsonObj().toString();
    }


    public class JoinTransaction implements Serializable {
        private static final long serialVersionUID = 989675312314345121L;
        private long transactionId;
        private int startBlockNo;
        private int endBlockNo;
        private long amount;

        public JoinTransaction(){}

        public JoinTransaction(long transactionId, int startBlockNo, int endBlockNo, long amount) {
            this.transactionId = transactionId;
            this.startBlockNo = startBlockNo;
            this.endBlockNo = endBlockNo;
            this.amount = amount;
        }

        public long getTransactionId() {
            return transactionId;
        }

        public int getEndBlockNo() {
            return endBlockNo;
        }

        public long getAmount() {
            return amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JoinTransaction joinTransaction = (JoinTransaction) o;

            return transactionId == joinTransaction.getTransactionId();
        }

        public void setTransactionId(long transactionId) {
            this.transactionId = transactionId;
        }

        public int getStartBlockNo() {
            return startBlockNo;
        }

        public void setStartBlockNo(int startBlockNo) {
            this.startBlockNo = startBlockNo;
        }

        public void setEndBlockNo(int endBlockNo) {
            this.endBlockNo = endBlockNo;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }

        public JSONObject toJsonObj(){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("transactionId", String.valueOf(transactionId));
            jsonObject.put("startBlockNo", startBlockNo);
            jsonObject.put("endBlockNo", endBlockNo);
            jsonObject.put("amount", String.valueOf(amount));
            return jsonObject;
        }
        
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static void main(String[] args) {
//        ConcurrentMap<Long, Consignor> consignors = new ConcurrentHashMap<>();
//        consignors.put(1L, new Consignor(1L, 11L, 2, 10, 11111L));
//        consignors.put(2L, new Consignor(2L, 22L, 3, 70, 22222L));
//        JSONObject jsonObjectFromObj = new JSONObject();
//        jsonObjectFromObj.put("consignors", consignors);
//
//        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
//        if(consignors != null && consignors.size() > 0) {
//            org.json.simple.JSONObject consignorJson = new org.json.simple.JSONObject();
//            Set<Long> ids = consignors.keySet();
//            for(Long id : ids){
//                consignorJson.put(id,consignors.get(id).toJsonStr());
//            }
//            jsonObject.put("consignors", consignorJson);
//        }
//
//        System.out.println("Parse from Object =>" + jsonObjectFromObj.toJSONString());
//        System.out.println("Parse from Method =>" + jsonObject.toJSONString());
        String detail = "{" +
                "  \"mintRewards\": 25600000000,\n" +
                "  \"creatorRS\": \"SSA-SRT2-36L7-A85Z-7PTME\",\n" +
                "  \"historicalFees\": 0,\n" +
                "  \"chance\": 1.0,\n" +
                "  \"historicalIncome\": 25700000000,\n" +
                "  \"level\": 0,\n" +
                "  \"updateHeight\": 5,\n" +
                "  \"creatorId\": 6219247923802955552,\n" +
                "  \"rule\": {\n" +
                "    \"level0\": {\n" +
                "      \"forgepool\": {\n" +
                "        \"reward\": {\n" +
                "          \"min\": 0.0,\n" +
                "          \"max\": 0.07\n" +
                "        },\n" +
                "        \"number\": {\n" +
                "          \"min\": 1,\n" +
                "          \"max\": 100\n" +
                "        }\n" +
                "      },\n" +
                "      \"consignor\": {\n" +
                "        \"amount\": {\n" +
                "          \"min\": 1000000000000,\n" +
                "          \"max\": 48000000000000\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"number\": 1,\n" +
                "  \"historicalMintRewards\": 25600000000,\n" +
                "  \"poolId\": 4610428661034380225,\n" +
                "  \"startBlockNo\": 4,\n" +
                "  \"power\": 13122200000000,\n" +
                "  \"endBlockNo\": 7,\n" +
                "  \"state\": 2,\n" +
                "  \"consignors\": {\n" +
                "    6219247923802955552: {\n" +
                "      \"amount\": 11122200000000,\n" +
                "      \"id\": 6219247923802955552,\n" +
                "      \"transactions\": [\n" +
                "        {\n" +
                "          \"amount\": 11122200000000,\n" +
                "          \"endBlockNo\": 406,\n" +
                "          \"startBlockNo\": 6,\n" +
                "          \"transactionId\": -498888686781292537\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  },\n" +
                "  \"historicalBlocks\": 2,\n" +
                "  \"totalBlocks\": 1\n" +
                "}";

        SharderPoolProcessor poolProcessor = JSON.parseObject(detail, SharderPoolProcessor.class);
        System.out.println(poolProcessor.toString());
    }
}
