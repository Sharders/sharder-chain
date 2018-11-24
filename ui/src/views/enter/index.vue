<template>
    <div class="content_enter">

        <el-col :span="24" class="tip">
            <a>您的密钥非常重要！为确保您已保存它, 请填写上一步生生成的密钥：</a>
        </el-col>

        <el-col :span="24" class="input">
            <el-input type="textarea" :autosize=notAutoSize></el-input>
        </el-col>

        <el-col :span="24">
            <el-button class="common_btn_contrary medium" @click="cancel">取消</el-button>
            <el-button class="common_btn medium" @click="enter">进入客户端</el-button>
        </el-col>

    </div>
</template>

<script>
    import Store from "../../store";

    export default {
        name: "index",
        data () {
            return {
                notAutoSize: {
                    minRows: 4,
                    maxRows: 4
                }
            };
        },
        methods: {
            cancel: function () {
                this.$router.push("/login");
            },
            enter: function () {
                Store.commit("loginState");
                this.$router.push("/account");
            }
        }
    };
</script>

<style lang="scss">
    @import './style.scss';
    @import '../../styles/css/common.scss';
</style>
