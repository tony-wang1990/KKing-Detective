package com.tony.kingdetective.enums;

import lombok.Getter;

/**
 * <p>
 * ErrorEnum
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/11/7 18:55
 */
@Getter
public enum ErrorEnum {

    LIMIT_EXCEEDED(400, "limit", "??????,??????????"),
    NOT_AUTHENTICATED(401, "NotAuthenticated", "??????????"),
    TOO_MANY_REQUESTS(429, "TooManyRequests", "????"),
    CAPACITY(500, "Out of capacity", "Out of capacity"),
    CAPACITY_HOST(500, "Out of host capacity", "Out of host capacity"),

    ;

    private final int code;
    private final String errorType;
    private final String message;

    ErrorEnum(int code, String errorType, String message) {
        this.code = code;
        this.errorType = errorType;
        this.message = message;
    }
}
