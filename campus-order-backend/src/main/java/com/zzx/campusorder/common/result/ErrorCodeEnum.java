package com.zzx.campusorder.common.result;

import lombok.Getter;

/**
 * 错误码枚举。与作业1文档表4-3“接口错误码约定”一一对应。
 */
@Getter
public enum ErrorCodeEnum {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不被支持"),
    CONFLICT(409, "业务状态冲突，请刷新后重试"),
    INTERNAL_ERROR(500, "系统内部错误");

    private final Integer code;
    private final String message;

    ErrorCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
