<template>
    <div class="mining">
        <!--切换按钮-->
        <el-radio-group v-model="tabTitle" class="title">
            <el-radio-button label="mining" class="btn">豆匣矿场</el-radio-button>
            <el-radio-button label="welfare" class="btn">免费领SS</el-radio-button>
            <el-radio-button label="exchange" class="btn">SS兑换</el-radio-button>
        </el-radio-group>
        <!--豆匣矿场-->
        <div v-if="tabTitle === 'mining' && tabMenu === 'mining'">
            <div class="mining-content">
                <img src="../../assets/chatu.png" id="chatu">
                <div class="assets">
                    <ul>
                        <li>全网挖矿: 第236块</li>
                        <li>我的资产: 100000 SS</li>
                        <li>我的收益: 100000 SS</li>
                        <li class="strong">
                            <img src="../../assets/kuangchii_chakan.png">
                            <span @click="isVisible('isRanking')">查看排行</span>
                        </li>
                    </ul>
                </div>
                <div class="state">
                    <div class="state-info">
                        <p>挖矿中</p>
                        <p>全网收益 1000 SS</p>
                    </div>
                </div>
                <div class="instructions" @click="">豆匣矿场说明</div>
                <div class="invite-friends" @click="$router.push({name: 'invite-friends'})">邀请好友得奖励</div>
                <div class="rule-description" @click="$router.push({name: 'rule-description'})">规则说明</div>
                <div class="my-mining create" @click="$router.push({name: 'my-mining'})">
                    <img src="../../assets/wodekuangchi.png">
                    <p>我的矿池</p>
                </div>
                <div class="create" @click="isVisible('isCreatePool')">
                    <img src="../../assets/chuanjiankuangchi.png">
                    <p>创建矿池</p>
                </div>
            </div>
            <div class="mining-notice">
                <img src="../../assets/guangbo.png" class="notice-img">
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
                                <div class="info" @click="poolAttribute(mining)">
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
                        <el-col :span="12" v-for="reward in rewardList">
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
        <!--个人中心-->
        <div v-if="tabMenu === 'personal'">
            <div class="personal-content">
                <div class="user">
                    <img src="../../assets/wodezichan.png" class="header-img">
                    <p>
                        <span>矿工名称</span>:
                        <span>未设置</span>
                        <img src="../../assets/set.png" @click="isVisible('isSetName')">
                    </p>
                    <p>
                        <span>TSS地址</span>:
                        <span>SSA-WHXU-6UB3-DB75-78GAT</span>
                        <img src="../../assets/TSS.png" @click="isVisible('isTSS')">
                    </p>
                </div>
                <div class="list" @click="$router.push({name: 'my-assets'})">
                    <img src="../../assets/wodezichan.png">
                    <span>我的资产</span>
                </div>
                <div class="list" @click="$router.push({name: 'free-collar-drill'})">
                    <img src="../../assets/zhuanshi.png">
                    <span>免费领钻</span>
                </div>
                <div class="list" @click="$router.push({name: 'invite-friends'})">
                    <img src="../../assets/haoyou.png">
                    <span>邀请好友</span>
                </div>
                <div class="list" @click="$router.push({name: 'diamond-exchange'})">
                    <img src="../../assets/zhuanshiduihuan.png">
                    <span>钻石兑换</span>
                </div>
                <div class="list">
                    <img src="../../assets/guanyuwomen.png">
                    <span>关于我们</span>
                </div>
                <div class="about">
                    <p>关注我们</p>
                    <p>www.oxwallet.org</p>
                </div>
            </div>
        </div>
        <!--名称设置-->
        <div v-if="isSetName">
            <div class="set-name">
                <span class="img-close" @click="isVisible('isSetName')"></span>
                <h1>名称设置</h1>
                <div class="input">
                    <el-input v-model="setname" placeholder="请输入名称"></el-input>
                </div>
                <div class="determine">确定</div>
            </div>
        </div>
        <!--TSS说明-->
        <div v-if="isTSS">
            <div class="tss">
                <h1>TSS地址说明</h1>
                <div class="text">
                    TSS地址为豆匣测试网络的账户地址,共识矿场是基于豆匣链开发的DAPP,此地址同时也是你共识矿场应用内的账户.
                    TSS不具备价格流通.目前只能作为测试及与矿场砖石产生兑换关系所用,与钻石为一一对应关系.
                </div>
                <div class="close" @click="isVisible('isTSS')">关闭</div>
            </div>
        </div>
        <!--挖矿排行-->
        <div v-if="isRanking">
            <div class="ranking">
                <span class="img-close" @click="isVisible('isRanking')"></span>
                <div class="ranking-content">
                    <h3 class="ranking-title">挖矿排行</h3>
                    <table class="ranking-table">
                        <tr>
                            <th>排名</th>
                            <th>账户</th>
                            <th>SS数量</th>
                        </tr>
                        <tr v-for="(ranking,index) in rankingList">
                            <td>
                                <span v-if="index <= 2" :class="'ranking-logo bg-'+ index"></span>
                                <span v-if="index > 2">0{{index}}</span>
                            </td>
                            <td>
                                {{ranking.account}}
                            </td>
                            <td>
                                {{ranking.assets}}
                            </td>
                        </tr>
                    </table>
                    <div class="my-assets">
                        我的资产 : 100000 SS | 排名 : 98 名
                    </div>
                </div>
            </div>
        </div>
        <!--创建矿池-->
        <div v-if="isCreatePool">
            <div class="create-pool">
                <span class="img-close" @click="isVisible('isCreatePool')"></span>
                <div class="create-pool-content">
                    <h3 class="pool-header">创建矿池</h3>
                    <div class="pool-attribute">
                        <h1 class="pool-title">
                            <img src="../../assets/kuangchi_attribute.png">
                            <span>矿池属性</span>
                        </h1>
                        <p>
                            <span class="strong">矿池数量</span>:
                            <span>21/51</span>
                        </p>
                        <p>
                            <span class="strong">当前账户</span>:
                            <span>{{accountInfo.accountRS}}</span>
                        </p>
                        <p>
                            <span class="strong">矿池容量</span>:
                            <span>1000000 SS</span>
                        </p>
                        <p>
                            <span class="strong">挖矿时长</span>:
                            <span>2880块(约24h)</span>
                        </p>
                    </div>
                    <div class="pool-set">
                        <h1 class="pool-title">
                            <img src="../../assets/kuangchi_set.png">
                            <span>矿池设定</span>
                        </h1>
                        <div class="pool-data">
                            <p>
                                <span class="strong">投入SS:</span>
                                <span class="user-input">
                                    <el-input v-model="investment" placeholder="请输入投入矿池SS数量, 最低20000SS"></el-input>
                                </span>
                            </p>
                            <p>创建矿池时投入的SS也将参与挖矿并获得收益分配</p>
                        </div>
                        <div class="pool-data">
                            <p>
                                <span class="strong">收益分配:</span>
                                <span class="user-input slider">
                                    <el-slider v-model="incomeDistribution"></el-slider>
                                </span>
                            </p>
                            <p>将按照设置的百分比从矿池收入 (挖矿奖励等) 中提取并分配给其余矿池的参与者.</p>
                        </div>
                        <div class="pool-bth">
                            <button class="cancel" @click="isVisible('isCreatePool')">取消</button>
                            <button class="immediately-create" @click="createPool()">立即创建</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!--菜单栏-->
        <div class="menu" style="display: none">
            <el-radio-group v-model="tabMenu" class="title">
                <el-radio-button label="mining" class="btn miner">矿场</el-radio-button>
                <el-radio-button label="personal" class="btn personal">个人中心</el-radio-button>
            </el-radio-group>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'mining',
        data() {
            return {
                isCreatePool: false,
                isRanking: false,
                isTSS: false,
                isSetName: false,
                tabTitle: 'mining',
                tabMenu: 'mining',
                options: [
                    {
                        value: 'default',
                        label: '默认排序'
                    },
                    {
                        value: 'capacity',
                        label: '矿池容量'
                    },
                    {
                        value: 'distribution',
                        label: '奖励分配'
                    },
                    {
                        value: 'time',
                        label: '剩余时间'
                    }
                ],
                value: '',
                setname: '',
                incomeDistribution: 80,
                investment: '',
                miningList: [
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
                rewardList: ['1', '2', '3', '4'],
                rankingList: [
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    },
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    },
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    },
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    },
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    },
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    },
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    },
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    },
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    },
                    {
                        account: "SSA-92HT-CNBE-YADN-B4JPW",
                        assets: 1000000
                    }
                ],
                accountInfo: SSO.accountInfo,
            }
        },
        methods: {
            createPool() {
                let _this = this;
                // if (_this.accountInfo.errorCode === 5 || SSO.publicKey === "") {
                //     _this.isVisible('isCreatePool');
                //     _this.$message.info("权限不足");
                //     return;
                // }

                _this.$global.fetch("POST", {
                    secretPhrase: "",
                    publicKey: SSO.publicKey,
                }, "createPool").then(res => {

                    console.info(res);
                    _this.isVisible('isCreatePool');
                }).catch(error => {

                    console.info(error);
                    _this.isVisible('isCreatePool');
                });

            },
            poolAttribute(mining) {
                this.$router.push({name: "mining-attribute", params: mining});
            },
            isVisible(val) {
                this.$store.state.mask = !this[val];
                this[val] = !this[val];
            },
            handleSizeChange(val) {
                console.log(`每页 ${val} 条`);
            },
            handleCurrentChange(val) {
                console.log(`当前页: ${val}`);
            },
            account() {
                let _this = this;
                let platform = navigator.userAgent;
                console.info(platform);
                if (platform.indexOf("iPhone") !== -1 || platform.indexOf("Android") !== -1) {
                    console.info("移动");
                    let token = localStorage.getItem("MOBILE_ACCOUNT");
                    if (token === null) {
                        _this.$router.push({name: 'binding-account'});
                        return false;
                    }
                }
                return true;
            },
        },
        mounted: function () {
            let _this = this;
        },
        created: function () {
            //先判断是否为手机端和授权状态
            if (this.account()) {
                return;
            }

            console.info(this);

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

    #app .el-select .el-input .el-select__caret {
        top: 0 !important;
    }

    .el-select-dropdown__item.selected, .el-pager li.active {
        color: #fff !important;
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

    .mining .create-pool .el-slider__button,
    .mining .create-pool .el-slider__bar {
        background-color: #513acB;
    }

    .img-close {
        position: absolute;
        float: right;
        border-radius: 50%;
        width: 20px;
        height: 20px;
        right: 10px;
        top: 10px;
        cursor: pointer;
        background: url("../../assets/error.svg") no-repeat center;
    }

    .img-close:hover {
        opacity: 0.8;
    }

    .mining-back {
        position: absolute;
        left: 9px;
        top: 9px;
        color: #513ac8;
        cursor: pointer;
    }

    .mining-back:hover {
        color: #513ac8aa;
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
        padding: 30px;
        background: url("../../assets/kuangchi_bg.png") no-repeat center 140px;
        background-color: #513acB;
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

    .invite-friends, .rule-description {
        cursor: pointer;
        display: none;
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

    .my-mining.create {
        margin: 0 0 0 10px;
        display: none;
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
        height: 14px;
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
        cursor: pointer;
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

    @keyframes chatu {
        0% {
            top: 110px;
        }
        100% {
            top: 90px;
        }
    }

    #chatu {
        position: absolute;
        top: 110px;
        left: calc(50% - 34px);
        animation: chatu 1s infinite;
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
        background: #fff;
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
<!--排行-->
<style scoped>
    .ranking {
        position: fixed;
        top: 160px;
        left: calc(50% - 250px);
        background-color: #fff;
        width: 500px;
        border-radius: 6px;
        text-align: center;
        z-index: 9999;
    }

    .ranking-content .ranking-title {
        padding: 20px 0;
        font-size: 16px;
        font-weight: bold;
        background-color: #462cae;
        color: #fff;
        border-top-left-radius: 6px;
        border-top-right-radius: 6px;
    }

    .ranking-content .ranking-table {
        width: 100%;
        text-align: center;
    }

    .ranking-table .ranking-logo {
        display: inline-block;
        width: 100px;
        height: 40px;
        background: no-repeat center;
    }

    .ranking-table .ranking-logo.bg-0 {
        background-image: url("../../assets/ranking_1.png");
    }

    .ranking-table .ranking-logo.bg-1 {
        background-image: url("../../assets/ranking_2.png");
    }

    .ranking-table .ranking-logo.bg-2 {
        background-image: url("../../assets/ranking_3.png");
    }

    .ranking-content .my-assets {
        padding: 30px 0;
        text-align: center;
        font-size: 14px;
        font-weight: bold;
    }

    .ranking-table th {
        font-weight: bold;
        height: 60px;
        min-width: 100px;
        font-size: 14px;
    }

    .ranking-table tr {
        height: 50px;
        border-bottom: 1px solid #f4f7fd;
    }
</style>
<!--创建矿池-->
<style scoped>
    .create-pool {
        position: fixed;
        z-index: 9999;
        top: 180px;
        left: calc(50% - 250px);
        background-color: #fff;
        width: 500px;
        border-radius: 6px;
    }

    .create-pool-content .pool-header {
        text-align: center;
        font-weight: bold;
        font-size: 16px;
        max-height: 60px;
        padding: 20px 0;
    }

    .create-pool-content .pool-title {
        font-size: 14px;
        font-weight: bold;
        padding-bottom: 20px;
    }

    .create-pool-content .pool-title img {
        position: relative;
        top: 4px;
    }

    .create-pool-content .pool-attribute {
        padding: 30px 40px;
        background-color: #513ac8;
        color: #fff;
    }

    .create-pool-content .pool-attribute p {
        margin-top: 20px;
        font-size: 14px;
    }

    .pool-attribute p .strong {
        font-weight: bold;
    }

    .create-pool-content .pool-set {
        padding: 30px 40px;
        color: #999;
        font-size: 14px;
        line-height: 24px;
    }

    .pool-data p {
        padding-bottom: 10px;
        position: relative;
    }

    .pool-data .strong {
        font-weight: bold;
        font-size: 16px;
        color: #000;
        display: inline-block;
        width: 70px;
    }

    .pool-data .user-input {
        width: 340px;
        display: inline-block;
    }

    .pool-data .user-input.slider {
        position: absolute;
        top: -5px;
        left: 76px;
    }

    .pool-set .pool-bth {
        margin-top: 50px;
    }

    .pool-set .pool-bth button {
        height: 40px;
        width: 200px;
        border-radius: 6px;
        outline: none;
        font-size: 16px;
        cursor: pointer;
    }

    .pool-bth .immediately-create {
        float: right;
        background-color: #513ac8;
        color: #fff;
        border: none;
    }

    .pool-bth .immediately-create:hover {
        background-color: #513ac8aa;
    }

    .pool-bth .cancel {
        background-color: #fff;
        color: #513ac8;
        border: 1px solid #513ac8;
    }

    .pool-bth .cancel:hover {
        background-color: #513ac810;
    }

</style>
<!--钱包内置兼容-->
<style>
    @media (max-width: 640px) {
        #app .header {
            display: none;
        }

        #app .page-layout main {
            padding-top: 0;
            width: 100% !important;
        }

        #app .page-layout main .main-content {
            width: 100% !important;
        }

        .mining .el-radio-group {
            display: none;
        }

        .mining .mining-content {
            margin-top: 0;
            padding: 15px;
            border-radius: initial;
            height: 400px;
            background-position: center 210px;
        }

        .mining .mining-list-info .el-row {
            padding: 0 !important;
        }

        .mining .mining-list-info .el-col.el-col-8 {
            width: 100%;
            padding: 0 !important;
        }

        .mining .mining-list-info .grid-content .info {
            width: 35%;
        }

        .mining .mining-list-info .grid-content .tag {
            width: initial !important;
        }

        .mining .mining-list-info .grid-content .tag img {
            padding: 0 10px;
        }

        .mining .mining-content .instructions {
            display: none;
        }

        .mining .mining-content .invite-friends,
        .mining .mining-content .rule-description {
            display: inline-block;
            cursor: pointer;
            color: #fff;
            padding: 10px 20px;
            background: #0000ff;
            border-top-left-radius: 20px;
            border-bottom-left-radius: 20px;
            position: absolute;
            right: 0;
            top: 20px;
            font-size: 14px;
        }

        .mining .mining-content .rule-description {
            top: 66px;
        }

        .mining .mining-content .create {
            position: absolute;
            top: 320px;
            right: 75px;
            font-size: 13px;
        }

        .mining .mining-content .create.my-mining {
            display: initial;
            right: 15px;
        }

        .mining .mining-content .create img {
            width: 45px;
            height: 45px;
        }

        .mining .mining-paging {
            display: none;
        }

        .mining .mining-content .assets ul {
            font-size: 12px;
        }

        .mining .mining-content .assets .strong {
            font-size: 13px;
        }

        .mining .mining-content .assets .strong img {
            width: 12px;
            height: 12px;
            top: 1px;
        }

        .mining .mining-content .state .state-info {
            /*width: 130px;*/
            height: 45px;
            font-size: 14px;
            position: relative;
            top: 80px;
        }

        .mining .mining-list .mining-list-img {
            margin-left: 15px;
        }

        #chatu {
            top: 170px !important;
            animation-name: chatu-mobel !important;
        }

        @keyframes chatu-mobel {
            0% {
                top: 150px;
            }
            100% {
                top: 170px;
            }
        }

        .mining .ranking, .mining .create-pool {
            position: absolute;
            width: calc(100% - 30px);
            left: 15px;
            top: 80px;
        }

        .mining .create-pool {
            position: fixed;
            top: calc(50% - 250px);
        }

        .mining .create-pool-content .pool-title {
            padding: 0;
            text-align: center;
            font-size: 15px;
        }

        .mining .pool-set .pool-title {
            color: #333;
            margin: 0 0 20px 0;
        }

        .pool-attribute p span {
            font-size: 15px;
            font-weight: bold;
        }

        .pool-attribute p .strong {
            font-size: 12px;
            font-weight: initial;
        }

        .create-pool .pool-header {
            display: none;
        }

        .mining .create-pool .pool-attribute, .mining .create-pool .pool-set {
            padding: 15px;
            font-size: 12px;
        }

        .mining .create-pool .pool-set .user-input {
            width: calc(100% - 90px);
            font-size: 12px;
        }

        .mining .create-pool .pool-set .pool-bth {
            margin: 0;
        }

        .mining .create-pool .pool-set .pool-bth .immediately-create {
            width: 100% !important;
            float: initial;
        }

        .mining .create-pool .pool-set .pool-bth .cancel {
            display: none;
        }

        .mining .menu {
            display: initial !important;
            position: fixed;
            left: 0;
            right: 0;
            bottom: 0;
            top: calc(100% - 70px);
            background: #fff;
        }

        .mining .menu .el-radio-group {
            display: block;
        }

        .mining .menu .el-radio-button {
            width: 50%;
        }

        .mining .menu .title .el-radio-button__inner {
            max-width: initial;
            width: 100%;
            height: 70px;
            border: none;
            outline: none;
            background-color: initial !important;
            box-shadow: none;
            font-size: 15px;
        }

        .menu .title .btn {
            background-size: 40px 40px !important;
        }

        .menu .title .btn.miner {
            background: url("../../assets/index.png") no-repeat center 26px;
        }

        .menu .title .btn.personal {
            background: url("../../assets/personal.png") no-repeat center 26px;
        }

        .menu .title .is-active.btn.miner {
            background: url("../../assets/index-1.png") no-repeat center 26px;
        }

        .menu .title .is-active.btn.personal {
            background: url("../../assets/personal-1.png") no-repeat center 26px;
        }

        .menu .el-radio-button__orig-radio:checked + .el-radio-button__inner {
            color: #513ac8 !important;
        }

        .mining .mining-list .mining-list-info {
            padding: 0 10px 70px 10px;
        }
    }
