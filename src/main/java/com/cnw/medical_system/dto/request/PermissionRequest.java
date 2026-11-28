package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionRequest {
    @NotBlank(message = "Tên quyền hạn không được để trống")
    @Size(max = 100, message = "Tên quyền hạn tối đa 100 ký tự")
    @Pattern(regexp = "^[a-zA-z0-9_-]+$", message = "Tên quyền hạn không được chứa khoảng trắng hoặc ký tự đặc biệt")
    @JsonProperty("name")
    String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự ")
    @JsonProperty("description")
    String description;

}
