const SERVER_API = "http://localhost:9215";
// const SERVER_API = "https://biz.sharder.io";
// const SERVER_API_DEV = "47.107.188.3:8215";
// const SERVER_API_DEV = "http://localhost:8215";
// const SERVER_API_TEST = "http://localhost:8215";
const SERVER_API_DEV = "http://localhost:9215";

function getUrl() {
    console.log("window.location.port",window.location.port);
    if(window.location.port === '4000'){
        return SERVER_API_DEV;
    }else{
        return "";
    }
    // if (process.env.NODE_ENV === 'development' ) {
    //     return SERVER_API_DEV;
    //     // return '';
    // } else {
    //     return "";
    // }
}

const api = {
    apiUrl: getUrl(),
    sharderUrl: getUrl() + "/sharder",
};
window.api = api;
console.log("api.apiUrl",api.apiUrl);

export default getUrl();
