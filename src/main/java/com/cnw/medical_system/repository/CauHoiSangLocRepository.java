package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.CauHoiSangLoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CauHoiSangLocRepository extends JpaRepository<CauHoiSangLoc, Long> {
    List<CauHoiSangLoc> findByTrangThaiHoatDongTrueOrderByThuTuHienThiAscMaCauHoiSangLocAsc();

    Optional<CauHoiSangLoc> findByMaCodeIgnoreCase(String maCode);
}
