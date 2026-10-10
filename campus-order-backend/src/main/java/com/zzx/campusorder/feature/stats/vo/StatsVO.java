package com.zzx.campusorder.feature.stats.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 统计报表视图对象。
 */
@Data
public class StatsVO implements Serializable {

    /** 统计维度：window 表示单个窗口，all 表示全平台 */
    private String scope;

    /** 统计起止日期，格式 yyyy-MM-dd */
    private String startDate;
    private String endDate;

    /** 有效订单数：不含已取消与已拒绝 */
    private Long validOrderCount;

    /** 已完成订单数 */
    private Long finishedOrderCount;

    /** 已取消与已拒绝订单数 */
    private Long canceledOrderCount;

    /** 有效订单成交金额合计 */
    private BigDecimal validAmount;

    /** 已完成订单成交金额合计 */
    private BigDecimal finishedAmount;

    /** 平均客单价 = 有效金额 / 有效订单数，保留两位小数 */
    private BigDecimal averageAmount;

    /** 按日汇总的明细 */
    private List<DailyStat> dailyStats;

    /** 热销菜品排行（按销量倒序，最多 10 条） */
    private List<DishRank> dishRanking;

    @Data
    public static class DailyStat implements Serializable {
        private String date;
        private Long orderCount;
        private BigDecimal amount;
    }

    @Data
    public static class DishRank implements Serializable {
        private Long dishId;
        private String dishName;
        private Long quantity;
        private BigDecimal amount;
    }
}
