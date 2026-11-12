package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "quyenhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuyenHan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_quyen_han")
    Long maQuyenHan;
    @Column(name = "ten_quyen_han", nullable = false, unique = true, length = 100)
    String tenQuyenHan;

    @Column(name = "nhom_quyen", length = 100)
    String nhomQuyen;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    String moTa;

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
