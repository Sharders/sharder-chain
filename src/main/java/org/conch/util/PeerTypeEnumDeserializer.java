package org.conch.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import org.conch.peer.Peer;

import java.lang.reflect.Type;

/**
 * deserialize enum value
 * @author CloudSen
 */
@SuppressWarnings("unchecked")
public class PeerTypeEnumDeserializer implements ObjectDeserializer {
    @Override
    public <T> T deserialze(DefaultJSONParser defaultJSONParser, Type type, Object o) {
        String str = defaultJSONParser.parseObject(String.class);
        JSONObject jsonObject = JSON.parseObject(str);
        String peerTypeName = jsonObject.getString("name");
        return (T) Peer.Type.getTypeByName(peerTypeName);
    }

    @Override
    public int getFastMatchToken() {
        return JSONToken.LITERAL_INT;
    }
}
