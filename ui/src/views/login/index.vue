<template>
    <div class="content_login">

        <el-col :span="24" class="switch">
            <button class="common_btn_contrary" id="secretKeyLogin" @click="loginChange(0)">密钥登录</button>
            <button class="common_btn" id="accountLogin" @click="loginChange(1)">账户登录</button>
        </el-col>

        <el-col :span="24" class="login_operation">
            <input class="secret_key_input" v-model="data" placeholder="请输入账户密钥"/>
            <masked-input class="account_input" v-model="data" mask="AAA-****-****-****-*****" placeholder="Sharder账户" />
            <el-button class="common_btn" @click="login">登录</el-button>
        </el-col>

        <el-col :span="24">
            <img src="../../assets/create_account.png"/>
            <a @click="register">没有账户? 创建账户</a>
        </el-col>

    </div>
</template>

<script>
    import Store from "../../store";
    import $ from "jquery";
    import MaskedInput from "vue-masked-input";
    export default {
        components: {
            "masked-input": MaskedInput
        },
        name: "index",
        data () {
            return {
                data: ""
            };
        },
        methods: {
            login: function () {
                Store.commit("loginState");
                this.$router.push("/account");
            },
            loginChange: function (type) {
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
    @import '~scss_vars';
    @import './style.scss';
    @import '../../styles/common.scss';
</style>
