package com.zzx.campusorder.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限判断服务（实验4 任务二 步骤1）。
 *
 * <p>注册为名为 {@code ss} 的 Bean，对外提供四类判断：是否拥有指定权限、
 * 是否拥有任意权限、是否拥有指定角色、是否为超级管理员。</p>
 *
 * <p>判断逻辑三步：取当前登录用户（取不到或权限集合为空直接不通过）→
 * 权限集合含通配符则视为超级管理员直接通过 → 否则比对目标权限标识。</p>
 *
 * <p><b>关键约束</b>：路径级授权（{@link PathAuthorizationManager}）与方法注解
 * （{@code @PreAuthorize} 中的 {@code @ss.hasPerm(...)}）必须共用这一处逻辑，
 * 否则规则演进时两处会不一致。</p>
 *
 * @author 郑志雄 2520145325
 */
@Component("ss")
public class PermissionService {

    /** 通配权限标识：持有时视为拥有全部权限 */
    public static final String ALL_PERMISSION = DbUserDetails.SUPER_PERMISSION;

    /**
     * 判断当前用户是否拥有指定权限。
     * 支持传入「模块:*」形式的通配权限，与用户自身的「*:*」一并处理。
     */
    public boolean hasPerm(String permission) {
        return hasPerm(SecurityUtils.currentUserOrNull() == null
                ? null
                : org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication(), permission);
    }

    /**
     * 基于指定认证信息判断是否拥有权限，供路径级授权管理器直接复用。
     */
    public boolean hasPerm(Authentication authentication, String permission) {
        Set<String> authorities = authoritiesOf(authentication);
        // 第一步：未认证或权限为空，直接不通过
        if (authorities.isEmpty()) {
            return false;
        }
        // 第二步：持有通配符说明是超级管理员，直接通过
        if (authorities.contains(ALL_PERMISSION)) {
            return true;
        }
        if (permission == null || permission.isBlank()) {
            return false;
        }
        // 第三步：比对目标权限标识；同时支持「模块:*」形式的通配
        if (authorities.contains(permission)) {
            return true;
        }
        int idx = permission.indexOf(':');
        if (idx > 0) {
            return authorities.contains(permission.substring(0, idx) + ":*");
        }
        return false;
    }

    /**
     * 判断当前用户是否拥有其中任意一个权限。
     */
    public boolean hasAnyPerm(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        Authentication authentication = org.springframework.security.core.context
                .SecurityContextHolder.getContext().getAuthentication();
        return Arrays.stream(permissions).anyMatch(p -> hasPerm(authentication, p));
    }

    /**
     * 判断当前用户是否拥有指定角色编码，如 ROLE_ADMIN。
     */
    public boolean hasRole(String roleCode) {
        Set<String> authorities = authoritiesOf(org.springframework.security.core.context
                .SecurityContextHolder.getContext().getAuthentication());
        return roleCode != null && authorities.contains(roleCode);
    }

    /**
     * 判断当前用户是否为超级管理员。
     */
    public boolean isSuperAdmin() {
        Set<String> authorities = authoritiesOf(org.springframework.security.core.context
                .SecurityContextHolder.getContext().getAuthentication());
        return authorities.contains(ALL_PERMISSION);
    }

    /**
     * 取出认证信息中的全部权限标识（角色编码与权限标识都在其中）。
     */
    public Set<String> authoritiesOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication
                        .AnonymousAuthenticationToken) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
