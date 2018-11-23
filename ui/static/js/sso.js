import MaskedInput from "vue-masked-input";
import NRS from "./util/sso.encryption";
import NxtAddress from "./util/nxtaddress";
import converters from "./util/converters";
import CryptoJS from "./util/sha256";
import curve25519 from "./util/curve25519";

global.BigInteger = require("jsbn").BigInteger;
global.converters = converters;
global.CryptoJS = CryptoJS;
global.curve25519 = curve25519;

module.exports = {
    MaskedInput,
    NRS,
    NxtAddress
}
