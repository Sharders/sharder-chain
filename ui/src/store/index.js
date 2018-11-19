import Vue from "vue";
import Vuex from "vuex";
import ui from "./ui";

Vue.use(Vuex);
const store = new Vuex.Store({
    state: {
        isLogin: false,
        mask: false
    },
    mutations: { // 类似 vue 的 methods
        loginState (state) {
            state.isLogin = true;
        }
    },
    modules: {
        ui
    }
});
export default store;
