package com.cnw.medical_system.controller;

import com.cnw.medical_system.service.MaydayApiException;
import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.factory.ApiResponseFactory;
import com.cnw.medical_system.service.MaydayAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MaydayAuthController {
    private final MaydayAuthService auth;
    private final ApiResponseFactory apiResponseFactory;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(@RequestBody Map<String, Object> body) {
        String fullName = requiredText(body, "full_name");
        String phone = requiredText(body, "phone");
        String password = requiredText(body, "password");
        if (!phone.matches("^[0-9]{10}$")) {
            throw new MaydayApiException("1004", "Số điện thoại phải gồm 10 chữ số");
        }
        if (fullName.length() > 150 || password.length() < 6 || password.length() > 100) {
            throw new MaydayApiException("1004", "Dữ liệu đăng ký không hợp lệ");
        }
        Long userId = auth.signup(fullName, phone, password);
        return apiResponseFactory.success(Map.of("user_id", userId));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, Object> body) {
        String phone = requiredText(body, "phone");
        String password = requiredText(body, "password");
        return apiResponseFactory.success(Map.of("token", auth.login(phone, password)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.logout(authorization);
        return apiResponseFactory.success(Map.of("logged_out", true));
    }

    private String requiredText(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null || value instanceof String text && text.isBlank()) {
            throw new MaydayApiException("1002", "Thiếu trường bắt buộc: " + field);
        }
        if (!(value instanceof String)) {
            throw new MaydayApiException("1003", "Sai kiểu dữ liệu: " + field);
        }
        return (String) value;
    }
}
