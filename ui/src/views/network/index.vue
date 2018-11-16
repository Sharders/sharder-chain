<template>
    <div>
        <div class="block_network mb20">
            <p class="block_title">
                <img src="../../assets/network.svg"/>
                <span>网络总览</span>
            </p>
            <div class="w dfl">
                <div class="block_blue radius_blue">
                    <p>区块高度</p>
                    <p><span></span>个</p>
                    <p>生成时间：2018/10/27 11:49:28</p>
                </div>
                <div class="block_blue radius_blue">
                    <p>区块平均交易量</p>
                    <p><span></span>个</p>
                </div>
                <div class="block_blue radius_blue">
                    <p>节点数量</p>
                    <p><span></span>个</p>
                </div>
            </div>
        </div>
        <div class="mb20 fl">
            <p class="block_title">
                <img src="../../assets/miner.svg"/>
                <span>旷工信息</span>
            </p>
            <div class="whf xs_section_fa">
                <div class="xs_section br4">
                    <div>
                        <img src="../../assets/miner-info1.svg"/>
                        <div class="section_info">
                            <span>24</span>
                            <span>旷工数量</span>
                        </div>
                    </div>
                </div>
                <div class="xs_section br4">
                    <div>
                        <img src="../../assets/miner-info2.svg"/>
                        <div class="section_info">
                            <span>9999999</span>
                            <span>总交易量</span>
                        </div>
                    </div>

                </div>
                <div class="xs_section br4">
                    <div>
                        <img src="../../assets/miner-info3.svg"/>
                        <div class="section_info">
                            <span>1234</span>
                            <span>转账交易</span>
                        </div>
                    </div>
                </div>
                <div class="xs_section br4">
                    <div>
                        <img src="../../assets/miner-info4.svg"/>
                        <div class="section_info">
                            <span>123456</span>
                            <span>CoinBase交易</span>
                        </div>
                    </div>
                </div>
                <div class="xs_section br4">
                    <div>
                        <img src="../../assets/miner-info5.svg"/>
                        <div class="section_info">
                            <span>12345</span>
                            <span>存储交易</span>
                        </div>
                    </div>
                </div>
                <div class="xs_section br4">
                    <div>
                        <img src="../../assets/miner-info6.svg"/>
                        <div class="section_info">
                            <span>45</span>
                            <span>别名修改</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="block_peers mb20 fr">
            <p>
                <span class="block_title fl">
                    <img src="../../assets/peerlist.svg"/>
                    <span>节点信息</span>
                </span>
                <span class="hrefbtn fr block_title csp">
                    <a @click="turn2peers">
                        <span>节点详情</span>
                    </a>
                </span>
            </p>
            <span class="cb"></span>
            <div class="w600 br4">

            </div>
        </div>
        <div class="cb"></div>
        <div class="block_list">
            <p  class="block_title">
                <img src="../../assets/block.svg"/>
                <span>区块列表</span>
            </p>
            <div class="list_table w br4">
                <div class="list_content data_container table_responsive data_loading ">
                    <table class="table table_striped" id="blocks_table">
                        <thead>
                            <tr>
                                <th>高度</th>
                                <th class="w200">出块时间</th>
                                <th>金额</th>
                                <th>手续费</th>
                                <th>交易数</th>
                                <th class="w200 ">出块者</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>1263</td>
                                <td>2018/10/18 17:26:25</td>
                                <td>10</td>
                                <td>0</td>
                                <td>1</td>
                                <td class="linker">SSA-ZR3T-SD2F-V2R2-1E7F4</td>
                                <td class="linker">查看详情</td>
                            </tr>
                            <tr>
                                <td>1263</td>
                                <td>2018/10/18 17:26:25</td>
                                <td>10</td>
                                <td>0</td>
                                <td>1</td>
                                <td class="linker">SSA-ZR3T-SD2F-V2R2-1E7F4</td>
                                <td class="linker">查看详情</td>
                            </tr>
                            <tr>
                                <td>1263</td>
                                <td>2018/10/18 17:26:25</td>
                                <td>10</td>
                                <td>0</td>
                                <td>1</td>
                                <td class="linker">SSA-ZR3T-SD2F-V2R2-1E7F4</td>
                                <td class="linker">查看详情</td>
                            </tr>
                            <tr>
                                <td>1263</td>
                                <td>2018/10/18 17:26:25</td>
                                <td>10</td>
                                <td>0</td>
                                <td>1</td>
                                <td class="linker">SSA-ZR3T-SD2F-V2R2-1E7F4</td>
                                <td class="linker">查看详情</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div class="list_pagination">
                    <div id="pagination_blocks" class="pagination"></div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "Network",
        components: {},
        data () {
            return{

            }
        },
        methods: {
            turn2peers:function () {
                this.$router.push("/network/peers");
            }
        }
    }
</script>
<style lang="scss" type="text/scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
