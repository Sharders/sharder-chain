<template>
    <div :class="this.$i18n.locale === 'en'? 'en_login' : ''">
        <div class="content_login">
            <el-radio-group v-model="tabTitle" class="title">
                <el-radio-button label="key" class="btn">
                    {{$t('login.secret_login')}}
                </el-radio-button>
                <el-radio-button label="account" class="btn">{{$t('login.account_login')}}</el-radio-button>
            </el-radio-group>
            <el-col :span="24" class="login_operation">
                <div style="font-size: x-small;color: #5daf34;text-align: left;height: 12px;">
                    <span v-if="tabTitle === 'key' && hubBind && displayBindAddr">{{$t('login.login_binding_hub_account_tip')}}{{hubBindAddress}}</span>
                </div>
                <input v-if="tabTitle === 'key'" class="account_input" type="password" v-model="secretPhrase"
                       :placeholder="$t('login.login_placeholder')"/>
                <masked-input v-if="tabTitle === 'account'" class="secret_key_input" v-model="account"
                              mask="AAA-****-****-****-*****" :placeholder="$t('login.sharder_account')"/>
                <el-button class="common_btn writeBtn" @click="loginSharder()">{{$t('login.login')}}</el-button>
            </el-col>

            <el-col :span="24">
                <img src="../../assets/img/add.svg"/>
                <a @click="register">{{$t('login.register_tip')}}</a>
            </el-col>
        </div>
    </div>
</template>

