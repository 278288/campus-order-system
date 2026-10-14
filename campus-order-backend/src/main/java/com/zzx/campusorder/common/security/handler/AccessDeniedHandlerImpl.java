package com.zzx.campusorder.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzx.campusorder.common.result.ErrorCodeEnum;
import com.zzx.campusorder.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 无权限异常处理器（实验4 任务二 步骤3）：处理「已登录但权限不足」，返回 403。
 *
 * <p>实现 {@link AccessDeniedHandler}，重写 handle 方法。
 * 不抛异常、不继续调用过滤器链，避免响应被重复写入。</p>
 *
 * @author 郑志雄 2520145325
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("授权失败，返回 403：{} {}", request.getMethod(), request.getRequestURI());

        // 状态码 403
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> body = Result.failure(ErrorCodeEnum.FORBIDDEN, "没有操作权限，请联系管理员分配权限");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
