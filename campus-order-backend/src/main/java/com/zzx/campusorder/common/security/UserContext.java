package com.zzx.campusorder.common.security;

import com.zzx.campusorder.common.exception.BusinessException;
import lombok.Data;

/**
 * 当前登录用户上下文。由 {@link AuthInterceptor} 校验令牌后写入，Controller 与 Service 只读取。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 取当前登录用户编号。若上下文为空，说明该请求路径被错误地放进了免鉴权白名单，
     * 此时按 401 处理而不是抛出 500，同时暴露配置问题。
     */
    public static Long requireUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BusinessException.unauthorized("该接口需要登录后访问，请先登录");
        }
        return user.getUserId();
    }

    public static Integer requireRole() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BusinessException.unauthorized("该接口需要登录后访问，请先登录");
        }
        return user.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Data
    public static class LoginUser {
        private Long userId;
        private String username;
        private Integer role;

        public LoginUser() {
        }

        public LoginUser(Long userId, String username, Integer role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }
    }
}
