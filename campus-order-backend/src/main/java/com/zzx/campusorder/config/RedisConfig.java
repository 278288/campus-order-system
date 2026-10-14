package com.zzx.campusorder.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置（实验4 任务一 步骤2）。
 *
 * <p>四项要求：
 * <ol>
 *   <li>键使用字符串序列化器——否则 redis-cli 里看到的是带转义的可读性极差的二进制串；</li>
 *   <li>值使用 JSON 序列化器——缓存内容在客户端可读，便于排查；</li>
 *   <li>序列化时写入类型信息——反序列化时才知道要还原成哪个类；</li>
 *   <li>开启可信包白名单——只允许还原白名单内的类，防止反序列化漏洞。</li>
 * </ol>
 * </p>
 *
 * <p><b>易错点</b>：方法名必须是 {@code redisTemplate}，否则注入点拿到的仍是框架默认模板
 * （默认用 JDK 序列化），自定义的序列化配置形同虚设。</p>
 *
 * @author 郑志雄 2520145325
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate：键为字符串、值为 JSON、写入类型信息并使用可信包白名单。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        // 支持 LocalDateTime 等 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 缓存对象实现了 UserDetails，序列化时会写入 isEnabled 等派生属性（如 "enabled"），
        // 而该类本身没有对应字段。反序列化时若不容忍未知属性就会报
        // "Unrecognized field ... not marked as ignorable"，导致所有带令牌的请求 500。
        objectMapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 可信包白名单：只允许还原本项目与 Spring Security 权限相关的类
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.zzx.campusorder.")
                .allowIfSubType("org.springframework.security.core.authority.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .build();
        // 写入类型信息，反序列化时据此还原具体类型；白名单之外的类会被拒绝
        objectMapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
