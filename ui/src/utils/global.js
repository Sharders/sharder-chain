+6
import router from "../router/index";

export default {
    install (Vue, options) {
        Vue.prototype.routerLink = function () {
            if (sessionStorage.getItem("login") == null) {
                router.push("/login");
            }
        };
    }
}
