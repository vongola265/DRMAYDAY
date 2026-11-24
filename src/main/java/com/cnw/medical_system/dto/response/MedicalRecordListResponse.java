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
public class MedicalRecordListResponse {

    @JsonProperty("total")
    long total;

    @JsonProperty("page")
    int page;

    @JsonProperty("limit")
    int limit;

    @JsonProperty("data")
    List<MedicalRecordResponse> data;
}
