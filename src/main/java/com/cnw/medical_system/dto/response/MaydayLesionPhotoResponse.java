package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaydayLesionPhotoResponse(
        @JsonProperty("photo_id") String photoId,
        @JsonProperty("episode_id") String episodeId,
        @JsonProperty("location_id") String locationId,
        @JsonProperty("attachment_id") String attachmentId,
        MaydayImageAttachmentResponse attachment,
        @JsonProperty("taken_at") Object takenAt,
        @JsonProperty("captured_at") Object capturedAt,
        String milestone,
        @JsonProperty("itch_score") Object itchScore,
        @JsonProperty("pain_score") Object painScore,
        @JsonProperty("burn_score") Object burnScore,
        @JsonProperty("hives_count") Object hivesCount,
        @JsonProperty("local_auas") Object localAuas,
        @JsonProperty("annotated_url") String annotatedUrl) {
}
