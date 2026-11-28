package com.cnw.medical_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    String code;
    String message;
    T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("1000", "Success", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>("1000", "Success", null);
    }
}
