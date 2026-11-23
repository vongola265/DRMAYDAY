package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaydayLesionPhotoResponse(
        @JsonProperty("photo_id") String photoId,
        MaydayImageAttachmentResponse attachment,
        @JsonProperty("captured_at") Object capturedAt,
        String milestone) {
}
