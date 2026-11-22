package com.cnw.medical_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class LesionLocationResponse {
    private Long id;
    private Long episodeId;
    private String bodyPart;
    private OffsetDateTime occurredAt;
    private String relatedMedication;
    private String medicationDuration;
    private String circumstance;
    private String otherCircumstance;
    private String timeUnit;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
