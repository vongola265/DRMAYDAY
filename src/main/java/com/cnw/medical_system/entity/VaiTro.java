package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vaitro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VaiTro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_vai_tro")
    Long maVaiTro;

    @Column(name = "ten_vai_tro", nullable = false, unique = true, length = 50)
    String tenVaiTro;

    @Column(name = "ten_hien_thi", nullable = false, length = 100)
    String tenHienThi;

    @Column(name = "mo_ta_vai_tro", columnDefinition = "TEXT")
    String moTaVaiTro;

    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    OffsetDateTime thoiDiemTao;

    @Column(name = "thoi_diem_cap_nhat", nullable = false)
    OffsetDateTime thoiDiemCapNhat;

    @PrePersist
    protected void onCreate() {
        this.thoiDiemTao = OffsetDateTime.now();
        this.thoiDiemCapNhat = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.thoiDiemCapNhat = OffsetDateTime.now();
    }
}
