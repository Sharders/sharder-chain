<template>
    <div class="pool-attribute">
        <div>
            <p @click="$router.back()" class="pool-back">{{$t('mining.attribute.return_previous')}}</p>
            <div class="pool-content">
                <div class="attribute-info">
                    <img src="../../assets/img/shouyi.png" id="shouyi">
                    <div class="attribute-text">
                        <span class="pool-serial-number">
                            {{$t('mining.attribute.pool_number')}} {{mining.poolId}}
                            | {{$t('mining.index.my_assets')}} {{$global.getAmountFormat(accountInfo.effectiveBalanceNQT)}}
                            <!-- close chance of pool -->
                            <!-- | {{$t('mining.attribute.mining_probability')}}{{miningInfo.chance * 100}}%-->
                        </span>
                        <span class="pool-attribute-info" @click="miningMask('isAttribute')">{{$t('mining.attribute.pool_details')}}</span>
                    </div>
                    <div class="pool-state">
                        <h1>{{$t('mining.attribute.mining')}}</h1>
                        <h1>{{$t('mining.attribute.mining_current_number1')}}<span
                            class="number">{{newestBlock.height}}</span>{{$t('mining.attribute.mining_current_number2')}}
                        </h1>
                    </div>
                    <div class="earnings">{{$t('mining.attribute.income') + " +" +
                        $global.getAmountFormat(miningInfo.income)}}
                    </div>
                </div>
                <div class="my-info" v-loading="loading">
                    <h1>
                        <img src="../../assets/img/wodexingxi.png" class="head-portrait" v-if="$global.projectName === 'mw'">
                        <img src="../../assets/img/sharder/wodexingxi.png" class="head-portrait" v-else-if="$global.projectName === 'sharder'">
                        <span>{{$t('mining.attribute.self_info')}}</span>
                    </h1>
                    <div class="my-attribute">
                        <el-row :gutter="20">
                            <el-col :span="6">
                                <button class="info">
                                    <p>{{$t('mining.attribute.join_time')}}</p>
                                    <p class="strong">{{miningInfo.timestamp === 0 ?
                                        $t("mining.attribute.not_join"):miningInfo.timestamp}}</p>
                                </button>
                            </el-col>
                            <el-col :span="6">
                                <button class="info">
                                    <p>{{$t('mining.attribute.investing_diamonds')}}</p>
                                    <p class="strong">{{miningInfo.joinAmount/100000000}}</p>
                                </button>
                            </el-col>
                            <el-col :span="6">
                                <button class="info">
                                    <p>{{$t('mining.attribute.gain_profit')}}</p>
                                    <p class="strong">{{$global.getAmountFormat(miningInfo.rewardAmount)}}</p>
                                </button>
                            </el-col>
                            <el-col :span="6">
                                <button class="info">
                                    <p>{{$t('mining.attribute.remaining_mining_time')}}</p>
