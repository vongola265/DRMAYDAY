package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "ketquasangloc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KetQuaSangLoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_ket_qua_sang_loc")
    Long maKetQuaSangLoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_benh_nhan")
    BenhNhan benhNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nguoi_thuc_hien", nullable = false)
    Nguoi nguoiThucHien;

    @Column(name = "du_lieu_tra_loi_json", nullable = false, columnDefinition = "TEXT")
    String duLieuTraLoiJson;

    @Column(name = "loai_benh_an_de_xuat", nullable = false, length = 30)
    String loaiBenhAnDeXuat;

    @Column(name = "ma_bieu_mau_de_xuat", nullable = false, length = 50)
    String maBieuMauDeXuat;

    @Builder.Default
    @Column(name = "muc_do_uu_tien", nullable = false, length = 20)
    String mucDoUuTien = "normal";

    @Column(name = "ghi_chu_phan_loai", columnDefinition = "TEXT")
    String ghiChuPhanLoai;

    @Column(name = "thoi_diem_tao", nullable = false, updatable = false)
    LocalDateTime thoiDiemTao;

    @PrePersist
    protected void onCreate() {
        this.thoiDiemTao = LocalDateTime.now();
        if (this.mucDoUuTien == null) {
            this.mucDoUuTien = "normal";
        }
    }
}
