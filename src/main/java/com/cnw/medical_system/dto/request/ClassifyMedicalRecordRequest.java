package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassifyMedicalRecordRequest {

    @Positive(message = "patient_id phải là số nguyên dương")
    @JsonProperty("patient_id")
    Long patientId;

    @Positive(message = "medical_record_id phải là số nguyên dương")
    @JsonProperty("medical_record_id")
    Long medicalRecordId;

    @NotEmpty(message = "Danh sách câu trả lời sàng lọc không được để trống")
    @Valid
    @JsonProperty("answers")
    List<ClassifyAnswerItem> answers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ClassifyAnswerItem {

        @NotBlank(message = "question_code không được để trống")
        @Size(max = 50, message = "question_code tối đa 50 ký tự")
        @JsonProperty("question_code")
        String questionCode;

        @NotBlank(message = "answer_value không được để trống")
        @Size(max = 500, message = "answer_value tối đa 500 ký tự")
        @JsonProperty("answer_value")
        String answerValue;
    }
}