<!--                                    <p class="strong">{{miningInfo.startBlockNo > newestBlock.height ?-->
<!--                                        miningInfo.endBlockNo - miningInfo.startBlockNo : miningInfo.endBlockNo - -->
<!--                                        newestBlock.height}}</p>-->
                                    <p class="time">∞</p>
                                </button>
                            </el-col>
                        </el-row>
                    </div>
                    <div class="attribute-btn">
                        <button v-if="displayBtn('join')" class="join" @click="miningMask('isJoinPool')">
                            {{$t('mining.attribute.investing_diamonds')}}
                        </button>
                        <button v-if="displayBtn('quit')" class="exit" @click="miningMask('isExitPool')">
                            {{$t('mining.attribute.exit_pool')}}
                        </button>
                        <button v-if="displayBtn('destroy')" class="exit" @click="miningMask('isDestroyPool')">
                            {{$t('mining.attribute.destroy_pool')}}
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!--矿池属性-->
        <div v-if="isAttribute">
            <div class="mining-attribute">
                <span class="img-close" @click="miningMask('isAttribute')"></span>
                <div class="attribute">
                    <h1>
                        <img src="../../assets/img/pay.svg" class="attribute-img" v-if="$global.projectName === 'mw'">
                        <img src="../../assets/img/sharder/pay.svg" class="attribute-img" v-else-if="$global.projectName === 'sharder'">
                        <span>{{$t('mining.attribute.pool_details')}}</span>
                    </h1>
                    <div class="attribute-value">
                        <el-row :gutter="20">
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.creator')}}: {{miningInfo.account}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.participating_users')}}: {{miningInfo.amount}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.pool_number')}}: {{mining.poolId}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.capacity')}}: {{miningInfo.currentInvestment/100000000}}/{{miningInfo.investmentTotal/100000000}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.pool_income')}}: {{miningInfo.income/100000000}} MW
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.reward_distribution') }}: {{$global.getRewardRate(miningInfo)}}
                                </button>
                            </el-col>
                        </el-row>
                    </div>
                    <button class="btn" style="display: none" @click="miningMask('isAttribute')">
                        {{$t('mining.attribute.close')}}
                    </button>
                </div>
            </div>
        </div>
        <!--加入矿池-->
        <div v-if="isJoinPool">
            <div class="join-pool">
                <span class="img-close" @click="miningMask('isJoinPool')"></span>
                <h1 class="title">{{$t('mining.attribute.investing_diamonds')}}</h1>
                <p class="attribute">
                    {{$t('mining.attribute.currently_available') + $global.getAmountFormat(miningInfo.investmentTotal - miningInfo.currentInvestment)}} |
                    {{$t('mining.attribute.pool_capacity') + $global.getAmountFormat(miningInfo.investmentTotal)}}
                </p>
<!--                <p class="input">-->
<!--                    <el-input type="number" value="remainBlocks()" :readonly></el-input>-->
<!--                </p>-->
                <p class="input">
                    <el-input v-model="joinPool" type="number" :placeholder="$t('mining.attribute.join_pool_tip')"></el-input>
                </p>
                <p class="btn">
                    <button class="cancel" @click="miningMask('isJoinPool')">{{$t('mining.attribute.cancel')}}</button>
                    <el-button class="confirm" v-loading="btnLoading" :disabled="btnLoading" @click="miningJoin">{{$t('mining.attribute.confirm')}}</el-button>
                </p>
            </div>
        </div>
        <!--退出矿池-->
        <div v-if="isExitPool">
            <div class="exit-pool">
                <span class="img-close" @click="miningMask('isExitPool')"></span>
                <h1 class="title">{{$t('mining.attribute.exit_pool')}}</h1>
                <p class="info">{{$t('mining.attribute.exit_pool_tip')}}</p>

                <template>
                    <el-table
                            :data="miningInfo.consignor.txs"
                            stripe
                            style="width: 100%">
                        <el-table-column
                                prop="transactionId"
                                :label="$t('mining.attribute.tx_id')"
                                width="180">
                        </el-table-column>
                        <el-table-column
                                prop="amount"
                                :formatter="formatAmount"
                                :label="$t('mining.attribute.amount')">
                        </el-table-column>
                        <el-table-column
                                prop="startBlockNo"
                                :formatter="formatHeight"
                                :label="$t('mining.attribute.height_range')">
                        </el-table-column>
                        <el-table-column
                                fixed="right"
                                :label="$t('mining.attribute.exit_pool')"
                                width="80">
                            <template slot-scope="scope">
                                <el-button class="confirm" v-loading="btnLoading" :disabled="btnLoading" @click="miningExit(scope.row,scope.$index)" type="text" size="small">{{$t('mining.attribute.confirm')}}</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </template>
            </div>
        </div>
        <!--删除矿池-->
        <div v-if="isDestroyPool">
            <div class="exit-pool">
                <span class="img-close" @click="miningMask('isDestroyPool')"></span>
                <h1 class="title">{{$t('mining.attribute.destroy_pool')}}</h1>
                <p class="info">{{$t('mining.attribute.destroy_pool_tip')}}</p>
                <p class="btn">
                    <button class="cancel" @click="miningMask('isDestroyPool')">{{$t('mining.attribute.cancel')}}</button>
                    <el-button class="confirm" v-loading="btnLoading" :disabled="btnLoading" @click="miningDestroy()">{{$t('mining.attribute.confirm')}}</el-button>
                </p>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "attribute",
        data() {
            return {
                mining: this.$route.params.mining,
                joinPool: '',
                newestBlock: this.$route.params.newestBlock,
                isAttribute: false,
                isJoinPool: false,
                isExitPool: false,
                isDestroyPool: false,
                joinRSPool: '',
                myAccount: SSO.accountRS,
                accountInfo: SSO.accountInfo,
                secretPhrase: SSO.secretPhrase,
                miningInfo: {
                    account: '',
                    accountId: "",
                    amount: 0,
                    joinAmount: 0,
                    rewardAmount: 0,
                    poolId: '',
                    currentInvestment: 0,
                    investmentTotal: 0,
                    income: 0,
                    chance: 0,
                    timestamp: '',
                    startBlockNo: 0,
                    endBlockNo: 0,
                    consignor: {},
                    level: {}
                },
                loading: true,
                btnLoading: false,
                deletingPools: []
            }
        },
        methods: {
            displayBtn(btnName){
                const _t = this;
                if('join' === btnName) {
                    return _t.miningInfo.currentInvestment < _t.miningInfo.investmentTotal
                    && typeof(_t.secretPhrase) !== 'undefined'
                    &&  _t.$global.optHeight.join < _t.newestBlock.height
                    && _t.miningInfo.endBlockNo - 1 >= _t.newestBlock.height
                    && !_t.deletingPools.includes(_t.miningInfo.poolId);
                }else if('quit' === btnName) {
                    return typeof(_t.miningInfo.consignor) !== 'undefined'
                    && typeof(_t.miningInfo.consignor.txs) !== 'undefined'
                    && _t.miningInfo.consignor.txs.length > 0
                    && typeof(_t.secretPhrase) !== 'undefined'
                    && _t.$global.optHeight.quit < _t.newestBlock.height
                    && _t.miningInfo.endBlockNo - 1 >= _t.newestBlock.height
                    && !_t.deletingPools.includes(_t.miningInfo.poolId);
                }else if('destroy' === btnName) {
                    return _t.myAccount === _t.miningInfo.account
                        && !_t.$store.state.destroyPool[_t.miningInfo.poolId]
                        && typeof(_t.secretPhrase) !== 'undefined'
                        && _t.$global.optHeight.destroy < _t.newestBlock.height;
                }
                return false;

            },
            formatAmount(row, column) {
                return this.$global.getAmountFormat(row.amount);
            },
            formatHeight(row, column) {
                return row.startBlockNo + " - " + row.endBlockNo;
            },
            remainBlocks() {
                let _t = this;
                if (_t.mining.startBlockNo > _t.newestBlock.height) {
                    return _t.mining.endBlockNo - _t.mining.startBlockNo
                } else {
                    return _t.mining.endBlockNo - _t.newestBlock.height
                }
            },
            miningExit(_tx,_index) {
                // console.log(_tx);
                // console.log(_index);
                let _this = this;
                if (SSO.downloadingBlockchain) {
                    return this.$message.warning(this.$t("account.synchronization_block"));
                }
                _this.btnLoading = true;
                _this.$global.fetch("POST", {
                    txId: _tx.transactionId,
                    poolId: _this.miningInfo.poolId,
                    secretPhrase: SSO.secretPhrase,
                    deadline: 360,
                    feeNQT: 100000000
                }, "quitPool").then(val => {
                    if (val.errorDescription) {
                        _this.btnLoading = false;
                        return _this.$message.error(val.errorDescription);
                    }
                    _this.isExitPool = false;
                    _this.$global.optHeight.quit = _this.newestBlock.height;
                    _this.$store.state.quitPool[_this.miningInfo.poolId] -= _tx.amount;
                    delete _this.miningInfo.consignor.txs[_index];
                    _this.miningInfo.joinAmount -= _tx.amount;
                    _this.$store.state.mask = false;
                    _this.btnLoading = false;
                    return _this.$message.success(_this.$t("mining.attribute.exit_success"));
                });
            },
            miningDestroy() {
                let _this = this;
                if (SSO.downloadingBlockchain) {
                    return _this.$message.warning(_this.$t("account.synchronization_block"));
                }
                if (_this.$store.state.destroyPool[_this.miningInfo.poolId]) return;

                _this.btnLoading = true;
                _this.$global.fetch("POST", {
                    period: _this.remainBlocks(),
                    secretPhrase: SSO.secretPhrase,
                    deadline: 360,
                    feeNQT: 100000000,
                    poolId: _this.miningInfo.poolId,
                }, "destroyPool").then(res => {
                    if (res.errorDescription) {
                        return _this.$message.error(res.errorDescription);
                    }
                    _this.btnLoading = false;
                    _this.$store.state.mask = false;
                    _this.isDestroyPool = false;
                    _this.deletingPools.push(_this.miningInfo.poolId);
                    _this.$store.state.destroyPool[_this.miningInfo.poolId] = _this.myAccount;
                    _this.$global.optHeight.destroy = _this.newestBlock.height;
                    return _this.$message.success(_this.$t("mining.attribute.delete_success"));
                });
            },
            miningJoin() {
                let _this = this;
                if (_this.validationJoinMining()) return;
                let joinAmount = _this.joinPool * 100000000
                _this.btnLoading = true;
                _this.$global.fetch("POST", {
                    period: _this.remainBlocks(),
                    secretPhrase: SSO.secretPhrase,
                    deadline: 360,
                    feeNQT: 100000000,// 手续费默认是 1 MW
                    poolId: _this.mining.poolId,
                    amount: joinAmount
                }, "joinPool").then(res => {
                    _this.btnLoading = false;
                    if (typeof res.errorDescription === "undefined") {
                        _this.$message.success(_this.$t("mining.attribute.join_success"));
                        _this.$store.state.mask = false;
                        _this.isJoinPool = false;
                        _this.$global.optHeight.join = _this.newestBlock.height;
                        _this.miningInfo.joinAmount += joinAmount;
                    } else {
                        _this.$message.error(res.errorDescription);
                    }
                }).catch(err => {
                    console.log(err);
                    _this.btnLoading = false;
                });
            },
            miningMask(val) {
                if (this[val]) {
                    this.$store.state.mask = false;
                } else {
                    this.$store.state.mask = true;
                }
                this[val] = !this[val];
            },
            myMiningInfo() {
                let _this = this;
                _this.loading = true;

                _this.$global.fetch("POST", {
                    account: SSO.account,
                    poolId: _this.mining.poolId,
                }, "getPoolInfo").then(res => {
                    _this.loading = false;
                    if (res.errorDescription) {
                        _this.$message.warning(_this.$t("mining.attribute.pool_destruction"));
                        return _this.$router.back()
                    }
                    _this.miningInfo.amount = res.number;
                    _this.miningInfo.poolId = res.poolId;
                    _this.miningInfo.currentInvestment = res.power + res.joiningAmount;
                    _this.miningInfo.accountId = _this.$global.longUnsigned(res.creatorID);
                    _this.miningInfo.income = res.mintRewards;
                    _this.miningInfo.chance = res.chance;
                    _this.miningInfo.startBlockNo = res.startBlockNo;
                    _this.miningInfo.endBlockNo = res.endBlockNo;
                    _this.miningInfo.level = res.rule.level0 ? res.rule.level0 : res.rule.level1;
                    _this.miningInfo.joinAmount = res.joinAmount;
                    _this.miningInfo.rewardAmount = res.rewardAmount;
                    _this.miningInfo.consignor = res.consignor;
                    _this.miningInfo.investmentTotal = _this.miningInfo.level.consignor.amount.max + _this.$global.poolPledgeAmount;
                    _this.miningInfo.account = res.creatorRS;
                    if (!_this.$store.state.quitPool[res.poolId]) {
                        _this.$store.state.quitPool[res.poolId] = res.joinAmount;
                    }

                }).catch(err => {
                    _this.loading = false;
                });
            },
            validationJoinMining() {
                if (SSO.downloadingBlockchain) {
                    return this.$message.warning(this.$t("account.synchronization_block"));
                }

                if(this.joinPool > this.accountInfo.effectiveBalanceNQT / 100000000) {
                    return this.$message.error(this.$t("mining.attribute.not_enough_balance"));
                }

                let min = this.miningInfo.level.consignor.amount.min / 100000000;
                let max = this.miningInfo.level.consignor.amount.max / 100000000;
                if (this.joinPool < min || this.joinPool > max) {
                    return this.$message.error(this.$t("mining.attribute.join_number_info", {
                        min: min,
                        max: max
                    }));
                }

                if (this.miningInfo.currentInvestment + this.joinPool * 100000000 > this.miningInfo.investmentTotal) {
                    return this.$message.error(this.$t("mining.attribute.exceeding_total"));
                }
            },
            myJoinTime() {
                let _this = this;
                _this.$global.fetch("POST", {
                    account: _this.myAccount,
                    type: 8,
                    subtype: 2,
                    /*lastIndex: 0*/
                }, "getBlockchainTransactions").then(res => {
                    // console.info(res);
                    for (let t of res.transactions) {
                        if (t.attachment.poolId === _this.miningInfo.poolId) {
                            _this.formatJionTime(t.timestamp);
                            break;
                        }
                    }
                });
            },

            formatJionTime(time,fmt,tz){
                const _this = this;
                fmt = fmt || "yyyy-MM-dd hh:mm:ss";
                tz = tz || 0;
                _this.$global.fetch("GET",{},"getConstants").then(function (res) {
                    return parseInt(res.epochBeginning);
                }).then(res => {

                    let date = new Date(time * 1000 + res + tz * 3600000);
                    let o = {
                        "M+": date.getMonth() + 1,                          //月份
                        "d+": date.getDate(),                               //日
                        "h+": date.getHours(),                              //小时
                        "m+": date.getMinutes(),                            //分
                        "s+": date.getSeconds(),                            //秒
                        "S": date.getMilliseconds()                        //毫秒
                    };
                    if (/(y+)/.test(fmt))
                        fmt = fmt.replace(RegExp.$1, (date.getFullYear() + "").substr(4 - RegExp.$1.length));
                    for (let k in o)
                        if (new RegExp("(" + k + ")").test(fmt))
                            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
                    _this.miningInfo.timestamp = fmt;
                });

            }

        },
        created: function () {
            let _this = this;
            _this.myMiningInfo();
            _this.myJoinTime();
        }
    }
