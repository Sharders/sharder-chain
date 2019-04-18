<template>
    <div class="mining" :class="this.$i18n.locale === 'en'? 'en_mining' : ''">
        <!--切换按钮-->
        <el-radio-group v-model="tabTitle" class="title">
            <el-radio-button label="mining" class="btn">{{$t('mining.index.sharder_mining')}}</el-radio-button>
            <el-radio-button label="welfare" class="btn">{{$t('mining.index.free_collar')}}</el-radio-button>
            <el-radio-button label="exchange" class="btn">{{$t('mining.index.ss_exchange')}}</el-radio-button>
        </el-radio-group>
        <!--豆匣矿场-->
        <div v-if="tabTitle === 'mining' && tabMenu === 'mining'">
            <div class="mining-content">
                <img src="../../assets/img/chatu.png" id="chatu">
                <div class="assets">
                    <ul>
                        <li>{{$t('mining.index.net_mining')}}{{$t('mining.index.net_mining_number',
                            {number:newestBlock.height})}}
                        </li>
                        <li>{{$t('mining.index.my_assets')}}{{$global.formatMoney(accountInfo.balanceNQT/100000000)}}
                            SS
                        </li>
                        <li>
                            {{$t('mining.index.my_income')}}{{$global.formatMoney(accountInfo.forgedBalanceNQT/100000000)}}
                            SS
                        </li>
                        <li class="strong">
                            <img src="../../assets/img/kuangchii_chakan.png">
                            <span @click="isVisible('isRanking')">{{$t('mining.index.view_ranking')}}</span>
                        </li>
                    </ul>
                </div>
                <div class="state">
                    <div class="state-info">
                        <span>{{$t('mining.attribute.mining')}}</span><br/>
                        <span>{{$t('mining.index.net_income') + $global.getSSNumberFormat(allIncome)}}</span>
                    </div>
                </div>
                <div class="instructions" @click="$router.push({name: 'rule-description'})">
                    {{$t('mining.index.mining_description')}}
                </div>
                <div class="invite-friends" @click="$router.push({name: 'invite-friends'})">
                    {{$t('mining.index.join_friends')}}
                </div>
                <div class="rule-description" @click="$router.push({name: 'rule-description'})">
                    {{$t('mining.index.rule_description')}}
                </div>
                <div class="my-mining create" @click="$router.push({name: 'my-mining'})">
                    <img src="../../assets/img/wodekuangchi.png">
                    <p>{{$t('mining.index.my_pool')}}</p>
                </div>
                <div class="create" @click="isVisible('isCreatePool')">
                    <img src="../../assets/img/chuanjiankuangchi.png">
                    <p>{{$t('mining.index.create_pool')}}</p>
                </div>
            </div>
            <div class="mining-notice">
                <img src="../../assets/img/guangbo.png" class="notice-img">
                <span class="notice-info">
                    {{$t('mining.index.mineral') + $t('mining.index.net_mining_number',{number:newestBlock.height})}} |
                    {{$t('mining.index.blocker') + newestBlockCreator}} | {{$t('mining.index.reward') + $global.getSSNumberFormat(newBlockReward)}}
            </span>
            </div>
            <div class="mining-list">
                <h5>
                    <div class="list-title">
                        <img src="../../assets/img/miner.svg" class="mining-list-img">
                        <span>{{$t('mining.index.pool_list')}}</span>
                        <span>{{$t('mining.index.pool_list_block')}}</span>
                    </div>
                    <el-select v-model="sortFun" v-if="miningList.length > 0">
                        <el-option
                            v-for="item in options"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value">
                        </el-option>
                    </el-select>
                </h5>
                <div class="mining-list-info">
                    <el-row :gutter="10">
                        <el-col :span="8" v-for="(mining,index) in miningList">
                            <div class="grid-content">
                                <div class="info" @click="poolAttribute(mining)">
                                    <h2>{{$t('mining.index.pool')}}{{index+1}}</h2>
                                    <p>{{mining.power/100000000}}/{{getAmountMax(mining.rule)}}</p>
                                    <el-progress
                                        :percentage="(mining.power/100000000)/(getAmountMax(mining.rule))*100"
                                        :show-text="false"></el-progress>
                                </div>
                                <div class="tag">
                                    <p>
                                        <img src="../../assets/img/kuangchisouyi.png">
                                        <span>{{$t('mining.index.pool_income') + $global.getSSNumberFormat(mining.mintRewards)}}</span>
                                    </p>
                                    <p>
                                        <img src="../../assets/img/kuagnchifhenpei.png">
                                        <span>
                                            {{$t('mining.index.Income_distribution') + $global.getRewardRate(mining.rule)}}
                                        </span>
                                    </p>
                                    <p>
                                        <img src="../../assets/img/kuangchishenyu.png">
                                        <span>{{$t('mining.index.remaining_mining') + getMinerBlock(mining) + $t('mining.index.unit_block')}}</span>
                                    </p>
                                </div>
                            </div>
                        </el-col>
                        <div v-show="miningList.length === 0" class="mining-list-null">
                            {{$t("mining.index.mining_no_pit_moment")}}
                        </div>
                    </el-row>
                </div>
            </div>
            <div class="mining-paging" v-if="miningList.length > 0">
                <el-pagination
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    :page-size="10"
                    layout="total, prev, pager, next ,jumper"
                    :total="totalSize">
                </el-pagination>
            </div>
        </div>
        <!--免费领SS-->
        <div v-if="tabTitle === 'welfare'">
            <div class="receive">
                <img src="../../assets/img/logo.svg" class="receive-qr-img">
                <p class="receive-text">
                    {{$t('mining.index.welfare_title1')}}<br>
                    {{$t('mining.index.welfare_title2')}}
                </p>
            </div>
        </div>
        <!--SS兑换-->
        <div v-if="tabTitle === 'exchange'">
            <ExchangeReward></ExchangeReward>
        </div>
        <!--个人中心-->
        <div v-if="tabMenu === 'personal'">
            <div class="personal-content">
                <div class="user">
                    <img src="../../assets/img/wodezichan.png" class="header-img">
                    <p>
                        <span>{{$t('mining.index.miner_name')}}</span>:
                        <span v-if="accountInfo.name !== undefined">{{accountInfo.name}}</span>
                        <span v-else>{{$t('mining.index.miner_name_not_defined')}}</span>
                        <img src="../../assets/img/set.png" @click="isVisible('isSetName')">
                    </p>
                    <p>
                        <span>{{$t('mining.index.tss_address')}}</span>:
                        <span>{{accountInfo.accountRS}}</span>
                        <img src="../../assets/img/TSS.png" @click="isVisible('isTSS')">
                    </p>
                </div>
                <div class="list" @click="$router.push({name: 'my-assets'})">
                    <img src="../../assets/img/wodezichan.png">
                    <span>{{$t('mining.index.my_assets')}}</span>
                </div>
                <div class="list" @click="$router.push({name: 'free-collar-drill'})">
                    <img src="../../assets/img/zhuanshi.png">
                    <span>{{$t('mining.index.free_collar_drill')}}</span>
                </div>
                <div class="list" @click="$router.push({name: 'invite-friends'})">
                    <img src="../../assets/img/haoyou.png">
                    <span>{{$t('mining.index.join_friend')}}</span>
                </div>
                <div class="list" @click="$router.push({name: 'diamond-exchange'})">
                    <img src="../../assets/img/zhuanshiduihuan.png">
                    <span>{{$t('mining.index.diamond_exchange')}}</span>
                </div>
                <div class="list">
                    <img src="../../assets/img/guanyuwomen.png">
                    <span>{{$t('mining.index.about_us')}}</span>
                </div>
                <div class="about">
                    <p>{{$t('mining.index.follow_us')}}</p>
                    <p>{{$t('mining.index.webside')}}</p>
                </div>
            </div>
        </div>
        <!--名称设置-->
        <div v-if="isSetName">
            <div class="set-name">
                <span class="img-close" @click="isVisible('isSetName')"></span>
                <h1>{{$t('mining.index.set_name')}}</h1>
                <div class="input">
                    <el-input v-model="accountName" :placeholder="$t('mining.index.set_name_tip')"></el-input>
                </div>
                <div class="determine" @click="setAccountInfo()">{{$t('mining.attribute.confirm')}}</div>
            </div>
        </div>
        <!--TSS说明-->
        <div v-if="isTSS">
            <div class="tss">
                <h1>{{$t('mining.index.tss_address_tile')}}</h1>
                <div class="text">
                    {{$t('mining.index.tss_address_subtitle1')}}
                    {{$t('mining.index.tss_address_subtitle2')}}
                </div>
                <div class="close" @click="isVisible('isTSS')">{{$t('mining.create_history.close')}}</div>
            </div>
        </div>
        <!--挖矿排行-->
        <div v-if="isRanking">
            <div class="ranking">
                <span class="img-close" @click="isVisible('isRanking')"></span>
                <div class="ranking-content">
                    <h3 class="ranking-title">{{$t('mining.index.mining_ranking')}}</h3>
                    <table class="ranking-table">
                        <tr>
                            <th>{{$t('mining.index.sort')}}</th>
                            <th>{{$t('mining.index.account')}}</th>
                            <th>{{$t('mining.index.ss_volume')}}</th>
                        </tr>
                        <tr v-for="(ranking,index) in rankingList">
                            <td>
                                <span v-if="index <= 2" :class="'ranking-logo bg-'+ index"></span>
                                <span v-if="index > 2">{{index+1}}</span>
                            </td>
                            <td>
                                {{idToAccountRs(ranking.ID)}}
                            </td>
                            <td>
                                {{ranking.BALANCE > 0 ? ranking.BALANCE / 100000000 : 0}}
                            </td>
                        </tr>
                    </table>
                    <div class="my-assets">
                        {{$t('mining.index.my_assets') + $global.getSSNumberFormat(accountInfo.balanceNQT)}}
                        | {{$t('mining.index.sort') + myRanking + $t('mining.index.unit_ming')}}
                    </div>
                </div>
            </div>
        </div>
        <!--创建矿池-->
        <div v-if="isCreatePool">
            <div class="create-pool">
                <span class="img-close" @click="isVisible('isCreatePool')"></span>
                <div class="create-pool-content">
                    <h3 class="pool-header">{{$t('mining.index.create_pool')}}</h3>
                    <div class="pool-attribute">
                        <h1 class="pool-title">
                            <img src="../../assets/img/kuangchi_attribute.png">
                            <span>{{$t('mining.index.pool_properties')}}</span>
                        </h1>
                        <p>
                            <span class="strong">{{$t('mining.index.pool_volume')}}</span>:
                            <span>{{miningList.length}}/{{maxPoolsNum}}</span>
                        </p>
                        <p>
                            <span class="strong">{{$t('mining.index.current_account')}}</span>:
                            <span>{{accountInfo.accountRS}}</span>
                        </p>
                        <p>
                            <span class="strong">{{$t('mining.index.pool_capacity')}}</span>:
                            <span>{{$global.getSSNumberFormat(maxPoolInvestment)}}</span>
                        </p>
                        <p>
                            <span class="strong">{{$t('mining.index.mining_time')}}</span>:
                            <span>{{rule.rule.totalBlocks.max + $t("mining.index.unit_block")}}({{parseInt(rule.rule.totalBlocks.max/60)}}h)</span>
                        </p>
                    </div>
                    <div class="pool-set">
                        <h1 class="pool-title">
                            <img src="../../assets/img/kuangchi_set.png">
                            <span>{{$t('mining.index.mining_setting')}}</span>
                        </h1>
                        <!--创建矿池时默认投入-->
                        <!--<div class="pool-data">-->
                        <!--<p>-->
                        <!--<span class="strong">{{$t('mining.index.invest_ss')}}</span>-->
                        <!--<span class="user-input">-->
                        <!--<el-input v-model="investment" :placeholder="$t('mining.index.invest_tip')"></el-input>-->
                        <!--</span>-->
                        <!--</p>-->
                        <!--<p>{{$t('mining.index.invest_ss_tip')}}</p>-->
                        <!--</div>-->
                        <div class="pool-data">
                            <p>
                                <span class="strong">{{$t('mining.index.income_distribution')}}</span>
                                <span class="user-input slider">
                                    <el-slider v-model="incomeDistribution"
                                               :min="rule.forgepool.reward.min | getPercentage"
                                               :max="rule.forgepool.reward.max | getPercentage"></el-slider>
                                </span>
                            </p>
                            <p>{{$t('mining.index.income_distribution_tip')}}</p>
                        </div>
                        <div class="pool-bth">
                            <button class="cancel" @click="isVisible('isCreatePool')">{{$t('enter.enter_cancel')}}
                            </button>
                            <button class="immediately-create" @click="createPool()">{{$t('mining.index.create_now')}}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!--菜单栏-->
        <div class="menu" style="display: none">
            <el-radio-group v-model="tabMenu" class="title">
                <el-radio-button label="mining" class="btn miner">{{$t('mining.index.mine')}}</el-radio-button>
                <el-radio-button label="personal" class="btn personal">{{$t('mining.index.personal_center')}}
                </el-radio-button>
            </el-radio-group>
        </div>
    </div>
