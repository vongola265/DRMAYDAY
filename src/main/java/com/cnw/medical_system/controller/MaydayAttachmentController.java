package com.cnw.medical_system.controller;

import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.service.MaydayApiResponse;
import com.cnw.medical_system.service.MaydayAuthService;
import com.cnw.medical_system.service.MaydayPhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MaydayAttachmentController {
    private final MaydayAuthService auth;
    private final MaydayPhotoService photos;

    @GetMapping("/api/image-attachments/view/{attachmentId}")
    public ResponseEntity<Map<String, Object>> viewAttachment(
            @PathVariable String attachmentId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        if (!attachmentId.matches("[0-9]+")) {
            throw new com.cnw.medical_system.service.MaydayApiException("1003", "ID sai định dạng");
        }
        long id;
        try {
            id = Long.parseLong(attachmentId);
        } catch (NumberFormatException exception) {
            throw new com.cnw.medical_system.service.MaydayApiException("1004", "ID vượt giới hạn");
        }
        if (id <= 0) throw new com.cnw.medical_system.service.MaydayApiException("1004", "ID phải lớn hơn 0");
        return ResponseEntity.ok(MaydayApiResponse.success(photos.getAttachment(id, actor)));
    }
}