</script>
<!--矿池详情-->
<style scoped type="text/scss" lang="scss">
@import '../../styles/css/vars.scss';
    .pool-attribute .pool-back {
        font-size: 16px;
        color: $primary_color;
        cursor: pointer;
    }

    .pool-content .attribute-info {
        padding: 30px;
        background: url("../../assets/img/kuangchi_bg.png") no-repeat center 140px;
        background-color: $primary_color;
        height: 300px;
        border-top-right-radius: 6px;
        border-top-left-radius: 6px;
        margin-top: 10px;
        position: relative;
    }

    .attribute-info .attribute-text {
        font-size: 16px;
        color: #fff;
    }

    .attribute-info .pool-state {
        /*display: none;*/
        text-align: center;
        font-size: 18px;
        font-weight: bold;
        position: absolute;
        top: 66px;
        width: 200px;
        left: calc(50% - 100px);
        color: #fff;
    }

    .attribute-info .pool-state .number {
        color: $primary_color_aa;
    }

    .attribute-info .pool-attribute-info {
        float: right;
        cursor: pointer;
        font-size: 13px;
        border-bottom: 1px solid #fff;
    }

    .attribute-info .earnings {
        width: 140px;
        text-align: center;
        margin: auto;
        position: absolute;
        top: 180px;
        left: calc(50% - 70px);
        font-size: 16px;
        color: #fff;
    }

    #shouyi {
        position: absolute;
        top: 140px;
        left: calc(50% - 18px);
    }

    .pool-content .my-info {
        padding: 0 40px 50px;
        background: #fff;
    }

    .pool-content .my-info h1 {
        font-size: 18px;
        font-weight: bold;
        color: #555;
        padding: 18px 0;
    }

    .my-info .head-portrait {
        width: 20px;
        height: 20px;
        border-radius: 50%;
        position: relative;
        top: 4px;
        margin: 0 12px 0 0;
    }

    .my-attribute .info {
        box-shadow: 0 0 2px $primary_color_aa;
        height: 100px;
        width: 100%;
        border: none;
        outline: none;
        border-radius: 4px;
        padding: 0;
        font-size: 16px;
        color: #555;
        background: transparent;
    }

    .my-attribute .info .strong {
        font-size: 20px;
        margin-top: 16px;
    }

    .my-attribute .info .time {
        font-size: 20px;
        margin-top: 10px;
    }

    .my-info .attribute-btn button {
        border: none;
        outline: none;
        background: transparent;
        width: 200px;
        height: 40px;
        border-radius: 6px;
        font-size: 14px;
        margin: 40px 0 0 0;
        cursor: pointer;
    }

    .attribute-btn button.join {
        background: $primary_color;
        color: #fff;
    }

    .attribute-btn button.exit {
        border: 1px solid $primary_color;
        margin-left: 20px;
        color: $primary_color;
    }

    .attribute-btn button.join:hover {
        background: $primary_color_aa;
    }

    .attribute-btn button.exit:hover {
        background: $primary_color_33;
    }
