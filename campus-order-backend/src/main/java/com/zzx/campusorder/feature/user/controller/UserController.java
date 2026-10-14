package com.zzx.campusorder.feature.user.controller;

import com.zzx.campusorder.common.result.PageResult;
import com.zzx.campusorder.common.result.Result;
import com.zzx.campusorder.feature.user.dto.AssignRoleRequest;
import com.zzx.campusorder.feature.user.dto.UpdateProfileRequest;
import com.zzx.campusorder.feature.user.service.UserService;
import com.zzx.campusorder.feature.user.vo.LoginVO;
import com.zzx.campusorder.feature.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：个人信息维护与管理员账号管理。
 *
 * <p><b>方法级授权（实验4 任务二 步骤4）</b>：把原先「仅管理员」的角色硬编码判断
 * 替换为具体权限标识注解。权限标识在 sys_menu 中维护、经 sys_role_menu 授予角色，
 * 后续调整某个角色能做什么只需改授权数据，不必改代码。</p>
 *
 * <p>前置条件：安全配置类中已开启方法级安全注解，否则这些注解会静默失效——
 * 既不报错也不警告，现象是路径级拦截正常但注解完全不起作用。</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 获取当前登录用户信息，含角色与权限标识集合，供前端控制菜单与按钮 */
    @GetMapping("/me")
    public Result<LoginVO> me() {
        return Result.success(userService.currentUserWithAuthorities());
    }

    /** 修改当前登录用户信息（姓名与邮箱） */
    @PutMapping("/me")
    public Result<UserVO> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success("资料修改成功", userService.updateProfile(request));
    }

    /** 分页查询用户列表，需要 user:list 权限 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('user:list')")
    public Result<PageResult<UserVO>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer role,
                                           @RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(userService.pageUsers(keyword, role, pageNum, pageSize));
    }

    /** 维护指定账号的角色、所属窗口与启用状态，需要 user:edit 权限 */
    @PutMapping("/{id}/role")
    @PreAuthorize("@ss.hasPerm('user:edit')")
    public Result<UserVO> assignRole(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest request) {
        return Result.success("账号维护成功", userService.assignRole(id, request));
    }
}
