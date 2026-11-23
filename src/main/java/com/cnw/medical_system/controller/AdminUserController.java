package com.cnw.medical_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cnw.medical_system.dto.request.CreateUserRequest;
import com.cnw.medical_system.dto.request.UpdateUserRequest;
import com.cnw.medical_system.dto.response.UserAdminResponse;
import com.cnw.medical_system.dto.response.UserListResponse;
import com.cnw.medical_system.service.UserService;
import com.cnw.medical_system.dto.request.UpdateUserRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserController {
    UserService userService;

    // set_user - Admin tạo người dùng mới
    @PostMapping
    public ResponseEntity<UserAdminResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUserByAdmin(request));
    }

    // get_list_users - Admin lấy danh sách người dùng kèm phân trang & lọc đa điều
    // kiện
    @GetMapping
    public ResponseEntity<UserListResponse> getListUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "role_id", required = false) Long roleId,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "is_active", required = false) Integer isActive) {
        return ResponseEntity.ok(userService.getListUsers(page, limit, roleId, keyword, isActive));
    }

    // get_user_detail
    @GetMapping("/{id}")
    public ResponseEntity<UserAdminResponse> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserDetail(id));
    }

    // edit_user
    @PutMapping("/{id}")
    public ResponseEntity<UserAdminResponse> editUser(@PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.editUserByAdmin(id, request));
    }

    // delete_user
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        userService.deleteUserByAdmin(id);
        return ResponseEntity.ok(Map.of());
    }
}
