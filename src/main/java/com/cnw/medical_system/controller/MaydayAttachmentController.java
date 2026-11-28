package com.cnw.medical_system.controller;

import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.dto.response.MaydayAttachmentResponse;
import com.cnw.medical_system.factory.ApiResponseFactory;
import com.cnw.medical_system.service.MaydayAuthService;
import com.cnw.medical_system.service.MaydayAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class MaydayAttachmentController {
    private final MaydayAuthService auth;
    private final MaydayAttachmentService attachments;
    private final ApiResponseFactory apiResponseFactory;

    @PostMapping(value = "/api/image-attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MaydayAttachmentResponse>> upload(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "medical_record_id", required = false) String medicalRecordId,
            @RequestParam(value = "question_id", required = false) String questionId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "media_type", required = false) String mediaType,
            @RequestParam(value = "user_id", required = false) String userId) {
        TaiKhoan actor = auth.requireAccount(authorization);
        return apiResponseFactory.success(attachments.upload(actor, file, medicalRecordId, questionId, category,
                description, mediaType, userId));
    }

    @PostMapping(value = "/api/image-attachments/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<MaydayAttachmentResponse>>> uploadMultiple(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "user_id", required = false) String userId,
            @RequestParam(value = "images[]", required = false) List<MultipartFile> files) {
        TaiKhoan actor = auth.requireAccount(authorization);
        if (userId == null || userId.isBlank()) throw new com.cnw.medical_system.service.MaydayApiException("1002", "Thiếu user_id");
        if (!userId.matches("[0-9]+")) throw new com.cnw.medical_system.service.MaydayApiException("1003", "user_id sai định dạng");
        if (!String.valueOf(actor.getNguoi().getMaNguoi()).equals(userId)) {
            throw new com.cnw.medical_system.service.MaydayApiException("1009", "Không thể tải tệp cho người dùng khác");
        }
        if (files == null || files.isEmpty()) throw new com.cnw.medical_system.service.MaydayApiException("1002", "Thiếu danh sách hình ảnh");
        if (files.size() > 10) throw new com.cnw.medical_system.service.MaydayApiException("1008", "Tối đa 10 hình ảnh mỗi lần tải");
        java.util.ArrayList<MaydayAttachmentResponse> result = new java.util.ArrayList<>();
        for (MultipartFile file : files) result.add(attachments.upload(actor, file, null, null, null, null, "image", null));
        return apiResponseFactory.success(result);
    }

    @GetMapping("/api/image-attachments/user/{userId}")
    public ResponseEntity<ApiResponse<List<MaydayAttachmentResponse>>> listForUser(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        if (!userId.matches("[0-9]+")) throw new com.cnw.medical_system.service.MaydayApiException("1003", "user_id sai định dạng");
        long id;
        try { id = Long.parseLong(userId); }
        catch (NumberFormatException exception) { throw new com.cnw.medical_system.service.MaydayApiException("1004", "user_id vượt giới hạn"); }
        if (id <= 0) throw new com.cnw.medical_system.service.MaydayApiException("1004", "user_id phải lớn hơn 0");
        return apiResponseFactory.success(attachments.listForUser(id, actor));
    }

    @GetMapping("/api/image-attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<MaydayAttachmentResponse>> getAttachmentDetail(
            @PathVariable String attachmentId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        return apiResponseFactory.success(attachments.find(parseId(attachmentId), actor));
    }

    @DeleteMapping("/api/image-attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> deleteAttachment(
            @PathVariable String attachmentId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        long id = parseId(attachmentId);
        attachments.delete(id, actor);
        return apiResponseFactory.success(java.util.Map.of());
    }

    @GetMapping("/api/image-attachments/view/{attachmentId}")
    public ResponseEntity<byte[]> viewAttachment(
            @PathVariable String attachmentId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        MaydayAttachmentService.AttachmentFile file = attachments.file(parseId(attachmentId), actor);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store").body(file.bytes());
    }

    private long parseId(String value) {
        if (!value.matches("[0-9]+")) throw new com.cnw.medical_system.service.MaydayApiException("1003", "ID sai định dạng");
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new com.cnw.medical_system.service.MaydayApiException("1004", "ID phải lớn hơn 0");
            return id;
        } catch (NumberFormatException exception) {
            throw new com.cnw.medical_system.service.MaydayApiException("1004", "ID vượt giới hạn");
        }
    }
}
