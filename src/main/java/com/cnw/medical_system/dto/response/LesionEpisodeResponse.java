package com.cnw.medical_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class LesionEpisodeResponse {
    private Long id;
    private Long patientId;
    private Long medicalRecordId;
    private String name;
    private OffsetDateTime occurredAt;
    private String circumstance;
    private String otherCircumstance;
    private String medication;
    private String medicationDuration;
    private String timeUnit;
    private String details;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
