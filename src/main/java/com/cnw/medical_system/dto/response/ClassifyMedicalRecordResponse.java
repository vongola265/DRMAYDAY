package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassifyMedicalRecordResponse {

    @JsonProperty("classify_id")
    Long classifyId;

    @JsonProperty("patient_id")
    Long patientId;

    @JsonProperty("performed_by_id")
    Long performedById;

    @JsonProperty("classified_type")
    String classifiedType;

    @JsonProperty("recommended_template_code")
    String recommendedTemplateCode;

    @JsonProperty("priority_level")
    String priorityLevel;

    @JsonProperty("classification_note")
    String classificationNote;

    @JsonProperty("medical_record_id")
    Long medicalRecordId;

    @JsonProperty("created_at")
    LocalDateTime createdAt;
}
