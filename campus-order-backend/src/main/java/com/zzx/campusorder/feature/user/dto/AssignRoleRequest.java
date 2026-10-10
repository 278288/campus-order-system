package com.zzx.campusorder.feature.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员维护账号角色与状态的请求 DTO。
 */
@Data
public class AssignRoleRequest {

    @NotNull(message = "角色不能为空")
    @Min(value = 0, message = "角色取值只能是 0、1、2")
    @Max(value = 2, message = "角色取值只能是 0、1、2")
    private Integer role;

    @NotNull(message = "窗口编号不能为空，师生用户与管理员请传 0")
    private Long windowId;

    @NotNull(message = "账号状态不能为空")
    @Min(value = 0, message = "账号状态只能是 0 或 1")
    @Max(value = 1, message = "账号状态只能是 0 或 1")
    private Integer status;
}
