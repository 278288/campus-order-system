package com.zzx.campusorder.feature.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.campusorder.feature.dish.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 菜品持久层。库存扣减与恢复使用条件更新，避免并发超卖。
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 条件扣减库存：只有当剩余库存不小于购买数量时才成功，返回影响行数用于判断是否超卖。
     */
    @Update("UPDATE dish SET stock = stock - #{quantity}, update_time = NOW() "
            + "WHERE id = #{dishId} AND stock >= #{quantity}")
    int deductStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    /**
     * 恢复库存（取消订单、拒单时调用），同一事务内执行。
     */
    @Update("UPDATE dish SET stock = stock + #{quantity}, update_time = NOW() WHERE id = #{dishId}")
    int restoreStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);
}
