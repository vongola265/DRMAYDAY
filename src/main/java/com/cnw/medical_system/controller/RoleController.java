package com.cnw.medical_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnw.medical_system.dto.request.RoleRequest;
import com.cnw.medical_system.dto.response.RoleListResponse;
import com.cnw.medical_system.dto.response.RoleResponse;
import com.cnw.medical_system.service.RoleService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {
    RoleService roleService;

    // Api 15 : set_role
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@RequestBody @Valid RoleRequest request) {
        return ResponseEntity.ok(roleService.createRole(request));
    }

    // API 16: get_list_roles
    @GetMapping
    public ResponseEntity<RoleListResponse> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }
}