</template>

<script>

    export default {
        name: 'mining',
        data() {
            return {
                isCreatePool: false,
                isRanking: false,
                isTSS: false,
                isSetName: false,
                tabTitle: 'mining',
                tabMenu: 'mining',
                maxPoolInvestment: 0,
                maxPoolsNum: 51,
                maxForgeTime: 1 * 60 * 60,
                options: [
                    {
                        value: 'default',
                        label: this.$t('mining.index.mining_sort_default')
                    },
                    {
                        value: 'capacity',
                        label: this.$t('mining.index.mining_sort_capacity')
                    },
                    {
                        value: 'distribution',
                        label: this.$t('mining.index.mining_sort_distribution')
                    },
                    {
                        value: 'time',
                        label: this.$t('mining.index.mining_sort_time')
                    }
                ],
                myRanking: {},
                sortFun: 'default',
                accountName: SSO.accountInfo.name,
                incomeDistribution: 0,
                investment: '',
                newestBlock: '',
                newBlockReward: 0,
                newestBlockCreator: '',
                totalAssets: 0,
                forgeAssets: 0,
                rule: null,
                avgBlocksTime: '',
                miningList: [],
                rewardList: [/*'1', '2', '3', '4'*/],
                rankingList: [],
                accountInfo: SSO.accountInfo,
                allIncome: 0,
                totalSize: 10,
            }
        },
        computed: {
            getLang: function () {
                return this.$store.state.currentLang;
            }
        },
        methods: {
            getAllIncome() {
                let _this = this;
                _this.$global.fetch("GET", {allIncome: "allIncome"}, "getAccount").then(res => {
                    if (res.success) {
                        _this.allIncome = res.cutIncome[0]["NUM"];
                    }
                });
            },
            getAmountMax(rule) {
                let level = rule.level0 ? rule.level0 : rule.level1;
                return level.consignor.amount.max / 100000000;
            },
            setAccountInfo() {
                let _this = this;
                _this.$global.fetch("POST", {
                    name: _this.accountName,
                    secretPhrase: SSO.secretPhrase,
                    deadline: 1440,
                    phased: false,
                    phasingHashedSecretAlgorithm: 2,
                    feeNQT: 0
                }, "setAccountInfo").then(res => {
                    if (!res.errorDescription) {
                        _this.accountInfo.name = res.transactionJSON.attachment.name;
                        _this.$message.success(_this.$t('notification.modify_success'));
                    } else {
                        _this.$message.error(res.errorDescription);
                    }
                    _this.isVisible('isSetName');
                });
            },
            idToAccountRs(id) {
                let nxtAddress = new NxtAddress();
                let accountRS = "";
                if (nxtAddress.set(id)) {
                    accountRS = nxtAddress.toString();
                }
                return accountRS;
            },
            createPool() {
                let _this = this;
                if (SSO.downloadingBlockchain) {
                    return _this.$message.warning(_this.$t("account.synchronization_block"));
                }

                if (_this.accountInfo.errorCode === 5 || SSO.publicKey === "") {
                    _this.isVisible('isCreatePool');
                    return _this.$message.info(_this.$t('notification.insufficient_permissions'));
                }

                _this.$global.fetch("POST", {
                    period: _this.rule.rule.totalBlocks.max,
                    secretPhrase: SSO.secretPhrase,
                    deadline: 1440,
                    feeNQT: 100000000,
                    rule: JSON.stringify({
                        'forgepool': {
                            'reward': _this.incomeDistribution / 100,
                            'number': _this.rule.forgepool.number.max - 1,
                        },
                        "rule": {
                            "totalBlocks": 0
                        },
                        'consignor': {
                            'amount': _this.maxPoolInvestment
                        }
                    })
                }, "createPool").then(res => {
                    if (res.broadcasted) {
                        _this.$message.success(_this.$t("mining.index.creating_success"));
                        _this.isVisible('isCreatePool');
                    } else {
                        _this.$message.error(res.errorDescription);
                    }
                });
            },
            poolAttribute(mining) {

                this.$router.push({name: "mining-attribute", params: {mining: mining, newestBlock: this.newestBlock}});
            },
            isVisible(val) {

                if (val === "isCreatePool" && this.rule === null) {
                    this.$message.error(this.$t("mining.index.pool_no_permissions"));
                    return;
                }
                this.$store.state.mask = !this[val];
                this[val] = !this[val];
            },
            handleSizeChange(val) {
                // console.log(`每页 ${val} 条`);
            },
            handleCurrentChange(val) {
                // console.log(`当前页: ${val}`);
            },
            account() {
                let _this = this;
                _this.$global.fetch("POST", {
                    shell: "account",
                    token: window.token,
                }, "authorizationLogin").then(value => {
                    console.info("authorizationLogin ", value);
                    if (!value.success) {
                        console.log("验证签名：", value.success);
                        history.back(-1);
                        return;
                    }
                    SSO.secretPhrase = value.data.secretPhrase;
                    Login.login(1, value.data.secretPhrase, _this, function () {
                        _this.$store.state.isLogin = true;
                        _this.loginAfter();

                    });
                });
            },
            loginAfter() {
                let _this = this;
                _this.getAllIncome();

                _this.$global.fetch("POST", {creatorId: SSO.account}, "getPoolRule").then(res => {
                    if (!res.errorDescription) {
                        _this.rule = res;
                        _this.maxPoolInvestment = res.consignor.amount.max - 100000000;
                    }
                });

                _this.getPools({sort: _this.sortFun});

                _this.$global.fetch("POST", {limit: 99999}, "getNextBlockGenerators").then(res => {
                    if (res.errorDescription) {
                        return _this.$message.error(res.errorDescription);
                    }
                    _this.newestBlock = res;
                    _this.newestBlockCreator = res.generators[0].accountRS;
                    _this.getCoinBase(res.height);
                });

                _this.$global.fetch("GET", {
                    firstIndex: 0,
                    lastIndex: 9
                }, "getBlocks").then(res => {
                    if (res.errorDescription) {
                        return _this.$message.error(res.errorDescription);
                    }
                    let len = res.blocks.length;
                    _this.avgBlocksTime = _this.$global.getAvgTimestamp(res.blocks[0].timestamp, res.blocks[len - 1].timestamp, len);
                });

                _this.$global.fetch("GET", {
                    account: SSO.account,
                    includeLessors: true,
                    includeAssets: true,
                    includeEffectiveBalance: true,
                    includeCurrencies: true,
                }, "getAccount").then(res => {
                    if (res.errorDescription) {
                        return _this.$message.error(res.errorDescription);
                    }
                    _this.accountInfo = res;
                });

                _this.getAssetsRanking();
                _this.getAccountRanking();

                if (!window.$miningInitial) {
                    window.$miningInitial = setInterval(() => {
                        if (_this.$router.currentRoute.name !== "mining") return;
                        _this.loginAfter();
                        _this.$forceUpdate();//通知Vue渲染
                    }, 10000);
                }
            },
            getCoinBase(height) {
                let _this = this;
                _this.$global.fetch("GET", {
                    height: height,
                    includeTransactions: true
                }, "getBlock").then(res => {
                    for (let t of res.transactions) {
                        if (t.type === 9) {
                            _this.newBlockReward = t.amountNQT;
                            break;
                        }
                    }
                });
            },
            getAssetsRanking() {
                let _this = this;
                _this.$global.fetch("POST", {
                    ranking: 10
                }, "getAccountRanking").then(res => {
                    if (res.success) {
                        _this.rankingList = res.data;
                    }
                });
            },
            getAccountRanking() {
                let _this = this;
                _this.$global.fetch("POST", {
                    account: SSO.account
                }, "getAccountRanking").then(res => {
                    if (res.success) {
                        _this.myRanking = res.data[0]['RANDKING'];
                    }
                });
            },
            getPools(parameter) {
                let _this = this;
                _this.$global.fetch("POST", parameter, "getPools").then(res => {
                    if (res.errorDescription) {
                        return _this.$message.error(res.errorDescription);
                    }
                    _this.miningList = res.pools;
                    _this.totalSize = _this.miningList.length;
                });
            },
            getMinerBlock(mining) {
                let _t = this;
                if (!_t.newestBlock) {
                    return _t.$global.placeholder
                }
                if (mining.startBlockNo > _t.newestBlock.height) {
                    return mining.endBlockNo - mining.startBlockNo
                } else {
                    return mining.endBlockNo - _t.newestBlock.height
                }
            }
        },
        created: function () {
            let _this = this;
            if (!_this.$store.state.isLogin) {
                window.token = window.location.search.substring(1 + "token".length);
                console.info("token", token);
                _this.account();
            } else {
                _this.loginAfter();
            }
        },
        watch: {
            getLang: {
                handler: function (oldValue, newValue) {
                    const _this = this;
                    _this.options = [
                        {
                            value: 'default',
                            label: this.$t('mining.index.mining_sort_default')
                        },
                        {
                            value: 'capacity',
                            label: this.$t('mining.index.mining_sort_capacity')
                        },
                        {
                            value: 'distribution',
                            label: this.$t('mining.index.mining_sort_distribution')
                        },
                        {
                            value: 'time',
                            label: this.$t('mining.index.mining_sort_time')
                        }
                    ];
                },
                deep: true
            },
            sortFun(v) {
                console.info(v);
                this.getPools({sort: v});
            }
        },
        filters: {
            getPercentage: function (val) {
                return parseFloat(val) * 100;
            }
        }
    }
