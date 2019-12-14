<template>
    <div class="exchange">
        <div class="exchange-header">
            <h1>{{$t('mining.diamond_exchange.diamond_exchange_title')}}</h1>
            <p v-show="isSSA">
                <span >
                    <li v-if="sharderAccount">{{$t("reward.binding_account")}} : {{sharderAccount}}</li>
                    <li v-if="sharderAccount">{{$t("reward.convertible")}} : {{convertible}} MWFS</li>
                    <li v-if="sharderAccount">{{$t("reward.redeemed")}} : {{redeemed}} MWFS</li>
                    <li v-if="sharderAccount" style="color: #e64242;font-size: small">{{$t('mining.diamond_exchange.description')}}{{$t("reward.exchange_tip")}} </li>
                    <li v-else>
                        {{$t("reward.no_binding_account")}} ?
                        <a href="https://sharder.org">{{$t("reward.immediately_binding")}}</a>
                    </li>
                </span>
            </p>
            <p v-if="exchangeOpen">{{$t('mining.diamond_exchange.diamond_exchange_subtitle')}}</p>
        </div>
        <div v-if="exchangeOpen && !displayDefault" class="exchange-list" :class="(index+1)%3 === 0 ? ' right' :''" v-for="(exchange,index) in exchangeList" v-loading="loadingExchangeSS">
            <p>
                <img :src="exchange.img" class="exchange-img">
                <span class="title">{{exchange.title}}</span>
            </p>
            <p>{{$t('mining.diamond_exchange.description')}}{{exchange.info}}</p>
            <button @click="exchangeFun(exchange)">{{$t("reward.exchange")}}</button>
        </div>

        <div class="exchange-list info" v-if="displayDefault">
            {{$t('reward.insufficient_redemption')}}
        </div>
        <div class="exchange-list info" v-else>
            <span v-if="exchangeOpen">
                {{$t('mining.diamond_exchange.not_open_tip')}}
            </span>
            <span v-else>
                {{$t('mining.diamond_exchange.not_open')}}
            </span>
        </div>

        <!--申请兑换MWFS列表-->
        <div v-if="exchangeOpen && sharderAccount" class="block_list"  style="clear:both">
            <p class="block_title" style="padding-bottom: 10px;">
                <img src="../../assets/img/block.svg" width="20px" height="20px"/>
                <span>{{$t('exchange_list.exchange_title')}}</span>
            </p>
            <div class="list_table w br4">
                <div class="list_content data_container table_responsive data_loading">
                    <table class="table table_striped" id="blocks_table">
                        <thead>
                        <tr>
                            <th>{{$t('exchange_list.appliction_time')}}</th>
                            <th>{{$t('exchange_list.exchange_type')}}</th>
                            <th>{{$t('exchange_list.exchange_amount')}}</th>
                            <th>{{$t('exchange_list.exchange_status')}}</th>
                        </tr>
                        </thead>
                         <tbody>
                         <tr v-for="exchange in exchangeSSList">
                             <td>
                                 <span>{{exchange.createDate}}</span>
                             </td>
                             <td>
                                 <span v-if="exchange.source === 'EXCHANGESS'">{{$t('exchange_list.application_exchange')}}</span>
                                 <span v-if="exchange.source === 'SYSTEM'">{{$t('exchange_list.system_replacement')}}</span>
                             </td>
                             <td>
                                 <span>{{(exchange.awardAmount)}}</span>
                             </td>
                             <td>
                                 <span v-if="exchange.status === 2"><el-tag type="info">{{$t('exchange_list.status_pending')}}</el-tag></span>
                                 <span v-if="exchange.status === 3"><el-tag type="success">{{$t('exchange_list.status_issued')}}</el-tag></span>
                                 <span v-if="exchange.status === 4"><el-tag type="danger">{{$t('exchange_list.status_refuse')}}</el-tag></span>
                             </td>

                         </tr>
                         </tbody>
                    </table>
                </div>
                <!-- <div class="list_pagination">
                     <el-pagination
                         @size-change="handleSizeChange"
                         @current-change="handleCurrentChange"
                         :current-page.sync="currentPage"
                         :page-size="pageSize"
                         layout="total, prev, pager, next, jumper"
                         :total="totalSize">
                     </el-pagination>
                 </div>-->
            </div>
        </div>
    </div>

</template>

