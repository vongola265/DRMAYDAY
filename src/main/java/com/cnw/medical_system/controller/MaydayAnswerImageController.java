package com.cnw.medical_system.controller;

import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.dto.response.MaydayAnswerImageResponse;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.factory.ApiResponseFactory;
import com.cnw.medical_system.service.MaydayAnswerService;
import com.cnw.medical_system.service.MaydayAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MaydayAnswerImageController {
    private final MaydayAuthService auth;
    private final MaydayAnswerService answers;
    private final ApiResponseFactory apiResponseFactory;

    @PostMapping("/api/answer-images")
    public ResponseEntity<ApiResponse<MaydayAnswerImageResponse>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        TaiKhoan actor = auth.requireAccount(authorization);
        return apiResponseFactory.success(answers.createImage(actor, body));
    }

    @PostMapping("/api/answer-images/bulk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBulk(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        TaiKhoan actor = auth.requireAccount(authorization);
        return apiResponseFactory.success(answers.createImages(actor, body));
    }

    @GetMapping("/api/answer-images/{answerId}")
    public ResponseEntity<ApiResponse<List<MaydayAnswerImageResponse>>> list(
            @PathVariable String answerId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        long id = parseId(answerId);
        return apiResponseFactory.success(answers.listImages(id, auth.requireAccount(authorization)));
    }

    @DeleteMapping("/api/answer-images/{linkId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(
            @PathVariable String linkId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        long id = parseId(linkId);
        answers.deleteImage(id, auth.requireAccount(authorization));
        return apiResponseFactory.success(Map.of());
    }

    private long parseId(String value) {
        if (!value.matches("[0-9]+")) throw new com.cnw.medical_system.service.MaydayApiException("1003", "ID sai định dạng");
        try { long id = Long.parseLong(value); if (id <= 0) throw new com.cnw.medical_system.service.MaydayApiException("1004", "ID phải lớn hơn 0"); return id; }
        catch (NumberFormatException exception) { throw new com.cnw.medical_system.service.MaydayApiException("1004", "ID vượt giới hạn"); }
    }
}
