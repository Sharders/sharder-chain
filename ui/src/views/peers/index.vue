<template>
    <div>
        <div>
            <div class="block_network mb10">
                <p class="block_title csp">
                    <a @click="turn2network">
                        <span>&lt;&lt;返回网络</span>
                    </a>
                </p>
                <div class="w dfl">
                    <div class="block_blue radius_blue">
                        <p>在线节点数</p>
                        <p><span></span>个</p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>HUB运行数</p>
                        <p><span></span>个</p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>活跃节点数</p>
                        <p><span></span>个</p>
                    </div>
                </div>
            </div>
            <div class="block_map"  id="peers-map">

            </div>
            <div class="block_list">
                <p class="block_title">
                    <img src="../../assets/peerlist.svg"/>
                    <span>节点列表</span>
                </p>
                <div class="list_table w br4">
                    <div class="list_content table_responsive data-loading">
                        <table class="table table-striped"  id="peers_table">
                            <thead>
                                <tr>
                                    <th>节点地址</th>
                                    <th>已下载</th>
                                    <th>已上传</th>
                                    <th>应用程序</th>
                                    <th>平台</th>
                                    <th>服务</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td class="image_text linker csp" @click="openInfo">
                                        <img src="../../assets/success.svg"/>
                                        <span>114.115.210.116</span>
                                    </td>
                                    <td>113KB</td>
                                    <td>133KB</td>
                                    <td><span class="patch">COS0.1.0</span></td>
                                    <td>Linux amd64</td>
                                    <td class="linker">
                                        <el-tooltip class="item" content="API服务" placement="top">
                                            <a>AI</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="核心服务" placement="top">
                                            <a>CS</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="商业API" placement="top">
                                            <a>BI</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="存储服务" placement="top">
                                            <a>SE</a>
                                        </el-tooltip>
                                    </td>
                                    <td>
                                        <button class="list_button w40" @click="openConnectPeer">连接</button>
                                        <button class="list_button w50" @click="openBlackDialog">黑名单</button>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="image_text linker csp" @click="openInfo">
                                        <img src="../../assets/success.svg"/>
                                        <span>114.115.210.116</span>
                                    </td>
                                    <td>113KB</td>
                                    <td>133KB</td>
                                    <td><span class="patch">COS0.0.1</span></td>
                                    <td>Linux amd64</td>
                                    <td class="linker">
                                        <el-tooltip class="item" content="API服务" placement="top">
                                            <a>AI</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="核心服务" placement="top">
                                            <a>CS</a>
                                        </el-tooltip>
                                    </td>
                                    <td>
                                        <button class="list_button w40" @click="openConnectPeer">连接</button>
                                        <button class="list_button w50" @click="openBlackDialog">黑名单</button>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="image_text linker csp" @click="openInfo">
                                        <img src="../../assets/success.svg"/>
                                        <span>114.115.210.116</span>
                                    </td>
                                    <td>113KB</td>
                                    <td>133KB</td>
                                    <td><span class="patch">COS0.1.0</span></td>
                                    <td>Linux amd64</td>
                                    <td class="linker tl">
                                        <el-tooltip class="item" content="API服务" placement="top">
                                            <a>AI</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="核心服务" placement="top">
                                            <a>CS</a>
                                        </el-tooltip>
                                        <el-tooltip class="item" content="商业API" placement="top">
                                            <a>BI</a>
                                        </el-tooltip>
                                    </td>
                                    <td>
                                        <button class="list_button w40" @click="openConnectPeer">连接</button>
                                        <button class="list_button w50" @click="openBlackDialog">黑名单</button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <!--blacklist-->
        <div class="modal" id="blacklist_peer_modal" v-show="blacklist">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">加入黑名单</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>是否将节点"114.115.210.116"添加到黑名单？</p>
                        <p>管理密码</p>
                        <input type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn">添加</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal" id="connect_peer_modal" v-show="connectPeer">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">管理密码</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>节点名称：114.115.210.116</p>
                        <p>管理密码</p>
                        <input type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn">连接</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal_info" id="peer_info" v-show="peerInfo">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >节点：114.115.210.116</span>
                </h4>
            </div>
            <div class="modal-body">
                <table class="table">
                    <tbody>
                        <tr>
                            <th>blockchain_state</th>
                            <td>UP_TO_DATE</td>
                            <th>通讯端口</th>
                            <td>8218</td>
                        </tr>
                        <tr>
                            <th>服务</th>
                            <td>API,CORS,BAPI,STORAGE</td>
                            <th>Outbound Web Socket</th>
                            <td>true</td>
                        </tr>
                        <tr>
                            <th>版本</th>
                            <td>COS 0.1.0</td>
                            <th>peer_load</th>
                            <td>[object Object]</td>
                        </tr>
                        <tr>
                            <th>平台</th>
                            <td>Linux amd64</td>
                            <th>Last Connection Attempt</th>
                            <td>2018/10/19 9:47:02</td>
                        </tr>
                        <tr>
                            <th>最后更新</th>
                            <td>2018/10/19 9:47:02</td>
                            <th>状态</th>
                            <td>CONNECTION</td>
                        </tr>
                        <tr>
                            <th>黑名单</th>
                            <td>false</td>
                            <th>共享地址</th>
                            <td>true</td>
                        </tr>
                        <tr>
                            <th>公布的地址</th>
                            <td>114.115.210.116</td>
                            <th>已下载</th>
                            <td>388 KB</td>
                        </tr>
                        <tr>
                            <th>Api Port</th>
                            <td>8215</td>
                            <th>已上传</th>
                            <td>988 KB</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

    </div>

</template>

<!--<span>是否将节点"114.115.210.116"添加到黑名单？</span>-->
            <!--<span>管理密码</span>-->
<script>
    import echarts from "echarts";
    import world from "echarts-worldmap";
    export default {
        name: "Peers",
        components: { echarts, world },
        data () {
            return {
                blacklist: false,
                connectPeer: false,
                peerInfo: false
            };
        },
        methods: {
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
            turn2network: function () {
                this.$router.push("/network");
            },
            openBlackDialog: function () {
                this.$store.state.mask = true;
                this.blacklist = true;
            },
            closeDialog: function () {
                this.$store.state.mask = false;
                this.blacklist = false;
                this.connectPeer = false;
                this.peerInfo = false;
            },
            openConnectPeer: function () {
                this.$store.state.mask = true;
                this.connectPeer = true;
            },
            openInfo: function () {
                this.$store.state.mask = true;
                this.peerInfo = true;
            }
        },
        watch: {
            blacklist: function (val) {
                console.log("变了," + val);
            }
        },
        mounted () {
            this.drawPeers();
        }
    };
</script>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
