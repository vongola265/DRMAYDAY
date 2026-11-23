package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MaydayLesionLocationResponse(
        @JsonProperty("lesion_location_id") String lesionLocationId,
        @JsonProperty("episode_id") String episodeId,
        @JsonProperty("body_part") String bodyPart,
        @JsonProperty("occurred_at") String occurredAt,
        @JsonProperty("context_tags") String contextTags,
        @JsonProperty("context_other") String contextOther,
        @JsonProperty("medication_name") String medicationName,
        @JsonProperty("medication_taken_ago") String medicationTakenAgo,
        @JsonProperty("medication_unit") String medicationUnit,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        List<MaydayLesionPhotoResponse> photos) {
}
