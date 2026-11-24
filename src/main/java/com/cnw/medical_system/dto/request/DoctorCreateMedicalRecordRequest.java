package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorCreateMedicalRecordRequest {

    @NotNull(message = "patient_id không được để trống")
    @Positive(message = "patient_id phải là số nguyên dương")
    @JsonProperty("patient_id")
    Long patientId;

    @Positive(message = "classify_id phải là số nguyên dương")
    @JsonProperty("classify_id")
    Long classifyId;

    @NotBlank(message = "Loại bệnh án (record_type) không được để trống")
    @Size(max = 30, message = "record_type tối đa 30 ký tự")
    @JsonProperty("record_type")
    String recordType;

    @Size(max = 50, message = "template_code tối đa 50 ký tự")
    @JsonProperty("template_code")
    String templateCode;

    @Positive(message = "room_id phải là số nguyên dương")
    @JsonProperty("room_id")
    Long roomId;

    @Size(max = 30, message = "status tối đa 30 ký tự")
    @JsonProperty("status")
    String status;

    @NotBlank(message = "Lý do khám (chief_complaint) không được để trống")
    @Size(max = 2000, message = "Lý do khám tối đa 2000 ký tự")
    @JsonProperty("chief_complaint")
    String chiefComplaint;

    @Size(max = 2000, message = "Chẩn đoán sơ bộ tối đa 2000 ký tự")
    @JsonProperty("preliminary_diagnosis")
    String preliminaryDiagnosis;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @JsonProperty("notes")
    String notes;
}
