package com.cnw.medical_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cnw.medical_system.dto.request.PermissionRequest;
import com.cnw.medical_system.dto.request.UpdatePermissionRequest;
import com.cnw.medical_system.dto.response.PermissionListResponse;
import com.cnw.medical_system.dto.response.PermissionResponse;
import com.cnw.medical_system.entity.QuyenHan;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.repository.QuyenHanRepository;
import com.cnw.medical_system.repository.TaiKhoanRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionService {
    QuyenHanRepository quyenHanRepository;
    TaiKhoanRepository taiKhoanRepository;
    static Pattern XSS_PATTERN = Pattern.compile("(?i)<\\s*/?\\s*(script|iframe|img|svg|object|embed).*?>");

    // Api 17: set_permission
    @Transactional
    public PermissionResponse createPermission(PermissionRequest request) {
        checkAdminPermission();
        String name = request.getName().trim();
        validateNoXss(request.getDescription());

        // kiểm tra trùng tên quyền hạn
        if (quyenHanRepository.existsByTenQuyenHanIgnoreCase(name)) {
            throw new RuntimeException("Quyền hạn '" + name + "' đã tồn tại trên hệ thống");
        }
        QuyenHan quyenHan = QuyenHan.builder().tenQuyenHan(name).moTa(request.getDescription()).build();
        quyenHan = quyenHanRepository.save(quyenHan);
        return toPermissionResponse(quyenHan);
    }

    // Api 18: get_list_permissions
    @Transactional(readOnly = true)
    public PermissionListResponse getAllPermissions() {
        checkAdminPermission();
        List<PermissionResponse> list = quyenHanRepository.findAll(Sort.by("maQuyenHan").ascending()).stream()
                .map(this::toPermissionResponse).toList();
        return PermissionListResponse.builder().data(list).build();
    }

    // Api 19:edit_permission
    @Transactional
    public PermissionResponse updatePermission(Long id, UpdatePermissionRequest request) {
        checkAdminPermission();
        if (id == null || id <= 0) {
            throw new RuntimeException("ID quyền hạn phải là số nguyên dương");
        }
        if (request == null || request.isEmpty()) {
            throw new RuntimeException("Dữ liệu cập nhật không được để trống");
        }
        QuyenHan quyenHan = quyenHanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quyền hạn ID " + id + " không tồn tại"));
        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (newName.isEmpty()) {
                throw new RuntimeException("Tên quyền hạn không được để trống");
            }
            if (quyenHanRepository.existsByTenQuyenHanIgnoreCaseAndMaQuyenHanNot(newName, id)) {
                throw new RuntimeException("Quyền hạn '" + newName + "' đã được sử dụng");
            }
            quyenHan.setTenQuyenHan(newName);
        }
        if (request.getDescription() != null) {
            validateNoXss(request.getDescription());
            quyenHan.setMoTa(request.getDescription());
        }
        quyenHan.setThoiDiemCapNhat(LocalDateTime.now());
        quyenHan = quyenHanRepository.save(quyenHan);
        return toPermissionResponse(quyenHan);
    }

    // Api 20: delete_permission
    @Transactional
    public void deletePermission(Long id) {
        checkAdminPermission();
        if (id == null || id <= 0) {
            throw new RuntimeException("ID quyền hạn phải là số nguyên dương");
        }
        QuyenHan quyenHan = quyenHanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quyền hạn ID " + id + " không tồn tại hoặc đã bị xóa"));
        quyenHanRepository.delete(quyenHan);
    }

    private PermissionResponse toPermissionResponse(QuyenHan q) {
        return PermissionResponse.builder().id(q.getMaQuyenHan()).name(q.getTenQuyenHan()).description(q.getMoTa())
                .createdAt(q.getThoiDiemTao()).updatedAt(q.getThoiDiemCapNhat()).build();
    }

    private void validateNoXss(String text) {
        if (text != null && XSS_PATTERN.matcher(text).find()) {
            throw new RuntimeException("Nội dung chứa mã HTML/Script không hợp lệ");
        }
    }

    private void checkAdminPermission() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan currentAccount = taiKhoanRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản"));
        if (currentAccount.getVaiTro() == null
                || !"admin".equalsIgnoreCase(currentAccount.getVaiTro().getTenVaiTro())) {
            throw new RuntimeException(
                    "Bạn không có quyền thực hiện hành động này. Chỉ Admin mới được quản lý quyền hạn!");
        }
    }
}
