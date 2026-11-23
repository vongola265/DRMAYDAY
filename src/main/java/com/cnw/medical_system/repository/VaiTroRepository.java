package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VaiTroRepository extends JpaRepository<VaiTro, Long> {
    // Tìm vai trò theo tên
    Optional<VaiTro> findByTenVaiTro(String tenVaiTro);

    // Kiểm tra tên vai trò đã tồn tại chưa
    boolean existsByTenVaiTroIgnoreCase(String tenVaiTro);
}
