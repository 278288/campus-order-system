package com.zzx.campusorder.feature.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.campusorder.feature.menu.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单持久层。
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询某角色被授权的全部菜单，按层级与显示顺序排序。
     * 管理员通过 sys_role_menu 中被授权的全量菜单获得权限，无需特判。
     */
    @Select("""
            SELECT m.*
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            WHERE rm.role_code = #{roleCode} AND m.status = 1
            ORDER BY m.parent_id, m.order_num, m.id
            """)
    List<SysMenu> selectByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 查询某角色持有的全部权限标识（仅按钮类数据携带权限标识）。
     * 这是认证阶段装配权限集合的数据来源（实验4 任务一 步骤4）。
     */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            WHERE rm.role_code = #{roleCode}
              AND m.status = 1
              AND m.perms IS NOT NULL
              AND m.perms <> ''
            """)
    List<String> selectPermsByRoleCode(@Param("roleCode") String roleCode);
}
