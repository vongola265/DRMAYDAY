package com.cnw.medical_system.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginResponse {
    String token; // chuỗi jwt token
    boolean authenticated;// true nếu thành công
    String tenDangNhap; // tên vai trò
    String vaiTro;// ten vai trò
}
