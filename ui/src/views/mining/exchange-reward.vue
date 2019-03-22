<template>
    <div class="exchange">
        <div class="exchange-header">
            <h1>{{$t('mining.diamond_exchange.diamond_exchange_title')}}</h1>
            <p v-show="isSSA">
                <span v-if="sharderAccount">{{$t("reward.binding_account")}} : {{sharderAccount}}</span>
                <span v-else>
                    {{$t("reward.no_binding_account")}} ?
                    <a href="https://sharder.org">{{$t("reward.immediately_binding")}}</a>
                </span>
            </p>
            <p>{{$t('mining.diamond_exchange.diamond_exchange_subtitle')}}</p>
        </div>
        <div class="exchange-list" :class="(index+1)%3 === 0 ? ' right' :''" v-for="(exchange,index) in exchangeList">
            <p>
                <img :src="exchange.img" class="exchange-img">
                <span class="title">{{exchange.title}}</span>
            </p>
            <p>{{$t('mining.diamond_exchange.description')}}{{exchange.info}}</p>
            <button @click="exchangeFun(exchange)">{{$t("reward.exchange")}}</button>
        </div>
        <div class="exchange-list info">
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
                        title: "1000 SS(ERC-20)",
                        num: 1000000000000,
                        info: "兑换成功后请到豆匣官网提取",
                    },
                    {
                        img: "/76894d35b252344138a2de2a1927d9ca.svg",
                        title: "2000 SS(ERC-20)",
                        num: 2000000000000,
                        info: "兑换成功后请到豆匣官网提取",
                    },
                    {
                        img: "/76894d35b252344138a2de2a1927d9ca.svg",
                        title: "3000 SS(ERC-20)",
                        num: 3000000000000,
                        info: "兑换成功后请到豆匣官网提取",
                    },
                    {
                        img: "/76894d35b252344138a2de2a1927d9ca.svg",
                        title: "4000 SS(ERC-20)",
                        num: 4000000000000,
                        info: "兑换成功后请到豆匣官网提取",
                    }
                ],
                isSSA: false,
                sharderAccount: '',
                recipient: "",
            }
        },
        created() {
            let _this = this;
            let data = new FormData();
            data.append("ssa", SSO.accountRS);
            _this.$http.post(window.api.sharderExchangeSSA, data).then(res => {
                _this.isSSA = true;
                if (res.data.success) {
                    _this.sharderAccount = res.data.data;
                }
            });
            _this.$http.post(window.api.sharderExchangeRS).then(res => {
                if (res.data.success) {
                    _this.recipient = res.data.data;
                }
            });
        },
        methods: {
            exchangeFun(e) {
                let _this = this;
                if (!_this.sharderAccount) {
                    return _this.$message.warning(_this.$t("reward.sharder_binding_acconut"));
                }
                _this.$confirm(e.info,_this.$t("reward.exchange_sharder_account",{account:_this.sharderAccount})).then(() => {
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
                });
            }
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
