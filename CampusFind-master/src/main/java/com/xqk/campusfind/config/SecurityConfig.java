package com.xqk.campusfind.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xqk.campusfind.common.Result;
import com.xqk.campusfind.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security 配置：无状态 + JWT 认证 + 基于角色的接口鉴权。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 无状态：不创建 / 不使用 Session，完全依赖 JWT
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 跨域预检请求放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 登录 / 注册放行
                        .requestMatchers("/user/login", "/user/register").permitAll()
                        // 管理员专属接口
                        .requestMatchers("/item/pending", "/item/review").hasRole("ADMIN")
                        // 业务接口需登录
                        .requestMatchers("/item/**", "/claim/**", "/user/**").authenticated()
                        // 其余（静态资源等）放行
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        // 未登录 / 令牌失效：401
                        .authenticationEntryPoint((req, resp, e) ->
                                writeJson(resp, HttpServletResponse.SC_UNAUTHORIZED, Result.error("未登录或登录已失效")))
                        // 已登录但权限不足：403
                        .accessDeniedHandler((req, resp, e) ->
                                writeJson(resp, HttpServletResponse.SC_FORBIDDEN, Result.error("权限不足，禁止访问")))
                )
                // 在用户名密码认证过滤器之前加入 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 统一以 JSON 形式返回鉴权失败结果 */
    private void writeJson(HttpServletResponse response, int status, Result<?> result) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getOutputStream(), result);
    }

    /** CORS 配置：统一在此管理，避免与 MVC 层重复导致跨域头冲突 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:5173"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
