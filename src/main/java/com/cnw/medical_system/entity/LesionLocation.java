package com.cnw.medical_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "vitritonthuong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LesionLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_vi_tri_ton_thuong")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_dot_ton_thuong_da", nullable = false)
    LesionEpisode episode;

    @Column(name = "vi_tri_co_the", length = 255)
    String bodyPart;

    @Column(name = "thoi_diem_xuat_hien")
    OffsetDateTime occurredAt;

    @Column(name = "ten_thuoc_lien_quan", length = 255)
    String relatedMedication;

    @Column(name = "thoi_gian_dung_thuoc_truoc_khi_xuat_hien", length = 100)
    String medicationDuration;

    @Column(name = "hoan_canh_xuat_hien", length = 255)
    String circumstance;

    @Column(name = "hoan_canh_xuat_hien_khac", columnDefinition = "TEXT")
    String otherCircumstance;

    @Column(name = "don_vi_thoi_gian", length = 50)
    String timeUnit;

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
