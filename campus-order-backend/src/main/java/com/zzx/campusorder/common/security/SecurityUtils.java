package com.zzx.campusorder.common.security;

import com.zzx.campusorder.common.exception.BusinessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类。
 *
 * <p>用户身份的最终来源是 {@link SecurityContextHolder} 中的认证对象，
 * 而该对象由 JWT 认证过滤器根据 Redis 缓存装配。
 * 业务层不再自己解析令牌，也不依赖 ThreadLocal，避免与安全框架的状态不同步。</p>
 *
 * @author 郑志雄 2520145325
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 从安全上下文取出当前登录用户详情；未登录或为匿名身份时返回 null。
     *
     * <p>注意必须显式排除匿名身份：过滤器链前段的匿名身份过滤器会放入一个
     * AnonymousAuthenticationToken，只判断「认证对象是否为空」会把它当成已登录。</p>
     */
    public static DbUserDetails currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof DbUserDetails details) {
            return details;
        }
        return null;
    }

    /**
     * 取当前登录用户详情，未登录时抛 401。
     */
    public static DbUserDetails requireUser() {
        DbUserDetails details = currentUserOrNull();
        if (details == null) {
            throw BusinessException.unauthorized("未登录或登录已过期，请重新登录");
        }
        return details;
    }

    /** 取当前登录用户编号 */
    public static Long requireUserId() {
        return requireUser().getUserId();
    }

    /** 取当前登录用户的数值角色 */
    public static Integer currentRole() {
        DbUserDetails details = currentUserOrNull();
        if (details == null) {
            return null;
        }
        return Roles.roleOfCode(currentRoleCode(details));
    }

    /** 取当前登录用户的主角色编码 */
    public static String currentRoleCode(DbUserDetails details) {
        if (details == null || details.getRoleCodes() == null || details.getRoleCodes().isEmpty()) {
            return null;
        }
        return details.getRoleCodes().iterator().next();
    }

    /** 当前用户是否为超级管理员 */
    public static boolean isSuperAdmin() {
        DbUserDetails details = currentUserOrNull();
        return details != null && details.isSuperAdmin();
    }

    /**
     * 当前用户是否持有指定角色编码。
     */
    public static boolean hasRoleCode(String roleCode) {
        DbUserDetails details = currentUserOrNull();
        return details != null && details.getRoleCodes() != null
                && details.getRoleCodes().contains(roleCode);
    }

    /**
     * 当前用户是否持有指定数值角色。
     */
    public static boolean hasRole(Integer role) {
        return hasRoleCode(Roles.codeOf(role));
    }
}
