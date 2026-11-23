package com.cnw.medical_system.factory;

import com.cnw.medical_system.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {
    public <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(new ApiResponse<>("1000", "Success", data));
    }

    public <T> ResponseEntity<ApiResponse<T>> error(String code, String message) {
        return ResponseEntity.ok(new ApiResponse<>(code, message, null));
    }
}
