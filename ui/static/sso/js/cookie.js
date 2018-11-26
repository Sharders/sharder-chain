const cookie = {

    // 根据key值获取对应的cookie
    get: function (key) {
        // 获取cookie
        const data = document.cookie;
        // 获取key第一次出现的位置    pwd=
        let startIndex = data.indexOf(key + "=");
        //  name=123;pwd=abc
        // 如果开始索引值大于0表示有cookie
        if (startIndex > -1) {
            // key的起始位置等于出现的位置加key的长度+1
            startIndex = startIndex + key.length + 1;
            // 结束位置等于从key开始的位置之后第一次;号所出现的位置
            let endIndex = data.indexOf(";", startIndex);
            // 如果未找到结尾位置则结尾位置等于cookie长度，之后的内容全部获取
            endIndex = endIndex < 0 ? data.length : endIndex;
            return decodeURIComponent(data.substring(startIndex, endIndex));
        } else {
            return "";
        }
    },
    set: function (key, value, time) {
        // 默认保存时间
        const times = time;
        // 获取当前时间
        const cur = new Date();

        // 设置指定时间
        cur.setTime(cur.getTime() + times * 24 * 3600 * 1000);

        // 创建cookie  并且设置生存周期为GMT时间,一定要设置path呀，要不然你可能会被坑得不要不要的
        document.cookie = key + "=" + encodeURIComponent(value) + ";expires=" + (time === undefined ? "" : cur.toGMTString()) + ";path=" + "/";
    },
    del: function (key) {
        // 获取cookie
        const data = this.get(key);
        // 如果获取到cookie则重新设置cookie的生存周期为过去时间
        if (data !== false) {
            this.set(key, data, -1);
        }
    }

};
export default{
    get: cookie.get,
    set: cookie.set,
    del: cookie.del
};
