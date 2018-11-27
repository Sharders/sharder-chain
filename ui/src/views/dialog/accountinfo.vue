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

    </div>
</template>

<script>

    export default {
        name: "dialog_all",
        props: {
            accountInfoOpen: Boolean,
            blockInfoOpen: Boolean,
            generatorRS:'',
            height:{
            },
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

                blockInfoDialog: this.blockInfoOpen,
                blockInfo:[],
            }
        },
        methods:{
            checkAccountInfo(account){
                const _this = this;
                this.$http.get('/sharder?requestType=getBlockchainTransactions',{
                    params: {
                        account:account
                    }
                }).then(function (res) {
                    _this.accountTransactionInfo = res.data.transactions;
                    console.log(_this.accountTransactionInfo);
                }).catch(function (err) {
                    console.error("error",err);
                });
                this.$http.get('/sharder?requestType=getAccount',{
                    params: {
                        account:account
                    }
                }).then(function (res) {
                    _this.accountInfo = res.data;
                }).catch(function (err) {
                    console.error("error",err);
                });
            },
            openTransactionDialog(transaction){
                const _this = this;
                _this.transaction = transaction;
                _this.accountInfoDialog = false;
                _this.blockInfoDialog = false;
                _this.accountTransactionDialog = true;

            },
            openAccountInfo:function(accountRS){
                const _this = this;
                _this.accountTransactionDialog = false;
                _this.blockInfoDialog = false;
                if(accountRS !== null){
                    _this.accountRS = accountRS;
                }
                console.log("12321321",_this.accountRS);
                _this.accountInfoDialog = true;
            },
            openBlockInfo:function(blockId){
                const _this = this;
                _this.accountTransactionDialog = false;
                _this.accountInfoDialog = false;
                this.$http.get('/sharder?requestType=getBlock',{
                    params: {
                        includeTransactions:true,
                        block:blockId
                    }
                }).then(function (res) {
                    _this.blockInfo = res.data;
                }).catch(function (err) {
                    console.error("error",err);
                });
            },
            closeDialog: function () {
                const _this = this;
                // _this.accountInfoOpen = false;
                _this.$store.state.mask = false;
                _this.accountInfoDialog = false;
                _this.accountTransactionDialog = false;
                _this.blockInfoDialog = false;

                _this.accountTransactionInfo = [];
                _this.accountInfo = [];
                _this.transactionInfo = [];
                _this.blockInfo = [];
                _this.tabTitle = "account";
                _this.$emit('isClose', false);


            },
        },
        watch:{
            accountTransactionDialog:function(val){
                if(val){
                    const _this = this;
                    this.$http.get('/sharder?requestType=getTransaction',{
                        params:{
                            transaction:_this.transaction
                        }
                    }).then(function (res) {
                        _this.transactionInfo = res.data;
                    }).catch(function (err) {
                    });
                    this.$store.state.mask = true;
                }
            },
            accountInfoDialog:function (val) {
                console.log("accountInfoDialog",val);
                if(val){
                    const _this = this;
                    this.$http.get('/sharder?requestType=getBlockchainTransactions',{
                        params: {
                            account:_this.accountRS
                        }
                    }).then(function (res) {
                        _this.accountTransactionInfo = res.data.transactions;
                    }).catch(function (err) {
                        console.error("error",err);
                    });
                    this.$http.get('/sharder?requestType=getAccount',{
                        params: {
                            account:_this.accountRS
                        }
                    }).then(function (res) {
                        _this.accountInfo = res.data;
                    }).catch(function (err) {
                        console.error("error",err);
                    });

                    this.$store.state.mask = true;
                }
            },
            blockInfoDialog: function (val) {
                if(val){
                    const _this = this;
                    this.$http.get('/sharder?requestType=getBlock',{
                        params: {
                            height:_this.height,
                            includeTransactions:true,
                        }
                    }).then(function (res) {
                        _this.blockInfo = res.data;
                    }).catch(function (err) {
                        console.error("error",err);
                    });
                    this.$store.state.mask = true;
                }
            },
            blockInfoOpen:function (val) {
                const _this = this;
                _this.blockInfoDialog = val;
                if(val){
                    _this.accountInfoDialog = false;
                    _this.accountTransactionDialog = false;
                }
            },
            accountInfoOpen:function (val) {
                console.log("accountInfoOpen",val);
                const _this = this;
                _this.accountInfoDialog = val;
                if(val){
                    _this.blockInfoDialog = false;
                    _this.accountTransactionDialog = false;
                    _this.accountRS = _this.generatorRS;
                }
            }
        }
    }
</script>

<style scoped>

</style>
