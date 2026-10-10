package com.zzx.campusorder.feature.order.entity;

/**
 * 订单状态常量与迁移规则。
 *
 * <p>主路径：待接单(0) → 备餐中(1) → 待取餐(2) → 已完成(3)。
 * 两个终止分支：待接单(0) → 已取消(4)（用户操作）、待接单(0) → 已拒绝(5)（商家操作）。
 * 三个终态均不允许再次迁移。</p>
 */
public final class OrderStatus {

    /** 待接单：用户已提交，等待商家处理 */
    public static final int PENDING = 0;
    /** 备餐中：商家已接单 */
    public static final int PREPARING = 1;
    /** 待取餐：商家备餐完成，等待用户凭取餐码领取 */
    public static final int READY = 2;
    /** 已完成：取餐码核验通过 */
    public static final int FINISHED = 3;
    /** 已取消：用户主动取消 */
    public static final int CANCELED = 4;
    /** 已拒绝：商家拒单 */
    public static final int REJECTED = 5;

    private OrderStatus() {
    }

    public static String nameOf(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case PENDING -> "待接单";
            case PREPARING -> "备餐中";
            case READY -> "待取餐";
            case FINISHED -> "已完成";
            case CANCELED -> "已取消";
            case REJECTED -> "已拒绝";
            default -> "未知";
        };
    }

    /** 是否为终态：终态不允许再次迁移 */
    public static boolean isFinal(Integer status) {
        return status != null && (status == FINISHED || status == CANCELED || status == REJECTED);
    }

    /** 校验目标状态是否为合法迁移 */
    public static boolean canTransfer(Integer from, Integer to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == PENDING) {
            return to == PREPARING || to == CANCELED || to == REJECTED;
        }
        if (from == PREPARING) {
            return to == READY;
        }
        if (from == READY) {
            return to == FINISHED;
        }
        return false;
    }
}
