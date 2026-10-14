package com.zzx.campusorder.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（实验4 任务一 步骤3）。
 *
 * <p>对外明确四项能力：签发令牌、校验令牌、从令牌中解析用户 ID、读取令牌有效期秒数。</p>
 *
 * <p><b>载荷设计</b>：令牌只携带用户 ID 与签发、过期时间三项，完整用户信息（账号、角色、权限）
 * 存放在 Redis 中、以用户 ID 为键。这样设计的理由：
 * <ol>
 *   <li>JWT 载荷只做 Base64 编码，任何人解开都能看到内容，绝不能放密码、手机号等隐私数据；</li>
 *   <li>角色与权限会随管理员授权而变化，若写进令牌，改完权限必须等令牌过期才生效；
 *       放在 Redis 中则改完立即生效；</li>
 *   <li>退出登录只需删除 Redis 中的缓存，令牌即便签名有效、时间未到也取不到身份，等同于立即失效，
 *       弥补了 JWT 本身无法主动作废的缺陷；</li>
 *   <li>令牌体积小，减少每次请求的传输开销。</li>
 * </ol>
 * </p>
 *
 * @author 郑志雄 2520145325
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";

    @Value("${campus-order.jwt.secret}")
    private String secret;

    @Value("${campus-order.jwt.expire-minutes:120}")
    private long expireMinutes;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发令牌：载荷只放用户 ID、签发时间与过期时间。
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + getExpireSeconds() * 1000);
        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验令牌签名与有效期，通过返回 true，失败返回 false。
     * 该方法只做判定、不抛异常，供过滤器在放行与拒绝之间做选择。
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("令牌已过期：{}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("令牌校验失败：{}", e.getMessage());
        }
        return false;
    }

    /**
     * 从令牌中解析用户 ID，解析失败返回 null。
     */
    public Long parseUserId(String token) {
        try {
            Claims claims = parseClaims(token);
            Object value = claims.get(CLAIM_USER_ID);
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.debug("令牌已过期，无法解析用户 ID");
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("令牌解析失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 读取令牌有效期秒数，用于登录响应与缓存过期时间，保证两者一致。
     */
    public long getExpireSeconds() {
        return expireMinutes * 60;
    }

    public long getExpireMinutes() {
        return expireMinutes;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
