package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"phone", "username"})
    String tenDangNhap;

    @NotBlank(message = "Mật khẩu không được để trống")
    @JsonAlias("password")
    String matKhau;
}
