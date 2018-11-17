import Vue from "vue";
import Router from "vue-router";
import VueClipboard from "vue-clipboard2";
import Login from "components/login";
import Main from "components/main";
import Account from "views/account";
import Mining from "views/mining";
import Network from "views/network";
import Peers from "views/peers";
import Attribute from "views/mining/attribute";

Vue.use(Router);
Vue.use(VueClipboard);

export const routes = [
    {
        path: "/login",
        component: Login,
        hidden: true
    },
    {
        path: "/",
        component: Main,
        leaf: true,
        redirect: "/account",
        children: [
            {
                path: "/account",
                component: Account
            },
            {
                path: "/mining",
                component: Mining
            },
            {
                path: "/mining/attribute",
                name: "mining-attribute",
                component: Attribute
            },
            {
                path: "/network",
                component: Network
            },
            {
                path: "/network/peers",
                component: Peers
            }
        ]
    }
];

export const router = new Router({ mode: "history", routes });
