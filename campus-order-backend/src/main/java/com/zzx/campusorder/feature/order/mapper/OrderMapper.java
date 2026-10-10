package com.zzx.campusorder.feature.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzx.campusorder.feature.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单主表持久层。状态迁移统一使用带 WHERE status = 期望旧状态 的条件更新，
 * 由数据库行锁保证“取消与接单竞争”只有一方成功。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
