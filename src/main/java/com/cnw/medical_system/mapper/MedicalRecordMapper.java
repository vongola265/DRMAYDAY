package com.cnw.medical_system.mapper;

import com.cnw.medical_system.dto.response.ClassifyMedicalRecordResponse;
import com.cnw.medical_system.dto.response.MedicalRecordAnswerResponse;
import com.cnw.medical_system.dto.response.MedicalRecordHistoryResponse;
import com.cnw.medical_system.dto.response.MedicalRecordResponse;
import com.cnw.medical_system.entity.BenhNhan;
import com.cnw.medical_system.entity.CauTraLoiBieuMau;
import com.cnw.medical_system.entity.HoSoBenhAn;
import com.cnw.medical_system.entity.KetQuaSangLoc;
import com.cnw.medical_system.entity.Nguoi;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MedicalRecordMapper {

    default MedicalRecordResponse toMedicalRecordResponse(HoSoBenhAn hoSo, long answerCount) {
        if (hoSo == null) {
            return null;
        }
        BenhNhan benhNhan = hoSo.getBenhNhan();
        Nguoi nguoiBenh = (benhNhan != null) ? benhNhan.getNguoi() : null;
        Nguoi bacSi = hoSo.getBacSi();
        KetQuaSangLoc sangLoc = hoSo.getKetQuaSangLoc();

        return MedicalRecordResponse.builder()
                .medicalRecordId(hoSo.getMaHoSo())
                .patientId(benhNhan != null ? benhNhan.getMaBenhNhan() : null)
                .patientName(nguoiBenh != null ? nguoiBenh.getHoTen() : null)
                .patientPhone(nguoiBenh != null ? nguoiBenh.getSoDienThoai() : null)
                .doctorId(bacSi != null ? bacSi.getMaNguoi() : null)
                .doctorName(bacSi != null ? bacSi.getHoTen() : null)
                .roomId(hoSo.getMaPhongKham())
                .recordType(hoSo.getLoaiBenhAn())
                .templateCode(hoSo.getMaBieuMau())
                .classifyId(sangLoc != null ? sangLoc.getMaKetQuaSangLoc() : null)
                .status(hoSo.getTrangThai())
                .chiefComplaint(hoSo.getLyDoKham())
                .preliminaryDiagnosis(hoSo.getChanDoanSoBo())
                .finalDiagnosis(hoSo.getChanDoanXacDinh())
                .treatmentPlan(hoSo.getHuongXuTri())
                .notes(hoSo.getGhiChu())
                .followUpDate(hoSo.getNgayHenTaiKham())
                .answerCount(answerCount)
                .receivedAt(hoSo.getThoiDiemTiepNhan())
                .completedAt(hoSo.getThoiDiemHoanThanh())
                .createdAt(hoSo.getThoiDiemTao())
                .updatedAt(hoSo.getThoiDiemCapNhat())
                .build();
    }

    default MedicalRecordAnswerResponse toAnswerResponse(CauTraLoiBieuMau entity) {
        if (entity == null) {
            return null;
        }
        Nguoi nguoiNhap = entity.getNguoiNhap();
        return MedicalRecordAnswerResponse.builder()
                .answerId(entity.getMaCauTraLoi())
                .medicalRecordId(entity.getHoSoBenhAn() != null ? entity.getHoSoBenhAn().getMaHoSo() : null)
                .sectionId(entity.getMaPhanBieuMau())
                .questionId(entity.getMaCauHoi())
                .answerValue(entity.getGiaTriTraLoi())
                .dataType(entity.getKieuDuLieu())
                .note(entity.getGhiChu())
                .filledByRole(entity.getVaiTroNguoiNhap())
                .filledById(nguoiNhap != null ? nguoiNhap.getMaNguoi() : null)
                .filledByName(nguoiNhap != null ? nguoiNhap.getHoTen() : null)
                .createdAt(entity.getThoiDiemTao())
                .updatedAt(entity.getThoiDiemCapNhat())
                .build();
    }

    default ClassifyMedicalRecordResponse toClassifyResponse(KetQuaSangLoc entity, Long medicalRecordId) {
        if (entity == null) {
            return null;
        }
        return ClassifyMedicalRecordResponse.builder()
                .classifyId(entity.getMaKetQuaSangLoc())
                .patientId(entity.getBenhNhan() != null ? entity.getBenhNhan().getMaBenhNhan() : null)
                .performedById(entity.getNguoiThucHien() != null ? entity.getNguoiThucHien().getMaNguoi() : null)
                .classifiedType(entity.getLoaiBenhAnDeXuat())
                .recommendedTemplateCode(entity.getMaBieuMauDeXuat())
                .priorityLevel(entity.getMucDoUuTien())
                .classificationNote(entity.getGhiChuPhanLoai())
                .medicalRecordId(medicalRecordId)
                .createdAt(entity.getThoiDiemTao())
                .build();
    }

    default MedicalRecordHistoryResponse.VisitHistoryItem toVisitHistoryItem(
            HoSoBenhAn hoSo,
            List<MedicalRecordAnswerResponse> answers) {
        if (hoSo == null) {
            return null;
        }
        Nguoi bacSi = hoSo.getBacSi();
        return MedicalRecordHistoryResponse.VisitHistoryItem.builder()
                .medicalRecordId(hoSo.getMaHoSo())
                .recordType(hoSo.getLoaiBenhAn())
                .status(hoSo.getTrangThai())
                .doctorId(bacSi != null ? bacSi.getMaNguoi() : null)
                .doctorName(bacSi != null ? bacSi.getHoTen() : null)
                .roomId(hoSo.getMaPhongKham())
                .chiefComplaint(hoSo.getLyDoKham())
                .preliminaryDiagnosis(hoSo.getChanDoanSoBo())
                .finalDiagnosis(hoSo.getChanDoanXacDinh())
                .treatmentPlan(hoSo.getHuongXuTri())
                .followUpDate(hoSo.getNgayHenTaiKham())
                .visitDate(hoSo.getThoiDiemTao())
                .completedAt(hoSo.getThoiDiemHoanThanh())
                .answers(answers)
                .build();
    }
}
