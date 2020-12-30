"use strict";

module.exports = {
  port: 4000,
  title: "Sharder-Client",
  // when you use electron please set to relative path like ./
  // otherwise only set to absolute path when you're using history mode
  publicPath: "/",
  // add these dependencies to a standalone vendor bundle
  vendor: [
    "vue",
    "vuex",
    "vue-router",
    "vuex-router-sync",
    "whatwg-fetch",
    "normalize.css",
    "offline-plugin/runtime",
    "element-ui",
    "material-design-icons"
  ],

};
