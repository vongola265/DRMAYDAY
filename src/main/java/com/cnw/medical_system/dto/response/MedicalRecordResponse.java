package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalRecordResponse {

    @JsonProperty("medical_record_id")
    Long medicalRecordId;

    @JsonProperty("patient_id")
    Long patientId;

    @JsonProperty("patient_name")
    String patientName;

    @JsonProperty("patient_phone")
    String patientPhone;

    @JsonProperty("doctor_id")
    Long doctorId;

    @JsonProperty("doctor_name")
    String doctorName;

    @JsonProperty("room_id")
    Long roomId;

    @JsonProperty("record_type")
    String recordType;

    @JsonProperty("template_code")
    String templateCode;

    @JsonProperty("classify_id")
    Long classifyId;

    @JsonProperty("status")
    String status;

    @JsonProperty("chief_complaint")
    String chiefComplaint;

    @JsonProperty("preliminary_diagnosis")
    String preliminaryDiagnosis;

    @JsonProperty("final_diagnosis")
    String finalDiagnosis;

    @JsonProperty("treatment_plan")
    String treatmentPlan;

    @JsonProperty("notes")
    String notes;

    @JsonProperty("follow_up_date")
    LocalDate followUpDate;

    @JsonProperty("answer_count")
    Long answerCount;

    @JsonProperty("received_at")
    LocalDateTime receivedAt;

    @JsonProperty("completed_at")
    LocalDateTime completedAt;

    @JsonProperty("created_at")
    LocalDateTime createdAt;

    @JsonProperty("updated_at")
    LocalDateTime updatedAt;
}
