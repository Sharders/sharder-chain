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
                    <el-menu-item index="/account" :class="activeIndex === '/account' ? 'activeLi' : ''">账户</el-menu-item>
                    <el-menu-item index="/network" :class="activeIndex === '/network' ? 'activeLi' : ''">网络</el-menu-item>
                    <el-menu-item index="/mining" :class="activeIndex === '/mining' ? 'activeLi' : ''">矿池</el-menu-item>
                    <!--<el-menu-item index="/1" :class="activeIndex === '/1' ? 'activeLi' : ''">
                        <img src="../../assets/console.svg">
                    </el-menu-item>-->
                </el-menu>
                <div class="navbar_console">
                    <el-button type="text">
                        <span class="console"></span>
                    </el-button>
                </div>
                <div class="navbar_search">
                    <div>
                        <input class="navbar_search_input" :class="activeSearch ? 'navbar_search_input_active' : ''"
                               :placeholder="placeholder" type="text" name="search" v-model="search_val"
                               @focus="search_focus" @blur="search_blur" @keyup.enter="search_keydown"/>
                        <img src="../../assets/search.svg"/>
                    </div>
                </div>
                <div class="navbar_right">
                    <div class="navbar_status">
                        <span>SSA-9WKZ-DV7P-M6MN-SMH8B | 观察模式</span>
                    </div>
                    <div class="navbar_pilotLamp">
                        <el-tooltip class="item" content="挖矿中" placement="bottom" effect="light">
                            <div class="pilotLamp_circle"></div>
                        </el-tooltip>
                    </div>
                    <div class="navbar_exit">
                        <span><a>退出</a></span>
                    </div>
                    <div class="navbar_lang">
                        <button>语言&nbsp;<span class="triangle "></span></button>
                    </div>
                </div>
            </nav>
        </div>
    </header>
</template>

<script>
    export default {
        name: "Header",
        props: ["openSidebar", "title"],
        data () {
            return {
                activeIndex: "/account",
                isRouter: true,
                placeholder: "搜索",
                activeSearch: false,
                search_val: ""
            };
        },
        methods: {
            activeItem: function (val) {
                const _this = this;
                _this.activeIndex = val;
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
                console.log("你输入的值是" + _this.search_val);
                _this.search_val = "";
            }
        }
    };
</script>
<style lang="scss" type="text/scss">
    /* You can import all your SCSS variables using webpack alias*/
    /*@import '~scss_vars';*/
    @import './style.scss';
</style>
