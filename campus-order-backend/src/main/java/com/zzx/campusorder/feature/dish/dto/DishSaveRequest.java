package com.zzx.campusorder.feature.dish.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜品新增与修改请求 DTO。商家只能操作本人所属窗口的菜品，窗口由服务端根据登录身份判定。
 */
@Data
public class DishSaveRequest {

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 40, message = "菜品名称不能超过 40 个字符")
    private String dishName;

    @NotBlank(message = "菜品分类不能为空")
    @Size(max = 20, message = "菜品分类不能超过 20 个字符")
    private String category;

    @NotNull(message = "菜品价格不能为空")
    @DecimalMin(value = "0.01", message = "菜品价格必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "菜品价格最多 8 位整数与 2 位小数")
    private BigDecimal price;

    @NotNull(message = "菜品库存不能为空")
    @Min(value = 0, message = "库存必须为非负整数")
    @Max(value = 100000, message = "库存不能超过 100000")
    private Integer stock;

    @Size(max = 200, message = "菜品简介不能超过 200 个字符")
    private String description;

    @Size(max = 255, message = "图片地址不能超过 255 个字符")
    private String imageUrl;

    @NotNull(message = "上下架状态不能为空")
    @Min(value = 0, message = "上下架状态只能是 0 或 1")
    @Max(value = 1, message = "上下架状态只能是 0 或 1")
    private Integer status;
}
