package com.cnw.medical_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LesionLocationRequest {
    @NotNull private Long episodeId;
    @NotBlank @Size(max = 150) private String bodyPart;
    @Size(max = 50) private String side;
    @Size(max = 1000) private String description;
}
