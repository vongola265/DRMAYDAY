package com.cnw.medical_system.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VaiTroQuyenHanId implements Serializable {

    @Column(name = "ma_vai_tro")
    Long maVaiTro;

    @Column(name = "ma_quyen_han")
    Long maQuyenHan;
}