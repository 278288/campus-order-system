package com.zzx.campusorder.feature.user.controller;

import com.zzx.campusorder.common.result.Result;
import com.zzx.campusorder.feature.user.dto.LoginRequest;
import com.zzx.campusorder.feature.user.dto.RegisterRequest;
import com.zzx.campusorder.feature.user.service.UserService;
import com.zzx.campusorder.feature.user.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册、登录、退出（实验4 任务一 步骤6、步骤8）。
 *
 * <p>注册与登录在安全配置的白名单中放行；退出需要携带有效令牌，
 * 因为要依据当前身份定位 Redis 中的缓存键。</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册。注册成功的账号固定为师生用户角色。
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success("注册成功", userService.register(request));
    }

    /**
     * 用户登录，返回访问令牌、用户信息与权限标识集合。
     * 认证由 AuthenticationManager 完成；失败统一提示「账号或密码错误」。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", userService.login(request));
    }

    /**
     * 退出登录：删除 Redis 中的用户缓存并清空安全上下文。
     * 缓存删除后，该令牌即便签名有效、时间未到也取不到身份信息，等同于立即失效。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success("退出成功", null);
    }
}
