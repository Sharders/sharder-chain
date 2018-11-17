<template>
    <div id="app">
        <router-view/>
        <div v-if="$store.state.mask" class="mask"></div>
    </div>
</template>
<script>
    export default {
        name: "App"
    }
</script>
<style scoped>
    .mask {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: #000;
        opacity: 0.4;
        z-index: 999;
        min-width: 1200px;
    }
</style>
