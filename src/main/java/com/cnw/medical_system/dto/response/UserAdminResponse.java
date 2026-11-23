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
public class UserAdminResponse {

    @JsonProperty("user_id")
    Long userId;

    @JsonProperty("username")
    String username;

    @JsonProperty("full_name")
    String fullName;

    @JsonProperty("phone")
    String phone;

    @JsonProperty("gender")
    String gender;

    @JsonProperty("role_id")
    Long roleId;

    @JsonProperty("role_name")
    String roleName;

    @JsonProperty("is_active")
    Integer isActive;

    @JsonProperty("created_at")
    LocalDateTime createdAt;
}
