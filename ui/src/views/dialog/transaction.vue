<template>
    <div>
        <!--view account transaction info-->
        <div class="modal_info" id="account_transaction" v-show="accountTransactionDialog">
            <div class="modal-header">
                <img class="close" src="../../assets/close.svg" @click="closeDialog"/>
                <h4 class="modal-title">
                    <span >账户：{{accountInfo !== '' ? accountInfo.accountRS : ''}} 信息</span>
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
    </div>
</template>

<script>
    export default {
        name: "transactionDialog",
        prop: {
            accountTransactionOpen: Boolean,
            transaction:'',
            accountinfo:[],
        },
        data(){
            return{
                accountTransactionDialog: this.accountTransactionOpen,
                accountInfo:this.accountinfo,
                //旷工信息dialog
                transactionInfo:[]
            }
        },
        methods: {
           /* openAccountTransaction: function (transaction) {
                this.closeDialog();

                const _this = this;
                this.$http.get('/sharder?requestType=getTransaction',{
                    params:{
                        transaction:transaction
                    }
                }).then(function (res) {
                    _this.transactionInfo = res.data;
                }).catch(function (err) {
                })

                this.$store.state.mask = true;
                this.accountTransactionDialog = true;
            },*/
            openAccountInfo:function(accountRS){
                const _this = this;
                _this.$emit('openAccountInfo',accountRS);
            },
            closeDialog: function () {
                const _this = this;
                _this.isOpen = false;
                _this.$store.state.mask = false;
                _this.accountTransactionDialog = false;

                // _this.accountInfo = [];
                _this.transactionInfo = [];
            },
        },
        watch:{
            accountTransactionDialog: function (val) {
                console.log("accountTransactionDialog",val);
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
            accountTransactionOpen:function (val) {
                const _this = this;
                console.log("accountTransactionOpen",val);
                console.log("accountInfo",_this.accountInfo);
                _this.accountTransactionDialog = val;
            }
        }
    }
</script>

<style scoped>

</style>
