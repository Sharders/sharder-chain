<template>
    <div>
        <div class="content_login" v-if="typeof userConfig['sharder.HubBindAddress'] !== 'undefined'">
            <el-radio-group v-model="tabTitle" class="title">
                <el-radio-button label="key" class="btn">密钥登录</el-radio-button>
                <el-radio-button label="account" class="btn">账户登录</el-radio-button>
            </el-radio-group>

            <el-col :span="24" class="login_operation">
                <input v-if="tabTitle === 'key'" class="account_input" type="password" v-model="secretPhrase"
                       placeholder="请输入账户密钥"/>
                <masked-input v-if="tabTitle === 'account'" class="secret_key_input" v-model="account"
                              mask="AAA-****-****-****-*****" placeholder="Sharder账户" />
                <el-button class="common_btn" @click="login">登录</el-button>
            </el-col>

            <el-col :span="24">
                <img src="../../assets/create_account.png"/>
                <a @click="register">没有账户? 创建账户</a>
            </el-col>
        </div>
        <div class="content_welcome" v-else>  <!--Hub初始化-->
            <el-col :span="24" class="welcome_info">
                <p>欢迎来到豆匣链</p>
            </el-col>
            <el-col :span="24" class="welcome_main">
                <button class="init_hub_btn" @click="initHub">初始化Hub</button>
            </el-col>
        </div>

        <div class="modal_hubSetting" id="hub_setting" v-show="hubSettingDialog">
            <div class="modal-header">
                <h4 class="modal-title">
                    <span>初始化HUB</span>
                </h4>
            </div>
            <div class="modal-body">
                <el-form label-position="left" label-width="160px">
                    <el-form-item label="启动内网穿透服务:">
                        <el-checkbox v-model="hubsetting.openPunchthrough"></el-checkbox>
                    </el-form-item>
                    <el-form-item label="Sharder官网账户:">
                        <el-input v-model="hubsetting.sharderAccount"></el-input>
                    </el-form-item>
                    <el-form-item label="Sharder官网密码:" >
                        <el-input v-model="hubsetting.sharderPwd" @blur="checkSharder"></el-input>
                    </el-form-item>
                    <el-form-item label="穿透服务地址:" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.address" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item label="穿透服务端口:" v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.port" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item label="穿透服务客户端秘钥:"  v-if="hubsetting.openPunchthrough">
                        <el-input v-model="hubsetting.clientSecretkey" :disabled="true"></el-input>
                    </el-form-item>
                    <el-form-item label="公网地址:">
                        <el-input v-model="hubsetting.publicAddress" :disabled="hubsetting.openPunchthrough"></el-input>
                    </el-form-item>
                    <el-form-item label="关联SS地址:">
                        <el-input v-model="hubsetting.SS_Address"></el-input>
                    </el-form-item>
                    <el-form-item label="是否开启挖矿:">
                        <el-checkbox v-model="hubsetting.isOpenMining"></el-checkbox>
                    </el-form-item>
                    <el-form-item label="绑定助记词:" v-if="hubsetting.isOpenMining">
                        <el-input v-model="hubsetting.modifyMnemonicWord"></el-input>
                    </el-form-item>
                    <el-form-item label="初始化管理员密码:">
                        <el-input v-model="hubsetting.newPwd"></el-input>
                    </el-form-item>
                    <el-form-item label="确认管理员密码:">
                        <el-input v-model="hubsetting.confirmPwd"></el-input>
                    </el-form-item>
                </el-form>
                <div class="footer-btn">
                    <button class="common_btn" @click="verifyHubSetting">确认后重启</button>
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
                secretPhrase: "trail shape agree haunt distant attention perhaps skill admit deny week remind",
                account: "SSA-ENRV-X8TW-UQND-AFA7W",
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
            this.$global.getUserConfig(this).then(res=>{
                if(typeof res["sharder.HubBindAddress"] !== 'undefined'){
                    _this.$store.state.userConfig = res;
                    _this.userConfig = res;
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
                    console.log("___________________________________");
                    _this.$http.post('https://taskhall.sharder.org/bounties/hubDirectory/check.ss',formData).then(res=>{
                        if(res.data.status === 'success'){
                            _this.hubsetting.address = res.data.data.natServiceAddress;
                            _this.hubsetting.port = res.data.data.natServicePort;
                            _this.hubsetting.clientSecretkey = res.data.data.natClientKey;
                            _this.hubsetting.publicAddress = res.data.data.hubAddress;
                            _this.hubsetting.SS_Address = '';
                        }else if(res.data.errorType === 'unifiedUserIsNull'){
                            _this.$message.error(res.data.errorMessage);
                        }else if(res.data.errorType === 'hubDirectoryIsNull'){
                            _this.$message.error('暂无配置，请联系管理员');
                        }
                    })
                }
            },
            initHub:function(){
                this.$store.state.mask = true;
                this.hubSettingDialog = true;
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
                        _this.$message.success('请稍后再次打开页面');
                        _this.hubSettingDialog = false;
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
                            _this.$message.error("请输入Sharder账号获取HUB配置信息");
                        else
                            _this.$message.error("请联系管理员获取Hub设置");
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
                        _this.$message.warning('关联SS地址格式错误！');
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
                        _this.$message.warning('开启矿池必须填写助记词！');
                        return false;
                    }
                    params.append("sharder.HubBindPassPhrase",_this.hubsetting.modifyMnemonicWord);
                }else{
                    params.append("sharder.HubBind",false);
                }

               /* if(_this.hubsetting.isOpenMining){
                    params.append("sharder.HubBind",true);
                    if(_this.hubsetting.SS_Address !== ''){
                        const pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/;
                        if(!_this.hubsetting.SS_Address.toUpperCase().match(pattern)){
                            _this.$message.warning('关联SS地址格式错误！');
                            return false;
                        }else{
                            params.append("sharder.HubBindAddress",_this.hubsetting.SS_Address);
                        }
                    }
                    if(_this.hubsetting.modifyMnemonicWord !== '')
                        params.append("sharder.HubBindPassPhrase",_this.hubsetting.modifyMnemonicWord);
                }else{
                    params.append("sharder.HubBind",false);
                }*/
                params.append("restart",false);
                params.append("sharder.disableAdminPassword",false);

                if(_this.hubsetting.newPwd !== "" || _this.hubsetting.confirmPwd !== ""){
                    if(_this.hubsetting.newPwd !== _this.hubsetting.confirmPwd){
                        _this.$message.warning("密码不一致！");
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
                    _this.$message.info("请输入账号或私钥");
                    return;
                }
                Login.login(_this.type, val, _this, function () {
                    console.log(SSO);
                    // console.log("account", SSO.account);
                    // console.log("accountInfo", SSO.accountInfo);
                    // console.log("accountRS", SSO.accountRS);
                    // console.log("publicKey", SSO.publicKey);
                    // console.log("settings", SSO.settings);
                    _this.$global.setEpochBeginning(_this).then(res=>{
                        console.log("初试时间："+res);
                        _this.$store.state.isLogin = true;
                        _this.$router.push("/account");
                    });
                });

                // if (this.type && $.trim(this.account)) {
                //     this.$store.state.isPassphrase = true;
                //     this.$store.state.passphrase = this.account;
                // } else {
                //     this.$store.state.isPassphrase = false;
                //     this.$store.state.passphrase = "";
                // }
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
</style>
<style lang="scss">
    @import './style.scss';
    @import '../../styles/css/common.scss';
</style>
