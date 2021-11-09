import global from "../utils/common";

const SERVER_API = global.projectName === 'mw' ? "http://localhost:7216" : "http://localhost:7215";
const SERVER_API_TEST = global.projectName === 'mw' ? "http://localhost:7216" : "http://localhost:7215";
const SHARDER_URL = process.env.NODE_ENV === 'development' ? "http://localhost:8080" : (global.projectName === 'mw' ? "https://mw.run" : "https://sharder.org");
const MGR_URL = process.env.NODE_ENV === 'development' ? "http://localhost:8080" : (global.projectName === 'mw' ? "https://mw.run" : "https://sharder.org");
const MGR_API_URL = process.env.NODE_ENV === 'development' ? "http://localhost:8080/sc" : (global.projectName === 'mw' ? "https://mw.run/sc" : "https://sharder.org/sc");
const HECO_URL = process.env.NODE_ENV === 'development' ? "https://testnet.hecoinfo.com/" : "https://hecoinfo.com/";
const OKEX_URL = process.env.NODE_ENV === 'development' ? "https://www.oklink.com/okexchain-test/" : "https://www.oklink.com/okexchain/";
const ETH_URL = process.env.NODE_ENV === 'development' ? "https://ropsten.etherscan.io/" : "https://cn.etherscan.com/";
// const TRON_URL = process.env.NODE_ENV === 'development' ? "https://api.shasta.trongrid.io/" : "https://api.trongrid.io/";
const BSC_URL = process.env.NODE_ENV === 'development' ? "https://testnet.bscscan.com/" : "https://bscscan.com/";

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
    getAcrossAddress: MGR_URL + "/api/front/acrossChain/getAddress",
    //查询heco交易信息
    getHecoInfo:HECO_URL + "tx/",
    getOKExInfo:OKEX_URL + "tx/",
    getETHInfo:ETH_URL + "tx/",
    // getTronInfo:TRON_URL + "wallet/gettransactioninfobyid",
    getBSCInfo:BSC_URL + "tx/",
    // 鉴权服务
    updateHardwareProduct: MGR_API_URL + "/ssHardwareProduct/update"
};
window.api = api;

export default getUrl();
