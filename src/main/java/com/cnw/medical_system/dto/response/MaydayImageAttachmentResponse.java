package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaydayImageAttachmentResponse(
        @JsonProperty("attachment_id") String attachmentId,
        @JsonProperty("file_url") String fileUrl) {
}
