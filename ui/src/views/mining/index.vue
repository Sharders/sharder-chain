<template>
    <div class="mining">
        <!--切换按钮-->
        <el-radio-group v-model="tabTitle" class="title">
            <el-radio-button label="miner" class="btn">豆匣矿场</el-radio-button>
            <el-radio-button label="welfare" class="btn">免费领SS</el-radio-button>
            <el-radio-button label="exchange" class="btn">SS兑换</el-radio-button>
        </el-radio-group>
        <!--豆匣矿场-->
        <div v-if="tabTitle === 'miner'">
            <div class="mining-content">
                <img src="../../assets/shouyi.png" id="shouyi">
                <div class="assets">
                    <ul>
                        <li>全网挖矿: 第236块</li>
                        <li>我的资产: 100000 SS</li>
                        <li>我的收益: 100000 SS</li>
                        <li class="strong">
                            <img src="../../assets/kuangchii_chakan.png">
                            <span>查看排行</span>
                        </li>
                    </ul>
                </div>
                <div class="state">
                    <div class="state-info">
                        <p>挖矿中</p>
                        <p>全网收益 1000 SS</p>
                    </div>
                </div>
                <div class="instructions">豆匣矿场说明</div>
                <div class="create">334
                    <img src="../../assets/chuanjiankuangchi.png">
                    <p>创建矿池</p>
                </div>
            </div>
            <div class="mining-notice">
                <img src="../../assets/logo.svg" class="notice-img">
                <span class="notice-info">
                    矿产第2345块 | 出块者023 | 奖励: 1000 SS
                </span>
            </div>
            <div class="mining-list">
                <h5>
                    <div class="list-title">
                        <img src="../../assets/miner.svg" class="mining-list-img">
                        <span>矿池列表</span>
                    </div>
                    <el-select v-model="value" placeholder="排序">
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
                                <div class="info">
                                    <h2>矿池{{index}}</h2>
                                    <p>180000/500000</p>
                                    <el-progress :percentage="(180000/500000)*100" :show-text="false"></el-progress>
                                </div>
                                <div class="tag">
                                    <p>
                                        <img src="../../assets/kuangchisouyi.png">
                                        <span>矿池收益 : 1000 SS</span>
                                    </p>
                                    <p>
                                        <img src="../../assets/kuagnchifhenpei.png">
                                        <span>收益分配 : 80%</span>
                                    </p>
                                    <p>
                                        <img src="../../assets/kuangchishenyu.png">
                                        <span>剩余挖矿 : 800块(约13.5h)</span>
                                    </p>
                                </div>
                            </div>
                        </el-col>
                    </el-row>
                </div>
            </div>
            <div class="mining-paging">
                <el-pagination
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    :page-size="10"
                    layout="total, prev, pager, next ,jumper"
                    :total="1000">
                </el-pagination>
            </div>
        </div>
        <!--免费领SS-->
        <div v-if="tabTitle === 'welfare'">
            <div class="receive">
                <img src="../../assets/logo.svg" class="receive-qr-img">
                <p class="receive-text">
                    请扫描二维码下载0X钱包,<br>
                    进入"豆匣矿场"应用免费领取
                </p>
            </div>
        </div>
        <!--SS兑换-->
        <div v-if="tabTitle === 'exchange'">
            <div class="reward">
                <div class="reward-title">
                    目前豆匣网络为开放测试网络,内部流通的SS为测试的SS,为了回馈社区用户参与测试网络及使用,开放SS兑换功能.数量有限先到先得.
                </div>
                <div class="reward-content">
                    <el-row :gutter="20">
                        <el-col :span="12" v-for="(reward,index) in rewardList">
                            <div class="reward-content-div">
                                <div class="content-left">
                                    <p>
                                        <img src="../../assets/logo.svg" class="content-left-img">
                                        <span class="strong">1000 SS(ERC-20)</span>
                                        <span>剩余: 0</span>
                                    </p>
                                    <p class="reward-instructions">说明:兑换成功后请联系官方管理员领取</p>
                                </div>
                                <div class="content-right">
                                    <el-button type="info">暂未开放</el-button>
                                </div>
                            </div>
                        </el-col>
                    </el-row>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'mining',
        data() {
            return {
                tabTitle: 'miner',
                options: [{
                    value: 'default',
                    label: '默认排序'
                }, {
                    value: 'capacity',
                    label: '矿池容量'
                }, {
                    value: 'distribution',
                    label: '奖励分配'
                }, {
                    value: 'time',
                    label: '剩余时间'
                }],
                value: '',
                miningList: ['1', '2', '3', '4', '5', '6'],
                rewardList: ['1', '2', '3', '4'],
            }
        },
        methods: {
            handleSizeChange(val) {
                console.log(`每页 ${val} 条`);
            },
            handleCurrentChange(val) {
                console.log(`当前页: ${val}`);
            }
        },
        mounted: function () {
            let _this = this;
        },
        created: function () {

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

</style>
<!--豆匣矿场-->
<style scoped>
    .mining-content {
        position: relative;
        margin-top: 10px;
        border-top-left-radius: 6px;
        border-top-right-radius: 6px;
        height: 300px;
        background-color: #513acB;
        padding: 30px;
        background-image: url("../../assets/kuangchi_bg.png");
        background-repeat: no-repeat;
        background-position: center 140px;
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

    .state .state-info {
        width: 140px;
        height: 50px;
        text-align: center;
        margin: auto;
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
        top: -20px;
        background-color: #0000ff;
        display: inline-block;
        font-size: 14px;
        color: #fff;
        padding: 10px 15px;
        border-bottom-left-radius: 20px;
        border-top-left-radius: 20px;
    }

    .create {
        position: relative;
        text-align: center;
        color: #fff;
        top: 130px;
        float: right;
        right: -130px;
        cursor: pointer;
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
        height:14px;
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

    @keyframes shouyi {
        0% {
            top: 110px;
        }
        100% {
            top: 90px;
        }
    }

    #shouyi {
        position: absolute;
        top: 110px;
        left: calc(50% - 34px);
        animation: shouyi 1s infinite;
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
        background-color: #fff;
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

</style>
<!--移动端兼容-->
<style>
    @media (min-width: 640px) {

    }
</style>
