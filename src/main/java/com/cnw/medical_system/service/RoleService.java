package com.cnw.medical_system.service;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cnw.medical_system.dto.request.RoleRequest;
import com.cnw.medical_system.dto.response.RoleListResponse;
import com.cnw.medical_system.dto.response.RoleResponse;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.entity.VaiTro;
import com.cnw.medical_system.repository.TaiKhoanRepository;
import com.cnw.medical_system.repository.VaiTroRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    VaiTroRepository vaiTroRepository;
    TaiKhoanRepository taiKhoanRepository;
    // Danh sách tên vai trò hệ thống mặc định (không cho phép tạo trùng)
    static Set<String> RESERVED_ROLES = Set.of("admin", "doctor", "patient", "nurse", "bac_si", "benh_nhan",
            "dieu_duong");

    // API 15 : set_role
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        // 1.Kiểm tra quyền Admin
        checkAdminPermission();

        String roleName = request.getName().trim().toLowerCase();

        // 2. Chặn đặt tên trung vai trò mặc định của hệ thống
        if (RESERVED_ROLES.contains(roleName)) {
            throw new RuntimeException("Không được phép tạo vai trò trùng với vai trò hệ thống đã có: " + roleName);
        }
        // 3. Kiểm tra xem tên vai trò đã tồn tại trong CSDL chưa
        if (vaiTroRepository.existsByTenVaiTroIgnoreCase(roleName)) {
            throw new RuntimeException("Vai trò '" + "' đã tồn tại trên hệ thống");
        }
        // 4.Kiểm tra parent_role_id nếu có truyền lên
        if (request.getParentRoleId() != null) {
            if (!vaiTroRepository.existsById(request.getParentRoleId())) {
                throw new RuntimeException("Vai trò cha ID " + request.getParentRoleId() + " không tồn tại");
            }
        }
        // 5. Lưu vào CSDL
        VaiTro vaiTro = VaiTro.builder().tenVaiTro(roleName).tenHienThi(request.getName().trim())
                .moTaVaiTro(request.getDescription()).build();
        vaiTro = vaiTroRepository.save(vaiTro);
        return RoleResponse.builder().id(vaiTro.getMaVaiTro()).name(vaiTro.getTenVaiTro())
                .description(vaiTro.getMoTaVaiTro()).parentRoleId(request.getParentRoleId()).build();
    }

    // API 16: get_list_roles
    @Transactional(readOnly = true)
    public RoleListResponse getAllRoles() {
        checkAdminPermission();
        // Lấy tất cả vai trò sắp xếp tăng dần theo id
        List<VaiTro> list = vaiTroRepository.findAll(Sort.by("maVaiTro").ascending());
        List<RoleResponse> roleResponses = list.stream().map(vt -> RoleResponse.builder().id(vt.getMaVaiTro())
                .name(toStandardRoleName(vt.getTenVaiTro())).description(vt.getMoTaVaiTro()).parentRoleId(null).build())
                .toList();
        return RoleListResponse.builder().data(roleResponses).build();
    }

    // Chuẩn hóa tên vai trò cốt lõi để tương thích cả tiếng Việt và test suite quốc
    // tế
    private String toStandardRoleName(String roleName) {
        if (roleName == null)
            return null;
        return switch (roleName.toLowerCase()) {
            case "bac_si" -> "doctor";
            case "benh_nhan" -> "patient";
            case "dieu_duong" -> "nurse";
            default -> roleName;
        };
    }

    private void checkAdminPermission() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan currentAccount = taiKhoanRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản"));
        if (currentAccount.getVaiTro() == null
                || !"admin".equalsIgnoreCase(currentAccount.getVaiTro().getTenVaiTro())) {
            throw new RuntimeException(
                    "Bạn không có quyền thực hiện hành động này. Chỉ Admin mới được quản lý vai trò!");
        }
    }
}
