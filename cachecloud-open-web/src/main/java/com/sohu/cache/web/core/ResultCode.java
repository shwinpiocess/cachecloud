package com.sohu.cache.web.core;

/**
 * @author: lichunfeng
 * @date: 2019/7/5
 * @email: lichunfeng@ztgame.com
 * @description: 响应码枚举，参考HTTP状态码的语义
 * @since 2019/7/5
 */
public enum ResultCode {
    /*
    200     成功
    400     失败
    401     未认证(签名错误)
    404     接口不存在
    500     服务器内部错误
     */
    SUCCESS(200),
    FAIL(400),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500);

    private final int code;

    ResultCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
