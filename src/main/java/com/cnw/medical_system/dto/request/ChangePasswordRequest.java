package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    @JsonProperty("old_password")
    String matKhauCu;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất có 6 ký tự")
    @JsonProperty("new_password")
    String matKhauMoi;

    @NotBlank(message = "Mật khẩu xác nhận không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất có 6 ký tự")
    @JsonProperty("confirm_password")
    String matKhauXacNhan;

}
