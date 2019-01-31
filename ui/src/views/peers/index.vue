<template>
    <div>
        <div>
            <div class="block_network mb20">
                <p class="block_title csp">
                    <a @click="turn2network">
                        <span>&lt;&lt;{{$t('peers.return_network')}}</span>
                    </a>
                </p>
                <div class="w dfl">
                    <div class="block_blue radius_blue">
                        <p>{{$t('peers.total_peers')}}</p>
                        <p><span>{{peersCount}}</span></p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>{{$t('peers.active_hub')}}</p>
                        <p><span>{{activeHubCount}}</span></p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>{{$t('peers.active_peers')}}</p>
                        <p><span>{{activePeersCount}}</span></p>
                    </div>
                </div>
            </div>
            <div class="block_map"  id="peers-map">

            </div>
            <div class="block_list">
                <p>
                    <span class="block_title fl">
                        <img src="../../assets/img/peerlist.svg"/>
                        <span>{{$t('peers.peer_list')}}</span>
                    </span>
                    <span class="hrefbtn fr block_title csp">
                        <a @click="openAddPeer">
                            <span>添加节点</span>
                        </a>
                    </span>
                </p>
                <span class="cb"></span>
                <div class="list_table w br4">
                    <div class="list_content table_responsive data-loading">
                        <table class="table table-striped"  id="peers_table">
                            <thead>
                                <tr>
                                    <th>{{$t('peers.peer_address')}}</th>
                                    <th>{{$t('peers.download')}}</th>
                                    <th>{{$t('peers.upload')}}</th>
                                    <th>{{$t('peers.application')}}</th>
                                    <th>{{$t('peers.platform')}}</th>
                                    <th>{{$t('peers.server')}}</th>
                                    <th>{{$t('peers.operating')}}</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(peer,index) in peersList" v-if="index >= ((currentPage - 1) *10) && index <= (currentPage * 10 -1)">
                                    <td class="image_text linker tl" @click="openInfo(peer.address)">
                                        <el-tooltip class="item" placement="top" effect="light" v-if="peer.state === 0" content="无连接">
                                            <span>
                                                <img src="../../assets/img/error.svg"/>
                                                <span>{{peer.address}}</span>
                                            </span>
                                        </el-tooltip>
                                        <el-tooltip class="item" placement="top" effect="light" v-if="peer.state === 1" content="已连接">
                                            <span>
                                            <img src="../../assets/img/success.svg"/>
                                            <span>{{peer.address}}</span>
                                        </span>
                                        </el-tooltip>
                                        <el-tooltip class="item" placement="top" effect="light" v-if="peer.state === 2" content="断开连接">
                                            <span>
                                                <img src="../../assets/img/error.svg"/>
                                                <span>{{peer.address}}</span>
                                            </span>
                                        </el-tooltip>
                                    </td>
                                    <td>{{peer.downloadedVolume | formatByte}}</td>
                                    <td>{{peer.uploadedVolume | formatByte}}</td>
                                    <td><span class="patch">{{peer.application}}&nbsp;{{peer.version}}</span></td>
                                    <td>{{peer.platform}}</td>
                                    <td class="linker ">
                                        <span v-for="service in peer.services">
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'HALLMARK'" content="标记节点">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'PRUNABLE'" content="存储过期的可修改消息">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'API'" content="API服务">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'API_SSL'" content="API SSL服务">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'CORS'" content="启用CORS的API">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'BAPI'" content="商业API服务">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'STORAGE'" content="离线数据存储">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'MINER'" content="代理挖掘">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'NATER'" content="Nat服务">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light" v-if="service === 'PROVER'" content="证明服务">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>

                                        </span>


                                    </td>
                                    <td>
                                        <button class="list_button w40" @click="openConnectPeer(peer.address)">{{$t('peers.link')}}</button>
                                        <button class="list_button w50" @click="openBlackDialog(peer.address)">{{$t('peers.blacklist')}}</button>
                                    </td>
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

        <!--add black list-->
        <div class="modal" id="blacklist_peer_modal" v-show="blacklistDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">{{$t('peers.join_blacklist')}}</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>{{$t('peers.join_blacklist_tip1')}}{{blacklistPeer}}{{$t('peers.join_blacklist_tip2')}}</p>
                        <p>{{$t('peers.admin_password')}}</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="addBlacklist(blacklistPeer)">{{$t('peers.join')}}</button>
                    </div>
                </div>
            </div>
        </div>
        <!--connect peer-->
        <div class="modal" id="connect_peer_modal" v-show="connectPeerDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">{{$t('peers.link_peer')}}</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>{{$t('peers.peer_name')}}{{connectPeer}}</p>
                        <p>{{$t('peers.admin_password')}}</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="addConnectPeer(connectPeer)">{{$t('peers.link')}}</button>
                    </div>
                </div>
            </div>
        </div>
        <!--add peer-->
        <div class="modal" id="add_peer_modal" v-show="addPeerDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">添加节点</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>地址：</p>
                        <input v-model="addPeerAddress" type="text">
                        <p class="mt10">管理密码：</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="addPeer(addPeerAddress,adminPassword)">添加</button>
                    </div>
                </div>
            </div>
        </div>
        <!--view peer info-->
        <div class="modal_info" id="peer_info" v-show="peerInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/img/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >{{$t('peers.peer')}}{{peerInfo.address}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <table class="table">
                    <tbody>
                        <tr>
                            <th>blockchain_state</th>
                            <td>{{peerInfo.blockchainState}}</td>
                            <th>{{$t('peers.communication_port')}}</th>
                            <td>{{peerInfo.port}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('peers.server')}}</th>
                            <td><span  v-for="service in peerInfo.services">{{service}}&nbsp;</span></td>
                            <th>Outbound Web Socket</th>
                            <td>{{peerInfo.outboundWebSocket}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('peers.version')}}</th>
                            <td>{{peerInfo.application}} {{peerInfo.version}}</td>
                            <th>peer_load</th>
                            <td v-if="peerInfo.peerLoad">{{peerInfo.peerLoad.load}}</td>
                            <td v-else></td>
                        </tr>
                        <tr>
                            <th>{{$t('peers.platform')}}</th>
                            <td>{{peerInfo.platform}}</td>
                            <th>Last Connection Attempt</th>
                            <td>{{$global.myFormatTime(peerInfo.lastConnectAttempt,'YMDHMS',true)}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('peers.latest_update')}}</th>
                            <td>{{$global.myFormatTime(peerInfo.lastUpdated,'YMDHMS',true)}}</td>
                            <th>{{$t('peers.status')}}</th>
                            <td v-if="peerInfo.state === 0">NON_CONNECTED</td>
                            <td v-else-if="peerInfo.state === 1">CONNECTED</td>
                            <td v-else-if="peerInfo.state === 2">DISCONNECTED</td>
                        </tr>
                        <tr>
                            <th>{{$t('peers.blacklist')}}</th>
                            <td>{{peerInfo.blacklisted}}</td>
                            <th>{{$t('peers.shared_address')}}</th>
                            <td>{{peerInfo.shareAddress}}</td>
                        </tr>
                        <tr>
                            <th>{{$t('peers.shared_address')}}</th>
                            <td>{{peerInfo.announcedAddress}}</td>
                            <th>{{$t('peers.download')}}</th>
                            <td>{{peerInfo.downloadedVolume | formatByte}}</td>
                        </tr>
                        <tr>
                            <th>Api Port</th>
                            <td>{{peerInfo.apiPort}}</td>
                            <th>{{$t('peers.upload')}}</th>
                            <td>{{peerInfo.uploadedVolume | formatByte}}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</template>
