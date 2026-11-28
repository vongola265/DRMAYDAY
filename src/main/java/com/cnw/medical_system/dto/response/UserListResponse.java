package com.cnw.medical_system.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class UserListResponse {

    @Builder.Default
    @JsonProperty("code")
    String code = "1000";

    @JsonProperty("total")
    Long total;

    @JsonProperty("data")
    List<UserAdminResponse> data;

}
