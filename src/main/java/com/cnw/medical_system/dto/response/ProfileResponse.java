package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileResponse {

    @JsonProperty("user_id")
    Long maTaiKhoan;

    @JsonProperty("username")
    String tenDangNhap;

    @JsonProperty("full_name")
    String hoTen;

    @JsonProperty("phone")
    String soDienThoai;

    @JsonProperty("gender")
    String gioiTinh;

    @JsonProperty("date_of_birth")
    LocalDate ngaySinh;

    @JsonProperty("year_of_birth")
    Integer namSinh;

    @JsonProperty("hamlet_address")
    String diaChiSoNhaThonPho;

    @JsonProperty("commune_address")
    String diaChiXaPhuong;

    @JsonProperty("city_address")
    String diaChiTinhTp;

    @JsonProperty("role_id")
    Long maVaiTro;

    @JsonProperty("role_name")
    String tenVaiTro;

    @JsonProperty("is_active")
    Boolean trangThaiHoatDong;

    @JsonProperty("first_visit")
    LocalDateTime thoiDiemTao;

    // Thông tin từ bảng benhnhan
    @JsonProperty("ma_his")
    String maHis;

    @JsonProperty("career")
    String ngheNghiep;

    @JsonProperty("ethnic")
    String danToc;

    @JsonProperty("health_insurance_number")
    String bhytSoThe;

    @JsonProperty("health_insurance_validate")
    LocalDate bhytGiaTriDen;

    @JsonProperty("next_of_kin_name")
    String nguoiNhaHoTenDiaChi;

    @JsonProperty("next_of_kin_phone")
    String nguoiNhaSoDienThoai;
}