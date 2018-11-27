/* 全局变量及方法 */
const epochBeginning = -1;

function setEpochBeginning (t) {
    const _this = this;
    t.$http.get("/sharder?requestType=getConstants").then(function (res) {
        _this.epochBeginning = parseInt(res.data.epochBeginning);
    });
};
function myFormatTime(value,type){
    const _this = this;
    let dataTime="";
    let data = new Date();
    let date = parseInt(value+'000')+_this.epochBeginning;
    data.setTime(date);
    let year   =  data.getFullYear();
    let month  =  _this.addZero(data.getMonth() + 1);
    let day    =  _this.addZero(data.getDate());
    let hour   =  _this.addZero(data.getHours());
    let minute =  _this.addZero(data.getMinutes());
    let second =  _this.addZero(data.getSeconds());
    if(type === "YMD"){
        dataTime =  year + "-"+ month + "-" + day;
    }else if(type === "YMDHMS"){
        dataTime = year + "-"+month + "-" + day + " " +hour+ ":"+minute+":" +second;
    }else if(type === "HMS"){
        dataTime = hour+":" + minute+":" + second;
    }else if(type === "YM"){
        dataTime = year + "-" + month;

    }
    return dataTime;//将格式化后的字符串输出到前端显示
};
function addZero(val) {
    if (val < 10) {
        return "0" + val;
    } else {
        return val;
    }
};
function formatMoney(num) {
    let result = '', counter = 0;
    num = (num || 0).toString();
    for (let i = num.length - 1; i >= 0; i--) {
        counter++;
        result = num.charAt(i) + result;
        if (!(counter % 3) && i !== 0) { result = ',' + result; }
    }
    return result;
};
export default {
    epochBeginning,
    setEpochBeginning,
    myFormatTime,
    addZero,
    formatMoney
};
