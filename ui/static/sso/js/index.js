(function () {
    global.isNode = true;
    global.client = {};
    client.accountInfo = {};
    global.SSO = global.client;

    require("./util/sso.constants");
    require("./util/sso");
    require("./util/sso.forms");
    require("./util/sso.sitebuild");
    require("./util/sso.server");
    require("./util/sso.assetexchange");
    require("./util/sso.feature.detection");
    require("./util/sso.modals.accountdetails");
    require("./util/sso.modals.advanced");
    require("./util/sso.modals.token");
    require("./util/sso.localstorage");
    require("./util/sso.contacts");
    require("./util/sso.settings");
    require("./util/sso.transactions");
    require("./util/sso.notifications");
    require("./util/locale");

    global.BigInteger = require("jsbn").BigInteger;
    global.converters = require("./util/converters");
    global.CryptoJS = require("./util/sha256");
    global.curve25519 = require("./util/curve25519");
    global.encryption = require("./util/sso.encryption");
    global.Login = require("./util/sso.login");
    global.NxtAddress = require("./util/scaddress");
    global.extensions = require("./util/extensions");
    global.util = require("./util/sso.util");
    global.async = require("async");
})();
