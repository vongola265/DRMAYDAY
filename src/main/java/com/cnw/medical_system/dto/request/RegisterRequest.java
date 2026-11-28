package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @Size(min = 4, max = 50, message = "Tên đăng nhập phải từ 4 đến 50 kí tự")
    @JsonAlias("username")
    String tenDangNhap;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 64, message = "Mật khẩu phải từ 6 đến 64 ký tự")
    @JsonAlias("password")
    String matKhau;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    @JsonAlias("full_name")
    String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại phải gồm 10 chữ số hợp lệ")
    @JsonAlias("phone")
    String soDienThoai;

    @JsonProperty("gender")
    String gioiTinh;
}
