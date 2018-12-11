<template>
    <div class="binding-account">
        <!--矿池说明-->
        <div class="introduce">
            <div class="mining-info">
                <h3>共识矿场说明</h3>
                <p>
                    共识矿场是基于Sharder Chain开发的DAPP.以"创建矿池"获得"砖石"为系统逻辑.
                    用户可创建"矿池"以及投入钻石加入矿池挖矿享受砖石分红权.
                </p>
            </div>
            <div class="info">
                <p class="title">砖石说明:</p>
                <p>
                    砖石与钱包的TSS (Sharder测试网络Token) 为1:1对应关系.在进入应用前需要你在当前钱包创建TSS地址.
                </p>
                <p>
                    如果你是矿机用户,请在钱包导入你的矿机挖矿的TSS地址,用于管理挖矿资产及获得对应砖石.
                </p>
                <p>
                    普通用户请先创建TSS地址,以便获得空投砖石
                </p>
                <p>应用内的砖石可兑换为可流通的SS(ERC-20)</p>
            </div>
            <div class="info">
                <p class="title">TSS说明:</p>
                <p>
                    基于Sharder Chain主网正在内测,为了更好的测试主网的运行情况,提高社区用户的活跃度同时推广豆匣项目,
                    我们在测试网络发行了TSS (Sharder测试网络Token: Test SS).本钱包将支持创建TSS账户地址用于收发TSS.
                    TSS不具备流通性,目的只做为测试及与共识矿场钻石产生兑换关系所用.
                </p>
            </div>
            <div class="info">
                <p class="title">TSS获取</p>
                <p>
                    我们将于UTC时间2018年11月01日12:00对所有OX钱包中存储有SS(ERC-20)的地址进行快照.快照结束后将根据
                    XXX:1的比例空投TSS到你的TSS地址中.
                </p>
                <p>
                    未持有SS(ERC-20)的地址中也将会收到XXXX TSS.
                </p>
                <p>
                    进入应用完成应用任务或参与矿池挖矿获得钻石等同于获得TSS.
                </p>
            </div>
            <div class="btn">
                <button @click="isBindingAccount('isAccount')">绑定豆匣账户进入应用</button>
                <p>创建矿池权限也可以通过官网获得,请留意官网信息!</p>
                <p>www.xxxx.org</p>
            </div>
        </div>
        <!--绑定地址-->
        <div v-if="isAccount">
            <div class="account">
                <span class="img-close" @click="isBindingAccount('isAccount')"></span>
                <h1>绑定地址</h1>
                <p>检查到你的钱包持有TSS地址如下,请选择绑定地址</p>
                <div class="addr" v-for="account in accountList" @click="radio = account">
                    <h3>地址:{{account.addr}}</h3>
                    <p>
                        TSS 数量: {{account.TSS}}
                    </p>
                    <span class="radio">
                        <el-radio v-model="radio" :label="account">&nbsp;</el-radio>
                    </span>
                </div>
                <div class="btn">
                    <button @click="bindingAddr()">立即绑定</button>
                </div>
            </div>
        </div>
        <!--绑定状态-->
        <div v-if="isBinding">
            <div class="binding">
                <span class="img-close" @click="isBindingAccount('isBinding')" v-if="binding !== 'padding'"></span>
                <h1>绑定地址</h1>
                <p class="img">
                    <span v-if="binding === 'padding'"><i class="el-icon-loading"></i></span>
                    <span v-if="binding === 'success'"><i class="el-icon-success"></i></span>
                    <span v-if="binding === 'failure'"><i class="el-icon-error"></i></span>
                </p>
                <p>
                    <span v-if="binding === 'padding'">绑定中...</span>
                    <span v-if="binding === 'success'">绑定成功</span>
                    <span v-if="binding === 'failure'">绑定失败</span>
                </p>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "binding-account",
        data() {
            return {
                isAccount: false,
                isBinding: false,
                accountList: [],
                radio: '',
                binding: "padding",
            }
        },
        methods: {
            isBindingAccount(val) {
                this.$store.state.mask = !this[val];
                this[val] = !this[val];
            },
            bindingAddr() {
                let _this = this;
                if (_this.radio === '') {
                    return;
                }
                _this.binding = "padding";

                _this.isBindingAccount('isAccount');
                _this.isBindingAccount('isBinding');

                _this.$global.fetch("POST", {
                    shell: "bindingAccount",
                    token: window.token,
                    account: _this.radio.addr,
                }, "authorizationLogin").then(value => {
                    // console.info(value.data);
                    if (!value.success) return;
                    setTimeout(function () {
                        _this.binding = "success";
                        setTimeout(function () {
                            _this.isBindingAccount('isBinding');
                        }, 1000);
                    }, 1000);
                });

            },
        },
        mounted() {
            let _this = this;
            _this.$global.fetch("POST", {
                shell: "getAccountList",
                token: window.token,
            }, "authorizationLogin").then(value => {
                // console.info(value.data);
                if (!value.success) return;

                let keys = Object.keys(value.data);
                let accountList = [];
                for (let o of keys) {
                    if (o.indexOf("[addr]") !== 1) {
                        let a = o.substring("accountList[".length, o.lastIndexOf("][addr]"));
                        if (/^[0-9]+$/.test(a)) {
                            if (!accountList[a]) {
                                accountList[a] = {};
                            }
                            accountList[a]["addr"] = value.data[o];
                        }
                    }
                    if (o.indexOf("[TSS]") !== 1) {
                        let a = o.substring("accountList[".length, o.lastIndexOf("][TSS]"));
                        if (/^[0-9]+$/.test(a)) {
                            if (!accountList[a]) {
                                accountList[a] = {};
                            }
                            accountList[a]["TSS"] = value.data[o];
                        }
                    }
                }
                _this.accountList = accountList;
            });
        },
        created() {
            window.token = window.location.search.substring(1 + "token".length);
            console.info(token);

        }
    }
