package com.zzx.campusorder.common.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色常量与角色编码映射。
 *
 * <p>两套表示并存，各有用途（实验4 授权模型）：
 * <ul>
 *   <li><b>数值 role</b>：保存在 user.role 列，取值 0/1/2，用于业务查询与统计；</li>
 *   <li><b>角色编码 roleCode</b>：形如 ROLE_ADMIN，是 sys_role_menu 的授权主体，
 *       也是 Spring Security 中角色判断的依据。</li>
 * </ul>
 * 权限校验不再依赖角色数值的硬编码判断，而是依赖按钮上挂载的「模块:操作」权限标识；
 * 角色只是权限集合的来源，后续给某角色增减权限只需改 sys_role_menu，不必改代码。</p>
 *
 * @author 郑志雄 2520145325
 */
public final class Roles {

    /** 师生用户，user.role = 0 */
    public static final Integer STUDENT = 0;
    /** 食堂商家，user.role = 1 */
    public static final Integer MERCHANT = 1;
    /** 管理员，user.role = 2 */
    public static final Integer ADMIN = 2;

    /** 角色编码：师生用户 */
    public static final String CODE_STUDENT = "ROLE_STUDENT";
    /** 角色编码：食堂商家 */
    public static final String CODE_MERCHANT = "ROLE_MERCHANT";
    /** 角色编码：管理员 */
    public static final String CODE_ADMIN = "ROLE_ADMIN";

    /** 全部角色编码，供授权页面遍历 */
    public static final List<String> ALL_ROLE_CODES = List.of(CODE_STUDENT, CODE_MERCHANT, CODE_ADMIN);

    private static final Map<Integer, String> CODE_BY_ROLE = new LinkedHashMap<>();
    private static final Map<String, Integer> ROLE_BY_CODE = new LinkedHashMap<>();
    private static final Map<String, String> NAME_BY_CODE = new LinkedHashMap<>();

    static {
        CODE_BY_ROLE.put(STUDENT, CODE_STUDENT);
        CODE_BY_ROLE.put(MERCHANT, CODE_MERCHANT);
        CODE_BY_ROLE.put(ADMIN, CODE_ADMIN);
        ROLE_BY_CODE.put(CODE_STUDENT, STUDENT);
        ROLE_BY_CODE.put(CODE_MERCHANT, MERCHANT);
        ROLE_BY_CODE.put(CODE_ADMIN, ADMIN);
        NAME_BY_CODE.put(CODE_STUDENT, "师生用户");
        NAME_BY_CODE.put(CODE_MERCHANT, "食堂商家");
        NAME_BY_CODE.put(CODE_ADMIN, "管理员");
    }

    private Roles() {
    }

    /** 数值角色转角色编码，未知角色返回 null */
    public static String codeOf(Integer role) {
        return role == null ? null : CODE_BY_ROLE.get(role);
    }

    /** 角色编码转数值角色，未知编码返回 null */
    public static Integer roleOfCode(String roleCode) {
        return roleCode == null ? null : ROLE_BY_CODE.get(roleCode);
    }

    /** 角色编码转中文名 */
    public static String nameOfCode(String roleCode) {
        return roleCode == null ? "未知" : NAME_BY_CODE.getOrDefault(roleCode, "未知");
    }

    public static boolean isValidRoleCode(String roleCode) {
        return roleCode != null && ROLE_BY_CODE.containsKey(roleCode);
    }

    /** 数值角色转中文名 */
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

    /** 全部数值角色 */
    public static Set<Integer> allRoles() {
        return CODE_BY_ROLE.keySet();
    }
}
