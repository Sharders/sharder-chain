<template>
    <div class="content_register">

        <el-col :span="24">
            <a>已为您生成账户密钥</a>
        </el-col>

        <el-col :span="24">
            <div class="radius_blue">
                <div class="phrase">reason grandma guess mend behind odd sick fear tumble game hopefully dot</div>
            </div>
        </el-col>

        <el-col :span="24">
            <div class="radius_blue tip">
                <div>请写下来或记住这12单词。该密钥用于访问您的 豆匣 账户。</div>
                <div>注意：永远不要透露您的密钥。如果失去它您将无法访问您的账户！</div>
            </div>
        </el-col>

        <el-col :span="24">
            <el-button class="common_btn_contrary medium" @click="cancel">取消</el-button>
            <el-button class="common_btn medium" @click="enter">下一步</el-button>
        </el-col>

    </div>
</template>

<script>
    export default {
        name: "index",
        data () {
            return {};
        },
        methods: {
            cancel: function () {
                this.$router.push("/login");
            },
            enter: function () {
                this.$router.push("/enter");
            }
        }
    };
</script>


<style lang="scss">
    @import '~scss_vars';
    @import './style.scss';
    @import '../../styles/common.scss';
</style>
