import Vue from "vue";
import Vuex from "vuex";
import ui from "./ui";

Vue.use(Vuex);
const store = new Vuex.Store({
    state: {
        mask: false,
        isLogin: false,
        isHubInit:false,
        isPassphrase: false,
        passphrase: "",
        account:"",  //账户ID
        unconfirmedTransactionsList:[],
        userConfig:[],
        currentLang:localStorage.getItem('lang'),
    },
    mutations: { // 类似 vue 的 methods
        loginState (state) {
            state.isLogin = true;
        },
        setUnconfirmedNotificationsList(state,data){
            state.unconfirmedTransactionsList = data;
        },
        updateLang(state,value){
            state.currentLang =value;
            localStorage.setItem('lang',state.currentLang);
        }
    },
    modules: {
        ui
    }
});
export default store;
