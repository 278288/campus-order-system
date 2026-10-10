package com.zzx.campusorder.feature.order.controller;

import com.zzx.campusorder.common.result.PageResult;
import com.zzx.campusorder.common.result.Result;
import com.zzx.campusorder.feature.order.dto.OrderCancelRequest;
import com.zzx.campusorder.feature.order.dto.OrderCreateRequest;
import com.zzx.campusorder.feature.order.dto.OrderRateRequest;
import com.zzx.campusorder.feature.order.dto.OrderStatusRequest;
import com.zzx.campusorder.feature.order.service.OrderService;
import com.zzx.campusorder.feature.order.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口：用户下单与取消、商家接单备餐核码、管理员查询。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 提交订单。请求号由前端生成，重复提交携带同一请求号即可获得幂等保护 */
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success("下单成功", orderService.createOrder(request));
    }

    /** 分页查询订单列表 */
    @GetMapping
    public Result<PageResult<OrderVO>> page(@RequestParam(defaultValue = "mine") String scope,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Long windowId,
                                            @RequestParam(required = false) String orderNo,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(orderService.pageOrders(scope, status, windowId, orderNo, pageNum, pageSize));
    }

    /** 查询订单详情，包含订单明细 */
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        return Result.success(orderService.getDetail(orderNo));
    }

    /** 用户取消尚未接单的订单，成功后同一事务内恢复库存 */
    @PutMapping("/{orderNo}/cancel")
    public Result<OrderVO> cancel(@PathVariable String orderNo,
                                  @RequestBody(required = false) OrderCancelRequest request) {
        String reason = request == null ? null : request.getReason();
        return Result.success("订单已取消", orderService.cancelOrder(orderNo, reason));
    }

    /** 商家更新订单状态：1 接单、2 备餐完成、5 拒单 */
    @PutMapping("/{orderNo}/status")
    public Result<OrderVO> updateStatus(@PathVariable String orderNo,
                                        @Valid @RequestBody OrderStatusRequest request) {
        return Result.success("订单状态已更新", orderService.updateStatusByMerchant(orderNo, request));
    }

    /** 商家核验取餐码，核验通过后订单置为已完成 */
    @PutMapping("/{orderNo}/verify-pickup")
    public Result<OrderVO> verifyPickup(@PathVariable String orderNo, @RequestParam String pickupCode) {
        return Result.success("取餐核验成功", orderService.verifyPickupCode(orderNo, pickupCode));
    }

    /** 用户评价已完成订单，只允许评价一次 */
    @PutMapping("/{orderNo}/rating")
    public Result<OrderVO> rate(@PathVariable String orderNo, @Valid @RequestBody OrderRateRequest request) {
        return Result.success("评价成功", orderService.rateOrder(orderNo, request));
    }
}
