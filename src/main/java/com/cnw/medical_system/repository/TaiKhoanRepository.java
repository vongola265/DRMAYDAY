package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.TaiKhoan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long>, JpaSpecificationExecutor<TaiKhoan> {
    // Tìm tài khoản theo tên đăng nhập (Dùng cho đăng nhập)
    Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);

    // Kiểm tra xem tên đăng nhập đã tồn tại chưa ( dùng cho đăng ký)
    boolean existsByTenDangNhap(String tenDangNhap);

    // tìm tài khoản theo số điện thoại của người sở hữu
    Optional<TaiKhoan> findByNguoi_SoDienThoai(String soDienThoai);

    // Đếm số lượng tài khoản theo tên vai trò
    long countByVaiTro_TenVaiTroIgnoreCase(String tenVaiTro);

}
