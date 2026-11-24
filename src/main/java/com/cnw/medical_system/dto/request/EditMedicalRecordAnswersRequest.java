package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class EditMedicalRecordAnswersRequest {

    @NotEmpty(message = "Danh sách câu trả lời cần sửa (answers) không được để trống")
    @Valid
    @JsonProperty("answers")
    List<EditAnswerItem> answers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class EditAnswerItem {

        @Positive(message = "answer_id phải là số nguyên dương")
        @JsonProperty("answer_id")
        Long answerId;

        @Size(max = 100, message = "question_id tối đa 100 ký tự")
        @JsonProperty("question_id")
        String questionId;

        @Size(max = 50, message = "section_id tối đa 50 ký tự")
        @JsonProperty("section_id")
        String sectionId;

        @NotNull(message = "answer_value không được để trống")
        @Size(max = 5000, message = "answer_value tối đa 5000 ký tự")
        @JsonProperty("answer_value")
        String answerValue;

        @Size(max = 30, message = "data_type tối đa 30 ký tự")
        @JsonProperty("data_type")
        String dataType;

        @Size(max = 1000, message = "note tối đa 1000 ký tự")
        @JsonProperty("note")
        String note;
    }
}
