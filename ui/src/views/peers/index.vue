<template>
    <div>
        <div>
            <div class="block_network mb20">
                <p class="block_title csp">
                    <a @click="$router.back()">
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
                                <td class="image_text linker" @click="openInfo(peer.address)">
                                    <el-tooltip class="item" placement="top" effect="light"
                                                :content="peerState(peer.state)">
                                            <span class="peer-icon" :class="'icon'+peer.state">
                                                <span>{{peer.address}}</span>
                                            </span>
                                    </el-tooltip>
                                </td>
                                <td>{{formatByte(peer.downloadedVolume)}}</td>
                                <td>{{formatByte(peer.uploadedVolume)}}</td>
                                <td><span class="patch">{{peer.application}}&nbsp;{{peer.version}}</span></td>
                                <td>{{peer.platform}}</td>
                                <td class="linker service">
                                    <el-tooltip v-for="service in peer.services" class="item" placement="top"
                                                effect="light" :content="getPeerServicesString(service)">
                                        <span>{{service}}</span>
                                    </el-tooltip>
                                </td>
                                <td>
                                    <button class="list_button w40" @click="openConnectPeer(peer.address)">
                                        {{$t('peers.link')}}
                                    </button>
                                    <button class="list_button w70" @click="openBlackDialog(peer.address)">
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
                        <button type="button" v-loading="loading" class="btn" @click="addBlacklist(blacklistPeer)">
                            {{$t('peers.join')}}
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
                        <p>{{$t('peers.peer_name')}}</p>
                        <p>{{connectPeer}}</p>
                        <p>{{$t('peers.admin_password')}}</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" v-loading="loading" class="btn" @click="addConnectPeer(connectPeer)">
                            {{$t('peers.link')}}
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
                        <h4 class="modal-title">{{$t("peers.peers_add")}}</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>{{$t("peers.peer_address")}}</p>
                        <input v-model="addPeerAddress" type="text">
                        <p class="mt10">{{$t("peers.admin_password")}}</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" v-loading="loading" class="btn"
                                @click="addPeer(addPeerAddress,adminPassword)">
                            {{$t("peers.join")}}
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <!--view peer info-->
        <div class="modal_info" id="peer_info" v-show="peerInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/img/error.svg" @click="closeDialog"/>
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
                        <td>{{formatByte(peerInfo.downloadedVolume)}}</td>
                    </tr>
                    <tr>
                        <th>Api Port</th>
                        <td>{{peerInfo.apiPort}}</td>
                        <th>{{$t('peers.upload')}}</th>
                        <td>{{formatByte(peerInfo.uploadedVolume)}}</td>
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
                loading: false,
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
                    if (item.platform === "Sharder Hub ") {
                        _this.activeHubCount++;
                    }
                    if (item.state === 1) {
                        _this.activePeersCount++;
                    }
                });
            },
            addPeer: function (address, adminPassword) {
                let _this = this;
                if (!adminPassword || !address) {
                    return _this.$message.warning(_this.$t('rules.plz_input_address_admin_pwd'));
                }
                let formData = new FormData();
                formData.append("peer", address);
                formData.append("adminPassword", adminPassword);
                formData.append("feeNQT", 0);
                _this.loading = true;
                _this.$http.post('/sharder?requestType=addPeer', formData).then(function (res) {
                    _this.loading = false;
                    if (res.data.errorDescription) {
                        return _this.$message.error(res.data.errorDescription);
                    }
                    _this.$message.success("Add a success！");
                    _this.$global.setPeers(_this).then(res => {
                        _this.init(res.data.peers);
                    });
                    _this.closeDialog();
                }).catch(function (err) {
                    _this.loading = false;
                    console.log(err);
                });
            },
            addBlacklist: function (address) {
                let _this = this;
                if (!_this.adminPassword) {
                    return _this.$message.warning(_this.$t('rules.plz_input_admin_pwd'));
                }
                let formData = new FormData();
                formData.append("peer", address);
                formData.append("adminPassword", _this.adminPassword);
                _this.loading = true;
                _this.$http.post('/sharder?requestType=blacklistPeer', formData).then(function (res) {
                    _this.loading = false;
                    if (res.data.errorDescription) {
                        return _this.$message.error(res.data.errorDescription);
                    }
                    _this.$message.success(_this.$t('peers.join_blacklist_success1') + address + _this.$t('peers.join_blacklist_success2'));
                    _this.closeDialog();
                }).catch(function (err) {
                    _this.loading = false;
                    console.log(err);
                });
            },
            addConnectPeer: function (address) {
                let _this = this;
                if (!_this.adminPassword) {
                    return _this.$message.warning(_this.$t('rules.plz_input_admin_pwd'));
                }
                let formData = new FormData();
                formData.append("peer", address);
                formData.append("adminPassword", _this.adminPassword);
                _this.loading = true;
                this.$http.post('/sharder?requestType=addPeer', formData).then(function (res) {
                    _this.loading = false;
                    if (res.data.errorDescription) {
                        return _this.$message.error(res.data.errorDescription);
                    }
                    _this.$message.success(_this.$t('notification.join_link_peer_success1') + address + _this.$t('notification.join_link_peer_success2'));
                    _this.closeDialog();
                }).catch(function (err) {
                    _this.loading = false;
                    console.log(err);
                });
            },
            handleSizeChange(val) {
            },
            handleCurrentChange(val) {
            },
            peerState(state) {
                switch (state) {
                    case 0:
                        return this.$t("network.no_connection");
                    case 1:
                        return this.$t("network.in_connection");
                    case 2:
                        return this.$t("network.disconnect");
                }
            },
            formatByte(val) {
                let unit = [" Byte", " KB", " MB", "  GB", " TB", " PB"];
                let i = 0;
                while (val >= 1024) {
                    val = val / 1024;
                    i++;
                }
                return Math.round(val) + unit[i];
            },
            getPeerServicesString(service) {
                switch (service) {
                    case "HALLMARK":
                        return this.$t('peers.tag_node');
                    case "PRUNABLE":
                        return this.$t('peers.stores_modifiable_messages');
                    case "API":
                        return this.$t('peers.api_service');
                    case "API_SSL":
                        return this.$t('peers.api_ssl_service');
                    case "CORS":
                        return this.$t('peers.enable_cors_api');
                    case "BAPI":
                        return this.$t('peers.commercial_api_services');
                    case "STORAGE":
                        return this.$t('peers.offline_data_storage');
                    case "MINER":
                        return this.$t('peers.agent_mining');
                    case "NATER":
                        return this.$t('peers.nat_server');
                    case "PROVER":
                        return this.$t('peers.certificate_services');
                }
            }
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
