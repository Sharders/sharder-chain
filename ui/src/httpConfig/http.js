/**
 * ajax请求配置
 */
import axios from "axios";
import apiURL from "./api.js";
import Qs from 'qs';
import { Message } from 'element-ui';

import cookie from "../../static/sso/js/cookie.js";

// axios默认配置
axios.defaults.timeout = 50000;   // 超时时间
axios.defaults.baseURL = apiURL;  // 默认地址
axios.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
/**
 * 整理数据
 */
axios.defaults.transformRequest = function (data) {
    if(!(data instanceof FormData)){
        data = JSON.stringify(data);
    }
    return data;

// data = Qs.stringify(data);
/*
    if(typeof data !== 'formData'){
    }
*/

};

// 路由请求拦截
// http request 拦截器
axios.interceptors.request.use(
    config => {
        // config.data = JSON.stringify(config.data);
        if(config.headers["Content-Type"] === null)
            config.headers["Content-Type"] = "application/json;charset=UTF-8";
        // 判断是否存在ticket，如果存在的话，则每个http header都加上ticket
        if (cookie.get("token")) {
            // 用户每次操作，都将cookie设置成2小时
            cookie.set("token", cookie.get("token"), 1 / 12);
            cookie.set("name", cookie.get("name"), 1 / 12);
            config.headers.token = cookie.get("token");
            config.headers.name = cookie.get("name");
        }

        return config;
    },
    error => {
        return Promise.reject(error.response);
    });

// 路由响应拦截
// http response 拦截器
axios.interceptors.response.use(
    response => {
        if (response.data.resultCode === "404") {
            console.log("response.data.resultCode是404");
            // 返回 错误代码-1 清除ticket信息并跳转到登录页面
            // cookie.del("ticket")
            // window.location.href='http://login.com'
            return null;
        } else {
            return response;
        }
    },
    error => {
        // 返回接口返回的错误信息
        console.log(`axios error: ${JSON.stringify(error)}`)
        // Message.error(error.message)
        return Promise.reject(error);
    });
export default axios;
