<template>
    <div class="container_login">
        <div class="content">
            <div class="content_message">
                <div>
                    <a>请用您的测试网络账户登录-不是您的真实账户!</a><br/>
                    <a>警告:您已连接到测试网络,不要使用您的真实密钥!</a>
                    <a>The testnet peer port is 8218</a>
                </div>
            </div>
            <div class="content_login">
                <el-row>
                    <el-col :span="24">
                        <div >语言</div>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="24">
                        <div >this is login page2</div>
                    </el-col>
                </el-row>
                <button @click="login()"></button>
            </div>
        </div>
    </div>
</template>

<script>
    import store from "../../store";
    export default {
        name: "index",
        data () {
            return {
            };
        },
        methods: {
            login: function () {
                store.commit("loginState");
                this.$router.push("/");
                console.log(store.state.isLogin);
            }
        }
    };
</script>

<<style lang="scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
