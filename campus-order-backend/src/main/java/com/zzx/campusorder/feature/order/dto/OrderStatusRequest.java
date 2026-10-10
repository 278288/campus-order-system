package com.zzx.campusorder.feature.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家更新订单状态请求 DTO。目标状态只允许 1（接单）、2（备餐完成）、3（核码完成）、5（拒单）。
 */
@Data
public class OrderStatusRequest {

    @NotNull(message = "目标状态不能为空")
    private Integer targetStatus;

    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号不能超过 64 个字符")
    private String requestNo;

    /** 拒单原因，targetStatus = 5 时必填 */
    @Size(max = 200, message = "拒单原因不能超过 200 个字符")
    private String reason;

    /** 取餐码，targetStatus = 3 时必填 */
    @Size(max = 6, message = "取餐码为 6 位数字")
    private String pickupCode;
}
