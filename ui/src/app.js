import Vue from "vue";
import { sync } from "vuex-router-sync";
import Element from "element-ui";
import App from "components/App";
import { router } from "./router";
import axios from "./httpConfig/http.js";
import store from "./store";
import "theme/index.css";
import "normalize.css";
import "whatwg-fetch";
import "material-design-icons";
import "styles/css/index.scss";
import "element-ui/lib/theme-chalk/index.css";
import global from "./utils/common.js";

var passUrls = ["static", "login", "register", "enter"];

router.beforeEach((to, from, next) => {
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
Vue.prototype.$global = global;
Vue.prototype.$http = axios;
Vue.prototype.url = "http://localhost:8215/sharder?requestType=";
const app = new Vue({
    router,
    store,
    ...App
});
export { app, router, store };
