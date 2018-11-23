<template>
    <div class="content_login">

        <el-col :span="24" class="switch">
            <button class="common_btn_contrary" id="secretKeyLogin" @click="loginChange(0)">密钥登录</button>
            <button class="common_btn" id="accountLogin" @click="loginChange(1)">账户登录</button>
        </el-col>

        <el-col :span="24" class="login_operation">
            <input class="secret_key_input" type="password" v-model="data" placeholder="请输入账户密钥"/>
            <masked-input class="account_input" v-model="data" mask="AAA-****-****-****-*****" placeholder="Sharder账户"/>
            <el-button class="common_btn" @click="login">登录</el-button>
        </el-col>

        <el-col :span="24">
            <img src="../../assets/create_account.png"/>
            <a @click="register">没有账户? 创建账户</a>
        </el-col>

    </div>
</template>

<script>
    import SSO from "../../../static/js/sso";
    export default {
        components: {
            "masked-input": SSO.MaskedInput
        },
        name: "index",
        data () {
            return {
                data: "",
                type:1
            };
        },
        methods: {
            login: function () {
                console.log(this.type)
                let secretPhrase = SSO.NRS.getAccountId("together learn possibly change son search alive quick feather shape change chance");
                let nxtAddress = new SSO.NxtAddress();
                let accountRS = "";
                if (nxtAddress.set(secretPhrase)) {
                    accountRS = nxtAddress.toString();
                }
                console.log(accountRS);
                // this.$store.state.isLogin = true;
                // this.$router.push("/account");
                this.$global.setEpochBeginning(this);
            },
            loginChange: function (type) {
                this.type = type;
                if (type === 0) {
                    $("#secretKeyLogin").addClass("common_btn");
                    $("#secretKeyLogin").removeClass("common_btn_contrary");
                    $("#accountLogin").addClass("common_btn_contrary");
                    $("#accountLogin").removeClass("common_btn");
                    $(".secret_key_input").val("");
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
