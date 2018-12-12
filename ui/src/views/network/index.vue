<template>
    <div>
        <div>
            <div class="block_network mb20">
                <p class="block_title">
                    <img src="../../assets/img/network.svg"/>
                    <span>{{$t('network.network_title')}}</span>
                </p>
                <div class="w dfl">
                    <div class="block_blue radius_blue">
                        <p>{{$t('network.block_height')}}</p>
                        <p><span>{{newestHeight}}</span></p>
                        <p>{{$t('network.block_newest_time')}}{{newestTime}}</p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>{{$t('network.block_avg_transaction_volume')}}</p>
                        <p><span>{{averageAmount}}</span></p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>{{$t('network.block_peers_volume')}}</p>
                        <p><span>{{peerNum}}</span></p>
                    </div>
                </div>
            </div>
            <div class="mb20 fl">
                <p class="block_title">
                    <img src="../../assets/img/miner.svg"/>
                    <span>{{$t('network.miner_info')}}</span>
                </p>
                <div class="whf xs_section_fa">
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/img/miner-info1.svg"/>
                            <div class="section_info">
                                <span>{{activeCount}}</span>
                                <span>{{$t('network.miner_volume')}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/img/miner-info2.svg"/>
                            <div class="section_info">
                                <span>{{totalCount}}</span>
                                <span>{{$t('network.total_trading_volume')}}</span>
                            </div>
                        </div>

                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/img/miner-info3.svg"/>
                            <div class="section_info">
                                <span>{{transferCount}}</span>
                                <span>{{$t('network.transfer_transaction')}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/img/miner-info4.svg"/>
                            <div class="section_info">
                                <span>{{coinbaseCount}}</span>
                                <span>{{$t('network.coinbase_transaction')}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/img/miner-info5.svg"/>
                            <div class="section_info">
                                <span>{{storageCount}}</span>
                                <span>{{$t('network.store_transaction')}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/img/miner-info6.svg"/>
                            <div class="section_info">
                                <span></span>
                                <span>{{$t('network.alias_modification')}}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="block_peers mb20 fr">
                <p>
                    <span class="block_title fl">
                        <img src="../../assets/img/peerlist.svg"/>
                        <span>{{$t('network.peers_info')}}</span>
                    </span>
                    <span class="hrefbtn fr block_title csp">
                        <a @click="turn2peers">
                            <span>{{$t('network.peers_detail')}}</span>
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
                    <img src="../../assets/img/block.svg"/>
                    <span>{{$t('network.block_list')}}</span>
                </p>
                <div class="list_table w br4">
                    <div class="list_content data_container table_responsive data_loading">
                        <table class="table table_striped" id="blocks_table">
                            <thead>
                                <tr>
                                    <th>{{$t('network.block_list_height')}}</th>
                                    <th class="w200">{{$t('network.block_list_time')}}</th>
                                    <th>{{$t('network.block_list_amount')}}</th>
                                    <th>{{$t('network.block_list_fee')}}</th>
                                    <th>{{$t('network.block_list_transaction')}}</th>
                                    <th class="w200 ">{{$t('network.block_list_generator')}}</th>
                                    <th>{{$t('network.block_list_operating')}}</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(block,index) in blocklist">
                                    <td><span>{{block.height}}</span></td>
                                    <td><span>{{$global.myFormatTime(block.timestamp,'YMDHMS')}}</span></td>
                                    <td><span>{{block.totalAmount}}</span></td>
                                    <td><span>{{block.totalFee}}</span></td>
                                    <td><span>{{block.numberOfTransactions}}</span></td>
                                    <td class="linker" @click="openAccountInfo(block.generatorRS)">{{block.generatorRS}}</td>
                                    <td class="linker" @click="openBlockInfo(block.height)">{{$t('network.view_details')}}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="list_pagination" v-if="totalSize > pageSize">
                        <el-pagination
                            @size-change="handleSizeChange"
                            @current-change="handleCurrentChange"
                            :current-page.sync="currentPage"
                            :page-size="pageSize"
                            layout="total, prev, pager, next, jumper"
                            :total="totalSize">
                        </el-pagination>
                    </div>
                </div>
            </div>
        </div>
        <dialogCommon :accountInfoOpen="accountInfoDialog" :blockInfoOpen="blockInfoDialog" :height="blockInfoHeight" :generatorRS="generatorRS" @isClose="isClose" @openTransaction="openTransaction"></dialogCommon>
    </div>
</template>

<script>
    import echarts from "echarts";
    import world from "echarts-worldmap";
    import dialogCommon from "../dialog/dialog_common";


    export default {
        name: "Network",
        components: { echarts, world, dialogCommon},

        data () {
            return {
                tabTitle: "account",
                //blockinfoDialog
                blockInfoDialog: false,
                blockInfoHeight:-1,
                //accountinfoDialog
                accountInfoDialog:false,
                generatorRS:'',
                //transactionDialog
                transactionId:'',
                transactionDialog:false,
                accountInfo:[],

                //list列表
                blocklist:[],
                //网络总览
                newestHeight:0,
                newestTime:0,
                averageAmount:0,
                peerNum:0,
                //旷工信息
                activeCount:0,
                totalCount:0,
                storageCount:0,
                transferCount:0,
                coinbaseCount:0,
                //分页信息
                currentPage:1,
                totalSize:0,
                pageSize:10,
            };
        },
        created:function(){
            const _this = this;
            this.$http.get('/sharder?requestType=getBizBlocks',{
                params: {
                    firstIndex: (_this.currentPage-1)*10,
                    lastIndex:_this.currentPage * 10 -1
                }
            }).then(function(res){
                _this.blocklist = res.data;
                console.log(_this.blocklist);
                _this.calcAverageAmount(res);
                _this.newestHeight = res.data[0].height;
                _this.totalSize = res.data[0].height;
                _this.newestTime = _this.$global.myFormatTime(res.data[0].timestamp,'YMDHMS');
            }).catch(function (err) {
                console.error("error",err);
            });
            this.$http.get('/sharder?requestType=getPeers').then(function (res) {
                _this.peerNum = res.data.peers.length;
            }).catch(function (err) {
                console.error("error",err);
            });
            this.$http.get('/sharder?requestType=getNextBlockGenerators').then(function (res) {
                _this.activeCount = res.data.activeCount;
            }).catch(function (err) {
                console.error("error",err);
            });
            this.$http.get('/sharder?requestType=getTxStatistics').then(function (res) {
                _this.transferCount = res.data.transferCount;
                _this.storageCount = res.data.storageCount;
                _this.totalCount = _this.transferCount + _this.storageCount + _this.coinbaseCount;
            }).catch(function (err) {
                console.error("error",err);
            });
        },
        methods: {
            handleSizeChange(val) {
                this.getBlockList(val);
            },
            handleCurrentChange(val) {
                this.getBlockList(val);
            },
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

                function makeMapData(rawData) {
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

            getBlockList(currentPage){
                const _this = this;
                this.$http.get('/sharder?requestType=getBizBlocks',{
                    params: {
                        firstIndex: (currentPage-1)*10,
                        lastIndex:currentPage * 10 -1
                    }
                }).then(function(res){
                    _this.blocklist = res.data;
                    _this.calcAverageAmount(res);
                    return res;
                }).catch(function (err) {
                    return null;
                });
            },
            calcAverageAmount(res){
                const _this = this;
                let num = 0;
                res.data.forEach(function(item){
                    num += parseInt(item.totalAmount);
                });
                _this.averageAmount = num/10;
            },
            openBlockInfo(height){
                const _this = this;
                _this.blockInfoHeight = height;
                _this.blockInfoDialog = true;
            },
            openAccountInfo(generatorRS){
                const _this = this;
                _this.generatorRS = generatorRS;
                _this.accountInfoDialog = true;

                _this.transactionDialog = false;
            },
            openTransaction(transactionId,accountInfo){
                const _this = this;
                _this.transactionId = transactionId;
                _this.accountInfo = accountInfo;
                _this.transactionDialog = true;

                console.log(accountInfo);
                _this.accountInfoDialog = false;
            },
            isClose(){
                const _this = this;
                _this.accountInfoDialog = false;
                _this.adminPasswordDialog = false;
                _this.blockInfoDialog = false;
                _this.blockInfoHeight = -1;
                _this.generatorRS = '';

            }
        },
        mounted () {
            this.drawPeers();
        },
    };
</script>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
