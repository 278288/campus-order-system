package com.zzx.campusorder.feature.user.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.campusorder.common.exception.BusinessException;
import com.zzx.campusorder.common.result.PageResult;
import com.zzx.campusorder.common.security.JwtUtil;
import com.zzx.campusorder.common.security.Roles;
import com.zzx.campusorder.common.security.UserContext;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户与权限业务：注册、登录、个人资料维护、管理员账号管理。
 *
 * <p>安全约定：密码一律使用 BCrypt 带盐哈希存储；角色与权限以数据库为准，
 * 所有管理类操作在服务端强制校验角色，不依赖前端按钮隐藏。</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final WindowManagerMapper windowManagerMapper;
    private final CanteenWindowMapper windowMapper;
    private final WindowService windowService;
    private final JwtUtil jwtUtil;

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
        user.setPassword(BCrypt.withDefaults().hashToString(10, request.getPassword().toCharArray()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(Roles.STUDENT);
        user.setStatus(1);
        userMapper.insert(user);

        return buildLoginVO(user);
    }

    /**
     * 用户登录。校验账号状态与密码，成功后签发访问令牌并记录登录时间。
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        // 账号不存在与密码错误返回同一提示，避免账号枚举
        if (user == null || !BCrypt.verifyer()
                .verify(request.getPassword().toCharArray(), user.getPassword()).verified) {
            throw BusinessException.unauthorized("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.forbidden("账号已被停用，请联系管理员");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        return buildLoginVO(user);
    }

    private LoginVO buildLoginVO(User user) {
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()));
        vo.setExpireMinutes(jwtUtil.getExpireMinutes());
        vo.setUser(toVO(user));
        return vo;
    }

    /**
     * 查询当前登录用户信息。
     */
    public UserVO currentUser() {
        User user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw BusinessException.unauthorized("账号不存在，请重新登录");
        }
        return toVO(user);
    }

    /**
     * 修改当前登录用户的昵称与邮箱。手机号与角色不允许自行修改。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(UpdateProfileRequest request) {
        User user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw BusinessException.unauthorized("账号不存在，请重新登录");
        }
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        userMapper.updateById(user);
        return toVO(user);
    }

    /**
     * 管理员分页查询用户列表。
     */
    public PageResult<UserVO> pageUsers(String keyword, Integer role, long pageNum, long pageSize) {
        Roles.requireAdmin();
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
        Roles.requireAdmin();
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw BusinessException.notFound("用户不存在：" + userId);
        }
        Long operatorId = UserContext.requireUserId();
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
        return toVO(userMapper.selectById(userId));
    }

    /**
     * 实体转视图对象，顺便补全商家绑定的窗口信息。
     */
    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setRoleName(Roles.nameOf(user.getRole()));
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
