<template>
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
</template>

<script>
    export default {
        name: "blockInfoDialog",
        props: {
            blockInfoOpen: Boolean,
            height:{
            },
            blockId:{
            }
        },
        data(){
            return{
                tabTitle: "account",
                blockInfoDialog: this.blockInfoOpen,
               //区块信息dialog
                blockInfo:[],

            }
        },
        methods:{
            closeDialog: function () {
                const _this = this;
                // _this.blockInfoOpen = false;
                _this.$store.state.mask = false;
                _this.blockInfoDialog = false;
                this.$emit('isClose', false);

                _this.tabTitle = "account";
                _this.blockInfo = [];
            },
        },
        watch:{
            blockInfoDialog: function (val) {
                if(val){
                    const _this = this;
                    this.$http.get('/sharder?requestType=getBlock',{
                        params: {
                            height:_this.height,
                            includeTransactions:true,
                            block:_this.block
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
            }
        }
    }
</script>

<style scoped>

</style>
