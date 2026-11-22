package com.cnw.medical_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequest(@NotNull String department_id, @NotBlank @Size(max = 150) String name, String description) {}
