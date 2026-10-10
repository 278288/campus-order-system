package com.zzx.campusorder.common.security;

import com.zzx.campusorder.common.exception.BusinessException;

/**
 * 角色常量与权限校验工具。角色取值与数据库 user.role 一致。
 */
public final class Roles {

    /** 师生用户 */
    public static final Integer STUDENT = 0;
    /** 食堂商家 */
    public static final Integer MERCHANT = 1;
    /** 管理员 */
    public static final Integer ADMIN = 2;

    private Roles() {
    }

    public static String nameOf(Integer role) {
        if (role == null) {
            return "未知";
        }
        return switch (role) {
            case 0 -> "师生用户";
            case 1 -> "食堂商家";
            case 2 -> "管理员";
            default -> "未知";
        };
    }

    /**
     * 要求当前登录用户为管理员，否则返回 403。
     */
    public static void requireAdmin() {
        if (!ADMIN.equals(UserContext.requireRole())) {
            throw BusinessException.forbidden("该操作仅管理员可执行");
        }
    }

    /**
     * 要求当前登录用户为商家，否则返回 403。
     */
    public static void requireMerchant() {
        if (!MERCHANT.equals(UserContext.requireRole())) {
            throw BusinessException.forbidden("该操作仅食堂商家可执行");
        }
    }

    /**
     * 要求当前登录用户为师生用户，否则返回 403。
     */
    public static void requireStudent() {
        if (!STUDENT.equals(UserContext.requireRole())) {
            throw BusinessException.forbidden("该操作仅师生用户可执行");
        }
    }
}
