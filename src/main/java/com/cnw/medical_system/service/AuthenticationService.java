package com.cnw.medical_system.service;

import com.cnw.medical_system.dto.request.LoginRequest;
import com.cnw.medical_system.dto.response.LoginResponse;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.entity.VaiTro;
import com.cnw.medical_system.repository.NguoiRepository;
import com.cnw.medical_system.repository.TaiKhoanRepository;
import com.cnw.medical_system.repository.VaiTroRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import javax.management.RuntimeErrorException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.transaction.annotation.Transactional;
import com.cnw.medical_system.dto.request.RegisterRequest;
import com.cnw.medical_system.dto.request.ResetPasswordRequest;
import com.cnw.medical_system.entity.VaiTro;
import com.cnw.medical_system.entity.Nguoi;
import com.cnw.medical_system.dto.response.UserResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import com.cnw.medical_system.dto.request.ChangePasswordRequest;
import com.cnw.medical_system.dto.request.CreateStaffRequest;
import com.cnw.medical_system.dto.request.ForgotPasswordRequest;
import com.cnw.medical_system.service.TokenBlacklistService;
import com.cnw.medical_system.service.OtpService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    TaiKhoanRepository taiKhoanRepository;
    NguoiRepository nguoiRepository;
    VaiTroRepository vaiTroRepository;
    TokenBlacklistService tokenBlacklistService;
    OtpService otpService;
    // Khóa bí mật để ký token ( chuỗi phải dài tối thiểu 32 ký tự để đảm bảo chuẩn
    // SHA-256)
    public static final String SIGNER_KEY = "MedicalSystemSecretKeyForJwtTokenSigningMustBe256BitsLong!";
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Quên mật khẩu: kiểm tra số điện thoại và cấp mã OTP
    @Transactional(readOnly = true)
    public String forgotPassword(ForgotPasswordRequest request) {
        // tìm tài khoản theo số điện thoại
        TaiKhoan taiKhoan = taiKhoanRepository.findByNguoi_SoDienThoai(request.getSoDienThoai())
                .orElseThrow(() -> new RuntimeException("Số điện thoại chưa được đăng ký trên hệ thống"));
        if (!Boolean.TRUE.equals(taiKhoan.getTrangThaiHoatDong())) {
            throw new RuntimeException("Tài khoản của bạn hiện đang bị khóa ");
        }
        // Sinh mã OTP 6 chữ số
        String otpCode = otpService.generateOTP(request.getSoDienThoai());
        // Giả lập gửi SMS qua Console
        System.out.println("========================================================================");
        System.out.println("[SMS SERVICE] Gửi tới SĐT " + request.getSoDienThoai() + " (Tài khoản: "
                + taiKhoan.getTenDangNhap() + "):");
        System.out.println(">>> MÃ OTP KHÔI PHỤC MẬT KHẨU CỦA BẠN LÀ: " + otpCode + " (Hiệu lực: 5 phút)");
        System.out.println("========================================================================");
        return "Đã gửi hướng dẫn khôi phục (Mã test: " + otpCode + ")";
    }

    // Đặt lại mật khẩu mới khi có mã OTP
    @Transactional
    public String resetPassWord(ResetPasswordRequest request) {
        // xác thực OTP
        boolean isValid = otpService.validateOtp(request.getSoDienThoai(), request.getMaOtp());
        if (!isValid)
            throw new RuntimeException("Mã OTP không chính xác hoặc đã hết hạn");
        TaiKhoan taiKhoan = taiKhoanRepository.findByNguoi_SoDienThoai(request.getSoDienThoai())
                .orElseThrow(() -> new RuntimeException("Số điện thoại chưa được đăng ký trên hệ thống"));
        // mã hóa BCrypt mật khẩu mới và lưu lại
        taiKhoan.setMatKhauMaHoa(passwordEncoder.encode(request.getMatKhauMoi()));
        taiKhoanRepository.save(taiKhoan);
        return "Đặt lại mật khẩu thành công ";
    }

    // Phương thức logout
    public String logout(HttpServletRequest request) {
        // rút trực tiếp từ mẩu giấy đã dán từ filter ra
        Claims claims = (Claims) request.getAttribute("jwt_claims");
        String token = (String) request.getAttribute("jwt_token");
        if (claims == null || token == null) {
            throw new RuntimeException("Không tìm thấy thông tin phiên đăng nhập để đăng xuất");
        }
        // Lấy ngày hết hạn đã được giải mã sẵn từ trước
        Date expiryTime = claims.getExpiration();
        tokenBlacklistService.blacklistToken(token, expiryTime);
        return "Đăng xuất thành công!";
    }

    @Transactional
    public String createStaff(CreateStaffRequest request) {
        // 1.Lấy thông tin người đang thực hiện gọi API từ Token
        var context = SecurityContextHolder.getContext();
        String currentUsername = context.getAuthentication().getName();
        TaiKhoan currentAccount = taiKhoanRepository.findByTenDangNhap(currentUsername)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại "));
        // 2.Bảo vệ phân quyền: chỉ có vai trò 'admin' mới được phép tạo nhan viên
        if (currentAccount.getVaiTro() == null
                || !"admin".equalsIgnoreCase(currentAccount.getVaiTro().getTenVaiTro())) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này. Chỉ Admin mới được tạo nhân viên!");
        }
        // Tên đăng nhập nếu không truyền thì mặc định lấy số điện thoại
        String username = (request.getTenDangNhap() != null && !request.getTenDangNhap().isBlank())
                ? request.getTenDangNhap()
                : request.getSoDienThoai();

        // 3. Kiểm tra trùng tên đăng nhập và số điện thoại
        if (taiKhoanRepository.existsByTenDangNhap(username)) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trên hệ thống");
        }
        if (nguoiRepository.existsBySoDienThoai(request.getSoDienThoai())) {
            throw new RuntimeException("Số điện thoại này đã được sử dụng");
        }
        // 4. TÌm vai trò theo yêu cầu(roleID hoặc tenVaiTro)
        VaiTro vaiTro = null;
        if (request.getMaVaiTro() != null) {
            vaiTro = vaiTroRepository.findById(request.getMaVaiTro())
                    .orElseThrow(() -> new RuntimeException("Vai trò ID" + request.getMaVaiTro() + " không tồn tại"));
        } else if (request.getTenVaiTro() != null) {
            vaiTro = vaiTroRepository.findByTenVaiTro(request.getTenVaiTro())
                    .orElseThrow(() -> new RuntimeException("Vai trò '" + request.getTenVaiTro() + "' không tồn tại"));
        } else {
            // mặc định không truyền thì gán vai trò bác sĩ
            vaiTro = vaiTroRepository.findByTenVaiTro("bac_si")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò bác sĩ trong hệ thống"));
        }
        // Không cho tạo nhân viên với vai trò là bệnh nhân hoặc admin
        if ("benh_nhan".equalsIgnoreCase(vaiTro.getTenVaiTro()) || "admin".equalsIgnoreCase(vaiTro.getTenVaiTro())) {
            throw new RuntimeException("Chỉ được phép tạo tài khoản cho nhân viên y tế (Bác sĩ, Điều dưỡng)");
        }

        // 5. Lưu thông tin vào bảng 'nguoi'
        Nguoi nguoi = Nguoi.builder().hoTen(request.getHoTen()).soDienThoai(request.getSoDienThoai()).build();
        nguoi = nguoiRepository.save(nguoi);

        // 6. Lưu tài khoản vào bảng 'taikhoan' với mật khẩu đã mã hóa BCrypt
        TaiKhoan taiKhoan = TaiKhoan.builder().nguoi(nguoi).vaiTro(vaiTro).tenDangNhap(username)
                .matKhauMaHoa(passwordEncoder.encode(request.getMatKhau())).trangThaiHoatDong(true).build();

        taiKhoanRepository.save(taiKhoan);
        return "Tạo tài khoản nhân viên thành công với vai trò: " + nguoi.getHoTen() + " (" + vaiTro.getTenHienThi()
                + ")";
    }

    @Transactional
    public String changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {
        // Kiểm tra xác nhận mật khẩu mới và mật khẩu xác nhận giống nhau không
        if (!request.getMatKhauMoi().equals(request.getMatKhauXacNhan()))
            throw new RuntimeException("Mật khẩu xác nhận không khớp với mật khẩu mới");

        // 1. Tự động lấy tên tài khoản từ Token(SecurityContext)
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        // 2. Tìm tài khoản trong database
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản"));
        // 3. Kiểm tra mật khẩu cũ người dùng có khớp với csdl không
        if (!passwordEncoder.matches(request.getMatKhauCu(), taiKhoan.getMatKhauMaHoa())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác");

        }
        // 4. Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getMatKhauMoi(), taiKhoan.getMatKhauMaHoa())) {
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu cũ ");
        }
        // 5. Mã hóa BCrypt mật khẩu mới và lưu lại
        taiKhoan.setMatKhauMaHoa(passwordEncoder.encode(request.getMatKhauMoi()));
        taiKhoanRepository.save(taiKhoan);

        // Vô hiệu hóa token cũ
        Claims claims = (Claims) httpRequest.getAttribute("jwt_claims");
        String token = (String) httpRequest.getAttribute("jwt_token");
        if (claims != null && token != null)
            tokenBlacklistService.blacklistToken(token, claims.getExpiration());
        return "Đổi mật khẩu thành công!";

    }

    @Transactional(readOnly = true)
    public UserResponse getMyInfo() {
        // lấy tên đăng nhập từ SecurityContexxt ( do JwtAuthenticationFilter giải mã và
        // lưu vào)
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        // Tìm tài khoản trong database
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản"));
        return UserResponse.builder().maTaiKhoan(taiKhoan.getMaTaiKhoan()).tenDangNhap(taiKhoan.getTenDangNhap())
                .hoTen(taiKhoan.getNguoi() != null ? taiKhoan.getNguoi().getHoTen() : null)
                .soDienThoai(taiKhoan.getNguoi() != null ? taiKhoan.getNguoi().getSoDienThoai() : null)
                .vaiTro(taiKhoan.getVaiTro() != null ? taiKhoan.getVaiTro().getTenVaiTro() : null)
                .trangThaiHoatDong(taiKhoan.getTrangThaiHoatDong()).build();
    }

    @Transactional(readOnly = true)
    public LoginResponse authenticate(LoginRequest request) {
        // 1. tìm tài khoản theo tên đăng nhập
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(request.getTenDangNhap())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác"));
        // 2. kiểm tra mật khẩu (so khớp mật khẩu thô và mật khẩu mã hóa Bcrypt)
        boolean matches = passwordEncoder.matches(request.getMatKhau(), taiKhoan.getMatKhauMaHoa());
        if (!matches) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }
        // 3.Kiểm tra tài khoản có bị vô hiệu háo không
        if (!Boolean.TRUE.equals(taiKhoan.getTrangThaiHoatDong())) {
            throw new RuntimeException("Tài khoản của bạn hiện đang bị khóa");
        }
        // 4.Sinh JWT token
        String token = generateToken(taiKhoan);
        // 5. trả về kết quả
        return LoginResponse.builder().token(token).authenticated(true).tenDangNhap(taiKhoan.getTenDangNhap())
                .vaiTro(taiKhoan.getVaiTro() != null ? taiKhoan.getVaiTro().getTenVaiTro() : null).build();
    }

    @Transactional
    public String signup(RegisterRequest request) {
        // 1.Kiểm tra xem tên đăng nhập đã tồn tại chưa
        if (taiKhoanRepository.existsByTenDangNhap((request.getTenDangNhap()))) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trên hệ thống");
        }
        // 2.Kiểm tra xem số điện thoại đã được đăng ký chưa
        if (nguoiRepository.existsBySoDienThoai(request.getSoDienThoai())) {
            throw new RuntimeException("Số điện thoại này đã được sử dụng");
        }
        // 3. Lấy vai trò mặc định cho người đăng ký mới (vai trò 'benh_nhan' đã có sẵn
        // trong DB)
        VaiTro vaitTroMacDinh = vaiTroRepository.findByTenVaiTro("benh_nhan")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò mặc định trong hệ thống"));

        // 4. Tạo và lưu thông tin cá nhân vào bảng 'nguoi'
        Nguoi nguoi = Nguoi.builder().hoTen(request.getHoTen()).soDienThoai(request.getSoDienThoai()).build();
        nguoi = nguoiRepository.save(nguoi);

        // 5.Tạo và lưu tài khoản vào bảng 'taikhoan' với mật khẩu đã mã hóa BCrypt
        TaiKhoan taiKhoan = TaiKhoan.builder().nguoi(nguoi).vaiTro(vaitTroMacDinh).tenDangNhap(request.getTenDangNhap())
                .matKhauMaHoa(passwordEncoder.encode(request.getMatKhau())).trangThaiHoatDong(true).build();
        taiKhoanRepository.save(taiKhoan);
        return "Đăng ký tài khoản thành công!";
    }

    private String generateToken(TaiKhoan taiKhoan) {
        SecretKey key = Keys.hmacShaKeyFor(SIGNER_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject(taiKhoan.getTenDangNhap()).claim("userId", taiKhoan.getMaTaiKhoan())
                .claim("role", taiKhoan.getVaiTro() != null ? taiKhoan.getVaiTro().getTenVaiTro() : "")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(key).compact();
    }
}
