package com.cnw.medical_system.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMedicalRecordStatusRequest {

    @NotBlank(message = "Trạng thái (status) không được để trống")
    @Size(max = 30, message = "status tối đa 30 ký tự")
    @JsonProperty("status")
    String status;

    @Size(max = 1000, message = "Ghi chú chuyển trạng thái tối đa 1000 ký tự")
    @JsonProperty("note")
    String note;
}
