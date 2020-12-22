const SERVER_API = "http://localhost:7216";
// const SERVER_API = "https://biz.sharder.io";
// const SERVER_API_DEV = "47.107.188.3:8216";
// const SERVER_API_DEV = "http://localhost:8216";
// const SERVER_API_TEST = "http://localhost:8216";
const SERVER_API_DEV = "http://localhost:9216";
const SERVER_API_TEST = "http://localhost:7216";
const SHARDER_URL = process.env.NODE_ENV === 'development' ? "http://localhost:8080" : "https://sharder.org";

function getUrl() {
    if (window.location.port === '4000') {
        return SERVER_API_DEV;
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
};
window.api = api;

export default getUrl();
