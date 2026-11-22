package com.cnw.medical_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

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
    Long id;

    @Column(name = "ma_benh_nhan", nullable = false)
    Long patientId;

    @Column(name = "loai_benh_an", nullable = false, columnDefinition = "TEXT")
    String type;

    @Column(name = "trang_thai", nullable = false, columnDefinition = "TEXT")
    String status;
}
