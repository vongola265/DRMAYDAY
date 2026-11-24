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
public class ClassifyQuestionListResponse {

    @JsonProperty("total")
    int total;

    @JsonProperty("data")
    List<ClassifyQuestionResponse> data;
}
