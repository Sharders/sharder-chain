<template>
    <div class="my-assets">
        <p @click="$router.back()" class="mining-back">{{$t('mining.attribute.return_previous')}}</p>
        <div class="assets">
            <div class="assets-info">
                <div class="totle-assets">
                    <span>
                        <span style="font-size: x-large">{{$t('mining.my_assets.total_asset')}}</span>
                        <span style="font-size: small">({{$t('account_info.account_mining_balance')}}：{{accountInfo.forgedBalanceNQT/100000000}})</span>
                    </span>
                    <span class="strong">{{accountInfo.effectiveBalanceNQT/100000000 + accountInfo.frozenBalanceNQT/100000000}}</span>
                </div>
              <!--  <p class="exchang" @click="$router.push({name: 'free-collar-drill'})">
                    {{$t('mining.index.diamond_exchange')}}</p>-->
                <div class="assets-detail">
                    <div>
                        <p>{{$t('mining.my_assets.available_asset')}}</p>
                        <p class="strong">{{accountInfo.effectiveBalanceNQT/100000000}}</p>
                    </div>
                    <div>
                        <p style="float: right;">{{$t('mining.my_assets.frozen_assets')}}</p>
                        <p class="strong" style="float: right;">{{accountInfo.frozenBalanceNQT/100000000}}</p>
                    </div>
                </div>
            </div>
            <div class="assets-header">{{$t('mining.my_assets.asset_record')}}</div>
            <div class="transaction_type">
                    <span class="btn" :class="activeSelectType(0)" @click="selectType = 0">
                        {{$t('transaction.transaction_type_payment')}}
                    </span>
                <span class="btn" :class="activeSelectType(8)" @click="selectType = 8">
                        {{$t('transaction.transaction_type_forge_pool')}}
                    </span>
                <span class="btn" :class="activeSelectType(9)" @click="selectType = 9">
                        {{$t('transaction.transaction_type_system_reward')}}
                    </span>
                <el-select v-model="selectType" :placeholder="$t('transaction.transaction_type_all')">
                    <el-option
                        v-for="item in transactionType"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value">
                    </el-option>
                </el-select>
            </div>
            <div class="assets-list" v-for="al in assetsList">
                <div class="title">
                    <p class="strong">{{al.title}}</p>
                    <p>{{al.time}}</p>
                </div>
                <div class="number">{{al.num}}</div>
            </div>
            <div class="load-assets">
                <p v-if="isPage" @click="loadAssets()">{{$t("mining.my_assets.click_load")}}</p>
                <p v-else>{{$t("mining.my_assets.whether")}}</p>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "my-assets",
        data() {
            return {
                accountInfo: {
                    balanceNQT: 0,              //账户余额
                    effectiveBalanceNQT: 0,      //可用余额
                    forgedBalanceNQT: 0,        //挖矿余额
                    frozenBalanceNQT: 0,        //冻结余额
                },
                pageNO: 1,
                isPage: true,
                assetsList: [],
                selectType: '',
                epochBeginning: 0,
                transactionType: [{
                    value: '',
                    label: this.$t('transaction.transaction_type_all')
                }, {
                    value: 0,
                    label: this.$t('transaction.transaction_type_payment')
                }, {
                    value: 8,
                    label: this.$t('transaction.transaction_type_forge_pool')
                }, {
                    value: 9,
                    label: this.$t('transaction.transaction_type_system_reward')
                }],
            }
        },
        methods: {

            getAccount(account) {
                return new Promise((resolve) => {
                    this.$http.get('/sharder?requestType=getAccount', {
                        params: {
                            account: account,
                            includeLessors: true,
                            includeAssets: true,
                            includeEffectiveBalance: true,
                            includeCurrencies: true,
                        }
                    }).then(function (res) {
                        resolve(res.data);
                    }).catch(function (err) {
                        console.log(err);
                    });
                });
            },
            getAssetsList(status) {
                let _this = this;
                if(status === "selectType"){
                    _this.assetsList = [];
                    _this.isPage = true;
                }
                let params = new URLSearchParams();
                params.append("account", SSO.accountRS);
                params.append("firstIndex", (_this.pageNO - 1) * 10);
                params.append("lastIndex", (_this.pageNO - 1) * 10 + 9);

                if (_this.selectType === 1.5) {
                    params.append("type", "1");
                    params.append("subtype", "5");
                } else if (_this.selectType === 1) {
                    params.append("type", "1");
                    params.append("subtype", "0");
                } else {
                    params.append("type", _this.selectType);
                }
                _this.$http.get('/sharder?requestType=getBlockchainTransactions', {params}).then(function (res) {
                    if (res.data.transactions.length === 0) {
                        return _this.isPage = false;
                    }
                    for (let t of res.data.transactions) {
                        console.info(t);
                        if(_this.selectType === ""){
                            _this.assetsList.push({
                                title: _this.$global.getTransactionTypeStr(t),
                                time: _this.formatTime(t.timestamp),
                                num: _this.$global.getTransactionAmountNQT(t, _this.accountInfo.accountRS)
                            })
                        }else{
                            if (_this.selectType === 1 && t.subtype === 0) {
                                _this.assetsList.push({
                                    title: _this.$global.getTransactionTypeStr(t),
                                    time: _this.formatTime(t.timestamp),
                                    num: _this.$global.getTransactionAmountNQT(t, _this.accountInfo.accountRS)
                                });
                            } else if (_this.selectType !== 1 && _this.selectType === t.type) {
                                _this.assetsList.push({
                                    title: _this.$global.getTransactionTypeStr(t),
                                    time: _this.formatTime(t.timestamp),
                                    num: _this.$global.getTransactionAmountNQT(t, _this.accountInfo.accountRS)
                                });
                            } else if (_this.selectType === 1.5 &&
                                t.type === 1 &&
                                t.subtype === 5) {
                                _this.assetsList.push({
                                    title: _this.$global.getTransactionTypeStr(t),
                                    time: _this.formatTime(t.timestamp),
                                    num: _this.$global.getTransactionAmountNQT(t, _this.accountInfo.accountRS)
                                });
                            }

                        }
                    }
                });
            },

            activeSelectType(type) {
                const _this = this;
                return _this.selectType === type ? 'active' : ''
            },

            loadAssets() {
                const _this = this;
                _this.pageNO++;
                let status = "loadAssets";
                _this.getAssetsList(status);
            },

            getEpochBeginningTime(){
                const _this = this;
                _this.$global.fetch("GET",{},"getConstants").then(function (res) {
                    _this.epochBeginning = parseInt(res.epochBeginning);
                })
            },

            formatTime(time,fmt,tz){
                const _this = this;
                fmt = fmt || "yyyy-MM-dd hh:mm:ss";
                tz = tz || 0;
                let date = new Date(time * 1000 + _this.epochBeginning + tz * 3600000);
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
                    return fmt;

            }
        },
        created() {
            const  _this = this;
            _this.getAccount(SSO.accountRS).then(res => {
                _this.accountInfo.balanceNQT = res.balanceNQT;
                _this.accountInfo.effectiveBalanceNQT = res.effectiveBalanceNQT;
                _this.accountInfo.forgedBalanceNQT = res.forgedBalanceNQT;
                _this.accountInfo.frozenBalanceNQT = res.frozenBalanceNQT;
            });
            _this.getEpochBeginningTime();
            _this.getAssetsList();
        },
        watch:{
            selectType: function () {
                const _this = this;
                _this.pageNO = 1;
                let status = "selectType";
                _this.getAssetsList(status);
            },
        }
    }

