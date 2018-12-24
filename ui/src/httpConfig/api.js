const SERVER_API = "https://test.sharder.io";
// const SERVER_API = "https://biz.sharder.io";
// const SERVER_API_DEV = "47.107.188.3:8215";
// const SERVER_API_DEV = "http://localhost:8215";
const SERVER_API_DEV = "http://192.168.31.115:8215";


function getUrl() {
    if (process.env.NODE_ENV === 'development') {
        return SERVER_API_DEV;
        // return '';
    } else {
        return SERVER_API;
    }
}

const api = {
    apiUrl: getUrl(),
    sharderUrl: getUrl() + "/sharder",
};
window.api = api;
export default getUrl();
