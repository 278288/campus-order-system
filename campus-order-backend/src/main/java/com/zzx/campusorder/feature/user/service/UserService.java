package com.zzx.campusorder.feature.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.campusorder.common.cache.CacheService;
import com.zzx.campusorder.common.exception.BusinessException;
import com.zzx.campusorder.common.result.PageResult;
import com.zzx.campusorder.common.security.DbUserDetails;
import com.zzx.campusorder.common.security.DbUserDetailsService;
import com.zzx.campusorder.common.security.JwtUtil;
import com.zzx.campusorder.common.security.Roles;
import com.zzx.campusorder.common.security.SecurityUtils;
import com.zzx.campusorder.feature.user.dto.AssignRoleRequest;
import com.zzx.campusorder.feature.user.dto.LoginRequest;
import com.zzx.campusorder.feature.user.dto.RegisterRequest;
import com.zzx.campusorder.feature.user.dto.UpdateProfileRequest;
import com.zzx.campusorder.feature.user.entity.User;
import com.zzx.campusorder.feature.user.mapper.UserMapper;
import com.zzx.campusorder.feature.user.vo.LoginVO;
import com.zzx.campusorder.feature.user.vo.UserVO;
import com.zzx.campusorder.feature.window.entity.CanteenWindow;
import com.zzx.campusorder.feature.window.entity.WindowManager;
import com.zzx.campusorder.feature.window.mapper.CanteenWindowMapper;
import com.zzx.campusorder.feature.window.mapper.WindowManagerMapper;
import com.zzx.campusorder.feature.window.service.WindowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 用户与权限业务：注册、登录、退出、个人资料维护、管理员账号管理。
 *
 * <p>实验4 改造要点：
 * <ol>
 *   <li>登录不再自行比对密码，改为把账号密码交给 {@link AuthenticationManager} 完成认证，
 *       框架会自动调用 {@link DbUserDetailsService} 装配角色与权限；</li>
 *   <li>认证成功后把完整用户信息写入 Redis，键为 {@code login:user:{userId}}，
 *       过期时间与令牌有效期一致；签发令牌时载荷只放用户 ID；</li>
 *   <li>退出登录删除该缓存并清空安全上下文，令牌即便未过期也随即失效；</li>
 *   <li>角色变更后刷新受影响用户的缓存，使权限变更立即生效而不必等令牌过期。</li>
 * </ol>
 * </p>
 *
 * @author 郑志雄 2520145325
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final WindowManagerMapper windowManagerMapper;
    private final CanteenWindowMapper windowMapper;
    private final WindowService windowService;
    private final JwtUtil jwtUtil;
    private final CacheService cacheService;
    private final DbUserDetailsService dbUserDetailsService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    // ==================================================================================
    // 一、注册与登录
    // ==================================================================================

    /**
     * 用户注册。注册账号固定为师生用户角色，商家与管理员账号由管理员维护。
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterRequest request) {
        Long sameName = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (sameName != null && sameName > 0) {
            throw BusinessException.conflict("用户名已被占用：" + request.getUsername());
        }
        Long samePhone = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone()));
        if (samePhone != null && samePhone > 0) {
            throw BusinessException.conflict("手机号已被注册：" + request.getPhone());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        // 密码统一使用与 Spring Security 相同的 BCrypt 编码器，保证认证链可直接比对
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(Roles.STUDENT);
        user.setStatus(1);
        userMapper.insert(user);

        // 注册成功后直接完成一次登录流程，返回令牌与权限
        return issueToken(dbUserDetailsService.loadByUserId(user.getId()), user);
    }

    /**
     * 用户登录（实验4 任务一 步骤6），按七步实现。
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginRequest request) {
        // 步骤1：基本格式校验由 DTO 注解完成，此处兜底
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw BusinessException.badRequest("账号与密码不能为空");
        }

        // 步骤2：构造未认证的认证请求对象
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

        // 步骤3：交给认证管理器完成认证，框架会自动调用 UserDetailsService
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (DisabledException e) {
            // 账号被停用：给出明确提示，便于用户联系管理员
            throw BusinessException.forbidden("账号已被停用，请联系管理员");
        } catch (BadCredentialsException e) {
            // 账号不存在与密码错误统一转为同一提示，避免提示差异被用来探测账号是否存在
            throw BusinessException.unauthorized("账号或密码错误");
        } catch (AuthenticationException e) {
            log.warn("登录认证失败 username={} reason={}", request.getUsername(), e.getMessage());
            throw BusinessException.unauthorized("账号或密码错误");
        }

        // 步骤4：从认证结果取出已装配角色与权限的用户详情
        DbUserDetails details = (DbUserDetails) authentication.getPrincipal();

        // 记录最近登录时间
        User user = userMapper.selectById(details.getUserId());
        if (user != null) {
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);
        }

        // 步骤5～7：写入 Redis、签发令牌、组装响应
        return issueToken(details, user);
    }

    /**
     * 写入缓存、签发令牌并组装登录响应。
     */
    private LoginVO issueToken(DbUserDetails details, User user) {
        if (details == null) {
            throw BusinessException.unauthorized("账号或密码错误");
        }
        // 步骤5：把用户详情以用户 ID 为键写入 Redis，过期时间与令牌有效期保持一致。
        // 写入前清空密码：登录完成后认证链不再需要密码，避免凭据长期留在缓存中。
        details.setPassword(null);
        cacheService.set(cacheService.loginUserKey(details.getUserId()), details,
                Duration.ofSeconds(jwtUtil.getExpireSeconds()));

        // 步骤6：签发令牌，载荷中只放用户 ID
        String token = jwtUtil.generateToken(details.getUserId());

        // 步骤7：组装响应：令牌、有效期、用户信息、角色与权限标识列表
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtUtil.getExpireSeconds());
        vo.setExpireMinutes(jwtUtil.getExpireMinutes());
        vo.setUser(toVO(user == null ? userMapper.selectById(details.getUserId()) : user));
        vo.setRoles(new ArrayList<>(details.getRoleCodes()));
        vo.setPermissions(new ArrayList<>(details.getPermissions()));
        return vo;
    }

    /**
     * 退出登录（实验4 任务一 步骤8），按三步实现。
     */
    public void logout() {
        // 步骤1：从安全上下文取出当前已认证的用户信息
        DbUserDetails details = SecurityUtils.currentUserOrNull();
        if (details != null) {
            // 步骤2：按与登录时完全一致的规则拼出缓存键并删除
            boolean removed = cacheService.delete(cacheService.loginUserKey(details.getUserId()));
            log.info("用户退出登录 userId={} 缓存已清除={}", details.getUserId(), removed);
        }
        // 步骤3：清空安全上下文
        SecurityContextHolder.clearContext();
    }

    // ==================================================================================
    // 二、当前用户信息
    // ==================================================================================

    /**
     * 查询当前登录用户信息，并附带角色与权限集合。
     */
    public LoginVO currentUserWithAuthorities() {
        DbUserDetails details = SecurityUtils.requireUser();
        User user = userMapper.selectById(details.getUserId());
        if (user == null) {
            throw BusinessException.unauthorized("账号不存在，请重新登录");
        }
        LoginVO vo = new LoginVO();
        vo.setUser(toVO(user));
        vo.setRoles(new ArrayList<>(details.getRoleCodes()));
        vo.setPermissions(new ArrayList<>(details.getPermissions()));
        return vo;
    }

    /**
     * 修改当前登录用户的昵称与邮箱。手机号与角色不允许自行修改。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(UpdateProfileRequest request) {
        User user = userMapper.selectById(SecurityUtils.requireUserId());
        if (user == null) {
            throw BusinessException.unauthorized("账号不存在，请重新登录");
        }
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        userMapper.updateById(user);
        return toVO(user);
    }

    // ==================================================================================
    // 三、管理员账号管理
    // ==================================================================================

    /**
     * 管理员分页查询用户列表。
     */
    public PageResult<UserVO> pageUsers(String keyword, Integer role, long pageNum, long pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(role != null, User::getRole, role)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getUsername, keyword).or()
                        .like(User::getRealName, keyword).or()
                        .like(User::getPhone, keyword))
                .orderByAsc(User::getId);
        Page<User> result = userMapper.selectPage(page, wrapper);
        List<UserVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    /**
     * 管理员维护账号角色、所属窗口与启用状态。
     *
     * <p>约束：商家必须绑定窗口；禁止停用最后一个可用管理员；不允许管理员停用自己。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO assignRole(Long userId, AssignRoleRequest request) {
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw BusinessException.notFound("用户不存在：" + userId);
        }
        Long operatorId = SecurityUtils.requireUserId();
        if (request.getStatus() == 0 && userId.equals(operatorId)) {
            throw BusinessException.badRequest("不能停用当前登录的管理员账号");
        }
        if (request.getRole() == Roles.MERCHANT) {
            if (request.getWindowId() == null || request.getWindowId() <= 0) {
                throw BusinessException.badRequest("商家账号必须绑定一个食堂窗口");
            }
            CanteenWindow window = windowMapper.selectById(request.getWindowId());
            if (window == null) {
                throw BusinessException.badRequest("绑定的食堂窗口不存在：" + request.getWindowId());
            }
        }
        // 停用或降级最后一个可用管理员会锁死系统
        if (target.getRole() != null && target.getRole() == Roles.ADMIN
                && (request.getRole() != Roles.ADMIN || request.getStatus() == 0)) {
            Long availableAdmins = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, Roles.ADMIN)
                    .eq(User::getStatus, 1)
                    .ne(User::getId, userId));
            if (availableAdmins == null || availableAdmins == 0) {
                throw BusinessException.badRequest("系统必须保留至少一个可用管理员，操作被拒绝");
            }
        }

        target.setRole(request.getRole());
        target.setStatus(request.getStatus());
        userMapper.updateById(target);

        // 角色变更时同步维护窗口授权关系
        if (request.getRole() == Roles.MERCHANT) {
            windowService.bindMerchantWindow(userId, request.getWindowId());
        } else {
            windowService.bindMerchantWindow(userId, 0L);
        }
        // 刷新该用户缓存中的角色与权限，避免其继续使用旧权限直到令牌过期
        refreshCache(userId);
        return toVO(userMapper.selectById(userId));
    }

    /**
     * 按用户 ID 重新装配并刷新 Redis 缓存。
     *
     * <p>账号被停用时**不删除**缓存，而是把停用状态写回：
     * 这样 JWT 过滤器能读到用户详情、在「二次校验账号状态」这一步判定为停用，
     * 从而返回 403（身份合法但被禁用）；若直接删缓存，请求会退化成
     * 「缓存中不存在用户信息」的 401（未登录），语义上不准确。</p>
     */
    public void refreshCache(Long userId) {
        User user = userMapper.selectById(userId);
        String key = cacheService.loginUserKey(userId);
        if (user == null) {
            cacheService.delete(key);
            return;
        }
        if (!cacheService.hasKey(key)) {
            // 该用户当前未登录，无需预置缓存
            return;
        }
        DbUserDetails details = dbUserDetailsService.loadByUserId(userId);
        if (details != null) {
            details.setPassword(null);
            cacheService.set(key, details, Duration.ofSeconds(jwtUtil.getExpireSeconds()));
        }
    }

    /**
     * 刷新某角色下全部在线用户的缓存，用于角色菜单授权变更后立即生效。
     */
    public int refreshCacheByRoleCode(String roleCode) {
        Integer role = Roles.roleOfCode(roleCode);
        if (role == null) {
            return 0;
        }
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, role).eq(User::getStatus, 1));
        int refreshed = 0;
        for (User user : users) {
            if (cacheService.hasKey(cacheService.loginUserKey(user.getId()))) {
                refreshCache(user.getId());
                refreshed++;
            }
        }
        return refreshed;
    }

    /**
     * 当前登录用户持有的权限标识集合。
     */
    public Set<String> currentPermissions() {
        return SecurityUtils.requireUser().getPermissions();
    }

    // ==================================================================================
    // 四、实体转换
    // ==================================================================================

    /**
     * 实体转视图对象，顺便补全商家绑定的窗口信息。
     */
    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        if (user == null) {
            return vo;
        }
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setRoleName(Roles.nameOf(user.getRole()));
        vo.setRoleCode(Roles.codeOf(user.getRole()));
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        if (user.getRole() != null && user.getRole() == Roles.MERCHANT) {
            List<WindowManager> relations = windowManagerMapper.selectList(new LambdaQueryWrapper<WindowManager>()
                    .eq(WindowManager::getUserId, user.getId())
                    .orderByAsc(WindowManager::getId));
            if (!relations.isEmpty()) {
                Long windowId = relations.get(0).getWindowId();
                vo.setWindowId(windowId);
                CanteenWindow window = windowMapper.selectById(windowId);
                vo.setWindowName(window == null ? null : window.getWindowName());
            }
        }
        return vo;
    }
}
