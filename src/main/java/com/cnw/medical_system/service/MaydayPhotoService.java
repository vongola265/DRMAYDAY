package com.cnw.medical_system.service;

import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.dto.response.MaydayImageAttachmentResponse;
import com.cnw.medical_system.dto.response.MaydayLesionPhotoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaydayPhotoService {
    private final JdbcTemplate jdbc;
    private final MaydayAuthService auth;

    public List<MaydayLesionPhotoResponse> listForLocation(Long locationId) {
        return jdbc.query("""
                SELECT a.ma_anh AS photo_id, a.ma_tep_dinh_kem AS attachment_id,
                       COALESCE(t.duong_dan_url, t.duong_dan_luu_tru) AS file_url,
                       a.thoi_diem_chup AS captured_at, a.moc_tien_trien AS milestone
                FROM anhchuptonthuong a
                JOIN tepdinhkem t ON t.ma_tep_dinh_kem = a.ma_tep_dinh_kem
                WHERE a.ma_vi_tri_ton_thuong = ?
                ORDER BY a.ma_anh
                """, (rs, row) -> photoData(
                rs.getObject("photo_id"), rs.getObject("attachment_id"), rs.getString("file_url"),
                rs.getObject("captured_at"), rs.getString("milestone")), locationId);
    }

    public MaydayImageAttachmentResponse getAttachment(Long attachmentId, TaiKhoan actor) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT t.ma_tep_dinh_kem AS attachment_id,
                       COALESCE(t.duong_dan_url, t.duong_dan_luu_tru) AS file_url,
                       h.ma_benh_nhan AS patient_id
                FROM tepdinhkem t
                JOIN hosobenhan h ON h.ma_ho_so = t.ma_ho_so
                WHERE t.ma_tep_dinh_kem = ?
                """, (rs, row) -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("attachment_id", String.valueOf(rs.getObject("attachment_id")));
            data.put("file_url", rs.getString("file_url"));
            data.put("patient_id", rs.getLong("patient_id"));
            return data;
        }, attachmentId);
        if (rows.isEmpty()) throw new MaydayApiException("9992", "Không tìm thấy tệp đính kèm");
        Map<String, Object> attachment = rows.get(0);
        Long patientId = (Long) attachment.remove("patient_id");
        if (!auth.isStaff(actor) && !patientId.equals(actor.getNguoi().getMaNguoi())) {
            throw new MaydayApiException("9998", "Không có quyền xem tệp đính kèm");
        }
        return new MaydayImageAttachmentResponse(
                String.valueOf(attachment.get("attachment_id")),
                String.valueOf(attachment.get("file_url")));
    }

    public void deletePhotosForLocation(Long locationId) {
        jdbc.update("DELETE FROM anhchuptonthuong WHERE ma_vi_tri_ton_thuong = ?", locationId);
    }

    public void deletePhotosForEpisode(Long episodeId) {
        jdbc.update("DELETE FROM anhchuptonthuong WHERE ma_dot_ton_thuong_da = ?", episodeId);
        jdbc.update("DELETE FROM anhchuptonthuong WHERE ma_vi_tri_ton_thuong IN " +
                "(SELECT ma_vi_tri_ton_thuong FROM vitritonthuong WHERE ma_dot_ton_thuong_da = ?)", episodeId);
    }

    private MaydayLesionPhotoResponse photoData(Object photoId, Object attachmentId, String fileUrl,
                                                 Object capturedAt, String milestone) {
        return new MaydayLesionPhotoResponse(String.valueOf(photoId),
                new MaydayImageAttachmentResponse(String.valueOf(attachmentId), fileUrl), capturedAt, milestone);
    }
}
