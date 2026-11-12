package com.cnw.medical_system.entity;

//Bảng nguoi lưu toàn bộ thông tin định danh cá nhân (Họ tên, ngày sinh, giới tính, SĐT, địa chỉ).
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Nguoi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nguoi")
    Long maNguoi;

    @Column(name = "ho_ten", nullable = false, length = 150)
    String hoTen;

    @Column(name = "ngay_sinh")
    LocalDate ngaySinh;

    @Column(name = "gioi_tinh", length = 3)
    String gioiTinh;

    @Column(name = "so_dien_thoai", nullable = false, length = 10)
    String soDienThoai;

    @Column(name = "dia_chi_tinh_tp", length = 100)
    String diaChiTinhTp;

    @Column(name = "dia_chi_xa_phuong", length = 100)
    String diaChiXaPhuong;

    @Column(name = "dia_chi_so_nha_thon_pho", length = 255)
    String diaChiSoNhaThonPho;

    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    LocalDateTime thoiDiemTao;

    @Column(name = "thoi_diem_cap_nhat")
    LocalDateTime thoiDiemCapNhat;

    @PrePersist
    protected void onCreate() {
        this.thoiDiemTao = LocalDateTime.now();
        this.thoiDiemCapNhat = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.thoiDiemCapNhat = LocalDateTime.now();
    }
}
