package com.zzx.campusorder.common.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 委托式认证入口：按异常类型把响应分流到 401 或 403。
 *
 * <p>为什么需要一层委托：{@code ExceptionTranslationFilter} 在处理认证异常时，
 * 只会调用安全配置里注册的那一个入口，而具体异常类型它并不关心。
 * 于是把「异常类型 → 合适入口」的分派放在这里：</p>
 * <ul>
 *   <li>{@link com.zzx.campusorder.common.security.DisabledAccountAuthenticationException}
 *       → 403，账号被停用，提示联系管理员；</li>
 *   <li>其余认证异常 → 401，未登录、令牌无效或已过期，提示重新登录。</li>
 * </ul>
 *
 * @author 郑志雄 2520145325
 */
@Component
@RequiredArgsConstructor
public class DelegatingAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final DisabledAccountEntryPoint disabledAccountEntryPoint;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (authException instanceof com.zzx.campusorder.common.security
                .DisabledAccountAuthenticationException) {
            disabledAccountEntryPoint.commence(request, response, authException);
        } else {
            authenticationEntryPoint.commence(request, response, authException);
        }
    }
}
