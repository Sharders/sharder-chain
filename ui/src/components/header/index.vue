<template>
    <header class="header">
        <div class="header_content">
            <div id="logo">
                <a href="#" class="logo">
                    <img src="../../assets/img/logo.svg"/>
                    <div>
                        <span>Sharder</span>
                        <span>{{blockchainState.application}}{{$t('header.version')}}{{blockchainState.fullVersion}}</span>
                    </div>
                </a>
            </div>
            <nav class="navbar_main" role="navigation">
                <el-menu class="navbar_left el-menu-demo" :class="this.$i18n.locale === 'en'? 'en_menu' : ''" mode="horizontal" :router=isRouter @select="activeItem">
                    <el-menu-item index="/account" :class="this.$route.path.indexOf('/account') >= 0 ? 'activeLi' : ''">{{$t('header.account')}}</el-menu-item>
                    <el-menu-item index="/network" :class="this.$route.path.indexOf('/network') >= 0 ? 'activeLi' : ''">{{$t('header.network')}}</el-menu-item>
                    <el-menu-item index="/mining" :class="this.$route.path.indexOf('/mining') >= 0 ? 'activeLi' : ''">{{$t('header.mining')}}</el-menu-item>
                </el-menu>
                <div class="navbar_console">
                    <el-button type="text" @click="goConsole">
                        <span class="console"></span>
                    </el-button>
                </div>
                <div class="navbar_search">
                    <div>
                        <input class="navbar_search_input" :class="activeSearch ? 'navbar_search_input_active' : ''"
                               :placeholder="placeholder" type="text" name="search" v-model="search_val"
                               @focus="search_focus" @blur="search_blur" @keyup.enter="search_keydown"/>
                        <img src="../../assets/img/search.svg" @click="search_keydown"/>
                    </div>
                </div>
                <div class="navbar_right">
                    <div class="navbar_status">
                        <span v-if="typeof(secretPhrase) === 'undefined'">{{accountRS}} | {{$t('header.observation_mode')}}</span>
                        <span class="isLogin" v-else>{{accountRS}} | {{$t('header.secret_mode')}}</span>
                    </div>
                    <div class="navbar_pilotLamp">
                        <el-tooltip class="item csp" content="请先初始化Hub" placement="bottom" effect="light" v-if="isHubInit">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_new_account')" placement="bottom" effect="light" v-else-if="accountInfo.errorDescription === 'Unknown account'">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_effective_balance')" placement="bottom" effect="light" v-else-if="accountInfo.effectiveBalanceSS === 0">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_no_admin_password')" placement="bottom" effect="light" v-else-if="typeof(secretPhrase) === 'undefined' && userConfig['sharder.HubBindAddress'] !== accountRS">
                            <div class="pilotLamp_circle unknownForging"  @click="startForging(false,'')"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.forging_error_exceeds_account_volume')" placement="bottom" effect="light" v-else-if="typeof(secretPhrase) !== 'undefined' && userConfig['sharder.HubBindAddress']  !== accountRS">
                            <div class="pilotLamp_circle unknownForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" :content="$t('header.no_forging')" placement="bottom" effect="light" v-else-if="forging.errorCode === 5">
                            <div class="pilotLamp_circle notForging"  @click="startForging(true,'')"></div>
                        </el-tooltip>
                        <el-tooltip class="item" :content="$t('header.started_forging')" placement="bottom" effect="light" v-else-if="!forging.errorDescription">
                            <div class="pilotLamp_circle"></div>
                        </el-tooltip>

                    </div>
                    <div class="navbar_exit">
                        <span class="csp" @click="exit"><a>{{$t('header.exit')}}</a></span>
                    </div>
                    <div class="navbar_lang">
                        <el-select v-model="selectLan">
                            <el-option
                                v-for="item in language"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value">
                            </el-option>
                        </el-select>
                    </div>
                </div>
            </nav>
        </div>
        <dialogCommon :searchValue="search_val" :isSearch="isSearch" @isClose="isClose"></dialogCommon>

        <div class="modal" id="start_forging_modal" v-show="startForgingDialog">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button class="close" @click="closeDialog">X</button>
                        <h4 class="modal-title">{{$t('header.start_forging')}}</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>{{$t('header.admin_password')}}</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="startForging(false,adminPassword)">{{$t('header.starting_forging')}}</button>
                    </div>
                </div>
            </div>
        </div>
    </header>

