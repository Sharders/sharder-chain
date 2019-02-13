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
import global from "./utils/common.js";
import "../static/sso/js";
import i18n from "./i18n/i18n";
import querystring from 'querystring';

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

router.beforeEach((to, from, next) => {
    // console.info(to);
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

sync(store, router);
Vue.use(Element);
Vue.prototype.$http = axios;
Vue.prototype.$global = global;
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