</script>
<!--全局样式处理-->
<style>
    body {
        background: #00000010;
    }

    #app .page-layout main {
        height: initial !important;
        transform: initial !important;
        overflow-x: initial !important;
        position: initial !important;
        width: 1200px !important;
        margin: auto;
    }


    .el-select-dropdown__item.selected, .el-pager li.active {
        color: #fff !important;
    }

    input::-webkit-outer-spin-button,
    input::-webkit-inner-spin-button {
        -webkit-appearance: none;
    }

    input[type="number"] {
        -moz-appearance: textfield;
    }

    .mining .title .el-radio-button__inner {
        max-width: 140px;
        padding: 12px 41px;
    }

    .mining .title .el-radio-button__orig-radio:checked + .el-radio-button__inner,
    .el-select-dropdown__item.selected.hover, .el-select-dropdown__item.selected {
        background-color: #513ac8;
    }

    .mining .title .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
        color: #fff;
    }

    .mining .title .el-radio-button__inner:hover {
        color: #513ac8;
    }

    .mining .el-input {
        top: -4px;
        border: none;
    }

    .mining .mining-paging .el-input__inner {
        height: inherit;
    }

    .mining .mining-paging .el-input {
        width: 100px;
        margin: 0;
    }

    .mining P {
        margin: 0;
        padding: 0;
    }

    .mining .el-select {
        width: 110px !important;
    }

    .mining .mining-paging .el-pager li.active {
        background-color: #513acB;
        border: none;
    }

    .mining .mining-paging .el-pager li:hover {
        color: #513acB;
    }

    .mining .mining-paging .el-pager li.active:hover {
        color: #fff;
    }

    .mining .create-pool .el-slider__button,
    .mining .create-pool .el-slider__bar {
        background-color: #513acB;
    }

    .img-close {
        position: absolute;
        float: right;
        border-radius: 50%;
        width: 20px;
        height: 20px;
        right: 10px;
        top: 10px;
        cursor: pointer;
        background: url("../../assets/img/error.svg") no-repeat center;
    }

    .img-close:hover {
        opacity: 0.8;
    }

    .mining-back {
        position: absolute;
        left: 9px;
        top: 9px;
        color: #513ac8;
        cursor: pointer;
    }

    .mining-back:hover {
        color: #513ac8aa;
    }

    .mining-list .el-select .el-input .el-select__caret {
        top: 0 !important;
    }

    .mining-list .el-select .el-input__inner {
        height: 30px;
    }
    

    .en_mining .title .el-radio-button__inner {
        width: 140px;
        padding: 12px 25px;
    }


