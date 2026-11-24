package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EditSingleAnswerRequest {

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
