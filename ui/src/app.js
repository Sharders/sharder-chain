import Vue from "vue";
import { sync } from "vuex-router-sync";
import Element from "element-ui";
import App from "components/App";
import { router } from "./router";
import store from "./store";
import locale from "element-ui/lib/locale/lang/en";
import "theme/index.css";
import "normalize.css";
import "whatwg-fetch";
import "material-design-icons";
import "styles/index.scss";

var passUrls = ["static", "login"];

router.beforeEach((to, from, next) => {
        if (passUrls.indexOf(to.path.split("/")[1]) === -1) {
            if (store.state.isLogin) {
                next();
            } else {
                next("/login");
            }
        } else {
            next();
        }
    });

sync(store, router);
Vue.use(Element, { locale });

const app = new Vue({
  router,
  store,
  ...App
});

export { app, router, store };
