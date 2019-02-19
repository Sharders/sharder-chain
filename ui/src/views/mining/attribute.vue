<template>
    <div class="pool-attribute">
        <div>
            <p @click="$router.back()" class="pool-back">&lt;&lt;{{$t('mining.attribute.return_previous')}}</p>
            <div class="pool-content">
                <div class="attribute-info">
                    <img src="../../assets/img/shouyi.png" id="shouyi">
                    <div class="attribute-text">
                        <span class="pool-serial-number">
                            {{$t('mining.attribute.pool_number')}}{{$global.numToAzd(mining.poolId)}} | {{$t('mining.attribute.mining_probability')}}{{miningInfo.chance * 100}}%
                        </span>
                        <span class="pool-attribute-info" @click="miningMask('isAttribute')">{{$t('mining.attribute.pool_details')}}</span>
                    </div>
                    <div class="pool-state">
                        <h1>{{$t('mining.attribute.mining')}}</h1>
                        <h1>{{$t('mining.attribute.mining_current_number1')}}<span
                            class="number">{{newestBlock.height}}</span>{{$t('mining.attribute.mining_current_number2')}}
                        </h1>
                    </div>
                    <div class="earnings">{{$t('mining.attribute.income')}}+{{miningInfo.income/100000000}}SS</div>
                </div>
                <div class="my-info">
                    <h1>
                        <img src="../../assets/img/wodexingxi.png" class="head-portrait">
                        <span>{{$t('mining.attribute.self_info')}}</span>
                    </h1>
                    <div class="my-attribute">
                        <el-row :gutter="20">
                            <el-col :span="6">
                                <button class="info">
                                    <p>{{$t('mining.attribute.join_time')}}</p>
                                    <p class="strong">{{miningInfo.timestamp === 0 ?
                                        '未加入':$global.myFormatTime(miningInfo.timestamp,'YMDHMS',true)}}</p>
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
                                    <p class="strong">
                                        <!--{{(miningInfo.joinAmount/miningInfo.investmentTotal)*miningInfo.income / 100000000}} SS-->
                                        {{miningInfo.income / 100000000}} SS
                                    </p>
                                </button>
                            </el-col>
                            <el-col :span="6">
                                <button class="info">
                                    <p>{{$t('mining.attribute.remaining_mining_time')}}</p>
                                    <p class="strong">{{miningInfo.startBlockNo > newestBlock.height ?
                                        miningInfo.endBlockNo - miningInfo.startBlockNo : miningInfo.endBlockNo -
                                        newestBlock.height}}</p>
                                </button>
                            </el-col>
                        </el-row>
                    </div>
                    <div class="attribute-btn">
                        <button class="join" @click="miningMask('isJoinPool')">
                            {{$t('mining.attribute.investing_diamonds')}}
                        </button>
                        <button v-if="myAccount !== miningInfo.account" class="exit" @click="miningMask('isExitPool')">
                            {{$t('mining.attribute.exit_pool')}}
                        </button>
                        <button v-else class="exit" @click="miningMask('isDestroyPool')">
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
                        <img src="../../assets/img/pay.svg" class="attribute-img">
                        <span>{{$t('mining.attribute.pool_details')}}</span>
                    </h1>
                    <div class="attribute-value">
                        <el-row :gutter="20">
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.creator')}}{{miningInfo.account}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.participating_users')}}{{miningInfo.amount}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.pool_number')}}{{$global.numToAzd(mining.poolId)}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.capacity')}}{{miningInfo.currentInvestment/100000000}}/{{miningInfo.investmentTotal/100000000}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.pool_income')}}{{miningInfo.income/100000000}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    {{$t('mining.attribute.reward_distribution')}}
                                    {{(1-miningInfo.level.forgepool.reward.max/1)*100 }}%
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
                    {{$t('mining.attribute.currently_available')}}
                    {{(miningInfo.investmentTotal - miningInfo.currentInvestment)/100000000}}SS |
                    {{$t('mining.attribute.pool_capacity')}}{{miningInfo.investmentTotal/100000000}}SS
                </p>
                <p class="input">
                    <el-input v-model="joinPool" type="number"
                              :placeholder="$t('mining.attribute.join_pool_tip')"></el-input>
                </p>
                <p class="btn">
                    <button class="cancel" @click="miningMask('isJoinPool')">{{$t('mining.attribute.cancel')}}</button>
                    <button class="confirm" @click="miningJoin">{{$t('mining.attribute.confirm')}}</button>
                </p>
            </div>
        </div>
        <!--退出矿池-->
        <div v-if="isExitPool">
            <div class="exit-pool">
                <span class="img-close" @click="miningMask('isExitPool')"></span>
                <h1 class="title">{{$t('mining.attribute.exit_pool')}}</h1>
                <p class="info">{{$t('mining.attribute.exit_pool_tip')}}</p>
                <p class="btn">
                    <button class="cancel" @click="miningMask('isExitPool')">{{$t('mining.attribute.cancel')}}</button>
                    <button class="confirm" @click="miningExit">{{$t('mining.attribute.confirm')}}</button>
                </p>
            </div>
        </div>
        <!--删除矿池-->
        <div v-if="isDestroyPool">
            <div class="exit-pool">
                <span class="img-close" @click="miningMask('isDestroyPool')"></span>
                <h1 class="title">{{$t('mining.attribute.destroy_pool')}}</h1>
                <p class="info">{{$t('mining.attribute.destroy_pool_tip')}}</p>
                <p class="btn">
                    <button class="cancel" @click="miningMask('isDestroyPool')">{{$t('mining.attribute.cancel')}}
                    </button>
                    <button class="confirm" @click="miningDestroy()">{{$t('mining.attribute.confirm')}}</button>
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
                miningInfo: {
                    account: '',
                    accountId: "",
                    amount: 0,
                    joinAmount: 0,
                    poolId: '',
                    currentInvestment: 0,
                    investmentTotal: 0,
                    income: 0,
                    chance: 0,
                    timestamp: 0,
                    startBlockNo: 0,
                    endBlockNo: 0,
                    level: {}
                }
            }
        },
        methods: {
            miningExit() {
                let _this = this;
                if (SSO.downloadingBlockchain) {
                    return this.$message.warning("当前正在同步区块链，请稍后再试");
                }
                this.$http.get('sharder?requestType=getBlockchainTransactions', {
                    params: {
                        account: SSO.accountRS,
                        type: 8
                    }
                }).then(res => {
                    if (res.data.errorDescription) {
                        return _this.$message.error(res.data.errorDescription);
                    }
                    for (let t of res.data.transactions) {
                        if (t.attachment.poolId === _this.miningInfo.poolId) {
                            _this.$global.fetch("POST", {
                                txId: t.transaction,
                                poolId: _this.miningInfo.poolId,
                                period: 400,
                                secretPhrase: SSO.secretPhrase,
                                deadline: 1440,
                                feeNQT: 100000000
                            }, "quitPool").then(res => {
                                if (res.errorDescription) {
                                    _this.$message.error(res.errorDescription);
                                }
                            });
                        }

                    }
                    _this.$message.warning("请求已提交...");
                    _this.$store.state.mask = false;
                    _this.isExitPool = false;
                }).catch(err => {
                    _this.$message.error(err);
                });
            },
            miningDestroy() {
                let _this = this;
                if (SSO.downloadingBlockchain) {
                    this.$message.warning("当前正在同步区块链，请稍后再试");
                    return;
                }
                _this.$global.fetch("POST", {
                    period: 400,
                    secretPhrase: SSO.secretPhrase,
                    deadline: 1440,
                    feeNQT: 100000000,
                    poolId: _this.miningInfo.poolId,
                }, "destroyPool").then(res => {
                    if (res.errorDescription) {
                        return _this.$message.error(res.errorDescription);
                    }
                    _this.$store.state.mask = false;
                    _this.isDestroyPool = false;
                    return _this.$message.success("删除成功");
                });
            },
            miningJoin() {
                let _this = this;
                if (_this.validationJoinMining()) return;
                _this.$global.fetch("POST", {
                    period: 400,
                    secretPhrase: SSO.secretPhrase,
                    deadline: 1440,
                    feeNQT: 100000000,// 手续费默认是 1 SS
                    poolId: _this.mining.poolId,
                    amount: _this.joinPool * 100000000
                }, "joinPool").then(res => {
                    if (typeof res.errorDescription === "undefined") {
                        _this.$message.success("加入成功");
                        _this.$store.state.mask = false;
                        _this.isJoinPool = false;
                    } else {
                        _this.$message.error(res.error);
                    }
                }).catch(err => {
                    console.log(err);
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
                _this.$global.fetch("POST", {
                    account: SSO.account,
                    poolId: _this.mining.poolId,
                }, "getPoolInfo").then(res => {
                    console.info(res);
                    _this.miningInfo.amount = res.number;
                    _this.miningInfo.poolId = res.poolId;
                    _this.miningInfo.currentInvestment = res.power;
                    _this.miningInfo.accountId = res.creatorID;
                    _this.miningInfo.income = res.historicalIncome;
                    _this.miningInfo.chance = res.chance;
                    _this.miningInfo.startBlockNo = res.startBlockNo;
                    _this.miningInfo.endBlockNo = res.endBlockNo;
                    _this.miningInfo.level = res.rule.level0 ? res.rule.level0 : res.rule.level1;
                    _this.miningInfo.joinAmount = res.joinAmount;
                    _this.miningInfo.investmentTotal = _this.miningInfo.level.consignor.amount.max;
                    let nxtAddress = new NxtAddress();
                    if (nxtAddress.set(res.creatorID)) {
                        _this.miningInfo.account = nxtAddress.toString();
                    }
                });
            },
            validationJoinMining() {
                if (SSO.downloadingBlockchain) {
                    return this.$message.warning("当前正在同步区块链，请稍后再试");
                }
                if (this.joinPool <= 1) {
                    return this.$message.error("投入数量必须大于1");
                }
                if (this.miningInfo.currentInvestment + this.joinPool * 100000000 > this.miningInfo.investmentTotal) {
                    return this.$message.error("超出矿池总上限");
                }
            },
            myJoinTime() {
                let _this = this;
                _this.$global.fetch("POST", {
                    account: _this.myAccount,
                    type: 8,
                    subtype: 2,
                    lastIndex: 0
                }, "getBlockchainTransactions").then(res => {
                    // console.info(res);
                    for (let t of res.transactions) {
                        if (t.attachment.poolId === _this.miningInfo.poolId) {
                            _this.miningInfo.timestamp = t.timestamp;
                            break;
                        }
                    }
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
<style scoped>
    .pool-attribute .pool-back {
        font-size: 16px;
        color: #493eda;
        cursor: pointer;
    }

    .pool-content .attribute-info {
        padding: 30px;
        background: url("../../assets/img/kuangchi_bg.png") no-repeat center 140px;
        background-color: #513acB;
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
        color: #1bc98e;
    }

    .attribute-info .pool-attribute-info {
        float: right;
        cursor: pointer;
        font-size: 11px;
        border-bottom: 1px solid #fff;
    }

    .attribute-info .earnings {
        width: 100px;
        text-align: center;
        margin: auto;
        position: absolute;
        top: 180px;
        left: calc(50% - 50px);
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
        color: #333;
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
        box-shadow: 0 0 2px #513acBaa;
        height: 100px;
        width: 100%;
        border: none;
        outline: none;
        border-radius: 4px;
        padding: 0;
        font-size: 16px;
        color: #333;
        background: transparent;
    }

    .my-attribute .info .strong {
        font-size: 20px;
        margin-top: 16px;
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
        background: #513acB;
        color: #fff;
    }

    .attribute-btn button.exit {
        border: 1px solid #513acB;
        margin-left: 20px;
        color: #513acB;
    }

    .attribute-btn button.join:hover {
        background: #513acBaa;
    }

    .attribute-btn button.exit:hover {
        background: #513acB33;
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
        color: #333;
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
        color: #513acB;
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
        color: #333;
    }

    .join-pool .input {
        padding: 0 40px;
    }

    .join-pool .btn {
        margin: 30px 40px;
        height: 40px;
    }

    .join-pool .btn button {
        outline: none;
        width: 200px;
        height: 40px;
        border-radius: 4px;
        font-size: 14px;
        background: transparent;
        cursor: pointer;
    }

    .btn button.cancel {
        border: 1px solid #513acB;
        color: #513acB;
        float: left;
        background: #fff;
    }

    .btn button.confirm {
        float: right;
        background: #513acB;
        color: #fff;
        border: none;
    }

    .btn button.cancel:hover {
        background: #513acB11;
    }

    .btn button.confirm:hover {
        background: #513acBdd;
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
        padding: 35px;
        font-size: 14px;
        color: #333;
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
            right: 15px;
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
            display: none;
        }

        .pool-attribute .mining-attribute .btn {
            display: inline-block !important;
            width: 100%;
            height: 40px;
            outline: none;
            border: none;
            background: #513acB;
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
