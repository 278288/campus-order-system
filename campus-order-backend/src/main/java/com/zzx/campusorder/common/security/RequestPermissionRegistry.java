package com.zzx.campusorder.common.security;

import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 请求路径与所需权限的对照表（实验4 任务二 步骤2）。
 *
 * <p>集中在一处维护，便于审阅「哪些接口受保护、各需要什么权限」，
 * 避免权限要求散落在各个控制器里难以整体核对。</p>
 *
 * <p>匹配规则：按声明顺序匹配，先命中者生效；使用 Ant 风格的通配符。
 * 未在表中出现的路径视为「无路径级权限要求」，此时放行到方法级注解继续判断，
 * 或由控制器内部按数据归属做校验（如只能操作本人订单）。</p>
 *
 * @author 郑志雄 2520145325
 */
public final class RequestPermissionRegistry {

    /**
     * 一条路径权限规则。
     *
     * @param method     请求方法，null 表示不限
     * @param pattern    路径匹配表达式
     * @param permission 所需权限标识，null 表示只要求已认证
     */
    public record Rule(HttpMethod method, String pattern, String permission) {
    }

    private static final List<Rule> RULES = new ArrayList<>();
    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

    static {
        // ---------------- 账号与角色：管理员模块 ----------------
        rule(HttpMethod.GET, "/api/users", "user:list", "查询账号列表");
        rule(HttpMethod.PUT, "/api/users/*/role", "user:edit", "维护账号角色");

        // ---------------- 食堂窗口 ----------------
        rule(HttpMethod.GET, "/api/windows", "window:list", "查询窗口列表");
        rule(HttpMethod.GET, "/api/windows/*", "window:query", "查询窗口详情");
        rule(HttpMethod.POST, "/api/windows", "window:add", "新增窗口");
        rule(HttpMethod.PUT, "/api/windows/*/status", "window:status", "启停窗口");
        // 营业状态由所属商家维护，使用独立权限 window:business，
        // 避免直接复用 window:edit（那会连带放开窗口名称、位置等管理员级修改）
        rule(HttpMethod.PUT, "/api/windows/*/business-status", "window:business", "维护窗口营业状态");
        rule(HttpMethod.PUT, "/api/windows/*", "window:edit", "修改窗口");

        // ---------------- 菜品：商家维护 ----------------
        rule(HttpMethod.GET, "/api/dishes/mine", "dish:list", "查询本窗口菜品");
        rule(HttpMethod.POST, "/api/dishes", "dish:add", "新增菜品");
        rule(HttpMethod.PUT, "/api/dishes/*/status", "dish:status", "菜品上下架");
        rule(HttpMethod.PUT, "/api/dishes/*/stock", "dish:stock", "调整菜品库存");
        rule(HttpMethod.PUT, "/api/dishes/*", "dish:edit", "修改菜品");

        // ---------------- 订单与取餐 ----------------
        rule(HttpMethod.POST, "/api/orders", "order:create", "提交订单");
        rule(HttpMethod.GET, "/api/orders", "order:list", "查询订单列表");
        rule(HttpMethod.PUT, "/api/orders/*/cancel", "order:cancel", "取消订单");
        rule(HttpMethod.PUT, "/api/orders/*/rating", "order:rate", "评价订单");
        rule(HttpMethod.PUT, "/api/orders/*/verify-pickup", "order:pickup", "核验取餐码");
        rule(HttpMethod.PUT, "/api/orders/*/status", "order:accept", "接单与流转状态");
        rule(HttpMethod.GET, "/api/orders/*", "order:query", "查询订单详情");

        // ---------------- 统计 ----------------
        rule(HttpMethod.GET, "/api/stats/orders", "stats:list", "查看统计报表");

        // ---------------- 菜单与授权 ----------------
        // 注意顺序：/api/menus/mine 是「查询本人菜单」，任何已登录用户都可访问，
        // 必须放在 /api/menus/** 之前，否则会被后者的 user:list 要求误伤。
        rule(HttpMethod.GET, "/api/menus/mine", null, "查询本人菜单树（任何已登录用户）");
        rule(HttpMethod.GET, "/api/menus/**", "user:list", "查询菜单");
        rule(HttpMethod.GET, "/api/roles/**", "user:list", "查询角色授权");
        rule(HttpMethod.PUT, "/api/roles/**", "user:edit", "角色菜单授权");
    }

    private RequestPermissionRegistry() {
    }

    private static void rule(HttpMethod method, String pattern, String permission, String description) {
        RULES.add(new Rule(method, pattern, permission));
        DESCRIPTIONS.put(method + " " + pattern, description);
    }

    /**
     * 解析某次请求所需的权限标识。
     *
     * @return 所需权限标识；命中「只要求已认证」的规则时返回空字符串；该路径无规则时返回 null
     */
    public static String resolve(String method, String path) {
        for (Rule rule : RULES) {
            if (rule.method() != null && !rule.method().matches(method)) {
                continue;
            }
            if (matches(rule.pattern(), path)) {
                return rule.permission() == null ? "" : rule.permission();
            }
        }
        return null;
    }

    /** 该次请求是否命中了路径级规则 */
    public static boolean hasRule(String method, String path) {
        return resolve(method, path) != null;
    }

    /**
     * Ant 风格路径匹配，支持 {@code *} 匹配单段、{@code **} 匹配多段。
     */
    static boolean matches(String pattern, String path) {
        if (pattern == null || path == null) {
            return false;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");
        if (patternParts.length != pathParts.length) {
            return false;
        }
        for (int i = 0; i < patternParts.length; i++) {
            String p = patternParts[i];
            if ("*".equals(p) || "**".equals(p)) {
                continue;
            }
            if (!p.equals(pathParts[i])) {
                return false;
            }
        }
        return true;
    }

    /** 全部规则，供接口文档与自测脚本核对 */
    public static List<Rule> allRules() {
        return List.copyOf(RULES);
    }

    public static Map<String, String> descriptions() {
        return Map.copyOf(DESCRIPTIONS);
    }
}
