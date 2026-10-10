package com.zzx.campusorder.feature.stats.mapper;

import com.zzx.campusorder.feature.order.entity.Order;
import com.zzx.campusorder.feature.stats.vo.OrderItemStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计报表持久层。按时间范围取出必要字段，聚合计算放在 Service 层完成，保证逻辑可读可测。
 */
@Mapper
public interface StatsMapper {

    /**
     * 查询指定时间范围内（含首尾两天）的订单，可按窗口过滤。
     * 只查询统计需要的字段，避免全表字段加载。
     */
    @Select("""
            <script>
            SELECT id, order_no, window_id, status, total_amount, create_time
            FROM orders
            WHERE create_time &gt;= #{start} AND create_time &lt; DATE_ADD(#{end}, INTERVAL 1 DAY)
              <if test="windowId != null"> AND window_id = #{windowId} </if>
            ORDER BY create_time
            </script>
            """)
    List<Order> selectOrdersBetween(@Param("start") LocalDate start,
                                    @Param("end") LocalDate end,
                                    @Param("windowId") Long windowId);

    /**
     * 查询指定时间范围内有效订单的明细，用于热销菜品排行。
     */
    @Select("""
            <script>
            SELECT i.dish_id AS dishId, i.dish_name AS dishName,
                   SUM(i.quantity) AS quantity, IFNULL(SUM(i.amount), 0) AS amount
            FROM order_item i
            JOIN orders o ON o.id = i.order_id
            WHERE o.create_time &gt;= #{start} AND o.create_time &lt; DATE_ADD(#{end}, INTERVAL 1 DAY)
              AND o.status IN (0, 1, 2, 3)
              <if test="windowId != null"> AND o.window_id = #{windowId} </if>
            GROUP BY i.dish_id, i.dish_name
            ORDER BY quantity DESC
            LIMIT 10
            </script>
            """)
    List<OrderItemStatVO> selectDishRanking(@Param("start") LocalDate start,
                                            @Param("end") LocalDate end,
                                            @Param("windowId") Long windowId);
}
