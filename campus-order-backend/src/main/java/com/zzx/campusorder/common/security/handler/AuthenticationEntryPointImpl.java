package com.zzx.campusorder.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzx.campusorder.common.result.ErrorCodeEnum;
import com.zzx.campusorder.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证异常处理器（实验4 任务二 步骤3）：处理「未登录或凭证无效」，返回 401。
 *
 * <p>与 {@link AccessDeniedHandlerImpl} 的分工：
 * <ul>
 *   <li>本处理器：认证未通过，不知道「你是谁」→ 401；</li>
 *   <li>无权限处理器：认证已通过但不具备该权限，知道「你是谁」但「你不能做」→ 403。</li>
 * </ul>
 * </p>
 *
 * <p>两个处理器都不抛异常、也不继续调用过滤器链，否则响应会被重复写入。</p>
 *
 * @author 郑志雄 2520145325
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("认证失败，返回 401：{} {}", request.getMethod(), request.getRequestURI());

        // 状态码 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 内容类型与字符编码，避免中文乱码
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> body = Result.failure(ErrorCodeEnum.UNAUTHORIZED, "未登录或登录已过期，请重新登录");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