</style>
<!--个人中心-->
<style>
    @media (max-width: 640px) {
        .mining .personal-content {
            padding: 15px 15px 0;
        }

        .mining .personal-content .user {
            height: 170px;
            width: 100%;
            background: #513ac8;
            border-radius: 4px;
            text-align: center;
            color: #fff;
            font-size: 12px;
            margin: 0 0 15px 0;
        }

        .mining .personal-content .user img.header-img {
            width: 80px;
            height: 80px;
            border-radius: 50%;
            background: #fff;
            margin: 15px 0;
            border: 10px solid #0000ff55;
            top: 0;
        }

        .mining .personal-content .user img {
            width: 13px;
            height: 13px;
            position: relative;
            top: 3px;
            margin-left: 4px;
            cursor: pointer;
        }

        .mining .personal-content .user p {
            margin: 0 0 10px 0;
        }

        .mining .personal-content .list {
            background: #fff;
            border-bottom: 1px solid #dbe2e8;
            padding: 10px 15px;
            font-size: 13px;
            color: #333;
            cursor: pointer;
        }

        .mining .personal-content .list img {
            width: 15px;
            height: 15px;
            position: relative;
            top: 2px;
            margin-right: 6px;
        }

        .mining .personal-content .about {
            padding: 60px 0 0;
            text-align: center;
            font-size: 12px;
            color: #999;
        }
    }
