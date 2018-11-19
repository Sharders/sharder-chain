<template>
    <div class="pool-attribute">
        <div>
            <p @click="$router.back()" class="pool-back">&lt;&lt;返回矿池</p>
            <div class="pool-content">
                <div class="attribute-info">
                    <img src="../../assets/shouyi.png" id="shouyi">
                    <div class="attribute-text">
                    <span class="pool-serial-number">
                        矿池编号:{{mining.serialNumber}} | 挖矿几率 : {{mining.distribution}}%
                    </span>
                        <span class="pool-attribute-info" @click="miningMask('isAttribute')">矿池属性</span>
                    </div>
                    <div class="earnings">收益+2000SS</div>
                </div>
                <div class="my-info">
                    <h1>
                        <img src="../../assets/miner-info1.svg" class="head-portrait">
                        <span>我的信息</span>
                    </h1>
                    <div class="my-attribute">
                        <el-row :gutter="20">
                            <el-col :span="6">
                                <button class="info">
                                    <p>加入时间</p>
                                    <p class="strong">2018-06-13 12:00</p>
                                </button>
                            </el-col>
                            <el-col :span="6">
                                <button class="info">
                                    <p>投入SS</p>
                                    <p class="strong">100000</p>
                                </button>
                            </el-col>
                            <el-col :span="6">
                                <button class="info">
                                    <p>获得收益</p>
                                    <p class="strong">2000 SS</p>
                                </button>
                            </el-col>
                            <el-col :span="6">
                                <button class="info">
                                    <p>剩余挖矿时间</p>
                                    <p class="strong">600块(约12h)</p>
                                </button>
                            </el-col>
                        </el-row>
                    </div>
                    <div class="attribute-btn">
                        <button class="join" @click="miningMask('isJoinPool')">投入SS</button>
                        <button class="exit" @click="miningMask('isExitPool')">退出矿池</button>
                    </div>
                </div>
            </div>
        </div>
        <div v-if="isAttribute">
            <div class="mining-attribute">
                <span class="img-close" @click="miningMask('isAttribute')"></span>
                <div class="attribute">
                    <h1>
                        <img src="../../assets/pay.svg" class="attribute-img">
                        <span>矿池属性</span>
                    </h1>
                    <div class="attribute-value">
                        <el-row :gutter="20">
                            <el-col :span="12">
                                <button class="info">
                                    创建者:SSA......84UPW
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    参与用户:23
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    矿池编号:{{mining.serialNumber}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    容量:{{mining.currentInvestment}}/{{mining.investmentTotal}}
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    矿池收益:100000
                                </button>
                            </el-col>
                            <el-col :span="12">
                                <button class="info">
                                    奖励分配:{{mining.distribution}}%
                                </button>
                            </el-col>
                        </el-row>
                    </div>
                </div>
            </div>
        </div>
        <div v-if="isJoinPool">
            <div class="join-pool">
                <span class="img-close" @click="miningMask('isJoinPool')"></span>
                <h1 class="title">投入SS</h1>
                <p class="attribute">当前可用:200000SS | 矿池容量:2000000SS</p>
                <p class="input">
                    <el-input v-model="joinPool" placeholder="请输入投入数量"></el-input>
                </p>
                <p class="btn">
                    <button class="cancel" @click="miningMask('isJoinPool')">取消</button>
                    <button class="confirm">确认</button>
                </p>
            </div>
        </div>
        <div v-if="isExitPool">
            <div class="exit-pool">
                <span class="img-close" @click="miningMask('isExitPool')"></span>
                <h1 class="title">退出矿池</h1>
                <p class="info">退出就无法继续挖矿获得收益</p>
                <p class="btn">
                    <button class="cancel" @click="miningMask('isExitPool')">取消</button>
                    <button class="confirm">确认</button>
                </p>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "attribute",
        data() {
            return {
                mining: this.$route.params,
                isAttribute: false,
                isJoinPool: false,
                isExitPool: false,
                joinPool: '',
            }
        },
        methods: {
            miningExit() {
                this.miningMask('isExitPool');

            },
            miningMask(val) {
                if (this[val]) {
                    this.$store.state.mask = false;
                } else {
                    this.$store.state.mask = true;
                }
                this[val] = !this[val];
            },
        },
        created: function () {

        }
    }
