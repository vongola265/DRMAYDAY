package com.cnw.medical_system.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermissionRequest {

    @NotNull(message = "Mã vai trò không được để trống")
    @Positive(message = "Mã vai trò phải là số nguyên dương")
    @JsonProperty("role_id")
    Long roleId;

    @NotEmpty(message = "Danh sách quyền hạn không được để trống")
    @Size(max = 100, message = "Chỉ được gán tối đa 100 quyền mỗi lần")
    @JsonProperty("permissions")
    List<@NotNull(message = "ID quyền không được null") @Positive(message = "ID quyền phải là số nguyên dương") Long> permissions;
}