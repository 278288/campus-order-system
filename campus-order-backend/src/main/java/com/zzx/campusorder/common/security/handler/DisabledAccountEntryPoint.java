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
 * 账号停用异常处理器：令牌签名有效但账号已被停用时的出口，返回 403。
 *
 * <p>为什么需要它（区别于普通的认证异常处理器）：</p>
 * <ul>
 *   <li>「未登录、令牌无效或已过期」= 不知道你是谁 → 401，由
 *       {@link AuthenticationEntryPointImpl} 处理；</li>
 *   <li>「令牌有效、此前确实登录过，但账号已被管理员停用」= 知道你是谁、但你不能用 → 403。
 *       这两种情况的用户指引完全不同：前者要重新登录，后者要找管理员，混为一谈会误导用户。</li>
 * </ul>
 *
 * <p>实现上必须让异常沿过滤器链抛出、由 ExceptionTranslationFilter 转交给本处理器；
 * 直接在授权管理器里抛业务异常是行不通的——异常会被容器直接转成 500。</p>
 *
 * @author 郑志雄 2520145325
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DisabledAccountEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("账号已被停用，返回 403：{} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> body = Result.failure(ErrorCodeEnum.FORBIDDEN, "账号已被停用，请联系管理员");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
