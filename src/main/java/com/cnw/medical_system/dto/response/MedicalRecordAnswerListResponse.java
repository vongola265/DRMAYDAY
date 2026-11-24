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
public class MedicalRecordAnswerListResponse {

    @JsonProperty("medical_record_id")
    Long medicalRecordId;

    @JsonProperty("total")
    int total;

    @JsonProperty("data")
    List<MedicalRecordAnswerResponse> data;
}
