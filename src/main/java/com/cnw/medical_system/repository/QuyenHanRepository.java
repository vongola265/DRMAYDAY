package com.cnw.medical_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cnw.medical_system.entity.QuyenHan;

@Repository

public interface QuyenHanRepository extends JpaRepository<QuyenHan, Long> {
    Optional<QuyenHan> findByTenQuyenHan(String tenQuyenHan);

    // Kiểm tra trùng tên quyền khi tạo mới quyền
    boolean existsByTenQuyenHanIgnoreCase(String tenQuyenHan);

    // kiểm tra trùng tên quyền với bản ghi khác khi chỉnh sửa

    boolean existsByTenQuyenHanIgnoreCaseAndMaQuyenHanNot(String tenQuyenHan, Long maQuyenHan);

}