</style>
<!--豆匣矿场-->
<style scoped>

    .mining-content {
        position: relative;
        margin-top: 10px;
        border-top-left-radius: 6px;
        border-top-right-radius: 6px;
        height: 300px;
        padding: 30px;
        background: url("../../assets/img/kuangchi_bg.png") no-repeat center 140px;
        background-color: #513acB;
    }

    .mining-content .assets ul {
        color: #fff;
        float: left;
        font-size: 14px;
        margin: 0;
        padding: 0;
    }

    .assets li {
        margin-bottom: 10px;
        list-style: none;
    }

    .assets .strong {
        font-size: 16px;
        font-weight: bold;
        cursor: pointer;
    }

    .assets li img {
        width: 16px;
        height: 16px;
        position: relative;
        top: 2px;
    }

    .mining-content .state {
        text-align: center;
        width: calc(100% - 60px);
        position: absolute;
        top: 40px;
        word-break: break-all;
    }

    .state .state-info {
        display: inline-block;
        background-color: #20a0ff99;
        color: #14c6fc;
        font-size: 14px;
        font-weight: bold;
        border-radius: 4px;
        padding: 9px;
    }

    .instructions {
        cursor: pointer;
        position: relative;
        float: right;
        right: -30px;
        /*top: -20px;*/
        background-color: #0000ff;
        display: inline-block;
        font-size: 14px;
        color: #fff;
        padding: 10px 15px;
        border-bottom-left-radius: 20px;
        border-top-left-radius: 20px;
    }

    .invite-friends, .rule-description {
        cursor: pointer;
        display: none;
    }

    .create {
        position: relative;
        text-align: center;
        color: #fff;
        top: 180px;
        float: right;
        right: -130px;
        cursor: pointer;
    }

    .my-mining.create {
        margin: 0 0 0 10px;
        display: none;
    }

    .create img {
        width: 48px;
        height: 48px;
    }

    .create p {
        margin: 0;
        padding: 6px 0 0 0;
    }

    .mining-notice {
        text-align: center;
        background: #fff;
        padding: 9px 0;
        max-height: 40px;
        border-bottom-left-radius: 6px;
        border-bottom-right-radius: 6px;
    }

    .mining-notice .notice-img {
        width: 18px;
        height: 18px;
        position: relative;
        top: 3px;
    }

    .mining-notice .notice-info {
        font-size: 16px;
        color: #513ac8;
    }

    .mining-list .list-title {
        display: inline-block;
        padding: 20px 0 16px;
        font-size: 15px;
    }

    .mining-list .list-title + div {
        top: 3px;
    }

    .mining-list .mining-list-img {
        position: relative;
        top: 2px;
        margin-right: 6px;
        width: 14px;
        height: 14px;
    }

    .mining-list .mining-list-null {
        text-align: center;
        margin-top: 20px;
    }

    .grid-content {
        height: 120px;
        margin-bottom: 10px;
        background: #fff;
        border-radius: 6px;
        color: #fff;
    }

    .grid-content > div {
        display: inline-block;
        padding: 0 10px;
    }

    .grid-content .info {
        width: 120px;
        height: 120px;
        text-align: center;
        background-color: #513ac8;
        border-radius: 6px;
        cursor: pointer;
    }

    .grid-content .info h2 {
        font-size: 18px;
        padding: 30px 0;
        margin: 0;
    }

    .grid-content .info p {
        font-size: 12px;
        padding-bottom: 10px;
    }

    .grid-content .tag {
        width: 230px;
        height: 120px;
        color: #000;
        padding: 0;
        font-size: 15px;
        position: relative;
    }

    .grid-content .tag p {
        padding-bottom: 13px;
        position: relative;
        top: -6px;
    }

    .grid-content .tag img {
        padding: 0 12px 0 18px;
    }

    .mining-paging {
        position: relative;
        z-index: 99;
        float: right;
        margin-top: 20px;
    }

    .mining-paging > div {
        padding: 0;
        margin: 0;
    }

    @keyframes chatu {
        0% {
            top: 110px;
        }
        100% {
            top: 90px;
        }
    }

    #chatu {
        position: absolute;
        top: 110px;
        left: calc(50% - 34px);
        animation: chatu 1s infinite;
        /*播放动画myfirst 时间为 1秒 循环播放10次(infinite:循环播放)*/
        animation-direction: alternate;
        /*播放方式开始到结束,结束回到开始;*/
    }

    .mining .receive {
        text-align: center;
        height: 300px;
        background: #fff;
        margin-top: 10px;
        border-radius: 6px;
    }


    .en_mining .state .state-info {
        font-size: 12px;
    }

    .en_mining .state .state-info p:last-child {
        margin-top: 5px;
    }
