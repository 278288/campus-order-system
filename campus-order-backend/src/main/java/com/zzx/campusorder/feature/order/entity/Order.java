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
 * 订单主表实体，对应表 orders。状态取值见 {@link OrderStatus}。
 */
@Data
@TableName("orders")
public class Order implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对外订单号，唯一 */
    private String orderNo;

    private Long userId;

    private Long windowId;

    /** 0待接单，1备餐中，2待取餐，3已完成，4已取消，5已拒绝 */
    private Integer status;

    /** 成交总金额，由服务端按菜品单价重新计算 */
    private BigDecimal totalAmount;

    /** 客户端唯一请求号，与 userId 组成联合唯一约束实现幂等 */
    private String requestNo;

    private String pickupCode;

    private String rejectReason;

    private String cancelReason;

    /** 评价星级 1~5，为空表示未评价 */
    private Integer rating;

    private String ratingComment;

    private LocalDateTime acceptTime;

    private LocalDateTime readyTime;

    private LocalDateTime finishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
