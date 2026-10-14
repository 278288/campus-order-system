package com.zzx.campusorder.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzx.campusorder.feature.menu.service.MenuService;
import com.zzx.campusorder.feature.user.entity.User;
import com.zzx.campusorder.feature.user.mapper.UserMapper;
import com.zzx.campusorder.feature.window.entity.WindowManager;
import com.zzx.campusorder.feature.window.mapper.WindowManagerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户详情服务（实验4 任务一 步骤4）。
 *
 * <p>按六步装配 {@link DbUserDetails}：
 * <ol>
 *   <li>按登录账号查询用户；</li>
 *   <li>判断账号是否存在；</li>
 *   <li>判断账号是否被禁用；</li>
 *   <li>由角色数值换算出角色编码集合；</li>
 *   <li>查询该角色持有的权限标识集合（管理员持有通配符，直接放行全部）；</li>
 *   <li>装配用户详情对象返回，并补上商家绑定的窗口编号。</li>
 * </ol>
 * </p>
 *
 * @author 郑志雄 2520145325
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final WindowManagerMapper windowManagerMapper;
    private final MenuService menuService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 步骤1：按登录账号查询用户。user 表未使用逻辑删除列，此处按账号精确查询
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        // 步骤2：账号不存在。统一抛出同一异常，不区分「账号不存在」与「密码错误」
        if (user == null) {
            log.debug("认证失败：账号不存在 username={}", username);
            throw new UsernameNotFoundException("账号或密码错误");
        }

        // 步骤3：账号状态不在此处拦截。
        // 若在这里直接抛 UsernameNotFoundException，框架的 DisabledException 分支永远不会触发，
        // 「账号被停用」会被误报成「账号或密码错误」（401）。
        // 正确做法是照常装配用户详情，由 isEnabled() 返回 false，
        // 交给 DaoAuthenticationProvider 抛出 DisabledException，从而返回 403 并给出明确提示。
        int status = user.getStatus() == null ? 0 : user.getStatus();

        // 步骤4：数值角色换算成角色编码集合
        Set<String> roleCodes = new LinkedHashSet<>();
        String roleCode = Roles.codeOf(user.getRole());
        if (roleCode == null) {
            log.warn("账号角色取值异常 username={} role={}", username, user.getRole());
            throw new UsernameNotFoundException("账号角色配置异常，请联系管理员");
        }
        roleCodes.add(roleCode);

        // 步骤5：查询权限标识集合。管理员直接放入通配符，避免逐条比对
        Set<String> permissions;
        if (Roles.CODE_ADMIN.equals(roleCode)) {
            permissions = new LinkedHashSet<>();
            permissions.add(DbUserDetails.SUPER_PERMISSION);
        } else {
            permissions = menuService.permsByRoleCode(roleCode);
        }
        // 权限为空时保持空集合而不是 null，避免后续比对时层层判空
        if (permissions == null) {
            permissions = new LinkedHashSet<>();
        }

        // 步骤6：装配用户详情对象
        DbUserDetails details = new DbUserDetails();
        details.setUserId(user.getId());
        details.setUsername(user.getUsername());
        details.setPassword(user.getPassword());
        details.setRealName(user.getRealName());
        details.setStatus(status);
        details.setRoleCodes(roleCodes);
        details.setPermissions(permissions);

        // 商家角色补上所属窗口编号，业务层据此做数据范围校验
        if (Roles.CODE_MERCHANT.equals(roleCode)) {
            List<WindowManager> relations = windowManagerMapper.selectList(
                    new LambdaQueryWrapper<WindowManager>()
                            .eq(WindowManager::getUserId, user.getId())
                            .orderByAsc(WindowManager::getId));
            if (!relations.isEmpty()) {
                details.setWindowId(relations.get(0).getWindowId());
            }
        }
        return details;
    }

    /**
     * 按用户 ID 重新装配用户详情。
     *
     * <p>用于角色或菜单授权变更后刷新缓存：管理员改了某角色的权限，
     * 已经登录的用户重新读一次即可拿到最新权限，无需等令牌过期。</p>
     */
    public DbUserDetails loadByUserId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return (DbUserDetails) loadUserByUsername(user.getUsername());
    }
}
