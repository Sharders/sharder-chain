import Vue from "vue"
import Router from "vue-router"
import Login from "views/login"
import Account from "views/account"
import Mining from "views/mining"
import Network from "views/network"

Vue.use(Router)

export const routes = [
  {
    path: "/login",
    component: Login
  }, {
    path: "/account",
    component: Account

  }, {
    path: "/mining",
    component: Mining

  }, {
    path: "/network",
    component: Network
  }
]
export const router = new Router({ mode: "history", routes })
