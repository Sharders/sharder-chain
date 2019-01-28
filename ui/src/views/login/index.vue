<template>
    <div :class="this.$i18n.locale === 'en'? 'en_login' : ''">
        <div class="content_login">
            <el-radio-group v-model="tabTitle" class="title">
                <el-radio-button label="key" class="btn">{{$t('login.secret_login')}}</el-radio-button>
                <el-radio-button label="account" class="btn">{{$t('login.account_login')}}</el-radio-button>
            </el-radio-group>

            <el-col :span="24" class="login_operation">
                <input v-if="tabTitle === 'key'" class="account_input" type="password" v-model="secretPhrase"
                       :placeholder="$t('login.login_placeholder')"/>
                <masked-input v-if="tabTitle === 'account'" class="secret_key_input" v-model="account"
                              mask="AAA-****-****-****-*****" :placeholder="$t('login.sharder_account')" />
                <el-button class="common_btn writeBtn" @click="login">{{$t('login.login')}}</el-button>
            </el-col>

            <el-col :span="24">
                <img src="../../assets/img/create_account.png"/>
                <a @click="register">{{$t('login.register_tip')}}</a>
            </el-col>
        </div>
        <!--<button @click="initHub">test init</button>-->
<!--
        <div class="content_welcome" v-else>  &lt;!&ndash;Hub初始化&ndash;&gt;
            <el-col :span="24" class="welcome_info">
                <p>{{$t('login.welcome_tip')}}</p>
            </el-col>
            <el-col :span="24" class="welcome_main">
                <button class="init_hub_btn" @click="initHub">{{$t('login.init_hub')}}</button>
            </el-col>
        </div>
