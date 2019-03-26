<template>
    <div class="pocContent">
        <div class="template">
            <p v-if="pocInfo.weightMap">
                <strong>{{$t('poc.weight')}}</strong>
                <span>{{$t('poc.node_type')}}: {{pocInfo.weightMap.node}}%</span>
                <span>{{$t('poc.weight_role')}}: {{pocInfo.weightMap.serverOpen}}%</span>
                <span>{{$t('poc.weight_ss_held')}}: {{pocInfo.weightMap.ssHold}}%</span>
                <span>{{$t('poc.weight_hardware_config')}}: {{pocInfo.weightMap.hardwareConfig}}%</span>
                <span>{{$t('poc.weight_network_config')}}: {{pocInfo.weightMap.networkConfig}}%</span>
                <span>{{$t('poc.weight_tx_performance')}}:{{pocInfo.weightMap.txPerformance}}%</span>
            </p>
            <p v-if="pocInfo.nodeTypeTemplate">
                <strong>{{$t('poc.node_type_score')}}</strong>
                <span>{{$t('poc.sharder_node')}}: {{pocInfo.nodeTypeTemplate["1"]}}</span>
                <span>{{$t('poc.community_node')}}: {{pocInfo.nodeTypeTemplate["2"]}}</span>
                <span>{{$t('poc.hub_node')}}: {{pocInfo.nodeTypeTemplate["4"]}}</span>
                <span>{{$t('poc.box_node')}}: {{pocInfo.nodeTypeTemplate["5"]}}</span>
                <span>{{$t('poc.normal_node')}}: {{pocInfo.nodeTypeTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.serverOpenTemplate">
                <strong>{{$t('poc.role_score')}}</strong>
                <span>{{$t('poc.role_miner')}}: {{pocInfo.serverOpenTemplate["32"]}}</span>
                <span>{{$t('poc.role_watcher')}}: {{pocInfo.serverOpenTemplate["64"]}}</span>
                <span>{{$t('poc.role_traversal')}}: {{pocInfo.serverOpenTemplate["128"]}}</span>
                <span>{{$t('poc.role_storer')}}: {{pocInfo.serverOpenTemplate["256"]}}</span>
                <span>{{$t('poc.role_prover')}}: {{pocInfo.serverOpenTemplate["512"]}}</span>
            </p>
            <p v-if="pocInfo.txPerformanceTemplate">
                <strong>{{$t('poc.tx_performance_score')}}</strong>
                <span>{{$t('poc.item_low')}}: {{pocInfo.txPerformanceTemplate["1"]}}</span>
                <span>{{$t('poc.item_med')}}: {{pocInfo.txPerformanceTemplate["2"]}}</span>
                <span>{{$t('poc.item_high')}}: {{pocInfo.txPerformanceTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.hardwareConfigTemplate">
                <strong>{{$t('poc.hardware_score')}}</strong>
                <span>{{$t('poc.item_low')}}: {{pocInfo.hardwareConfigTemplate["1"]}}</span>
                <span>{{$t('poc.item_med')}}: {{pocInfo.hardwareConfigTemplate["2"]}}</span>
                <span>{{$t('poc.item_high')}}: {{pocInfo.hardwareConfigTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.networkConfigTemplate">
                <strong>{{$t('poc.network_score')}}</strong>
                <span>{{$t('poc.item_very_low')}}: {{pocInfo.networkConfigTemplate["0"]}}</span>
                <span>{{$t('poc.item_low')}}: {{pocInfo.networkConfigTemplate["1"]}}</span>
                <span>{{$t('poc.item_med')}}: {{pocInfo.networkConfigTemplate["2"]}}</span>
                <span>{{$t('poc.item_high')}}: {{pocInfo.networkConfigTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.generationMissingTemplate">
                <strong>{{$t('poc.amount_of_loss_blocks')}}</strong>
                <span>{{$t('poc.item_low')}}: {{pocInfo.generationMissingTemplate["1"]}}</span>
                <span>{{$t('poc.item_med')}}: {{pocInfo.generationMissingTemplate["2"]}}</span>
                <span>{{$t('poc.item_high')}}: {{pocInfo.generationMissingTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.bocSpeedTemplate">
                <strong>{{$t('poc.fork_convergence_speed')}}</strong>
                <span>{{$t('poc.hard_fork')}}: {{pocInfo.bocSpeedTemplate["1"]}}</span>
                <span>{{$t('poc.item_slow')}}: {{pocInfo.bocSpeedTemplate["2"]}}</span>
                <span>{{$t('poc.item_med')}}: {{pocInfo.bocSpeedTemplate["0"]}}</span>
            </p>
        </div>
        <div class="online-rate" v-if="pocInfo.onlineRateTemplate">
            <div>
                <h3>{{$t('poc.sharder_node')}}</h3>
                <p>99.00%&lt;{{$t('poc.uptime')}}&lt;99.99%<span>{{pocInfo.onlineRateTemplate.FOUNDATION["0"]}}</span></p>
                <p>97.00%&lt;{{$t('poc.uptime')}}&lt;99.00%<span>{{pocInfo.onlineRateTemplate.FOUNDATION["1"]}}</span></p>
                <p>{{$t('poc.uptime')}}&lt;97.00% <span>{{pocInfo.onlineRateTemplate.FOUNDATION["3"]}}</span></p>
            </div>
            <div>
                <h3>{{$t('poc.community_node')}}</h3>
                <p>97.00%&lt;{{$t('poc.uptime')}}&lt;99.00%<span>{{pocInfo.onlineRateTemplate.COMMUNITY["1"]}}</span></p>
                <p>90.00%&lt;{{$t('poc.uptime')}}&lt;97.00%<span>{{pocInfo.onlineRateTemplate.COMMUNITY["2"]}}</span></p>
                <p>{{$t('poc.uptime')}}&lt;90.00%<span>{{pocInfo.onlineRateTemplate.COMMUNITY["4"]}}</span></p>
            </div>
            <div>
                <h3>{{$t('poc.hub_node')}}</h3>
                <p>{{$t('poc.uptime')}}&lt;99.00%<span>{{pocInfo.onlineRateTemplate.HUB["5"]}}</span></p>
                <p>{{$t('poc.uptime')}}&lt;97.00%<span>{{pocInfo.onlineRateTemplate.HUB["6"]}}</span></p>
                <p>{{$t('poc.uptime')}}&lt;90.00%<span>{{pocInfo.onlineRateTemplate.HUB["4"]}}</span></p>
            </div>
            <div>
                <h3>{{$t('poc.box_node')}}</h3>
                <p>{{$t('poc.uptime')}}&lt;99.00%<span>{{pocInfo.onlineRateTemplate.BOX["5"]}}</span></p>
                <p>{{$t('poc.uptime')}}&lt;97.00%<span>{{pocInfo.onlineRateTemplate.BOX["6"]}}</span></p>
                <p>{{$t('poc.uptime')}}&lt;90.00%<span>{{pocInfo.onlineRateTemplate.BOX["4"]}}</span></p>
            </div>
            <div>
                <h3>{{$t('poc.normal_node')}}</h3>
                <p>{{$t('poc.uptime')}}&lt;97.00%<span>{{pocInfo.onlineRateTemplate.NORMAL["6"]}}</span></p>
                <p>{{$t('poc.uptime')}}&lt;90.00%<span>{{pocInfo.onlineRateTemplate.NORMAL["7"]}}</span></p>
            </div>
        </div>

        <p class="template-version">{{$t('poc.template_version')}} {{pocInfo.templateVersion}}</p>
        <p class="testnet-tips">{{$t('poc.block_reward_tips_phase1')}}</p>
    </div>
</template>

<script>
    export default {
        name: "pocContent",
        props: {
            pocInfo: {}
        }
    }
</script>

<style scoped>
    .pocContent .template p {
        border-bottom: 1px solid #493eda;
        margin-bottom: 4px;
        padding-bottom: 4px;
    }

    .pocContent .template p strong {
        display: inline-block;
        width: 200px;
        font-size: 14px;
        font-weight: bold;
        color: #000;
    }

    .pocContent .template p span {
        display: inline-block;
        width: 116px;
    }

    .pocContent .online-rate {
        display: flex;
        justify-content: space-between;
        margin-top: 16px;
    }

    .pocContent .online-rate > div {
        box-shadow: 1px 1px 10px #493eda;
        padding: 10px;
        border-radius: 4px;
    }

    .online-rate > div > h3 {
        font-size: 15px;
        font-weight: bold;
        color: #000;
    }

    .online-rate > div > P {
        padding: 8px 0 0 0;
        font-size: 14px;
    }

    .online-rate > div > P span {
        float: right;
        color: red;
        font-weight: bold;
        padding-left: 10px;
    }

    .pocContent .template-version {
        padding: 16px 0 0;
        font-size: 13px;
        font-weight: normal;
        text-align: right;
        font-style: italic;
    }
    
    .pocContent .testnet-tips {
        padding: 16px 0 0;
        font-size: 13px;
        font-weight: normal;
        text-align: center;
    }
</style>
