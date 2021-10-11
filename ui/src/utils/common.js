/**
 * 全局变量及方法
 */
import vars from "../styles/css/vars.scss";

export default {
    // core config of project
    projectName: vars.projectName,
    displayName: "Sharder",
    displaySymbol: "SS",
    projectPrefixStr: "CDW",
    loginState: 'hub',
    foundationHost: 'mw.run',
    foundationTestHost: 'test.mw.run',
    unit: " MW",
    primaryColor: vars.primary_color,
    primaryColor_dd: vars.primary_color_dd,
    pattern: /CDW-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/,
    receiverPrefixStr: "CDW-____-____-____-_____",
    receiverEmptyStr: "___-____-____-____-_____",
    projectReg: /^CDW\-/i,
    apiUrl: '',
    cfg: {defaultInterval: 300000, soonInterval: 60000, slowInterval: 600000, topSpeedInterval: 30000},
    epochBeginning: -1,
    newConsole: null,
    isOpenConsole: false,
    blockchainState: [],
    peers: [],
    userConfig: [],
    registerSharderSiteUser: [],
    coordinatesMap: null,
    $vue: {},
    placeholder: "--",
    HecoUnit: "HMW",
    OKExUnit: "OMW",
    ETHUnit: "EMW",
    TronUnit: "TMW",
    BSCUnit: "BMW",
    unitValue: 100000000,
    HecoUnitValue: 100000000,
    OKExUnitValue: 100000000,
    ETHUnitValue: 100000000,
    TronUnitValue: 100000000,
    BSCUnitValue: 100000000,
    poolPledgeAmount: 10000000000000, // pledge amount of pool creator
    optHeight: {join: 0, quit: 0, destroy: 0, create: 0},
    validPeerPercentage: 0.7, // Less than this value filter display mode, greater than or equal to close
    defineConf() {
        if (this.projectName === 'mw') {
            this.projectPrefixStr = "CDW"
            this.receiverPrefixStr = "CDW-____-____-____-_____"
            this.pattern = /CDW-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/
            this.unit = " MW"
            this.projectReg = /^CDW\-/i
        } else if (this.projectName === 'sharder') {
            this.projectPrefixStr = "SSA"
            this.receiverPrefixStr = "SSA-____-____-____-_____"
            this.pattern = /SSA-([A-Z0-9]{4}-){3}[A-Z0-9]{5}/
            this.unit = " SS"
            this.projectReg = /^SSA\-/i
        }
    },
    updateConf(res) {
        // Get the config and render the corresponding UI scheme
        const projectName = res["sharder.projectName"];
        const coinUnit = res["sharder.coinUnit"];
        const foundationUrl = res["sharder.foundationUrl"];
        const foundationTestUrl = res["sharder.foundationTestUrl"];

        if (coinUnit != null) {
            // Close the server to update the core configuration
            // this.unit = " " + coinUnit;
        }

        if (projectName != null) {
            // Close the server to update the core configuration
            // this.defineConf(projectName);
        }

        if (foundationUrl != null) {
            this.foundationHost = foundationUrl;
        }

        if (foundationTestUrl != null) {
            this.foundationTestHost = foundationTestUrl;
        }
    },
    sendVerifyCode(url, username, fun) {

        $.ajax({
            url: url,
            type: 'GET',
            data: {
                email: username,
                mobile: username,
                bizType: 'biz_register'
            },
            xhrFields: {
                withCredentials: true
            },
            dataType: 'json',
            success: function (res) {
                fun(res);
            },
            error: function (e) {
                console.info(e);

            }
        });
    },

    registerSharderSite(url, data, fun) {
        $.ajax({
            url: url,
            type: 'POST',
            data: data,
            dataType: 'json',
            xhrFields: {
                withCredentials: true
            },
            crossDomain: true,
            success: function (res) {
                fun(res);
            },
            error: function (e) {
                console.info(e);

            }
        });
    },

    ajaxGetPicVCode(url, fun) {
        $.ajax({
            url: url,
            type: 'POST',
            dataType: 'json',
            success: function (res) {
                console.info(res);
                if (res.success) {
                    fun(res);
                }

            },
            error: function (e) {
                console.info(e);

            }
        });
    },

    fetch(type, requestData, requestType) {
        const _this = this;
        return new Promise(function (resolve, reject) {
            let sharderUrl = _this.isOpenApiProxy() ? window.api.sharderProxyUrl : window.api.sharderUrl;
            $.ajax({
                url: sharderUrl + "?requestType=" + requestType,
                dataType: "json",
                type: type,
                data: requestData,
                timeout: 60000,
                success: function (data) {
                    resolve(data)
                },
                error: function (error) {
                    reject(error)
                }
            })
        })
    },
    /**
     * 获取区块链信息
     * @param t
     * @returns {Promise<any>}
     */
    setBlockchainState(t) {
        const _this = this;
        return new Promise(function (resolve, reject) {
            _this.blockchainState =
                t.$http.get(_this.urlPrefix() + '?requestType=getBlockchainStatus', {
                    params: {
                        random: parseInt(new Date().getTime().toString())
                    }
                }).then(res => {
                    res.data;
                    resolve(res);
                });
        });
    },
    /**
     * 获取未出块的交易
     * @param t
     * @param account
     * @returns {Promise<any>}
     */
    setUnconfirmedTransactions(t, account) {
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get(_this.urlPrefix() + '?requestType=getUnconfirmedTransactions', {
                params: {
                    random: parseInt(new Date().getTime().toString()),
                    account: account
                }
            }).then(res => {
                resolve(res);
            });
        });
    },
    /**
     * 获取节点
     * @param t
     * @returns {Promise<any>}
     */
    setPeers(t) {
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get(_this.urlPrefix() + '?requestType=getPeers', {
                params: {
                    includePeerInfo: true,
                    random: parseInt(new Date().getTime().toString())
                }
            }).then(res => {
                _this.peers = res.data;
                resolve(res);
            });
        });
    },
    /**
     * 获取创世时间
     * @param t
     * @returns {Promise<any>}
     */
    setEpochBeginning(t) {
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get("/sharder?requestType=getConstants").then(function (res) {
                _this.epochBeginning = parseInt(res.data.epochBeginning);
                resolve(res.data.epochBeginning);
            });
        });
    },
    /**
     * 获取在创世时间后的时间
     * @param value
     * @param type
     * @param hasEpochBeginning
     * @returns {string}
     */
    myFormatTime(value, type, hasEpochBeginning) {
        const _this = this;
        let dataTime = "";
        let data = new Date();
        if (typeof value === 'undefined')
            value = "0";

        let date = parseInt(value + '000');
        if (hasEpochBeginning) {
            date = date + _this.epochBeginning;
        }
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
    /**
     * 获取在创世时间后的时间
     * @param time
     * @param fmt
     * @param tz 本地时区(默认)或UTC时区
     */
    formatTime(time, tz, fmt) {
        const _this = this;
        fmt = fmt || "yyyy-MM-dd hh:mm:ss";
        tz = tz || 0;
        let date = new Date(time * 1000 + _this.epochBeginning + tz * 3600000);
        let o = {
            "M+": date.getUTCMonth() + 1,                          //月份
            "d+": date.getUTCDate(),                               //日
            "h+": date.getUTCHours(),                              //小时
            "m+": date.getUTCMinutes(),                            //分
            "s+": date.getUTCSeconds(),                            //秒
            "q+": Math.floor((date.getUTCMonth() + 3) / 3),     //季度
            "S": date.getUTCMilliseconds()                         //毫秒
        };
        if (/(y+)/.test(fmt))
            fmt = fmt.replace(RegExp.$1, (date.getFullYear() + "").substr(4 - RegExp.$1.length));
        for (let k in o)
            if (new RegExp("(" + k + ")").test(fmt))
                fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
        return fmt;
    },
    /**
     *格式化时间 加0
     * @param val
     * @returns {*}
     */
    addZero(val) {
        if (val < 10) {
            return "0" + val;
        } else {
            return val;
        }
    },
    formatNQTMoney(num, n = 8) {
        let s = new BigNumber(num).dividedBy("100000000").toFixed();

        if (isNaN(s)) {
            return 0 + this.unit;
        }

        n = n >= 0 && n <= 20 ? n : 2;
        s = parseFloat((s + "").replace(/[^\d\.-]/g, "")).toFixed(n) + "";
        let l = s.split(".")[0].split("").reverse(),
            r = s.split(".")[1].replace(/(0+)\b/, '');         //去除末尾的零
        let t = "";
        for (let i = 0; i < l.length; i++) {
            t += l[i] + ((i + 1) % 3 === 0 && (i + 1) !== l.length ? "," : "");
        }
        if (r === '') {
            return t.split("").reverse().join("") + this.unit
        } else
            return t.split("").reverse().join("") + "." + r + this.unit;


    },
    /**
     * 格式化金额
     * @param s
     * @returns {string}
     */
    formatMoney(s, n = 8) {
        /*        let result = '', counter = 0;
                num = (num || 0).toString();
                for (let i = num.length - 1; i >= 0; i--) {
                    counter++;
                    result = num.charAt(i) + result;
                    if (!(counter % 3) && i !== 0) {
                        result = ',' + result;
                    }
                }
                return result;*/
        if (isNaN(s)) {
            return 0;
        }

        n = n >= 0 && n <= 20 ? n : 2;
        s = parseFloat((s + "").replace(/[^\d\.-]/g, "")).toFixed(n) + "";
        let l = s.split(".")[0].split("").reverse(),
            r = s.split(".")[1].replace(/(0+)\b/, '');         //去除末尾的零
        let t = "";
        for (let i = 0; i < l.length; i++) {
            t += l[i] + ((i + 1) % 3 === 0 && (i + 1) !== l.length ? "," : "");
        }
        /*let num = t.split("").reverse().join("") + "." + r;

        num = parseFloat(num).toString();
        return num;*/
        if (r === '') {
            return t.split("").reverse().join("")
        } else
            return t.split("").reverse().join("") + "." + r;


    },
    /**
     * IP 数组 查询 坐标
     */
    byIPtoCoordinates(params) {
        let xhr = new XMLHttpRequest();
        xhr.open("POST", "https://mw.run/api/front/coordinates/ip");
        xhr.setRequestHeader("content-type", "application/json;charset=UTF-8");
        return new Promise(function (resolve, reject) {
            xhr.onload = function () {
                resolve(xhr.response);
            };
            xhr.onerror = function (error) {
                reject(error);
            };
            xhr.send(JSON.stringify(params));
        });
    },
    /**
     * 获取用户配置
     * @param t
     * @returns {Promise<any>}
     */
    getUserConfig(t) {
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get('/sharder?requestType=getUserConfig', {
                params: {
                    random: new Date().getTime().toString()
                }
            }).then(res => {
                _this.userConfig = res.data;
                resolve(res.data);
            }).catch(err => {
                console.log(err);
            });
        });
    },
    /**
     * string转为byte[]
     * @param str
     * @returns {Array}
     */
    stringToByte(str) {
        let bytes = [];
        let len, c;
        len = str.length;
        for (let i = 0; i < len; i++) {
            c = str.charCodeAt(i);
            if (c >= 0x010000 && c <= 0x10FFFF) {
                bytes.push(((c >> 18) & 0x07) | 0xF0);
                bytes.push(((c >> 12) & 0x3F) | 0x80);
                bytes.push(((c >> 6) & 0x3F) | 0x80);
                bytes.push((c & 0x3F) | 0x80);
            } else if (c >= 0x000800 && c <= 0x00FFFF) {
                bytes.push(((c >> 12) & 0x0F) | 0xE0);
                bytes.push(((c >> 6) & 0x3F) | 0x80);
                bytes.push((c & 0x3F) | 0x80);
            } else if (c >= 0x000080 && c <= 0x0007FF) {
                bytes.push(((c >> 6) & 0x1F) | 0xC0);
                bytes.push((c & 0x3F) | 0x80);
            } else {
                bytes.push(c & 0xFF);
            }
        }
        return bytes;
    },
    /**
     * byte[]转为string
     * @param arr
     * @returns {string}
     */
    byteToString(arr) {
        if (typeof arr === 'string') {
            return arr;
        }
        let str = '',
            _arr = arr;
        for (let i = 0; i < _arr.length; i++) {
            let one = _arr[i].toString(2),
                v = one.match(/^1+?(?=0)$/);
            if (v && one.length === 8) {
                let bytesLength = v[0].length;
                let store = _arr[i].toString(2).slice(7 - bytesLength);
                for (let st = 1; st < bytesLength; st++) {
                    store += _arr[st + i].toString(2).slice(2);
                }
                str += String.fromCharCode(parseInt(store, 2));
                i += bytesLength - 1;
            } else {
                str += String.fromCharCode(_arr[i]);
            }
        }
        return str;
    },

    /**
     * 将数据添加进打开的命令行窗口
     * @param url
     * @param type
     * @param data
     * @param response
     * @param error
     */
    addToConsole(url, type, data, response, error) {
        const _this = this;
        if (!_this.isOpenConsole || !_this.newConsole) {
            return;
        }
        if (!_this.newConsole.document || !_this.newConsole.document.body) {
            _this.isOpenConsole = false;
            _this.newConsole = null;
            return;
        }
        url = url.replace(/&random=[\.\d]+/, "", url);
        _this.addToConsoleBody(url + "(" + type + ")" + new Date().toString(), "url");

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
            if (typeof response === 'undefined') {
                return;
            } else {
                _this.addToConsoleBody(JSON.stringify(response, null, "\t"), (response.errorCode ? "error" : ""));
            }
        }
    },
    addToConsoleBody(text, type) {
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
    logConsole(msg, isDateIncluded, isDisplayTimeExact) {

    },
    queryStringToObject(qs) {
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

    setJsonLocalStorage: function (key, val) {
        try {
            localStorage.setItem(key, JSON.stringify(val));
        } catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }

    },
    getJsonLocalStorage: function (key) {
        try {
            return JSON.parse(localStorage.getItem(key));
        } catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },
    setlocalStorage: function (key, val) {
        try {
            localStorage.setItem(key, val);
        } catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },
    getLocalStorage: function (key) {
        try {
            return localStorage.getItem(key);
        } catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },
    setlang: function (_lang) {
        try {
            localStorage.lang = _lang;
        } catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },
    lang: function () {
        try {
            return localStorage.lang;
        } catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },

    getAvgTimestamp: function (lastBlockTimestamp, firstBlockTimestamp, length) {
        let avgTimestamp = parseInt((lastBlockTimestamp - firstBlockTimestamp) / length);
        /*
                let minute = parseInt(avgTimestamp/60);
                let hour = parseInt(minute/60);
                let day = parseInt(hour/24);
                let second = avgTimestamp - minute * 60;

                console.log("lastBlockTimestamp",lastBlockTimestamp);
                console.log("firstBlockTimestamp",firstBlockTimestamp);
                console.log("length",length);
                console.log("avgTimestamp",avgTimestamp);
                console.log("second",second);
                console.log("minute",minute);
                console.log("hour",hour);
                console.log("day",day);

                let dataTime = "";
                if (day !== 0) {
                    dataTime = dataTime + day + " (day) ";
                }
                if (hour !== 0) {
                    dataTime = dataTime + hour + " (hour) ";
                }
                if (minute !== 0) {
                    dataTime = dataTime + minute + " (min) ";
                }
                if (second !== 0) {
                    dataTime = dataTime + second + " (s) ";
                }

                return dataTime;//将格式化后的字符串输出到前端显示
        */
        return avgTimestamp;
    },

    /**
     * 将科学计数法转换为小数
     * @param num
     * @returns {string | * | * | *}
     */
    toNonExponential: function (num) {
        let m = num.toExponential().match(/\d(?:\.(\d*))?e([+-]\d+)/);
        console.log("m", m);
        let result = num.toFixed(Math.max(0, (m[1] || '').length - m[2]));
        console.log("result", result);
        return result;
    },
    /**
     * 数字转Azd
     * @param num
     * @returns {string}
     */
    numToAzd(num) {
        let str = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        let s = num < 0 ? '$' : '';
        num = Math.abs(num);
        let i = 0;
        while (true) {
            i = num % str.length;
            s += str.substr(i, 1);
            num = parseInt(num / str.length);
            if (num < str.length) {
                s += str.substr(num, 1);
                break;
            }
        }
        return s;
    },
    /**
     * 将数字转换成无符号long
     */
    longUnsigned(num) {

        if (typeof (num) == 'number' && num > 0) return num;

        if (typeof (num) == 'string') {
            num = new BigInteger(num)
            if (num > 0) return num.toString();
        }

        num = new BigInteger(num).abs();
        return new BigInteger("9223372036854775808").subtract(num).multiply(new BigInteger("2")).add(num).toString();

    },
    isTestNet() {
        return SSO.netWorkType === 'Testnet';
    },
    isMainNet() {
        return SSO.netWorkType === 'Mainnet';
    },
    isDevNet() {
        return SSO.netWorkType === 'Devnet';
    },
    isOpenApiProxy() {
        return SSO.state && SSO.state.apiProxy;
    },
    urlPrefix() {
        const _this = this;
        return  _this.isOpenApiProxy() === true ? '/sharder-proxy' : '/sharder';
    },

    useEoLinker() {
        return SSO.useEoLinker;
    },
    useLocal() {
        return SSO.useLocal;
    },
    getFoundationHost() {
        return (this.isDevNet()) ?
            this.foundationTestHost : this.foundationHost;
    },
    getCommonFoundationAPI(eoLinkerUrl, path) {
        if (this.isMainNet() || this.isTestNet()) {
            return "http://" + this.getFoundationHost() + path;
        }
        return eoLinkerUrl;
    },
    /**
     * 渲染节点坐标
     */
    drawPeers() {
        var dom = document.getElementById("peers-map")
        if (!dom) {
            console.log('dom peers-map got failed，echarts can not draw the peer map')
            return
        }
        let _this = this.$vue;
        let myChart = _this.$echarts.init(dom);

        function parseData(coordinatesMap) {
            if (undefined == coordinatesMap || null == coordinatesMap) return;

            let mapData = [];
            for (let i of Object.keys(coordinatesMap)) {
                if (coordinatesMap[i]["X"] !== "" && coordinatesMap[i]["X"] !== "0"
                    && coordinatesMap[i]["Y"] !== "" && coordinatesMap[i]["Y"] !== "0"
                    && !isNaN(coordinatesMap[i]["X"]) && !isNaN(coordinatesMap[i]["Y"])) {
                    let locationArray = [coordinatesMap[i]["Y"], coordinatesMap[i]["X"]];
                    mapData.push({
                        name: i,
                        value: locationArray
                    });
                }
            }
            return mapData;
        };

        let option = {
            geo: {
                map: "world",
                silent: false,
                label: {
                    emphasis: {
                        show: true,
                        areaColor: "#eceef1"
                    }
                },
                itemStyle: {
                    normal: {
                        borderWidth: 1,
                        borderColor: "#fff"
                    },
                    emphasis: {
                        areaColor: this.primaryColor
                    }
                },
                left: 0,
                top: 0,
                bottom: 0,
                right: 0,
                roam: false
            },
            parallel: {
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                parallelAxisDefault: {
                    type: "value",
                    nameLocation: "start",
                    nameTextStyle: {
                        fontSize: 12
                    },
                    nameGap: 20,
                    splitNumber: 3,
                    tooltip: {
                        show: false
                    },
                    axisLine: {
                        show: true,
                        lineStyle: {
                            width: 1,
                            color: "rgba(255,255,255,0.3)"
                        }
                    },
                    axisTick: {
                        show: true
                    },
                    splitLine: {
                        show: true
                    },
                    z: 100
                }
            },
            series: [
                {
                    name: "Node",
                    type: "scatter",
                    coordinateSystem: "geo",
                    symbolSize: 8,
                    data: parseData(this.coordinatesMap),
                    activeOpacity: 1,
                    label: {
                        normal: {
                            formatter: "{b}",
                            position: "right",
                            show: false
                        },
                        emphasis: {
                            show: true
                        }
                    },
                    itemStyle: {
                        normal: {
                            borderColor: "#fff",
                            color: this.primaryColor_dd
                        }
                    }
                }
            ]
        };
        myChart.setOption(option);
    },
    /**
     * 获得交易类型的字符串
     * @param t
     * @returns {string}
     */
    getTransactionTypeStr(t) {
        if (t.type === 0) return this.$vue.$t("transaction.transaction_type_payment");
        if (t.type === 1 && t.subtype === 0) return this.$vue.$t("transaction.transaction_type_information");
        if (t.type === 1 && t.subtype === 5) return this.$vue.$t("transaction.transaction_type_account");
        if (t.type === 11) return this.$vue.$t("transaction.transaction_type_storage_service");
        if (t.type === 8) {
            if (t.subtype === 0) {
                return this.$vue.$t("transaction.transaction_type_pool_create");
            } else if (t.subtype === 1) {
                return this.$vue.$t("transaction.transaction_type_pool_destroy");
            } else if (t.subtype === 2) {
                return this.$vue.$t("transaction.transaction_type_pool_join");
            } else if (t.subtype === 3) {
                if (t.senderRS === SSO.accountRS && t.recipientRS !== SSO.accountRS) {
                    return this.$vue.$t("transaction.transaction_type_pool_quit_not_myself");
                }
                return this.$vue.$t("transaction.transaction_type_pool_quit");
            } else {
                return this.$vue.$t("transaction.transaction_type_forge_pool");
            }
        }
        if (t.type === 9) {
            // BLOCK_REWARD, SINGLE, FOUNDING_TX, GENESIS, SPECIAL_LOGIC
            if ("GENESIS" === t.attachment.coinBaseType) {
                return this.$vue.$t("transaction.transaction_type_genesis_reward");
            } else if ("BLOCK_REWARD" === t.attachment.coinBaseType) {
                return this.$vue.$t("transaction.transaction_type_block_reward");
            }
            return this.$vue.$t("transaction.transaction_type_system_reward");
        }
        if (t.type === 12) {
            if (t.subtype === 0) {
                return this.$vue.$t("transaction.transaction_type_poc_node_type");
            } else if (t.subtype === 1) {
                return this.$vue.$t("transaction.transaction_type_poc_node_config");
            } else if (t.subtype === 2) {
                return this.$vue.$t("transaction.transaction_type_poc_weight_table");
            } else if (t.subtype === 4) {
                return this.$vue.$t("transaction.transaction_type_poc_block_missing");
            } else {
                return this.$vue.$t("transaction.transaction_type_poc");
            }
        }
        if (t.type === 18) {
            if (t.subtype === 0) {
                return this.$vue.$t("transaction.transaction_type_burn")
            }
        }
    },
    /**
     * 获得交易金额
     * @param t
     * @param accountRS
     * @returns {string}
     */
    getTransactionAmountNQT(t, accountRS) {
        let _this = this;

        let isCreatePoolTx = (t.type === 8 && t.subtype === 0) ? true : false;
        let isDestroyPoolTx = (t.type === 8 && t.subtype === 1) ? true : false;
        let isJoinPoolTx = (t.type === 8 && t.subtype === 2) ? true : false;
        let isQuitPoolTx = (t.type === 8 && t.subtype === 3) ? true : false;

        let amountNQT = t.amountNQT;
        if (isJoinPoolTx || isQuitPoolTx) {
            amountNQT = t.attachment.amount
        } else if (isCreatePoolTx || isDestroyPoolTx) {
            amountNQT = _this.poolPledgeAmount;
        }

        amountNQT = new BigNumber(amountNQT).dividedBy("100000000").toFixed();

        if (isJoinPoolTx || isCreatePoolTx) {
            return -amountNQT + this.unit
        } else if (isQuitPoolTx || isDestroyPoolTx) {
            return "+" + amountNQT + this.unit
        } else if (amountNQT <= 0) {
            return this.placeholder
        } else if (t.type === 18) {
            return amountNQT + this.unit
        } else if (t.senderRS === accountRS && t.type !== 9) {
            return -amountNQT + this.unit
        } else {
            return "+" + amountNQT + this.unit
        }
    },
    /**
     * 获得手续费
     * @param t
     * @returns {string}
     */
    getTransactionFeeNQT(t) {
        if (t.feeNQT <= 0) {
            return this.placeholder;
        }
        return new BigNumber(t.feeNQT).dividedBy("100000000").toFixed() + this.unit;
    },
    /**
     * 返回对象 或 占位符
     * @param o1
     * @param o2
     * @returns {string}
     */
    returnObj(o1, o2) {
        return o1 !== undefined && o2 !== undefined ? o2 : this.placeholder;
    },
    /**
     * 获得发送者或接受者
     * @param t
     */
    getSenderOrRecipient(t) {
        if (t.type === 12) {
            return "System"
        } else if (t.type === 9 && this.$vue.$store.state.account === t.recipientRS) {
            return t.senderRS
        } else if (t.type === 9 && this.$vue.$store.state.account !== t.recipientRS) {
            return this.$vue.$t('dialog.account_transaction_own')
        } else if (this.$vue.$store.state.account === t.recipientRS) {
            return this.$vue.$t('dialog.account_transaction_own')
        } else if (typeof t.recipientRS === 'undefined') {
            return this.placeholder
        } else if (this.$vue.$store.state.account !== t.recipientRS) {
            return t.recipientRS
        }
    },
    /**
     * 获得发送者
     */
    getSenderRSOrWo(t) {
        if (t.type === 9 || t.type === 18) {
            return "System";
        } else if (this.$vue.$store.state.account !== t.senderRS) {
            return t.senderRS
        } else if (this.$vue.$store.state.account === t.senderRS) {
            return this.$vue.$t('dialog.account_transaction_own')
        }
    },
    /**
     * 获得区块总手续费
     * @param totalFeeNQT
     */
    getBlockTotalFeeNQT(totalFeeNQT) {
        if (totalFeeNQT <= 0) {
            return this.placeholder
        }
        return new BigNumber(totalFeeNQT).dividedBy("100000000").toFixed()
    },
    /**
     * 获得区块总金额
     * @param totalAmountNQT
     */
    getBlocKTotalAmountNQT(totalAmountNQT) {
        if (totalAmountNQT <= 0) {
            return this.placeholder
        }
        return new BigNumber(totalAmountNQT).dividedBy("100000000").toFixed() + this.unit;
    },
    /**
     * 获得交易的区块时间
     * @param t
     */
    getTransactionBlockTimestamp(t) {
        if (t.block) {
            return this.formatTime(t.blockTimestamp, 8) + ' | ' + this.formatTime(t.blockTimestamp) + " +UTC"
        }
        return this.placeholder
    },
    /**
     * 获得格式化后的时间
     * @param timestamp
     */
    getFormattedTimestamp(t) {
        if (t) {
            return this.formatTime(t, 8) + ' | ' + this.formatTime(t) + " +UTC"
        }
        return this.placeholder
    },
    /**
     * 格式化MW数量 + "MW"
     * @param num
     * @param f
     * @returns {string}
     */
    getAmountFormat(amount, f) {
        if (!amount || amount <= 0) {
            return this.placeholder
        } else if (f) {
            return amount + this.unit
        } else {
            var precision = amount < 100000000 ? 8 : 2;
            return new BigNumber(amount).dividedBy("100000000").toFixed(precision) + this.unit
        }
    },
    /**
     * 获得奖励分配率
     * @param rule 对象
     * @param num 小数位数
     * @returns {string}
     */
    getRewardRate(rule, num) {
        let level = (rule.level) || (rule.level1 ? rule.level1 : rule.level0);
        return new BigNumber(level.forgepool.reward.max).multipliedBy("100").toFixed(num || 2) + "%";
    },
    /**
     * 字符串反转义方法
     * @param str
     * @returns {*}
     */
    escape2Html(str) {
        let arrEntities = {'lt': '<', 'gt': '>', 'nbsp': ' ', 'amp': '&', 'quot': '"'};
        return str.replace(/&(lt|gt|nbsp|amp|quot);/ig, function (all, t) {
            return arrEntities[t];
        });
    },
    /**
     * 下载文件方法
     * @param content
     * @param filename
     */
    funDownload(content, filename) {
        var eleLink = document.createElement('a');
        eleLink.download = filename;
        eleLink.style.display = 'none';
        // 字符内容转变成blob地址
        var blob = new Blob([content]);
        eleLink.href = URL.createObjectURL(blob);
        // 触发点击
        document.body.appendChild(eleLink);
        eleLink.click();
        // 然后移除
        document.body.removeChild(eleLink);
    },
    /**
     * 读取文件内容方法
     * @param file
     * @returns {Promise}
     */
    readFile(file) {
        let _this = this;
        return new Promise(function (resolve, reject) {
            let reader = new FileReader();
            if (typeof FileReader === 'undefined') {
                _this.$message.error(_this.$t('notification.unsupported_file_type'));
                return;
            }
            reader.readAsText(file, 'utf-8');
            reader.onload = function () {
                resolve(reader.result)
            }
        })
    },

    /**
     *  去除前后空格
     * @param str
     * @returns {*}
     */
    trimAll(str){
        return this.trimRight(this.trimLeft(str));
    },
    /**
     * 去掉左边的空白
     * @param s
     * @returns {String|string}
     */
    trimLeft(s){
        if(s == null) {
            return "";
        }
        var whitespace = new String(" \t\n\r");
        var str = new String(s);
        if (whitespace.indexOf(str.charAt(0)) != -1) {
            var j=0, i = str.length;
            while (j < i && whitespace.indexOf(str.charAt(j)) != -1){
                j++;
            }
            str = str.substring(j, i);
        }
        return str;
    },

    /**
     * 去掉右边的空白
     * @param s
     * @returns {String|string}
     */
    trimRight(s){
        if(s == null) return "";
        var whitespace = new String(" \t\n\r");
        var str = new String(s);
        if (whitespace.indexOf(str.charAt(str.length-1)) != -1){
            var i = str.length - 1;
            while (i >= 0 && whitespace.indexOf(str.charAt(i)) != -1){
                i--;
            }
            str = str.substring(0, i+1);
        }
        return str;
    },
};
