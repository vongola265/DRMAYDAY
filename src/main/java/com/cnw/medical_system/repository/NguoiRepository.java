package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.Nguoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NguoiRepository extends JpaRepository<Nguoi, Long> {
    // Kiểm tra số điện thoại đã đăng ký chưa
    boolean existsBySoDienThoai(String soDienThoai);

}
