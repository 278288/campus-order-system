package com.zzx.campusorder.feature.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体，对应表 order_item。
 * 其中 dishName 与 unitPrice 是下单时刻的快照，属于有意的反范式设计，
 * 用于保证历史订单金额不随菜品改名或调价而变动。
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long dishId;

    private String dishName;

    private BigDecimal unitPrice;

    private Integer quantity;

    /** 小计金额 = unitPrice × quantity */
    private BigDecimal amount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
