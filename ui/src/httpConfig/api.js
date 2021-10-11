import global from "../utils/common";

const SERVER_API = "http://localhost:7216";
// const SERVER_API = "https://biz.sharder.io";
// const SERVER_API_DEV = "47.107.188.3:8215";
// const SERVER_API_DEV = "http://localhost:8215";
// const SERVER_API_TEST = "http://localhost:8215";
const SERVER_API_DEV = "http://localhost:9216";
const SERVER_API_TEST = "http://localhost:7216";
const SHARDER_URL = process.env.NODE_ENV === 'development' ? "http://localhost:8080" : (global.projectName === 'mw' ? "https://mw.run" : "https://sharder.org");
// const MGR_URL = process.env.NODE_ENV === 'development' ? "http://192.168.3.247:8080" : "https://mw.run/admin";
const MGR_URL = process.env.NODE_ENV === 'development' ? "https://mw.run/" : "https://mw.run/";
const HECO_URL = process.env.NODE_ENV === 'development' ? "https://testnet.hecoinfo.com/" : "https://hecoinfo.com/";
const OKEX_URL = process.env.NODE_ENV === 'development' ? "https://www.oklink.com/okexchain-test/" : "https://www.oklink.com/okexchain/";
const ETH_URL = process.env.NODE_ENV === 'development' ? "https://ropsten.etherscan.io/" : "https://cn.etherscan.com/";
// const TRON_URL = process.env.NODE_ENV === 'development' ? "https://api.shasta.trongrid.io/" : "https://api.trongrid.io/";
const BSC_URL = process.env.NODE_ENV === 'development' ? "https://testnet.bscscan.com/" : "https://bscscan.com/";

const MGR_API_URL = process.env.NODE_ENV === 'development' ? "http://localhost:8080/sc" :  "https://mw.run/sc";

function getUrl() {
    if (window.location.port === '4000') {
        return SERVER_API_TEST;
    } else {
        return "";
    }
}

const api = {
    apiUrl: getUrl(),
    sharderUrl: getUrl() + "/sharder",
    sharderProxyUrl: getUrl() + "/sharder-proxy",
    sharderExchange: SHARDER_URL + "/official/exchange.ss",
    ssContactAmount: SHARDER_URL + "/official/getExchangeAmount.ss",
    sharderExchangeRS: SHARDER_URL + "/official/exchange/rs.ss",
    sharderExchangeSSA: SHARDER_URL + "/official/address/ssa.ss",
    simulatedPositioningUrl:SHARDER_URL +"/coordinates/getSPUrl.ss",

    //跨链请求
    getAccountInfoUrl: MGR_URL + "/api/front/acrossChain/getAccountInfo",
    updateChainAccountUrl: MGR_URL + "/api/front/acrossChain/updateChainAccount",
    getRecordUrl: MGR_URL + "/api/front/acrossChain/getRecord",
    getAddress: MGR_URL + "/api/front/acrossChain/getAddress",
    //查询heco交易信息
    getHecoInfo:HECO_URL + "tx/",
    getOKExInfo:OKEX_URL + "tx/",
    getETHInfo:ETH_URL + "tx/",
    // getTronInfo:TRON_URL + "wallet/gettransactioninfobyid",
    getBSCInfo:BSC_URL + "tx/",
    // 快捷授权相关
    updateHardwareProduct: MGR_API_URL + "/ssHardwareProduct/update"
};
window.api = api;

export default getUrl();
