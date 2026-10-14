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
 * 菜单实体，对应表 sys_menu。
 *
 * <p>用一张表承载三类数据（实验4 任务二 步骤1）：
 * 目录（menuType = 1）用于分组、菜单（menuType = 2）对应前端页面、
 * 按钮（menuType = 3）是权限标识的挂载点。</p>
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    /** 目录 */
    public static final int TYPE_DIR = 1;
    /** 菜单 */
    public static final int TYPE_MENU = 2;
    /** 按钮 */
    public static final int TYPE_BUTTON = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单编号，0 表示顶级 */
    private Long parentId;

    private String menuName;

    /** 1 目录，2 菜单，3 按钮 */
    private Integer menuType;

    /** 权限标识，遵循「模块:操作」命名规范，如 user:list */
    private String perms;

    private String path;

    private String component;

    private String icon;

    private Integer orderNum;

    /** 1 显示，0 隐藏 */
    private Integer visible;

    /** 1 正常，0 停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
