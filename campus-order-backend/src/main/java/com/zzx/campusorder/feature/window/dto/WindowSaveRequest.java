package com.zzx.campusorder.feature.window.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 食堂窗口新增与修改请求 DTO。
 */
@Data
public class WindowSaveRequest {

    @NotBlank(message = "窗口名称不能为空")
    @Size(max = 50, message = "窗口名称不能超过 50 个字符")
    private String windowName;

    @NotBlank(message = "窗口位置不能为空")
    @Size(max = 100, message = "窗口位置不能超过 100 个字符")
    private String location;

    @NotNull(message = "营业状态不能为空")
    @Min(value = 0, message = "营业状态只能是 0 或 1")
    @Max(value = 1, message = "营业状态只能是 0 或 1")
    private Integer businessStatus;
}
