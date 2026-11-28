package com.cnw.medical_system.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cnw.medical_system.dto.request.RolePermissionRequest;
import com.cnw.medical_system.dto.response.RolePermissionListResponse;
import com.cnw.medical_system.dto.response.RolePermissionResponse;
import com.cnw.medical_system.entity.QuyenHan;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.entity.VaiTro;
import com.cnw.medical_system.entity.VaiTroQuyenHan;
import com.cnw.medical_system.entity.VaiTroQuyenHanId;
import com.cnw.medical_system.repository.QuyenHanRepository;
import com.cnw.medical_system.repository.TaiKhoanRepository;
import com.cnw.medical_system.repository.VaiTroQuyenHanRepository;
import com.cnw.medical_system.repository.VaiTroRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RolePermissionService {

    VaiTroQuyenHanRepository vaiTroQuyenHanRepository;
    VaiTroRepository vaiTroRepository;
    QuyenHanRepository quyenHanRepository;
    TaiKhoanRepository taiKhoanRepository;

    // API STT 21: set_role_permissions - Gán danh sách quyền cho vai trò
    @Transactional
    public RolePermissionListResponse assignPermissions(RolePermissionRequest request) {
        TaiKhoan currentAdmin = checkAdminPermission();

        // 1. Tìm vai trò cần gán
        VaiTro vaiTro = vaiTroRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Vai trò ID " + request.getRoleId() + " không tồn tại"));

        // 2. Loại bỏ ID trùng lặp nhưng giữ nguyên thứ tự
        Set<Long> uniquePermissionIds = new LinkedHashSet<>(request.getPermissions());

        // 3. Kiểm tra toàn vẹn (All-or-nothing): tất cả permission ID phải tồn tại
        // trước khi lưu
        List<QuyenHan> permissionsToAssign = new ArrayList<>();
        for (Long permId : uniquePermissionIds) {
            QuyenHan qh = quyenHanRepository.findById(permId)
                    .orElseThrow(() -> new RuntimeException("Quyền hạn ID " + permId + " không tồn tại"));
            permissionsToAssign.add(qh);
        }

        // 4. Lưu vào bảng nối vaitroquyenhan (Idempotent: nếu đã có rồi thì bỏ qua,
        // không báo lỗi)
        for (QuyenHan qh : permissionsToAssign) {
            VaiTroQuyenHanId id = new VaiTroQuyenHanId(vaiTro.getMaVaiTro(), qh.getMaQuyenHan());
            if (!vaiTroQuyenHanRepository.existsById(id)) {
                VaiTroQuyenHan entity = VaiTroQuyenHan.builder()
                        .id(id)
                        .vaiTro(vaiTro)
                        .quyenHan(qh)
                        .nguoiGan(currentAdmin.getNguoi())
                        .build();
                vaiTroQuyenHanRepository.save(entity);
            }
        }

        return getPermissionsByRole(vaiTro.getMaVaiTro());
    }

    // API STT 22: get_role_permissions - Xem danh sách quyền của một vai trò
    @Transactional(readOnly = true)
    public RolePermissionListResponse getPermissionsByRole(Long roleId) {
        checkAdminPermission();

        if (roleId == null || roleId <= 0) {
            throw new RuntimeException("ID vai trò phải là số nguyên dương");
        }
        if (!vaiTroRepository.existsById(roleId)) {
            throw new RuntimeException("Vai trò ID " + roleId + " không tồn tại");
        }

        List<RolePermissionResponse> list = vaiTroQuyenHanRepository
                .findById_MaVaiTroOrderById_MaQuyenHanAsc(roleId)
                .stream()
                .map(vtqh -> RolePermissionResponse.builder()
                        .rolePermissionId(vtqh.getId().getMaVaiTro() + "_" + vtqh.getId().getMaQuyenHan())
                        .roleId(vtqh.getId().getMaVaiTro())
                        .permissionId(vtqh.getId().getMaQuyenHan())
                        .name(vtqh.getQuyenHan().getTenQuyenHan())
                        .description(vtqh.getQuyenHan().getMoTa())
                        .assignedAt(vtqh.getThoiDiemGan())
                        .build())
                .toList();

        return RolePermissionListResponse.builder()
                .data(list)
                .build();
    }

    // API STT 23: delete_role_permissions - Thu hồi 1 quyền khỏi vai trò
    @Transactional
    public void revokePermission(Long roleId, Long permissionId) {
        checkAdminPermission();

        if (roleId == null || roleId <= 0 || permissionId == null || permissionId <= 0) {
            throw new RuntimeException("ID vai trò và ID quyền hạn phải là số nguyên dương");
        }

        VaiTro vaiTro = vaiTroRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Vai trò ID " + roleId + " không tồn tại"));

        // Quy tắc bảo vệ quyền của Admin
        if ("admin".equalsIgnoreCase(vaiTro.getTenVaiTro())) {
            throw new RuntimeException("Không được phép thu hồi quyền của vai trò Admin");
        }

        if (!quyenHanRepository.existsById(permissionId)) {
            throw new RuntimeException("Quyền hạn ID " + permissionId + " không tồn tại");
        }

        VaiTroQuyenHanId id = new VaiTroQuyenHanId(roleId, permissionId);
        VaiTroQuyenHan link = vaiTroQuyenHanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vai trò này chưa được gán quyền hạn ID " + permissionId));

        vaiTroQuyenHanRepository.delete(link);
    }

    private TaiKhoan checkAdminPermission() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan currentAccount = taiKhoanRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản"));
        if (currentAccount.getVaiTro() == null
                || !"admin".equalsIgnoreCase(currentAccount.getVaiTro().getTenVaiTro())) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này. Chỉ Admin mới được phân quyền!");
        }
        return currentAccount;
    }
}