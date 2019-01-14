const SERVER_API = "http://localhost:9215";
// const SERVER_API = "https://biz.sharder.io";
// const SERVER_API_DEV = "47.107.188.3:8215";
// const SERVER_API_DEV = "http://localhost:8215";
const SERVER_API_TEST = "http://localhost:8215";
const SERVER_API_DEV = "http://localhost:9215";
const SERVER_PORT_TEST = 8215;
const SERVER_PORT_DEV = 9215;


function getUrl() {
    if (process.env.NODE_ENV === 'development') {
        return SERVER_API_DEV;
        // return '';
    } else {
        return SERVER_API;
    }
}
/*

function setUrl(url){
    if(url !== null || url !== "")
        SERVER_API_DEV = url;
}
*/

const api = {
    apiUrl: getUrl(),
    sharderUrl: getUrl() + "/sharder",
};
window.api = api;
export default getUrl();

