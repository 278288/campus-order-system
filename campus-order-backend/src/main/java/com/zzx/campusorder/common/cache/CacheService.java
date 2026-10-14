package com.zzx.campusorder.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 缓存操作工具类（实验4 任务一 步骤2）。
 *
 * <p>对外只提供四类方法：写入并设置过期时间、按键读取、按键删除、为已存在的键续期。
 * 业务代码不直接依赖 RedisTemplate，便于后续替换缓存实现。</p>
 *
 * <p>键名规范：固定前缀 + 业务主键，登录用户缓存的键为 {@code login:user:{userId}}，
 * 例如 {@code login:user:1}。前缀统一在本类中拼装，避免各处手写不一致。</p>
 *
 * @author 郑志雄 2520145325
 */
@Component
@RequiredArgsConstructor
public class CacheService {

    /** 登录用户缓存键前缀 */
    private static final String LOGIN_USER_PREFIX = "login:user:";

    private final RedisTemplate<String, Object> redisTemplate;

    /** 拼装登录用户缓存键，登录、退出、过滤器三处必须使用同一规则 */
    public String loginUserKey(Long userId) {
        return LOGIN_USER_PREFIX + userId;
    }

    /**
     * 按键写入并设置过期时间。
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时长，为 null 或非正数时不设置过期
     */
    public void set(String key, Object value, Duration timeout) {
        if (timeout != null && !timeout.isZero() && !timeout.isNegative()) {
            redisTemplate.opsForValue().set(key, value, timeout);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * 按键读取，键不存在返回 null。
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 按键删除，返回是否确实删除了键。
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 为已存在的键续期，键不存在时返回 false。
     */
    public boolean expire(String key, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout));
    }

    /**
     * 判断键是否存在。
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
