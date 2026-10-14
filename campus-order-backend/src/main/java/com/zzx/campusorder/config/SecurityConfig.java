package com.zzx.campusorder.config;

import com.zzx.campusorder.common.security.JwtAuthenticationFilter;
import com.zzx.campusorder.common.security.PathAuthorizationManager;
import com.zzx.campusorder.common.security.handler.AccessDeniedHandlerImpl;
import com.zzx.campusorder.common.security.handler.DelegatingAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 安全配置类（实验4 任务一 步骤5）。
 *
 * <p>Spring Security 6 中配置适配器基类已被移除，因此直接注册 {@link SecurityFilterChain} Bean。</p>
 *
 * @author 郑志雄 2520145325
 */
@Configuration
@EnableWebSecurity
// 开启方法级安全注解：不开启时 @PreAuthorize 会静默失效，既不报错也不警告
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PathAuthorizationManager pathAuthorizationManager;
    private final DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    @Value("${campus-order.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    /**
     * 白名单：登录、注册、公开浏览接口与错误页转发路径。
     * 不放行会出现「想登录但登录接口也被拦」的问题。
     */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/public/**",
            "/error",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 跨站请求伪造防护：前后端分离项目用令牌认证、不依赖会话，关闭
                .csrf(csrf -> csrf.disable())
                // 表单登录：不关闭时未认证请求会被重定向到登录页
                .formLogin(form -> form.disable())
                // HTTP 基础认证：不关闭会弹出浏览器原生登录框
                .httpBasic(basic -> basic.disable())
                // 无状态：不创建也不使用 HttpSession
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 框架自带的退出接口行为固定，关闭后自行实现退出逻辑
                .logout(logout -> logout.disable())
                // 跨域配置迁移到安全框架统一接管，使预检请求先经过安全链
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 认证与授权失败的出口：401 与 403 分流。
                // 认证入口使用委托式实现，按异常类型区分「未登录」与「账号停用」。
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(delegatingAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 预检请求不携带令牌，必须放行，否则跨域失败
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 白名单放行
                        .requestMatchers(WHITE_LIST).permitAll()
                        // 其余请求：先要求已认证，再由自定义权限管理器做路径级权限判断
                        .anyRequest().access(pathAuthorizationManager))
                // JWT 过滤器置于用户名密码认证过滤器之前，顺序错误会导致后续授权读不到身份
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 跨域配置。由安全框架接管后，预检请求会先经过安全链再被放行。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * 密码编码器：注册 BCrypt，认证过程中框架会自动调用它比对密码。
     * 数据库中已有的哈希由同算法生成，可直接复用。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器：供登录业务类调用，框架会自动调用 UserDetailsService 装配用户信息。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
