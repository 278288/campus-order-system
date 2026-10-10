package com.zzx.campusorder.feature.stats.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细联查投影：用于热销菜品排行统计。
 */
@Data
public class OrderItemStatVO implements Serializable {

    private Long dishId;
    private String dishName;
    private Long quantity;
    private BigDecimal amount;
}
