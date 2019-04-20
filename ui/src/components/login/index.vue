<template>
    <div class="container_login">
        <div class="content">
            <div class="content_message">
                <div>
                    <a>{{$t('login.login_tip1')}}</a><br/>
                    <a>{{$t('login.login_tip2')}}</a>
                    <a>The testnet peer port is 8218</a>
                </div>
            </div>

            <el-col :span="24">
                <el-dropdown trigger="click"  @command="languageChange">
                        <span  class="el-dropdown-link" >
                            <a>{{$t('login.language')}}</a><img src="../../assets/img/select-language.png"/>
                        </span>
                    <el-dropdown-menu slot="dropdown">
                        <el-dropdown-item command="cn">中文</el-dropdown-item>
                        <el-dropdown-item command="en">English</el-dropdown-item>
                    </el-dropdown-menu>
                </el-dropdown>
            </el-col>

            <el-col :span="24" class="sharder_style">
                <img src="../../assets/img/logo.svg"/>
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
                this.$i18n.locale = language;
                this.$store.commit('updateLang',language);
            }
        }
    };
</script>


<style lang="scss">
    @import './style.scss';
</style>
