package com.cnw.medical_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cnw.medical_system.entity.VaiTroQuyenHan;
import com.cnw.medical_system.entity.VaiTroQuyenHanId;

@Repository
public interface VaiTroQuyenHanRepository extends JpaRepository<VaiTroQuyenHan, VaiTroQuyenHanId> {

    // Lấy toàn bộ quyền đã gán cho một vai trò
    List<VaiTroQuyenHan> findById_MaVaiTroOrderById_MaQuyenHanAsc(Long maVaiTro);
}