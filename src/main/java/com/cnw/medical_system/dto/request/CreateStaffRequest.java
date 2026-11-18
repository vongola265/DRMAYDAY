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
public class CreateStaffRequest {
    // @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 4, max = 50, message = "Tên đăng nhập phải từ 4 đến 50 ký tự")
    // @JsonProperty ("full_name")
    String tenDangNhap;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "mật khẩu phải có ít nhất 6 ký tự")
    @JsonProperty("password")
    String matKhau;

    @NotBlank(message = "Họ và tên không được để trống")
    @JsonProperty("full_name")
    String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10}$", message = "Số điện thoại phải gồm đúng 10 chữ số")
    @JsonProperty("phone")
    String soDienThoai;

    // @NotBlank(message = "Vai trò không được để trống ")
    String tenVaiTro;// bsi hoặc dieu_duong hoặc admin

    @JsonProperty("role_id")
    Long maVaiTro;
    @JsonProperty("branch_id")
    Long maChiNhanh;
    @JsonProperty("department_id")
    Long maKhoa;
    @JsonProperty("room_id")
    Long maPhong;
    @JsonProperty("gender")
    String gioiTinh;
}
