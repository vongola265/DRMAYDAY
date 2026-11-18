package com.cnw.medical_system.config;

import java.security.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {
        JwtAuthenticationFilter jwtAuthenticationFilter;
        // danh sách các đường dẫn công khai (ai cũng vào được, không cần token)
        static String[] PUBLIC_ENDPOINTS = {
                        "/auth/login",
                        "/auth/signup",
                        "/v3/api-docs/**",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/error",
        };

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http)
                        throws Exception {
                http
                                // 1. Tắt CSRF vì ứng dụng REST API dùng JWT, không dùng Session Cookie
                                .csrf(AbstractHttpConfigurer::disable)
                                // 2. Cấu hình phân quyền truy cập cho các API
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll() // Mở cửa tự do cho
                                                                                               // Login, Register,
                                                                                               // Swagger
                                                .anyRequest().authenticated() // Bắt buộc phải có Token cho tất cả API
                                                                              // còn lại
                                )
                                // 3. Chế độ STATELESS: Server không lưu session trong RAM, hoàn toàn dựa vào
                                // Token
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // 4. Đặt JwtAuthenticationFilter vào trước bộ lọc mặc định của Spring
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

}
