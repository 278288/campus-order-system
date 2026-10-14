package com.zzx.campusorder.feature.menu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzx.campusorder.common.exception.BusinessException;
import com.zzx.campusorder.common.security.Roles;
import com.zzx.campusorder.feature.menu.entity.SysMenu;
import com.zzx.campusorder.feature.menu.entity.SysRoleMenu;
import com.zzx.campusorder.feature.menu.mapper.SysMenuMapper;
import com.zzx.campusorder.feature.menu.mapper.SysRoleMenuMapper;
import com.zzx.campusorder.feature.menu.vo.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜单与角色授权业务。
 *
 * @author 郑志雄 2520145325
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    // ==================================================================================
    // 一、菜单树构建
    // ==================================================================================

    /**
     * 构建菜单树。
     *
     * <p>采用「两次遍历 + 映射表」而不是递归（实验4 任务二 步骤1 的要求）：
     * <ol>
     *   <li>第一次遍历：把每个节点按 id 放进映射表，同时把根节点（parentId 为 0 或父节点不在结果集内）收集起来；</li>
     *   <li>第二次遍历：按 id 从映射表取出父节点，直接挂到父节点的 children 上。</li>
     * </ol>
     * 相比递归的优势：
     * <ol>
     *   <li>只遍历两次，复杂度 O(n)，递归在深层级或数据异常成环时会栈溢出；</li>
     *   <li>不需要为每一层重复扫描子节点，避免 O(n²)；</li>
     *   <li>环状引用（父节点指向子孙）不会导致无限递归，只是该节点挂不上任何父节点，可被识别为孤立节点。</li>
     * </ol>
     * </p>
     *
     * @param menus 已按层级与顺序排好序的菜单列表
     */
    public List<MenuTreeVO> buildTree(List<SysMenu> menus) {
        Map<Long, MenuTreeVO> nodeMap = new LinkedHashMap<>();
        List<MenuTreeVO> roots = new ArrayList<>();

        // 第一次遍历：全部转成节点放进映射表，同时挑出根节点
        for (SysMenu menu : menus) {
            MenuTreeVO node = toVO(menu);
            nodeMap.put(node.getId(), node);
            Long parentId = node.getParentId();
            // 父节点为 0，或父节点未被授权（不在本次结果集中）时，该节点作为根节点展示
            if (parentId == null || parentId == 0L || !containsParent(menus, parentId)) {
                roots.add(node);
            }
        }

        // 第二次遍历：按 id 直接挂到父节点上，无需递归查找
        for (MenuTreeVO node : nodeMap.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L) {
                continue;
            }
            MenuTreeVO parent = nodeMap.get(parentId);
            if (parent != null && parent != node) {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private boolean containsParent(List<SysMenu> menus, Long parentId) {
        for (SysMenu menu : menus) {
            if (menu.getId().equals(parentId)) {
                return true;
            }
        }
        return false;
    }

    public MenuTreeVO toVO(SysMenu menu) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPerms(menu.getPerms());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setOrderNum(menu.getOrderNum());
        return vo;
    }

    /**
     * 查询全部菜单（管理端维护用）。
     */
    public List<SysMenu> listAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum)
                .orderByAsc(SysMenu::getId));
    }

    /**
     * 查询指定角色被授权的菜单树。
     */
    public List<MenuTreeVO> treeByRoleCode(String roleCode) {
        return buildTree(menuMapper.selectByRoleCode(roleCode));
    }

    /**
     * 查询指定角色持有的权限标识集合，认证阶段装配权限集合时使用。
     */
    public Set<String> permsByRoleCode(String roleCode) {
        return new LinkedHashSet<>(menuMapper.selectPermsByRoleCode(roleCode));
    }

    // ==================================================================================
    // 二、角色菜单授权
    // ==================================================================================

    /**
     * 角色菜单授权：先删后插 + 事务保护 + 父级目录补全。
     *
     * <p>三个要点（实验4 任务二 步骤2）：
     * <ol>
     *   <li><b>先删后插</b>：授权是「覆盖」语义，先清空该角色的旧关联再写入新关联，
     *       避免增量比对带来的脏数据；</li>
     *   <li><b>事务保护</b>：删除与插入必须原子完成，否则中途失败会留下「权限被清空」
     *       的危险中间态；</li>
     *   <li><b>父级目录补全</b>：只勾选子菜单时，其父目录必须自动补上，
     *       否则前端拿到的树缺少父节点，子菜单会挂不上而丢失。</li>
     * </ol>
     * </p>
     *
     * @param roleCode 角色编码
     * @param menuIds  勾选的菜单编号，可为空表示收回全部授权
     */
    @Transactional(rollbackFor = Exception.class)
    public int grantMenus(String roleCode, List<Long> menuIds) {
        if (roleCode == null || !Roles.isValidRoleCode(roleCode)) {
            throw BusinessException.badRequest("角色编码不合法：" + roleCode);
        }
        Set<Long> finalIds = new LinkedHashSet<>();
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                if (menuId == null) {
                    continue;
                }
                SysMenu menu = menuMapper.selectById(menuId);
                if (menu == null) {
                    throw BusinessException.badRequest("菜单不存在：" + menuId);
                }
                // 父级目录补全：沿 parentId 向上补齐到根，避免只授权子菜单时树上缺父节点
                fillParents(menu, finalIds);
            }
        }

        // 先删后插：两步在同一事务内完成
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleCode, roleCode));
        for (Long menuId : finalIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleCode(roleCode);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
        return finalIds.size();
    }

    /**
     * 自底向上补齐父级目录，直到根节点。
     */
    private void fillParents(SysMenu menu, Set<Long> collected) {
        SysMenu current = menu;
        int guard = 0;
        while (current != null && collected.add(current.getId())) {
            Long parentId = current.getParentId();
            if (parentId == null || parentId == 0L) {
                break;
            }
            // 防御性判断：数据异常成环时最多向上 16 层，避免死循环
            if (++guard > 16) {
                throw BusinessException.badRequest("菜单层级异常，疑似存在循环引用：" + menu.getId());
            }
            current = menuMapper.selectById(parentId);
        }
    }

    /**
     * 查询指定角色已授权的菜单编号，供前端回显勾选状态。
     */
    public List<Long> grantedMenuIds(String roleCode) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleCode, roleCode))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    /**
     * 查询角色授权概览：角色编码、菜单数量、权限数量。
     */
    public List<Map<String, Object>> roleSummary() {
        List<Map<String, Object>> summary = new ArrayList<>();
        for (String roleCode : Roles.ALL_ROLE_CODES) {
            List<Long> menuIds = grantedMenuIds(roleCode);
            Set<String> perms = permsByRoleCode(roleCode);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("roleCode", roleCode);
            row.put("roleName", Roles.nameOfCode(roleCode));
            row.put("menuCount", menuIds.size());
            row.put("permCount", perms.size());
            row.put("perms", perms);
            summary.add(row);
        }
        return summary;
    }
}
