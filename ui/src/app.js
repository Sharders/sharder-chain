import Vue from "vue";
import {sync} from "vuex-router-sync";
import Element from "element-ui";
import App from "components/app";
import {router} from "./router";
import axios from "./httpConfig/http.js";
import store from "./store";
import "theme/index.css";
import "normalize.css";
import "whatwg-fetch";
import "material-design-icons";
import "styles/css/index.scss";
import "element-ui/lib/theme-chalk/index.css";
import 'element-ui/lib/theme-chalk/display.css';
import global from "./utils/common.js";
import "../static/sso/js";
import i18n from "./i18n/i18n";
import querystring from 'querystring';
import echarts from "echarts";
import "echarts-worldmap";
let passUrls = ["static", "login", "register", "enter"];
let whiteList = ["/mining", "/mining/binding-account"];

//移动端可访问白名单路径
function passage(path) {
    let platform = navigator.userAgent;
    if (platform.indexOf("iPhone") !== -1 || platform.indexOf("Android") !== -1) {
        if (whiteList.indexOf(path) !== -1) {
            return true;
        }
    }
    return false;
}

/**
 * 根据路径参数切换语言
 */
function urlTabLanguage(query){
    let language = query['language'];
    if (language === "en-US") return i18n.locale = 'en';
    if (language === "zh-CN") return i18n.locale = 'cn';
}

router.beforeEach((to, from, next) => {
    // console.info(to);
    urlTabLanguage(to.query);
    let redirect = to.query['redirect'];
    if (passage(redirect)) {
        next(redirect + "?token" + to.query['token']);
        return;
    }

    if (passage(to.path)) {
        next();
        return;
    }

    if (passUrls.indexOf(to.path.split("/")[1]) === -1) {
        if (store.state.isLogin) {
            next();
        } else {
            next("/");
        }
    } else {
        next();
    }
});


import dialogCommon from "./views/dialog/dialog_common";
import maskedInput from "vue-masked-input";
import ReceiveAlert from "./views/mining/receiveAlert";
import ExchangeReward from "./views/mining/exchange-reward";
//注册组件
Vue.component("dialogCommon",dialogCommon);
Vue.component("masked-input",maskedInput);
Vue.component("ReceiveAlert",ReceiveAlert);
Vue.component("ExchangeReward",ExchangeReward);


sync(store, router);
Vue.use(Element);
Vue.prototype.$http = axios;
Vue.prototype.$global = global;
Vue.prototype.$echarts = echarts;
Vue.prototype.url = "http://localhost:8215/sharder?requestType=";
Vue.prototype.$qs = querystring;
// Vue.prototype.url = "http://47.107.188.3:8215/sharder?requestType=";

const app = new Vue({
    router,
    store,
    i18n,
    ...App
});
export {app, router, store};
global.$vue = app;
