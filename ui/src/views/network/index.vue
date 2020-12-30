<template>
    <div>
        <div>
            <div class="block_network mb20" v-loading="loading">
                <p class="block_title">
                    <img src="../../assets/img/network.svg"/>
                    <span>{{$t('network.network_title')}}</span>
                    <span class="hrefbtn fr csp only_mobile">
                        <a @click="turn2peers" style="color: #3fb09a;">
                            <span>{{$t('network.peers_detail')}}</span>
                        </a>
                    </span>
                </p>
                <div class="w dfl">
                    <div class="block_blue radius_blue">
                        <p>{{$t('network.block_height')}}</p>
                        <p><span>{{newestHeight}}</span></p>
<!--                        <p>{{$t('network.block_newest_time')}}{{newestTime}}</p>-->
                        <p class="last_block"><span class="generator" >{{lastBlockRS}}</span><span>{{newestTime}}</span></p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>{{$t('network.block_avg_transaction_volume')}}</p>
                        <p><span>{{averageAmount === 0 ? 1:averageAmount}}</span></p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>{{$t('network.block_peers_volume')}}</p>
                        <p><span>{{peerNum}}</span></p>
                        <p class="declared_peers"><span>{{$t('network.declared_peers_size')}}</span><span class="declared_size">{{declaredPeerSize}}</span></p>
                    </div>
                </div>
            </div>
            <div class="block_peers mb20 fl" v-loading="loading">
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
            <div class="block_peers mb20 fr" v-loading="loading">
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
                                <th class="w200">{{$t('network.block_list_amount')}}</th>
                                <th class="pc-table">{{$t('network.block_list_fee')}}</th>
                                <th class="pc-table">{{$t('network.block_list_transaction')}}</th>
                                <th class="pc-table">{{$t('network.block_has_distribution_reward')}}</th>
                                <th class="w200 block_generator">{{$t('network.block_list_generator')}}</th>
                                <th class="pc-table">{{$t('network.block_list_operating')}}</th>
                                <th class="mobile title block_reward">
                                    <span>{{$t('network.block_has_distribution_reward')}}</span>
                                </th>
                            </tr>
                            </thead>
                            <tbody v-loading="loading">
                            <tr v-for="block in blocklist" @click="openBlockInfoMobile(block.height)">
                                <td class="pl0"><span>{{block.height}}</span></td>
                                <td>
                                    <span>{{$global.myFormatTime(block.timestamp,'YMDHMS',true)}}</span><br>
                                    <span class="utc-time">{{$global.formatTime(block.timestamp)}} +UTC</span>
                                </td>
                                <td><span>{{$global.getBlocKTotalAmountNQT(block.totalAmountNQT)}}</span></td>
                                <td class="pc-table"><span>{{$global.getBlockTotalFeeNQT(block.totalFeeNQT)}}</span></td>
                                <td class="pc-table"><span>{{block.numberOfTransactions}}</span></td>
                                <td class="pc-table">
                                    <span v-if="block.hasRewardDistribution">{{$t('network.crowd_miner_reward_success')}}</span>
                                    <span v-if="!block.hasRewardDistribution">{{$t('network.crowd_miner_reward_fail')}}</span>
                                </td>
