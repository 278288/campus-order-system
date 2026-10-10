package com.zzx.campusorder.feature.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求 DTO。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度须为 3~20 个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为 6~32 个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名不能超过 20 个字符")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Size(min = 11, max = 11, message = "手机号必须为 11 位")
    private String phone;

    @Size(max = 50, message = "邮箱不能超过 50 个字符")
    private String email;
}
