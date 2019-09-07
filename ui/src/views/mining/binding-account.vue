<template>
    <div class="binding-account">
        <!--矿池说明-->
        <div class="introduce">
            <div class="mining-info">
                <h3>{{$t('mining.binding_account.title')}}</h3>
                <p>
                    {{$t('mining.binding_account.subtitle1')}}
                    {{$t('mining.binding_account.subtitle2')}}
                </p>
            </div>
            <div class="info">
                <p class="title">{{$t('mining.binding_account.description')}}</p>
                <p>
                    {{$t('mining.binding_account.description_tip1')}}
                </p>
                <p>
                    {{$t('mining.binding_account.description_tip2')}}
                </p>
                <p>
                    {{$t('mining.binding_account.description_tip3')}}
                </p>
                <p>{{$t('mining.binding_account.description_tip4')}}</p>
            </div>
            <div class="info">
                <p class="title">{{$t('mining.binding_account.tss_description')}}</p>
                <p>
                    {{$t('mining.binding_account.tss_description_tip')}}
                </p>
            </div>
            <div class="info">
                <p class="title">{{$t('mining.binding_account.tss_acquisition')}}</p>
                <p>
                    {{$t('mining.binding_account.tss_acquisition_tip1')}}
                </p>
                <p>
                    {{$t('mining.binding_account.tss_acquisition_tip2')}}
                </p>
                <p>
                    {{$t('mining.binding_account.tss_acquisition_tip3')}}
                </p>
            </div>
            <div class="btn">
                <button @click="isBindingAccount('isAccount')">{{$t('mining.binding_account.bind_btn')}}</button>
                <p>{{$t('mining.binding_account.bind_btn_tip1')}}</p>
                <p>{{$t('mining.binding_account.bind_btn_tip2')}}</p>
            </div>
        </div>
        <!--绑定地址-->
        <div v-if="isAccount">
            <div class="account">
                <span class="img-close" @click="isBindingAccount('isAccount')"></span>
                <h1>{{$t('mining.binding_account.bind_address')}}</h1>
                <p>{{$t('mining.binding_account.bind_address_tip')}}</p>
                <div class="addr" v-for="account in accountList" @click="radio = account">
                    <h3>{{$t('mining.binding_account.address')}}{{account.account}}</h3>
                    <p>
                        {{$t('mining.binding_account.tss_volume')}}{{account.assets}}
                    </p>
                    <span class="radio">
                        <el-radio v-model="radio" :label="account">&nbsp;</el-radio>
                    </span>
                </div>
                <div class="btn">
                    <button @click="bindingAddr()">{{$t('mining.binding_account.binding_immediately')}}</button>
                </div>
            </div>
        </div>
        <!--绑定状态-->
        <div v-if="isBinding">
            <div class="binding">
                <span class="img-close" @click="isBindingAccount('isBinding')" v-if="binding !== 'padding'"></span>
                <h1>{{$t('mining.binding_account.bind_address')}}</h1>
                <p class="img">
                    <span v-if="binding === 'padding'"><i class="el-icon-loading"></i></span>
                    <span v-if="binding === 'success'"><i class="el-icon-success"></i></span>
                    <span v-if="binding === 'failure'"><i class="el-icon-error"></i></span>
                </p>
                <p>
                    <span v-if="binding === 'padding'">{{$t('mining.binding_account.bind_address')}}</span>
                    <span v-if="binding === 'success'">{{$t('mining.binding_account.bind_success')}}</span>
                    <span v-if="binding === 'failure'">{{$t('mining.binding_account.bing_error')}}</span>
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
                const _this = this;
                if (_this.radio === '') {
                    return;
                }
                _this.binding = "padding";

                _this.isBindingAccount('isAccount');
                _this.isBindingAccount('isBinding');

                _this.$global.fetch("POST", {
                    shell: "bindingAccount",
                    token: window.token,
                    account: _this.radio.account,
                }, "authorizationLogin").then(value => {
                    // console.info(value.data);
                    if (!value.success) {
                        window.parent.postMessage("false","*");
                        return;

                    }else{
                        window.parent.postMessage("success","*");
                        setTimeout(function () {
                            _this.binding = "success";
                            setTimeout(function () {
                                _this.isBindingAccount('isBinding');
                            }, 1000);
                        }, 1000);
                    }


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
                    if (o.indexOf("[account]") !== 1) {
                        let a = o.substring("accountList[".length, o.lastIndexOf("][account]"));
                        if (/^[0-9]+$/.test(a)) {
                            if (!accountList[a]) {
                                accountList[a] = {};
                            }
                            accountList[a]["account"] = value.data[o];
                        }
                    }
                    if (o.indexOf("[assets]") !== 1) {
                        let a = o.substring("accountList[".length, o.lastIndexOf("][assets]"));
                        if (/^[0-9]+$/.test(a)) {
                            if (!accountList[a]) {
                                accountList[a] = {};
                            }
                            accountList[a]["assets"] = value.data[o];
                        }
                    }
                }
                _this.accountList = accountList;
            });
        },
        created() {
            window.token = window.location.search.substring(1 + "token".length);
            console.info("token:"+token);

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