<!--                                <td class="linker" @click="openAccountInfo(block.generatorRS)">{{block.generatorRS | generatorRSFilter}}</td>-->
                                <td class="linker" @click="openAccountInfo(block.generatorRS)">{{block.generatorRS }}</td>
                                <td class="linker pc-table" @click="openBlockInfo(block.height)">{{$t('network.view_details')}}</td>
                                <td class="mobile icon-box">
                                    <span v-if="block.hasRewardDistribution"><i class="el-icon-check" style="font-size: 15px;color: green"></i></span>
                                    <span v-if="!block.hasRewardDistribution"><i class="el-icon-minus" style="color: red"></i></span>
                                    <!--<i class="el-icon-arrow-right"></i>-->
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="list_pagination">
                        <el-pagination
                            :small="isMobile"
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
                    <div class="modal-body modal-miner pc">
                        <el-table
                            :data="minerlist"
                            style="width: 100%" >
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
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_server')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.serverScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
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
<!--                                            <el-row>-->
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_online_rate')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.onlineRateScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_block_missing')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.blockMissScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                            </el-row>-->
<!--                                            <el-row>-->
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_bc')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.bcScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                            </el-row>-->
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
<!--                                                <el-col :xl="8" :lg="8" :md="8">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_server')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.serverScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
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
<!--                                            <el-row>-->
<!--                                                <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_online_rate')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.onlineRateScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                                <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_block_missing')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.blockMissScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                                <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_bc')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.bcScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                            </el-row>-->
                                        </div>
                                    </el-form>
                                </template>
                            </el-table-column>
                            <el-table-column :label="$t('dialog.account_info_account')" width="230">
                                <template slot-scope="scope">
                                    <div v-html="scope.row.accountRS" v-if="scope.row.accountRS === accountRS" style="color:#1bc98e;"></div>
                                    <div v-html="scope.row.accountRS" v-if="scope.row.accountRS !== accountRS" style=""></div>
                                </template>
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
                                width="160"
                            >
                            </el-table-column>
                        </el-table>
                    </div>
                    <!-- mobile display -->
                    <div class="modal-body modal-miner mobile">
                        <el-table
                            :data="minerlist"
                            style="width: 100%" >
                            <el-table-column type="expand">
                                <template slot-scope="props">
                                    <el-form label-position="left" inline>
                                        <div class="hidden-md-and-up">
                                            <el-row>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('dialog.account_transaction_type')">
                                                        <span>{{ props.row.bindPeerType }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score')">
                                                        <span>{{ props.row.pocScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
                                            <el-row>
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_server')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.serverScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_node_type')">
                                                        <span>{{ props.row.detailedPocScore.nodeTypeScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                                <el-col :sm="12" :xs="12">
                                                    <el-form-item :label="$t('network.poc_score_hardware')">
                                                        <span>{{ props.row.detailedPocScore.hardwareScore }}</span>
                                                    </el-form-item>
                                                </el-col>
                                            </el-row>
<!--                                            <el-row>-->
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_network')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.networkScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_hardware')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.hardwareScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                            </el-row>-->
                                            <el-row>
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_bc')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.bcScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
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
                                                <el-form-item :label="$t('network.poc_score_ss')">
                                                    <span>{{ props.row.detailedPocScore.ssScore }}</span>
                                                </el-form-item>
                                            </el-row>
                                            <el-row>
                                                <el-form-item :label="$t('network.mining_time')">
                                                    <span>{{ dateFormat(props.row) }}</span>
                                                </el-form-item>
                                            </el-row>
<!--                                            <el-row>-->
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_online_rate')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.onlineRateScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                                <el-col :sm="12" :xs="12">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_block_missing')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.blockMissScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                            </el-row>-->
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
<!--                                                <el-col :xl="8" :lg="8" :md="8">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_server')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.serverScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
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
<!--                                            <el-row>-->
<!--                                                <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_online_rate')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.onlineRateScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                                <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_block_missing')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.blockMissScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                                <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">-->
<!--                                                    <el-form-item :label="$t('network.poc_score_bc')">-->
<!--                                                        <span>{{ props.row.detailedPocScore.bcScore }}</span>-->
<!--                                                    </el-form-item>-->
<!--                                                </el-col>-->
<!--                                            </el-row>-->
                                        </div>
                                    </el-form>
                                </template>
                            </el-table-column>
                            <el-table-column :label="$t('dialog.account_info_account')">
                                <template slot-scope="scope">
                                    <div v-html="scope.row.accountRS" v-if="scope.row.accountRS === accountRS" style="color:#14c6fc;"></div>
                                    <div v-html="scope.row.accountRS" v-if="scope.row.accountRS !== accountRS" style=""></div>
                                </template>
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
                isMyMinerDialog:false,
                minerlistHeight: 590,
                lastBlockRS:'',
                peersLocationList: {},
                peersTimeList: [],
                accountRS:"",
                //list列表
                blocklist: [],
                //网络总览
                newestHeight: '--',
                newestTime: '--',
                averageAmount: '--',
                peerNum: '--',
                declaredPeerSize: '--',
                fetchCoordinates: false,
                //矿工信息
                activeCount: '--',
                totalCount: '--',
                storageCount: '--',
                transferCount: '--',
                systemReward: '--',
                poolCount: '--',
                aliasCount: '--',
                //分页信息
                isMobile: false,
                currentPage: 1,
                totalSize: 0,
                pageSize: 10,
                loading: true,
                limitPeerSize: 12
            };
        },
        created() {
            let _this = this;
            _this.handleCurrentChange(_this.currentPage);
            if (/(iPhone|iPad|iPod|iOS|Android)/i.test(navigator.userAgent)) { //移动端
                this.isMobile = true
            }
        },
        mounted() {
            let _this = this;
            _this.init();
            // window.NETWORK_URL = setInterval(() => {
            let networkDataLoader = setInterval(() => {
                if (_this.$route.path === '/network') {
                    _this.networkUrlBlocks();
                    _this.httpGetNextBlockGenerators();
                    _this.fetchPeers();
                    _this.httpGetTxStatistics();
                }else{
                    clearInterval(networkDataLoader);
                }
            }, SSO.downloadingBlockchain ? this.$global.cfg.soonInterval : (this.$global.isOpenApiProxy() ? this.$global.cfg.slowInterval : this.$global.cfg.defaultInterval));

            let peerDataLoader = setInterval(() => {
                if (_this.$route.path === '/network') {
                    _this.drawPeerMap();
                }else{
                    clearInterval(peerDataLoader);
                }
            }, 15*60*1000);

            window.onbeforeunload = function (e) {
                e = e || window.event;
                return e;
            };
        },
        filters: {
            generatorRSFilter(val) {
                if (/(iPhone|iPad|iPod|iOS|Android)/i.test(navigator.userAgent)) { //移动端
                    let temp = val
                    return temp.slice(0, 4) + '...' + temp.slice(temp.length-4, temp.length-1)
                }
                return val
            }
        },
        methods: {
            init() {
                const _this = this;
                _this.networkUrlBlocks();
                _this.httpGetNextBlockGenerators();
                _this.drawPeerMap();
                _this.httpGetTxStatistics();
            },
            httpGetNextBlockGenerators(){
                const _this = this;
                let poolArr1 = [];
                let poolArr2 = [];
                _this.$global.fetch("GET", {
                    limit: 99999
                }, "getNextBlockGenerators").then(res => {
                    _this.activeCount = res.activeCount;
                    _this.accountRS = SSO.accountRS;
                    for(let t of res.generators){
                        if(t.accountRS === SSO.accountRS){
                            poolArr1.push(t);
                        }else{
                            poolArr2.push(t);
                        }
                    }
                    _this.minerlist = poolArr1.concat(poolArr2);
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
            fetchPeers(){
                const _this = this;
                _this.$global.fetch("GET", {startThis:"startThis"}, "getPeers").then(res => {
                    _this.peerNum = res.peers.length>0 ? res.peers.length : _this.limitPeerSize;
                    _this.declaredPeerSize = _this.$global.isOpenApiProxy() ? '--' : res.declaredPeerSize;
                }).catch(err => {
                    console.info("error", err);
                });
            },
            drawPeerMap(){
                const _this = this;
                _this.$global.fetch("GET", {startThis:"startThis"}, "getPeers").then(res => {
                    _this.peerNum = res.peers.length>0 ? res.peers.length : _this.limitPeerSize;
                    _this.declaredPeerSize = _this.$global.isOpenApiProxy() ? '--' : res.declaredPeerSize;
                    try {
                        _this.$global.coordinatesMap = JSON.parse(res.coordinates);
                    }catch (e) {
                        console.log(e);
                    }finally {
                        _this.$global.drawPeers();
                    }
                }).catch(err => {
                    console.info("error", err);
                });
            },
            networkUrlBlocks() {
                const _this = this;

                _this.getBlocks(1).then(res => {
                    _this.newestHeight = res.blocks[0].height;
                    _this.lastBlockRS = res.blocks[0].generatorRS;
                    _this.totalSize = _this.newestHeight + 1;
                    _this.newestTime = _this.$global.myFormatTime(res.blocks[0].timestamp, 'YMDHMS', true);
                    if (_this.currentPage === 1) {
                      //   _this.blocklist.splice(0,_this.blocklist.length)
                        //console.log(res.blocks);
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
                    _this.loading = false;
                });
            },
            turn2peers: function () {
                this.$router.push({
                    name: "peers",
                    params: {
                        peersLocationList: this.peersLocationList,
                        peersTimeList: this.peersTimeList,
                        minerList:this.minerlist
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
            openBlockInfoMobile(height) {
                if (/(iPhone|iPad|iPod|iOS|Android)/i.test(navigator.userAgent)) { //移动端
                    this.openBlockInfo(height)
                }
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
            // menuAdapter() {
            //     document.getElementsByClassName('header')[0].style.display = 'block'
            //     var menuLi = document.querySelectorAll('.navbar .el-menu li')
            //     for (let i = 0; i < menuLi.length; i++) {
            //         if (i === 1) {
            //             menuLi[i].setAttribute('class', 'el-menu-item is-active')
            //             menuLi[i].style.borderBottomColor = '#409EFF'
            //         } else {
            //             menuLi[i].setAttribute('class', 'el-menu-item')
            //             menuLi[i].style.borderBottomColor = 'transparent'
            //         }
            //     }
            // }
        }
    };
</script>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';

    @media only screen and (max-width: 780px) {
        .list_pagination /deep/ .el-pagination__jump {
            display: initial!important;
            float: right!important;
            margin-top: 11px!important;
        }

        .el-pagination--small .el-pagination__editor, .el-pagination--small .el-pagination__editor.el-input .el-input__inner {
            height: 26px!important;
        }
    }

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


    .last_block {
        text-align: left!important;
        font-size: 12px!important;

        .generator {
            margin-right: 10px;
        }
    }

    .declared_peers {
        text-align: left!important;
        font-size: 13px!important;

        .declared_size {
            margin-left: 5px;
            font-weight: bold;
        }
    }

</style>
