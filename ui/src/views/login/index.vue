<template>
    <div>
        this is login page
        <button @click="login()"></button>
    </div>
</template>

<script>
    import store from "../../store"
    export default {
        name: "index",
        data () {
            return {
            }
        },
        methods: {
            login: function () {
                store.commit("loginState")
                console.log(store.state.isLogin)
            }
        }
    }
</script>

<style scoped>

</style>
