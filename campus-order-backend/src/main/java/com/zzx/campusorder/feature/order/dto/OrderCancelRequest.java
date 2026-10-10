package com.zzx.campusorder.feature.order.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户取消订单请求 DTO。
 */
@Data
public class OrderCancelRequest {

    @Size(max = 200, message = "取消原因不能超过 200 个字符")
    private String reason;
}
