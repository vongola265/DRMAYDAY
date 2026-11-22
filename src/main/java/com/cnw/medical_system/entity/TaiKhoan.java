package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.OffsetDateTime;

@Entity
@Table(name = "taikhoan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaiKhoan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tai_khoan")
    Long maTaiKhoan;
    // quan he 1 - 1 với bảng nguoi qua khoa nguoi ma_nguoi(unique)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nguoi", referencedColumnName = "ma_nguoi", nullable = false, unique = true)
    Nguoi nguoi;

    // Quan he N - 1 với bảng vaitro qua khoa nguoi ma_vai_tro
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_vai_tro", referencedColumnName = "ma_vai_tro", nullable = false)
    VaiTro vaiTro;

    @Column(name = "ten_dang_nhap", nullable = false, unique = true, length = 100)
    String tenDangNhap;

    @Column(name = "mat_khau_ma_hoa", nullable = false, columnDefinition = "TEXT")
    String matKhauMaHoa;

    @Builder.Default
    @Column(name = "trang_thai_hoat_dong", nullable = false)
    Boolean trangThaiHoatDong = true;

    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    OffsetDateTime thoiDiemTao;

    @Column(name = "thoi_diem_cap_nhat", nullable = false)
    OffsetDateTime thoiDiemCapNhat;

    @PrePersist
    protected void onCreate() {
        this.thoiDiemTao = OffsetDateTime.now();
        this.thoiDiemCapNhat = OffsetDateTime.now();
        if (this.trangThaiHoatDong == null) {
            this.trangThaiHoatDong = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.thoiDiemCapNhat = OffsetDateTime.now();
    }
}
