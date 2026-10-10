package com.zzx.campusorder.common.security;

import com.zzx.campusorder.feature.user.entity.User;
import com.zzx.campusorder.feature.user.mapper.UserMapper;
import com.zzx.campusorder.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器：校验 Authorization 头中的访问令牌，并把登录用户写入 {@link UserContext}。
 * 关键点：除校验令牌签名外，每次请求都回查数据库，确保账号被停用后立即失去访问能力。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader(HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(PREFIX)) {
            throw BusinessException.unauthorized("未携带访问令牌，请先登录");
        }
        String token = header.substring(PREFIX.length()).trim();
        UserContext.LoginUser loginUser = jwtUtil.parseToken(token);

        User user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw BusinessException.unauthorized("账号不存在，请重新登录");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.forbidden("账号已被停用，请联系管理员");
        }
        // 以数据库中的最新角色为准，避免旧令牌沿用已变更的权限
        loginUser.setRole(user.getRole());
        loginUser.setUsername(user.getUsername());
        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
