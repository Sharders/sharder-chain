<template>
    <div class="content_enter">

        <el-col :span="24" class="tip">
            <a>{{$t('enter.enter_tip')}}</a>
        </el-col>

        <el-col :span="24" class="input">
            <el-input type="textarea" :autosize=notAutoSize v-model="passphrase"></el-input>
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
                },
                passphrase:''
            };
        },
        methods: {
            cancel: function () {
                this.$router.push("/login");
            },
            enter: function () {
                let _this = this;
                if (_this.passphrase === "") {
                    _this.$message.info(_this.$t('notification.login_no_input_error'));
                    return;
                }
                Login.login(true, _this.passphrase, _this, function () {
/*                    console.log(SSO);
                    console.log("account", SSO.account);
                    console.log("accountInfo", SSO.accountInfo);
                    console.log("accountRS", SSO.accountRS);
                    console.log("publicKey", SSO.publicKey);
                    console.log("settings", SSO.settings);
                    */
                    _this.$global.setEpochBeginning(_this).then(res=>{
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