</style>
<!--免费领SS-->
<style scoped>
    .receive .receive-qr-img {
        width: 160px;
        height: 160px;
        border-radius: 6px;
        margin: 40px 0 30px;
        background-color: #dbe2e8;
    }

    .receive .receive-text {
        color: #513ac8;
        font-size: 14px;
    }
</style>
<!--SS兑换-->
<style scoped>
    .reward .reward-title {
        margin-top: 10px;
        font-size: 14px;
        color: #333;
        padding: 22px 0;
        max-height: 60px;
        background-color: #fff;
        text-align: center;
        border-radius: 4px;
    }

    .reward-content .reward-content-div {
        background: #fff;
        height: 120px;
        padding: 20px;
        border-radius: 4px;
        margin-top: 20px;
    }

    .reward-content .reward-content-div > div {
        display: inline-block;

    }

    .reward-content-div .content-left-img {
        width: 50px;
        height: 50px;
        margin: 0 10px 0 0;
    }

    .reward-content-div .content-left {
        font-size: 16px;
        color: #999;
    }

    .content-left span.strong {
        font-size: 31px;
        font-weight: bold;
        color: #666;
        padding-right: 20px;
    }

    .content-left span {
        position: relative;
        top: -16px;
    }

    .content-left .reward-instructions {
        padding-top: 6px;
        max-width: 400px;
        font-size: 12px;
    }

    .reward-content-div .content-right {
        float: right;
        position: relative;
        top: calc(50% - 26px);
        margin-right: 10px;
    }

    .reward-content-div .content-right button {
        width: 120px;
        height: 50px;
        font-size: 16px;
        border-radius: 4px;
        background-color: #513ac8;
        border: none;
    }

    .content-right button:hover {
        background-color: #513ac8dd;
    }

    .content-right button:active {
        background-color: #513ac8aa;
    }

    .en_mining .content-left span.strong {
        font-size: 24px;
    }

    .en_mining .reward-content-div .content-right button {
        padding: 0;
    }

    .en_mining .reward .reward-title {
        font-size: 14px;
        text-align: left;
        padding: 10px 20px;
    }
