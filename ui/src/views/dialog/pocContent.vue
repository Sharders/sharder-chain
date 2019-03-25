<template>
    <div class="pocContent">
        <div class="template">
            <p v-if="pocInfo.weightMap">
                <strong>{{$t('poc.weight')}}</strong>
                <span>{{$t('poc.nodeType')}}:{{pocInfo.weightMap.node}}</span>
                <span>开启服务:{{pocInfo.weightMap.serverOpen}}</span>
                <span>SS持有量:{{pocInfo.weightMap.ssHold}}</span>
                <span>硬件配置:{{pocInfo.weightMap.hardwareConfig}}</span>
                <span>硬件配置:{{pocInfo.weightMap.hardwareConfig}}</span>
                <span>网络配置:{{pocInfo.weightMap.networkConfig}}</span>
                <span>交易处理性能:{{pocInfo.weightMap.txPerformance}}</span>
            </p>
            <p v-if="pocInfo.nodeTypeTemplate">
                <strong>节点类型分数</strong>
                <span>基金会节点:{{pocInfo.nodeTypeTemplate["1"]}}</span>
                <span>社区认证节点:{{pocInfo.nodeTypeTemplate["2"]}}</span>
                <span>HUB节点:{{pocInfo.nodeTypeTemplate["4"]}}</span>
                <span>BOX节点:{{pocInfo.nodeTypeTemplate["5"]}}</span>
                <span>普通节点:{{pocInfo.nodeTypeTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.serverOpenTemplate">
                <strong>开启服务分数</strong>
                <span>矿工:{{pocInfo.serverOpenTemplate["32"]}}</span>
                <span>观察者:{{pocInfo.serverOpenTemplate["64"]}}</span>
                <span>穿透者:{{pocInfo.serverOpenTemplate["128"]}}</span>
                <span>存储者:{{pocInfo.serverOpenTemplate["256"]}}</span>
                <span>证明者:{{pocInfo.serverOpenTemplate["512"]}}</span>
            </p>
            <p v-if="pocInfo.txPerformanceTemplate">
                <strong>交易处理性能分数</strong>
                <span>低:{{pocInfo.txPerformanceTemplate["1"]}}</span>
                <span>中:{{pocInfo.txPerformanceTemplate["2"]}}</span>
                <span>高:{{pocInfo.txPerformanceTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.hardwareConfigTemplate">
                <strong>硬件分数</strong>
                <span>低:{{pocInfo.hardwareConfigTemplate["1"]}}</span>
                <span>中:{{pocInfo.hardwareConfigTemplate["2"]}}</span>
                <span>高:{{pocInfo.hardwareConfigTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.networkConfigTemplate">
                <strong>网络分数</strong>
                <span>极差:{{pocInfo.networkConfigTemplate["0"]}}</span>
                <span>低:{{pocInfo.networkConfigTemplate["1"]}}</span>
                <span>中:{{pocInfo.networkConfigTemplate["2"]}}</span>
                <span>高:{{pocInfo.networkConfigTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.generationMissingTemplate">
                <strong>出块错过数惩罚表(扣分)</strong>
                <span>低:{{pocInfo.generationMissingTemplate["1"]}}</span>
                <span>中:{{pocInfo.generationMissingTemplate["2"]}}</span>
                <span>高:{{pocInfo.generationMissingTemplate["3"]}}</span>
            </p>
            <p v-if="pocInfo.bocSpeedTemplate">
                <strong>分叉收敛惩罚表(扣分)</strong>
                <span>硬分叉:{{pocInfo.bocSpeedTemplate["1"]}}</span>
                <span>慢:{{pocInfo.bocSpeedTemplate["2"]}}</span>
                <span>中:{{pocInfo.bocSpeedTemplate["0"]}}</span>
            </p>
        </div>
        <div class="online-rate" v-if="pocInfo.onlineRateTemplate">
            <div>
                <h3>基金会节点</h3>
                <p>99.00%&lt;在线率&lt;99.99%<span>{{pocInfo.onlineRateTemplate.FOUNDATION["0"]}}</span></p>
                <p>97.00%&lt;在线率&lt;99.00%<span>{{pocInfo.onlineRateTemplate.FOUNDATION["1"]}}</span></p>
                <p>在线率&lt;97.00% <span>{{pocInfo.onlineRateTemplate.FOUNDATION["3"]}}</span></p>
            </div>
            <div>
                <h3>社区认证节点</h3>
                <p>97.00%&lt;在线率&lt;99.00%<span>{{pocInfo.onlineRateTemplate.COMMUNITY["1"]}}</span></p>
                <p>90.00%&lt;在线率&lt;97.00%<span>{{pocInfo.onlineRateTemplate.COMMUNITY["2"]}}</span></p>
                <p>在线率&lt;90.00%<span>{{pocInfo.onlineRateTemplate.COMMUNITY["4"]}}</span></p>
            </div>
            <div>
                <h3>Hub节点</h3>
                <p>在线率&lt;99.00%<span>{{pocInfo.onlineRateTemplate.HUB["5"]}}</span></p>
                <p>在线率&lt;97.00%<span>{{pocInfo.onlineRateTemplate.HUB["6"]}}</span></p>
                <p>在线率&lt;90.00%<span>{{pocInfo.onlineRateTemplate.HUB["4"]}}</span></p>
            </div>
            <div>
                <h3>Box节点</h3>
                <p>在线率&lt;99.00%<span>{{pocInfo.onlineRateTemplate.BOX["5"]}}</span></p>
                <p>在线率&lt;97.00%<span>{{pocInfo.onlineRateTemplate.BOX["6"]}}</span></p>
                <p>在线率&lt;90.00%<span>{{pocInfo.onlineRateTemplate.BOX["4"]}}</span></p>
            </div>
            <div>
                <h3>普通节点</h3>
                <p>在线率&lt;97.00%<span>{{pocInfo.onlineRateTemplate.NORMAL["6"]}}</span></p>
                <p>在线率&lt;90.00%<span>{{pocInfo.onlineRateTemplate.NORMAL["7"]}}</span></p>
            </div>
        </div>

        <p class="template-version">版本号: {{pocInfo.templateVersion}}</p>
        <p class="testnet-tips">{{$t('poc.reward')}}</p>
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
        font-size: 15px;
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
        font-size: 16px;
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
        font-size: 14px;
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
