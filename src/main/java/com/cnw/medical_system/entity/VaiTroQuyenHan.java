package com.cnw.medical_system.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "vaitroquyenhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VaiTroQuyenHan {

    @EmbeddedId
    VaiTroQuyenHanId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maVaiTro")
    @JoinColumn(name = "ma_vai_tro", nullable = false)
    VaiTro vaiTro;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maQuyenHan")
    @JoinColumn(name = "ma_quyen_han", nullable = false)
    QuyenHan quyenHan;

    @Column(name = "thoi_diem_gan", nullable = false, updatable = false)
    LocalDateTime thoiDiemGan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nguoi_gan")
    Nguoi nguoiGan;

    @PrePersist
    protected void onCreate() {
        this.thoiDiemGan = LocalDateTime.now();
    }
}