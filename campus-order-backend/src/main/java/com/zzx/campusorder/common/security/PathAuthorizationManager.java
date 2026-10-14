package com.zzx.campusorder.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 自定义权限管理器（实验4 任务二 步骤2）。
 *
 * <p>实现 {@link AuthorizationManager}，泛型参数取 {@link RequestAuthorizationContext}，
 * 表示这是一次基于请求的授权判断。判断过程分四层：</p>
 * <ol>
 *   <li>从请求上下文取出请求对象，得到请求方法与路径，并取出当前认证信息；</li>
 *   <li>认证信息为空、未通过认证，或主体是匿名用户 → 拒绝（由认证异常处理器返回 401）；</li>
 *   <li>当前用户是超级管理员 → 直接通过，不再逐条比对权限；</li>
 *   <li>按方法与路径解析所需权限；该路径无权限要求 → 通过；
 *       否则交给权限判断服务比对，命中通过，未命中拒绝并记录告警日志（返回 403）。</li>
 * </ol>
 *
 * <p>路径与权限的对应关系集中在 {@link RequestPermissionRegistry} 中维护，
 * 与方法级注解共用 {@link PermissionService} 这一处判断逻辑。</p>
 *
 * @author 郑志雄 2520145325
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PathAuthorizationManager implements
        AuthorizationManager<RequestAuthorizationContext> {

    private final PermissionService permissionService;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication,
                                     RequestAuthorizationContext context) {
        // 第一层：取出请求信息与认证信息
        HttpServletRequest request = context.getRequest();
        String method = request.getMethod();
        String path = request.getRequestURI();
        Authentication auth = authentication.get();

        // 第二层：未认证或匿名身份，拒绝。不在这里写响应，交由认证异常处理器统一返回 401
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof org.springframework.security.authentication
                        .AnonymousAuthenticationToken) {
            // 令牌有效但账号已被停用：身份合法但被禁用，按 403 处理，
            // 与「没有身份」的 401 区分开，提示用户联系管理员而不是重新登录。
            // 注意：必须抛认证异常，由 ExceptionTranslationFilter 转交给
            // DisabledAccountEntryPoint 输出 403；直接抛业务异常会被容器当成 500。
            if (Boolean.TRUE.equals(context.getRequest()
                    .getAttribute(JwtAuthenticationFilter.ATTR_DISABLED_ACCOUNT))) {
                log.warn("账号已停用，拒绝访问：{} {}", method, path);
                throw new DisabledAccountAuthenticationException("账号已被停用，请联系管理员");
            }
            log.debug("路径级授权拒绝：未认证 {} {}", method, path);
            return new AuthorizationDecision(false);
        }

        // 第三层：超级管理员直接放行
        if (permissionService.isSuperAdmin()) {
            return new AuthorizationDecision(true);
        }

        // 第四层：解析该请求所需的权限标识
        String required = RequestPermissionRegistry.resolve(method, path);
        if (required == null || required.isEmpty()) {
            // 该路径未配置权限要求，或规则本身只要求「已认证」：
            // 放行到方法级注解或控制器内部的数据归属校验
            return new AuthorizationDecision(true);
        }
        if (permissionService.hasPerm(auth, required)) {
            return new AuthorizationDecision(true);
        }

        log.warn("路径级授权拒绝：用户 {} 访问 {} {} 需要权限 {} 但未持有",
                auth.getName(), method, path, required);
        return new AuthorizationDecision(false);
    }
}
