/* 全局变量及方法 */
const epochBeginning = -1;

function setEpochBeginning (t) {
    const _this = this;
    t.$http.get("/sharder?requestType=getConstants").then(function (res) {
        _this.epochBeginning = parseInt(res.data.epochBeginning);
    });
};
export default {
    epochBeginning,
    setEpochBeginning
};
