package com.cnw.medical_system.config;

import com.cnw.medical_system.service.AuthenticationService;
import com.cnw.medical_system.service.TokenBlacklistService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@RequiredArgsConstructor
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // . Lấy chuỗi Authorization từ Header của Request gửi lên
        String authHeader = request.getHeader("Authorization");

        // 2. Kiểm tra xem Header có bắt đầu bằng chữ "Bearer " không
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Cắt bỏ 7 ký tự "Bearer " để lấy nguyên chuỗi Token
            // kiểm tra blacklist: nếu token đã đăng xuất -> chặn đứng ngay lập tức!
            if (tokenBlacklistService.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"9998\", \"message\": \"Token đã hết hiệu lực do đã đăng xuất!\"}");
                return;
            }
            try {
                // Dùng chung SIGNER_KEY từ AuthenticationService
                SecretKey key = Keys.hmacShaKeyFor(AuthenticationService.SIGNER_KEY.getBytes(StandardCharsets.UTF_8));

                // 3. Giải mã và kiểm tra chữ ký số của Token
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                request.setAttribute("jwt_claims", claims);
                request.setAttribute("jwt_token", token);

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                // 4. Nếu Token hợp lệ, đưa thông tin người dùng vào SecurityContext của Spring
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
                    var authToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Nếu Token hết hạn hoặc bị sửa đổi trái phép -> Bỏ qua không xác thực
                logger.warn("Token không hợp lệ hoặc đã hết hạn: " + e.getMessage());
            }
        }

        // 5. Cho phép request tiếp tục đi tới Controller
        filterChain.doFilter(request, response);
    }
}