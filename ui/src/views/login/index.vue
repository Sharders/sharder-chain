<template>
    <div class="content_login">
        <el-radio-group v-model="tabTitle" class="title">
            <el-radio-button label="key" class="btn">密钥登录</el-radio-button>
            <el-radio-button label="account" class="btn">账户登录</el-radio-button>
        </el-radio-group>

        <el-col :span="24" class="login_operation">
            <input v-if="tabTitle === 'key'" class="account_input" type="password" v-model="secretPhrase"
                   placeholder="请输入账户密钥"/>
            <masked-input v-if="tabTitle === 'account'" class="secret_key_input" v-model="account"
                          mask="AAA-****-****-****-*****" placeholder="Sharder账户" />
            <el-button class="common_btn" @click="login">登录</el-button>
        </el-col>

        <el-col :span="24">
            <img src="../../assets/create_account.png"/>
            <a @click="register">没有账户? 创建账户</a>
        </el-col>

    </div>
</template>

<script>
    export default {
        components: {
            "masked-input": require("vue-masked-input").default
        },
        name: "index",
        data() {
            return {
                tabTitle: "key",
                secretPhrase: "trail shape agree haunt distant attention perhaps skill admit deny week remind",
                account: "SSA-ENRV-X8TW-UQND-AFA7W",
                type: 1
            };
        },
        methods: {
            getAccount() {
                if (this.tabTitle === "key") {
                    this.type = 1;
                    SSO.secretPhrase = this.secretPhrase;
                    return this.secretPhrase ? this.secretPhrase : "";
                }
                if (this.tabTitle === "account") {
                    this.type = 0;
                    return this.account ? this.account : "";
                }
            },
            login: function () {
                let _this = this;
                let val = _this.getAccount();
                if (val === "") {
                    _this.$message.info("请输入账号或私钥");
                    return;
                }
                Login.login(_this.type, val, _this, function () {
                    console.log(SSO);
                    // console.log("account", SSO.account);
                    // console.log("accountInfo", SSO.accountInfo);
                    // console.log("accountRS", SSO.accountRS);
                    // console.log("publicKey", SSO.publicKey);
                    // console.log("settings", SSO.settings);
                    _this.$store.state.isLogin = true;
                    _this.$router.push("/account");
                    _this.$global.setEpochBeginning(_this);
                });

                // if (this.type && $.trim(this.account)) {
                //     this.$store.state.isPassphrase = true;
                //     this.$store.state.passphrase = this.account;
                // } else {
                //     this.$store.state.isPassphrase = false;
                //     this.$store.state.passphrase = "";
                // }
            },
            register: function () {
                this.$router.push("/register");
            },
            languageChange: function (language) {
                console.log(language);
            }
        }
    };
</script>

<style>
    .content_login .title .el-radio-button__orig-radio:checked + .el-radio-button__inner {
        background-color: #513ac8;
        border-color: #513ac8;
    }

    .content_login .title .el-radio-button__inner:hover {
        color: #513ac8;
    }

    .content_login .title .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
        color: #fff;
    }

    .content_login .title {
        display: block !important;
        text-align: left;
    }
</style>
<style lang="scss">
    @import './style.scss';
    @import '../../styles/css/common.scss';
</style>
