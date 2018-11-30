<template>
    <div>
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
                                <tr v-for="transactions in accountTransactionInfo">
                                    <td>{{$global.myFormatTime(transactions.timestamp,'YMDHMS')}}</td>
                                    <td v-if="transactions.type === 0">
                                        <img src="../../assets/pay.svg"/>
                                        <span>普通支付</span>
                                    </td>
                                    <td v-else-if="transactions.type === 1">
                                        <img src="../../assets/infomation.svg"/>
                                        <span>任意信息</span>
                                    </td>
                                    <td v-else-if="transactions.type === 6">
                                        <img src="../../assets/infomation.svg"/>
                                        <span>数据存储</span>
                                    </td>
                                    <td v-else-if="transactions.type === 9">
                                        <img src="../../assets/coinBase.svg"/>
                                        <span>CoinBase</span>
                                    </td>
                                    <td>{{transactions.amountNQT/100000000}}</td>
                                    <td>{{transactions.feeNQT/100000000}}</td>
                                    <td class="linker w200" @click="checkAccountInfo(transactions.senderRS)"><span>{{transactions.senderRS}}</span></td>
                                    <td class="linker" @click="openTransactionDialog(transactions.transaction)">查看详情</td>
                                </tr>
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
                            <td v-if="transactionInfo.type === 9"></td>
                            <td v-else-if="$store.state.account !== transactionInfo.senderRS">{{transactionInfo.senderRS}}</td>
                            <td v-else-if="$store.state.account === transactionInfo.senderRS">您</td>
                        </tr>
                        <tr>
                            <th>数额</th>
                            <td>{{transactionInfo.amountNQT/10000000}}</td>
                        </tr>
                        <tr>
                            <th>接收者</th>
                            <td v-if="transactionInfo.type === 9&&$store.state.account === transactionInfo.recipientRS">{{transactionInfo.senderRS}}</td>
                            <td v-else-if="transactionInfo.type === 9&&$store.state.account !== transactionInfo.recipientRS">您</td>
                            <td v-else-if="$store.state.account !== transactionInfo.recipientRS">{{transactionInfo.recipientRS}}</td>
                            <td v-else-if="$store.state.account === transactionInfo.recipientRS">您</td>
                        </tr>
                        <tr>
                            <th>区块时间戳</th>
                            <td>{{transactionInfo.blockTimestamp}}&nbsp;&nbsp;|
                                &nbsp;&nbsp;{{$global.myFormatTime(transactionInfo.blockTimestamp,'YMDHMS')}}</td>
                        </tr>
                        <tr>
                            <th>时间戳</th>
                            <td>{{transactionInfo.timestamp}}&nbsp;&nbsp;|
                                &nbsp;&nbsp;{{$global.myFormatTime(transactionInfo.timestamp,'YMDHMS')}}</td>
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
                            <td v-if="transactionInfo.type === 9"></td>
                            <td v-else-if="$store.state.account !== transactionInfo.senderRS">{{transactionInfo.sender}}</td>
                            <td v-else-if="$store.state.account === transactionInfo.senderRS">您</td>
                        </tr>
                        <tr>
                            <th>接收者</th>
                            <td v-if="transactionInfo.type === 9&&$store.state.account === transactionInfo.recipientRS">{{transactionInfo.sender}}</td>
                            <td v-else-if="transactionInfo.type === 9&&$store.state.account !== transactionInfo.recipientRS">您</td>
                            <td v-else-if="$store.state.account !== transactionInfo.recipientRS">{{transactionInfo.recipient}}</td>
                            <td v-else-if="$store.state.account === transactionInfo.recipientRS">您</td>
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
        <!--view block info-->
        <div class="modal_info" id="block_info" v-show="blockInfoDialog">

            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog()"/>
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
                            <td>{{$global.myFormatTime(transaction.timestamp,'YMDHMS')}}</td>

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
                            <td class="linker" v-else @click="openAccountInfo(transaction.senderRS)">{{transaction.senderRS}}</td>
                            <td class="linker" v-if="transaction.type === 9" @click="openAccountInfo(transaction.senderRS)">{{transaction.senderRS}}</td>
                            <td class="linker" v-else @click="openAccountInfo(transaction.recipientRS)">{{transaction.recipientRS}}</td>
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
                            <td>{{blockInfo.totalAmountNQT/100000000}} SS</td>
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
                            <td class="linker" @click="openAccountInfo(blockInfo.generatorRS)">{{blockInfo.generatorRS}}</td>
                        </tr>
                        <tr>
                            <th>上一个区块</th>
                            <td class="linker" @click="openBlockInfo(blockInfo.previousBlock)">{{blockInfo.previousBlock}}</td>
                        </tr>
                        <tr>
                            <th>下一个区块</th>
                            <td class="linker" v-if="blockInfo.nextBlock" @click="openBlockInfo(blockInfo.nextBlock)">{{blockInfo.nextBlock}}</td>
                            <td v-else></td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <!--view account transaction dialog-->
        <div class="modal_info" id="trading_info" v-show="tradingInfoDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >交易详情</span>
                </h4>
            </div>
            <div class="modal-body">
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
                        <td v-if="transactionInfo.type === 9"></td>
                        <td v-else-if="$store.state.account !== transactionInfo.senderRS">{{transactionInfo.senderRS}}</td>
                        <td v-else-if="$store.state.account === transactionInfo.senderRS">您</td>
                    </tr>
                    <tr>
                        <th>数额</th>
                        <td>{{transactionInfo.amountNQT/10000000}}</td>
                    </tr>
                    <tr>
                        <th>接收者</th>
                        <td v-if="transactionInfo.type === 9&&$store.state.account === transactionInfo.recipientRS">{{transactionInfo.senderRS}}</td>
                        <td v-else-if="transactionInfo.type === 9&&$store.state.account !== transactionInfo.recipientRS">您</td>
                        <td v-else-if="$store.state.account !== transactionInfo.recipientRS">{{transactionInfo.recipientRS}}</td>
                        <td v-else-if="$store.state.account === transactionInfo.recipientRS">您</td>
                    </tr>
                    <tr>
                        <th>区块时间戳</th>
                        <td>{{transactionInfo.blockTimestamp}}&nbsp;&nbsp;|
                            &nbsp;&nbsp;{{$global.myFormatTime(transactionInfo.blockTimestamp,'YMDHMS')}}</td>
                    </tr>
                    <tr>
                        <th>时间戳</th>
                        <td>{{transactionInfo.timestamp}}&nbsp;&nbsp;|
                            &nbsp;&nbsp;{{$global.myFormatTime(transactionInfo.timestamp,'YMDHMS')}}</td>
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
                        <td v-if="transactionInfo.type === 9"></td>
                        <td v-else-if="$store.state.account !== transactionInfo.senderRS">{{transactionInfo.sender}}</td>
                        <td v-else-if="$store.state.account === transactionInfo.senderRS">您</td>
                    </tr>
                    <tr>
                        <th>接收者</th>
                        <td v-if="transactionInfo.type === 9&&$store.state.account === transactionInfo.recipientRS">{{transactionInfo.sender}}</td>
                        <td v-else-if="transactionInfo.type === 9&&$store.state.account !== transactionInfo.recipientRS">您</td>
                        <td v-else-if="$store.state.account !== transactionInfo.recipientRS">{{transactionInfo.recipient}}</td>
                        <td v-else-if="$store.state.account === transactionInfo.recipientRS">您</td>
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
</template>

