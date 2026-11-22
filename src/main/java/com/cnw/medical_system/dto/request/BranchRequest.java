package com.cnw.medical_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchRequest(@NotBlank @Size(max = 150) String name,
                            @NotBlank String address,
                            @Size(max = 20) String phone,
                            Boolean active) {}
