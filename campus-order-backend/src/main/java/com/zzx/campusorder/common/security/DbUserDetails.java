package com.zzx.campusorder.common.security;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 用户详情模型，同时作为写入 Redis 的缓存对象。
 *
 * <p>设计要求（实验4 任务一 步骤4）：
 * <ol>
 *   <li>实现 {@link UserDetails}，至少包含用户 ID、登录账号、密码、账号状态、角色编码集合、权限标识集合六部分；</li>
 *   <li>权限集合为空时必须是空集合而不是 null，避免后续比对时层层判空；</li>
 *   <li>作为缓存对象必须提供无参构造方法：反序列化时先创建空对象，再逐字段赋值。</li>
 * </ol>
 * </p>
 *
 * @author 郑志雄 2520145325
 */
@Data
@NoArgsConstructor
// UserDetails 的 isEnabled 等派生属性会被序列化成 enabled 等字段，
// 反序列化时忽略这些未知属性，避免缓存读取失败
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class DbUserDetails implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户主键 */
    private Long userId;

    /** 登录账号 */
    private String username;

    /**
     * BCrypt 密码哈希。
     * 写入 Redis 前会被置空：登录完成后认证链不再需要密码，
     * 避免把凭据长期留在缓存里。
     */
    private String password;

    /** 真实姓名，供前端展示 */
    private String realName;

    /** 账号状态：1 正常，0 停用 */
    private Integer status;

    /** 角色编码集合，如 ROLE_STUDENT、ROLE_MERCHANT、ROLE_ADMIN */
    private Set<String> roleCodes = new java.util.LinkedHashSet<>();

    /**
     * 权限标识集合，命名规范为「模块:操作」，如 dish:list、order:accept。
     * 超级管理员持有通配符 {@link #SUPER_PERMISSION}。
     */
    private Set<String> permissions = new java.util.LinkedHashSet<>();

    /** 超级管理员通配权限标识 */
    public static final String SUPER_PERMISSION = "*:*";

    /** 商家绑定的窗口编号，供业务层做数据范围校验 */
    private Long windowId;

    // ------------------------------------------------------------------
    // UserDetails 接口实现
    // ------------------------------------------------------------------

    /**
     * 权限集合 = 角色编码 + 权限标识。
     * 角色编码以 ROLE_ 前缀参与角色判断，权限标识参与权限判断。
     *
     * <p>该方法由 roleCodes 与 permissions 派生而来，必须标注 {@code @JsonIgnore}：
     * 否则 Jackson 会把它当成名为 authorities 的属性写入缓存，
     * 反序列化时因缺少对应 setter 而报
     * "Problem deserializing 'setterless' property (authorities)"，导致缓存读取失败。</p>
     */
    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roleCodes != null) {
            roleCodes.stream().filter(java.util.Objects::nonNull)
                    .forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        }
        if (permissions != null) {
            permissions.stream().filter(java.util.Objects::nonNull)
                    .forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 账号状态为 1 才视为启用；被停用的账号无法通过认证 */
    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    /** 是否为超级管理员：持有通配权限标识 */
    public boolean isSuperAdmin() {
        return permissions != null && permissions.contains(SUPER_PERMISSION);
    }
}
