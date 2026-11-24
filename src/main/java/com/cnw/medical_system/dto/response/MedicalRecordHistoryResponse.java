package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalRecordHistoryResponse {

    @JsonProperty("patient_id")
    Long patientId;

    @JsonProperty("patient_name")
    String patientName;

    @JsonProperty("patient_phone")
    String patientPhone;

    @JsonProperty("total_visits")
    long totalVisits;

    @JsonProperty("completed_visits")
    long completedVisits;

    @JsonProperty("chronic_visits")
    long chronicVisits;

    @JsonProperty("page")
    int page;

    @JsonProperty("limit")
    int limit;

    @JsonProperty("visits")
    List<VisitHistoryItem> visits;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VisitHistoryItem {

        @JsonProperty("medical_record_id")
        Long medicalRecordId;

        @JsonProperty("record_type")
        String recordType;

        @JsonProperty("status")
        String status;

        @JsonProperty("doctor_id")
        Long doctorId;

        @JsonProperty("doctor_name")
        String doctorName;

        @JsonProperty("room_id")
        Long roomId;

        @JsonProperty("chief_complaint")
        String chiefComplaint;

        @JsonProperty("preliminary_diagnosis")
        String preliminaryDiagnosis;

        @JsonProperty("final_diagnosis")
        String finalDiagnosis;

        @JsonProperty("treatment_plan")
        String treatmentPlan;

        @JsonProperty("follow_up_date")
        LocalDate followUpDate;

        @JsonProperty("visit_date")
        LocalDateTime visitDate;

        @JsonProperty("completed_at")
        LocalDateTime completedAt;

        @JsonProperty("answers")
        List<MedicalRecordAnswerResponse> answers;
    }
}
