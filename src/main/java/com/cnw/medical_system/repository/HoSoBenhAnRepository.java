package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.HoSoBenhAn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoSoBenhAnRepository extends JpaRepository<HoSoBenhAn, Long>, JpaSpecificationExecutor<HoSoBenhAn> {
    Page<HoSoBenhAn> findByBenhNhan_MaBenhNhan(Long maBenhNhan, Pageable pageable);

    List<HoSoBenhAn> findByBenhNhan_MaBenhNhanOrderByThoiDiemTaoDesc(Long maBenhNhan);

    long countByBenhNhan_MaBenhNhan(Long maBenhNhan);

    long countByBenhNhan_MaBenhNhanAndTrangThaiIgnoreCase(Long maBenhNhan, String trangThai);

    long countByBenhNhan_MaBenhNhanAndLoaiBenhAnIgnoreCase(Long maBenhNhan, String loaiBenhAn);
}
