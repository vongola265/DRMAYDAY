package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @Size(max = 255, message = "Giới tính tối đa 255 ký tự")
    @JsonProperty("gender")
    @JsonAlias({ "gender", "gioiTinh" })
    String gioiTinh;

    @JsonProperty("date_of_birth")
    @JsonAlias({ "date_of_birth", "ngaySinh" })
    LocalDate ngaySinh;

    @Min(value = 1900, message = "Năm sinh không hợp lệ")
    @Max(value = 2026, message = "Năm sinh không hợp lệ")
    @JsonProperty("year_of_birth")
    @JsonAlias({ "year_of_birth", "namSinh" })
    Integer namSinh;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    @JsonProperty("hamlet_address")
    @JsonAlias({ "hamlet_address", "diaChiSoNhaThonPho" })
    String diaChiSoNhaThonPho;

    @Size(max = 255, message = "Xã/phường tối đa 255 ký tự")
    @JsonProperty("commune_address")
    @JsonAlias({ "commune_address", "diaChiXaPhuong" })
    String diaChiXaPhuong;

    @Size(max = 255, message = "Tỉnh/TP tối đa 255 ký tự")
    @JsonProperty("city_address")
    @JsonAlias({ "city_address", "diaChiTinhTp" })
    String diaChiTinhTp;

    @Size(max = 255, message = "Nghề nghiệp tối đa 255 ký tự")
    @JsonProperty("career")
    @JsonAlias({ "career", "ngheNghiep" })
    String ngheNghiep;

    @Size(max = 255, message = "Dân tộc tối đa 255 ký tự")
    @JsonProperty("ethnic")
    @JsonAlias({ "ethnic", "danToc" })
    String danToc;

    @Size(max = 255, message = "Số BHYT tối đa 255 ký tự")
    @JsonProperty("health_insurance_number")
    @JsonAlias({ "health_insurance_number", "bhytSoThe" })
    String bhytSoThe;

    @JsonProperty("health_insurance_validate")
    @JsonAlias({ "health_insurance_validate", "bhytGiaTriDen" })
    LocalDate bhytGiaTriDen;

    @Size(max = 255, message = "Tên người nhà tối đa 255 ký tự")
    @JsonProperty("next_of_kin_name")
    @JsonAlias({ "next_of_kin_name", "nguoiNhaHoTenDiaChi" })
    String nguoiNhaHoTenDiaChi;

    @Pattern(regexp = "^0(3|5|7|8|9)[0-9]{8}$", message = "SĐT người nhà không đúng định dạng VN")
    @JsonProperty("next_of_kin_phone")
    @JsonAlias({ "next_of_kin_phone", "nguoiNhaSoDienThoai" })
    String nguoiNhaSoDienThoai;

    @JsonIgnore
    @Builder.Default
    Map<String, Object> unknownFields = new HashMap<>();

    @JsonIgnore
    @Builder.Default
    Set<String> presentFields = new HashSet<>();

    @JsonAnySetter
    public void addUnknownField(String key, Object value) {
        unknownFields.put(key, value);
    }

    public void setHoTen(String hoTen) { this.hoTen = hoTen; presentFields.add("hoTen"); }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; presentFields.add("gioiTinh"); }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; presentFields.add("ngaySinh"); }
    public void setNamSinh(Integer namSinh) { this.namSinh = namSinh; presentFields.add("namSinh"); }
    public void setDiaChiSoNhaThonPho(String v) { this.diaChiSoNhaThonPho = v; presentFields.add("diaChiSoNhaThonPho"); }
    public void setDiaChiXaPhuong(String v) { this.diaChiXaPhuong = v; presentFields.add("diaChiXaPhuong"); }
    public void setDiaChiTinhTp(String v) { this.diaChiTinhTp = v; presentFields.add("diaChiTinhTp"); }
    public void setNgheNghiep(String v) { this.ngheNghiep = v; presentFields.add("ngheNghiep"); }
    public void setDanToc(String v) { this.danToc = v; presentFields.add("danToc"); }
    public void setBhytSoThe(String v) { this.bhytSoThe = v; presentFields.add("bhytSoThe"); }
    public void setBhytGiaTriDen(LocalDate v) { this.bhytGiaTriDen = v; presentFields.add("bhytGiaTriDen"); }
    public void setNguoiNhaHoTenDiaChi(String v) { this.nguoiNhaHoTenDiaChi = v; presentFields.add("nguoiNhaHoTenDiaChi"); }
    public void setNguoiNhaSoDienThoai(String v) { this.nguoiNhaSoDienThoai = v; presentFields.add("nguoiNhaSoDienThoai"); }

    public boolean hasNoValidFields() {
        return presentFields.isEmpty();
    }
}