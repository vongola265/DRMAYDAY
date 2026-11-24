package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hosobenhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HoSoBenhAn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ho_so")
    Long maHoSo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_benh_nhan", nullable = false)
    BenhNhan benhNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_bac_si")
    Nguoi bacSi;

    @Column(name = "ma_phong_kham")
    Long maPhongKham;

    @Column(name = "loai_benh_an", nullable = false, length = 30)
    String loaiBenhAn;

    @Column(name = "ma_bieu_mau", length = 50)
    String maBieuMau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ket_qua_sang_loc")
    KetQuaSangLoc ketQuaSangLoc;

    @Builder.Default
    @Column(name = "trang_thai", nullable = false, length = 30)
    String trangThai = "pending";

    @Column(name = "ly_do_kham", columnDefinition = "TEXT")
    String lyDoKham;

    @Column(name = "chan_doan_so_bo", columnDefinition = "TEXT")
    String chanDoanSoBo;

    @Column(name = "chan_doan_xac_dinh", columnDefinition = "TEXT")
    String chanDoanXacDinh;

    @Column(name = "huong_xu_tri", columnDefinition = "TEXT")
    String huongXuTri;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    String ghiChu;

    @Column(name = "ngay_hen_tai_kham")
    LocalDate ngayHenTaiKham;

    @Column(name = "thoi_diem_tiep_nhan")
    LocalDateTime thoiDiemTiepNhan;

    @Column(name = "thoi_diem_hoan_thanh")
    LocalDateTime thoiDiemHoanThanh;

    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    LocalDateTime thoiDiemTao;

    @Column(name = "thoi_diem_cap_nhat")
    LocalDateTime thoiDiemCapNhat;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.thoiDiemTao = now;
        this.thoiDiemCapNhat = now;
        if (this.trangThai == null) {
            this.trangThai = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.thoiDiemCapNhat = LocalDateTime.now();
    }
}
