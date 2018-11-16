<template>
    <div id="app">
        <router-view/>
        <div :class="$store.state.mask"></div>
    </div>
</template>
<script>
    export default {
        name: "App"
    }
</script>

