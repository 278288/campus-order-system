package com.zzx.campusorder.feature.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提交订单请求 DTO。
 *
 * <p>金额不由前端传入，服务端按菜品当前单价重新计算；
 * requestNo 由前端生成并在失败重试时保持不变，用于幂等控制。</p>
 */
@Data
public class OrderCreateRequest {

    @NotBlank(message = "请求号不能为空，重复提交必须携带同一请求号")
    @Size(max = 64, message = "请求号不能超过 64 个字符")
    private String requestNo;

    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "菜品编号不能为空")
        private Long dishId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须为正整数")
        private Integer quantity;
    }
}