</style>
<!--排行-->
<style scoped>
    .ranking {
        position: fixed;
        top: calc(50% - 300px);
        left: calc(50% - 250px);
        background-color: #fff;
        width: 500px;
        border-radius: 6px;
        text-align: center;
        z-index: 9999;
    }

    .ranking-content .ranking-title {
        padding: 20px 0;
        font-size: 16px;
        font-weight: bold;
        background-color: #462cae;
        color: #fff;
        border-top-left-radius: 6px;
        border-top-right-radius: 6px;
    }

    .ranking-content .ranking-table {
        width: 100%;
        text-align: center;
    }

    .ranking-table .ranking-logo {
        display: inline-block;
        width: 100px;
        height: 40px;
        background: no-repeat center;
    }

    .ranking-table .ranking-logo.bg-0 {
        background-image: url("../../assets/img/ranking_1.png");
    }

    .ranking-table .ranking-logo.bg-1 {
        background-image: url("../../assets/img/ranking_2.png");
    }

    .ranking-table .ranking-logo.bg-2 {
        background-image: url("../../assets/img/ranking_3.png");
    }

    .ranking-content .my-assets {
        padding: 20px 0;
        text-align: center;
        font-size: 14px;
        font-weight: bold;
    }

    .ranking-table th {
        font-weight: bold;
        height: 50px;
        min-width: 100px;
        font-size: 14px;
    }

    .ranking-table tr {
        height: 50px;
        border-bottom: 1px solid #f4f7fd;
    }