</script>
<style lang="scss" type="text/scss">
    @import './style.scss';
</style>
<style scoped>

    .my-assets .assets {
        padding: 30px 15px 0;
        background: #fff;
    }

    .assets .assets-info {
        background: #513ac8;
        border-radius: 4px;
        color: #ddd;
        padding: 25px;
        font-size: 14px;
        position: relative;
    }

    .assets .load-assets {
        text-align: center;
        padding: 10px 0;
    }

    .assets-info .totle-assets .strong {
        display: block;
        font-size: 30px;
        font-weight: bold;
        padding: 15px 0 10px;
        border-bottom: 1px solid #fff;
        color: #fff;
    }

    .assets .assets-info .exchang {
        position: absolute;
        right: 24px;
        top: 30px;
        cursor: pointer;
        color: #fff;
    }

    .assets-info .assets-detail > div {
        display: inline-block;
        width: 49%;
    }

    .assets-info .assets-detail p {
        padding: 10px 0 0 0;
    }

    .assets-info .assets-detail .strong {
        font-size: 20px;
        overflow: hidden;
    }

    .assets .assets-header {
        font-size: 12px;
        color: #666;
        padding: 15px 0;
    }

    .assets .assets-list {
        font-size: 12px;
        position: relative;
        color: #666;
        height: 50px;
        border-top: 1px solid #ddd;
    }

    .assets .assets-list .number {
        position: absolute;
        right: 0;
        top: calc(50% - 6px);
        font-weight: bold;
        color: #333;
    }

    .assets .assets-list .title .strong {
        font-weight: bold;
        padding: 10px 0 4px;
        color: #333;
    }

</style>
