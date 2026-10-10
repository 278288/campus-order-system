package com.zzx.campusorder.common.exception;

import com.zzx.campusorder.common.result.ErrorCodeEnum;
import lombok.Getter;

/**
 * 业务异常。Service 层只抛该异常，由全局异常处理器统一转换为 Result。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCodeEnum errorCode;

    public BusinessException(ErrorCodeEnum errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCodeEnum.BAD_REQUEST, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCodeEnum.UNAUTHORIZED, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCodeEnum.FORBIDDEN, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCodeEnum.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(ErrorCodeEnum.CONFLICT, message);
    }
}
