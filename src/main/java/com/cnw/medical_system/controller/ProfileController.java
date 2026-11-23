package com.cnw.medical_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnw.medical_system.dto.request.UpdateProfileRequest;
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
    public ResponseEntity<ProfileResponse> getUserInfo() {
        return ResponseEntity.ok(userService.getUserInfo());
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> setUserInfo(@RequestBody @Valid UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.setUserInfo(request));
    }
}
