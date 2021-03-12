package org.conch.consensus.genesis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.conch.common.Constants;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-28
 */
public class GenesisRecipient {
    public long id;
    public long amount;
    public byte[] publicKey;
    public byte[] signature;
    public String memo;

    GenesisRecipient(long id, long amount, byte[] publicKey, byte[] signature) {
        this.id = id;
        this.amount = amount;
        this.publicKey = publicKey;
        this.signature = signature;
    }

    public GenesisRecipient() {}

    static final Map<Constants.Network, List<GenesisRecipient>> genesisRecipients = loadGenesisRecipients();
    static Map<Constants.Network, List<GenesisRecipient>>  loadGenesisRecipients () {
        Map<Constants.Network, List<GenesisRecipient>> genesisRecipients = Maps.newHashMap();
        JSONArray jsonArrayDev = SharderGenesis.genesisJsonObj.getJSONArray("devnetRecipients");
        List<GenesisRecipient> devnetRecipients = JSONObject.parseArray(jsonArrayDev.toJSONString(), GenesisRecipient.class);
        JSONArray jsonArrayTest = SharderGenesis.genesisJsonObj.getJSONArray("testnetRecipients");
        List<GenesisRecipient> testnetRecipients = JSONObject.parseArray(jsonArrayTest.toJSONString(), GenesisRecipient.class);
        JSONArray jsonArrayMain = SharderGenesis.genesisJsonObj.getJSONArray("mainnetRecipients");
        List<GenesisRecipient> mainnetRecipients = JSONObject.parseArray(jsonArrayMain.toJSONString(), GenesisRecipient.class);
        genesisRecipients.put(Constants.Network.DEVNET,devnetRecipients);
        genesisRecipients.put(Constants.Network.TESTNET,testnetRecipients);
        genesisRecipients.put(Constants.Network.MAINNET,mainnetRecipients);
        return genesisRecipients;
    }
    
    public static GenesisRecipient getByAccountId(long accountId) {
        for(GenesisRecipient recipient : getAll()){
            if(recipient.id == accountId){
                return recipient;
            }
        }
        return null;
    }

    public static List<GenesisRecipient> getAll(){
        return genesisRecipients.get(Constants.getNetwork());
    }
}