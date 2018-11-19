<template>
    <div>
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
                <div class="w600 br4" id="peers-map">

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
                                    <td class="linker" @click="openAccountInfo">SSA-ZR3T-SD2F-V2R2-1E7F4</td>
                                    <td class="linker" @click="openBlockInfo">查看详情</td>
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

        <div class="modal_info" id="block_info" v-show="blockInfo">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >区块：1221 信息</span>
                </h4>
            </div>
            <div class="modal-body">
                <el-radio-group v-model="tabTitle" class="title">
                    <el-radio-button label="account" class="btn">所有交易</el-radio-button>
                    <el-radio-button label="blockInfo" class="btn">区块详情</el-radio-button>
                </el-radio-group>

                <div v-if="tabTitle === 'account'" class="account_list">
                    <table class="table">
                        <tbody>
                            <tr>
                                <th>时间</th>
                                <th>类型</th>
                                <th>数量</th>
                                <th>手续费</th>
                                <th>发送者</th>
                                <th>接受者</th>
                            </tr>
                            <tr>
                                <td>2018/10/18 8:29:16</td>
                                <td>
                                    <img src="../../assets/coinBase.svg"/>
                                    <span>CoinBase</span>
                                </td>
                                <td>10</td>
                                <td>0</td>
                                <td>CoinBase</td>
                                <td class="linker">SSA-DEUD-WXFN-AZ8H-BPKX</td>
                            </tr>
                            <tr>
                                <td>2018/10/18 8:29:16</td>
                                <td>
                                    <img src="../../assets/pay.svg"/>
                                    <span>普通支付</span>
                                </td>
                                <td>100,000</td>
                                <td>1</td>
                                <td class="linker">SSA-9WKZ-DV7P-M6MN-5MH8B</td>
                                <td class="linker">SSA-DEUD-WXFN-AZ8H-BPKX</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div v-if="tabTitle === 'blockInfo'" class="blockInfo">
                    <table class="table">
                        <tbody>
                            <tr>
                                <th>上一个区块哈希</th>
                                <td></td>
                            </tr>
                            <tr>
                                <th>载荷长度</th>
                                <td>193</td>
                            </tr>
                            <tr>
                                <th>总数</th>
                                <td>10 SS</td>
                            </tr>
                            <tr>
                                <th>矿工签名</th>
                                <td></td>
                            </tr>
                            <tr>
                                <th>矿工公钥</th>
                                <td></td>
                            </tr>
                            <tr>
                                <th>交易数量</th>
                                <td>1</td>
                            </tr>
                            <tr>
                                <th>区块签名</th>
                                <td></td>
                            </tr>
                            <tr>
                                <th>版本：</th>
                                <td>3</td>
                            </tr>
                            <tr>
                                <th>总手续费</th>
                                <td>0 SS</td>
                            </tr>
                            <tr>
                                <th>挖矿难度</th>
                                <td></td>
                            </tr>
                            <tr>
                                <th>区块高度</th>
                                <td>1216</td>
                            </tr>
                            <tr>
                                <th>时间戳</th>
                                <td></td>
                            </tr>
                            <tr>
                                <th>矿工</th>
                                <td></td>
                            </tr>
                            <tr>
                                <th>上一个区块</th>
                                <td>1220</td>
                            </tr>
                            <tr>
                                <th>下一个区块</th>
                                <td>122</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>


        <div class="modal_info" id="account_info" v-show="accountInfo">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >账户：SSA-9WKZ-DV7P-M6MN-5MH8B 信息</span>
                </h4>
            </div>
            <div class="modal-body">
                <div class="account_preInfo">
                    <span>账户命名&nbsp;</span><span>Kuhoerk</span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>可用资金&nbsp;</span><span>1,000,046&nbsp;SS</span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>别名：</span><span>无</span>
                </div>
                <div class="account_allInfo">
                    <el-radio-group v-model="tabTitle" class="title">
                        <el-radio-button label="account" class="btn">所有交易</el-radio-button>
                    </el-radio-group>

                    <div v-if="tabTitle === 'account'" class="account_list">
                        <table class="table">
                            <tbody>
                                <tr>
                                    <th>交易时间</th>
                                    <th>交易类型</th>
                                    <th>数量</th>
                                    <th>手续费</th>
                                    <th>账户</th>
                                    <th>操作</th>
                                </tr>
                                <tr>
                                    <td>2018/10/18 8:29:16</td>
                                    <td>
                                        <img src="../../assets/coinBase.svg"/>
                                        <span>CoinBase</span>
                                    </td>
                                    <td>10</td>
                                    <td>0</td>
                                    <td class="linker">SSA-DEUD-WXFN-AZ8H-BPKX</td>
                                    <td class="linker">查看详情</td>
                                </tr>
                                <tr>
                                    <td>2018/10/18 8:29:16</td>
                                    <td>
                                        <img src="../../assets/pay.svg"/>
                                        <span>普通支付</span>
                                    </td>
                                    <td>100,000</td>
                                    <td>1</td>
                                    <td class="linker" @click="openAccountInfo">SSA-DEUD-WXFN-AZ8H-BPKX</td>
                                    <td class="linker" @click="openAccountTransaction">查看详情</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal_info" id="account_transaction" v-show="accountTransaction">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >账户：SSA-9WKZ-DV7P-M6MN-5MH8B 信息</span>
                </h4>
            </div>
            <div class="modal-body">
                <div class="account_preInfo">
                    <span>账户命名&nbsp;</span><span>Kuhoerk</span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>可用资金&nbsp;</span><span>1,000,046&nbsp;SS</span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>别名：</span><span>无</span>
                </div>
                <div class="account_transactionInfo">
                    <p class="fl">交易详情</p>
                    <button class="fr common_btn" @click="openAccountInfo">返回账户信息</button>
                    <div class="cb"></div>
                    <table class="table">
                        <tbody>
                        <tr>
                            <th>上一个区块哈希</th>
                            <td></td>
                        </tr>
                        <tr>
                            <th>载荷长度</th>
                            <td>193</td>
                        </tr>
                        <tr>
                            <th>总数</th>
                            <td>10 SS</td>
                        </tr>
                        <tr>
                            <th>矿工签名</th>
                            <td></td>
                        </tr>
                        <tr>
                            <th>矿工公钥</th>
                            <td></td>
                        </tr>
                        <tr>
                            <th>交易数量</th>
                            <td>1</td>
                        </tr>
                        <tr>
                            <th>区块签名</th>
                            <td></td>
                        </tr>
                        <tr>
                            <th>版本：</th>
                            <td>3</td>
                        </tr>
                        <tr>
                            <th>总手续费</th>
                            <td>0 SS</td>
                        </tr>
                        <tr>
                            <th>挖矿难度</th>
                            <td></td>
                        </tr>
                        <tr>
                            <th>区块高度</th>
                            <td>1216</td>
                        </tr>
                        <tr>
                            <th>时间戳</th>
                            <td></td>
                        </tr>
                        <tr>
                            <th>矿工</th>
                            <td></td>
                        </tr>
                        <tr>
                            <th>上一个区块</th>
                            <td>1220</td>
                        </tr>
                        <tr>
                            <th>下一个区块</th>
                            <td>122</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>

        </div>
    </div>
