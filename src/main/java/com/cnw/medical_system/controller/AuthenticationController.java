package com.cnw.medical_system.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnw.medical_system.dto.request.LoginRequest;
import com.cnw.medical_system.dto.request.RegisterRequest;
import com.cnw.medical_system.dto.request.ResetPasswordRequest;
import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.dto.response.LoginResponse;
import com.cnw.medical_system.dto.response.UserAdminResponse;
import com.cnw.medical_system.service.AuthenticationService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.cnw.medical_system.dto.response.UserResponse;
import com.cnw.medical_system.dto.request.ChangePasswordRequest;
import com.cnw.medical_system.dto.request.CreateStaffRequest;
import com.cnw.medical_system.dto.request.ForgotPasswordRequest;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.success(authenticationService.authenticate(request));
    }

    @PostMapping("/signup")
    public ApiResponse<UserAdminResponse> signup(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.success(authenticationService.signup(request));
    }

    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.success(authenticationService.getMyInfo());
    }

    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(@RequestBody @Valid ChangePasswordRequest request,
            HttpServletRequest htttpRequest) {
        return ApiResponse.success(authenticationService.changePassword(request, htttpRequest));
    }

    @PostMapping("/create-staff")
    public ApiResponse<UserAdminResponse> createStaff(@RequestBody @Valid CreateStaffRequest request) {
        return ApiResponse.success(authenticationService.createStaff(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(HttpServletRequest request) {
        authenticationService.logout(request);
        return ApiResponse.success(Map.of());
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPasword(@RequestBody @Valid ForgotPasswordRequest request) {
        return ApiResponse.success(authenticationService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        return ApiResponse.success(authenticationService.resetPassWord(request));
    }
}
