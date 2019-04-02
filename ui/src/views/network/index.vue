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
            <div class="block_peers mb20 fl">
                <p>
                    <span class="block_title fl">
                        <img src="../../assets/img/miner.svg"/>
                        <span>{{$t('network.miner_info')}}</span>
                    </span>
                    <span class="hrefbtn fr block_title csp mr5">
                        <a @click="openMinerList">
                            <span>{{$t('network.miners_list')}}</span>
                        </a>
                    </span>
                </p>
                <div class="trading_situation">
                    <div class="trading_situation_info">
                        <div>
                            <img src="../../assets/img/miner-info1.svg"/>
                            <div class="section_info">
                                <span>{{activeCount}}</span>
                                <span>{{$t('network.miner_volume')}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="trading_situation_info">
                        <div>
                            <img src="../../assets/img/miner-info2.svg"/>
                            <div class="section_info">
                                <span>{{totalCount}}</span>
                                <span>{{$t('network.total_trading_volume')}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="trading_situation_info">
                        <div>
                            <img src="../../assets/img/miner-info3.svg"/>
                            <div class="section_info">
                                <span>{{transferCount}}</span>
                                <span>{{$t('network.transfer_transaction')}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="trading_situation_info">
                        <div>
                            <img src="../../assets/img/miner-info4.svg"/>
                            <div class="section_info">
                                <span>{{poolCount}}</span>
                                <span>{{$t('network.coinbase_transaction')}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="trading_situation_info">
                        <div>
                            <img src="../../assets/img/miner-info5.svg"/>
                            <div class="section_info">
                                <span>{{storageCount}}</span>
                                <span>{{$t('network.store_transaction')}}</span>
                            </div>
                        </div>
                    </div>
                    <div class="trading_situation_info">
                        <div>
                            <img src="../../assets/img/miner-info6.svg"/>
                            <div class="section_info">
                                <span>{{systemReward}}</span>
                                <span>{{$t('network.system_reward')}}</span>
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
                <p class="block_title">
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
                                <td class="pl0"><span>{{block.height}}</span></td>
                                <td><span>{{$global.myFormatTime(block.timestamp,'YMDHMS',true)}}</span></td>
                                <td>
                                    <span>{{$global.getBlocKTotalAmountNQT(block.totalAmountNQT)}}</span>
                                </td>
                                <td>
                                    <span>{{$global.getBlockTotalFeeNQT(block.totalFeeNQT)}}</span>
                                </td>
                                <td><span>{{block.numberOfTransactions}}</span></td>
                                <td class="linker" @click="openAccountInfo(block.generatorRS)">{{block.generatorRS}}
                                </td>
                                <td class="linker" @click="openBlockInfo(block.height)">{{$t('network.view_details')}}
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="list_pagination">
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
        <div class="modal modal-w900" id="miner_list" v-show="minerlistDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog"></button>
                        <h4 class="modal-title">{{$t("network.miners_roll")}}</h4>
                    </div>
                    <div class="modal-body modal-miner">
                        <el-table
                            :data="minerlist"
                            style="width: 100%">
                            <el-table-column type="expand">
                                <template slot-scope="props">
                                    <el-form label-position="left" inline>
                                        <div class="hidden-md-and-up">
                                            <el-row>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_ss')">
                                                        <span>{{ props.row.detailedPocScore.ssScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_node_type')">
                                                        <span>{{ props.row.detailedPocScore.nodeTypeScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
                                            <el-row>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_server')">
                                                        <span>{{ props.row.detailedPocScore.serverScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_hardware')">
                                                        <span>{{ props.row.detailedPocScore.hardwareScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
                                            <el-row>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_network')">
                                                        <span>{{ props.row.detailedPocScore.networkScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_performance')">
                                                        <span>{{ props.row.detailedPocScore.performanceScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
                                            <el-row>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_online_rate')">
                                                        <span>{{ props.row.detailedPocScore.onlineRateScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_block_missing')">
                                                        <span>{{ props.row.detailedPocScore.blockMissScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
                                            <el-row>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_bc')">
                                                        <span>{{ props.row.detailedPocScore.bcScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
                                        </div>
                                        <div class="hidden-sm-and-down">
                                            <el-row>
                                                <el-col :xl="8" :lg="8" :md="8">
                                                    <el-form-item :label="$t('network.poc_score_ss')">
                                                        <span>{{ props.row.detailedPocScore.ssScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :xl="8" :lg="8" :md="8">
                                                    <el-form-item :label="$t('network.poc_score_node_type')">
                                                        <span>{{ props.row.detailedPocScore.nodeTypeScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :xl="8" :lg="8" :md="8">
                                                    <el-form-item :label="$t('network.poc_score_server')">
                                                        <span>{{ props.row.detailedPocScore.serverScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
                                            <el-row>
                                                <el-col :xl="8" :lg="8" :md="8">
                                                    <el-form-item :label="$t('network.poc_score_hardware')">
                                                        <span>{{ props.row.detailedPocScore.hardwareScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :xl="8" :lg="8" :md="8">
                                                    <el-form-item :label="$t('network.poc_score_network')">
                                                        <span>{{ props.row.detailedPocScore.networkScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :xl="8" :lg="8" :md="8">
                                                    <el-form-item :label="$t('network.poc_score_performance')">
                                                        <span>{{ props.row.detailedPocScore.performanceScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
                                            <el-row>
                                                <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">
                                                    <el-form-item :label="$t('network.poc_score_online_rate')">
                                                        <span>{{ props.row.detailedPocScore.onlineRateScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">
                                                    <el-form-item :label="$t('network.poc_score_block_missing')">
                                                        <span>{{ props.row.detailedPocScore.blockMissScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">
                                                    <el-form-item :label="$t('network.poc_score_bc')">
                                                        <span>{{ props.row.detailedPocScore.bcScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
                                        </div>
                                    </el-form>
                                </template>
                            </el-table-column>
                            <el-table-column
                                prop="accountRS"
                                :label="$t('dialog.account_info_account')"
                                width="220">
                            </el-table-column>
                            <el-table-column
                                prop="bindPeerType"
                                :label="$t('dialog.account_transaction_type')"
                            >
                            </el-table-column>
                            <el-table-column
                                sortable
                                prop="pocScore"
                                :label="$t('network.poc_score')"
                            >
                            </el-table-column>
                            <el-table-column
                                prop="hitTime"
                                :formatter="dateFormat"
                                :label="$t('network.mining_time')"
                                width="160">
                            </el-table-column>
                        </el-table>
                    </div>
                </div>
            </div>
            <p class="testnet-tips">{{$t('poc.block_reward_tips_phase1')}}</p>
        </div>
        <dialogCommon :accountInfoOpen="accountInfoDialog" :blockInfoOpen="blockInfoDialog" :height="blockInfoHeight"
                      :generatorRS="generatorRS" @isClose="isClose" @openTransaction="openTransaction"></dialogCommon>
    </div>
</template>
<script>

    export default {
        name: "Network",
        data() {
            return {
                tabTitle: "account",
                //blockinfoDialog
                blockInfoDialog: false,
                blockInfoHeight: -1,
                //accountinfoDialog
                accountInfoDialog: false,
                generatorRS: '',
                //transactionDialog
                transactionId: '',
                transactionDialog: false,
                accountInfo: [],

                minerlistDialog: false,
                minerlist: [],
                minerlistHeight: 590,

                peersLocationList: {},
                peersTimeList: [],

                //list列表
                blocklist: [],
                //网络总览
                newestHeight: 0,
                newestTime: 0,
                averageAmount: 0,
                peerNum: 0,
                //旷工信息
                activeCount: 0,
                totalCount: 0,
                storageCount: 0,
                transferCount: 0,
                coinbaseCount: 0,
                systemReward: 0,
                poolCount: 0,
                aliasCount: 0,
                //分页信息
                currentPage: 1,
                totalSize: 0,
                pageSize: 10,
            };
        },
        created: function () {
            let _this = this;
            _this.init();
            if (!window.NETWORK_URL) {
                window.NETWORK_URL = setInterval(() => {
                    if (_this.$route.path === '/network') {
                        _this.init();
                    }
                }, 10000);
            }
        },
        methods: {
            init() {
                const _this = this;
                _this.$http.get('/sharder?requestType=getBlocks', {
                    params: {
                        firstIndex: (_this.currentPage - 1) * 10,
                        lastIndex: _this.currentPage * 10 - 1
                    }
                }).then(function (res) {
                    if (!res.data.errorDescription) {
                        _this.blocklist = res.data.blocks;
                        console.log("blocklist", _this.blocklist);
                        // _this.calcAverageAmount(res);

                        if (_this.currentPage === 1) {
                            _this.totalSize = res.data.blocks[0].height;
                            _this.coinbaseCount = _this.newestHeight;
                            _this.newestHeight = res.data.blocks[0].height;
                            _this.newestTime = _this.$global.myFormatTime(res.data.blocks[0].timestamp, 'YMDHMS', true);
                        }
                    } else {
                        _this.$message.error(res.data.errorDescription);
                    }

                }).catch(function (err) {
                    _this.$message.error(err);
                });

                _this.$http.get('/sharder?requestType=getPeers').then(function (res) {
                    _this.peerNum = res.data.peers.length;
                    _this.$global.byIPtoCoordinates(res.data.peers).then(res1 => {
                        let json = JSON.parse(res1);
                        for (let i of Object.keys(json)) {
                            if (json[i]["X"] !== "" && json[i]["X"] !== "0"
                                && json[i]["Y"] !== "" && json[i]["Y"] !== "0"
                                && !isNaN(json[i]["X"]) && !isNaN(json[i]["Y"])) {
                                let arr = [];
                                arr.push(json[i]["Y"]);
                                arr.push(json[i]["X"]);
                                _this.peersLocationList[i] = arr;
                                arr = [];
                                arr.push(i);
                                arr.push(_this.$global.myFormatTime(json[i]["time"], "HMS", false));
                                _this.peersTimeList.push(arr);
                            }
                        }
                        _this.$global.drawPeers(_this.peersLocationList, _this.peersTimeList);
                    });
                }).catch(function (err) {
                    console.error("error", err);
                });

                _this.$http.get('/sharder?requestType=getNextBlockGenerators&limit=99999').then(function (res) {
                    // console.log("矿工数量：",res);
                    _this.activeCount = res.data.activeCount;
                    _this.minerlist = res.data.generators;
                    console.log("success to fetch miners");
                }).catch(function (err) {
                    console.error("error", err);
                });

                _this.$http.get('/sharder?requestType=getTxStatistics').then(function (res) {
                    _this.transferCount = res.data.transferCount;
                    _this.storageCount = res.data.storageCount;
                    _this.totalCount = res.data.transferAmount;
                    _this.poolCount = res.data.poolCount;
                    _this.systemReward = res.data.coinBaseCount;
                    _this.averageAmount = res.data.storageCount24H + res.data.transferCount24H;
                }).catch(function (err) {
                    console.error("error", err);
                });
            },
            handleSizeChange(val) {
                this.getBlockList(val);
            },
            handleCurrentChange(val) {
                this.getBlockList(val);
            },
            turn2peers: function () {
                this.$router.push({
                    name: "peers",
                    params: {
                        peersLocationList: this.peersLocationList,
                        peersTimeList: this.peersTimeList
                    }
                });
            },
            openMinerList: function () {
                let _this = this;
                this.$store.state.mask = true;
                this.minerlistDialog = true;
            },
            closeDialog: function () {
                this.$store.state.mask = false;
                this.minerlistDialog = false;
            },
            getBlockList(currentPage) {
                const _this = this;
                this.$http.get('/sharder?requestType=getBlocks', {
                    params: {
                        firstIndex: (currentPage - 1) * 10,
                        lastIndex: currentPage * 10 - 1
                    }
                }).then(function (res) {
                    _this.blocklist = res.data.blocks;
                    // _this.calcAverageAmount(res);
                    return res;
                }).catch(function (err) {
                    return null;
                });
            },
            openBlockInfo(height) {
                const _this = this;
                _this.blockInfoHeight = height;
                _this.blockInfoDialog = true;
            },
            openAccountInfo(generatorRS) {
                const _this = this;
                _this.generatorRS = generatorRS;
                _this.accountInfoDialog = true;

                _this.transactionDialog = false;
            },
            openTransaction(transactionId, accountInfo) {
                const _this = this;
                _this.transactionId = transactionId;
                _this.accountInfo = accountInfo;
                _this.transactionDialog = true;

                // console.log("accountInfo", accountInfo);
                _this.accountInfoDialog = false;
            },
            isClose() {
                const _this = this;
                _this.accountInfoDialog = false;
                _this.adminPasswordDialog = false;
                _this.blockInfoDialog = false;
                _this.blockInfoHeight = -1;
                _this.generatorRS = '';
            },
            dateFormat(val) {
                return this.$global.myFormatTime(val.hitTime, "YMDHMS", true);
            }
        }
    };
</script>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';

    .el-table th > .cell {
        background-color: white;
    }

    #miner_list .modal-body .el-form .el-form-item .el-form-item__label {
        color: #99a9bf !important;
    }

    .testnet-tips {
        padding: 10px 0 20px 0;
        font-size: 13px;
        font-weight: normal;
        text-align: center;
    }
</style>
<!--<style scoped>-->
<!--.modal.w700{-->
<!--width: 960px!important;-->
<!--}-->
<!--</style>-->
