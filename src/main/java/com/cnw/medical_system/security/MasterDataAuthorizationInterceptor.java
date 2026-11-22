package com.cnw.medical_system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MasterDataAuthorizationInterceptor implements HandlerInterceptor {
    private static final String SIGNER_KEY = System.getenv().getOrDefault("JWT_SECRET", "MedicalSystemSecretKeyForJwtTokenSigningMustBe256BitsLong!");
    private final LoggedOutTokenRegistry loggedOut;
    private final ObjectMapper mapper;

    public MasterDataAuthorizationInterceptor(LoggedOutTokenRegistry loggedOut, ObjectMapper mapper) {
        this.loggedOut = loggedOut;
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String path = uri.startsWith("/api/v1/") ? uri.substring(7) : uri.startsWith("/api/") ? uri.substring(4) : uri;
        if (!(path.startsWith("/branches") || path.startsWith("/departments") || path.startsWith("/rooms") || path.startsWith("/templates"))) return true;
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) return reject(response, "9998", "Yêu cầu đăng nhập");
        String token = header.substring(7).trim();
        if (token.isEmpty() || loggedOut.isRevoked(token)) return reject(response, "9998", "Token không hợp lệ");
        String role;
        try {
            var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SIGNER_KEY.getBytes(StandardCharsets.UTF_8))).build().parseSignedClaims(token).getPayload();
            role = claims.get("role", String.class);
        } catch (RuntimeException ex) {
            return reject(response, "9998", "Token không hợp lệ hoặc đã hết hạn");
        }
        if (!"GET".equalsIgnoreCase(request.getMethod()) && !"admin".equalsIgnoreCase(role)) return reject(response, "1009", "Không có quyền thực hiện thao tác này");
        return true;
    }

    private boolean reject(HttpServletResponse response, String code, String message) throws Exception {
        response.setStatus("9998".equals(code) ? 401 : 403);
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getWriter(), Map.of("code", code, "message", message, "data", Map.of()));
        return false;
    }
}
