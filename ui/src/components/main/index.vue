<template>
    <div class="page-layout">
        <div class="page-layout-inner">
            <header-component />
            <main>
                <div class="main-content">
                    <el-row class="container">
                        <router-view/>
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
        components: {
            "header-component": Header
        }
    };
</script>

<style lang="scss">
    @import '~scss_vars';
    @import './style.scss';
    @import '../../styles/base.scss';
    @import '../../styles/common.scss';
</style>
