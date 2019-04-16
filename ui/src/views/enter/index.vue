<template>
    <div class="content_enter">

        <el-col :span="24" class="tip">
            <a>{{$t('enter.enter_tip')}}</a>
        </el-col>

        <el-col :span="24" class="input">
            <el-input type="textarea" :autosize=notAutoSize v-model="confirmPassphrase"></el-input>
        </el-col>

        <el-col :span="24" style="margin-top: 10px;">
            <el-button class="common_btn_contrary medium" @click="cancel">{{$t('enter.enter_cancel')}}</el-button>
            <el-button class="common_btn medium" @click="enter">{{$t('enter.enter_client')}}</el-button>
        </el-col>

    </div>
</template>

<script>

    export default {
        name: "index",
        data() {
            return {
                notAutoSize: {
                    minRows: 3,
                    maxRows: 4
                },
                passphrase: this.$route.params.passPhrase,
                confirmPassphrase: ''
            };
        },
        created: function () {
        },
        methods: {
            cancel: function () {
                this.$router.push("/login");
            },
            enter: function () {
                let _this = this;
                if (_this.confirmPassphrase === "") {
                    return _this.$message.info(_this.$t('notification.login_no_input_error'));
                }
                if (_this.confirmPassphrase !== _this.passphrase) {
                    return _this.$message.error(_this.$t('login.incorrect_key'));
                }
                SSO.secretPhrase = _this.passphrase;
                Login.login(1, _this.passphrase, _this, function () {
                    _this.$global.setEpochBeginning(_this).then(res => {
                        _this.$store.state.isLogin = true;
                        _this.$router.push("/account");

                    });
                });
            }
        }
    };
</script>

<style lang="scss">
    @import './style.scss';
    @import '../../styles/css/common.scss';
</style>
