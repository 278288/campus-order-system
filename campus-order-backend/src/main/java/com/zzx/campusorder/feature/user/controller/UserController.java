package com.zzx.campusorder.feature.user.controller;

import com.zzx.campusorder.common.result.PageResult;
import com.zzx.campusorder.common.result.Result;
import com.zzx.campusorder.feature.user.dto.AssignRoleRequest;
import com.zzx.campusorder.feature.user.dto.UpdateProfileRequest;
import com.zzx.campusorder.feature.user.service.UserService;
import com.zzx.campusorder.feature.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：个人信息维护与管理员账号管理。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.currentUser());
    }

    /** 修改当前登录用户信息（姓名与邮箱） */
    @PutMapping("/me")
    public Result<UserVO> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success("资料修改成功", userService.updateProfile(request));
    }

    /** 分页查询用户列表，支持关键字与角色过滤，仅管理员可调用 */
    @GetMapping
    public Result<PageResult<UserVO>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer role,
                                           @RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(userService.pageUsers(keyword, role, pageNum, pageSize));
    }

    /** 维护指定账号的角色、所属窗口与启用状态，仅管理员可调用 */
    @PutMapping("/{id}/role")
    public Result<UserVO> assignRole(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest request) {
        return Result.success("账号维护成功", userService.assignRole(id, request));
    }
}
