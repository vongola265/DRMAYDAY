package com.cnw.medical_system.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermissionResponse {

    @JsonProperty("role_permission_id")
    String rolePermissionId;

    @JsonProperty("role_id")
    Long roleId;

    @JsonProperty("permission_id")
    Long permissionId;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("assigned_at")
    LocalDateTime assignedAt;
}