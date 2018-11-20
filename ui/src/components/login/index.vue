<template>
    <div class="container_login">
        <div class="content">
            <div class="content_message">
                <div>
                    <a>请用您的测试网络账户登录-不是您的真实账户!</a><br/>
                    <a>警告:您已连接到测试网络,不要使用您的真实密钥!</a>
                    <a>The testnet peer port is 8218</a>
                </div>
            </div>

            <el-col :span="24">
                <el-dropdown trigger="click"  @command="languageChange">
                        <span  class="el-dropdown-link" >
                            <a>语言</a><img src="../../assets/select-language.png"/>
                        </span>
                    <el-dropdown-menu slot="dropdown">
                        <el-dropdown-item command="cn">中文</el-dropdown-item>
                        <el-dropdown-item command="en">English</el-dropdown-item>
                    </el-dropdown-menu>
                </el-dropdown>
            </el-col>

            <el-col :span="24" class="sharder_style">
                <img src="../../assets/logo.svg"/><br/>
                <a>Sharder</a>
            </el-col>

           <div class="content_operation">
               <div class="content_container">
                   <router-view />
               </div>
           </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "index",
        data () {
            return {};
        },
        methods: {
            languageChange: function (language) {
                console.log(language);
            }
        }
    };
</script>


<style lang="scss">
    @import '~scss_vars';
    @import './style.scss';
</style>
