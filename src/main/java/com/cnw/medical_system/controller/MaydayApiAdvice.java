package com.cnw.medical_system.controller;

import com.cnw.medical_system.service.MaydayApiException;
import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.factory.ApiResponseFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class MaydayApiAdvice {
    private static final Logger log = LoggerFactory.getLogger(MaydayApiAdvice.class);
    private final ApiResponseFactory apiResponseFactory;
    @ExceptionHandler(MaydayApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiError(MaydayApiException exception) {
        return apiResponseFactory.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidJson(HttpMessageNotReadableException exception) {
        return apiResponseFactory.error("1003", "JSON không hợp lệ");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return apiResponseFactory.error("1003", "Content-Type không được hỗ trợ");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleOversizedUpload(MaxUploadSizeExceededException exception) {
        return apiResponseFactory.error("1006", "Tệp vượt quá giới hạn cho phép");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraint(DataIntegrityViolationException exception) {
        return apiResponseFactory.error("1004", "Dữ liệu vi phạm ràng buộc");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleNotFound(ResponseStatusException exception, HttpServletRequest request) {
        String code = exception.getStatusCode().value() == 404 ? "9992" : "1004";
        if (request.getRequestURI().startsWith("/api/")) {
            return apiResponseFactory.error(code, exception.getReason());
        }
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiResponse<>(code, exception.getReason(), null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleMissingApi(NoResourceFoundException exception, HttpServletRequest request) {
        String message = "Không tìm thấy API";
        if (request.getRequestURI().startsWith("/api/")) {
            return apiResponseFactory.error("9992", message);
        }
        return ResponseEntity.status(404).body(new ApiResponse<>("9992", message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception exception) {
        log.error("Unexpected API failure", exception);
        return apiResponseFactory.error("1010", "Lỗi xử lý yêu cầu");
    }
}
