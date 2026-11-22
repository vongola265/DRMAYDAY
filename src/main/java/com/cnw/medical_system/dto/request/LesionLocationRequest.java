package com.cnw.medical_system.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LesionLocationRequest {
    private Long episodeId;
    @Size(max = 255) private String bodyPart;
    private OffsetDateTime occurredAt;
    @Size(max = 255) private String relatedMedication;
    @Size(max = 100) private String medicationDuration;
    @Size(max = 255) private String circumstance;
    private String otherCircumstance;
    @Size(max = 50) private String timeUnit;
}
