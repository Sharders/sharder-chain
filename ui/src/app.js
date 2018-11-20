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
import "styles/css/index.scss";
import "element-ui/lib/theme-chalk/index.css";

router.beforeEach((to, from, next) => {
  if (to.path !== "/login") {
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
