package com.cnw.medical_system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LesionEpisodeRequest {
    @NotNull private Long patientId;
    @NotNull private LocalDate startDate;
    private LocalDate endDate;
    @Size(max = 1000) private String description;
}
