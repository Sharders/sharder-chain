package org.conch.http;

import java.util.Arrays;
import java.util.Optional;

/**
 * 生成返回结果
 *
 * @author CloudSen
 */
public class ResultUtil {

    public enum HttpStatus {
        /**
         * Http状态码
         */
        OK(200, "OK"),
        CREATED(201, "Created"),
        ACCEPTED(202, "Accepted"),
        NO_CONTENT(204, "No Content"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        ;

        private final int value;
        private final String reasonPhrase;

        HttpStatus(int value, String reasonPhrase) {
            this.value = value;
            this.reasonPhrase = reasonPhrase;
        }

        public int value() {
            return value;
        }

        public String getReasonPhrase() {
            return reasonPhrase;
        }

        public static HttpStatus valueOf(int statusCode) {
            return Arrays.stream(HttpStatus.values())
                    .filter(httpStatus -> httpStatus.value() == statusCode)
                    .findAny().orElseThrow(() -> new IllegalArgumentException("No matching constant for [" + statusCode + "]"));
        }
    }

    /**
     * 请求处理成功，并返回操作对象
     *
     * @return 仅带数据的成功結果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(Boolean.TRUE, data);
    }

    /**
     * 请求处理成功，返回成功提示消息，不返回操作对象
     *
     * @param msg 成功提示消息
     * @return 不带数据的成功结果
     */
    public static Result success(String msg) {
        return new Result<>(Boolean.TRUE, null, msg, null);
    }

    /**
     * 请求处理成功，并返回操作对象
     *
     * @param code 操作码
     * @param msg  详情
     * @return 带数据和详情的成功結果
     */
    public static <T> Result<T> success(Integer code, String msg) {
        return new Result<>(Boolean.TRUE, code, msg);
    }

    /**
     * 请求处理成功，并返回操作对象
     *
     * @param code 操作码
     * @param msg  详情
     * @param data 操作的数据
     * @return 带数据和详情的成功結果
     */
    public static <T> Result<T> success(Integer code, String msg, T data) {
        return new Result<>(Boolean.TRUE, code, msg, data);
    }

    /**
     * 请求处理成功，并返回操作对象
     *
     * @param httpStatus HTTP请求状态
     * @param data       操作长的数据
     * @return 带数据和详情的成功结果
     */
    public static <T> Result<T> success(HttpStatus httpStatus, T data) {
        return new Result<>(Boolean.TRUE, httpStatus.value(), httpStatus.getReasonPhrase(), data);
    }

    /**
     * HTTP响应200，请求处理成功，并返回操作对象
     *
     * @param data 操作对象
     * @return 带数据和详情的成功结果
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(Boolean.TRUE, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data);
    }

    /**
     * HTTP响应201，新增成功，并返回新增的对象
     *
     * @param data 需要新增的对象
     * @return 带数据和详情的成功结果
     */
    public static <T> Result<T> created(T data) {
        return new Result<>(Boolean.TRUE, HttpStatus.CREATED.value(), HttpStatus.CREATED.getReasonPhrase(), data);
    }

    /**
     * HTTP响应204，No Content
     *
     * @return 仅带提示信息的成功结果
     */
    public static Result noContent() {
        return new Result<>(Boolean.TRUE, HttpStatus.NO_CONTENT.value(), HttpStatus.NO_CONTENT.getReasonPhrase());
    }

    /**
     * 请求处理异常，返回异常详情
     *
     * @param code 错误代码
     * @param msg  错误详情
     * @return 异常结果
     */
    public static Result error(Integer code, String msg) {
        return new Result<>(Boolean.FALSE, code, msg);
    }

    /**
     * 403 Forbidden
     *
     * @return 仅带提示信息的异常结果
     */
    public static Result error403() {
        return new Result<>(Boolean.FALSE, HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase());
    }

    /**
     * 404 not found
     *
     * @return 仅带提示信息的异常结果
     */
    public static Result error404() {
        return new Result<>(Boolean.FALSE, HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase());
    }

    /**
     * 500 内部服务器错误
     *
     * @return 仅带提示信息的异常结果
     */
    public static Result error500(String msg) {
        msg = Optional.ofNullable(msg).orElse(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        return new Result<>(Boolean.FALSE, HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
    }

    /**
     * 请求处理失败，返回失败详情
     *
     * @param msg 失败详情
     * @return 失败结果
     */
    public static Result failed(String msg) {
        return new Result<>(Boolean.FALSE, msg);
    }

    /**
     * 请求处理失败，返回失败详情
     *
     * @param code 详情代码
     * @param msg  失败详情
     * @return 失败结果
     */
    public static Result failed(Integer code, String msg) {
        return new Result<>(Boolean.FALSE, code, msg);
    }
}