</style>
<!--创建矿池-->
<style scoped>
    .create-pool {
        position: fixed;
        z-index: 9999;
        top: calc(50% - 300px);
        left: calc(50% - 250px);
        background-color: #fff;
        width: 500px;
        border-radius: 6px;
    }

    .create-pool-content .pool-header {
        text-align: center;
        font-weight: bold;
        font-size: 16px;
        max-height: 60px;
        padding: 20px 0;
    }

    .create-pool-content .pool-title {
        font-size: 14px;
        font-weight: bold;
        padding-bottom: 20px;
    }

    .create-pool-content .pool-title img {
        position: relative;
        top: 4px;
    }

    .create-pool-content .pool-attribute {
        padding: 30px 40px;
        background-color: #513ac8;
        color: #fff;
    }

    .create-pool-content .pool-attribute p {
        margin-top: 20px;
        font-size: 14px;
    }

    .pool-attribute p .strong {
        font-weight: bold;
    }

    .create-pool-content .pool-set {
        padding: 30px 40px;
        color: #999;
        font-size: 14px;
        line-height: 24px;
    }

    .pool-data p {
        padding-bottom: 10px;
        position: relative;
    }

    .pool-data .strong {
        font-weight: bold;
        font-size: 16px;
        color: #000;
        display: inline-block;
        width: 70px;
    }

    .pool-data .user-input {
        width: 340px;
        display: inline-block;
    }

    .pool-data .user-input.slider {
        position: absolute;
        top: -5px;
        left: 76px;
    }

    .pool-set .pool-bth {
        margin-top: 50px;
    }

    .pool-set .pool-bth button {
        height: 40px;
        width: 200px;
        border-radius: 6px;
        outline: none;
        font-size: 16px;
        cursor: pointer;
    }

    .pool-bth .immediately-create {
        float: right;
        background-color: #513ac8;
        color: #fff;
        border: none;
    }

    .pool-bth .immediately-create:hover {
        background-color: #513ac8aa;
    }

    .pool-bth .cancel {
        background-color: #fff;
        color: #513ac8;
        border: 1px solid #513ac8;
    }

    .pool-bth .cancel:hover {
        background-color: #513ac810;
    }

    .en_mining .pool-data .strong {
        font-size: 12px;
    }
</style>
<!--钱包内置兼容-->
<style>
    @media (max-width: 640px) {
        #app .header {
            display: none;
        }

        #app .page-layout main {
            padding-top: 0;
            width: 100% !important;
        }

        #app .page-layout main .main-content {
            width: 100% !important;
        }

        .mining .el-radio-group {
            display: none;
        }

        .mining .mining-content {
            margin-top: 0;
            padding: 15px;
            border-radius: initial;
            height: 400px;
            background-position: center 210px;
        }

        .mining .mining-list-info .el-row {
            padding: 0 !important;
        }

        .mining .mining-list-info .el-col.el-col-8 {
            width: 100%;
            padding: 0 !important;
        }

        .mining .mining-list-info .grid-content .info {
            width: 35%;
        }

        .mining .mining-list-info .grid-content .tag {
            width: initial !important;
        }

        .mining .mining-list-info .grid-content .tag img {
            padding: 0 10px;
        }

        .mining .mining-content .instructions {
            display: none;
        }

        .mining .mining-content .invite-friends,
        .mining .mining-content .rule-description {
            display: inline-block;
            cursor: pointer;
            color: #fff;
            padding: 10px 20px;
            background: #0000ff;
            border-top-left-radius: 20px;
            border-bottom-left-radius: 20px;
            position: absolute;
            right: 0;
            top: 20px;
            font-size: 14px;
        }

        .mining .mining-content .rule-description {
            top: 66px;
        }

        .mining .mining-content .create {
            position: absolute;
            top: 320px;
            right: 75px;
            font-size: 13px;
        }

        .mining .mining-content .create.my-mining {
            display: initial;
            right: 15px;
        }

        .mining .mining-content .create img {
            width: 45px;
            height: 45px;
        }

        .mining .mining-paging {
            display: none;
        }

        .mining .mining-content .assets ul {
            font-size: 12px;
        }

        .mining .mining-content .assets .strong {
            font-size: 13px;
        }

        .mining .mining-content .assets .strong img {
            width: 12px;
            height: 12px;
            top: 1px;
        }

        .mining .mining-content .state {
            top: 110px;
            width: calc(100% - 30px);
        }

        .mining .mining-content .state .state-info {
            font-size: 12px;
            max-width: 100%;
        }

        .mining .mining-list .mining-list-img {
            margin-left: 15px;
        }

        .ranking-content .ranking-table {
            font-size: 12px;
        }

        #chatu {
            top: 170px !important;
            animation-name: chatu-mobel !important;
        }

        @keyframes chatu-mobel {
            0% {
                top: 150px;
            }
            100% {
                top: 170px;
            }
        }

        .mining .ranking, .mining .create-pool {
            position: absolute;
            width: calc(100% - 30px);
            left: 15px;
            top: 60px;
        }

        .mining .create-pool {
            position: fixed;
            top: calc(50% - 250px);
        }

        .mining .create-pool-content .pool-title {
            padding: 0;
            text-align: center;
            font-size: 15px;
        }

        .mining .pool-set .pool-title {
            color: #333;
            margin: 0 0 20px 0;
        }

        .pool-attribute p span {
            font-size: 15px;
            font-weight: bold;
        }

        .pool-attribute p .strong {
            font-size: 12px;
            font-weight: initial;
        }

        .create-pool .pool-header {
            display: none;
        }

        .mining .create-pool .pool-attribute, .mining .create-pool .pool-set {
            padding: 15px;
            font-size: 12px;
        }

        .mining .create-pool .pool-set .user-input {
            width: calc(100% - 90px);
            font-size: 12px;
        }

        .mining .create-pool .pool-set .pool-bth {
            margin: 0;
        }

        .mining .create-pool .pool-set .pool-bth .immediately-create {
            width: 100% !important;
            float: initial;
        }

        .mining .create-pool .pool-set .pool-bth .cancel {
            display: none;
        }

        .mining .menu {
            display: initial !important;
            position: fixed;
            left: 0;
            right: 0;
            bottom: 0;
            top: calc(100% - 70px);
            background: #fff;
        }

        .mining .menu .el-radio-group {
            display: block;
        }

        .mining .menu .el-radio-button {
            width: 50%;
        }

        .mining .menu .title .el-radio-button__inner {
            max-width: initial;
            width: 100%;
            height: 70px;
            border: none;
            outline: none;
            background-color: initial !important;
            box-shadow: none;
            font-size: 15px;
        }

        .menu .title .btn {
            background-size: 40px 40px !important;
        }

        .menu .title .btn.miner {
            background: url("../../assets/img/index.png") no-repeat center 26px;
        }

        .menu .title .btn.personal {
            background: url("../../assets/img/personal.png") no-repeat center 26px;
        }

        .menu .title .is-active.btn.miner {
            background: url("../../assets/img/index-1.png") no-repeat center 26px;
        }

        .menu .title .is-active.btn.personal {
            background: url("../../assets/img/personal-1.png") no-repeat center 26px;
        }

        .menu .el-radio-button__orig-radio:checked + .el-radio-button__inner {
            color: #513ac8 !important;
        }

        .mining .mining-list .mining-list-info {
            padding: 0 10px 70px 10px;
        }

        .ranking-table th {
            height: 30px !important;
        }

        .ranking-table tr {
            height: 40px !important;
        }
    }
