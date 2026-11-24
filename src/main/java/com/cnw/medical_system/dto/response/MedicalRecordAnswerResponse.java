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
public class MedicalRecordAnswerResponse {

    @JsonProperty("answer_id")
    Long answerId;

    @JsonProperty("medical_record_id")
    Long medicalRecordId;

    @JsonProperty("section_id")
    String sectionId;

    @JsonProperty("question_id")
    String questionId;

    @JsonProperty("answer_value")
    String answerValue;

    @JsonProperty("data_type")
    String dataType;

    @JsonProperty("note")
    String note;

    @JsonProperty("filled_by_role")
    String filledByRole;

    @JsonProperty("filled_by_id")
    Long filledById;

    @JsonProperty("filled_by_name")
    String filledByName;

    @JsonProperty("created_at")
    LocalDateTime createdAt;

    @JsonProperty("updated_at")
    LocalDateTime updatedAt;
}
