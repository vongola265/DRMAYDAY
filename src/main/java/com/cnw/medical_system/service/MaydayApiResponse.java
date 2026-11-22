package com.cnw.medical_system.service;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MaydayApiResponse {
    private MaydayApiResponse() {}

    public static Map<String, Object> success(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "1000");
        response.put("message", "Success");
        response.put("data", data);
        return response;
    }

    public static Map<String, Object> error(String code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", code);
        response.put("message", message);
        return response;
    }
}