</style>
<!--个人中心-->
<style>
    @media (max-width: 640px) {
        .mining .personal-content {
            padding: 15px 15px 0;
        }

        .mining .personal-content .user {
            height: 170px;
            width: 100%;
            background: #513ac8;
            border-radius: 4px;
            text-align: center;
            color: #fff;
            font-size: 12px;
            margin: 0 0 15px 0;
        }

        .mining .personal-content .user img.header-img {
            width: 80px;
            height: 80px;
            border-radius: 50%;
            background: #fff;
            margin: 15px 0;
            border: 10px solid #0000ff55;
            top: 0;
        }

        .mining .personal-content .user img {
            width: 13px;
            height: 13px;
            position: relative;
            top: 3px;
            margin-left: 4px;
            cursor: pointer;
        }

        .mining .personal-content .user p {
            margin: 0 0 10px 0;
        }

        .mining .personal-content .list {
            background: #fff;
            border-bottom: 1px solid #dbe2e8;
            padding: 10px 15px;
            font-size: 13px;
            color: #333;
            cursor: pointer;
        }

        .mining .personal-content .list img {
            width: 15px;
            height: 15px;
            position: relative;
            top: 2px;
            margin-right: 6px;
        }

        .mining .personal-content .about {
            padding: 60px 0 0;
            text-align: center;
            font-size: 12px;
            color: #999;
        }
    }
</style>
<!--TSS兑换-->
<style>
    @media (max-width: 640px) {
        .mining .tss {
            position: fixed;
            z-index: 9999;
            width: calc(100% - 20px);
            left: 10px;
            top: calc(50% - 100px);
            background: #fff;
            border-radius: 6px;
        }

        .mining .tss h1 {
            font-size: 14px;
            font-weight: bold;
            color: #333;
            padding: 15px 0;
            text-align: center;
        }

        .mining .tss .text {
            padding: 0 20px 20px;
            font-size: 13px;
            color: #333;
        }

        .mining .tss .close {
            background: #513ac8;
            cursor: pointer;
            color: #fff;
            width: 100%;
            font-size: 15px;
            font-weight: bold;
            padding: 15px 0;
            text-align: center;
            border-bottom-right-radius: 6px;
            border-bottom-left-radius: 6px;
        }

    }
</style>
<!--名称设置-->
<style>
    @media (max-width: 640px) {
        .mining .set-name {
            position: fixed;
            width: calc(100% - 20px);
            left: 10px;
            top: calc(50% - 50px);
            z-index: 9999;
            background: #fff;
            border-radius: 6px;
            font-size: 14px;
        }

        .mining .set-name h1 {
            font-weight: bold;
            padding: 15px;
            text-align: center;
        }

        .mining .set-name .input {
            padding: 0 15px 15px;
        }

        .mining .set-name .input input {
            text-align: center;
        }

        .mining .set-name .determine {
            width: 100%;
            background: #513ac8;
            padding: 15px 0;
            border-bottom-left-radius: 6px;
            border-bottom-right-radius: 6px;
            text-align: center;
            color: #fff;
        }

        .main-content .mining-notice .notice-img {
            width: 14px;
            height: 14px;
        }

        .main-content .mining-notice .notice-info {
            font-size: 10px;
        }

    }
</style>
