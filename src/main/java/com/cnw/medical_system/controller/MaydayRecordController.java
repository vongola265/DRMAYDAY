package com.cnw.medical_system.controller;

import com.cnw.medical_system.service.MaydayApiResponse;
import com.cnw.medical_system.service.MaydayAuthService;
import com.cnw.medical_system.service.MaydayRecordService;
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

@RestController
@RequestMapping("/api/medical_records")
@RequiredArgsConstructor
public class MaydayRecordController {
    private final MaydayAuthService auth;
    private final MaydayRecordService records;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(MaydayApiResponse.success(records.create(auth.requireAccount(authorization), body)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(MaydayApiResponse.success(records.get(id, auth.requireAccount(authorization))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> put(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Object status = body.get("status");
        if (status != null && !(status instanceof String)) {
            throw new com.cnw.medical_system.service.MaydayApiException("1003", "status sai kiểu dữ liệu");
        }
        return ResponseEntity.ok(MaydayApiResponse.success(records.updateStatus(
                id, (String) status, auth.requireAccount(authorization))));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> patchStatus(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Object status = body.get("status");
        if (status != null && !(status instanceof String)) {
            throw new com.cnw.medical_system.service.MaydayApiException("1003", "status sai kiểu dữ liệu");
        }
        return ResponseEntity.ok(MaydayApiResponse.success(records.updateStatus(
                id, (String) status, auth.requireAccount(authorization))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        records.delete(id, auth.requireAccount(authorization));
        return ResponseEntity.ok(MaydayApiResponse.success(Map.of("deleted", true)));
    }
}