</style>
<!--TSS兑换-->
<style>
    @media (max-width: 640px) {
        .mining .tss {
            position: fixed;
            z-index: 9999;
            width: calc(100% - 20px);
            left: 10px;
            top: calc(50% - 100px);
            background: #fff;
            border-radius: 6px;
        }

        .mining .tss h1 {
            font-size: 14px;
            font-weight: bold;
            color: #333;
            padding: 15px 0;
            text-align: center;
        }

        .mining .tss .text {
            padding: 0 20px 20px;
            font-size: 13px;
            color: #333;
        }

        .mining .tss .close {
            background: #513ac8;
            cursor: pointer;
            color: #fff;
            width: 100%;
            font-size: 15px;
            font-weight: bold;
            padding: 15px 0;
            text-align: center;
            border-bottom-right-radius: 6px;
            border-bottom-left-radius: 6px;
        }

    }
</style>
<!--名称设置-->
<style>
    @media (max-width: 640px) {
        .mining .set-name {
            position: fixed;
            width: calc(100% - 20px);
            left: 10px;
            top: calc(50% - 50px);
            z-index: 9999;
            background: #fff;
            border-radius: 6px;
            font-size: 14px;
        }

        .mining .set-name h1 {
            font-weight: bold;
            padding: 15px;
            text-align: center;
        }

        .mining .set-name .input {
            padding: 0 15px 15px;
        }

        .mining .set-name .input input {
            text-align: center;
        }

        .mining .set-name .determine {
            width: 100%;
            background: #513ac8;
            padding: 15px 0;
            border-bottom-left-radius: 6px;
            border-bottom-right-radius: 6px;
            text-align: center;
            color: #fff;
        }

    }
</style>