<script>
    export default {
        name: "exchange-reward",
        data() {
            return {
                exchangeList: [
                    {
                        img: "/76894d35b252344138a2de2a1927d9ca.svg",
                        title: "500 MWFS(ERC-20)",
                        num: 100000000000,
                        info: "1000 TMWFS 兑换 500 MWFS(ERC-20)",
                    },
                    {
                        img: "/76894d35b252344138a2de2a1927d9ca.svg",
                        title: "5000 MWFS(ERC-20)",
                        num: 1000000000000,
                        info: "10000 TMWFS 兑换 5000 MWFS(ERC-20)",
                    }
                ],
                isSSA: false,
                linkedSSAddr: "",
                sharderAccount: "",
                recipient: "",
                exchangeOpen: false,
                exchangeSS: 0,
                convertible:0,
                redeemed:0,
                forgedBalanceNQT:0,
                loadingExchangeSS:false,
                exchangeSSList:[],
                lastExchangeTime:"",
                displayDefault:false

            }
        },
        created() {
            let _this = this;
            _this.getAccount(SSO.accountRS).then(res => {
                _this.forgedBalanceNQT = res.forgedBalanceNQT;

            });
            _this.$http.get('/sharder?requestType=getUserConfig', {
                params: {
                    random: new Date().getTime().toString()
                }
            }).then(res => {
                _this.linkedSSAddr = res.data['sharder.HubBindAddress'];
                let ownerLogin = typeof(SSO.secretPhrase) !== 'undefined' && res.data['sharder.HubBindAddress'] === SSO.accountRS;
                if(!ownerLogin){
                    _this.isSSA = true;
                    return;
                }
                let data = new FormData();
                data.append("ssa", SSO.accountRS);
                _this.$http.post(window.api.sharderExchangeSSA, data).then(res => {
                    _this.isSSA = true;
                    if (res.data.success) {
                        _this.sharderAccount = res.data.data;
                        _this.checkExchangeNum();
                    }
                }).catch(() => {
                    _this.isSSA = true
                });
                _this.$http.post(window.api.sharderExchangeRS).then(res => {
                    if (res.data.success) {
                        _this.recipient = res.data.data;
                    }
                });
            }).catch(err => {
                console.log(err);
            });

        },
        methods: {
            checkExchangeNum(){
                const _this = this;
                _this.loadingExchangeSS = true;
                let forgedBalanceNQT = _this.$global.getSSNumberFormat(_this.forgedBalanceNQT);
                /*let effectiveBalanceNQT = _this.$global.getSSNumberFormat(SSO.accountInfo.effectiveBalanceNQT);*/
                let exchangeSS = _this.exchangeSS;
                let data = new FormData();
               /* effectiveBalanceNQT = Number(effectiveBalanceNQT.substring(0,effectiveBalanceNQT.length-2));*/
                forgedBalanceNQT = Number(forgedBalanceNQT.substring(0,forgedBalanceNQT.length-2));
                data.append("accountRS", SSO.accountRS);
                _this.$http.post(window.api.ssContactAmount, data).then(res => {
                    _this.exchangeSSList = res.data.exchangeSSList;
                    _this.lastExchangeTime = res.data.exchangeSSList[0].createDate;
                    return res.data.totalExchangeAmount ?  res.data.totalExchangeAmount : 0;
                }).then(res=>{
                    exchangeSS = Number(res);
                    _this.redeemed = exchangeSS * 2;
                    _this.convertible = Math.floor(forgedBalanceNQT - exchangeSS * 2);
                    let ConvertibleSS = Math.floor((forgedBalanceNQT - exchangeSS * 2)/1000)*1000;
                    let data1 = {
                        img: "/76894d35b252344138a2de2a1927d9ca.svg",
                        title: ConvertibleSS / 2 + " MWFS(ERC-20)",
                        num: ConvertibleSS * 100000000,
                        info: ConvertibleSS  + " TMWFS 兑换 "+ConvertibleSS / 2+" MWFS(ERC-20)",
                    };


                    if(ConvertibleSS >= 1000){
                        _this.displayDefault = false;
                        if(_this.exchangeList.length === 2){
                            _this.exchangeList.push(data1);
                        }else{
                            _this.exchangeList.splice(2,1);
                            _this.exchangeList.push(data1);
                        }

                    }else{
                        _this.displayDefault = true;
                    }

                });
                _this.loadingExchangeSS = false;

            },
            exchangeFun(e) {
                let _this = this;
                console.info(SSO.accountInfo);
                if (_this.exchangeSS + e.num > _this.forgedBalanceNQT) {
                    return _this.$message.warning(_this.$t("reward.miner_acconut"));
                }
                if (!_this.sharderAccount) {
                    return _this.$message.warning(_this.$t("reward.sharder_binding_acconut"));
                }
                if(_this.convertible < 1000){
                    return _this.$message.warning(_this.$t("reward.exchange_tip"));
                }
                if(new Date() - new Date(_this.lastExchangeTime) < 7*24*3600*1000){
                    return _this.$message.warning(_this.$t("reward.exchange_time_tip"));
                }
                _this.$confirm(e.info, _this.$t("reward.exchange_sharder_account", {account: _this.sharderAccount})).then(() => {
                    _this.sendMoney(e.num)
                });
            },
            sendMoney(num) {
                let _this = this;
                _this.loadingExchangeSS = true;
                _this.$global.fetch("POST", {
                    recipient: _this.recipient,
                    deadline: "1440",
                    phasingHashedSecretAlgorithm: 2,
                    feeNQT: 100000000,
                    amountNQT: num,
                    secretPhrase: SSO.secretPhrase
                }, "sendMoney").then(res => {
                    if (res.broadcasted) {
                        _this.exchangeRS(res.transaction);
                    } else {
                        _this.$message.error(_this.$t("reward.transfer_failed"));
                    }
                });
            },
            exchangeRS(val) {
                let _this = this;
                let data = new FormData();
                data.append("ssa", SSO.accountRS);
                data.append("transaction", val);
                _this.$http.post(window.api.sharderExchange, data).then(res => {
                    if (res.data.status) {
                        _this.$message.success(_this.$t("reward.exchange_success"));
                    } else {
                        _this.$message.error(_this.$t("reward.exchange_error"));
                    }
                    _this.checkExchangeNum();
                    _this.loadingExchangeSS = false;
                });
            },
            getAccount(account) {
                const _this = this;
                return new Promise((resolve, reject) => {
                    _this.$http.get('/sharder?requestType=getAccount', {
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
        }
    }
</script>

<style scoped>
    .exchange .exchange-header h1 {
        font-size: 28px;
        font-weight: bold;
        padding: 20px 0 10px;
    }

    .exchange .exchange-header p {
        padding: 0 0 20px;
    }

    .exchange .exchange-list {
        float: left;
        width: 360px;
        display: inline-block;
        height: 70px;
        border-radius: 4px;
        background: #fff;
        color: #555;
        padding: 13px 10px;
        position: relative;
        margin: 0 60px 20px 0;
    }

    .exchange .exchange-list.right {
        margin: 0 0 20px;
    }

    .exchange .exchange-list .exchange-img {
        position: relative;
        top: 0;
        left: 0;
        width: 20px;
        height: 20px;
    }

    .exchange .exchange-list p {
        font-size: 11px;
        color: #555;
    }

    .exchange .exchange-list button {
        position: absolute;
        top: 16px;
        right: 10px;
        outline: none;
        border: none;
        border-radius: 4px;
        background: #3fb09a;
        height: 40px;
        width: 80px;
        color: #fff;
        font-size: 14px;
        font-weight: bold;
        cursor: pointer;
    }

    .exchange .exchange-list .title {
        display: inline-block;
        font-size: 16px;
        font-weight: bold;
        color: #000;
        position: relative;
        top: -4px;
        padding: 5px 0 5px;
    }

    .exchange .exchange-list.info {
        padding: 24px 0;
        text-align: center;
        margin: 0 0 20px;
    }

    @media (max-width: 640px) {
        .exchange .exchange-list {
            max-width: 100%;
        }
    }
</style>
<style lang="scss" type="text/scss">
    /*@import '~scss_vars';*/
    @import './style.scss';

    .el-table {
        th > .cell {
            background-color: white;
        }

        .cell {
            font-size: 13px;
        }
    }

    #miner_list .modal-body .el-form .el-form-item .el-form-item__label {
        color: #99a9bf !important;
    }

    .testnet-tips {
        padding: 10px 0 20px 0;
        font-size: 13px;
        font-weight: normal;
        text-align: center;
    }


    .last_block {
        text-align: left!important;
        font-size: 12px!important;

        .generator {
            margin-right: 10px;
        }
    }

</style>
