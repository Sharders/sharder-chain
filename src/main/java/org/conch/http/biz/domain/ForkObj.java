package org.conch.http.biz.domain;
/**
 * 分叉链数据结构：
 * - blockGenerator：该链的最新区块打包者
 * - blocks：包含的区块列表
 * - generates：矿工节点列表
 */
import com.google.common.collect.Lists;
import org.json.simple.JSONObject;
import java.util.List;

/**
 * @author bowen
 * @date 2021/01/06
 */
public class ForkObj {
        String blockGenerator;
        List<JSONObject> blocks;
        List<String> generators = Lists.newArrayList();

        public String getBlockGenerator() {
            return blockGenerator;
        }

        public void setBlockGenerator(String blockGenerator) {
            this.blockGenerator = blockGenerator;
        }

        public List<String> getGenerators() {
            return generators;
        }

        public void setGenerators(List<String> generators) {
            this.generators = generators;
        }

        public void addGenerator(String generator) {
            this.generators.add(generator);
        }

        public void deleteGenerator(String generator) {
            this.generators.remove(generator);
        }

        public List<JSONObject> getBlocks() {
            return blocks;
        }

        public void setBlocks(List<JSONObject> blocks) {
            this.blocks = blocks;
        }

        public ForkObj(String blockGenerator, List<JSONObject> blocks, String generator) {
            this.blockGenerator = blockGenerator;
            this.blocks = blocks;
            this.generators.add(generator);
        }


}
