package com.zzx.campusorder.feature.order.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图对象，包含明细、窗口信息与状态中文名。
 */
@Data
public class OrderVO implements Serializable {

    private Long id;
    private String orderNo;
    private Long userId;
    private String userRealName;
    private Long windowId;
    private String windowName;
    private Integer status;
    private String statusName;
    private BigDecimal totalAmount;
    private String pickupCode;
    private String rejectReason;
    private String cancelReason;
    private Integer rating;
    private String ratingComment;
    private LocalDateTime acceptTime;
    private LocalDateTime readyTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;

    /** 订单明细，列表查询时不填充以降低开销 */
    private List<OrderItemVO> items;

    @Data
    public static class OrderItemVO implements Serializable {
        private Long dishId;
        private String dishName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal amount;
    }
}
