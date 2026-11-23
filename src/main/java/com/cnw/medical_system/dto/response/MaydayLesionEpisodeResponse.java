package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaydayLesionEpisodeResponse(
        @JsonProperty("episode_id") String episodeId,
        @JsonProperty("medical_record_id") String medicalRecordId,
        @JsonProperty("patient_id") String patientId,
        String name,
        @JsonProperty("occurred_at") String occurredAt,
        @JsonProperty("context_tags") String contextTags,
        @JsonProperty("context_other") String contextOther,
        @JsonProperty("medication_name") String medicationName,
        @JsonProperty("medication_taken_ago") String medicationTakenAgo,
        @JsonProperty("medication_unit") String medicationUnit,
        String description,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt) {
}
