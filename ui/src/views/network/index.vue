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
                loadLocalCoordinates: false,
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
                    // _this.httpGetPeersNum();
                    _this.drawPeerMap();
                }
            }, SSO.downloadingBlockchain ? _this.$global.cfg.soonInterval : _this.$global.cfg.defaultInterval);
        },
        methods: {
            init() {
                const _this = this;
                _this.networkUrlBlocks();
                _this.httpGetNextBlockGenerators();
                //_this.drawPeerMap();
                // _this.httpGetPeersNum();
                _this.httpGetTxStatistics();
                // let peersArr = localStorage.getItem("coordinates");
                // if(undefined === peersArr || peersArr.substring(1,peersArr.length-1) === "{}"){
                //     _this.drawPeerMap();
                // }else{
                //     _this.parsePeerCoordinatesAndDraw(JSON.parse(peersArr));
                // }
            },
            httpGetNextBlockGenerators(){
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
            // httpGetPeersNum(){
            //     const _this = this;
            //    
            //     _this.$global.fetch("GET", {}, "getPeers").then(res => {
            //         _this.peerNum = res.peers.length;
            //         let pn = localStorage.getItem('peerNum');
            //         if (pn === null || pn === "") {
            //             localStorage.setItem('peerNum', _this.peerNum);
            //         } else if (pn != res.peers.length) {
            //             localStorage.setItem('peerNum', res.peers.length);
            //             _this.drawPeerMap();
            //         }
            //         resolve(res);
            //
            //     }).catch(err => {
            //         reject(err);
            //         console.info("error", err);
            //     });
            //
            // },
            drawPeerMap(){
                const _this = this;
                _this.$global.fetch("GET", {}, "getPeers").then(res => { 
                    _this.peerNum = res.peers.length;
                    let cachedPeerNum = localStorage.getItem('peerNum');
                    let fetchCoordinates = (cachedPeerNum === null || cachedPeerNum === "" || undefined === cachedPeerNum) 
                                          || cachedPeerNum != _this.peerNum; // first time or peer numbers changed
                    console.info(cachedPeerNum)
                    if (fetchCoordinates) {
                        localStorage.setItem('peerNum', _this.peerNum);
                        _this.loadLocalCoordinates = false;
                        return _this.$global.byIPtoCoordinates(res.peers);
                    } else{
                        _this.loadLocalCoordinates = true;
                        return ;
                    }
                }).then(res => {
                    let coordinatesMap;
                    if(_this.loadLocalCoordinates){
                        coordinatesMap = JSON.parse(localStorage.getItem("coordinates"));
                    }else{
                        coordinatesMap = JSON.parse(res);
                        localStorage.setItem('coordinates', JSON.stringify(res));
                    }
                    alert(coordinatesMap)
                    _this.parsePeerCoordinatesAndDraw(coordinatesMap);
                }).catch(err => {
                    console.info("error", err);
                });
                
            },
            parsePeerCoordinatesAndDraw(coordinatesMap){
                const _this = this;
                
                // let XX = "{\n" +
                //     "  \"eu.testnat.sharder.io:8706\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"usa.testnat.sharder.io:8532\": {\n" +
                //     "    \"X\": \"43.6683\",\n" +
                //     "    \"Y\": \"-79.4205\",\n" +
                //     "    \"time\": 1563505605902\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8686\": {\n" +
                //     "    \"X\": \"14.55027\",\n" +
                //     "    \"Y\": \"121.03269\",\n" +
                //     "    \"time\": 1563505604599\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8684\": {\n" +
                //     "    \"X\": \"14.629\",\n" +
                //     "    \"Y\": \"121.0726\",\n" +
                //     "    \"time\": 1563505606841\n" +
                //     "  },\n" +
                //     "  \"cn.testnat.sharder.io:8966\": {\n" +
                //     "    \"X\": \"38.69889\",\n" +
                //     "    \"Y\": \"116.09361\",\n" +
                //     "    \"time\": 1563505621395\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8422\": {\n" +
                //     "    \"X\": \"37.481\",\n" +
                //     "    \"Y\": \"-5.9153\",\n" +
                //     "    \"time\": 1563506129977\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8664\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8466\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8700\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8666\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8468\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8426\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8668\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8704\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8662\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"cn.testnat.sharder.io:8962\": {\n" +
                //     "    \"X\": \"38.69889\",\n" +
                //     "    \"Y\": \"116.09361\",\n" +
                //     "    \"time\": 1563505621395\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8682\": {\n" +
                //     "    \"X\": \"14.55027\",\n" +
                //     "    \"Y\": \"121.03269\",\n" +
                //     "    \"time\": 1563505608761\n" +
                //     "  },\n" +
                //     "  \"cn.testnat.sharder.io:8960\": {\n" +
                //     "    \"X\": \"32.0617\",\n" +
                //     "    \"Y\": \"118.7778\",\n" +
                //     "    \"time\": 1563506132374\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8718\": {\n" +
                //     "    \"X\": \"51.95358\",\n" +
                //     "    \"Y\": \"-2.89325\",\n" +
                //     "    \"time\": 1563505610729\n" +
                //     "  },\n" +
                //     "  \"cn.testnat.sharder.io:8934\": {\n" +
                //     "    \"time\": 1563505606124\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8734\": {\n" +
                //     "    \"X\": \"14.629\",\n" +
                //     "    \"Y\": \"121.0726\",\n" +
                //     "    \"time\": 1563505606841\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8458\": {\n" +
                //     "    \"X\": \"14.629\",\n" +
                //     "    \"Y\": \"121.0726\",\n" +
                //     "    \"time\": 1563505606841\n" +
                //     "  },\n" +
                //     "  \"usa.testnat.sharder.io:8506\": {\n" +
                //     "    \"X\": \"25.8833\",\n" +
                //     "    \"Y\": \"-97.5\",\n" +
                //     "    \"time\": 1563506132474\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8676\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8738\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"47.107.183.179\": {\n" +
                //     "    \"X\": \"45.3155\",\n" +
                //     "    \"Y\": \"-75.837\",\n" +
                //     "    \"time\": 1563506602847\n" +
                //     "  },\n" +
                //     "  \"47.107.188.3\": {\n" +
                //     "    \"X\": \"30.2936\",\n" +
                //     "    \"Y\": \"120.1614\",\n" +
                //     "    \"time\": 1563505612063\n" +
                //     "  },\n" +
                //     "  \"119.23.61.59\": {\n" +
                //     "    \"X\": \"30.29365\",\n" +
                //     "    \"Y\": \"120.16142\",\n" +
                //     "    \"time\": 1563505611142\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8438\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8516\": {\n" +
                //     "    \"X\": \"53.1921\",\n" +
                //     "    \"Y\": \"5.7919\",\n" +
                //     "    \"time\": 1563505613007\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8470\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8670\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8472\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8672\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8792\": {\n" +
                //     "    \"X\": \"50.8828\",\n" +
                //     "    \"Y\": \"5.9623\",\n" +
                //     "    \"time\": 1563506134336\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8432\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8674\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"cn.testnat.sharder.io:8970\": {\n" +
                //     "    \"X\": \"23.6977\",\n" +
                //     "    \"Y\": \"103.3037\",\n" +
                //     "    \"time\": 1563506134441\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8500\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8742\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8620\": {\n" +
                //     "    \"X\": \"15.6689\",\n" +
                //     "    \"Y\": \"120.5806\",\n" +
                //     "    \"time\": 1563506129002\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8606\": {\n" +
                //     "    \"X\": \"51.95358\",\n" +
                //     "    \"Y\": \"-2.89325\",\n" +
                //     "    \"time\": 1563505610729\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8740\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8746\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8744\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8502\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"cn.testnat.sharder.io:8946\": {\n" +
                //     "    \"X\": \"32.0617\",\n" +
                //     "    \"Y\": \"118.7778\",\n" +
                //     "    \"time\": 1563506137274\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8720\": {\n" +
                //     "    \"X\": \"51.95358\",\n" +
                //     "    \"Y\": \"-2.89325\",\n" +
                //     "    \"time\": 1563505610729\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8524\": {\n" +
                //     "    \"X\": \"52.21833\",\n" +
                //     "    \"Y\": \"6.89583\",\n" +
                //     "    \"time\": 1563506137944\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8748\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8626\": {\n" +
                //     "    \"X\": \"14.629\",\n" +
                //     "    \"Y\": \"121.0726\",\n" +
                //     "    \"time\": 1563505606841\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8446\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8688\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8724\": {\n" +
                //     "    \"X\": \"51.95358\",\n" +
                //     "    \"Y\": \"-2.89325\",\n" +
                //     "    \"time\": 1563505610729\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8602\": {\n" +
                //     "    \"X\": \"51.95358\",\n" +
                //     "    \"Y\": \"-2.89325\",\n" +
                //     "    \"time\": 1563505610729\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8708\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8604\": {\n" +
                //     "    \"X\": \"51.95358\",\n" +
                //     "    \"Y\": \"-2.89325\",\n" +
                //     "    \"time\": 1563505610729\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8648\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8482\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8562\": {\n" +
                //     "    \"X\": \"54.8167\",\n" +
                //     "    \"Y\": \"9.45\",\n" +
                //     "    \"time\": 1563505615177\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8484\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8442\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8640\": {\n" +
                //     "    \"X\": \"46.62472\",\n" +
                //     "    \"Y\": \"14.30528\",\n" +
                //     "    \"time\": 1563506138524\n" +
                //     "  },\n" +
                //     "  \"cn.testnat.sharder.io:8940\": {\n" +
                //     "    \"X\": \"29.5628\",\n" +
                //     "    \"Y\": \"106.5528\",\n" +
                //     "    \"time\": 1563506138621\n" +
                //     "  },\n" +
                //     "  \"cn.testnat.sharder.io:8942\": {\n" +
                //     "    \"X\": \"32.0617\",\n" +
                //     "    \"Y\": \"118.7778\",\n" +
                //     "    \"time\": 1563506132374\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8462\": {\n" +
                //     "    \"X\": \"14.629\",\n" +
                //     "    \"Y\": \"121.0726\",\n" +
                //     "    \"time\": 1563505606841\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8460\": {\n" +
                //     "    \"X\": \"14.629\",\n" +
                //     "    \"Y\": \"121.0726\",\n" +
                //     "    \"time\": 1563505606841\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8752\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"cn.testnat.sharder.io:8918\": {\n" +
                //     "    \"X\": \"29.5628\",\n" +
                //     "    \"Y\": \"106.5528\",\n" +
                //     "    \"time\": 1563505618176\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8756\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8712\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8678\": {\n" +
                //     "    \"X\": \"14.629\",\n" +
                //     "    \"Y\": \"121.0726\",\n" +
                //     "    \"time\": 1563505606841\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8710\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8754\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8698\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8610\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8576\": {\n" +
                //     "    \"X\": \"51.35\",\n" +
                //     "    \"Y\": \"3.26667\",\n" +
                //     "    \"time\": 1563505620078\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8716\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8656\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"as.testnat.sharder.io:8714\": {\n" +
                //     "    \"X\": \"35.8569\",\n" +
                //     "    \"Y\": \"139.6489\",\n" +
                //     "    \"time\": 1563505610384\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8614\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8658\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8690\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8692\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8450\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8452\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8650\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8694\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8652\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  },\n" +
                //     "  \"eu.testnat.sharder.io:8696\": {\n" +
                //     "    \"X\": \"51.1\",\n" +
                //     "    \"Y\": \"17.0333\",\n" +
                //     "    \"time\": 1563505603134\n" +
                //     "  }\n" +
                //     "}"
                // coordinatesMap = JSON.parse(XX);
                
                let now = new Date();
                for (let i of Object.keys(coordinatesMap)) {
                    if (coordinatesMap[i]["X"] !== "" && coordinatesMap[i]["X"] !== "0"
                        && coordinatesMap[i]["Y"] !== "" && coordinatesMap[i]["Y"] !== "0"
                        && !isNaN(coordinatesMap[i]["X"]) && !isNaN(coordinatesMap[i]["Y"])) {
                        let arr = [];
                        arr.push(coordinatesMap[i]["Y"]);
                        arr.push(coordinatesMap[i]["X"]);
                        _this.peersLocationList[i] = arr;
                        arr = [];
                        arr.push(i);
                        arr.push(_this.$global.myFormatTime(coordinatesMap[i]["time"], "HMS", false));
                        _this.peersTimeList.push(arr);
                    }
                    _this.$global.drawPeers(_this.peersLocationList, _this.peersTimeList);
                }
                console.info("use " + (new Date().getMilliseconds() - now.getMilliseconds()) + " milliseconds to draw map")
            },
            networkUrlBlocks() {
                const _this = this;

                _this.getBlocks(1).then(res => {
                    _this.newestHeight = res.blocks[0].height;
                    _this.totalSize = _this.newestHeight + 1;
                    _this.newestTime = _this.$global.myFormatTime(res.blocks[0].timestamp, 'YMDHMS', true);
                    if (_this.currentPage === 1) {
                      //   _this.blocklist.splice(0,_this.blocklist.length)
                        _this.blocklist = res.blocks
                    }
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
                    // _this.blocklist.splice(0,_this.blocklist.length)
                    _this.blocklist = res.blocks
                    _this.loading = false;
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
        }

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
