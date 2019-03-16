<template>
    <div class="my-assets">
        <p @click="$router.back()" class="mining-back">&lt;&lt;{{$t('mining.attribute.return_previous')}}</p>
        <div class="assets">
            <div class="assets-info">
                <div class="totle-assets">
                    <span>{{$t('mining.my_assets.total_asset')}}</span>
                    <span class="strong">{{accountInfo.balanceNQT/100000000}}</span>
                </div>
                <p class="exchang" @click="$router.push({name: 'free-collar-drill'})">
                    {{$t('mining.index.diamond_exchange')}}</p>
                <div class="assets-detail">
                    <div>
                        <p>{{$t('mining.my_assets.available_asset')}}</p>
                        <p class="strong">{{accountInfo.guaranteedBalanceNQT/100000000}}</p>
                    </div>
                    <div>
                        <p>{{$t('mining.my_assets.frozen_assets')}}</p>
                        <p class="strong">{{accountInfo.forgedBalanceNQT/100000000}}</p>
                    </div>
                </div>
            </div>
            <div class="assets-header">{{$t('mining.my_assets.asset_record')}}</div>
            <div class="assets-list" v-for="al in assetsList">
                <div class="title">
                    <p class="strong">{{al.title}}</p>
                    <p>{{al.time}}</p>
                </div>
                <div class="number">{{al.num}} SS</div>
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
                accountInfo: SSO.accountInfo,
                pageNO: 1,
                isPage: true,
                assetsList: []
            }
        },
        methods: {
            getAssetsList() {
                let _this = this;
                _this.$global.fetch("GET", {
                    account: SSO.accountRS,
                    firstIndex: (_this.pageNO - 1) * 10,
                    lastIndex: (_this.pageNO - 1) * 10 + 9
                }, "getBlockchainTransactions").then(res => {
                    if (res.transactions.length === 0) {
                        return _this.isPage = false;
                    }
                    for (let t of res.transactions) {
                        console.info(t);
                        _this.assetsList.push({
                            title: _this.$global.getTransactionTypeStr(t),
                            time: _this.$global.myFormatTime(t.timestamp, 'YMDHMS', true),
                            num: _this.$global.getTransactionAmountNQT(t, _this.accountInfo.accountRS)
                        })
                    }
                });
            },
            loadAssets() {
                this.pageNO++;
                this.getAssetsList();
            }
        },
        created() {
            this.getAssetsList();
        }
    }
</script>

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
