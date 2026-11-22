package com.cnw.medical_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LesionLocationResponse {
    private Long id;
    private Long episodeId;
    private String bodyPart;
    private String side;
    private String description;
}
