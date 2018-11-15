<template>
<div id="app">
    <div class="page-layout">
        <div class="page-layout-inner">
            <header-component />
            <main>
            <div class="main-content">
                <el-row class="container">
                    <router-view></router-view>
                </el-row>
            </div>
        </main>
    </div>
    </div>
</div>
</template>
<script>
import Header from "components/header"

export default {
  name: "App",
  components: {
    "header-component": Header
  }
}
</script>

<style lang="scss">
/*// You can import all your SCSS variables using webpack alias*/
@import '~scss_vars';
@import './style.scss';
</style>