</script>
<!--矿池详情-->
<style scoped>
    .pool-attribute .pool-back {
        font-size: 16px;
        color: #493eda;
        cursor: pointer;
    }

    .pool-content .attribute-info {
        padding: 30px;
        background: url("../../assets/kuangchi_bg.png") no-repeat center 140px;
        background-color: #513acB;
        height: 300px;
        border-top-right-radius: 6px;
        border-top-left-radius: 6px;
        margin-top: 10px;
        position: relative;
    }

    .attribute-info .attribute-text {
        font-size: 16px;
        color: #fff;
    }

    .attribute-info .pool-attribute-info {
        float: right;
        cursor: pointer;
        border-bottom: 1px solid #fff;
    }

    .attribute-info .earnings {
        width: 100px;
        text-align: center;
        margin: auto;
        position: absolute;
        top: 180px;
        left: calc(50% - 50px);
        font-size: 16px;
        color: #fff;
    }

    #shouyi {
        position: absolute;
        top: 140px;
        left: calc(50% - 18px);
    }

    .pool-content .my-info {
        padding: 0 40px 50px;
        background: #fff;
    }

    .pool-content .my-info h1 {
        font-size: 18px;
        font-weight: bold;
        color: #333;
        padding: 18px 0;
    }

    .my-info .head-portrait {
        width: 20px;
        height: 20px;
        background: #513acB;
        border-radius: 50%;
        position: relative;
        top: 4px;
        margin-right: 12px;
    }

    .my-attribute .info {
        box-shadow: 0 0 2px #513acBaa;
        height: 100px;
        width: 100%;
        border: none;
        outline: none;
        border-radius: 4px;
        padding: 0;
        font-size: 16px;
        color: #333;
        background: transparent;
    }

    .my-attribute .info .strong {
        font-size: 20px;
        margin-top: 16px;
    }

    .my-info .attribute-btn button {
        border: none;
        outline: none;
        background: transparent;
        width: 200px;
        height: 40px;
        border-radius: 6px;
        font-size: 14px;
        margin: 40px 0 0 0;
        cursor: pointer;
    }

    .attribute-btn button.join {
        background: #513acB;
        color: #fff;
    }

    .attribute-btn button.exit {
        border: 1px solid #513acB;
        margin-left: 20px;
        color: #513acB;
    }

    .attribute-btn button.join:hover {
        background: #513acBaa;
    }

    .attribute-btn button.exit:hover {
        background: #513acB33;
    }
</style>
<!--矿池属性-->
<style scoped>
    .mining-attribute {
        position: absolute;
        top: 140px;
        left: calc(50% - 325px);
        width: 650px;
        border-radius: 6px;
        z-index: 9999;
        background: #fff;
    }

    .mining-attribute .attribute h1 {
        text-align: center;
        font-size: 20px;
        color: #333;
        font-weight: bold;
        padding: 28px 0;
    }

    .mining-attribute .attribute-img {
        width: 20px;
        height: 20px;
        position: relative;
        top: 3px;
    }

    .mining-attribute .attribute-value {
        padding: 0 50px 40px;
    }

    .attribute-value .info {
        border: 1px dashed #dbe2e8;
        outline: none;
        width: 100%;
        height: 60px;
        background: transparent;
        margin-top: 10px;
        color: #513acB;
        font-size: 16px;
    }

</style>
<!--加入矿池-->
<style scoped>
    .join-pool {
        position: fixed;
        top: calc(50% - 128px);
        z-index: 9999;
        left: calc(50% - 250px);
        width: 500px;
        background: #fff;
        border-radius: 6px;
        text-align: center;
    }

    .join-pool .title {
        font-size: 16px;
        font-weight: bold;
        padding: 20px 0;
        border-bottom: 1px solid #d2d2d2;
    }

    .join-pool .attribute {
        font-size: 14px;
        padding: 20px 0;
        color: #333;
    }

    .join-pool .input {
        padding: 0 40px;
    }

    .join-pool .btn {
        margin: 30px 40px;
        height: 40px;
    }

    .join-pool .btn button {
        outline: none;
        width: 200px;
        height: 40px;
        border-radius: 4px;
        font-size: 14px;
        background: transparent;
        cursor: pointer;
    }

    .btn button.cancel {
        border: 1px solid #513acB;
        color: #513acB;
        float: left;
        background: #fff;
    }

    .btn button.confirm {
        float: right;
        background: #513acB;
        color: #fff;
        border: none;
    }

    .btn button.cancel:hover {
        background: #513acB11;
    }

    .btn button.confirm:hover {
        background: #513acBdd;
    }

</style>
<!--退出矿池-->
<style scoped>
    .exit-pool {
        position: fixed;
        width: 500px;
        height: 220px;
        top: calc(50% - 110px);
        left: calc(50% - 250px);
        background: #fff;
        border-radius: 4px;
        z-index: 9999;
    }

    .exit-pool .title {
        text-align: center;
        padding: 22px 0;
        font-weight: bold;
    }

    .exit-pool .info {
        text-align: center;
        padding: 35px;
        font-size: 14px;
        color: #333;
    }

    .exit-pool .btn {
        height: 40px;
        padding: 0 40px;
    }

    .exit-pool .btn button {
        outline: none;
        border-radius: 4px;
        width: 200px;
        height: 40px;
        font-size: 14px;
        cursor: pointer;
    }

</style>
