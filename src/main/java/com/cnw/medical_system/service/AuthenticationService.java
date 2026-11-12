package com.cnw.medical_system.service;

import com.cnw.medical_system.dto.request.LoginRequest;
import com.cnw.medical_system.dto.response.LoginResponse;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.entity.VaiTro;
import com.cnw.medical_system.repository.NguoiRepository;
import com.cnw.medical_system.repository.TaiKhoanRepository;
import com.cnw.medical_system.repository.VaiTroRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import com.cnw.medical_system.entity.VaiTro;
import com.cnw.medical_system.entity.Nguoi;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    TaiKhoanRepository taiKhoanRepository;
    NguoiRepository nguoiRepository;
    VaiTroRepository vaiTroRepository;
    // Khóa bí mật để ký token ( chuỗi phải dài tối thiểu 32 ký tự để đảm bảo chuẩn
    // SHA-256)
    static String SIGNER_KEY = "MedicalSystemSecretKeyForJwtTokenSigningMustBe256BitsLong!";
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
    public String register(RegisterRequest request) {
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
