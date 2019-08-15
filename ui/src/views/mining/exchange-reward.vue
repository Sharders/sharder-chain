<template>
    <div class="exchange" >
        <div class="exchange-header">
            <h1>{{$t('mining.diamond_exchange.diamond_exchange_title')}}</h1>
            <p v-show="isSSA">
                <span >
                    <li v-if="sharderAccount">{{$t("reward.binding_account")}} : {{sharderAccount}}</li>
                    <li v-if="sharderAccount">{{$t("reward.convertible")}} : {{convertible}} TSS</li>
                    <li v-if="sharderAccount">{{$t("reward.redeemed")}} : {{redeemed}} TSS</li>
                    <li v-if="sharderAccount" style="color: #e64242">{{$t('mining.diamond_exchange.description')}}{{$t("reward.exchangeTip")}} </li>
                    <li v-else>
                        {{$t("reward.no_binding_account")}} ?
                        <a href="https://sharder.org">{{$t("reward.immediately_binding")}}</a>
                    </li>
                </span>
            </p>
            <p>{{$t('mining.diamond_exchange.diamond_exchange_subtitle')}}</p>
        </div>
        <div class="exchange-list" :class="(index+1)%3 === 0 ? ' right' :''" v-for="(exchange,index) in exchangeList" v-loading="loadingExchangeSS">
            <p>
                <img :src="exchange.img" class="exchange-img">
                <span class="title">{{exchange.title}}</span>
            </p>
            <p>{{$t('mining.diamond_exchange.description')}}{{exchange.info}}</p>
            <button @click="exchangeFun(exchange)">{{$t("reward.exchange")}}</button>
        </div>
        <div class="exchange-list info" v-loading="loadingExchangeSS">
            {{$t('mining.diamond_exchange.not_open_tip')}}
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
                        title: "500 SS(ERC-20)",
                        num: 100000000000,
                        info: "1000 TSS 兑换 500 SS(ERC-20)",
                    },
                    {
                        img: "/76894d35b252344138a2de2a1927d9ca.svg",
                        title: "5000 SS(ERC-20)",
                        num: 1000000000000,
                        info: "10000 TSS 兑换 5000 SS(ERC-20)",
                    }
                ],
                isSSA: false,
                linkedSSAddr: this.$store.state.userConfig['sharder.HubBindAddress'],
                sharderAccount: '',
                recipient: "",
                exchangeSS: 0,
                convertible:0,
                redeemed:0,
                loadingExchangeSS:false,

            }
        },
        created() {
            let _this = this;
            let data = new FormData();
            let ownerLogin = SSO.secretPhrase && _this.linkedSSAddr === SSO.accountRS;
            if(!ownerLogin){
                _this.isSSA = true;
                return;
            }
            data.append("ssa", SSO.accountRS);
            _this.$http.post(window.api.sharderExchangeSSA, data).then(res => {
                _this.isSSA = true;
                if (res.data.success) {
                    _this.sharderAccount = res.data.data;
                    _this.checkExchangeNum();
                    //_this.getSSAmount(_this.sharderAccount);
                }
            }).catch(() => {
                _this.isSSA = true
            });
            _this.$http.post(window.api.sharderExchangeRS).then(res => {
                if (res.data.success) {
                    _this.recipient = res.data.data;
                }
            });

        },
        methods: {
            checkExchangeNum(){
                const _this = this;
                _this.loadingExchangeSS = true;
                let forgedBalanceNQT = _this.$global.getSSNumberFormat(SSO.accountInfo.forgedBalanceNQT);
                let effectiveBalanceNQT = _this.$global.getSSNumberFormat(SSO.accountInfo.effectiveBalanceNQT);

                let exchangeSS = _this.exchangeSS;
                let data = new FormData();
                effectiveBalanceNQT = Number(effectiveBalanceNQT.substring(0,effectiveBalanceNQT.length-2));
                forgedBalanceNQT = Number(forgedBalanceNQT.substring(0,forgedBalanceNQT.length-2));
                data.append("accountRS", SSO.accountRS);
                _this.$http.post(window.api.ssContactAmount, data).then(res => {
                    return res.data.totalExchangeAmount ?  res.data.totalExchangeAmount : 0;
                }).then(res=>{
                    exchangeSS = Number(res);
                    _this.redeemed = exchangeSS * 2;
                    let ConvertibleSS = 0;
                    if(effectiveBalanceNQT <= forgedBalanceNQT - exchangeSS){
                        _this.convertible = Math.floor(effectiveBalanceNQT  - exchangeSS);
                        ConvertibleSS = Math.floor((effectiveBalanceNQT  - exchangeSS)/1000)*1000;
                    }else{
                        _this.convertible = Math.floor(forgedBalanceNQT  - exchangeSS);
                        ConvertibleSS = Math.floor((forgedBalanceNQT  - exchangeSS)/1000)*1000;
                    }
                    let data1 = {
                        img: "/76894d35b252344138a2de2a1927d9ca.svg",
                        title: ConvertibleSS / 2 + " SS(ERC-20)",
                        num: ConvertibleSS * 10000000,
                        info: ConvertibleSS  + " TSS 兑换 "+ConvertibleSS / 2+" SS(ERC-20)",
                    };

                    if(ConvertibleSS >= 1000){
                        if(_this.exchangeList.length === 2){
                            _this.exchangeList.push(data1);
                        }else{
                            _this.exchangeList.splice(2,1);
                            _this.exchangeList.push(data1);
                        }

                    }
                    _this.loadingExchangeSS = false;
                });

            },
            exchangeFun(e) {
                let _this = this;
                console.info(SSO.accountInfo);
                if (_this.exchangeSS + e.num > SSO.accountInfo.forgedBalanceNQT) {
                    return _this.$message.warning(_this.$t("reward.miner_acconut"));
                }
                if (!_this.sharderAccount) {
                    return _this.$message.warning(_this.$t("reward.sharder_binding_acconut"));
                }
                if(_this.convertible < 1000){
                    return _this.$message.warning(_this.$t("reward.exchangeTip"));
                }
                _this.$confirm(e.info, _this.$t("reward.exchange_sharder_account", {account: _this.sharderAccount})).then(() => {
                    _this.sendMoney(e.num)
                });
            },
            sendMoney(num) {
                let _this = this;
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
        color: #333;
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
        color: #333;
    }

    .exchange .exchange-list button {
        position: absolute;
        top: 16px;
        right: 10px;
        outline: none;
        border: none;
        border-radius: 4px;
        background: #493eda;
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
