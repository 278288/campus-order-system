package com.zzx.campusorder.common.security;

import org.springframework.security.core.AuthenticationException;

/**
 * 账号停用认证异常。
 *
 * <p>继承 {@link AuthenticationException} 而不是业务异常，是为了让
 * {@code ExceptionTranslationFilter} 能够捕获它并转交给对应的认证入口处理器；
 * 若直接抛业务异常，异常会穿透过滤器链被容器当成 500 处理。</p>
 *
 * @author 郑志雄 2520145325
 */
public class DisabledAccountAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public DisabledAccountAuthenticationException(String message) {
        super(message);
    }
}
