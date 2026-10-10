package com.zzx.campusorder.config;

import com.zzx.campusorder.common.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Web 配置：注册鉴权拦截器并放开前端开发服务器的跨域访问。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /** 允许跨域的前端地址，配置中以英文逗号分隔 */
    private final List<String> allowedOrigins;

    public WebConfig(AuthInterceptor authInterceptor,
                     @Value("${campus-order.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
                     String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 无需登录即可访问的路径白名单。
     *
     * <p>设计说明：Spring MVC 的拦截器排除规则按“路径”匹配，不区分 HTTP 方法。
     * 如果把 /api/dishes、/api/windows 这类同时承载“查询”与“写操作”的路径放进白名单，
     * 它们的 POST、PUT 写接口也会一并跳过鉴权，服务层将拿不到登录上下文。
     * 因此这里采用资源命名空间隔离的做法：只放行 /api/public/** 这一只读命名空间与登录注册入口，
     * 其余 /api/** 一律要求携带有效访问令牌。这样既保留了未登录浏览菜品的体验，
     * 又保证所有写操作都在服务端完成鉴权与越权校验。</p>
     */
    private static final String[] WHITE_LIST = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/public/**",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(WHITE_LIST);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
