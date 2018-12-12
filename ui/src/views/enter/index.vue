<template>
    <div class="content_enter">

        <el-col :span="24" class="tip">
            <a>{{$t('enter.enter_tip')}}</a>
        </el-col>

        <el-col :span="24" class="input">
            <el-input type="textarea" :autosize=notAutoSize></el-input>
        </el-col>

        <el-col :span="24">
            <el-button class="common_btn_contrary medium" @click="cancel">{{$t('enter.enter_cancel')}}</el-button>
            <el-button class="common_btn medium" @click="enter">{{$t('enter.enter_client')}}</el-button>
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
