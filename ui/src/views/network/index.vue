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
                        <p><span>{{newestHeight}}</span>个</p>
                        <p>生成时间：{{newestTime}}</p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>区块平均交易量</p>
                        <p><span>{{averageAmount}}</span>个</p>
                    </div>
                    <div class="block_blue radius_blue">
                        <p>节点数量</p>
                        <p><span>{{peerNum}}</span>个</p>
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
                                <span>{{activeCount}}</span>
                                <span>旷工数量</span>
                            </div>
                        </div>
                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/miner-info2.svg"/>
                            <div class="section_info">
                                <span>{{totalCount}}</span>
                                <span>总交易量</span>
                            </div>
                        </div>

                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/miner-info3.svg"/>
                            <div class="section_info">
                                <span>{{transferCount}}</span>
                                <span>转账交易</span>
                            </div>
                        </div>
                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/miner-info4.svg"/>
                            <div class="section_info">
                                <span>{{coinbaseCount}}</span>
                                <span>CoinBase交易</span>
                            </div>
                        </div>
                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/miner-info5.svg"/>
                            <div class="section_info">
                                <span>{{storageCount}}</span>
                                <span>存储交易</span>
                            </div>
                        </div>
                    </div>
                    <div class="xs_section br4">
                        <div>
                            <img src="../../assets/miner-info6.svg"/>
                            <div class="section_info">
                                <span>无</span>
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
                    <div class="list_content data_container table_responsive data_loading">
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
                                <tr v-for="(block,index) in blocklist">
                                    <td><span>{{block.height}}</span></td>
                                    <td><span>{{myFormatTime(block.timestamp,'YMDHMS')}}</span></td>
                                    <td><span>{{block.totalAmount}}</span></td>
                                    <td><span>{{block.totalFee}}</span></td>
                                    <td><span>{{block.numberOfTransactions}}</span></td>
                                    <td class="linker" @click="openAccountInfo(block.generatorRS)">{{block.generatorRS}}</td>
                                    <td class="linker" @click="openBlockInfo(block.height,block.totalAmount,'')">查看详情</td>
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

        <!--view block info-->
        <div class="modal_info" id="block_info" v-show="blockInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >区块：{{blockInfo.block}} 信息</span>
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
                            <tr v-for="(transaction,index) in blockInfo.transactions">
                                <td>{{myFormatTime(transaction.timestamp,'YMDHMS')}}</td>

                                </td>
                                <td v-if="transaction.type === 0">
                                    <img src="../../assets/pay.svg"/>
                                    <span>普通支付</span>
                                </td>
                                <td v-else-if="transaction.type === 1">
                                    <img src="../../assets/infomation.svg"/>
                                    <span>任意信息</span>
                                </td>
                                <td v-else-if="transaction.type === 6">
                                    <img src="../../assets/infomation.svg"/>
                                    <span>数据存储</span>
                                </td>
                                <td v-else-if="transaction.type === 9">
                                    <img src="../../assets/coinBase.svg"/>
                                    <span>CoinBase</span>
                                </td>
                                <td>{{transaction.amountNQT/100000000}}</td>
                                <td v-if="transaction.feeNQT">{{transaction.feeNQT/100000000}} SS</td>
                                <td v-else></td>
                                <td v-if="transaction.type === 9">CoinBase</td>
                                <td class="linker" v-else>{{transaction.senderRS}}</td>
                                <td class="linker" v-if="transaction.type === 9">{{transaction.senderRS}}</td>
                                <td class="linker" v-else>{{transaction.recipientRS}}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div v-else-if="tabTitle === 'blockInfo'" class="blockInfo">
                    <table class="table">
                        <tbody>
                            <tr>
                                <th>上一个区块哈希</th>
                                <td>{{blockInfo.previousBlockHash}}</td>
                            </tr>
                            <tr>
                                <th>载荷长度</th>
                                <td>{{blockInfo.payloadLength}}</td>
                            </tr>
                            <tr>
                                <th>总数</th>
                                <td>{{totalAmount}} SS</td>
                            </tr>
                            <tr>
                                <th>矿工签名</th>
                                <td>{{blockInfo.generationSignature}}</td>
                            </tr>
                            <tr>
                                <th>矿工公钥</th>
                                <td>{{blockInfo.generatorPublicKey}}</td>
                            </tr>
                            <tr>
                                <th>交易数量</th>
                                <td>{{blockInfo.numberOfTransactions}}</td>
                            </tr>
                            <tr>
                                <th>区块签名</th>
                                <td>{{blockInfo.blockSignature}}</td>
                            </tr>
                            <tr>
                                <th>版本：</th>
                                <td>{{blockInfo.version}}</td>
                            </tr>
                            <tr>
                                <th>总手续费</th>
                                <td>{{blockInfo.totalFeeNQT/100000000}} SS</td>
                            </tr>
                            <tr>
                                <th>挖矿难度</th>
                                <td>{{blockInfo.cumulativeDifficulty}}</td>
                            </tr>
                            <tr>
                                <th>区块高度</th>
                                <td>{{blockInfo.height}}</td>
                            </tr>
                            <tr>
                                <th>时间戳</th>
                                <td>{{blockInfo.timestamp}}</td>
                            </tr>
                            <tr>
                                <th>矿工</th>
                                <td class="linker">{{blockInfo.generatorRS}}</td>
                            </tr>
                            <tr>
                                <th>上一个区块</th>
                                <td class="linker" @click="openBlockInfo('',totalAmount,blockInfo.previousBlock)">{{blockInfo.previousBlock}}</td>
                            </tr>
                            <tr>
                                <th>下一个区块</th>
                                <td class="linker" v-if="blockInfo.nextBlock" @click="openBlockInfo('',totalAmount,blockInfo.nextBlock)">{{blockInfo.nextBlock}}</td>
                                <td v-else></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <!--view account info-->
        <div class="modal_info" id="account_info" v-show="accountInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >账户：{{accountInfo.accountRS}} 信息</span>
                </h4>
            </div>
            <div class="modal-body">
                <div class="account_preInfo">
                    <span>账户命名：&nbsp;</span><span></span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>可用资金：&nbsp;</span><span>{{accountInfo.unconfirmedBalanceNQT/100000000}}&nbsp;SS</span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>别名：&nbsp;</span><span>无</span>
                </div>
                <div class="account_allInfo">
                    <el-radio-group v-model="tabTitle" class="title">
                        <el-radio-button label="account" class="btn">所有交易</el-radio-button>
                    </el-radio-group>

                    <div v-if="tabTitle === 'account'" class="account_list">
                        <table class="table">
                            <tr>
                                <th>交易时间</th>
                                <th>交易类型</th>
                                <th>数量</th>
                                <th>手续费</th>
                                <th>账户</th>
                                <th>操作</th>
                                <th class="gutter"></th>
                            </tr>
                        </table>
                        <div class="table_body">
                            <table class="table">
                                <tbody>
                                    <tr v-for="transaction in accountTransactionInfo">
                                        <td>{{myFormatTime(transaction.timestamp,'YMDHMS')}}</td>
                                        <td v-if="transaction.type === 0">
                                            <img src="../../assets/pay.svg"/>
                                            <span>普通支付</span>
                                        </td>
                                        <td v-else-if="transaction.type === 1">
                                            <img src="../../assets/infomation.svg"/>
                                            <span>任意信息</span>
                                        </td>
                                        <td v-else-if="transaction.type === 6">
                                            <img src="../../assets/infomation.svg"/>
                                            <span>数据存储</span>
                                        </td>
                                        <td v-else-if="transaction.type === 9">
                                            <img src="../../assets/coinBase.svg"/>
                                            <span>CoinBase</span>
                                        </td>
                                        <td>{{transaction.amountNQT/100000000}}</td>
                                        <td>{{transaction.feeNQT/100000000}}</td>
                                        <td class="linker" @click="openAccountInfo(transaction.senderRS)">{{transaction.senderRS}}</td>
                                        <td class="linker" @click="openAccountTransaction(transaction.transaction)">查看详情</td>
                                    </tr>
                                    <!--<tr>
                                        <td>2018/10/18 8:29:16</td>
                                        <td>
                                            <img src="../../assets/pay.svg"/>
                                            <span>普通支付</span>
                                        </td>
                                        <td>100,000</td>
                                        <td>1</td>
                                        <td class="linker" >SSA-DEUD-WXFN-AZ8H-BPKX</td>
                                        <td class="linker" >查看详情</td>
                                    </tr>-->
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!--view account transaction info-->
        <div class="modal_info" id="account_transaction" v-show="accountTransactionDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >账户：{{accountInfo.accountRS}} 信息</span>
                </h4>
            </div>
            <div class="modal-body">
                <div class="account_preInfo">
                    <span>账户命名：&nbsp;</span><span></span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>可用资金：&nbsp;</span><span>{{accountInfo.unconfirmedBalanceNQT/100000000}}&nbsp;SS</span><span>&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;</span>
                    <span>别名：&nbsp;</span><span>无</span>
                </div>
                <div class="account_transactionInfo">
                    <p class="fl">交易详情</p>
                    <button class="fr common_btn" @click="openAccountInfo(accountInfo.accountRS)">返回账户信息</button>
                    <div class="cb"></div>
                    <table class="table">
                        <tbody>
                        <tr>
                            <th>签名</th>
                            <td>{{transactionInfo.signature}}</td>
                        </tr>
                        <tr>
                            <th>交易序列号</th>
                            <td>{{transactionInfo.transactionIndex}}</td>
                        </tr>
                        <tr>
                            <th>类型</th>
                            <td v-if="transactionInfo.type === 0">
                                <span>普通支付</span>
                            </td>
                            <td v-else-if="transactionInfo.type === 1">
                                <span>任意信息</span>
                            </td>
                            <td v-else-if="transactionInfo.type === 6">
                                <span>数据存储</span>
                            </td>
                            <td v-else-if="transactionInfo.type === 9">
                                <span>CoinBase</span>
                            </td>
                        </tr>
                        <tr>
                            <th>哈希签名</th>
                            <td>{{transactionInfo.signatureHash}}</td>
                        </tr>
                        <tr>
                            <th>发送者</th>
                            <td>{{transactionInfo.senderRS}}</td>
                        </tr>
                        <tr>
                            <th>数额</th>
                            <td>{{transactionInfo.amountNQT/10000000}}</td>
                        </tr>
                        <tr>
                            <th>接收者</th>
                            <td>您</td>
                        </tr>
                        <tr>
                            <th>区块时间戳</th>
                            <td>{{transactionInfo.blockTimestamp}}&nbsp;&nbsp;|
                                &nbsp;&nbsp;{{myFormatTime(transactionInfo.blockTimestamp,'YMDHMS')}}</td>
                        </tr>
                        <tr>
                            <th>时间戳</th>
                            <td>{{transactionInfo.timestamp}}&nbsp;&nbsp;|
                                &nbsp;&nbsp;{{myFormatTime(transactionInfo.timestamp,'YMDHMS')}}</td>
                        </tr>
                        <tr>
                            <th>发送者公钥</th>
                            <td>{{transactionInfo.senderPublicKey}}</td>
                        </tr>
                        <tr>
                            <th>手续费</th>
                            <td>{{transactionInfo.feeNQT/10000000}}</td>
                        </tr>
                        <tr>
                            <th>确认</th>
                            <td>{{transactionInfo.confirmations}}</td>
                        </tr>
                        <tr>
                            <th>类型完整哈希：</th>
                            <td>{{transactionInfo.fullHash}}</td>
                        </tr>
                        <tr>
                            <th>版本：</th>
                            <td>{{transactionInfo.version}}</td>
                        </tr>
                        <tr>
                            <th>发送者</th>
                            <td>{{transactionInfo.sender}}</td>
                        </tr>
                        <tr>
                            <th>接收者</th>
                            <td>您</td>
                        </tr>
                        <tr>
                            <th>区块高度</th>
                            <td>{{transactionInfo.height}}</td>
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
                tabTitle: "account",
                //dialog开关
                blockInfoDialog: false,
                accountInfoDialog: false,
                accountTransactionDialog: false,
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
                //区块信息dialog
                blockInfo:[],
                totalAmount:0,
                //旷工信息dialog
                accountInfo:[],
                accountTransactionInfo:[],
                transactionInfo:[]

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
                _this.newestTime = _this.myFormatTime(res.data[0].timestamp,'YMDHMS');

                console.log(res);
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
            openBlockInfo: function (height, totalAmount,block) {
                this.closeDialog();
                const _this = this;
                _this.totalAmount = totalAmount;
                this.$http.get('/sharder?requestType=getBlock',{
                    params: {
                        height:height,
                        includeTransactions:true,
                        block:block
                    }
                }).then(function (res) {
                    _this.blockInfo = res.data;
                    console.log(_this.blockInfo);
                }).catch(function (err) {
                    console.error("error",err);
                });

                this.$store.state.mask = true;
                this.blockInfoDialog = true;
            },
            openAccountInfo: function (generatorRS) {
                this.closeDialog();

                const _this = this;
                this.$http.get('/sharder?requestType=getBlockchainTransactions',{
                    params: {
                        account:generatorRS
                    }
                }).then(function (res) {
                    _this.accountTransactionInfo = res.data.transactions;
                    console.log(_this.accountTransactionInfo);
                }).catch(function (err) {
                    console.error("error",err);
                });
                this.$http.get('/sharder?requestType=getAccount',{
                    params: {
                        account:generatorRS
                    }
                }).then(function (res) {
                    _this.accountInfo = res.data;
                    console.log("accountInfo",_this.accountInfo);
                }).catch(function (err) {
                    console.error("error",err);
                });

                this.$store.state.mask = true;
                this.accountInfoDialog = true;
            },
            openAccountTransaction: function (transaction) {
                this.closeDialog();

                const _this = this;
                this.$http.get('/sharder?requestType=getTransaction',{
                    params:{
                        transaction:transaction
                    }
                }).then(function (res) {
                    _this.transactionInfo = res.data;
                    console.log("transactionInfo",_this.transactionInfo);
                }).catch(function (err) {
                    console.error("error",err);
                })

                this.$store.state.mask = true;
                this.accountTransactionDialog = true;
            },
            closeDialog: function () {
                this.$store.state.mask = false;
                this.blockInfoDialog = false;
                this.accountInfoDialog = false;
                this.accountTransactionDialog = false;

                this.initDialog();


            },
            myFormatTime: function(value,type){
                const _this = this;
                let dataTime="";
                let data = new Date();
                let date = parseInt(value+'000')+_this.$global.epochBeginning;
                data.setTime(date);
                let year   =  data.getFullYear();
                let month  =  _this.addZero(data.getMonth() + 1);
                let day    =  _this.addZero(data.getDate());
                let hour   =  _this.addZero(data.getHours());
                let minute =  _this.addZero(data.getMinutes());
                let second =  _this.addZero(data.getSeconds());
                if(type === "YMD"){
                    dataTime =  year + "-"+ month + "-" + day;
                }else if(type === "YMDHMS"){
                    dataTime = year + "-"+month + "-" + day + " " +hour+ ":"+minute+":" +second;
                }else if(type === "HMS"){
                    dataTime = hour+":" + minute+":" + second;
                }else if(type === "YM"){
                    dataTime = year + "-" + month;

                }
                return dataTime;//将格式化后的字符串输出到前端显示
            },
            addZero: function (val) {
                if (val < 10) {
                    return "0" + val;
                } else {
                    return val;
                }
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
                    console.log(_this.blocklist);
                    _this.calcAverageAmount(res);
                    return res;
                }).catch(function (err) {
                    console.error(err);
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
            initDialog(){
                const _this = this;
                _this.tabTitle = "account";
                _this.blockInfo = [];
                _this.totalAmount = 0;

            }

        },
        mounted () {
            this.drawPeers();
        },
        filters: {
            formatCurrency(){

            }
        }
    };
</script>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
