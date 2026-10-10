package com.zzx.campusorder.common.security;

import com.zzx.campusorder.common.exception.BusinessException;
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
 * 访问令牌工具类：签发与解析 JWT。令牌中只保存 userId、username 与 role，不保存任何敏感信息。
 */
@Slf4j
@Component
public class JwtUtil {

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
     * 签发访问令牌。
     */
    public String generateToken(Long userId, String username, Integer role) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMinutes * 60 * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析令牌，失败时抛出 401 业务异常。
     */
    public UserContext.LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            Integer role = claims.get("role", Integer.class);
            return new UserContext.LoginUser(userId, username, role);
        } catch (ExpiredJwtException e) {
            throw BusinessException.unauthorized("登录已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("令牌解析失败：{}", e.getMessage());
            throw BusinessException.unauthorized("登录凭证无效，请重新登录");
        }
    }

    public long getExpireMinutes() {
        return expireMinutes;
    }
}
