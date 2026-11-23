package com.cnw.medical_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "benhnhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BenhNhan {
    @Id
    @Column(name = "ma_benh_nhan")
    Long maBenhNhan;

    // Quan hệ 1-1 chia sẻ chung khóa chính với bảng nguoi
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "ma_benh_nhan")
    Nguoi nguoi;

    @Column(name = "ma_his", unique = true, length = 50)
    String maHis;

    @Column(name = "nghe_nghiep", length = 40)
    String ngheNghiep;

    @Column(name = "dan_toc", length = 50)
    String danToc;

    @Column(name = "bhyt_so_the", length = 30)
    String bhytSoThe;

    @Column(name = "bhyt_gia_tri_den")
    LocalDate bhytGiaTriDen;

    @Column(name = "nguoi_nha_ho_ten_dia_chi", columnDefinition = "TEXT")
    String nguoiNhaHoTenDiaChi;

    @Column(name = "nguoi_nha_so_dien_thoai", length = 10)
    String nguoiNhaSoDienThoai;
}