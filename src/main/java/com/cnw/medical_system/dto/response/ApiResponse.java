package com.cnw.medical_system.dto.response;

public record ApiResponse<T>(String code, String message, T data) {
}
