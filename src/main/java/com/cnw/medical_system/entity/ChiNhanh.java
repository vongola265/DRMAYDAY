package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chinhanh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiNhanh {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ma_chi_nhanh")
    private Long id;
    @Column(name = "ten_chi_nhanh", nullable = false, length = 150)
    private String name;
    @Column(name = "dia_chi", nullable = false, columnDefinition = "TEXT")
    private String address;
    @Column(name = "so_dien_thoai", length = 20)
    private String phone;
    @Column(name = "trang_thai", nullable = false)
    private Boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (active == null) active = true;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
