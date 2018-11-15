<template>
    <div>
        <div class="block-network mb20">
            <p class="block-title">
                <img src="../../assets/network.png"/>
                <span>网络总览</span>
            </p>
            <div class="w dfl">
                <div class="block-blue">
                    <p>区块高度</p>
                    <p><span></span>个</p>
                    <p>生成时间：2018/10/27 11:49:28</p>
                </div>
                <div class="block-blue">
                    <p>区块平均交易量</p>
                    <p><span></span>个</p>
                </div>
                <div class="block-blue">
                    <p>节点数量</p>
                    <p><span></span>个</p>
                </div>
            </div>
        </div>
        <div class="mb20 fl">
            <p class="block-title">
                <img src="../../assets/miner.png"/>
                <span>旷工信息</span>
            </p>
            <div class="whf xs-section-fa">
                <div class="xs-section br4">
                    <div>
                        <img src="../../assets/miner-info1.png"/>
                        <div class="section-info">
                            <span>24</span>
                            <span>旷工数量</span>
                        </div>
                    </div>
                </div>
                <div class="xs-section br4">
                    <div>
                        <img src="../../assets/miner-info2.png"/>
                        <div class="section-info">
                            <span>9999999</span>
                            <span>总交易量</span>
                        </div>
                    </div>

                </div>
                <div class="xs-section br4">
                    <div>
                        <img src="../../assets/miner-info3.png"/>
                        <div class="section-info">
                            <span>1234</span>
                            <span>转账交易</span>
                        </div>
                    </div>
                </div>
                <div class="xs-section br4">
                    <div>
                        <img src="../../assets/miner-info4.png"/>
                        <div class="section-info">
                            <span>123456</span>
                            <span>CoinBase交易</span>
                        </div>
                    </div>
                </div>
                <div class="xs-section br4">
                    <div>
                        <img src="../../assets/miner-info5.png"/>
                        <div class="section-info">
                            <span>12345</span>
                            <span>存储交易</span>
                        </div>
                    </div>
                </div>
                <div class="xs-section br4">
                    <div>
                        <img src="../../assets/miner-info6.png"/>
                        <div class="section-info">
                            <span>45</span>
                            <span>别名修改</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="block-peers mb20 fr">
            <p>
                <span class="block-title fl">
                    <img src="../../assets/peerlist.png"/>
                    <span>节点信息</span>
                </span>
                <span class="hrefbtn fr">
                    <a class="block-title">
                        <sapn>节点详情</sapn>
                    </a>
                </span>
            </p>
            <span class="cb"></span>
            <div class="w600 br4">

            </div>
        </div>
        <div class="block-list">
            <p  class="block-title">
                <img src="../../assets/block.png"/>
                <span>区块列表</span>
            </p>
            <div class="list-table w br4">
                <div class="list-content data-container table-responsive data-loading ">
                    <table class="table table-striped" id="blocks_table">
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
                <div class="list-pagination">
                    <div id="pagination_blocks" class="pagination"></div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "Network",
        components: {}
    }
</script>
<style lang="scss" type="text/scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
