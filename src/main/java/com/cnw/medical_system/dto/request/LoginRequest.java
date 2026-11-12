package com.cnw.medical_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    String tenDangNhap;

    @NotBlank(message = "Mật khẩu không được để trống")
    String matKhau;
}
