package com.cnw.medical_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LesionLocationRequest {
    @NotNull private Long episodeId;
    @NotBlank @Size(max = 150) private String bodyPart;
    @Size(max = 50) private String side;
    @Size(max = 1000) private String description;
}
