package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassifyQuestionResponse {

    @JsonProperty("question_id")
    Long questionId;

    @JsonProperty("question_code")
    String questionCode;

    @JsonProperty("question_text")
    String questionText;

    @JsonProperty("question_type")
    String questionType;

    @JsonProperty("options")
    List<OptionItem> options;

    @JsonProperty("is_required")
    Boolean isRequired;

    @JsonProperty("display_order")
    Integer displayOrder;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OptionItem {

        @JsonProperty("value")
        String value;

        @JsonProperty("label")
        String label;
    }
}
