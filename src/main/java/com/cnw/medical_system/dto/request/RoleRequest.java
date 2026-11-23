package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleRequest {
    @NotBlank(message = "Tên vai trò không được để trống")
    @Size(max = 50, message = "Tên vai trò tối đa 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Tên vai trò không được chứa khoảng trắng hoặc ký tự đặc biệt")
    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @Positive(message = "Mã vai trò cha phỉa là số nguyên dương")
    @JsonProperty("parent_role_id")
    Long parentRoleId;
}
