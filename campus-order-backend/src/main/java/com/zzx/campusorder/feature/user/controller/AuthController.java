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
 * 认证接口：注册与登录。
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
     * 用户登录，返回访问令牌与用户信息。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", userService.login(request));
    }
}