<script>

    export default {
        name: "dialog_all",
        props: {
            accountInfoOpen: Boolean,
            blockInfoOpen: Boolean,
            tradingInfoOpen:Boolean,

            isSearch:Boolean,
            searchValue:'',
            generatorRS:'',
            trading:'',
            height:'',
        },
        data(){
            return{
                tabTitle:'account',
                accountInfoDialog: this.accountInfoOpen,
                accountInfo:[],
                accountTransactionInfo:[],

                accountTransactionDialog:false,
                transactionInfo:[],
                transaction:'',
                accountRS:this.generatorRS,
                searchVal:'',
                blockInfoDialog: this.blockInfoOpen,
                blockInfo:[],

                tradingInfoDialog:this.tradingInfoOpen,
            }
        },
        methods:{
            httpGetAccountInfo(accountID){
                const _this = this;
                return new Promise((resolve, reject) => {

                    _this.$http.get('/sharder?requestType=getAccount',{
                        params: {
                            account:accountID
                        }
                    }).then(function (res) {
                        if(!res.data.errorDescription){
                            _this.accountInfo = res.data;
                            _this.$http.get('/sharder?requestType=getBlockchainTransactions',{
                                params: {
                                    account:accountID
                                }
                            }).then(function (res) {
                                _this.accountTransactionInfo = res.data.transactions;
                            }).catch(function (err) {
                                resolve(err);
                            });
                            resolve("success");
                        }else{
                            resolve(res.data.errorDescription);
                        }
                    }).catch(function (err) {
                        resolve(err);
                    });
                })
            },
            httpGetBlockInfo(height,BlockID){
                const _this = this;
                return new Promise((resolve, reject) =>{
                    _this.$http.get('/sharder?requestType=getBlock',{
                        params: {
                            height:height,
                            block:BlockID,
                            includeTransactions:true,
                        }
                    }).then(function (res) {
                        if(!res.data.errorDescription){
                            _this.blockInfo = res.data;

                            console.log(_this.blockInfo);
                            resolve("success");
                        }else{
                            resolve(res.data.errorDescription);
                        }
                    }).catch(function (err) {
                        resolve(err);
                    });
                });
            },
            httpGetTradingInfo(tradingID){
                const _this = this;
                return new Promise((resolve, reject) => {
                    this.$http.get('/sharder?requestType=getTransaction',{
                        params:{
                            transaction:tradingID
                        }
                    }).then(function (res) {
                        if(!res.data.errorDescription){
                            _this.transactionInfo = res.data;
                            resolve("success");
                        }else{
                            resolve(res.data.errorDescription);
                        }
                    }).catch(function (err) {
                        resolve(err);
                    });
                })
            },
            checkAccountInfo(account){
                const _this = this;
                _this.httpGetAccountInfo(account).then(res =>{
                    if(res !== "success"){
                        _this.$emit('isClose', false);
                        _this.$message({
                            showClose: true,
                            message: res,
                            type: "error"
                        });
                    }
                });
                //
                // this.$http.get('/sharder?requestType=getBlockchainTransactions',{
                //     params: {
                //         account:account
                //     }
                // }).then(function (res) {
                //     _this.accountTransactionInfo = res.data.transactions;
                // }).catch(function (err) {
                //     console.error("error",err);
                // });
                // this.$http.get('/sharder?requestType=getAccount',{
                //     params: {
                //         account:account
                //     }
                // }).then(function (res) {
                //     _this.accountInfo = res.data;
                // }).catch(function (err) {
                //     console.error("error",err);
                // });
            },
            openTransactionDialog(transaction){
                const _this = this;
                _this.httpGetTradingInfo(transaction).then(res =>{
                    if(res !== "success"){
                        _this.$emit('isClose', false);
                        _this.$message({
                            showClose: true,
                            message: res,
                            type: "error"
                        });
                    }else{
                        _this.$store.state.mask = true;
                        _this.accountInfoDialog = false;
                        _this.blockInfoDialog = false;
                        _this.accountTransactionDialog = true;
                    }
                });
                // _this.transaction = transaction;
                // _this.accountInfoDialog = false;
                // _this.blockInfoDialog = false;
                // _this.accountTransactionDialog = true;
            },
            openAccountInfo:function(accountRS){
                const _this = this;
                if(accountRS){
                    _this.httpGetAccountInfo(accountRS).then(res =>{
                        if(res !== "success"){
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        }else{
                            _this.$store.state.mask = true;
                            _this.accountTransactionDialog = false;
                            _this.blockInfoDialog = false;
                            _this.accountInfoDialog = true;
                        }
                    });
                }else{
                    _this.httpGetAccountInfo(_this.accountRS).then(res =>{
                        if(res !== "success"){
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        }else{
                            _this.$store.state.mask = true;
                            _this.accountTransactionDialog = false;
                            _this.blockInfoDialog = false;
                            _this.accountInfoDialog = true;
                        }
                    });
                }
                /*if(_this.resultInfo !== 'success'){
                    _this.$message({
                        showClose: true,
                        message: _this.resultInfo,
                        type: "error"
                    });
                }else{
                    _this.accountTransactionDialog = false;
                    _this.blockInfoDialog = false;
                    _this.accountInfoDialog = true;
                }*/
                // _this.accountTransactionDialog = false;
                // _this.blockInfoDialog = false;
                // if(accountRS !== null){
                //     _this.accountRS = accountRS;
                // }
                // _this.accountInfoDialog = true;
            },
            openBlockInfo:function(blockId){
                const _this = this;
                _this.httpGetBlockInfo('',blockId).then(res =>{
                    if(res !== "success"){
                        _this.$emit('isClose', false);
                        _this.$message({
                            showClose: true,
                            message: res,
                            type: "error"
                        });
                    }else{
                        _this.$store.state.mask = true;
                        _this.accountTransactionDialog = false;
                        _this.accountInfoDialog = false;
                        _this.tabTitle = "account";
                    }
                });
                /*if(_this.resultInfo !== 'success'){
                    _this.$message({
                        showClose: true,
                        message: _this.resultInfo,
                        type: "error"
                    });
                }else{
                    _this.accountTransactionDialog = false;
                    _this.accountInfoDialog = false;
                    _this.tabTitle = "account";
                }*/
                // _this.accountTransactionDialog = false;
                // _this.accountInfoDialog = false;
                // this.$http.get('/sharder?requestType=getBlock',{
                //     params: {
                //         includeTransactions:true,
                //         block:blockId
                //     }
                // }).then(function (res) {
                //     _this.blockInfo = res.data;
                // }).catch(function (err) {
                //     console.error("error",err);
                // });
            },
            closeDialog: function () {
                const _this = this;
                _this.$store.state.mask = false;
                _this.accountInfoDialog = false;
                _this.accountTransactionDialog = false;
                _this.blockInfoDialog = false;
                _this.tradingInfoDialog = false;

                _this.accountTransactionInfo = [];
                _this.accountInfo = [];
                _this.transactionInfo = [];
                _this.blockInfo = [];
                _this.tabTitle = "account";
                _this.$emit('isClose', false);


            }
        },
        watch:{
            // accountTransactionDialog:function(val){
            //     if(val){
            //         const _this = this;
            //         this.$http.get('/sharder?requestType=getTransaction',{
            //             params:{
            //                 transaction:_this.transaction
            //             }
            //         }).then(function (res) {
            //             _this.transactionInfo = res.data;
            //         }).catch(function (err) {
            //         });
            //         this.$store.state.mask = true;
            //     }
            // },
            // accountInfoDialog:function (val) {
            //     if(val){
            //         const _this = this;
            //         this.$http.get('/sharder?requestType=getBlockchainTransactions',{
            //             params: {
            //                 account:_this.accountRS
            //             }
            //         }).then(function (res) {
            //             _this.accountTransactionInfo = res.data.transactions;
            //         }).catch(function (err) {
            //             console.error("error",err);
            //         });
            //         this.$http.get('/sharder?requestType=getAccount',{
            //             params: {
            //                 account:_this.accountRS
            //             }
            //         }).then(function (res) {
            //             _this.accountInfo = res.data;
            //         }).catch(function (err) {
            //             console.error("error",err);
            //         });
            //
            //         this.$store.state.mask = true;
            //     }
            // },
            // blockInfoDialog: function (val) {
            //     if(val){
            //         const _this = this;
            //         this.$http.get('/sharder?requestType=getBlock',{
            //             params: {
            //                 height:_this.height,
            //                 includeTransactions:true,
            //             }
            //         }).then(function (res) {
            //             _this.blockInfo = res.data;
            //         }).catch(function (err) {
            //             console.error("error",err);
            //         });
            //         this.$store.state.mask = true;
            //     }
            // },
            // tradingInfoDialog:function(val){
            //     if(val){
            //         const _this = this;
            //         this.$http.get('/sharder?requestType=getTransaction',{
            //             params:{
            //                 transaction:_this.trading
            //             }
            //         }).then(function (res) {
            //             _this.transactionInfo = res.data;
            //         }).catch(function (err) {
            //
            //         });
            //         this.$store.state.mask = true;
            //     }
            // },
            blockInfoOpen:function (val) {
                const _this = this;
                if(val) {
                    _this.httpGetBlockInfo(_this.height, '').then(res => {
                        if (res !== "success") {
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        } else {
                            _this.$store.state.mask = true;
                            _this.blockInfoDialog = true;
                            _this.accountInfoDialog = false;
                            _this.accountTransactionDialog = false;
                            _this.tradingInfoDialog = false;
                        }
                    });
                }
                  /*  if(_this.resultInfo !== 'success'){
                        _this.$message({
                            showClose: true,
                            message: _this.resultInfo,
                            type: "error"
                        });
                    }else{
                        _this.blockInfoDialog = true;
                        _this.accountInfoDialog = false;
                        _this.accountTransactionDialog = false;
                        _this.tradingInfoDialog = false;
                    }
                }*/
                // _this.blockInfoDialog = val;
                // if(val){
                //     _this.accountInfoDialog = false;
                //     _this.accountTransactionDialog = false;
                //     _this.tradingInfoDialog = false;
                // }
            },
            accountInfoOpen:function (val) {
                console.log(12321321);
                const _this = this;
                if(val) {
                    _this.httpGetAccountInfo(_this.generatorRS).then(res => {
                        if (res !== "success") {
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        } else {
                            _this.$store.state.mask = true;
                            _this.accountInfoDialog = true;
                            _this.blockInfoDialog = false;
                            _this.accountTransactionDialog = false;
                            _this.tradingInfoDialog = false;
                            _this.accountRS = _this.generatorRS;
                        }
                    });
                }
                //     if(_this.resultInfo !== 'success'){
                //         _this.$message({
                //             showClose: true,
                //             message: _this.resultInfo,
                //             type: "error"
                //         });
                //     }else{
                //         _this.accountInfoDialog = true;
                //         _this.blockInfoDialog = false;
                //         _this.accountTransactionDialog = false;
                //         _this.tradingInfoDialog = false;
                //         _this.accountRS = _this.generatorRS;
                //     }
                // }
                // _this.accountInfoDialog = val;
                // if(val){
                //     _this.blockInfoDialog = false;
                //     _this.accountTransactionDialog = false;
                //     _this.tradingInfoDialog = false;
                //     _this.accountRS = _this.generatorRS;
                // }
            },
            tradingInfoOpen: function (val) {
                const _this = this;
                if(val) {
                    _this.httpGetTradingInfo(_this.trading).then(res => {
                        if (res !== "success") {
                            _this.$emit('isClose', false);
                            _this.$message({
                                showClose: true,
                                message: res,
                                type: "error"
                            });
                        } else {
                            _this.$store.state.mask = true;
                            _this.tradingInfoDialog = true;
                            _this.blockInfoDialog = false;
                            _this.accountTransactionDialog = false;
                            _this.accountInfoDialog = false;
                        }
                    });
                }
                //     if(_this.resultInfo !== 'success'){
                //         _this.$message({
                //             showClose: true,
                //             message: _this.resultInfo,
                //             type: "error"
                //         });
                //     }else{
                //         _this.tradingInfoDialog = true;
                //         _this.blockInfoDialog = false;
                //         _this.accountTransactionDialog = false;
                //         _this.accountInfoDialog = false;
                //     }
                // }
                // _this.tradingInfoDialog = val;
                // if(val){
                //     _this.blockInfoDialog = false;
                //     _this.accountTransactionDialog = false;
                //     _this.accountInfoDialog = false;
                // }
            },
            isSearch: function (val) {
                const _this = this;
                _this.searchVal = _this.searchValue;
                if(val){
                    // _this.httpGetAccountInfo(_this.searchValue);
                    _this.httpGetAccountInfo(_this.searchVal).then(function (res) {
                        console.log("httpGetAccountInfo",res);
                        if(res === 'success'){
                            _this.$store.state.mask = true;
                            _this.accountInfoDialog = true;
                            _this.blockInfoDialog = false;
                            _this.accountTransactionDialog = false;
                            _this.tradingInfoDialog = false;
                            _this.accountRS = _this.searchVal;
                        }else{
                            _this.httpGetBlockInfo('',_this.searchVal).then(function (res) {
                                console.log("httpGetBlockInfo",res);
                                if(res === 'success') {
                                    _this.$store.state.mask = true;
                                    _this.blockInfoDialog = true;
                                    _this.accountInfoDialog = false;
                                    _this.accountTransactionDialog = false;
                                    _this.tradingInfoDialog = false;
                                }else{
                                    _this.httpGetTradingInfo(_this.searchVal).then(function (res) {
                                        console.log("httpGetTradingInfo",res);
                                        if(res === 'success') {
                                            _this.$store.state.mask = true;
                                            _this.tradingInfoDialog = true;
                                            _this.blockInfoDialog = false;
                                            _this.accountTransactionDialog = false;
                                            _this.accountInfoDialog = false;
                                        }else{
                                            _this.$message({
                                                showClose: true,
                                                message: "未找到任何信息，请再次查询。",
                                                type: "error"
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    }).catch(err =>{
                        console.log(err);
                    });
                    _this.$emit('isClose', false);

                    // if(_this.resultInfo === 'success'){
                    //     _this.accountInfoDialog = true;
                    //     _this.blockInfoDialog = false;
                    //     _this.accountTransactionDialog = false;
                    //     _this.tradingInfoDialog = false;
                    //     _this.accountRS = _this.searchValue;
                    // }else{
                    //     _this.httpGetBlockInfo('',_this.searchValue);
                    //     console.log("httpGetBlockInfo",_this.resultInfo);
                    //     if(_this.resultInfo === 'success'){
                    //         _this.blockInfoDialog = true;
                    //         _this.accountInfoDialog = false;
                    //         _this.accountTransactionDialog = false;
                    //         _this.tradingInfoDialog = false;
                    //     }else{
                    //         _this.httpGetTradingInfo(_this.searchValue);
                    //         console.log("httpGetTradingInfo",_this.resultInfo);
                    //         if(_this.resultInfo === 'success'){
                    //             _this.tradingInfoDialog = true;
                    //             _this.blockInfoDialog = false;
                    //             _this.accountTransactionDialog = false;
                    //             _this.accountInfoDialog = false;
                    //         }else{
                    //             _this.$message({
                    //                 showClose: true,
                    //                 message: "未找到任何信息，请再次查询。",
                    //                 type: "error"
                    //             });
                    //         }
                    //     }
                    // }
                }
            }
        }
    }
</script>

<style scoped type="text/scss" lang="scss">
    #block_info{
        .modal-body{
            margin: 30px 20px 40px!important;
            .title{
                .el-radio-button__orig-radio:checked + .el-radio-button__inner,
                .el-select-dropdown__item.selected.hover, .el-select-dropdown__item.selected {
                    background-color: #493eda;
                }
                .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
                    color: #fff;
                }
                .el-radio-button__inner:hover {
                    color: #493eda;
                }
            }

            .account_list{
                .table{
                    border: none !important;
                    word-break:break-all;
                    margin-top: 33px;
                    td{
                        border: none !important;
                        text-align: center;
                        line-height: 40px;
                        font-size: 12px;
                        padding: 0;
                        img{
                            width:12px;
                            vertical-align: middle;
                        }
                        span{
                            vertical-align: middle;
                        }
                    }
                    th{
                        border: none !important;
                    }
                    .linker{
                        color:#493eda;
                        cursor: pointer;
                        a{
                            margin: 0 6px;
                        }
                    }
                }
            }
            .blockInfo{
                .table{
                    word-break:break-all;
                    margin-top: 20px;
                    th{
                        width: 160px;
                    }
                    td{
                        width: 1100px;
                        padding:0 60px;
                        line-height: 40px;
                    }
                }
            }
        }
    }

    #account_info{
        .modal-header{
            padding: 0 20px 0 40px;
        }
        .modal-body{
            margin:0;
            .account_preInfo{
                background: #f4f7fd;
                width: 100%;
                padding-left: 41px;
                height: 60px;
                span{
                    line-height: 60px;
                    font-size: 16px;
                    color:#333;
                }
            }
            .account_allInfo{
                margin: 20px;
                .el-radio-button__orig-radio:checked + .el-radio-button__inner,
                .el-select-dropdown__item.selected.hover, .el-select-dropdown__item.selected {
                    background-color: #493eda;
                }
                .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
                    color: #fff;
                }
                .el-radio-button__inner:hover {
                    color: #493eda;
                }
                .account_list{
                    max-height: 510px;
                    .table{
                        border: none !important;
                        word-break:break-all;
                        margin-top: 33px;
                        tr{
                            height:40px;
                            &:nth-child(even){
                                background: #f4f7fc;
                            }
                        }
                        td{
                            border: none !important;
                            width: 190px;
                            text-align: center;
                            line-height: 40px;
                            font-size: 12px;
                            padding: 0;
                            &:nth-child(5){
                                width: 205px;
                            }
                            img{
                                width:12px;
                                vertical-align: middle;
                            }
                            span{
                                vertical-align: middle;
                            }
                        }
                        th{
                            border: none !important;
                            width: 190px;
                            padding: 0;
                            text-align: center;
                            font-weight: bold;
                            &:nth-child(5){
                                width: 205px;
                            }

                        }
                        .linker{
                            color:#493eda;
                            cursor: pointer;
                            a{
                                margin: 0 6px;
                            }
                        }
                        .gutter{
                            width: 16px;
                            padding: 0;
                        }
                    }
                    .table_body{
                        overflow: auto;
                        max-height: 450px;
                        .table{
                            margin-top: 0;
                        }
                    }

                }
            }
        }
    }

    #account_transaction{
        top: 20px;
        .modal-header{
            padding: 0 40px;
            border-bottom: none;
        }
        .modal-body{
            margin:0;
            .account_preInfo{
                background: #f4f7fd;
                width: 100%;
                padding-left: 41px;
                height: 60px;
                span{
                    line-height: 60px;
                    font-size: 16px;
                    color:#333;
                }
            }
            .account_transactionInfo{
                margin: 0 30px 20px;
                p{
                    line-height: 80px;
                    font-size: 17px;
                    font-weight: bold;
                    margin-left: 11px;
                    color: #000;
                }
                button{
                    margin: 20px 0;
                    padding: 0 20px;
                    height: 40px;
                }
                .table{
                    word-break:break-all;
                    //margin-top: 20px;
                    th{
                        width:160px;
                    }
                    td{
                        width:1140px;
                        padding:0 60px;
                        line-height: 40px;
                    }
                }
            }
        }
    }

    #trading_info{
        top: 80px;
        .table{
            td{
                width: 980px;
                padding: 0 60px;
            }
            th{
                width:160px;
            }
        }
    }
</style>