</template>

<script>
    import echarts from "echarts";
    import world from "echarts-worldmap";
    export default {
        name: "Network",
        components: { echarts, world },
        data () {
            return {
                tabTitle: 'account',
                blockInfo:false,
                accountInfo:false,
                accountTransaction:false
            };
        },
        methods: {
            turn2peers: function () {
                this.$router.push("/network/peers");
            },
            drawPeers: function () {
                const myChart = echarts.init(document.getElementById("peers-map"));

                const geoCoordMap = {
                    "Amsterdam": [4.895168, 52.370216],
                    "Athens": [-83.357567, 33.951935],
                    "Auckland": [174.763332, -36.84846],
                    "Bangkok": [100.501765, 13.756331],
                    "Barcelona": [2.173403, 41.385064],
                    "Beijing": [116.407395, 39.904211],
                    "Berlin": [13.404954, 52.520007],
                    "Bogotá": [-74.072092, 4.710989],
                    "Bratislava": [17.107748, 48.148596],
                    "Brussels": [4.35171, 50.85034],
                    "Budapest": [19.040235, 47.497912],
                    "Buenos Aires": [-58.381559, -34.603684],
                    "Bucharest": [26.102538, 44.426767],
                    "Caracas": [-66.903606, 10.480594],
                    "Chicago": [-87.629798, 41.878114],
                    "Delhi": [77.209021, 28.613939],
                    "Doha": [51.53104, 25.285447],
                    "Dubai": [55.270783, 25.204849],
                    "Dublin": [-6.26031, 53.349805],
                    "Frankfurt": [8.682127, 50.110922],
                    "Geneva": [6.143158, 46.204391],
                    "Helsinki": [24.938379, 60.169856],
                    "Hong Kong": [114.109497, 22.396428],
                    "Istanbul": [28.978359, 41.008238],
                    "Jakarta": [106.845599, -6.208763],
                    "Johannesburg": [28.047305, -26.204103],
                    "Cairo": [31.235712, 30.04442],
                    "Kiev": [30.5234, 50.4501],
                    "Copenhagen": [12.568337, 55.676097],
                    "Kuala Lumpur": [101.686855, 3.139003],
                    "Lima": [-77.042793, -12.046374],
                    "Lisbon": [-9.139337, 38.722252],
                    "Ljubljana": [14.505751, 46.056947],
                    "London": [-0.127758, 51.507351],
                    "Los Angeles": [-118.243685, 34.052234],
                    "Luxembourg": [6.129583, 49.815273],
                    "Lyon": [4.835659, 45.764043],
                    "Madrid": [-3.70379, 40.416775],
                    "Milan": [9.185924, 45.465422],
                    "Manama": [50.58605, 26.228516],
                    "Manila": [120.984219, 14.599512],
                    "Mexico City": [-99.133208, 19.432608],
                    "Miami": [-80.19179, 25.76168],
                    "Montreal": [-73.567256, 45.501689],
                    "Moscow": [37.6173, 55.755826],
                    "Mumbai": [72.877656, 19.075984],
                    "Munich": [11.581981, 48.135125],
                    "Nairobi": [36.821946, -1.292066],
                    "New York": [-74.005941, 40.712784],
                    "Nicosia": [33.382276, 35.185566],
                    "Oslo": [10.752245, 59.913869],
                    "Paris": [2.352222, 48.856614],
                    "Prague": [14.4378, 50.075538],
                    "Riga": [24.105186, 56.949649],
                    "Rio de Janeiro": [-43.172896, -22.906847],
                    "Rome": [12.496366, 41.902783],
                    "Santiago de Chile": [-70.669265, -33.44889],
                    "São Paulo": [-46.633309, -23.55052],
                    "Seoul": [126.977969, 37.566535],
                    "Shanghai": [121.473701, 31.230416],
                    "Singapore": [103.819836, 1.352083],
                    "Sofia": [23.321868, 42.697708],
                    "Stockholm": [18.068581, 59.329323],
                    "Sydney": [151.209296, -33.86882],
                    "Taipei": [121.565418, 25.032969],
                    "Tallinn": [24.753575, 59.436961],
                    "Tel Aviv": [34.781768, 32.0853],
                    "Tokyo": [139.691706, 35.689487],
                    "Toronto": [-79.383184, 43.653226],
                    "Vilnius": [25.279651, 54.687156],
                    "Warsaw": [21.012229, 52.229676],
                    "Vienna": [16.373819, 48.208174],
                    "Zurich": [8.541694, 47.376887]
                };

                const rawData = [
                    ["Amsterdam", 101.6],
                    ["Athens", 62.6],
                    ["Auckland", 77.9],
                    ["Bangkok", 26.4],
                    ["Barcelona", 79.7],
                    ["Beijing", 28.2],
                    ["Berlin", 109.7],
                    ["Bogotá", 41.4],
                    ["Bratislava", 51.3],
                    ["Brussels", 107.5],
                    ["Budapest", 35.5],
                    ["Buenos Aires", 42.9],
                    ["Bucharest", 37.1],
                    ["Caracas", 21.9],
                    ["Chicago", 105.3],
                    ["Delhi", 23],
                    ["Doha", 38.8],
                    ["Dubai", 63.5],
                    ["Dublin", 101.9],
                    ["Frankfurt", 102.2],
                    ["Geneva", 116],
                    ["Helsinki", 93],
                    ["Hong Kong", 58.5],
                    ["Istanbul", 39],
                    ["Jakarta", 14.7],
                    ["Johannesburg", 80.6],
                    ["Cairo", 26.2],
                    ["Kiev", 19.5],
                    ["Copenhagen", 122],
                    ["Kuala Lumpur", 41.1],
                    ["Lima", 43.6],
                    ["Lisbon", 65.3],
                    ["Ljubljana", 57.5],
                    ["London", 91.2],
                    ["Los Angeles", 113.8],
                    ["Luxembourg", 111.6],
                    ["Lyon", 81.8],
                    ["Madrid", 83.6],
                    ["Milan", 88.2],
                    ["Manama", 56.4],
                    ["Manila", 19.2],
                    ["Mexico City", 26.8],
                    ["Miami", 106.2],
                    ["Montreal", 93.1],
                    ["Moscow", 45.1],
                    ["Mumbai", 24.9],
                    ["Munich", 108.3],
                    ["Nairobi", 21.4],
                    ["New York", 100],
                    ["Nicosia", 95],
                    ["Oslo", 102.7],
                    ["Paris", 94.8],
                    ["Prague", 45.1],
                    ["Riga", 44.3],
                    ["Rio de Janeiro", 44.4],
                    ["Rome", 69.6],
                    ["Santiago de Chile", 42.8],
                    ["São Paulo", 48.7],
                    ["Seoul", 80.8],
                    ["Shanghai", 37.2],
                    ["Singapore", 50.8],
                    ["Sofia", 32.6],
                    ["Stockholm", 90.2],
                    ["Sydney", 112.5],
                    ["Taipei", 52],
                    ["Tallinn", 47.9],
                    ["Tel Aviv", 57],
                    ["Tokyo", 84.7],
                    ["Toronto", 103.4],
                    ["Vilnius", 42.6],
                    ["Warsaw", 44.3],
                    ["Vienna", 100.6],
                    ["Zurich", 119.1]
                ];

                function makeMapData (rawData) {
                    const mapData = [];
                    for (let i = 0; i < rawData.length; i++) {
                        const geoCoord = geoCoordMap[rawData[i][0]];
                        if (geoCoord) {
                            mapData.push({
                                name: rawData[i][0],
                                value: geoCoord.concat(rawData[i].slice(1))
                            });
                        }
                    }
                    return mapData;
                }

                const option = {
                    tooltip: {
                        trigger: "item",
                        formatter: function (params) {
                            let value = (params.value + "").split(".");
                            value = value[0].replace(/(\d{1,3})(?=(?:\d{3})+(?!\d))/g, "$1,") + "." + value[1];
                            return params.seriesName + "<br/>" + params.name + " : " + value;
                        }
                    },
                    geo: {
                        map: "world",
                        silent: true,
                        label: {
                            emphasis: {
                                show: true,
                                areaColor: "#eceef1"
                            }
                        },
                        itemStyle: {
                            normal: {
                                borderWidth: 1,
                                borderColor: "#fff"
                            }
                        },
                        left: 0,
                        top: 0,
                        bottom: 0,
                        right: 0,
                        roam: true
                    },
                    parallel: {
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        parallelAxisDefault: {
                            type: "value",
                            nameLocation: "start",
                            nameTextStyle: {
                                fontSize: 12
                            },
                            nameGap: 20,
                            splitNumber: 3,
                            tooltip: {
                                show: true
                            },
                            axisLine: {
                                show: true,
                                lineStyle: {
                                    width: 1,
                                    color: "rgba(255,255,255,0.3)"
                                }
                            },
                            axisTick: {
                                show: true
                            },
                            splitLine: {
                                show: true
                            },
                            z: 100
                        }
                    },
                    series: [
                        {
                            name: "Prices and Earnings 2012",
                            type: "scatter",
                            coordinateSystem: "geo",
                            symbolSize: 8,
                            data: makeMapData(rawData),
                            activeOpacity: 1,
                            label: {
                                normal: {
                                    formatter: "{b}",
                                    position: "right",
                                    show: false
                                },
                                emphasis: {
                                    show: true
                                }
                            },
                            itemStyle: {
                                normal: {
                                    borderColor: "#fff",
                                    color: "#577ceb"
                                }
                            }
                        }
                    ]
                };

                myChart.setOption(option);
            },
            openBlockInfo:function () {
                this.closeDialog();
                this.$store.state.mask = true;
                this.blockInfo = true;
            },
            openAccountInfo:function(){
                this.closeDialog();
                this.$store.state.mask = true;
                this.accountInfo = true;
            },
            openAccountTransaction:function(){
                this.closeDialog();
                this.$store.state.mask = true;
                this.accountTransaction = true;
            },
            closeDialog:function(){
                this.$store.state.mask = false;
                this.blockInfo = false;
                this.accountInfo = false;
                this.accountTransaction = false;
            }
        },
        mounted () {
            this.drawPeers();
        }
    };
</script>
<style lang="scss" type="text/scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
