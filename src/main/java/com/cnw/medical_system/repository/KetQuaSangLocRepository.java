package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.KetQuaSangLoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KetQuaSangLocRepository extends JpaRepository<KetQuaSangLoc, Long> {
    List<KetQuaSangLoc> findByBenhNhan_MaBenhNhanOrderByThoiDiemTaoDesc(Long maBenhNhan);
}
