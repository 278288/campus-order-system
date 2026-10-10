package com.zzx.campusorder.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结构。所有 Controller 一律返回该结构，前端只需判断 code 与 message。
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    /** 业务状态码，取值见 {@link ErrorCodeEnum} */
    private Integer code;

    /** 提示信息，成功时为 "操作成功" */
    private String message;

    /** 业务数据，失败时为 null */
    private T data;

    /** 服务端处理时间戳，便于排查问题 */
    private Long timestamp;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> failure(ErrorCodeEnum errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> failure(ErrorCodeEnum errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /**
     * 分页数据统一包装，避免前端为不同列表重复适配。
     */
    public static <T> Result<PageResult<T>> page(long total, long pageNum, long pageSize, java.util.List<T> records) {
        return success(new PageResult<>(total, pageNum, pageSize, records));
    }
}
