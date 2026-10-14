package com.zzx.campusorder.feature.menu.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色菜单关联实体，对应表 sys_role_menu。
 *
 * <p>角色用角色编码（ROLE_STUDENT / ROLE_MERCHANT / ROLE_ADMIN）标识，
 * 与 user.role 数值一一对应，授权关系可直接用 SQL 维护。</p>
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleCode;

    private Long menuId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
