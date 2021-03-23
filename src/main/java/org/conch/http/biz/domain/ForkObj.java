package org.conch.http.biz.domain;

import com.google.common.collect.Lists;
import org.json.simple.JSONObject;
import java.util.List;

/**
 * @author bowen
 * @date 2021/01/06
 */
public class ForkObj {
        String key;
        List<String> peers = Lists.newArrayList();
        List<JSONObject> blocks;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public List<String> getPeers() {
            return peers;
        }

        public void setPeers(List<String> peers) {
            this.peers = peers;
        }

        public void addPeer(String peer) {
            this.peers.add(peer);
        }

        public void deletePeer(String peer) {
            this.peers.remove(peer);
        }

        public List<JSONObject> getBlocks() {
            return blocks;
        }

        public void setBlocks(List<JSONObject> blocks) {
            this.blocks = blocks;
        }

        public ForkObj(String key, List<JSONObject> blocks, String peer) {
            this.key = key;
            this.blocks = blocks;
            this.peers.add(peer);
        }


}
