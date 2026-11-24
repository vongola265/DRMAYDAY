package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMedicalRecordRequest {

    @Size(max = 30, message = "record_type tối đa 30 ký tự")
    @JsonProperty("record_type")
    String recordType;

    @Size(max = 50, message = "template_code tối đa 50 ký tự")
    @JsonProperty("template_code")
    String templateCode;

    @Positive(message = "room_id phải là số nguyên dương")
    @JsonProperty("room_id")
    Long roomId;

    @Positive(message = "doctor_id phải là số nguyên dương")
    @JsonProperty("doctor_id")
    Long doctorId;

    @Size(max = 2000, message = "Lý do khám tối đa 2000 ký tự")
    @JsonProperty("chief_complaint")
    String chiefComplaint;

    @Size(max = 2000, message = "Chẩn đoán sơ bộ tối đa 2000 ký tự")
    @JsonProperty("preliminary_diagnosis")
    String preliminaryDiagnosis;

    @Size(max = 2000, message = "Chẩn đoán xác định tối đa 2000 ký tự")
    @JsonProperty("final_diagnosis")
    String finalDiagnosis;

    @Size(max = 4000, message = "Hướng xử trí tối đa 4000 ký tự")
    @JsonProperty("treatment_plan")
    String treatmentPlan;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @JsonProperty("notes")
    String notes;

    @JsonProperty("follow_up_date")
    LocalDate followUpDate;

    public boolean isEmpty() {
        return recordType == null
                && templateCode == null
                && roomId == null
                && doctorId == null
                && chiefComplaint == null
                && preliminaryDiagnosis == null
                && finalDiagnosis == null
                && treatmentPlan == null
                && notes == null
                && followUpDate == null;
    }
}
