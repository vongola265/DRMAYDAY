package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    @JsonProperty("full_name")
    @JsonAlias({ "full_name", "hoTen" })
    String hoTen;

    @JsonProperty("gender")
    @JsonAlias({ "gender", "gioiTinh" })
    String gioiTinh;

    @JsonProperty("date_of_birth")
    @JsonAlias({ "date_of_birth", "ngaySinh" })
    LocalDate ngaySinh;

    @Min(value = 1900, message = "Năm sinh không hợp lệ")
    @JsonProperty("year_of_birth")
    @JsonAlias({ "year_of_birth", "namSinh" })
    Integer namSinh;

    @JsonProperty("hamlet_address")
    @JsonAlias({ "hamlet_address", "diaChiSoNhaThonPho" })
    String diaChiSoNhaThonPho;

    @JsonProperty("commune_address")
    @JsonAlias({ "commune_address", "diaChiXaPhuong" })
    String diaChiXaPhuong;

    @JsonProperty("city_address")
    @JsonAlias({ "city_address", "diaChiTinhTp" })
    String diaChiTinhTp;

    @JsonProperty("career")
    @JsonAlias({ "career", "ngheNghiep" })
    String ngheNghiep;

    @JsonProperty("ethnic")
    @JsonAlias({ "ethnic", "danToc" })
    String danToc;

    @JsonProperty("health_insurance_number")
    @JsonAlias({ "health_insurance_number", "bhytSoThe" })
    String bhytSoThe;

    @JsonProperty("health_insurance_validate")
    @JsonAlias({ "health_insurance_validate", "bhytGiaTriDen" })
    LocalDate bhytGiaTriDen;

    @JsonProperty("next_of_kin_name")
    @JsonAlias({ "next_of_kin_name", "nguoiNhaHoTenDiaChi" })
    String nguoiNhaHoTenDiaChi;

    @Pattern(regexp = "^0(3|5|7|8|9)[0-9]{8}$", message = "SĐT người nhà không đúng định dạng VN")
    @JsonProperty("next_of_kin_phone")
    @JsonAlias({ "next_of_kin_phone", "nguoiNhaSoDienThoai" })
    String nguoiNhaSoDienThoai;
}