package com.cnw.medical_system.dto.response;

public record ApiResponse<T>(String code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>("1000", "OK", data); }
    public static <T> ApiResponse<T> created(T data) { return new ApiResponse<>("1000", "OK", data); }
}