</style>
<!--矿池属性-->
<style scoped>
    .mining-attribute {
        position: absolute;
        top: 140px;
        left: calc(50% - 325px);
        width: 650px;
        border-radius: 6px;
        z-index: 9999;
        background: #fff;
    }

    .mining-attribute .attribute h1 {
        text-align: center;
        font-size: 20px;
        color: #555;
        font-weight: bold;
        padding: 28px 0;
    }

    .mining-attribute .attribute-img {
        width: 20px;
        height: 20px;
        position: relative;
        top: 3px;
    }

    .mining-attribute .attribute-value {
        padding: 0 50px 40px;
    }

    .attribute-value .info {
        border: 1px dashed #dbe2e8;
        outline: none;
        width: 100%;
        height: 60px;
        background: transparent;
        margin-top: 10px;
        color: $primary_color;
        font-size: 14px;
    }

</style>
<!--加入矿池-->
<style scoped>
    .join-pool {
        position: fixed;
        top: calc(50% - 128px);
        z-index: 9999;
        left: calc(50% - 250px);
        width: 500px;
        background: #fff;
        border-radius: 6px;
        text-align: center;
    }

    .join-pool .title {
        font-size: 16px;
        font-weight: bold;
        padding: 20px 0;
        border-bottom: 1px solid #d2d2d2;
    }

    .join-pool .attribute {
        font-size: 14px;
        padding: 20px 0;
        color: #555;
    }

    .join-pool .input {
        padding: 0 40px;
    }

    .join-pool .btn {
        margin: 30px 40px;
        height: 40px;
    }

    .join-pool .btn button{
        outline: none;
        width: 200px;
        height: 40px;
        border-radius: 4px;
        font-size: 14px;
        background: transparent;
        cursor: pointer;
    }

    .btn button.cancel{
        border: 1px solid $primary_color;
        color: $primary_color;
        float: left;
        background: #fff;
    }

    .btn button.confirm{
        float: right;
        background: $primary_color;
        color: #fff;
        border: none;
    }

    button.confirm {
        background: $primary_color;
        color: #fff;
        padding: 5px;
    }

    .btn button.cancel:hover {
        background: $primary_color_11;
    }

    .btn button.confirm:hover,
    button.confirm:hover {
        background: $primary_color_dd;
    }

