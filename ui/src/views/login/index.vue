<template>
    <div class="content_login">

        <el-col :span="24" class="switch">
            <button class="common_btn_contrary" id="secretKeyLogin" @click="loginChange(1)">密钥登录</button>
            <button class="common_btn" id="accountLogin" @click="loginChange(0)">账户登录</button>
        </el-col>

        <el-col :span="24" class="login_operation">
            <input class="account_input" type="password" v-model="account" placeholder="请输入账户密钥"/>
            <masked-input class="secret_key_input" v-model="account" mask="AAA-****-****-****-*****"
                          placeholder="Sharder账户"/>
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
                account: "",
                type: 0
            };
        },
        methods: {
            login: function () {
                Login.login(this.type,this.account,this);
                console.log(SSO);
// many blend glass feet fragile drawn hey ready horse throat tide space
// SSA-TPLD-BHYH-DF2B-GAU6P
                // console.log(this.type)
                // console.info(this.account)
                // let account = this.account;
                // let secretPhrase = SSO.getAccountId(this.account);
                // let nxtAddress = new NxtAddress();
                // let accountRS = "";
                // if (nxtAddress.set(secretPhrase)) {
                //     accountRS = nxtAddress.toString();
                // }
                // console.log(accountRS);
                // let accountRequest;
                // let requestVariable;
                // if (this.type) {
                //     accountRequest = "getAccountId";
                //     requestVariable = {secretPhrase: account};
                // }else {
                //     accountRequest = "getAccount";
                //     requestVariable = {account: account};
                // };
                // this.$http.post(this.url + "getBlockchainStatus").then(res => {
                //     console.log(res)
                //     if (res.errorCode) {
                //         this.$message.error(res.errorDescription);
                //         return;
                //     }
                //     // global.state = res;
                // })
                if (this.type && $.trim(this.account)) {
                    this.$store.state.isPassphrase = true;
                    this.$store.state.passphrase = this.account;
                } else {
                    this.$store.state.isPassphrase = false;
                    this.$store.state.passphrase = "";
                }
                ;
                this.$store.state.isLogin = true;
                this.$router.push("/account");
                this.$global.setEpochBeginning(this);
            },
            loginChange: function (type) {
                this.type = type;
                this.account = "";
                if (type === 0) {
                    $("#secretKeyLogin").addClass("common_btn");
                    $("#secretKeyLogin").removeClass("common_btn_contrary");
                    $("#accountLogin").addClass("common_btn_contrary");
                    $("#accountLogin").removeClass("common_btn");
                    $(".secret_key_input").show();
                    $(".account_input").hide();
                } else {
                    $("#accountLogin").addClass("common_btn");
                    $("#accountLogin").removeClass("common_btn_contrary");
                    $("#secretKeyLogin").addClass("common_btn_contrary");
                    $("#secretKeyLogin").removeClass("common_btn");
                    $(".account_input").show();
                    $(".secret_key_input").hide();
                }
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


<style lang="scss">
    @import './style.scss';
    @import '../../styles/css/common.scss';
</style>