<script>
    import echarts from "echarts";
    import world from "echarts-worldmap";
    export default {
        name: "Peers",
        components: { echarts, world },
        data () {
            return {
                //dialog开关
                blacklistDialog: false,
                connectPeerDialog: false,
                peerInfoDialog: false,
                addPeerDialog:false,
                //list列表
                peersList:[],
                //节点总览
                peersCount:0,
                activeHubCount:0,
                activePeersCount:0,
                //分页信息
                currentPage:1,
                totalSize:0,
                pageSize:10,
                //节点详情
                peerInfo:[],
                //黑名单
                blacklistPeer:'',
                //连接节点
                connectPeer:'',
                adminPassword:'',
                //加入节点
                addPeerAddress:'',

                peersLocationList:this.$route.params.peersLocationList,
                peersTimeList:this.$route.params.peersTimeList,
            };
        },
        created:function(){
            let _this = this;

            _this.peersList = _this.$global.peers.peers;
            _this.totalSize = _this.$global.peers.peers.length;
            _this.getPeersInfo(_this.peersList);
        },
        methods: {
            drawPeers: function () {
                let _this = this;
                const myChart = echarts.init(document.getElementById("peers-map"));

                function makeMapData (rawData) {
                    const mapData = [];
                    for (let i = 0; i < rawData.length; i++) {
                        const geoCoord = _this.peersLocationList[rawData[i][0]];
                        if (geoCoord) {
                            mapData.push({
                                name: rawData[i][0],
                                value: geoCoord
                            });
                        }
                    }
                    return mapData;
                }

                const option = {
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
                        roam: false
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
                                show: false
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
                            data: makeMapData(_this.peersTimeList),
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
            openBlackDialog: function (address) {
                let _this = this;
                this.closeDialog();
                _this.blacklistPeer = address;
                this.$store.state.mask = true;
                this.blacklistDialog = true;
            },
            closeDialog: function () {
                this.$store.state.mask = false;
                this.blacklistDialog = false;
                this.connectPeerDialog = false;
                this.addPeerDialog = false;
                this.peerInfoDialog = false;
                this.blacklist = '';
                this.connectPeer = '';
                this.adminPassword = '';
                this.addPeerAddress = '';

            },
            openAddPeer:function(){
                let _this = this;
                _this.closeDialog();
                _this.$store.state.mask = true;
                _this.addPeerDialog = true;
            },
            openConnectPeer: function (address) {
                let _this = this;
                this.closeDialog();
                _this.connectPeer = address;
                this.$store.state.mask = true;
                this.connectPeerDialog = true;
            },
            openInfo: function (address) {
                let _this = this;
                this.closeDialog();
                _this.$http.get('/sharder?requestType=getPeer',{
                    params:{
                        peer:address
                    }
                }).then(function (res) {
                    _this.peerInfo = res.data;
                }).catch(function (err) {
                    console.log(err);
                });

                this.$store.state.mask = true;
                this.peerInfoDialog = true;
            },
            getPeersInfo:function (data) {
                let _this = this;
                _this.peersCount = data.length;
                data.forEach(function(item){
                    if(item.type === 4){
                        _this.activeHubCount++;
                    }
                    if(item.state === 1){
                        _this.activePeersCount++;
                    }
                });
            },
            addPeer:function(address,adminPassword){
                let _this = this;
                let formData = new FormData();
                formData.append("peer",address);
                formData.append("adminPassword",adminPassword);
                formData.append("feeNQT",0);

                this.$http.post('sharder?requestType=addPeer',formData).then(function (res) {
                    if(typeof res.data.errorDescription === 'undefined'){
                        _this.$message.success("添加成功！");

                        _this.$global.setPeers(_this).then(res=>{
                            _this.peersList = res.data;
                        });
                    }else{
                        _this.$message.error(res.data.errorDescription);
                    }
                })
            },
            addBlacklist:function(address){
                let _this = this;
                let formData = new FormData();
                formData.append("peer",address);
                formData.append("adminPassword",_this.adminPassword);

                this.$http.post('sharder?requestType=blacklistPeer',formData).then(function (res) {
                    if(typeof res.data.errorDescription === 'undefined'){
                        _this.$message({
                            showClose: true,
                            message: this.$t('peers.join_blacklist_success1')+address+this.$t('peers.join_blacklist_success2'),
                            type: "success"
                        });
                    }else{
                        _this.$message({
                            showClose: true,
                            message: this.$t('peers.join_blacklist_error'),
                            type: "error"
                        });
                    }
                    _this.closeDialog();
                }).catch(function (err) {
                    console.log(err);
                });
            },
            addConnectPeer:function(address){
                let _this = this;
                let formData = new FormData();
                formData.append("peer",address);
                formData.append("adminPassword",_this.adminPassword);
                this.$http.post('sharder?requestType=addPeer',formData).then(function (res) {
                    if(typeof res.data.errorDescription === 'undefined'){
                        _this.$message({
                            showClose: true,
                            message: this.$t('peers.join_link_peer_success1')+address+this.$t('peers.join_link_peer_success2'),
                            type: "success"
                        });
                    }else{
                        _this.$message({
                            showClose: true,
                            message: this.$t('peers.join_link_peer_error'),
                            type: "error"
                        });
                    }
                    _this.closeDialog();
                }).catch(function (err) {
                    console.log(err);
                });
            },
            handleSizeChange(val) {
            },
            handleCurrentChange(val) {
            },
        },
        filters:{
            formatByte:function (val) {
                if(val < 1024){
                    return Math.round(val) + " Byte";
                }else if(val>=1024){
                    val = val/1024;
                    if(val < 1024){
                        return Math.round(val) + " KB";
                    }else if(val>=1024){
                        val = val/1024;
                        if(val < 1024){
                            return Math.round(val) + " MB";
                        }else if(val>=1024){
                            val = val/1024;
                            if(val < 1024){
                                return Math.round(val) + " GB";
                            }else if(val>=1024){
                                val = val/1024;
                                return Math.round(val) + " TB";
                            }
                        }
                    }
                }
            },
            getPeerServicesLabel:function (service) {
                return service.substring(0, 1) + service.substring(service.length - 1);
            },
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
<style scoped lang="scss" type="text/css">

</style>
