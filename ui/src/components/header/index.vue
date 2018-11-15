<template>
    <header class="header">
        <div class="header-content">
            <div id="logo">
                <a href="#" class="logo">
                    <img src="../../../static/asset/header-logo.png"/>
                    <div>
                        <span>Sharder</span>
                        <span>COS 版本：0.1.0-Alpha</span>
                    </div>
                </a>
            </div>
            <nav class="navbar-main" role="navigation">
                <el-menu class="navbar-left el-menu-demo" mode="horizontal" :router=isRouter @select="activeItem">
                    <el-menu-item index="/account" :class="activeIndex == '/account' ? 'activeLi' : ''">账户
                    </el-menu-item>
                    <el-menu-item index="/network" :class="activeIndex == '/network' ? 'activeLi' : ''">网络
                    </el-menu-item>
                    <el-menu-item index="/mining" :class="activeIndex == '/mining' ? 'activeLi' : ''">矿池</el-menu-item>
                    <el-menu-item index="/console" :class="activeIndex == '/console' ? 'activeLi' : ''">
                        <img src="../../../static/asset/header-console.png">
                    </el-menu-item>
                </el-menu>
                <div class="navbar-search">
                    <div>
                        <transition name="flix">
                            <input class="navbar-search-input" placeholder="搜索" type="text" name="search"/>
                        </transition>
                        <img src="../../../static/asset/search.png"/>
                    </div>
                </div>
                <div class="navbar-right">
                    <div class="navbar-status">
                        <span>SSA-9WKZ-DV7P-M6MN-SMH8B | 观察模式</span>
                    </div>
                    <div class="navbar-pilotLamp">
                        <div class="pilotLamp-circle"></div>
                    </div>
                    <div class="navbar-exit">
                        <span><a>退出</a></span>
                    </div>
                    <div class="navbar-lang">
                        <button>语言&nbsp;<span class="triangle "></span></button>
                    </div>
                </div>
            </nav>
        </div>
    </header>
</template>

<script>
export default {
    name: 'Header',
    props: ['openSidebar', 'title'],
    data() {
        return {
            activeIndex: '/account',
            isRouter: true,
        }
    },
    methods: {
        activeItem: function (val) {
            this.activeIndex = val;
        },
        search_mouseup: function () {
            $(this).animate({width: '300px'}, 500);
        }
    }
}
</script>
<style lang="scss" type="text/scss">
    /* You can import all your SCSS variables using webpack alias*/
    @import '~scss_vars';
    @import './style.scss';
</style>
