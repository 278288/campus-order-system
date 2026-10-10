package com.zzx.campusorder.feature.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 订单评价请求 DTO，仅允许对已完成订单评价一次。
 */
@Data
public class OrderRateRequest {

    @NotNull(message = "评价星级不能为空")
    @Min(value = 1, message = "评价星级为 1~5")
    @Max(value = 5, message = "评价星级为 1~5")
    private Integer rating;

    @Size(max = 200, message = "评价内容不能超过 200 个字符")
    private String comment;
}
