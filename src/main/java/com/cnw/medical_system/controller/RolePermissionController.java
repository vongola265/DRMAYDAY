package com.cnw.medical_system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnw.medical_system.dto.request.RolePermissionRequest;
import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.dto.response.RolePermissionResponse;
import com.cnw.medical_system.service.RolePermissionService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/role_permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RolePermissionController {

    RolePermissionService rolePermissionService;

    // API STT 21: set_role_permissions - Gán quyền cho vai trò
    @PostMapping
    public ApiResponse<List<RolePermissionResponse>> assignPermissions(
            @RequestBody @Valid RolePermissionRequest request) {
        return ApiResponse.success(rolePermissionService.assignPermissions(request).getData());
    }

    // API STT 22: get_role_permissions - Xem danh sách quyền của vai trò
    @GetMapping("/role/{roleId}")
    public ApiResponse<List<RolePermissionResponse>> getRolePermissions(@PathVariable Long roleId) {
        return ApiResponse.success(rolePermissionService.getPermissionsByRole(roleId).getData());
    }

    // API STT 23: delete_role_permissions - Thu hồi quyền khỏi vai trò
    @DeleteMapping("/role/{roleId}/permission/{permissionId}")
    public ApiResponse<Map<String, Object>> revokePermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        rolePermissionService.revokePermission(roleId, permissionId);
        return ApiResponse.success(Map.of());
    }
}
