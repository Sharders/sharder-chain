<template>
    <div class="page-layout">
        <div class="page-layout-inner">
            <header-component />
            <main>
                <div class="main-content">
                    <el-row class="container">
                        <router-view :key="componentKey"/>
                    </el-row>
                </div>
            </main>
        </div>
    </div>
</template>

<script>
    import Header from "components/header";
    export default {
        name: "index",
        data() {
            return {
                componentKey: 0,
            }
        },
        components: {
            "header-component": Header
        },
        computed: {
            refresh: function () {
                return this.$store.state.isRefresh;
            }
        },
        watch: {
            refresh: {
                handler: function (val, oldVal) {
                    const _this = this;
                    if (val) {
                        _this.componentKey ++;
                    }
                }
            }
        }
    };
</script>

<style lang="scss">
    @import './style.scss';
    @import '../../styles/css/base.scss';
    @import '../../styles/css/common.scss';
</style>
