package com.zzx.campusorder.feature.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.campusorder.feature.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细持久层。
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
