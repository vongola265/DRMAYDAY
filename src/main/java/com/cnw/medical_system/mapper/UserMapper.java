package com.cnw.medical_system.mapper;

import org.springframework.stereotype.Component;

import com.cnw.medical_system.dto.request.UpdateProfileRequest;
import com.cnw.medical_system.dto.response.ProfileResponse;
import com.cnw.medical_system.dto.response.UserAdminResponse;
import com.cnw.medical_system.entity.BenhNhan;
import com.cnw.medical_system.entity.Nguoi;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.entity.VaiTro;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    // MapStruct tự động copy mọi trường khác null từ request ->
    @Mapping(target = "gioiTinh", ignore = true) // tự chuẩn hóa Nam/nữ
    @Mapping(target = "ngaySinh", ignore = true) // tự xử lý ngaySinh/namSinh
    void updateNguoi(@MappingTarget Nguoi nguoi, UpdateProfileRequest request);

    @Mapping(target = "ngheNghiep", ignore = true) // tự chuẩn hóa 5 nghề DB
    void updateBenhNhan(@MappingTarget BenhNhan benhNhan, UpdateProfileRequest request);

    default ProfileResponse toProfileResponse(TaiKhoan taiKhoan, BenhNhan benhNhan) {
        Nguoi nguoi = taiKhoan.getNguoi();
        return ProfileResponse.builder().maTaiKhoan(taiKhoan.getMaTaiKhoan()).tenDangNhap(taiKhoan.getTenDangNhap())
                .hoTen(nguoi != null ? nguoi.getHoTen() : null)
                .soDienThoai(nguoi != null ? nguoi.getSoDienThoai() : null)
                .gioiTinh(nguoi != null ? nguoi.getGioiTinh() : null)
                .ngaySinh(nguoi != null ? nguoi.getNgaySinh() : null)
                .namSinh((nguoi != null && nguoi.getNgaySinh() != null) ? nguoi.getNgaySinh().getYear() : null)
                .diaChiSoNhaThonPho(nguoi != null ? nguoi.getDiaChiSoNhaThonPho() : null)
                .diaChiXaPhuong(nguoi != null ? nguoi.getDiaChiXaPhuong() : null)
                .diaChiTinhTp(nguoi != null ? nguoi.getDiaChiTinhTp() : null)
                .maVaiTro(taiKhoan.getVaiTro() != null ? taiKhoan.getVaiTro().getMaVaiTro() : null)
                .tenVaiTro(taiKhoan.getVaiTro() != null ? taiKhoan.getVaiTro().getTenVaiTro() : null)
                .trangThaiHoatDong(taiKhoan.getTrangThaiHoatDong()).thoiDiemTao(taiKhoan.getThoiDiemTao())
                .maHis(benhNhan != null ? benhNhan.getMaHis() : null)
                .ngheNghiep(benhNhan != null ? benhNhan.getNgheNghiep() : null)
                .danToc(benhNhan != null ? benhNhan.getDanToc() : null)
                .bhytSoThe(benhNhan != null ? benhNhan.getBhytSoThe() : null)
                .bhytGiaTriDen(benhNhan != null ? benhNhan.getBhytGiaTriDen() : null)
                .nguoiNhaHoTenDiaChi(benhNhan != null ? benhNhan.getNguoiNhaHoTenDiaChi() : null)
                .nguoiNhaSoDienThoai(benhNhan != null ? benhNhan.getNguoiNhaSoDienThoai() : null).build();
    }

    default UserAdminResponse toUserAdminResponse(TaiKhoan taiKhoan) {
        if (taiKhoan == null) return null;
        Nguoi nguoi = taiKhoan.getNguoi();
        VaiTro vaiTro = taiKhoan.getVaiTro();
        return UserAdminResponse.builder()
                .userId(taiKhoan.getMaTaiKhoan())
                .username(taiKhoan.getTenDangNhap())
                .fullName(nguoi != null ? nguoi.getHoTen() : null)
                .phone(nguoi != null ? nguoi.getSoDienThoai() : null)
                .gender(nguoi != null ? nguoi.getGioiTinh() : null)
                .roleId(vaiTro != null ? vaiTro.getMaVaiTro() : null)
                .roleName(vaiTro != null ? vaiTro.getTenVaiTro() : null)
                .isActive(taiKhoan.getTrangThaiHoatDong() != null && taiKhoan.getTrangThaiHoatDong() ? 1 : 0)
                .createdAt(taiKhoan.getThoiDiemTao())
                .build();
    }
}
