package com.zzx.campusorder.feature.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录成功返回对象：访问令牌 + 用户信息 + 角色与权限集合。
 *
 * <p>权限集合随登录一并下发，前端据此控制按钮与菜单的可见性；
 * 服务端仍会对每次请求独立鉴权，前端隐藏只作为体验优化。</p>
 */
@Data
public class LoginVO implements Serializable {

    /** 访问令牌，载荷只含用户 ID 与时间信息 */
    private String token;

    private String tokenType = "Bearer";

    /** 令牌有效期，单位秒，与 Redis 缓存的过期时间完全一致 */
    private Long expiresIn;

    /** 令牌有效期，单位分钟，便于前端展示 */
    private Long expireMinutes;

    private UserVO user;

    /** 当前用户的角色编码集合，如 ROLE_ADMIN */
    private List<String> roles;

    /** 当前用户的权限标识集合，如 dish:list、order:accept */
    private List<String> permissions;
}