</template>

<script>
    import dialogCommon from "../../views/dialog/dialog_common";
    export default {
        name: "Header",
        components: {dialogCommon},
        props: ["openSidebar", "title"],
        data () {
            return {
                startForgingDialog:false,
                activeIndex: "/account",
                isRouter: true,
                placeholder: this.$t('header.search'),
                activeSearch: false,
                blockchainState:this.$global.blockchainState,
                secretPhrase:SSO.secretPhrase,
                adminPassword:'',
                accountRS:SSO.accountRS,
                accountInfo:[],
                forging:[],
                userConfig:[],
                search_val: "",
                isSearch:false,
                isHubInit:this.$store.state.isHubInit,
                selectLan:'',
                selectLanValue:'',
                language:[{
                    value:'cn',
                    label:'简体中文'
                },{
                    value:'en',
                    label:'English'
                }],
                i:0,
            };
        },
        created(){
            const _this = this;
            let lang = this.$i18n.locale;
            if(typeof lang !== 'undefined'){

                for(let i=0;i<_this.language.length;i++){
                    if(_this.language[i].value === lang){
                        _this.selectLan = _this.language[i].label;
                        _this.selectLanValue = _this.language[i].value;
                    }
                }
            }else{
                _this.selectLan = _this.language[value === 'cn'].label;
                _this.selectLanValue = _this.language[value === 'cn'].value;
            }



            this.getData();
            this.$http.get("/sharder?requestType=getAccount",{
                params: {
                    includeEffectiveBalance:true,
                    account:SSO.account
                }
            }).then(res=>{
                _this.accountInfo = res.data;
                // console.log("accountInfo",_this.accountInfo);
            }).catch(err=>{
                _this.$message.error(err);
                console.error(err);
            });
            _this.$global.getUserConfig(_this).then(res=>{
                _this.userConfig = res;

                console.log("accountRS",_this.accountRS);
                console.log("userConfig",_this.userConfig);
            });

            let formData = new FormData();
            formData.append("secretPhrase",_this.secretPhrase);
            let config = {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            };
            _this.$http.post('/sharder?requestType=getForging',formData,config).then(res=>{
                _this.forging = res.data;
                // console.log("forging",_this.forging);
            }).catch(err=>{
                _this.$message.error(err);
                console.error(err);
            });
        },
        mounted(){
            let _this = this;
            setInterval(()=>{
                _this.getData();
            },30000);
        },
        methods: {
            getData:function(){
                const _this = this;
                // if(_this.i%30 === 0){
                    _this.$global.setBlockchainState(_this).then(res=>{
                        _this.blockchainState = res;
                        if(_this.$global.isOpenConsole){
                            _this.$global.addToConsole("/sharder?requestType=getBlockchainStatus",'GET',res);
                        }
                    });
                    _this.$global.setUnconfirmedTransactions(_this,SSO.account).then(res=>{
                        _this.$store.state.unconfirmedTransactionsList = res;
                        if(_this.$global.isOpenConsole){
                            _this.$global.addToConsole("/sharder?requestType=getUnconfirmedTransactions",'GET',res);
                        }
                    });
                    _this.$global.setPeers(_this).then(res=>{
                        if(_this.$global.isOpenConsole){
                            _this.$global.addToConsole("/sharder?requestType=getPeers",'GET',res);
                        }
                    });
                // }
            },
            startForging:function(b,pwd){
                const _this = this;
                let formData = new FormData();
                let config = {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                };
                if(b){
                    formData.append("secretPhrase",SSO.secretPhrase);
                    _this.$http.post("/sharder?requestType=startForging",formData,config).then(res=>{
                        if(!res.data.errorDescription){
                            _this.$http.post('/sharder?requestType=getForging',formData,config).then(res=>{
                                _this.forging = res.data;
                                // console.log("forging",_this.forging);
                            }).catch(err=>{
                                _this.$message.error(err);
                                console.error(err);
                            });

                        }else{
                            _this.$message.error(res.data.errorDescription);
                            console.error(res.data.errorDescription);
                        }
                    }).catch(err=>{
                        _this.$message.error(err);
                        console.error(err);
                    });
                }else if(b === false&&pwd === ''){
                    _this.startForgingDialog = true;
                    _this.$store.state.mask = true;
                }else{
                    formData.append("secretPhrase",pwd);
                    _this.$http.post("/sharder?requestType=startForging",formData,config).then(res=>{
                        if(!res.data.errorDescription){
                            _this.$http.post('/sharder?requestType=getForging',formData,config).then(res=>{
                                _this.forging = res.data;
                                // console.log("forging",_this.forging);
                            }).catch(err=>{
                                _this.$message.error(err);
                                console.error(err);
                            });
                        }else{
                            _this.$message.error(res.data.errorDescription);
                            console.error(res.data.errorDescription);
                        }
                    }).catch(err=>{
                        _this.$message.error(err);
                        console.error(err);
                    });
                    closeDialog();
                }
            },
            activeItem: function (val) {
                const _this = this;
                _this.activeIndex = val;
            },
            goConsole: function () {
                const _this = this;
                _this.$global.newConsole = window.open("", "console", "width=750,height=400,menubar=no,scrollbars=yes,status=no,toolbar=no,resizable=yes");
                $(_this.$global.newConsole.document.head).html("<title>CONSOLE</title><style type='text/css'>body { background:black; color:white; font-family:courier-new,courier;font-size:14px; } pre { font-size:14px; } #console { padding-top:15px; }</style>");
                $(_this.$global.newConsole.document.body).html("<div style='position:fixed;top:0;left:0;right:0;padding:5px;background:#efefef;color:black;'>"+_this.$t('header.open_console')+"<div style='float:right;text-decoration:underline;color:blue;font-weight:bold;cursor:pointer;' onclick='document.getElementById(\"console\").innerHTML=\"\"'>clear</div></div><div id='console'></div>");

                let loop = setInterval(function() {
                    if(_this.$global.newConsole.closed) {
                        clearInterval(loop);
                        _this.$global.isOpenConsole = false;
                    }
                }, 1000);
                this.$global.isOpenConsole = true;
            },
            search_focus: function () {
                const _this = this;
                _this.activeSearch = true;
                _this.placeholder = _this.$t('header.search_open');
            },
            search_blur: function () {
                const _this = this;
                if (_this.search_val === "") {
                    _this.activeSearch = false;
                    _this.placeholder = _this.$t('header.search');
                }
            },
            search_keydown: function () {
                const _this = this;
                if(_this.search_val !== ""){
                    _this.isSearch = true;

                }else{
                    _this.$message({
                        showClose: true,
                        message: _this.$t('notification.search_no_null_error'),
                        type: "error"
                    });
                }
            },
            closeDialog:function(){
                this.startForgingDialog = false;
                this.$store.state.mask = false;
            },
            exit:function () {
               const _this = this;
               _this.$store.state.isLogin = false;
               _this.$router.push("/login");
            },
            isClose:function () {
                const _this = this;
                _this.isSearch = false;

            },
        },
        watch:{
            blockchainState:function (res) {
                // console.log(res);
            },
            selectLan:function (language) {
                const _this = this;
                for(let i=0;i<_this.language.length;i++){
                    if(_this.language[i].value === language){
                        _this.$i18n.locale = language;
                        _this.$store.commit('updateLang',language);
                        _this.selectLanValue = language;
                    }
                }
            }
        },
    };
</script>
<style lang="scss" type="text/scss">
    /* You can import all your SCSS variables using webpack alias*/
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
<style scoped  lang="scss" type="text/scss">
    .el-select-dropdown{
        .el-select-dropdown__item.selected{
            background-color: #493eda!important;
            color: #fff!important;
        }
        .el-select-dropdown__item.selected.hover{
            background-color: #493eda!important;
            color: #fff!important;
        }
    }
    .en_menu{
        .el-menu-item {
            font-size: 12px!important;
        }
    }
</style>
