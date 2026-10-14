package com.zzx.campusorder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 消息转换器配置：让响应头带上 {@code charset=UTF-8}。
 *
 * <p>解决的问题：默认的 Jackson 转换器在响应头中只写 {@code Content-Type: application/json}，
 * 不带 charset。按 HTTP 规范，缺少 charset 时客户端有权按 ISO-8859-1 解码，
 * 于是中文提示会变成乱码（如「登录成功」显示为「ç»å½æå」）。
 * 浏览器通常按 JSON 规范默认 UTF-8 处理，但 PowerShell 的 Invoke-RestMethod、
 * 部分 HTTP 客户端与接口测试工具会严格按规范解码，从而出现乱码。</p>
 *
 * <p>这里采用「改写已有转换器」而不是「重新注册 @Bean」的方式：
 * 若直接注册同类型的 Bean 覆盖，会丢掉 Spring Boot 自动配置的 ObjectMapper，
 * 导致 application.yml 中配置的日期格式与字段包含策略一并失效。</p>
 *
 * @author 郑志雄 2520145325
 */
@Configuration
public class HttpMessageConverterConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
                jackson.setDefaultCharset(StandardCharsets.UTF_8);
                // 在支持的类型中补上带 charset 的精确类型，确保响应头声明编码
                List<MediaType> types = new ArrayList<>(jackson.getSupportedMediaTypes());
                MediaType utf8Json = new MediaType("application", "json", StandardCharsets.UTF_8);
                if (!types.contains(utf8Json)) {
                    types.add(utf8Json);
                    jackson.setSupportedMediaTypes(types);
                }
            } else if (converter instanceof StringHttpMessageConverter stringConverter) {
                stringConverter.setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
    }
}
