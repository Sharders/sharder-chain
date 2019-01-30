package org.conch.http;

import com.alibaba.fastjson.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

/**
 * http api 结果详情
 * 仅可用于http api 返回操作结果，不允许用于其他地方！
 *
 * @author cloudsen
 */
public class Result<T> implements Serializable, JSONStreamAware {
    private static final long serialVersionUID = 31231640260507525L;
    /**
     * 是否成功
     */
    private Boolean success;
    /**
     * 异常代码
     */
    private Integer code;
    /**
     * 提示信息
     */
    private String msg;
    /**
     * 返回的数据
     */
    private T data;

    public Result() {

    }

    public Result(Boolean success, Integer code, String msg) {
        this(success, code, msg, null);
    }

    public Result(Boolean success, T data) {
        this(success, null, null, data);
    }

    public Result(Boolean success, Integer code, String msg, T data) {
        this.success = success;
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public Result<T> setSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    public Integer getCode() {
        return code;
    }

    public Result<T> setCode(Integer code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public Result<T> setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public T getData() {
        return data;
    }

    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }

    public String toJsonString() {
        return JSONObject.toJSONString(this);
    }

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(toJsonString());
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException {
        writer.write(JSONObject.toJSONString(this).toCharArray());
    }
}
