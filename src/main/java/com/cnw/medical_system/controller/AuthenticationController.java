package com.cnw.medical_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnw.medical_system.dto.request.LoginRequest;
import com.cnw.medical_system.dto.request.RegisterRequest;
import com.cnw.medical_system.dto.request.ResetPasswordRequest;
import com.cnw.medical_system.dto.response.LoginResponse;
import com.cnw.medical_system.service.AuthenticationService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

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
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody RegisterRequest request) {
        String result = authenticationService.signup(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-info")
    public ResponseEntity<UserResponse> getMyInfo() {
        return ResponseEntity.ok(authenticationService.getMyInfo());
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody @Valid ChangePasswordRequest request,
            HttpServletRequest htttpRequest) {
        String result = authenticationService.changePassword(request, htttpRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create-staff")
    public ResponseEntity<String> createStaff(@RequestBody @Valid CreateStaffRequest request) {
        String result = authenticationService.createStaff(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String result = authenticationService.logout(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPasword(@RequestBody @Valid ForgotPasswordRequest request) {
        String result = authenticationService.forgotPassword(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        String result = authenticationService.resetPassWord(request);
        return ResponseEntity.ok(result);
    }

}
