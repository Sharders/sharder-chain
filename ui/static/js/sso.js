(function () {
    global.client = {};
    client.accountInfo = {};

    require("./util/sso.server");
    require("./util/sso.constants");
    require("./util/nrs");
    require("./util/sso.feature.detection");
    global.BigInteger = require("jsbn").BigInteger;
    global.converters = require("./util/converters");
    global.CryptoJS = require("./util/sha256");
    global.curve25519 = require("./util/curve25519");
    global.SSO = require("./util/sso.encryption");
    global.Login = require("./util/sso.login");
    global.MaskedInput = require("vue-masked-input").default;
    global.NxtAddress = require("./util/nxtaddress");
    global.extensions = require("./util/extensions");
    global.util = require("./util/sso.util");
})();
