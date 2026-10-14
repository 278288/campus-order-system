package com.zzx.campusorder.feature.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息视图对象。不包含密码字段，避免敏感信息外泄。
 */
@Data
public class UserVO implements Serializable {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer role;
    private String roleName;

    /** 角色编码，如 ROLE_ADMIN，与 sys_role_menu 的授权主体一致 */
    private String roleCode;

    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;

    /** 商家绑定的窗口编号，师生用户与管理员为 null */
    private Long windowId;

    /** 商家绑定的窗口名称，便于前端直接展示 */
    private String windowName;
}