</script>
<style>
    .account .addr .radio .el-radio__label {
        display: none;
    }
</style>
<style scoped>
    .img-close {
        position: absolute;
        float: right;
        border-radius: 50%;
        width: 20px;
        height: 20px;
        right: 10px;
        top: 10px;
        cursor: pointer;
        background: url("../../assets/img/error.svg") no-repeat center;
    }

    .img-close:hover {
        opacity: 0.8;
    }

    .binding-account .introduce {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        overflow: auto;
        background: #fff;
        padding: 15px;
    }

    .introduce .mining-info {
        background: #513ac8;
        border-radius: 4px;
        color: #fff;
        padding: 15px 20px;
    }

    .introduce .mining-info p {
        color: #fff;
    }

    .introduce .mining-info h3 {
        text-align: center;
        font-size: 15px;
        font-weight: bold;
        padding: 0 0 15px;
    }

    .introduce > div p {
        font-size: 12px;
        color: #333;
    }

    .introduce .info {
        margin: 10px 0 0;
    }

    .introduce .info .title {
        font-weight: bold;
    }

    .introduce .btn button {
        width: 100%;
        height: 40px;
        margin: 30px 0;
        border-radius: 4px;
        outline: none;
        border: none;
        font-size: 15px;
        color: #fff;
        background: #513ac8;
    }

    .introduce .btn p {
        color: #513ac8;
        text-align: center;
    }

    .account {
        position: fixed;
        max-height: 50%;
        width: calc(100% - 30px);
        left: 15px;
        top: 25%;
        background: #fff;
        overflow: auto;
        z-index: 9999;
        border-radius: 4px;
    }

    .account h1 {
        padding: 15px 0;
        font-size: 14px;
        font-weight: bold;
        color: #333;
        text-align: center;
    }

    .account h1 + p {
        text-align: center;
    }

    .account p {
        font-size: 12px;
        color: #333;
        padding: 0 0 10px;
    }

    .account .addr {
        margin: 0 15px;
        padding: 10px 5px 0;
        border-top: 1px solid #dbe2e8;
        border-bottom: 1px solid #dbe2e8;
        position: relative;
    }

    .account p + .addr {
        border-bottom: none;
    }

    .account .addr h3 {
        font-size: 12px;
        font-weight: bold;
        color: #333;
        padding: 0 0 8px;
    }

    .account .btn button {
        width: 100%;
        height: 40px;
        border-bottom-right-radius: 4px;
        border-bottom-left-radius: 4px;
        background: #513ac8;
        color: #fff;
        outline: none;
        border: none;
    }

    .account .addr .radio {
        position: absolute;
        top: 20px;
        right: 10px;
    }

    .binding {
        text-align: center;
        position: fixed;
        top: calc(50% - 75px);
        height: 150px;
        width: calc(100% - 20px);
        background: #fff;
        border-radius: 4px;
        z-index: 9999;
        left: 10px;
    }

    .binding h1 {
        font-size: 15px;
        font-weight: bold;
        padding: 15px 30px;
    }

    .binding p {
        font-size: 14px;
        color: #666;
    }

    .binding p.img {
        margin: 0 0 10px;
    }

    .binding .img span {
        font-size: 60px;
    }

    .binding .el-icon-loading {
        color: #493eda;
    }

    .binding .el-icon-success {
        color: #31da78;
    }

    .binding .el-icon-error {
        color: red;
    }

</style>
