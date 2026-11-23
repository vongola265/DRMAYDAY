package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateUserRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
    @JsonProperty("full_name")
    String hoTen;

    @Size(min = 4, max = 50, message = "Tên đăng nhập từ 4 đến 50 ký tự")
    @JsonProperty("username")
    String tenDangNhap;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại phải gồm 10 chữ số hợp lệ")
    @JsonProperty("phone")
    String soDienThoai;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @JsonProperty("password")
    String matKhau;

    @NotNull(message = "Mã vai trò không được để trống")
    @Positive(message = "Mã vai trò phải là số nguyên dương")
    @JsonProperty("role_id")
    Long maVaiTro;

    @JsonProperty("gender")
    String gioiTinh;

    @Min(value = 0, message = "is_active chỉ nhận giá trị 0 hoặc 1")
    @Max(value = 1, message = "is_active chỉ nhận giá trị 0 hoặc 1")
    @JsonProperty("is_active")
    Integer isActive;
}
