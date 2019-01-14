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
                <el-button class="common_btn" @click="login">{{$t('login.login')}}</el-button>
            </el-col>

            <el-col :span="24">
                <img src="../../assets/img/create_account.png"/>
                <a @click="register">{{$t('login.register_tip')}}</a>
            </el-col>
        </div>
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

        <div class="modal_hubSetting" id="hub_setting" v-show="hubSettingDialog">
            <div class="modal-header">
                <h4 class="modal-title">
                    <span>{{$t('login.init_hub')}}</span>
                </h4>
            </div>
            <div class="modal-body">
                <el-form label-position="left" :label-width="this.$i18n.locale === 'en'? '200px':'160px'">
                    <el-form-item :label="$t('hubsetting.enable_nat_traversal')">
                        <el-checkbox v-model="hubsetting.openPunchthrough"></el-checkbox>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.sharder_account')">
                        <el-input v-model="hubsetting.sharderAccount"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.sharder_account_password')">
                        <el-input type="password" v-model="hubsetting.sharderPwd" @blur="checkSharder"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.nat_traversal_address')" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.address" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.nat_traversal_port')" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.port" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.nat_traversal_clent_privateKey')"  v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.clientSecretkey" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.public_ip_address')">
                        <el-input v-model="hubsetting.publicAddress" :disabled="hubsetting.openPunchthrough"></el-input>
                    </el-form-item>
                    <el-form-item class="create_account" :label="$t('hubsetting.token_address')">
                        <el-input  v-model="hubsetting.SS_Address"></el-input>
                        <a @click="register"><span>创建账户</span></a>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.enable_auto_mining')">
                        <el-checkbox v-model="hubsetting.isOpenMining"></el-checkbox>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.set_mnemonic_phrase')" v-if="hubsetting.isOpenMining">
                        <el-input type="password" v-model="hubsetting.modifyMnemonicWord"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.set_password')">
                        <el-input type="password" v-model="hubsetting.newPwd"></el-input>
                    </el-form-item>
                    <el-form-item :label="$t('hubsetting.confirm_password')">
                        <el-input type="password" v-model="hubsetting.confirmPwd"></el-input>
                    </el-form-item>
                </el-form>
                <div class="footer-btn">
                    <button class="common_btn" @click="verifyHubSetting">{{$t('hubsetting.confirm_restart')}}</button>
                    <button class="common_btn" @click="closeHub">{{$t('hubsetting.cancel')}}</button>
                </div>
            </div>
        </div>

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
                },
            };
        },
        created() {
            const _this = this;

            let url = window.location.href;
            let pathname = window.location.pathname;
            console.log("~~~~~~~~~~~~~~~~~~~~~~", url.split(pathname)[0]);



            // this.$global.getUserConfig(this).then(res=>{
            //     if(typeof res["sharder.HubBindAddress"] !== 'undefined'){
            //         _this.$store.state.userConfig = res;
            //         _this.userConfig = res;
            //         _this.$store.state.isHubInit = true;
            //     }else{
            //         if(_this.$route.query.info === 'register2Init'){
            //             _this.hubSettingDialog = true;
            //             _this.$store.state.mask = true;
            //             _this.hubsetting.SS_Address = SSO.accountRS;
            //         }
            //     }
            // });

            this.$global.setBlockchainState(this).then(res=>{
                if(typeof res.data.errorDescription === 'undefined'){
                    _this.$message.error(res.data)
                }else{
                    //TODO  获取区块链
                }
            });
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
            initHub:function(){
                this.$store.state.mask = true;
                this.hubSettingDialog = true;
            },
            closeHub:function(){
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
            },
            verifyHubSetting:function(){
                const _this = this;
                let params = _this.verifyHubSettingInfo();
                if(params === false){
                    return;
                }else{
                    params.append("isInit",true);
                }
                this.$http.post('/sharder?requestType=reConfig', params).then(res => {
                    if(typeof res.data.errorDescription === 'undefined'){
                        _this.$message.success(_this.$t('notification.restart_success'));
                        _this.hubSettingDialog = false;
                        this.$router.push("/login");
                    }else{
                        _this.$message.error(res.data.errorDescription);
                    }
                }).catch(err => {
                    _this.$message.error(err);
                });
            },
            verifyHubSettingInfo(){
                const _this = this;
                let params = new FormData();

                if(_this.hubsetting.openPunchthrough){
                    params.append("sharder.useNATService",true);
                    if(_this.hubsetting.address === '' ||
                        _this.hubsetting.port === '' ||
                        _this.hubsetting.clientSecretkey === ''){
                        if(_this.hubsetting.sharderPwd === '')
                            _this.$message.error(_this.$t('notification.hubsetting_no_sharder_account'));
                        else
                            _this.$message.error(_this.$t('notification.hubsetting_sharder_account_no_permission'));
                        return false;
                    }else{
                        params.append("sharder.NATServiceAddress",_this.hubsetting.address);
                        params.append("sharder.NATServicePort",_this.hubsetting.port);
                        params.append("sharder.NATClientKey",_this.hubsetting.clientSecretkey);
                        params.append("sharder.myAddress", _this.hubsetting.publicAddress);
                    }
                }else{
                    params.append("sharder.useNATService",false);
                }

                if(_this.hubsetting.SS_Address !== ''){
                    const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                    if(!_this.hubsetting.SS_Address.toUpperCase().match(pattern)){
                        _this.$message.warning(_this.$t('notification.hubsetting_account_address_error_format'));
                        return false;
                    }else{
                        params.append("sharder.HubBindAddress",_this.hubsetting.SS_Address);
                        params.append("reBind",true);
                    }
                }else{
                    params.append("reBind",false);
                }

                if(_this.hubsetting.isOpenMining){
                    params.append("sharder.HubBind",true);
                    if(_this.hubsetting.modifyMnemonicWord === ''){
                        _this.$message.warning(_this.$t('notification.hubsetting_no_mnemonic_word'));
                        return false;
                    }
                    params.append("sharder.HubBindPassPhrase",_this.hubsetting.modifyMnemonicWord);
                }else{
                    params.append("sharder.HubBind",false);
                }
                params.append("restart",false);
                params.append("sharder.disableAdminPassword",false);

                if(_this.hubsetting.newPwd !== "" || _this.hubsetting.confirmPwd !== ""){
                    if(_this.hubsetting.newPwd !== _this.hubsetting.confirmPwd){
                        _this.$message.warning(_this.$t('notification.hubsetting_inconsistent_password'));
                        return false;
                    }else{
                        params.append("newAdminPassword",_this.hubsetting.newPwd);
                    }
                }

                return params;
            },
            login: function () {
                let _this = this;
                let val = _this.getAccount();
                if (val === "") {
                    _this.$message.info(_this.$t('notification.login_no_input_error'));
                    return;
                }
                Login.login(_this.type, val, _this, function () {
                    // console.log(SSO);
                    // console.log("account", SSO.account);
                    // console.log("accountInfo", SSO.accountInfo);
                    // console.log("accountRS", SSO.accountRS);
                    // console.log("publicKey", SSO.publicKey);
                    // console.log("settings", SSO.settings);
                    _this.$global.setEpochBeginning(_this).then(res=>{
                        _this.$store.state.isLogin = true;
                        _this.$router.push("/account");
                    });
                });
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

    .modal_hubSetting{
        width: 800px!important;
    }
    .modal_hubSetting .modal-header .modal-title{
        margin: 0!important;
    }
    .modal_hubSetting .modal-body{
        padding: 20px 40px 60px!important;
    }
    .modal_hubSetting .modal-body .el-form{
        margin-top: 20px!important;
    }
    .modal_hubSetting .modal-body .el-form .create_account .el-input{
        width:450px;
    }
    .modal_hubSetting .modal-body .el-form .create_account a{
        position: absolute;
        right: 20px;
        top: 0;
        cursor: pointer;
    }
    /*.en_login .modal_hubSetting .modal-body{*/

    /*}*/
</style>
<style lang="scss">
    @import './style.scss';
    @import '../../styles/css/common.scss';
</style>
