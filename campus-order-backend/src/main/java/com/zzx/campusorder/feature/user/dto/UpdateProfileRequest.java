package com.zzx.campusorder.feature.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改当前用户资料请求 DTO。
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名不能超过 20 个字符")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱不能超过 50 个字符")
    private String email;
}
