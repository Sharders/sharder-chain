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
            <div class="mining-info-div">
                <div class="info">
                    <p class="title">{{$t('mining.binding_account.poc')}}</p>
                    <p>
                        {{$t('mining.binding_account.poc_tip1')}}
                    </p>
                    <p>
                        {{$t('mining.binding_account.poc_tip2')}}
                    </p>

                </div>

                <div class="info">
                    <p class="title">{{$t('mining.binding_account.sharder_pool')}}</p>
                    <p>
                        {{$t('mining.binding_account.sharder_pool_tip1')}}
                    </p>
                    <p>
                        {{$t('mining.binding_account.sharder_pool_tip2')}}
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
                    <p>
                        {{$t('mining.binding_account.ss_tip1')}}
                    </p>
                    <p>
                        {{$t('mining.binding_account.ss_tip2')}}
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
                <div class="addressListDiv">
                    <div class="addr" v-for="account in accountList" @click="radio = account">
                        <h3>{{$t('mining.binding_account.address')}}{{account.account}}</h3>
                        <p>
                            {{$t('mining.binding_account.tss_volume')}}{{account.assets}}
                        </p>
                        <span class="radio">
                        <el-radio v-model="radio" :label="account">&nbsp;</el-radio>
                    </span>
                    </div>
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
    .account .addressListDiv .addr .radio .el-radio__label {
        display: none;
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

        .introduce .mining-info-div{
            overflow: auto;
            height: 386px;
        }

        .mining .mining-list-info .el-row {
            padding: 0 !important;
        }

        .mining .mining-list-info .el-col.el-col-8 {
            width: 100%;
            padding: 0 !important;
        }

        .mining .mining-list-info .grid-content .info {
            width: 45%;
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
            /*display: none;*/
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

        .mining .mining-content .state {
            top: 110px;
            width: calc(100% - 30px);
        }

        .mining .mining-content .state .state-info {
            font-size: 12px;
            max-width: 100%;
        }

        .mining .mining-list .mining-list-img {
            margin-left: 15px;
        }

        .ranking-content .ranking-table {
            font-size: 12px;
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
            top: 60px;
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
            background: url("../../assets/img/index.png") no-repeat center 26px;
        }

        .menu .title .btn.personal {
            background: url("../../assets/img/personal.png") no-repeat center 26px;
        }

        .menu .title .is-active.btn.miner {
            background: url("../../assets/img/index-1.png") no-repeat center 26px;
        }

        .menu .title .is-active.btn.personal {
            background: url("../../assets/img/personal-1.png") no-repeat center 26px;
        }

        .menu .el-radio-button__orig-radio:checked + .el-radio-button__inner {
            color: #513ac8 !important;
        }

        .mining .mining-list .mining-list-info {
            padding: 7px 8px 110px 10px;
        }

        .ranking-table th {
            height: 30px !important;
        }

        .ranking-table tr {
            height: 40px !important;
        }
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
      /*  position: fixed;*/
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
        /*overflow: auto;*/
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

    .account .addressListDiv{
        overflow: auto;
        max-width: 40%;
    }

    .account .addressListDiv .addr {
        margin: 0 15px;
        padding: 10px 5px 0;
        border-top: 1px solid #dbe2e8;
        border-bottom: 1px solid #dbe2e8;
        position: relative;
    }

    .account p + .addressListDiv .addr {
        border-bottom: none;
    }

    .account .addressListDiv .addr h3 {
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

    .account .addressListDiv .addr .radio {
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
