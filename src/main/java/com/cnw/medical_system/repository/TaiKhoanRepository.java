package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long> {
    // Tìm tài khoản theo tên đăng nhập (Dùng cho đăng nhập)
    Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);

    Optional<TaiKhoan> findByNguoi_SoDienThoai(String soDienThoai);

    // Kiểm tra xem tên đăng nhập đã tồn tại chưa ( dùng cho đăng ký)
    boolean existsByTenDangNhap(String tenDangNhap);
}
