package com.zzx.campusorder.feature.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.campusorder.feature.menu.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联持久层。
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
