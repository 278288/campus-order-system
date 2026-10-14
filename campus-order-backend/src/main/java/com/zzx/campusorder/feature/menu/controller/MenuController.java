package com.zzx.campusorder.feature.menu.controller;

import com.zzx.campusorder.common.result.Result;
import com.zzx.campusorder.common.security.RequestPermissionRegistry;
import com.zzx.campusorder.common.security.SecurityUtils;
import com.zzx.campusorder.common.security.Roles;
import com.zzx.campusorder.feature.menu.entity.SysMenu;
import com.zzx.campusorder.feature.menu.service.MenuService;
import com.zzx.campusorder.feature.menu.vo.MenuTreeVO;
import com.zzx.campusorder.feature.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单与角色授权接口（实验4 任务二）。
 *
 * <p>提供四类能力：查询当前用户可见菜单树、查询全部菜单、查询角色授权概览、
 * 为角色授权菜单。授权接口遵循「先删后插 + 事务保护 + 父级目录补全」。</p>
 *
 * @author 郑志雄 2520145325
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final UserService userService;

    /**
     * 查询当前登录用户可见的菜单树。
     * 前端用它渲染导航；菜单树只影响展示，接口鉴权始终独立进行。
     */
    @GetMapping("/menus/mine")
    public Result<List<MenuTreeVO>> myMenus() {
        String roleCode = SecurityUtils.currentRoleCode(SecurityUtils.requireUser());
        if (roleCode == null) {
            return Result.success(new ArrayList<>());
        }
        return Result.success(menuService.treeByRoleCode(roleCode));
    }

    /** 查询全部菜单（含按钮），供授权页面渲染勾选树，需要 user:list 权限 */
    @GetMapping("/menus")
    @PreAuthorize("@ss.hasPerm('user:list')")
    public Result<List<MenuTreeVO>> allMenus() {
        return Result.success(menuService.buildTree(menuService.listAll()));
    }

    /** 查询角色授权概览：每个角色已授权的菜单数与权限数 */
    @GetMapping("/roles")
    @PreAuthorize("@ss.hasPerm('user:list')")
    public Result<List<Map<String, Object>>> roles() {
        return Result.success(menuService.roleSummary());
    }

    /** 查询某角色已授权的菜单编号，供前端回显勾选状态 */
    @GetMapping("/roles/{roleCode}/menus")
    @PreAuthorize("@ss.hasPerm('user:list')")
    public Result<Map<String, Object>> grantedMenus(@PathVariable String roleCode) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roleCode", roleCode);
        data.put("menuIds", menuService.grantedMenuIds(roleCode));
        data.put("perms", menuService.permsByRoleCode(roleCode));
        return Result.success(data);
    }

    /**
     * 角色菜单授权：先删后插、事务保护、父级目录补全。
     * 授权完成后立即刷新该角色下在线用户的缓存，使权限变更即时生效。
     */
    @PutMapping("/roles/{roleCode}/menus")
    @PreAuthorize("@ss.hasPerm('user:edit')")
    public Result<Map<String, Object>> grantMenus(@PathVariable String roleCode,
                                                  @RequestBody MenuGrantRequest request) {
        int granted = menuService.grantMenus(roleCode, request.getMenuIds());
        int refreshed = userService.refreshCacheByRoleCode(roleCode);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roleCode", roleCode);
        data.put("roleName", Roles.nameOfCode(roleCode));
        data.put("grantedMenus", granted);
        data.put("refreshedSessions", refreshed);
        data.put("perms", menuService.permsByRoleCode(roleCode));
        return Result.success("授权成功", data);
    }

    /**
     * 查询路径级权限规则，便于审阅「哪些接口受保护、各需要什么权限」。
     */
    @GetMapping("/menus/request-rules")
    @PreAuthorize("@ss.hasPerm('user:list')")
    public Result<List<Map<String, String>>> requestRules() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (RequestPermissionRegistry.Rule rule : RequestPermissionRegistry.allRules()) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("method", rule.method() == null ? "ANY" : rule.method().name());
            row.put("pattern", rule.pattern());
            row.put("permission", rule.permission());
            row.put("description", RequestPermissionRegistry.descriptions()
                    .getOrDefault(rule.method() + " " + rule.pattern(), ""));
            rows.add(row);
        }
        return Result.success(rows);
    }

    /** 供管理员维护菜单时使用的菜单实体列表 */
    @GetMapping("/menus/entities")
    @PreAuthorize("@ss.hasPerm('user:list')")
    public Result<List<SysMenu>> menuEntities(@RequestParam(required = false) Integer menuType) {
        List<SysMenu> all = menuService.listAll();
        if (menuType == null) {
            return Result.success(all);
        }
        return Result.success(all.stream().filter(m -> menuType.equals(m.getMenuType())).toList());
    }

    /** 角色菜单授权请求体 */
    @lombok.Data
    public static class MenuGrantRequest {
        /** 勾选的菜单编号集合；传空表示收回该角色的全部授权 */
        private List<Long> menuIds;
    }
}
