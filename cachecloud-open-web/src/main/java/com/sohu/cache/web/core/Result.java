package com.sohu.cache.web.core;

import com.alibaba.fastjson.JSON;

/**
 * @author: lichunfeng
 * @date: 2019/7/5
 * @email: lichunfeng@ztgame.com
 * @description: 统一API响应结果封装
 * @since 2019/7/5
 */
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public Result setCode(ResultCode resultCode) {
        this.code = resultCode.code();
        return this;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Result setMessage(String message) {
        this.message = message;
        return this;
    }

    public T getData() {
        return data;
    }

    public Result setData(T data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
