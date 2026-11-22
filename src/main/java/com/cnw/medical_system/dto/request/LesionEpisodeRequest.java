package com.cnw.medical_system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class LesionEpisodeRequest {
    @NotNull private Long patientId;
    private Long medicalRecordId;
    @Size(max = 255) private String name;
    private OffsetDateTime occurredAt;
    @Size(max = 255) private String circumstance;
    private String otherCircumstance;
    @Size(max = 255) private String medication;
    @Size(max = 100) private String medicationDuration;
    @Size(max = 50) private String timeUnit;
    private String details;
}