<script>
    export default {
        name: "index",
        data() {
            return {
                tabTitle: "key",
                secretPhrase: "",
                account: "SSA-____-____-____-_____",
                type: 1,
                userConfig: [],
                hubBindAddress:"",
                hubBind:false,
                displayBindAddr: true,
                hubSettingDialog: false,
                hubsetting: {
                    openPunchthrough: true,
                    sharderAccount: '',
                    sharderPwd: '',
                    address: '',
                    port: '',
                    clientSecretkey: '',
                    publicAddress: '',
                    SS_Address: '',
                    isOpenMining: false,
                    modifyMnemonicWord: '',
                    newPwd: '',
                    confirmPwd: ''
                }
            };
        },
        created() {
            const _this = this;

            console.log("Net work type is:", SSO.netWorkType);

            this.$global.getUserConfig(this).then(res => {
                _this.$store.state.isHubInit = res["sharder.HubBindAddress"] ? false : true;
                _this.$store.state.userConfig = res;
                _this.hubBind = res["sharder.HubBind"];
                _this.hubBindAddress = res["sharder.HubBindAddress"];
                _this.displayBindAddr = this.checkAndSetdisplayBindAddr();
                _this.autoLogin(res);
            });

            SSO.init();
        },
        methods: {
            checkAndSetdisplayBindAddr(){
                const _this = this;
                if(_this.$store.state.userConfig["sharder.myAddress"]) {
                    if(_this.$store.state.userConfig["sharder.myAddress"].indexOf("sharder.io") != -1
                    || _this.$store.state.userConfig["sharder.myAddress"].indexOf("sharder.org") != -1) {
                        return false;
                    }
                }
                return true
            },
            autoLogin(val) {
                if (val["sharder.login.mode"] !== "auto" || sessionStorage.getItem("sharder.login.mode") === "manual") return;
                let _this = this;
                SSO.secretPhrase = val["sharder.login.sp"] || "faint color remember innocence drum object frighten show lace happy apologize sunset";
                Login.login(1, SSO.secretPhrase, _this, function () {
                    _this.$global.setEpochBeginning(_this).then(res => {
                        _this.$store.state.isLogin = true;
                        _this.$router.push(val["sharder.login.to"] || "/network");
                    });
                });
            },
            checkSharder() {
                const _this = this;
                let formData = new FormData();
                if (_this.hubsetting.sharderAccount !== '' && _this.hubsetting.sharderPwd !== '' && _this.hubsetting.openPunchthrough) {
                    formData.append("username", _this.hubsetting.sharderAccount);
                    formData.append("password", _this.hubsetting.sharderPwd);
                    _this.$http.post('https://taskhall.sharder.org/bounties/hubDirectory/check.ss', formData).then(res => {
                        if (res.data.status === 'success') {
                            _this.hubsetting.address = res.data.data.natServiceAddress;
                            _this.hubsetting.port = res.data.data.natServicePort;
                            _this.hubsetting.clientSecretkey = res.data.data.natClientKey;
                            _this.hubsetting.publicAddress = res.data.data.hubAddress;
                            // _this.hubsetting.SS_Address = '';
                        } else if (res.data.errorType === 'unifiedUserIsNull') {
                            _this.$message.error(res.data.errorMessage);
                        } else if (res.data.errorType === 'hubDirectoryIsNull') {
                            _this.$message.error(_this.$t('notification.hubsetting_sharder_account_no_permission'));
                        }
                    })
                }
            },
            register: function () {
                this.$store.state.mask = false;
                this.$router.push("/register");
            },
            languageChange: function (language) {
                console.log(language);
                this.label_width = '200px'
            },
            loginSharder() {
                let _this = this;
                if (!_this.validationInfo()) return;
                if (_this.tabTitle === "account") {
                    _this.$global.fetch("GET", {account: _this.account}, "getAccount").then(res => {
                        if (res.errorDescription) {
                            return _this.$message.error(_this.$t("login.no_found_account"));
                        }
                        Login.login(0, _this.account, _this, function () {
                            _this.$global.setEpochBeginning(_this).then(res => {
                                _this.$store.state.isLogin = true;
                                _this.$router.push("/account");
                            });
                        });
                    })
                } else if (_this.tabTitle === "key") {
                    SSO.secretPhrase = _this.secretPhrase;
                    Login.login(1, _this.secretPhrase, _this, function () {
                        _this.$global.setEpochBeginning(_this).then(res => {
                            _this.$store.state.isLogin = true;
                            _this.$router.push("/account");
                        });
                    });
                }
            },
            validationInfo() {
                let _this = this;
                if (_this.tabTitle === "key") {
                    if (!_this.secretPhrase) {
                        _this.$message.warning(_this.$t("password_modal.input_tip"));
                        return false;
                    }
                    if (_this.secretPhrase.replace(/\s*/g, "").length < 50) {
                        _this.$message.warning(_this.$t("password_modal.input_tip_length"));
                        return false;
                    }
                    return true;
                }
                if (_this.tabTitle === "account") {
                    if (!_this.account) {
                        _this.$message.warning(_this.$t("password_modal.input_account"));
                        return false;
                    }
                    if (!_this.account.toUpperCase().match(/^(SSA)-([A-Z0-9]{4})-([A-Z0-9]{4})-([A-Z0-9]{4})-([A-Z0-9]{5})/)) {
                        _this.$message.warning(_this.$t("password_modal.account_error"));
                        return false;
                    }
                    return true;
                }
            }
        }
    };
</script>

<style>
    .content_login .title .el-radio-button__orig-radio:checked + .el-radio-button__inner {
        background-color: #3fb09a;
        border-color: #3fb09a;
    }

    .content_login .title .el-radio-button__inner:hover {
        color: #3fb09a;
    }

    .content_login .title .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
        color: #fff;
    }

    .content_login .title {
        display: block !important;
        text-align: left;
    }

    .content_welcome .welcome_info {
        text-align: center;
        font-size: 24px;
        font-weight: bold;
        color: #3fb09a;
        margin-top: 40px;
    }

    .content_welcome .welcome_main {
        text-align: center;
        margin-top: 30px;
    }

    .content_welcome .welcome_main .init_hub_btn {
        width: 400px;
        height: 40px;
        background: #3fb09a;
        border: none;
        color: #fff;
        font-size: 18px;
        border-radius: 20px;
        outline: none;
    }

    .content_welcome .welcome_main .init_hub_btn:hover {
        background: #fff;
        color: #3fb09a;
        border: 1px solid #3fb09a;
        transition: .4s;
    }

    /*.en_login .modal_hubSetting .modal-body{*/

    /*}*/
</style>
<style lang="scss">
    @import './style.scss';
    @import '../../styles/css/common.scss';
</style>
