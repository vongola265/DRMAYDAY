package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phongkham")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhongKham {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ma_phong_kham")
    private Long id;
    @Column(name = "ma_chi_nhanh", nullable = false)
    private Long branchId;
    @Column(name = "ten_phong_kham", nullable = false, length = 150)
    private String name;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void onCreate() { LocalDateTime now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void onUpdate() { updatedAt=LocalDateTime.now(); }
}
