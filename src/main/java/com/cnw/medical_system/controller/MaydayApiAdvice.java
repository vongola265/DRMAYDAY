package com.cnw.medical_system.controller;

import com.cnw.medical_system.service.MaydayApiException;
import com.cnw.medical_system.service.MaydayApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestControllerAdvice
public class MaydayApiAdvice {
    @ExceptionHandler(MaydayApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiError(MaydayApiException exception) {
        return ResponseEntity.ok(MaydayApiResponse.error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.ok(MaydayApiResponse.error("1003", "JSON không hợp lệ"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(DataIntegrityViolationException exception) {
        return ResponseEntity.ok(MaydayApiResponse.error("1004", "Dữ liệu vi phạm ràng buộc"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResponseStatusException exception, HttpServletRequest request) {
        String code = exception.getStatusCode().value() == 404 ? "9992" : "1004";
        Map<String, Object> response = MaydayApiResponse.error(code, exception.getReason());
        if (request.getRequestURI().startsWith("/api/") && !request.getRequestURI().startsWith("/api/v1/")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(exception.getStatusCode()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        return ResponseEntity.ok(MaydayApiResponse.error("1010", "Lỗi xử lý yêu cầu"));
    }
}