-->



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
                tabTitle: "key",
                secretPhrase: "finish rant princess crimson cold forward such known lace built poetry ceiling",
                account: "SSA-EF9Z-8J9G-LLHC-9VU5U",
      /*          secretPhrase: "",
                account: "SSA-____-____-____-_____",*/
                type: 1,
                userConfig:[],
                hubSettingDialog:false,
                hubsetting: {
                    openPunchthrough: true,
                    sharderAccount: '',
                    sharderPwd: '',
                    address:'',
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

            this.$global.getUserConfig(this).then(res=>{

                if(typeof res["sharder.HubBindAddress"] === 'undefined' || res["sharder.HubBindAddress"] === ""){
                    console.log("HUbSetting is undefined");
                    _this.$store.state.userConfig = res;
                    _this.$store.state.isHubInit = true;
                }else{
                    _this.$store.state.userConfig = res;
                    _this.$store.state.isHubInit = false;
                }
            });

            SSO.init();
        },
        methods: {
            getAccount() {
                if (this.tabTitle === "key") {
                    this.type = 1;
                    SSO.secretPhrase = this.secretPhrase;
                    return this.secretPhrase ? this.secretPhrase : "";
                }
                if (this.tabTitle === "account") {
                    this.type = 0;
                    return this.account ? this.account : "";
                }
            },
            checkSharder(){
                const _this = this;
                let formData = new FormData();
                if(_this.hubsetting.sharderAccount !== '' && _this.hubsetting.sharderPwd !== '' && _this.hubsetting.openPunchthrough){
                    formData.append("username",_this.hubsetting.sharderAccount);
                    formData.append("password",_this.hubsetting.sharderPwd);
                    _this.$http.post('https://taskhall.sharder.org/bounties/hubDirectory/check.ss',formData).then(res=>{
                        if(res.data.status === 'success'){
                            _this.hubsetting.address = res.data.data.natServiceAddress;
                            _this.hubsetting.port = res.data.data.natServicePort;
                            _this.hubsetting.clientSecretkey = res.data.data.natClientKey;
                            _this.hubsetting.publicAddress = res.data.data.hubAddress;
                            // _this.hubsetting.SS_Address = '';
                        }else if(res.data.errorType === 'unifiedUserIsNull'){
                            _this.$message.error(res.data.errorMessage);
                        }else if(res.data.errorType === 'hubDirectoryIsNull'){
                            _this.$message.error(_this.$t('notification.hubsetting_sharder_account_no_permission'));
                        }
                    })
                }
            },


/*
            initHub:function(){
                let _this = this;
                let formData = new FormData();
                formData.append("sharder.useNATService",true);
                formData.append("sharder.NATServiceAddress","devnat.sharder.io");
                formData.append("sharder.NATServicePort","8995");
                formData.append("sharder.NATClientKey","d4dc126cf43f41439f6b149b51891762");
                formData.append("sharder.myAddress", "devnat.sharder.io\\:8995");
                formData.append("sharder.HubBindAddress","SSA-EF9Z-8J9G-LLHC-9VU5U");
                formData.append("reBind",true);
                formData.append("sharder.HubBind",true);
                formData.append("sharder.HubBindPassPhrase","finish rant princess crimson cold forward such known lace built poetry ceiling");
                formData.append("restart",false);
                formData.append("sharder.disableAdminPassword",false);
                formData.append("newAdminPassword","hubtesttest");
                formData.append("isInit",true);


                formData.append("sharder.useNATService",true);

                this.$http.post('/sharder?requestType=reConfig', formData).then(res => {
                    console.log("test init hub,", res.data);
                }).catch(err => {
                    _this.$message.error(err);
                });
            },*/
            /*          closeHub:function(){
                          this.$store.state.mask = false;
                          this.hubSettingDialog = false;

                          this.hubsetting = {
                              openPunchthrough: true,
                              sharderAccount: '',
                              sharderPwd: '',
                              address:'',
                              port: '',
                              clientSecretkey: '',
                              publicAddress: '',
                              SS_Address: '',
                              isOpenMining: false,
                              modifyMnemonicWord: '',
                              newPwd: '',
                              confirmPwd: ''
                          };
                      },*/

            login: function () {
                let _this = this;
                let val = _this.getAccount();
                console.log(val);
                if (val === "") {
                    _this.$message.info(_this.$t('notification.login_no_input_error'));
                    return;
                }
                if(_this.tabTitle === "account"){
                    if(val === "SSA-____-____-____-_____" || val ===  "___-____-____-____-_____"){
                        _this.$message.info(_this.$t('notification.login_no_input_error'));
                        return;
                    }
                    this.$http.get('/sharder?requestType=getAccount', {
                        params: {
                            account: val,
                        }
                    }).then(function (res) {
                        if(typeof res.data.errorDescription !== 'undefined'){
                            _this.$message.error("无法找到您的帐户地址。");
                        }else{
                            Login.login(_this.type, val, _this, function () {
                                _this.$global.setEpochBeginning(_this).then(res=>{
                                    _this.$store.state.isLogin = true;
                                    _this.$router.push("/account");
                                });
                            });
                        }
                    }).catch(function (err) {
                        _this.$message.error(err);
                    });
                }
                else{
                    Login.login(_this.type, val, _this, function () {
                        _this.$global.setEpochBeginning(_this).then(res=>{
                            _this.$store.state.isLogin = true;
                            _this.$router.push("/account");
                        });
                    });
                }
            },
            register: function () {
                this.$store.state.mask = false;
                this.$router.push("/register");
            },
            languageChange: function (language) {
                console.log(language);
                this.label_width = '200px'
            }
        }
    };
</script>

<style>
    .content_login .title .el-radio-button__orig-radio:checked + .el-radio-button__inner {
        background-color: #513ac8;
        border-color: #513ac8;
    }

    .content_login .title .el-radio-button__inner:hover {
        color: #513ac8;
    }

    .content_login .title .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
        color: #fff;
    }

    .content_login .title {
        display: block !important;
        text-align: left;
    }
    .content_welcome .welcome_info{
        text-align: center;
        font-size: 24px;
        font-weight: bold;
        color: #493eda;
        margin-top: 40px;
    }
    .content_welcome .welcome_main{
        text-align: center;
        margin-top: 30px;
    }
    .content_welcome .welcome_main .init_hub_btn{
        width: 400px;
        height: 40px;
        background: #493eda;
        border: none;
        color: #fff;
        font-size: 18px;
        border-radius: 20px;
        outline: none;
    }

    .content_welcome .welcome_main .init_hub_btn:hover{
        background: #fff;
        color:#493eda;
        border: 1px solid #493eda;
        transition: .4s;
    }

    /*.en_login .modal_hubSetting .modal-body{*/

    /*}*/
</style>
<style lang="scss">
    @import './style.scss';
    @import '../../styles/css/common.scss';
</style>