</style>
<!--退出矿池-->
<style scoped>
    .exit-pool {
        position: fixed;
        width: 500px;
        height: 220px;
        top: calc(50% - 110px);
        left: calc(50% - 250px);
        background: #fff;
        border-radius: 4px;
        z-index: 9999;
    }

    .exit-pool .title {
        text-align: center;
        padding: 22px 0;
        font-weight: bold;
    }

    .exit-pool .info {
        text-align: center;
        padding-bottom: 20px;
        font-size: 14px;
        color: #555;
    }

    .exit-pool .btn {
        height: 40px;
        padding: 0 40px;
    }

    .exit-pool .btn button {
        outline: none;
        border-radius: 4px;
        width: 200px;
        height: 40px;
        font-size: 14px;
        cursor: pointer;
    }

</style>
<!--钱包内置兼容-->
<style>
    @media (max-width: 640px) {
        .main-content .pool-content .attribute-info {
            padding: 15px;
            border-top-right-radius: 0;
            border-top-left-radius: 0;
            margin: 0;
            height: 380px;
            background-position: center 180px;
        }

        .pool-attribute .pool-back {
            position: absolute;
            top: 15px;
            left: 15px;
            color: #fff !important;
            z-index: 9;
        }

        .attribute-info .attribute-text .pool-serial-number {
            position: absolute;
            left: 4%;
            bottom: 10px;
            font-weight: initial;
            font-size: 14px;
        }

        .main-content .pool-content .my-info {
            padding: 0 15px;
        }

        .pool-content .my-info .my-attribute .el-col.el-col-6 {
            width: 50%;
            margin: 0;
            padding-left: 0 !important;
            padding-right: 0 !important;
        }

        .pool-content .my-info .my-attribute .info {
            box-shadow: none;
            font-size: 14px;
            height: 60px;
        }

        .my-info .my-attribute .info .strong {
            font-size: 15px;
            font-weight: bold;
            margin: 0;
        }

        .pool-content .my-info .my-attribute {
            position: relative;
            margin: 0 0 20px 0;
        }

        .pool-content .my-info .my-attribute:after {
            content: "";
            display: inline-block;
            width: 100%;
            height: 1px;
            background: #d2d2d2;
            position: absolute;
            top: 60px;
        }

        .pool-content .my-info .my-attribute:before {
            content: "";
            display: inline-block;
            width: 1px;
            height: 120px;
            background: #d2d2d2;
            position: absolute;
            left: 50%;
        }

        .pool-content .my-info .attribute-btn button {
            width: 100%;
            margin: 10px 0;
        }

        .pool-content .attribute-info .pool-state {
            display: block;
        }

        .pool-content .attribute-info .earnings {
            top: 190px;
        }

        .pool-content #shouyi {
            top: 150px;
        }

        .pool-attribute .mining-attribute {
            width: calc(100% - 30px);
            position: fixed;
            top: calc(50% - 80px);
            left: 15px;
        }

        .pool-attribute .mining-attribute .img-close,
        .pool-attribute .exit-pool .img-close {
            /*display: none;*/
        }

        .pool-attribute .mining-attribute .btn {
            display: inline-block !important;
            width: 100%;
            height: 40px;
            outline: none;
            border: none;
            background: $primary_color;
            color: #fff;
            font-size: 15px;
            font-weight: bold;
            border-bottom-right-radius: 6px;
            border-bottom-left-radius: 6px;
        }

        .pool-attribute .mining-attribute .attribute h1 {
            padding: 15px 0;
            font-size: 14px;
        }

        .pool-attribute .mining-attribute .attribute-img {
            width: 16px;
            height: 16px;
            top: 2px;
        }

        .pool-attribute .mining-attribute .attribute-value {
            padding: 0 15px 10px;
        }

        .pool-attribute .attribute-value .info {
            font-size: 10px;
            height: 40px;
        }

        .pool-attribute .join-pool, .pool-attribute .exit-pool {
            width: calc(100% - 20px);
            left: 10px;
        }

        .pool-attribute .exit-pool {
            height: 180px;
        }

        .pool-attribute .join-pool .input,
        .pool-attribute .join-pool .btn,
        .pool-attribute .exit-pool .info,
        .pool-attribute .exit-pool .btn {
            padding: 0 15px;
            margin: 0;
        }

        .join-pool .btn button.cancel {
            display: none;
        }

        .join-pool .btn button.confirm {
            width: 100%;
            margin: 20px 0;
        }

        .pool-attribute .exit-pool .btn button {
            margin-top: 40px;
            width: 49%;
        }

        .pool-attribute .pool-content .my-info h1 {
            font-size: 15px;
            padding: 12px 0;
        }

        .pool-attribute .my-info .head-portrait {
            width: 16px;
            height: 16px;
            margin: 0 6px 0 0;
            top: 2px;
        }

    }
</style>
