package com.zzx.campusorder.common.security;

import com.zzx.campusorder.common.cache.CacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器（实验4 任务一 步骤7）。
 *
 * <p>继承 {@link OncePerRequestFilter} 保证一次请求只执行一次，按八步实现：</p>
 * <ol>
 *   <li>从请求头取出令牌，兼容带 {@code Bearer } 前缀与不带前缀两种写法；</li>
 *   <li>没有令牌 → 直接放行（登录、注册等白名单接口本就不需要令牌）；</li>
 *   <li>校验令牌签名与有效期，不通过 → 放行；</li>
 *   <li>解析出用户 ID，解析不到 → 放行；</li>
 *   <li>用该 ID 从 Redis 取回用户详情，键为 {@code login:user:{userId}}；</li>
 *   <li>缓存中不存在 → 放行（令牌已退出登录或被驱逐，等同失效）；</li>
 *   <li>二次校验账号状态，被停用 → 放行；</li>
 *   <li>构造已认证对象写入安全上下文，放行请求。</li>
 * </ol>
 *
 * <p><b>两个关键点：</b></p>
 * <ol>
 *   <li>认证失败时不要自己抛异常、也不要自己写 401 响应，而应直接放行，
 *       由后续授权环节统一处理。这样 401 与 403 的分流只有一处出口，不会出现重复写响应；</li>
 *   <li>判断「本次请求是否已处理过」不能只看认证对象是否为空：过滤器链前段的匿名身份过滤器
 *       已经放入了一个匿名认证对象，必须显式排除匿名身份，否则带令牌的请求会被当成已处理而跳过认证。</li>
 * </ol>
 *
 * @author 郑志雄 2520145325
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    /**
     * 请求属性名：标记「令牌有效但账号已被停用」。
     * 授权管理器据此返回 403（身份合法但被禁用）而不是 401（没有身份）。
     */
    public static final String ATTR_DISABLED_ACCOUNT = "campus-order.disabled-account";

    private final JwtUtil jwtUtil;
    private final CacheService cacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 关键点2：显式排除匿名身份，只有「真正的未认证」才继续走认证流程
        if (!isAuthenticated()) {
            // 步骤1：取出令牌，兼容带前缀与不带前缀两种写法
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                // 步骤3：校验签名与有效期
                if (jwtUtil.validateToken(token)) {
                    // 步骤4：解析用户 ID
                    Long userId = jwtUtil.parseUserId(token);
                    if (userId != null) {
                        // 步骤5：用用户 ID 从 Redis 取回完整用户信息。
                        // 缓存读取失败不能让整个请求变成 500，按未认证处理。
                        DbUserDetails details = loadCachedUser(userId);
                        if (details != null) {
                            // 步骤7：二次校验账号状态。
                            // 缓存存在说明此前已登录，此时状态为停用属于「身份合法但已被禁用」，
                            // 与「没有身份」语义不同，因此写入一个停用标记，
                            // 由授权环节返回 403 而不是 401。
                            if (details.isEnabled()) {
                                // 步骤8：构造已认证对象写入安全上下文
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                details, null, details.getAuthorities());
                                authentication.setDetails(
                                        new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            } else {
                                log.debug("令牌对应的账号已被停用：userId={}", userId);
                                request.setAttribute(ATTR_DISABLED_ACCOUNT, Boolean.TRUE);
                            }
                        } else {
                            // 步骤6：缓存不存在，说明已退出登录或缓存已过期，按未认证处理
                            log.debug("缓存中不存在用户信息，按未认证处理：userId={}", userId);
                        }
                    }
                }
            }
        }
        // 关键点1：无论认证是否成功都放行，未认证的请求由授权环节统一返回 401
        filterChain.doFilter(request, response);
    }

    /**
     * 从缓存读取用户详情，读取失败（连接异常或反序列化异常）时返回 null 并记录告警。
     * 认证环节的异常不应升级为 500，按未认证处理更符合语义。
     */
    private DbUserDetails loadCachedUser(Long userId) {
        try {
            Object cached = cacheService.get(cacheService.loginUserKey(userId));
            return cached instanceof DbUserDetails details ? details : null;
        } catch (Exception e) {
            log.warn("读取登录缓存失败，按未认证处理：userId={} 原因={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 判断当前请求是否已经认证过。
     * 必须排除匿名身份，否则链前段放入的匿名对象会让带令牌的请求跳过认证。
     */
    private boolean isAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * 从请求头解析令牌，同时兼容 {@code Bearer xxx} 与直接写 {@code xxx} 两种写法。
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (!StringUtils.hasText(header)) {
            // 部分客户端把令牌放在自定义头中，兼容处理
            header = request.getHeader("token");
        }
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String trimmed = header.trim();
        if (trimmed.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return trimmed.substring(PREFIX.length()).trim();
        }
        return trimmed;
    }
}
