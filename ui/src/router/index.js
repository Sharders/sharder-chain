import Vue from "vue";
import Router from "vue-router";
import VueClipboard from "vue-clipboard2";
import Login from "components/login";
import Main from "components/main";
import LoginPage from "views/login";
import Register from "views/register";
import Enter from "views/enter";
import Account from "views/account";
import Mining from "views/mining";
import Network from "views/network";
import Peers from "views/peers";
import Attribute from "views/mining/attribute";
import InviteFriends from "views/mining/invite-friends";
import RuleDescription from "views/mining/rule-description";
import MyMining from "views/mining/my-mining";

Vue.use(Router);
Vue.use(VueClipboard);

export const routes = [
    {
        path: "/",
        component: Login,
        redirect: "/login",
        children: [
            {
                path: "/login",
                component: LoginPage,
                hidden: true
            },
            {
                path: "/register",
                component: Register
            },
            {
                path: "/enter",
                component: Enter
            }
        ]
    },
    {
        path: "/account",
        component: Main,
        leaf: true,
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
                path: "/mining/invite-friends",
                name: "invite-friends",
                component: InviteFriends
            },
            {
                path: "/mining/rule-description",
                name: "rule-description",
                component: RuleDescription
            },
            {
                path: "/mining/my-mining",
                name: "my-mining",
                component: MyMining
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
