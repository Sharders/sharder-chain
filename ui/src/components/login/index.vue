<template>
    <div class="login">
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
                this.$router.push("/")
                console.log(store.state.isLogin)
            }
        }
    }
</script>

<style scoped>
    .login{
        /*background-image: url("/static/img/background-login.svg");*/

    }
</style>
