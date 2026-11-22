package com.cnw.medical_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class LesionEpisodeResponse {
    private Long id;
    private Long patientId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
