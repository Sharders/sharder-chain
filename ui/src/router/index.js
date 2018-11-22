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
import CreateHistory from "views/mining/create-history";
import MyAssets from "views/mining/my-assets";
import FreeCollarDrill from "views/mining/free-collar-drill";
import DiamondExchange from "views/mining/diamond-exchange";
import BindingValidation from "views/mining/binding-validation";

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
                name: "mining",
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
                path: "/mining/create-history",
                name: "create-history",
                component: CreateHistory
            },
            {
                path: "/mining/my-assets",
                name: "my-assets",
                component: MyAssets
            },
            {
                path: "/mining/free-collar-drill",
                name: "free-collar-drill",
                component: FreeCollarDrill
            },
            {
                path: "/mining/diamond-exchange",
                name: "diamond-exchange",
                component: DiamondExchange
            },
            {
                path: "/mining/binding-validation",
                name: "binding-validation",
                component: BindingValidation
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
