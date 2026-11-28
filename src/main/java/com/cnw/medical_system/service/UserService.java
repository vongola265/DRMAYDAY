package com.cnw.medical_system.service;

import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.cnw.medical_system.dto.request.CreateUserRequest;
import com.cnw.medical_system.dto.request.UpdateProfileRequest;
import com.cnw.medical_system.dto.response.ProfileResponse;
import com.cnw.medical_system.dto.response.UserAdminResponse;
import com.cnw.medical_system.entity.BenhNhan;
import com.cnw.medical_system.entity.Nguoi;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.entity.VaiTro;
import com.cnw.medical_system.mapper.UserMapper;
import com.cnw.medical_system.repository.BenhNhanRepository;
import com.cnw.medical_system.repository.NguoiRepository;
import com.cnw.medical_system.repository.TaiKhoanRepository;
import com.cnw.medical_system.repository.VaiTroRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import com.cnw.medical_system.dto.response.UserListResponse;
import org.springframework.http.HttpStatus;
import com.cnw.medical_system.exception.ApiContractException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.Year;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.RuntimeErrorException;
import com.cnw.medical_system.dto.request.UpdateUserRequest;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    TaiKhoanRepository taiKhoanRepository;
    NguoiRepository nguoiRepository;
    BenhNhanRepository benhNhanRepository;
    VaiTroRepository vaiTroRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Api 14: delete_user
    @Transactional
    public void deleteUserByAdmin(Long id) {
        // 1.Kiểm tra quyền admin
        TaiKhoan currentAccount = getCurrentAccount();
        if (currentAccount.getVaiTro() == null
                || !"admin".equalsIgnoreCase(currentAccount.getVaiTro().getTenVaiTro()))
            throw new RuntimeException(
                    "Bạn không có quyền thực hiện hành động này. Chỉ Admin mới được xóa người dùng!");

        // 2.Validate ID hợp lệ
        if (id == null || id <= 0)
            throw new RuntimeException("ID người dùng phải là số nguyên dương");

        // 3.Tìm tài khoản cần xóa ( nếu không tìm thấy hoặc đã xóa thì ném lỗi 9992)
        TaiKhoan targetAccount = taiKhoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng với ID " + id + " không tồn tại hoặc đã bị xóa"));
        // 4.Quy tắc an toàn 1 : Admin không tự xóa chính mình
        if (currentAccount.getMaTaiKhoan().equals(targetAccount.getMaTaiKhoan())) {
            throw new RuntimeException("Admin không thể tự xóa tài khoản của chính mình");
        }
        // 5. Quy tắc an toàn 2: Khong được xóa Admin duy nhất còn lại trong hệ thống
        if ("admin".equalsIgnoreCase(targetAccount.getVaiTro().getTenVaiTro())) {
            long adminCount = taiKhoanRepository.countByVaiTro_TenVaiTroIgnoreCase("admin");
            if (adminCount <= 1)
                throw new RuntimeException("Không thể xóa tài khoản Admin duy nhất còn lại trong hệ thống");
        }
        // 6. Xóa tài khoản ( bảng taikhoan) không xóa bảng nguoi
        taiKhoanRepository.delete((targetAccount));
    }

    // Api 13: edit_user
    @Transactional
    public UserAdminResponse editUserByAdmin(Long id, UpdateUserRequest request) {
        // 1. Kiểm tra quyền Admin
        TaiKhoan currentAccount = getCurrentAccount();
        if (currentAccount.getVaiTro() == null
                || !"admin".equalsIgnoreCase(currentAccount.getVaiTro().getTenVaiTro())) {
            throw new RuntimeException(
                    "Bạn không có quyền thực hiện hành động này. Chỉ Admin mới được sửa người dùng!");
        }
        // 2. Validate ID hợp lệ
        if (id == null || id <= 0) {
            throw new RuntimeException("ID người dùng phải là số nguyên dương");
        }
        // 3. Kiểm tra body rỗng
        if (request == null || request.isEmpty()) {
            throw new RuntimeException("Dữ liệu cập nhật không được để trống");
        }
        // 4. Kiểm tra họ tên nếu truyền lên không được là chuỗi trắng
        if (request.getHoTen() != null && request.getHoTen().trim().isEmpty()) {
            throw new ApiContractException(HttpStatus.BAD_REQUEST, "1004", "Họ và tên không được để trống");
        }
        // 5. Tìm tài khoản cần sửa
        TaiKhoan targetAccount = taiKhoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng với ID " + id + " không tồn tại"));
        // 6. Quy tắc an toàn: Admin không được tự khóa hoặc tự giáng chức chính mình
        if (currentAccount.getMaTaiKhoan().equals(targetAccount.getMaTaiKhoan())) {
            if (request.getIsActive() != null && request.getIsActive() == 0) {
                throw new RuntimeException("Admin không thể tự khóa tài khoản của chính mình");
            }
            if (request.getMaVaiTro() != null
                    && !request.getMaVaiTro().equals(currentAccount.getVaiTro().getMaVaiTro())) {
                throw new RuntimeException("Admin không thể tự thay đổi vai trò của chính mình");
            }
        }
        // 7. Cập nhật họ tên và giới tính (nếu có truyền)
        Nguoi nguoi = targetAccount.getNguoi();
        if (nguoi != null) {
            if (request.getHoTen() != null && !request.getHoTen().isBlank()) {
                nguoi.setHoTen(request.getHoTen().trim());
            }
            if (request.getGioiTinh() != null && !request.getGioiTinh().isBlank()) {
                nguoi.setGioiTinh(normalizeGender(request.getGioiTinh()));
            }
            nguoiRepository.save(nguoi);
        }
        // 8. Cập nhật trạng thái hoạt động (is_active)
        if (request.getIsActive() != null) {
            targetAccount.setTrangThaiHoatDong(request.getIsActive() == 1);
        }
        // 9. Cập nhật vai trò (role_id)
        if (request.getMaVaiTro() != null) {
            VaiTro newRole = vaiTroRepository.findById(request.getMaVaiTro())
                    .orElseThrow(() -> new RuntimeException("Vai trò ID " + request.getMaVaiTro() + " không tồn tại"));
            // Chặn không cho gán quyền Admin
            if ("admin".equalsIgnoreCase(newRole.getTenVaiTro())) {
                throw new RuntimeException("Không được phép gán vai trò Admin qua chức năng này");
            }
            // Chặn chuyển đổi giữa nhóm Bệnh nhân và nhóm Nhân sự y tế
            VaiTro oldRole = targetAccount.getVaiTro();
            if (oldRole != null) {
                boolean oldIsPatient = isPatientRole(oldRole.getTenVaiTro());
                boolean newIsPatient = isPatientRole(newRole.getTenVaiTro());
                if (oldIsPatient != newIsPatient) {
                    throw new RuntimeException(
                            "Không được chuyển đổi vai trò giữa nhóm Bệnh nhân và nhóm Nhân sự y tế");
                }
            }
            targetAccount.setVaiTro(newRole);
        }
        // 10. Lưu và trả về kết quả
        targetAccount = taiKhoanRepository.save(targetAccount);
        return userMapper.toUserAdminResponse(targetAccount);
    }

    private boolean isPatientRole(String roleName) {
        return "patient".equalsIgnoreCase(roleName) || "benh_nhan".equalsIgnoreCase(roleName);
    }

    // Api 12 : get_user_detail
    @Transactional(readOnly = true)
    public UserAdminResponse getUserDetail(Long id) {
        // 1. Kiểm tra quyền admin
        TaiKhoan currentAccount = getCurrentAccount();
        if (currentAccount.getVaiTro() == null || !"admin".equalsIgnoreCase(currentAccount.getVaiTro().getTenVaiTro()))
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này. Chỉ Admin mới được xem chi tiết!");
        // 2. Kiểm tra ID hợp lệ
        if (id == null || id <= 0)
            throw new RuntimeException("ID người dùng phải là số nguyên dương");
        // 3. Tìm tài khoản theo ID
        TaiKhoan taiKhoan = taiKhoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng với ID " + id + " không tồn tại"));
        // 4. Chuyển đổi sang DTO trả về
        return userMapper.toUserAdminResponse(taiKhoan);
    }

    // Api 11 : get_list_users - Lấy danh sách người dùng kèm phân trang & lọc đa
    // điều kiện
    @Transactional(readOnly = true)
    public UserListResponse getListUsers(int page, int limit, Long roleId, String keyword, Integer isActive) {
        // 1. Kiểm tra quyền Admin
        TaiKhoan currentAccount = getCurrentAccount();
        if (currentAccount.getVaiTro() == null
                || !"admin".equalsIgnoreCase(currentAccount.getVaiTro().getTenVaiTro())) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này. Chỉ Admin mới được xem danh sách!");
        }
        // 2.Validate tham số đầu vào
        if (page <= 0) {
            throw new RuntimeException("Số trang (page) phải lớn hơn hoặc bằng 1");
        }
        if (limit <= 0 || limit > 100) {
            throw new RuntimeException("Số lượng bản ghi trên một trang(limit) phải từ 1 đến 100");
        }
        if (roleId != null && roleId <= 0) {
            throw new RuntimeException("Mã vai trò (role_id) phải là số nguyên dương");
        }
        // 3. xây dựng điều kiện lọc động (Specification)
        Specification<TaiKhoan> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Nếu lọc theo vai trò
            if (roleId != null) {
                predicates.add(cb.equal(root.get("vaiTro").get("maVaiTro"), roleId));
            }
            // Nếu có lọc theo trạng thái hoạt động(1:active, 0:locked)
            if (isActive != null) {
                predicates.add(cb.equal(root.get("trangThaiHoatDong"), isActive == 1));
            }
            // Nếu cố tìm kiếm từ khóa ( họ tên hoặc sđt)
            if (keyword != null && !keyword.isBlank()) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("nguoi").get("hoTen")), searchPattern);
                Predicate phoneLike = cb.like(root.get("nguoi").get("soDienThoai"), searchPattern);
                predicates.add(cb.or(nameLike, phoneLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        // 4. Phân trang và sắp xếp tăng dần theo ID(maTaiKhoan asc)
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("maTaiKhoan").ascending());
        // 5. Truy vấn CSDL
        Page<TaiKhoan> pageResult = taiKhoanRepository.findAll(spec, pageable);
        // 6. Chuyển đổi Entity - > Dto
        List<UserAdminResponse> userList = pageResult.getContent().stream().map(userMapper::toUserAdminResponse)
                .toList();
        return UserListResponse.builder().total(pageResult.getTotalElements()).data(userList).build();
    }

    // API STT 10: set_user - Admin tạo người dùng mới
    @Transactional
    public UserAdminResponse createUserByAdmin(CreateUserRequest request) {
        // 1. Kiểm tra quyền Admin
        TaiKhoan currentAccount = getCurrentAccount();
        if (currentAccount.getVaiTro() == null
                || !"admin".equalsIgnoreCase(currentAccount.getVaiTro().getTenVaiTro())) {
            throw new RuntimeException(
                    "Bạn không có quyền thực hiện hành động này. Chỉ Admin mới được tạo người dùng!");
        }

        // 2. Kiểm tra trùng số điện thoại
        if (nguoiRepository.existsBySoDienThoai(request.getSoDienThoai())) {
            throw new RuntimeException("Số điện thoại này đã được sử dụng");
        }

        // 3. Tên đăng nhập: nếu truyền lên thì dùng username đó, nếu không truyền thì
        // mặc định lấy số điện thoại
        String username = (request.getTenDangNhap() != null && !request.getTenDangNhap().isBlank())
                ? request.getTenDangNhap().trim()
                : request.getSoDienThoai().trim();
        if (taiKhoanRepository.existsByTenDangNhap(username)) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trên hệ thống");
        }

        // 4. Tìm vai trò theo role_id
        VaiTro vaiTro = vaiTroRepository.findById(request.getMaVaiTro())
                .orElseThrow(() -> new RuntimeException("Vai trò ID " + request.getMaVaiTro() + " không tồn tại"));

        // Chặn không cho tạo tài khoản có vai trò Admin qua chức năng này
        if ("admin".equalsIgnoreCase(vaiTro.getTenVaiTro())) {
            throw new RuntimeException("Không được phép tạo tài khoản có vai trò Admin qua chức năng này");
        }

        // 5. Chuẩn hóa giới tính nếu có gửi lên
        String normalizedGender = null;
        if (request.getGioiTinh() != null && !request.getGioiTinh().isBlank()) {
            normalizedGender = normalizeGender(request.getGioiTinh());
        }

        // 6. Tạo và lưu thông tin người (nguoi)
        Nguoi nguoi = Nguoi.builder()
                .hoTen(request.getHoTen().trim())
                .soDienThoai(request.getSoDienThoai().trim())
                .gioiTinh(normalizedGender)
                .build();
        nguoi = nguoiRepository.save(nguoi);

        // 7. Nếu là bệnh nhân, tạo luôn bản ghi trong bảng benhnhan
        if ("patient".equalsIgnoreCase(vaiTro.getTenVaiTro()) || "benh_nhan".equalsIgnoreCase(vaiTro.getTenVaiTro())) {
            BenhNhan benhNhan = BenhNhan.builder()
                    .nguoi(nguoi)
                    .build();
            benhNhanRepository.save(benhNhan);
        }

        // 8. Tạo và lưu tài khoản (taikhoan)
        boolean isActive = (request.getIsActive() == null || request.getIsActive() == 1);
        TaiKhoan taiKhoan = TaiKhoan.builder()
                .nguoi(nguoi)
                .vaiTro(vaiTro)
                .tenDangNhap(username)
                .matKhauMaHoa(passwordEncoder.encode(request.getMatKhau()))
                .trangThaiHoatDong(isActive)
                .build();
        taiKhoan = taiKhoanRepository.save(taiKhoan);

        return userMapper.toUserAdminResponse(taiKhoan);
    }

    // api get user info
    @Transactional(readOnly = true)
    public ProfileResponse getUserInfo() {
        return mapToProfileResponse(getCurrentAccount());
    }

    // api set User info
    @Transactional
    public ProfileResponse setUserInfo(UpdateProfileRequest request) {
        if (request == null || request.hasNoValidFields()) {
            throw new ApiContractException(HttpStatus.BAD_REQUEST, "1002", "Không có dữ liệu hợp lệ để cập nhật");
        }
        if (request.getHoTen() != null && request.getHoTen().trim().isEmpty()) {
            throw new ApiContractException(HttpStatus.BAD_REQUEST, "1004", "Họ và tên không được để trống");
        }
        // Kiểm tra XSS đơn giản
        for (String val : java.util.Arrays.asList(request.getHoTen(), request.getDiaChiSoNhaThonPho(),
                request.getDiaChiXaPhuong(), request.getDiaChiTinhTp(), request.getNgheNghiep(),
                request.getDanToc(), request.getBhytSoThe(), request.getNguoiNhaHoTenDiaChi())) {
            if (val != null && val.matches("(?i).*<\\s*/?\\s*(script|iframe|img|svg|object|embed).*")) {
                throw new ApiContractException(HttpStatus.BAD_REQUEST, "1004",
                        "Nội dung chứa mã HTML/Script không hợp lệ");
            }
        }

        TaiKhoan taiKhoan = getCurrentAccount();
        Nguoi nguoi = taiKhoan.getNguoi();
        // 1. MapStruct tự động cập nhật mọi trường khác null cho 'nguoi'
        userMapper.updateNguoi(nguoi, request);
        if (request.getHoTen() != null)
            nguoi.setHoTen(request.getHoTen().trim());
        if (request.getGioiTinh() != null)
            nguoi.setGioiTinh(normalizeGender(request.getGioiTinh()));
        else if (request.getPresentFields().contains("gioiTinh"))
            nguoi.setGioiTinh(null);
        if (request.getNgaySinh() != null)
            nguoi.setNgaySinh(request.getNgaySinh());
        else if (request.getNamSinh() != null)
            nguoi.setNgaySinh(LocalDate.of(request.getNamSinh(), 1, 1));
        nguoiRepository.save(nguoi);

        // 2. MapStruct tự động cập nhật mọi trường khác null cho 'benhnhan'
        BenhNhan benhNhan = benhNhanRepository.findById(nguoi.getMaNguoi())
                .orElseGet(() -> BenhNhan.builder().nguoi(nguoi).build());
        userMapper.updateBenhNhan(benhNhan, request);
        if (request.getNgheNghiep() != null)
            benhNhan.setNgheNghiep((normalizeCareer(request.getNgheNghiep())));
        else if (request.getPresentFields().contains("ngheNghiep"))
            benhNhan.setNgheNghiep(null);
        if (request.getPresentFields().contains("danToc") && request.getDanToc() == null)
            benhNhan.setDanToc(null);
        if (request.getPresentFields().contains("bhytSoThe") && request.getBhytSoThe() == null)
            benhNhan.setBhytSoThe(null);
        if (request.getPresentFields().contains("bhytGiaTriDen") && request.getBhytGiaTriDen() == null)
            benhNhan.setBhytGiaTriDen(null);
        if (request.getPresentFields().contains("nguoiNhaHoTenDiaChi") && request.getNguoiNhaHoTenDiaChi() == null)
            benhNhan.setNguoiNhaHoTenDiaChi(null);
        if (request.getPresentFields().contains("nguoiNhaSoDienThoai") && request.getNguoiNhaSoDienThoai() == null)
            benhNhan.setNguoiNhaSoDienThoai(null);

        benhNhanRepository.save(benhNhan);
        return userMapper.toProfileResponse(taiKhoan, benhNhan);
    }

    private String normalizeCareer(String c) {
        return switch (c.trim().toLowerCase()) {
            case "cong nhan", "công nhân" -> "Cong nhan";
            case "nong dan", "nông dân" -> "Nong dan";
            case "hoc sinh - sinh vien", "học sinh - sinh viên", "sinh viên", "học sinh" -> "Hoc sinh - Sinh vien";
            case "can bo cong chuc", "cán bộ công chức" -> "Can bo cong chuc";
            default -> "Khac";
        };
    }

    private String normalizeGender(String g) {
        if ("male".equalsIgnoreCase(g) || "Nam".equalsIgnoreCase(g))
            return "Nam";
        if ("female".equalsIgnoreCase(g) || "Nu".equalsIgnoreCase(g) || "Nữ".equalsIgnoreCase(g))
            return "Nu";
        throw new RuntimeException("Giới tính chỉ chấp nhận 'male'/'female' hoặc 'Nam'/'Nu'");
    }

    private TaiKhoan getCurrentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new ApiContractException(HttpStatus.UNAUTHORIZED, "9998",
                        "Tài khoản không tồn tại hoặc token không hợp lệ"));
        if (!Boolean.TRUE.equals(taiKhoan.getTrangThaiHoatDong())) {
            throw new ApiContractException(HttpStatus.FORBIDDEN, "1009", "Tài khoản hiện đang bị khóa");
        }
        return taiKhoan;
    }

    private ProfileResponse mapToProfileResponse(TaiKhoan taiKhoan) {
        BenhNhan benhNhan = (taiKhoan.getNguoi() != null)
                ? benhNhanRepository.findById(taiKhoan.getNguoi().getMaNguoi()).orElse(null)
                : null;
        return userMapper.toProfileResponse(taiKhoan, benhNhan);
    }
}
