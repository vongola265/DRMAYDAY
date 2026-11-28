package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaydayAttachmentResponse(
        String id,
        @JsonProperty("attachment_id") String attachmentId,
        @JsonProperty("user_id") String userId,
        @JsonProperty("medical_record_id") String medicalRecordId,
        @JsonProperty("question_id") Long questionId,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_type") String fileType,
        @JsonProperty("media_type") String mediaType,
        String category,
        String description,
        @JsonProperty("file_url") String fileUrl,
        @JsonProperty("created_at") String createdAt) {
}
