<template>
    <div class="my-mining">
        <div class="tabs">
            <p @click="$router.back()" class="mining-back">{{$t('mining.attribute.return_previous')}}</p>
            <el-radio-group v-model="tabPosition" class="title">
                <el-radio-button label="join" class="btn">{{$t('mining.my_mining.i_join')}}</el-radio-button>
                <el-radio-button label="create" class="btn">{{$t('mining.my_mining.i_create')}}</el-radio-button>
            </el-radio-group>
        </div>
        <div v-if="tabPosition === 'join'">
            <div class="mining-list">
                <el-row :gutter="0">
                    <el-col :span="24" v-for="(mining,index) in joinList">
                        <div class="grid-content">
                            <div class="info">
                                <h2>{{$t('mining.index.pool')}}{{index}}</h2>
                                <p>{{mining.currentInvestment}}/{{mining.investmentTotal}}</p>
                                <el-progress :percentage="(mining.currentInvestment/mining.investmentTotal)*100"
                                             :show-text="false"></el-progress>
                            </div>
                            <div class="tag">
                                <p>
                                    <img src="../../assets/img/kuangchisouyi.png">
                                    <span>{{$t('mining.index.pool_income') + $global.getAmountFormat(mining.earnings)}}</span>
                                </p>
                                <p>
                                    <img src="../../assets/img/kuagnchifhenpei.png">
                                    <span>
                                        {{$t('mining.index.Income_distribution')}}
                                        {{(1 - mining.distribution)*100}}%
                                    </span>
                                </p>
                                <p>
                                    <img src="../../assets/img/kuangchishenyu.png">
                                    <span>{{$t('mining.index.remaining_mining')}}{{mining.remaining}}</span>
                                </p>
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </div>
        </div>
        <div v-if="tabPosition === 'create'">
            <div class="mining-list create">
                <el-row :gutter="0">
                    <el-col :span="24" v-for="(mining,index) in createList">
                        <div class="grid-content">
                            <div class="info">
                                <h2>{{$t('mining.index.pool')}}{{index}}</h2>
                                <p>{{mining.currentInvestment}}/{{mining.investmentTotal}}</p>
                                <el-progress :percentage="(mining.currentInvestment/mining.investmentTotal)*100"
                                             :show-text="false"></el-progress>
                            </div>
                            <div class="tag">
                                <p>
                                    <img src="../../assets/img/kuangchisouyi.png">
                                    <span>{{$t('mining.index.pool_income') + $global.getAmountFormat(mining.earnings)}}</span>
                                </p>
                                <p>
                                    <img src="../../assets/img/kuagnchifhenpei.png">
                                    <span>
                                        {{$t('mining.index.Income_distribution')}}
                                        {{(1 - mining.distribution)*100}}%
                                    </span>
                                </p>
                                <p>
                                    <img src="../../assets/img/kuangchishenyu.png">
                                    <span>{{$t('mining.index.remaining_mining')}}{{mining.remaining}}</span>
                                </p>
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </div>
        </div>
        <div class="info">{{$t('mining.my_mining.no_more')}}</div>
    </div>
</template>

<script>
    export default {
        name: "my-mining",
        data() {
            return {
                tabPosition: 'join',
                joinList: [],
                createList: [],
                joinPoolIds: {},
            }
        },
        methods: {
            myCreateList() {
                let _this = this;
                _this.$global.fetch("POST", {
                    creatorId: SSO.account
                }, "getPools").then(res => {
                    for (let n of Object.keys(res)) {
                        if (!Number(n)) continue;
                        let level = res[n].rule.level0 ? res[n].rule.level0 : res[n].rule.level1;
                        _this.createList.push({
                            investmentTotal: level.consignor.amount.max / 100000000,
                            currentInvestment: res[n].power / 100000000,
                            earnings: res[n].mintRewards,
                            distribution: level.forgepool.reward.max,
                            remaining: res[n].startBlockNo > res[n].updateHeight ? res[n].endBlockNo - res[n].startBlockNo : res[n].endBlockNo - res[n].updateHeight
                        });
                    }
                });
            },
            myJoinList() {
                let _this = this;
                _this.$global.fetch("POST", {}, "getPools").then(res => {
                    for (let mining of res.pools) {
                        if (!_this.joinPoolIds[mining.poolId]) continue;
                        let level = mining.rule.level0 ? mining.rule.level0 : mining.rule.level1;
                        _this.joinList.push({
                            investmentTotal: level.consignor.amount.max / 100000000,
                            currentInvestment: mining.power / 100000000,
                            earnings: mining.mintRewards,
                            distribution: level.forgepool.reward.max,
                            remaining: mining.startBlockNo > mining.updateHeight ? mining.endBlockNo - mining.startBlockNo : mining.endBlockNo - mining.updateHeight
                        });
                    }
                });
            },
            getJoinPoolId() {
                let _this = this;
                _this.$global.fetch("GET", {
                    account: SSO.accountRS,
                    type: 8,
                    subtype: 2,
                }, "getBlockchainTransactions").then(res => {
                    for (let t of res.transactions) {
                        _this.joinPoolIds[t.attachment.poolId] = t.signature;
                    }
                    _this.myJoinList();
                });
            }
        },
        created() {
            this.myCreateList();
            this.getJoinPoolId();
        }
    }
</script>
<style>
    .my-mining .tabs .el-radio-button__inner {
        width: 100%;
        border: none;
        box-shadow: initial;
        background-color: initial;
        color: #666;
        font-size: 15px;
        font-weight: bold;
    }

    .my-mining .tabs .is-active .el-radio-button__inner {
        color: #513ac8;
    }

    .my-mining .tabs .btn.is-active:after {
        content: "";
        display: inline-block;
        position: absolute;
        background: #513ac8;
        top: 30px;
        width: 60px;
        height: 4px;
        border-radius: 20px;
        left: calc(50% - 30px);
    }

</style>
<style scoped>
    .my-mining .tabs {
        padding: 30px 15px 0;
        background: #fff;
    }

    .my-mining .tabs .title {
        display: block;
        text-align: center;
        padding: 10px 0;
    }

    .my-mining .tabs .title .btn {
        width: 50%;
        outline: none;
    }

    .my-mining .info {
        text-align: center;
    }

    .my-mining .mining-list {
        padding: 15px;
    }

    .my-mining .mining-list .grid-content {
        background: #fff;
        border-radius: 6px;
        margin: 0 0 10px 0;
        font-size: 13px;
    }

    .mining-list .grid-content .info {
        width: 33%;
        display: inline-block;
        text-align: center;
        background: #513ac8;
        color: #fff;
        border-bottom-left-radius: 6px;
        border-top-left-radius: 6px;
        height: 120px;
        padding: 10px;
    }

    .grid-content .info h2 {
        padding: 15px 0 20px;
        font-size: 18px;
    }

    .grid-content .info p {
        margin: 0 0 5px 0;
    }

    .mining-list .grid-content .tag {
        display: inline-block;
    }

    .mining-list .grid-content .tag p {
        padding: 5px 5px 5px 10px;
    }

    .grid-content .tag p img {
        margin: 0 5px 0 0;
    }

    .mining-list + .info {
        color: #999;
    }

    .mining-list.create + .history {
        color: #513ac8;
        cursor: pointer;
    }

    .mining-list + div {
        padding: 5px 0 30px;
        text-align: center;
    }

</style>
