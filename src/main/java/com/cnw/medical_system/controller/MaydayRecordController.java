package com.cnw.medical_system.controller;

import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.factory.ApiResponseFactory;
import com.cnw.medical_system.service.MaydayAuthService;
import com.cnw.medical_system.service.MaydayRecordService;
import com.cnw.medical_system.service.MaydayAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/medical_records")
@RequiredArgsConstructor
public class MaydayRecordController {
    private final MaydayAuthService auth;
    private final MaydayRecordService records;
    private final MaydayAnswerService answers;
    private final ApiResponseFactory apiResponseFactory;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        return apiResponseFactory.success(records.create(auth.requireAccount(authorization), body));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return apiResponseFactory.success(records.get(parseId(id), auth.requireAccount(authorization)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> put(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Object status = body.get("status");
        if (status != null && !(status instanceof String)) {
            throw new com.cnw.medical_system.service.MaydayApiException("1003", "status sai kiểu dữ liệu");
        }
        return apiResponseFactory.success(records.updateStatus(parseId(id), (String) status, auth.requireAccount(authorization)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> patchStatus(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Object status = body.get("status");
        if (status != null && !(status instanceof String)) {
            throw new com.cnw.medical_system.service.MaydayApiException("1003", "status sai kiểu dữ liệu");
        }
        return apiResponseFactory.success(records.updateStatus(parseId(id), (String) status, auth.requireAccount(authorization)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        records.delete(parseId(id), auth.requireAccount(authorization));
        return apiResponseFactory.success(Map.of("deleted", true));
    }

    @PutMapping("/{id}/answers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveAnswers(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        return apiResponseFactory.success(answers.saveAnswers(parseId(id), auth.requireAccount(authorization), body));
    }

    @GetMapping("/{id}/answers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAnswers(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return apiResponseFactory.success(answers.listAnswers(parseId(id), auth.requireAccount(authorization)));
    }

    private long parseId(String value) {
        if (value == null || !value.matches("-?[0-9]+")) throw new com.cnw.medical_system.service.MaydayApiException("1003", "ID hồ sơ sai định dạng");
        try { long id = Long.parseLong(value); if (id <= 0) throw new com.cnw.medical_system.service.MaydayApiException("1004", "ID hồ sơ phải lớn hơn 0"); return id; }
        catch (NumberFormatException exception) { throw new com.cnw.medical_system.service.MaydayApiException("1004", "ID hồ sơ vượt giới hạn"); }
    }
}
