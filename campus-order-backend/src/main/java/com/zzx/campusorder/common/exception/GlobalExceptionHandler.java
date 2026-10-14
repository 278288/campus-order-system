package com.zzx.campusorder.common.exception;

import com.zzx.campusorder.common.result.ErrorCodeEnum;
import com.zzx.campusorder.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：把各类异常统一转换为 {@link Result}，保证前端只处理一种结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：code={} message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.failure(e.getErrorCode(), e.getMessage());
    }

    /**
     * Spring Security 的授权异常。
     *
     * <p>方法级注解（{@code @PreAuthorize}）抛出的 {@link AccessDeniedException}
     * 不经过过滤器链的 AccessDeniedHandler，而是由本处理器捕获，
     * 否则会被兜底分支转成 500，导致「权限不足」被误报为系统错误。</p>
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Result<Void> handleAccessDenied(org.springframework.security.access.AccessDeniedException e) {
        log.warn("方法级授权拒绝：{}", e.getMessage());
        return Result.failure(ErrorCodeEnum.FORBIDDEN, "没有操作权限，请联系管理员分配权限");
    }

    /**
     * Spring Security 的认证异常。
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public Result<Void> handleAuthentication(org.springframework.security.core.AuthenticationException e) {
        log.warn("认证异常：{}", e.getMessage());
        return Result.failure(ErrorCodeEnum.UNAUTHORIZED, "未登录或登录已过期，请重新登录");
    }

    /**
     * Redis 不可用等缓存异常：给出可读提示，避免暴露底层细节。
     */
    @ExceptionHandler(org.springframework.data.redis.RedisConnectionFailureException.class)
    public Result<Void> handleRedisFailure(
            org.springframework.data.redis.RedisConnectionFailureException e) {
        log.error("Redis 连接失败，登录缓存不可用", e);
        return Result.failure(ErrorCodeEnum.INTERNAL_ERROR, "缓存服务不可用，请确认 Redis 已启动");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.failure(ErrorCodeEnum.BAD_REQUEST, message.isEmpty() ? "请求参数有误" : message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.failure(ErrorCodeEnum.BAD_REQUEST, message.isEmpty() ? "请求参数有误" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        return Result.failure(ErrorCodeEnum.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.failure(ErrorCodeEnum.BAD_REQUEST, "缺少必填参数：" + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.failure(ErrorCodeEnum.BAD_REQUEST, "请求体格式不合法，无法解析为 JSON");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.failure(ErrorCodeEnum.METHOD_NOT_ALLOWED, "请求方法不被支持：" + e.getMethod());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandler(NoHandlerFoundException e) {
        return Result.failure(ErrorCodeEnum.NOT_FOUND, "请求的资源不存在：" + e.getRequestURL());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突：{}", e.getMessage());
        return Result.failure(ErrorCodeEnum.CONFLICT, "数据已存在，违反唯一约束");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception e) {
        log.error("系统内部错误", e);
        return Result.failure(ErrorCodeEnum.INTERNAL_ERROR);
    }
}
