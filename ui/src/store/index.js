import Vue from "vue";
import Vuex from "vuex";
import ui from "./ui";

Vue.use(Vuex);
const store = new Vuex.Store({
    state: {
        mask: false,
        isLogin: false,
        isPassphrase: false,
        passphrase: "",
        account:"",  //账户ID
        unconfirmedTransactionsList:[],
    },
    mutations: { // 类似 vue 的 methods
        loginState (state) {
            state.isLogin = true;
        },
        setUnconfirmedNotificationsList(state,data){
            state.unconfirmedTransactionsList = data;
        }
    },
    modules: {
        ui
    }
});
export default store;
