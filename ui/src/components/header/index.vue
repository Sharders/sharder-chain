<template>
    <header class="header">
        <div class="header_content">
            <div id="logo">
                <a href="#" class="logo">
                    <img src="../../assets/img/logo.svg"/>
                    <div>
                        <span>Sharder</span>
                        <span>COS 版本：{{blockchainState.fullVersion}}</span>
                    </div>
                </a>
            </div>
            <nav class="navbar_main" role="navigation">
                <el-menu class="navbar_left el-menu-demo" mode="horizontal" :router=isRouter @select="activeItem">
                    <el-menu-item index="/account" :class="this.$route.path.indexOf('/account') >= 0 ? 'activeLi' : ''">账户</el-menu-item>
                    <el-menu-item index="/network" :class="this.$route.path.indexOf('/network') >= 0 ? 'activeLi' : ''">网络</el-menu-item>
                    <el-menu-item index="/mining" :class="this.$route.path.indexOf('/mining') >= 0 ? 'activeLi' : ''">矿池</el-menu-item>
                    <!--<el-menu-item index="/1" :class="activeIndex === '/1' ? 'activeLi' : ''">
                        <img src="../../assets/console.svg">
                    </el-menu-item>-->
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
                        <span v-if="typeof(secretPhrase) === 'undefined'">{{accountRS}} | 观察模式</span>
                        <span class="isLogin" v-else>{{accountRS}} | 私钥模式</span>
                    </div>
                    <div class="navbar_pilotLamp">

                        <el-tooltip class="item" content="您不能挖矿，因为您的帐户还没有公钥。请完成一次交易或则使用密钥重新登录。" placement="bottom" effect="light" v-if="accountInfo.errorDescription === 'Unknown account'">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item" content="您的有效余额不足，不能挖矿。需要满足:有效余额经过10个区块确认并且至少达到1000SS。" placement="bottom" effect="light" v-else-if="accountInfo.effectiveBalanceSS === 0">
                            <div class="pilotLamp_circle notForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" content="无法确定挖矿状态，请指定管理员密码" placement="bottom" effect="light" v-else-if="typeof(secretPhrase) === 'undefined' && userConfig.SS_Address !== accountRS">
                            <div class="pilotLamp_circle unknownForging"  @click="startForging(false,'')"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" content="不能拥有多个账户在同一节点挖矿,请使用关联账户重新登陆" placement="bottom" effect="light" v-else-if="typeof(secretPhrase) !== 'undefined' && userConfig.SS_Address !== accountRS">
                            <div class="pilotLamp_circle unknownForging"></div>
                        </el-tooltip>
                        <el-tooltip class="item csp" content="未挖矿" placement="bottom" effect="light" v-else-if="forging.errorCode === 5">
                            <div class="pilotLamp_circle notForging"  @click="startForging(true,'')"></div>
                        </el-tooltip>
                        <el-tooltip class="item" content="已启动" placement="bottom" effect="light" v-else-if="!forging.errorDescription">
                            <div class="pilotLamp_circle"></div>
                        </el-tooltip>

                    </div>
                    <div class="navbar_exit">
                        <span class="csp" @click="exit"><a>退出</a></span>
                    </div>
                    <div class="navbar_lang">
                        <!--<button>语言&nbsp;<span class="triangle "></span></button>-->
                        <el-select v-model="selectLan" placeholder="语言">
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
                        <h4 class="modal-title">开启挖矿</h4>
                    </div>
                    <div class="modal-body modal-peer">
                        <p>管理密码</p>
                        <input v-model="adminPassword" type="password"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" @click="startForging(false,adminPassword)">开启</button>
                    </div>
                </div>
            </div>
        </div>
    </header>

</template>

<script>
    import Store from "../../store";
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
                placeholder: "搜索",
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
                selectLan:'语言',
                language:[{
                    value:'zh-cn',
                    label:'中文简体'
                },{
                    value:'zh-tw',
                    label:'中文繁体'
                },{
                    value:'en',
                    label:'Engligh'
                },{
                    value:'ja',
                    label:'日本語'
                },{
                    value:'de',
                    label:'Deutsch'
                }]

            };
        },
        created(){
            const _this = this;
            this.getData();
            this.$http.get("/sharder?requestType=getAccount",{
                params: {
                    includeEffectiveBalance:true,
                    account:SSO.account
                }
            }).then(res=>{
                _this.accountInfo = res.data;
                console.log("accountInfo",_this.accountInfo);
            }).catch(err=>{
                _this.$message.error(err);
                console.error(err);
            });
            _this.$global.getUserConfig(_this).then(res=>{
                _this.userConfig = res;
            });

            let config = {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            };
            let formData = new FormData();
            formData.append("secretPhrase",_this.secretPhrase);
            this.$http.post("/sharder?requestType=getForging",formData,config
            ).then(res=>{
                _this.forging = res.data;
                console.log("forging",_this.forging);
            }).catch(err=>{
                _this.$message.error(err);
                console.error(err);
            });

            // console.log("accountInfo",accountInfo);

        },
        mounted(){
            setInterval(this.getData(),30000);
        },
        methods: {
            getData:function(){
                const _this = this;
                _this.$global.setBlockchainState(_this).then(res=>{
                    _this.blockchainState = res;
                    console.log(_this.$global.isOpenConsole);
                    if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getBlockchainStatus",'GET',res);
                    }
                });
                _this.$global.setUnconfirmedTransactions(_this,SSO.account).then(res=>{
                    Store.commit("setUnconfirmedNotificationsList",res.unconfirmedTransactions);
                    console.log("接收unconfirmedTransactionsList",res.unconfirmedTransactions);
                    if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getUnconfirmedTransactions",'GET',res);
                    }
                });
                _this.$global.setPeers(_this).then(res=>{
                    if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getPeers",'GET',res);
                    }
                });
            },
            startForging:function(b,pwd){
                const _this = this;
                if(b){
                    _this.$http.post("/sharder?requestType=startForging").then(res=>{
                        if(!res.data.errorDescription){
                            console.log("开启挖矿",res.data);
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
                    _this.$http.post("/sharder?requestType=startForging",{
                        secretPhrase:pwd
                    }).then(res=>{
                        if(!res.data.errorDescription){
                            console.log("开启挖矿",res.data);
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
                $(_this.$global.newConsole.document.body).html("<div style='position:fixed;top:0;left:0;right:0;padding:5px;background:#efefef;color:black;'>打开控制台。日志记录开始......<div style='float:right;text-decoration:underline;color:blue;font-weight:bold;cursor:pointer;' onclick='document.getElementById(\"console\").innerHTML=\"\"'>clear</div></div><div id='console'></div>");

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
                _this.placeholder = "输入账户ID/交易ID/区块ID进行搜索";
            },
            search_blur: function () {
                const _this = this;
                if (_this.search_val === "") {
                    _this.activeSearch = false;
                    _this.placeholder = "搜索";
                }
            },
            search_keydown: function () {
                const _this = this;
                if(_this.search_val !== ""){
                    _this.isSearch = true;
                }else{
                    _this.$message({
                        showClose: true,
                        message: "搜索框不能为空",
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
                // _this.search_val = "";
                _this.isSearch = false;
            }
        },
        watch:{
            blockchainState:function (res) {
                console.log(res);
            }
        }
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
</style>
