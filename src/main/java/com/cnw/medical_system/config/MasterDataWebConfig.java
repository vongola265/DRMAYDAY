package com.cnw.medical_system.config;

import com.cnw.medical_system.security.MasterDataAuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MasterDataWebConfig implements WebMvcConfigurer {
    private final MasterDataAuthorizationInterceptor authorization;
    public MasterDataWebConfig(MasterDataAuthorizationInterceptor authorization) { this.authorization = authorization; }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(authorization).addPathPatterns("/api/**", "/api/v1/**"); }
}
