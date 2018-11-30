/**
 * 全局变量及方法
 */
export default {
    epochBeginning: -1,
    newConsole:null,
    isOpenConsole:false,
    blockchainState:[],
    peers:[],
    fetch(type, date, requestType) {
        return new Promise(function (resolve, reject) {
            $.ajax({
                url: window.api.sharderUrl + "?requestType=" + requestType,
                dataType: "json",
                type: type,
                data: date,
                success: function (data) {
                    resolve(data)
                },
                error: function (error) {
                    reject(error)
                }
            })
        })
    },
    setBlockchainState(t) {
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get('/sharder?requestType=getBlockchainStatus',{
                params:{
                    random:parseInt(new Date().getTime().toString())
                }
            }).then(res => {
                _this.blockchainState = res.data;
                resolve(res.data);
                // if (_this.isOpenConsole) {
                //     console.log(res.data);
                // }
            });
        });
    },
    setgetForging(t){
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get('/sharder?requestType=getForging',{
                params:{
                    random:parseInt(new Date().getTime().toString())
                }
            }).then(res => {
                resolve(res.data);
                // if (_this.isOpenConsole) {
                //     console.log(res.data);
                // }
            });
        });
    },
    setPeers(t){
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get('/sharder?requestType=getPeers',{
                params: {
                    includePeerInfo:true,
                    random:parseInt(new Date().getTime().toString())
                }
            }).then(res => {
                _this.peers = res.data;
                resolve(res.data);
                // console.log(res.data);
                // if (_this.isOpenConsole) {
                //     console.log(res.data);
                // }
            });
        });
    },
    setEpochBeginning(t) {
        const _this = this;
        t.$http.get("/sharder?requestType=getConstants").then(function (res) {
            _this.epochBeginning = parseInt(res.data.epochBeginning);
        });
    },
    myFormatTime(value, type) {
        const _this = this;
        let dataTime = "";
        let data = new Date();
        let date = parseInt(value + '000') + _this.epochBeginning;
        data.setTime(date);
        let year = data.getFullYear();
        let month = _this.addZero(data.getMonth() + 1);
        let day = _this.addZero(data.getDate());
        let hour = _this.addZero(data.getHours());
        let minute = _this.addZero(data.getMinutes());
        let second = _this.addZero(data.getSeconds());
        if (type === "YMD") {
            dataTime = year + "-" + month + "-" + day;
        } else if (type === "YMDHMS") {
            dataTime = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
        } else if (type === "HMS") {
            dataTime = hour + ":" + minute + ":" + second;
        } else if (type === "YM") {
            dataTime = year + "-" + month;

        }
        return dataTime;//将格式化后的字符串输出到前端显示
    },
    addZero(val) {
        if (val < 10) {
            return "0" + val;
        } else {
            return val;
        }
    },
    formatMoney(num) {
        let result = '', counter = 0;
        num = (num || 0).toString();
        for (let i = num.length - 1; i >= 0; i--) {
            counter++;
            result = num.charAt(i) + result;
            if (!(counter % 3) && i !== 0) {
                result = ',' + result;
            }
        }
        return result;
    },


    addToConsole(url, type, data, response, error) {
        const _this = this;
        if(!_this.isOpenConsole || !_this.newConsole){
            return;
        }
        if(!_this.newConsole.document || !_this.newConsole.document.body){
            _this.isOpenConsole = false;
            _this.newConsole = null;
            return;
        }

        url = url.replace(/&random=[\.\d]+/, "",url);
        _this.addToConsoleBody(url+ "("+type+")"+new Date().toString(), "url");

        if (data) {
            if (typeof data === "string") {
                let d = _this.queryStringToObject(data);
                _this.addToConsoleBody(JSON.stringify(d, null, "\t"), "post");
            } else {
                _this.addToConsoleBody(JSON.stringify(data, null, "\t"), "post");
            }
        }

        if (error) {
            _this.addToConsoleBody(response, "error");
        } else {
            if(typeof response === 'undefined'){
                return;
            }else{
                _this.addToConsoleBody(JSON.stringify(response, null, "\t"), (response.errorCode ? "error" : ""));
            }
        }
    },

    addToConsoleBody(text,type){
        const _this = this;
        let color = "";
        switch (type) {
            case "url":
                color = "#29FD2F";
                break;
            case "post":
                color = "lightgray";
                break;
            case "error":
                color = "red";
                break;
        }
        _this.logConsole(text, true);
        $(_this.newConsole.document.body).find("#console").append("<pre" + (color ? " style='color:" + color + "'" : "") + ">" + text.escapeHTML() + "</pre>");

    },
    logConsole(msg, isDateIncluded, isDisplayTimeExact){

    },
    queryStringToObject(qs){
        qs = qs.split("&");

        if (!qs) {
            return {};
        }

        let obj = {};

        for (let i = 0; i < qs.length; ++i) {
            let p = qs[i].split('=');

            if (p.length !== 2) {
                continue;
            }

            obj[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
        }
        if ("secretPhrase" in obj) {
            obj.secretPhrase = "***";
        }

        return obj;
    },
};
