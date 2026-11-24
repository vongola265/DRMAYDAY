package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SelfCreateMedicalRecordRequest {

    @Positive(message = "classify_id phải là số nguyên dương")
    @JsonProperty("classify_id")
    Long classifyId;

    @Size(max = 30, message = "record_type tối đa 30 ký tự")
    @JsonProperty("record_type")
    String recordType;

    @Size(max = 50, message = "template_code tối đa 50 ký tự")
    @JsonProperty("template_code")
    String templateCode;

    @Positive(message = "room_id phải là số nguyên dương")
    @JsonProperty("room_id")
    Long roomId;

    @NotBlank(message = "Lý do khám (chief_complaint) không được để trống")
    @Size(max = 2000, message = "Lý do khám tối đa 2000 ký tự")
    @JsonProperty("chief_complaint")
    String chiefComplaint;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @JsonProperty("notes")
    String notes;
}
