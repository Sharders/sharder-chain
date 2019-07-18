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
                            <tbody v-loading="loading">
                            <tr v-for="block in blocklist">
                                <td class="pl0"><span>{{block.height}}</span></td>
                                <td>
                                    <span>{{$global.myFormatTime(block.timestamp,'YMDHMS',true)}}</span><br>
                                    <span class="utc-time">{{$global.formatTime(block.timestamp)}} +UTC</span>
                                </td>
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
                systemReward: 0,
                poolCount: 0,
                aliasCount: 0,
                //分页信息
                currentPage: 1,
                totalSize: 0,
                pageSize: 10,
                loading: true
            };
        },
        created() {
            let _this = this;

            _this.handleCurrentChange(_this.currentPage);
        },
        mounted() {
            let _this = this;
            _this.init();
            window.NETWORK_URL = setInterval(() => {
                if (_this.$route.path === '/network') {
                    _this.networkUrlBlocks();
                    _this.httpGetPeersNum();

                }
            }, SSO.downloadingBlockchain ? _this.$global.cfg.soonInterval : _this.$global.cfg.defaultInterval);

        },
        methods: {

            init() {

                const _this = this;
                _this.networkUrlBlocks();
                _this.httpGetNextBlockGennerators();
                _this.httpGetPeersNum();
                _this.httpGetTxStatistics();
                let peersArr=localStorage.getItem("coordinates");
                if(JSON.parse(peersArr)===null||JSON.parse(peersArr)==="{}"){
                    _this.httpGetPeersAndDraw();
                }else{
                    _this.getPeersListAndDraw(peersArr);
                }
            },
            httpGetNextBlockGennerators(){
                const _this = this;
                _this.$global.fetch("GET", {
                    limit: 99999
                }, "getNextBlockGenerators").then(res => {
                    _this.activeCount = res.activeCount;
                    _this.minerlist = res.generators;
                }).catch(err => {
                    console.info("error", err);
                });
            },
            httpGetTxStatistics(){
                const _this = this;
                _this.$global.fetch("GET", {}, "getTxStatistics").then(res => {
                    _this.transferCount = res.transferCount;
                    _this.storageCount = res.storageCount;
                    _this.totalCount = res.transferAmount;
                    _this.poolCount = res.poolCount;
                    _this.systemReward = res.coinBaseCount;
                    _this.averageAmount = res.storageCount24H + res.transferCount24H;
                }).catch(err => {
                    console.info("error", err);
                });
            },
            httpGetPeersNum(){
                const _this = this;
                _this.$global.fetch("GET", {}, "getPeers").then(res => {
                    _this.peerNum = res.peers.length;
                    let pn = localStorage.getItem('peerNum');
                    if(pn===null||pn===""){
                        localStorage.setItem('peerNum',_this.peerNum);
                    }else if(pn != res.peers.length){
                        localStorage.setItem('peerNum',res.peers.length);
                        _this.httpGetPeersAndDraw();
                    }

                }).catch(err => {
                    console.info("error", err);
                });
            },
            httpGetPeersAndDraw(){
                const _this = this;
                _this.$global.fetch("GET", {}, "getPeers").then(res => {
                return _this.$global.byIPtoCoordinates(res.peers);
                }).then(res => {
                    localStorage.setItem('coordinates',JSON.stringify(res));
                    let json = JSON.parse(res);
                    _this.getPeersListAndDraw(json);

                }).catch(err => {
                    console.info("error", err);
                });
            },
            getPeersListAndDraw(json){
                const _this = this;
                json = JSON.parse(json);
                for (let i of Object.keys(json)) {
                   // console.info("X:"+json[i]["X"]+",Y:"+json[i]["Y"]);
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
                    _this.$global.drawPeers(_this.peersLocationList, _this.peersTimeList);
                }
            },
            networkUrlBlocks() {
                const _this = this;
                // console.info("networkUrlBlocks，currentPage=" + _this.currentPage);
                _this.getBlocks(1).then(res => {
                    _this.newestHeight = res.blocks[0].height;
                    _this.totalSize = _this.newestHeight + 1;
                    _this.newestTime = _this.$global.myFormatTime(res.blocks[0].timestamp, 'YMDHMS', true);
                    if (_this.currentPage === 1) {
                      //   console.info("refresh block list, newest height is: " + _this.newestHeight);
                        _this.blocklist.splice(0,_this.blocklist.length)
                        _this.blocklist = res.blocks
                        // let blocksStr = JSON.stringify(res.blocks)
                        // _this.blocklist = JSON.parse(blocksStr);
                    }
                    //_this.$forceUpdate();//通知Vue渲染
                }).catch(error => {
                    console.info('error', error)
                });
            },
            handleSizeChange(val) {
                // console.log(`每页 ${val} 条`);
            },
            handleCurrentChange(val) {
                // console.log(`当前页: ${val}`);
                let _this = this;
                _this.loading = true;
                _this.getBlocks(val).then(res => {
                    _this.blocklist.splice(0,_this.blocklist.length)
                    _this.blocklist = res.blocks
                    // _this.blocklist = res.blocks
                    // console.info(_this.blocklist);
                    // let blocksStr = JSON.stringify(res.blocks);
                    // _this.blocklist = JSON.parse(blocksStr);
                    _this.loading = false;
                    // _this.$forceUpdate()
                }).catch(err => {
                    console.info('error', err);
                    _this.$message.error(err);
                    _this.loading = false;
                });
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
            getBlocks(currentPage) {
                let _this = this;
                return new Promise(function (resolve, reject) {
                    _this.$global.fetch("GET", {
                        firstIndex: (currentPage - 1) * _this.pageSize,
                        lastIndex: currentPage * _this.pageSize - 1
                    }, "getBlocks").then(res => {
                        if (res.errorDescription) {
                            _this.$message.error(res.errorDescription);
                            throw (res.errorDescription);
                        }
                        resolve(res);
                    }).catch(err => {
                        reject(err);
                    });
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
        },

    };
</script>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';

    .el-table {
        th > .cell {
            background-color: white;
        }

        .cell {
            font-size: 13px;
        }
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
