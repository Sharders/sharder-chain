const SERVER_API = "https://test.sharder.io";
// const SERVER_API = "https://biz.sharder.io";
const SERVER_API_DEV = "http://localhost:8215";

function getUrl() {
    if (process.env.NODE_ENV === 'development') {
        return SERVER_API_DEV;
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
