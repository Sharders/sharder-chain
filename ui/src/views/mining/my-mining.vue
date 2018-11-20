<template>
    <div class="my-mining">
        <div class="tabs">
            <p @click="$router.back()" class="mining-back">&lt;&lt;返回矿池</p>
            <el-radio-group v-model="tabPosition" class="title">
                <el-radio-button label="join" class="btn">我加入的</el-radio-button>
                <el-radio-button label="create" class="btn">我创建的</el-radio-button>
            </el-radio-group>
        </div>
        <div v-if="tabPosition === 'join'">
            <div class="mining-list">
                <el-row :gutter="0">
                    <el-col :span="24" v-for="(mining,index) in joinList">
                        <div class="grid-content">
                            <div class="info">
                                <h2>矿池{{mining.serialNumber}}</h2>
                                <p>{{mining.currentInvestment}}/{{mining.investmentTotal}}</p>
                                <el-progress :percentage="(mining.currentInvestment/mining.investmentTotal)*100"
                                             :show-text="false"></el-progress>
                            </div>
                            <div class="tag">
                                <p>
                                    <img src="../../assets/kuangchisouyi.png">
                                    <span>矿池收益 : {{mining.earnings}} SS</span>
                                </p>
                                <p>
                                    <img src="../../assets/kuagnchifhenpei.png">
                                    <span>收益分配 : {{mining.distribution}}%</span>
                                </p>
                                <p>
                                    <img src="../../assets/kuangchishenyu.png">
                                    <span>剩余挖矿 : {{mining.remaining}}块(约13.5h)</span>
                                </p>
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </div>
            <div class="info">没有更多</div>
        </div>
        <div v-if="tabPosition === 'create'">
            <div class="mining-list create">
                <el-row :gutter="0">
                    <el-col :span="24" v-for="(mining,index) in createList">
                        <div class="grid-content">
                            <div class="info">
                                <h2>矿池{{mining.serialNumber}}</h2>
                                <p>{{mining.currentInvestment}}/{{mining.investmentTotal}}</p>
                                <el-progress :percentage="(mining.currentInvestment/mining.investmentTotal)*100"
                                             :show-text="false"></el-progress>
                            </div>
                            <div class="tag">
                                <p>
                                    <img src="../../assets/kuangchisouyi.png">
                                    <span>矿池收益 : {{mining.earnings}} SS</span>
                                </p>
                                <p>
                                    <img src="../../assets/kuagnchifhenpei.png">
                                    <span>收益分配 : {{mining.distribution}}%</span>
                                </p>
                                <p>
                                    <img src="../../assets/kuangchishenyu.png">
                                    <span>剩余挖矿 : {{mining.remaining}}块(约13.5h)</span>
                                </p>
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </div>
            <div class="history" @click="$router.push({name: 'create-history'})">历史创建记录&gt;&gt;</div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "my-mining",
        data() {
            return {
                tabPosition: 'join',
                joinList: [
                    {
                        serialNumber: "001",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "002",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "003",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "004",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "005",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "006",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                ],
                createList: [
                    {
                        serialNumber: "001",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "002",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "003",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "004",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "005",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                    {
                        serialNumber: "006",
                        investmentTotal: 500000,
                        currentInvestment: 180000,
                        earnings: 1000,
                        distribution: 80,
                        remaining: 800,
                    },
                ],
            }
        },
        methods: {}
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
