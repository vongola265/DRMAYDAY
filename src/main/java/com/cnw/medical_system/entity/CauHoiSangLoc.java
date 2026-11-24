package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "cauhoisangloc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CauHoiSangLoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_cau_hoi_sang_loc")
    Long maCauHoiSangLoc;

    @Column(name = "ma_code", nullable = false, unique = true, length = 50)
    String maCode;

    @Column(name = "noi_dung_cau_hoi", nullable = false, columnDefinition = "TEXT")
    String noiDungCauHoi;

    @Column(name = "loai_cau_hoi", nullable = false, length = 30)
    String loaiCauHoi;

    @Column(name = "lua_chon_json", columnDefinition = "TEXT")
    String luaChonJson;

    @Builder.Default
    @Column(name = "bat_buoc", nullable = false)
    Boolean batBuoc = true;

    @Builder.Default
    @Column(name = "thu_tu_hien_thi", nullable = false)
    Integer thuTuHienThi = 1;

    @Builder.Default
    @Column(name = "trang_thai_hoat_dong", nullable = false)
    Boolean trangThaiHoatDong = true;

    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    LocalDateTime thoiDiemTao;

    @Column(name = "thoi_diem_cap_nhat")
    LocalDateTime thoiDiemCapNhat;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.thoiDiemTao = now;
        this.thoiDiemCapNhat = now;
        if (this.batBuoc == null) {
            this.batBuoc = true;
        }
        if (this.thuTuHienThi == null) {
            this.thuTuHienThi = 1;
        }
        if (this.trangThaiHoatDong == null) {
            this.trangThaiHoatDong = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.thoiDiemCapNhat = LocalDateTime.now();
    }
}
