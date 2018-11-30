<template>
    <header class="header">
        <div class="header_content">
            <div id="logo">
                <a href="#" class="logo">
                    <img src="../../assets/logo.svg"/>
                    <div>
                        <span>Sharder</span>
                        <span>COS 版本：0.1.0-Alpha</span>
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
                        <img src="../../assets/search.svg" @click="search_keydown"/>
                    </div>
                </div>
                <div class="navbar_right">
                    <div class="navbar_status">
                        <span v-if="typeof(secretPhrase) === 'undefined'">{{accountRS}} | 观察模式</span>
                        <span class="isLogin" v-else>{{accountRS}} | 私钥模式</span>
                    </div>
                    <div class="navbar_pilotLamp">
                        <el-tooltip class="item" content="挖矿中" placement="bottom" effect="light" v-if="">
                            <div class="pilotLamp_circle"></div>
                        </el-tooltip>
                        <el-tooltip class="item" content="挖矿中" placement="bottom" effect="light">
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
                activeIndex: "/account",
                isRouter: true,
                placeholder: "搜索",
                activeSearch: false,

                secretPhrase:SSO.secretPhrase,
                accountRS:SSO.accountRS,

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
            console.log("secretPhrase",this.secretPhrase);
       },
        mounted(){
            const _this = this;
            let getData = setInterval(function () {
                _this.$global.setBlockchainState(_this).then(res=>{
                    if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getBlockchainStatus",'GET',res);
                    }
                });
                _this.$global.setgetForging(_this).then(res=>{
                    if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getForging",'GET',res);
                    }
                });
                _this.$global.setPeers(_this).then(res=>{
                    if(_this.$global.isOpenConsole){
                        _this.$global.addToConsole("/sharder?requestType=getPeers",'GET',res);
                    }
                });
            },5000);
        },
        methods: {
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
                        console.log("console关闭");
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
