package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRequest {

    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
    @JsonProperty("full_name")
    String hoTen;

    @JsonProperty("gender")
    String gioiTinh;

    @Positive(message = "Mã vai trò phải là số nguyên dương")
    @JsonProperty("role_id")
    Long maVaiTro;

    @Min(value = 0, message = "is_active chỉ nhận giá trị 0 hoặc 1")
    @Max(value = 1, message = "is_active chỉ nhận giá trị 0 hoặc 1")
    @JsonProperty("is_active")
    Integer isActive;

    // Kiểm tra xem request có rỗng không
    public boolean isEmpty() {
        return hoTen == null && gioiTinh == null && maVaiTro == null && isActive == null;
    }
}