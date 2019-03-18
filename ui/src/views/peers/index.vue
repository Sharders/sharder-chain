<template>
    <div>
        <div>
            <div class="block_network mb20">
                <p class="block_title csp">
                    <a @click="turn2network">
                        <span>{{$t('peers.return_network')}}</span>
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
            <div class="block_map" id="peers-map">

            </div>
            <div class="block_list">
                <p>
                    <span class="block_title fl">
                        <img src="../../assets/img/peerlist.svg"/>
                        <span>{{$t('peers.peer_list')}}</span>
                    </span>
                    <span class="hrefbtn fr block_title csp">
                        <a @click="openAddPeer">
                            <span>{{$t("network.peers_add")}}</span>
                        </a>
                    </span>
                </p>
                <span class="cb"></span>
                <div class="list_table w br4">
                    <div class="list_content table_responsive data-loading">
                        <table class="table table-striped" id="peers_table">
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
                            <tr v-for="(peer,index) in peersList"
                                v-if="index >= ((currentPage - 1) *10) && index <= (currentPage * 10 -1)">
                                <td class="image_text linker tl" @click="openInfo(peer.address)">
                                    <el-tooltip class="item" placement="top" effect="light" v-if="peer.state === 0"
                                                :content="$t('network.no_connection')">
                                            <span>
                                                <img src="../../assets/img/error.svg"/>
                                                <span>{{peer.address}}</span>
                                            </span>
                                    </el-tooltip>
                                    <el-tooltip class="item" placement="top" effect="light" v-if="peer.state === 1"
                                                :content="$t('network.in_connection')">
                                            <span>
                                            <img src="../../assets/img/success.svg"/>
                                            <span>{{peer.address}}</span>
                                        </span>
                                    </el-tooltip>
                                    <el-tooltip class="item" placement="top" effect="light" v-if="peer.state === 2"
                                                :content="$t('network.disconnect')">
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
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'HALLMARK'" :content="$t('peers.tag_node')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'PRUNABLE'"
                                                        :content="$t('peers.stores_modifiable_messages')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'API'" :content="$t('peers.api_service')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'API_SSL'"
                                                        :content="$t('peers.api_ssl_service')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'CORS'"
                                                        :content="$t('peers.enable_cors_api')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'BAPI'"
                                                        :content="$t('peers.commercial_api_services')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'STORAGE'"
                                                        :content="$t('peers.offline_data_storage')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'MINER'" :content="$t('peers.agent_mining')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'NATER'" :content="$t('peers.nat_server')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>
                                            <el-tooltip class="item" placement="top" effect="light"
                                                        v-if="service === 'PROVER'"
                                                        :content="$t('peers.certificate_services')">
                                                <a>{{service | getPeerServicesLabel}}</a>
                                            </el-tooltip>

                                        </span>


                                </td>
                                <td>
                                    <button class="list_button w40" @click="openConnectPeer(peer.address)">
                                        {{$t('peers.link')}}
                                    </button>
                                    <button class="list_button w50" @click="openBlackDialog(peer.address)">
                                        {{$t('peers.blacklist')}}
                                    </button>
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
                        <button class="close" @click="closeDialog"></button>
                        <h4 class="modal-title">{{$t('peers.join_blacklist')}}</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>{{$t('peers.join_blacklist_tip1')}}{{blacklistPeer}}{{$t('peers.join_blacklist_tip2')}}</p>
                        <p>{{$t('peers.admin_password')}}</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="addBlacklist(blacklistPeer)">{{$t('peers.join')}}
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <!--connect peer-->
        <div class="modal" id="connect_peer_modal" v-show="connectPeerDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog"></button>
                        <h4 class="modal-title">{{$t('peers.link_peer')}}</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>{{$t('peers.peer_name')}}{{connectPeer}}</p>
                        <p>{{$t('peers.admin_password')}}</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="addConnectPeer(connectPeer)">{{$t('peers.link')}}
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <!--add peer-->
        <div class="modal" id="add_peer_modal" v-show="addPeerDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog"></button>
                        <h4 class="modal-title">{{$t("network.peers_add")}}</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>{{$t("mining.binding_account.address")}}：</p>
                        <input v-model="addPeerAddress" type="text">
                        <p class="mt10">{{$t("password_modal.admin_password")}}：</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="addPeer(addPeerAddress,adminPassword)">
                            {{$t("peers.join")}}
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <!--view peer info-->
        <div class="modal_info" id="peer_info" v-show="peerInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/img/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span>{{$t('peers.peer')}}{{peerInfo.address}}</span>
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
                        <td><span v-for="service in peerInfo.services">{{service}}&nbsp;</span></td>
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
    export default {
        name: "Peers",
        data() {
            return {
                //dialog开关
                blacklistDialog: false,
                connectPeerDialog: false,
                peerInfoDialog: false,
                addPeerDialog: false,
                //list列表
                peersList: [],
                //节点总览
                peersCount: 0,
                activeHubCount: 0,
                activePeersCount: 0,
                //分页信息
                currentPage: 1,
                totalSize: 0,
                pageSize: 10,
                //节点详情
                peerInfo: [],
                //黑名单
                blacklistPeer: '',
                //连接节点
                connectPeer: '',
                adminPassword: '',
                //加入节点
                addPeerAddress: '',

                peersLocationList: this.$route.params.peersLocationList,
                peersTimeList: this.$route.params.peersTimeList,
            };
        },
        created: function () {
            let _this = this;
            _this.init(_this.$global.peers.peers);
        },
        methods: {
            init: function (peersList) {
                this.peersList = peersList;
                this.totalSize = peersList.length;
                this.getPeersInfo(peersList);
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
            openAddPeer: function () {
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
                _this.$http.get('/sharder?requestType=getPeer', {
                    params: {
                        peer: address
                    }
                }).then(function (res) {
                    _this.peerInfo = res.data;
                }).catch(function (err) {
                    console.log(err);
                });

                this.$store.state.mask = true;
                this.peerInfoDialog = true;
            },
            getPeersInfo: function (data) {
                let _this = this;
                _this.peersCount = data.length;
                _this.activeHubCount = 0;
                _this.activePeersCount = 0;
                data.forEach(function (item) {
                    if (item.platform === "Sharder Hub") {
                        _this.activeHubCount++;
                    }
                    if (item.state === 1) {
                        _this.activePeersCount++;
                    }
                });
            },
            addPeer: function (address, adminPassword) {
                let _this = this;
                let formData = new FormData();
                formData.append("peer", address);
                formData.append("adminPassword", adminPassword);
                formData.append("feeNQT", 0);

                this.$http.post('sharder?requestType=addPeer', formData).then(function (res) {
                    if (typeof res.data.errorDescription === 'undefined') {
                        _this.$message.success("Add a success！");
                        _this.$global.setPeers(_this).then(res => {
                            _this.init(res.data.peers);
                        });
                    } else {
                        _this.$message.error(res.data.errorDescription);
                    }
                    _this.closeDialog();
                });
            },
            addBlacklist: function (address) {
                let _this = this;
                let formData = new FormData();
                formData.append("peer", address);
                formData.append("adminPassword", _this.adminPassword);

                this.$http.post('sharder?requestType=blacklistPeer', formData).then(function (res) {
                    if (!res.data.errorDescription) {
                        _this.$message({
                            showClose: true,
                            message: _this.$t('peers.join_blacklist_success1') + address + _this.$t('peers.join_blacklist_success2'),
                            type: "success"
                        });
                    } else {
                        _this.$message({
                            showClose: true,
                            message: _this.$t('peers.join_blacklist_error'),
                            type: "error"
                        });
                    }
                    _this.closeDialog();
                }).catch(function (err) {
                    console.log(err);
                });
            },
            addConnectPeer: function (address) {
                let _this = this;
                let formData = new FormData();
                formData.append("peer", address);
                formData.append("adminPassword", _this.adminPassword);
                this.$http.post('sharder?requestType=addPeer', formData).then(function (res) {
                    if (!res.data.errorDescription) {
                        _this.$message({
                            showClose: true,
                            message: _this.$t('peers.join_link_peer_success1') + address + _this.$t('peers.join_link_peer_success2'),
                            type: "success"
                        });
                    } else {
                        _this.$message({
                            showClose: true,
                            message: _this.$t('peers.join_link_peer_error'),
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
        filters: {
            formatByte: function (val) {
                if (val < 1024) {
                    return Math.round(val) + " Byte";
                } else if (val >= 1024) {
                    val = val / 1024;
                    if (val < 1024) {
                        return Math.round(val) + " KB";
                    } else if (val >= 1024) {
                        val = val / 1024;
                        if (val < 1024) {
                            return Math.round(val) + " MB";
                        } else if (val >= 1024) {
                            val = val / 1024;
                            if (val < 1024) {
                                return Math.round(val) + " GB";
                            } else if (val >= 1024) {
                                val = val / 1024;
                                return Math.round(val) + " TB";
                            }
                        }
                    }
                }
            },
            getPeerServicesLabel: function (service) {
                return service.substring(0, 1) + service.substring(service.length - 1);
            },
        },
        mounted() {
            this.$global.drawPeers(this.peersLocationList, this.peersTimeList);
        }
    };
</script>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
<style scoped lang="scss" type="text/css">

</style>
