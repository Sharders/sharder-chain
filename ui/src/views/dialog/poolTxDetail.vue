<template>

    <div class="poolTxDetail">
        <div class="template">
            <el-row :gutter="20">
                <el-col :xl="6" :lg="6" :md="6" :sm="24" :xs="24">
                    <el-card shadow="hover" class="content">
                        <!--<p v-if="rowData.subtype === 1 " class="node-type">
                            <strong>{{$t('transaction.transaction_pool_id')}}:</strong>{{ rowData.poolInfo.poolId }}
                            <strong>{{$t('network.block_list_fee')}}:</strong>{{formatAmount(rowData)}}
                        </p>-->

                        <div class="mobile">
                            <p class="node-type">
                                <strong>{{$t('poc.creator')}}: </strong>{{ rowData.senderRS }}
                            </p>
                            <p class="node-type">
                                <strong>{{$t('poc.type')}}: </strong>{{ parseSubTypePool(rowData.subType) }}
                            </p>
                            <p class="node-type">
                                <strong>{{$t('poc.started_height')}}: </strong>{{ rowData.height }}
                            </p>
                            <p class="node-type">
                                <strong>{{$t('poc.started_block')}}: </strong>{{ rowData.block }}
                            </p>
                        </div>

                        <p v-if="rowData.poolInfo.poolId" class="node-type">
                            <strong>{{$t('transaction.transaction_pool_id')}}: </strong>{{ rowData.poolInfo.poolId }}
                        </p>
                        <p v-if="rowData.poolInfo.amount" class="node-type">
                            <strong>{{$t('transaction.transaction_amount')}}: </strong>{{formatAmount(rowData)}}
                        </p>
<!--                        <p v-if="rowData.poolInfo.period" class="node-type">-->
<!--                            <strong>{{$t('mining.attribute.remaining_mining_time')}}:</strong>{{rowData.poolInfo.period}}-->
<!--                        </p>-->
                        <p v-if="rowData.poolInfo.txSId" class="node-type">
                            <strong>txSId:</strong>{{rowData.poolInfo.txSId}}
                        </p>
                        <p v-if="rowData.poolInfo.txId" class="node-type">
                            <strong>{{$t('mining.attribute.tx_id')}}: </strong>{{rowData.poolInfo.txId}}
                        </p>
                        <p v-if="rowData.poolInfo.rule" class="node-type">
                            <strong>{{$t('mining.index.Income_distribution')}}: </strong>{{rowData.poolInfo.rule.forgepool.reward}}
                            <br/>
                            <strong>{{$t('network.block_list_fee')}}: </strong>{{$global.getTransactionFeeNQT(rowData)}}
                        </p>
                    </el-card>
                </el-col>
            </el-row>
        </div>

    </div>

</template>

<script>

    import BigNumber from "bignumber.js";

    export default {

        name: "poolTxDetail",
        props: {
            rowData: {}
        },
        methods:{
           formatAmount(t){
               let subtype = t.subType;
               let amountNQT = t.amountNQT;
               if(subtype === 0||subtype===1){
                   amountNQT = this.poolPledgeAmount;
               }else if(subtype === 2||subtype ===3){
                   amountNQT=t.poolInfo.amount;
               }
               amountNQT =new BigNumber(amountNQT).dividedBy("100000000").toFixed();

               if (subtype===2 || subtype===0) {
                   return "-"+amountNQT + " MW";
               } else if (subtype===1 || subtype===3){
                   return "+" + amountNQT + " MW";
               }
           },
            parseSubTypePool(subtype) {
                switch (subtype) {
                    case 0:
                        return this.$root.$t("transaction.transaction_type_pool_create");
                    case 1:
                        return this.$root.$t("transaction.transaction_type_pool_destroy");
                    case 2:
                        return this.$root.$t("transaction.transaction_type_pool_join");
                    case 3:
                        return this.$root.$t("transaction.transaction_type_pool_quit");
                    default:
                        return this.$root.$t("transaction.transaction_type_forge_pool");
                }
            },

        },

    }
</script>

<style scoped type="text/scss" lang="scss">
@import '../../styles/css/vars.scss';
    .poolTxDetail {
        padding: 10px;
    }

    .poolTxDetail .template p {
        margin-bottom: 5px;
    }

    .poolTxDetail .template p strong {
        width: 400px;
        font-size: 14px;
        font-weight: bold;
        color: #000;
    }

    .poolTxDetail .template p span {
        display: inline-block;
        width: 232px;
    }

    .poolTxDetail .template .content {
        box-shadow: 1px 1px 10px $primary_color;
        border-radius: 4px;
    }
</style>

