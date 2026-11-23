package com.cnw.medical_system.config;

import com.cnw.medical_system.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MalformedPathFilter extends OncePerRequestFilter {
    private final ObjectMapper mapper;

    public MalformedPathFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.toLowerCase(Locale.ROOT).contains("%3b")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getWriter(), new ApiResponse<>("1003", "Tham số không hợp lệ", null));
            return;
        }
        if (path.toLowerCase(Locale.ROOT).contains("%20")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getWriter(), new ApiResponse<>("1004", "Đường dẫn không hợp lệ", null));
            return;
        }
        chain.doFilter(request, response);
    }
}
