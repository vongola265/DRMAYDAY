package com.cnw.medical_system.service;

import com.cnw.medical_system.entity.BenhNhan;
import com.cnw.medical_system.entity.Nguoi;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.entity.VaiTro;
import com.cnw.medical_system.repository.BenhNhanRepository;
import com.cnw.medical_system.repository.NguoiRepository;
import com.cnw.medical_system.repository.TaiKhoanRepository;
import com.cnw.medical_system.repository.VaiTroRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MaydayAuthService {
    public static final String SIGNER_KEY = "MedicalSystemSecretKeyForJwtTokenSigningMustBe256BitsLong!";

    private final TaiKhoanRepository accounts;
    private final NguoiRepository people;
    private final VaiTroRepository roles;
    private final BenhNhanRepository patients;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    @Transactional
    public Long signup(String fullName, String phone, String password) {
        if (people.existsBySoDienThoai(phone) || accounts.existsByTenDangNhap(phone)) {
            throw new MaydayApiException("1009", "Số điện thoại đã được đăng ký");
        }
        VaiTro defaultRole = roles.findByTenVaiTro("benh_nhan")
                .or(() -> roles.findByTenVaiTro("patient"))
                .or(() -> roles.findAll().stream().findFirst())
                .orElseThrow(() -> new MaydayApiException("1010", "Chưa có vai trò trong cơ sở dữ liệu"));

        Nguoi person = people.save(Nguoi.builder().hoTen(fullName).soDienThoai(phone).build());
        patients.save(BenhNhan.builder().maBenhNhan(person.getMaNguoi()).build());
        accounts.save(TaiKhoan.builder()
                .nguoi(person)
                .vaiTro(defaultRole)
                .tenDangNhap(phone)
                .matKhauMaHoa(passwordEncoder.encode(password))
                .trangThaiHoatDong(true)
                .build());
        return person.getMaNguoi();
    }

    @Transactional(readOnly = true)
    public String login(String phone, String password) {
        TaiKhoan account = accounts.findByNguoi_SoDienThoai(phone)
                .orElseThrow(() -> new MaydayApiException("9998", "Số điện thoại hoặc mật khẩu không chính xác"));
        if (!passwordEncoder.matches(password, account.getMatKhauMaHoa()) ||
                !Boolean.TRUE.equals(account.getTrangThaiHoatDong())) {
            throw new MaydayApiException("9998", "Số điện thoại hoặc mật khẩu không chính xác");
        }
        SecretKey key = Keys.hmacShaKeyFor(SIGNER_KEY.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(account.getTenDangNhap())
                .claim("userId", account.getMaTaiKhoan())
                .claim("personId", account.getNguoi().getMaNguoi())
                .claim("role", account.getVaiTro().getTenVaiTro())
                .issuedAt(new Date(now))
                .expiration(new Date(now + 24L * 60 * 60 * 1000))
                .signWith(key)
                .compact();
    }

    public TaiKhoan requireAccount(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MaydayApiException("9998", "Thiếu hoặc sai token xác thực");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty() || revokedTokens.contains(token)) {
            throw new MaydayApiException("9998", "Token không hợp lệ hoặc đã đăng xuất");
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(SIGNER_KEY.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return accounts.findByTenDangNhap(claims.getSubject())
                    .filter(account -> Boolean.TRUE.equals(account.getTrangThaiHoatDong()))
                    .orElseThrow(() -> new MaydayApiException("9998", "Tài khoản không còn hoạt động"));
        } catch (MaydayApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MaydayApiException("9998", "Token không hợp lệ hoặc đã hết hạn");
        }
    }

    public void logout(String authorization) {
        TaiKhoan account = requireAccount(authorization);
        revokedTokens.add(authorization.substring("Bearer ".length()).trim());
    }

    public boolean isStaff(TaiKhoan account) {
        String role = account.getVaiTro().getTenVaiTro().toLowerCase().replace('-', '_').replace(' ', '_');
        return role.contains("admin") || role.contains("director") || role.contains("bac_si") ||
                role.contains("doctor") || role.contains("dieu_duong") || role.contains("nurse") ||
                account.getVaiTro().getMaVaiTro() == 1L;
    }

    public boolean isAdmin(TaiKhoan account) {
        return account.getVaiTro().getMaVaiTro() == 1L ||
                account.getVaiTro().getTenVaiTro().toLowerCase().contains("admin");
    }
}
