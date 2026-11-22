package com.cnw.medical_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Entity
@Table(name = "dottonthuongda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LesionEpisode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dot_ton_thuong_da")
    Long id;

    @Column(name = "ma_benh_nhan", nullable = false)
    Long patientId;

    @Column(name = "ma_ho_so_man")
    Long medicalRecordId;

    @Column(name = "ten_dot_ton_thuong_da", length = 255)
    String name;

    @Column(name = "thoi_diem_xuat_hien")
    OffsetDateTime occurredAt;

    @Column(name = "hoan_canh_xuat_hien", length = 255)
    String circumstance;

    @Column(name = "hoan_canh_xuat_hien_khac", columnDefinition = "TEXT")
    String otherCircumstance;

    @Column(name = "ten_thuoc_dung_gan_thoi_diem_xuat_hien", length = 255)
    String medication;

    @Column(name = "thoi_gian_dung_thuoc_truoc_khi_xuat_hien", length = 100)
    String medicationDuration;

    @Column(name = "don_vi_thoi_gian", length = 50)
    String timeUnit;

    @Column(name = "chi_tiet_dot_ton_thuong", columnDefinition = "TEXT")
    String details;

    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "thoi_diem_cap_nhat", nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
