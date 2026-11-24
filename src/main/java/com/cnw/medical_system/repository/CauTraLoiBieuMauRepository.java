package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.CauTraLoiBieuMau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CauTraLoiBieuMauRepository extends JpaRepository<CauTraLoiBieuMau, Long> {
    List<CauTraLoiBieuMau> findByHoSoBenhAn_MaHoSoOrderByMaCauTraLoiAsc(Long maHoSo);

    Optional<CauTraLoiBieuMau> findByHoSoBenhAn_MaHoSoAndMaCauHoi(Long maHoSo, String maCauHoi);

    Optional<CauTraLoiBieuMau> findByMaCauTraLoiAndHoSoBenhAn_MaHoSo(Long maCauTraLoi, Long maHoSo);

    boolean existsByHoSoBenhAn_MaHoSoAndMaCauHoi(Long maHoSo, String maCauHoi);

    boolean existsByHoSoBenhAn_MaHoSoAndVaiTroNguoiNhapIn(Long maHoSo, List<String> vaiTroNguoiNhap);

    long countByHoSoBenhAn_MaHoSo(Long maHoSo);

    void deleteByHoSoBenhAn_MaHoSo(Long maHoSo);
}
