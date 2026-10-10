package com.zzx.campusorder.feature.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.campusorder.common.exception.BusinessException;
import com.zzx.campusorder.common.result.PageResult;
import com.zzx.campusorder.common.security.Roles;
import com.zzx.campusorder.common.security.UserContext;
import com.zzx.campusorder.feature.dish.entity.Dish;
import com.zzx.campusorder.feature.dish.mapper.DishMapper;
import com.zzx.campusorder.feature.order.dto.OrderCreateRequest;
import com.zzx.campusorder.feature.order.dto.OrderRateRequest;
import com.zzx.campusorder.feature.order.dto.OrderStatusRequest;
import com.zzx.campusorder.feature.order.entity.Order;
import com.zzx.campusorder.feature.order.entity.OrderItem;
import com.zzx.campusorder.feature.order.entity.OrderStatus;
import com.zzx.campusorder.feature.order.mapper.OrderItemMapper;
import com.zzx.campusorder.feature.order.mapper.OrderMapper;
import com.zzx.campusorder.feature.order.vo.OrderVO;
import com.zzx.campusorder.feature.user.entity.User;
import com.zzx.campusorder.feature.user.mapper.UserMapper;
import com.zzx.campusorder.feature.window.entity.CanteenWindow;
import com.zzx.campusorder.feature.window.mapper.CanteenWindowMapper;
import com.zzx.campusorder.feature.window.service.WindowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单业务：提交、查询、取消、商家接单备餐核码、评价。
 *
 * <p>核心约束：
 * <ol>
 *   <li>提交订单：服务端重新计算金额，库存扣减与订单写入处于同一事务，失败整体回滚；</li>
 *   <li>幂等：user_id + request_no 联合唯一，重复提交直接返回原订单，不重复扣库存；</li>
 *   <li>取消与接单竞争同一初始状态，使用带 WHERE status = 0 的条件更新，只有一方成功；</li>
 *   <li>越权防护：用户只能操作本人订单，商家只能操作本人所属窗口的订单。</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final DishMapper dishMapper;
    private final UserMapper userMapper;
    private final CanteenWindowMapper windowMapper;
    private final WindowService windowService;

    // ==================================================================================
    // 一、用户侧：提交订单、查询、取消、评价
    // ==================================================================================

    /**
     * 提交订单入口：负责幂等前置检查与并发唯一约束冲突后的兜底查询。
     *
     * <p>该方法本身不开启事务，真正的写库逻辑在 {@link #doCreateOrder} 中，
     * 这样并发冲突导致的事务回滚不会把外层事务一起标记为回滚。</p>
     */
    public OrderVO createOrder(OrderCreateRequest request) {
        Roles.requireStudent();
        Long userId = UserContext.requireUserId();

        // 步骤 1：幂等检查。命中则直接返回原订单，不重复扣库存
        Order existed = findByIdempotentKey(userId, request.getRequestNo());
        if (existed != null) {
            log.info("幂等命中，直接返回原订单：userId={} requestNo={} orderNo={}",
                    userId, request.getRequestNo(), existed.getOrderNo());
            return toVO(existed, true);
        }
        try {
            return doCreateOrder(request, userId);
        } catch (DuplicateKeyException e) {
            // 并发下两个相同请求同时进入：唯一约束冲突的一方整体回滚，
            // 此处查询已提交的原订单返回，保证“重复提交返回原订单，不重复扣库存”。
            Order concurrent = findByIdempotentKey(userId, request.getRequestNo());
            if (concurrent != null) {
                log.warn("并发重复请求，返回已提交订单：requestNo={}", request.getRequestNo());
                return toVO(concurrent, true);
            }
            throw BusinessException.conflict("重复提交，请稍后重试");
        }
    }

    private Order findByIdempotentKey(Long userId, String requestNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRequestNo, requestNo));
    }

    /**
     * 真正的下单写入：校验、扣库存、写订单与明细全部处于同一事务，任一步失败整体回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO doCreateOrder(OrderCreateRequest request, Long userId) {
        // 步骤 2：合并同一菜品的重复条目，按 dishId 升序排列以减少死锁概率
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        for (OrderCreateRequest.Item item : request.getItems()) {
            quantityMap.merge(item.getDishId(), item.getQuantity(), Integer::sum);
        }
        List<Long> dishIds = quantityMap.keySet().stream().sorted().toList();

        // 步骤 3：校验菜品存在、在售、同一窗口、窗口营业
        List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
        if (dishes.size() != dishIds.size()) {
            throw BusinessException.badRequest("订单中包含不存在的菜品，请刷新后重新选择");
        }
        Map<Long, Dish> dishMap = dishes.stream().collect(Collectors.toMap(Dish::getId, Function.identity()));

        Long windowId = null;
        for (Long dishId : dishIds) {
            Dish dish = dishMap.get(dishId);
            if (dish.getStatus() == null || dish.getStatus() != 1) {
                throw BusinessException.conflict("菜品「" + dish.getDishName() + "」已下架，请重新选择");
            }
            if (windowId == null) {
                windowId = dish.getWindowId();
            } else if (!windowId.equals(dish.getWindowId())) {
                throw BusinessException.badRequest("一个订单只能包含同一窗口的菜品，请分窗口下单");
            }
        }
        CanteenWindow window = windowMapper.selectById(windowId);
        if (window == null || window.getStatus() == null || window.getStatus() != 1) {
            throw BusinessException.conflict("该窗口已停用，无法下单");
        }
        if (window.getBusinessStatus() == null || window.getBusinessStatus() != 1) {
            throw BusinessException.conflict("窗口「" + window.getWindowName() + "」当前已打烊，无法下单");
        }

        // 步骤 4：逐条条件扣减库存，任一条失败即抛异常触发整体回滚
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (Long dishId : dishIds) {
            Dish dish = dishMap.get(dishId);
            int quantity = quantityMap.get(dishId);
            int affected = dishMapper.deductStock(dishId, quantity);
            if (affected != 1) {
                throw BusinessException.conflict("菜品「" + dish.getDishName() + "」库存不足，请调整数量后重新提交");
            }
            BigDecimal amount = dish.getPrice().multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(amount);

            OrderItem item = new OrderItem();
            item.setDishId(dishId);
            item.setDishName(dish.getDishName());
            item.setUnitPrice(dish.getPrice());
            item.setQuantity(quantity);
            item.setAmount(amount);
            items.add(item);
        }
        if (totalAmount.compareTo(MAX_AMOUNT) > 0) {
            throw BusinessException.badRequest("订单金额超出上限，请联系食堂线下处理");
        }

        // 步骤 5：写入订单主表与明细
        Order order = new Order();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setWindowId(windowId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setRequestNo(request.getRequestNo());
        order.setPickupCode(generatePickupCode());
        orderMapper.insert(order);
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        log.info("下单成功：orderNo={} windowId={} amount={}", order.getOrderNo(), windowId, totalAmount);
        // 重新读取订单，确保返回对象包含数据库生成的字段（如 create_time）
        return toVO(orderMapper.selectById(order.getId()), true);
    }

    /**
     * 分页查询订单。
     *
     * @param scope    mine 表示仅本人订单（师生用户），window 表示本人窗口订单（商家），all 表示全平台（管理员）
     * @param status   订单状态过滤
     * @param windowId 指定窗口过滤（管理员）
     */
    public PageResult<OrderVO> pageOrders(String scope, Integer status, Long windowId,
                                          String orderNo, long pageNum, long pageSize) {
        Long userId = UserContext.requireUserId();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(status != null, Order::getStatus, status)
                .like(StringUtils.hasText(orderNo), Order::getOrderNo, orderNo)
                .orderByDesc(Order::getCreateTime)
                .orderByDesc(Order::getId);

        if ("mine".equalsIgnoreCase(scope)) {
            wrapper.eq(Order::getUserId, userId);
        } else if ("window".equalsIgnoreCase(scope)) {
            Roles.requireMerchant();
            CanteenWindow window = windowService.requireMerchantWindow();
            wrapper.eq(Order::getWindowId, window.getId());
        } else {
            Roles.requireAdmin();
            wrapper.eq(windowId != null, Order::getWindowId, windowId);
        }

        Page<Order> page = new Page<>(pageNum, pageSize);
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(),
                toVOList(result.getRecords(), false));
    }

    /**
     * 查询订单详情，包含明细。越权访问返回 403。
     */
    public OrderVO getDetail(String orderNo) {
        Order order = requireOrder(orderNo);
        assertCanView(order);
        return toVO(order, true);
    }

    /**
     * 用户取消订单。只有待接单状态可取消，取消与商家接单竞争同一状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancelOrder(String orderNo, String reason) {
        Roles.requireStudent();
        Long userId = UserContext.requireUserId();
        Order order = requireOrder(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw BusinessException.forbidden("只能取消本人的订单");
        }
        if (order.getStatus() == OrderStatus.CANCELED) {
            // 重复取消：直接返回当前状态，不重复恢复库存
            throw BusinessException.conflict("订单已取消，无需重复操作");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw BusinessException.conflict("订单当前状态为「" + OrderStatus.nameOf(order.getStatus()) + "」，无法取消");
        }

        LocalDateTime now = LocalDateTime.now();
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.PENDING)
                .set(Order::getStatus, OrderStatus.CANCELED)
                .set(Order::getCancelReason, StringUtils.hasText(reason) ? reason : "用户主动取消")
                .set(Order::getUpdateTime, now));
        if (affected != 1) {
            throw BusinessException.conflict("订单已被商家接单，无法取消");
        }
        restoreStock(order.getId());
        log.info("订单已取消并恢复库存：orderNo={} userId={}", orderNo, userId);
        return toVO(orderMapper.selectById(order.getId()), true);
    }

    /**
     * 用户对已完成订单评价，只允许评价一次。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO rateOrder(String orderNo, OrderRateRequest request) {
        Roles.requireStudent();
        Long userId = UserContext.requireUserId();
        Order order = requireOrder(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw BusinessException.forbidden("只能评价本人的订单");
        }
        if (order.getStatus() != OrderStatus.FINISHED) {
            throw BusinessException.conflict("只有已完成的订单可以评价");
        }
        if (order.getRating() != null) {
            throw BusinessException.conflict("该订单已评价，不能重复评价");
        }
        order.setRating(request.getRating());
        order.setRatingComment(request.getComment());
        orderMapper.updateById(order);
        return toVO(order, false);
    }

    // ==================================================================================
    // 二、商家侧：接单、备餐完成、核码取餐、拒单
    // ==================================================================================

    /**
     * 商家更新订单状态。目标状态只允许 1 接单、2 备餐完成、3 核码完成、5 拒单。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO updateStatusByMerchant(String orderNo, OrderStatusRequest request) {
        Roles.requireMerchant();
        Long userId = UserContext.requireUserId();
        Order order = requireOrder(orderNo);
        if (!windowService.belongsToMerchant(order.getWindowId(), userId)) {
            throw BusinessException.forbidden("只能操作本人所属窗口的订单");
        }
        int target = request.getTargetStatus();

        // 终态不允许再次迁移
        if (OrderStatus.isFinal(order.getStatus())) {
            throw BusinessException.conflict("订单已处于终态「" + OrderStatus.nameOf(order.getStatus()) + "」，无法再次流转");
        }
        if (!OrderStatus.canTransfer(order.getStatus(), target)) {
            throw BusinessException.conflict("不允许从「" + OrderStatus.nameOf(order.getStatus())
                    + "」直接流转到「" + OrderStatus.nameOf(target) + "」");
        }
        if (target == OrderStatus.REJECTED && !StringUtils.hasText(request.getReason())) {
            throw BusinessException.badRequest("拒单必须填写原因");
        }
        if (target == OrderStatus.FINISHED && !StringUtils.hasText(request.getPickupCode())) {
            throw BusinessException.badRequest("核码取餐必须提供取餐码");
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Order> update = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, order.getStatus())
                .set(Order::getStatus, target)
                .set(Order::getUpdateTime, now);
        if (target == OrderStatus.PREPARING) {
            update.set(Order::getAcceptTime, now);
        } else if (target == OrderStatus.READY) {
            update.set(Order::getReadyTime, now);
        } else if (target == OrderStatus.FINISHED) {
            update.set(Order::getFinishTime, now);
        } else if (target == OrderStatus.REJECTED) {
            update.set(Order::getRejectReason, request.getReason());
        }
        int affected = orderMapper.update(null, update);
        if (affected != 1) {
            throw BusinessException.conflict("订单状态已被其他操作变更，请刷新后重试");
        }

        if (target == OrderStatus.REJECTED) {
            // 拒单在同一事务内恢复库存
            restoreStock(order.getId());
            log.info("商家拒单并恢复库存：orderNo={} reason={}", orderNo, request.getReason());
        } else if (target == OrderStatus.FINISHED) {
            log.info("核码取餐完成：orderNo={} operator={}", orderNo, userId);
        }
        return toVO(orderMapper.selectById(order.getId()), true);
    }

    /**
     * 核验取餐码：取餐码与订单绑定，错误码不改变状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO verifyPickupCode(String orderNo, String pickupCode) {
        Roles.requireMerchant();
        Long userId = UserContext.requireUserId();
        Order order = requireOrder(orderNo);
        if (!windowService.belongsToMerchant(order.getWindowId(), userId)) {
            throw BusinessException.forbidden("只能核验本人所属窗口的订单");
        }
        if (order.getStatus() == OrderStatus.FINISHED) {
            throw BusinessException.conflict("订单已完成取餐，取餐码已失效");
        }
        if (order.getStatus() != OrderStatus.READY) {
            throw BusinessException.conflict("订单当前状态为「" + OrderStatus.nameOf(order.getStatus())
                    + "」，尚未备餐完成，无法核码");
        }
        if (pickupCode == null || !pickupCode.trim().equals(order.getPickupCode())) {
            throw BusinessException.badRequest("取餐码不正确，请核对后重试");
        }
        OrderStatusRequest request = new OrderStatusRequest();
        request.setTargetStatus(OrderStatus.FINISHED);
        request.setRequestNo("verify-" + order.getOrderNo());
        request.setPickupCode(pickupCode.trim());
        return updateStatusByMerchant(orderNo, request);
    }

    // ==================================================================================
    // 三、内部工具方法
    // ==================================================================================

    /**
     * 恢复订单涉及的全部菜品库存，在调用方的事务内执行。
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            dishMapper.restoreStock(item.getDishId(), item.getQuantity());
        }
    }

    public Order requireOrder(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw BusinessException.notFound("订单不存在：" + orderNo);
        }
        return order;
    }

    /**
     * 查看权限：本人、所属窗口商家、管理员三者之一。
     */
    private void assertCanView(Order order) {
        Long userId = UserContext.requireUserId();
        Integer role = UserContext.requireRole();
        if (Roles.ADMIN.equals(role)) {
            return;
        }
        if (Roles.STUDENT.equals(role)) {
            if (!order.getUserId().equals(userId)) {
                throw BusinessException.forbidden("无权查看他人订单");
            }
            return;
        }
        if (Roles.MERCHANT.equals(role)) {
            if (!windowService.belongsToMerchant(order.getWindowId(), userId)) {
                throw BusinessException.forbidden("无权查看其他窗口的订单");
            }
            return;
        }
        throw BusinessException.forbidden("没有操作权限");
    }

    /** 订单号：时间戳 + 用户编号后两位 + 4 位随机数，保证可读且基本不重复 */
    private String generateOrderNo(Long userId) {
        String suffix = String.format("%02d", userId % 100);
        return LocalDateTime.now().format(ORDER_NO_FORMAT) + suffix
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /** 取餐码：6 位数字，首位不为 0 */
    private String generatePickupCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private List<OrderVO> toVOList(List<Order> orders, boolean withItems) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        return orders.stream().map(o -> toVO(o, withItems)).toList();
    }

    public OrderVO toVO(Order order, boolean withItems) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setWindowId(order.getWindowId());
        vo.setStatus(order.getStatus());
        vo.setStatusName(OrderStatus.nameOf(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPickupCode(order.getPickupCode());
        vo.setRejectReason(order.getRejectReason());
        vo.setCancelReason(order.getCancelReason());
        vo.setRating(order.getRating());
        vo.setRatingComment(order.getRatingComment());
        vo.setAcceptTime(order.getAcceptTime());
        vo.setReadyTime(order.getReadyTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCreateTime(order.getCreateTime());

        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUserRealName(user.getRealName());
        }
        CanteenWindow window = windowMapper.selectById(order.getWindowId());
        if (window != null) {
            vo.setWindowName(window.getWindowName());
        }
        if (withItems) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId())
                    .orderByAsc(OrderItem::getId));
            vo.setItems(items.stream().map(i -> {
                OrderVO.OrderItemVO itemVO = new OrderVO.OrderItemVO();
                itemVO.setDishId(i.getDishId());
                itemVO.setDishName(i.getDishName());
                itemVO.setUnitPrice(i.getUnitPrice());
                itemVO.setQuantity(i.getQuantity());
                itemVO.setAmount(i.getAmount());
                return itemVO;
            }).toList());
        }
        return vo;
    }
}
