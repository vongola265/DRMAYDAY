package com.cnw.medical_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnw.medical_system.dto.request.UpdateProfileRequest;
import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.dto.response.ProfileResponse;
import com.cnw.medical_system.service.UserService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileController {
    UserService userService;

    @GetMapping
    public ApiResponse<ProfileResponse> getUserInfo() {
        return ApiResponse.success(userService.getUserInfo());
    }

    @PutMapping
    public ApiResponse<ProfileResponse> setUserInfo(@RequestBody @Valid UpdateProfileRequest request) {
        return ApiResponse.success(userService.setUserInfo(request));
    }
}
