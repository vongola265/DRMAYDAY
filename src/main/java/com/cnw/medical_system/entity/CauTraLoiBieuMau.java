package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cautraloibieumau",
        uniqueConstraints = @UniqueConstraint(name = "uk_ho_so_cau_hoi", columnNames = {"ma_ho_so", "ma_cau_hoi"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CauTraLoiBieuMau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_cau_tra_loi")
    Long maCauTraLoi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho_so", nullable = false)
    HoSoBenhAn hoSoBenhAn;

    @Column(name = "ma_phan_bieu_mau", length = 50)
    String maPhanBieuMau;

    @Column(name = "ma_cau_hoi", nullable = false, length = 100)
    String maCauHoi;

    @Column(name = "gia_tri_tra_loi", nullable = false, columnDefinition = "TEXT")
    String giaTriTraLoi;

    @Column(name = "kieu_du_lieu", length = 30)
    String kieuDuLieu;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    String ghiChu;

    @Column(name = "vai_tro_nguoi_nhap", nullable = false, length = 30)
    String vaiTroNguoiNhap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nguoi_nhap")
    Nguoi nguoiNhap;

    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    LocalDateTime thoiDiemTao;

    @Column(name = "thoi_diem_cap_nhat")
    LocalDateTime thoiDiemCapNhat;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.thoiDiemTao = now;
        this.thoiDiemCapNhat = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.thoiDiemCapNhat = LocalDateTime.now();
    }
}
