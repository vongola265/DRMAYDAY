package com.cnw.medical_system.service;

import com.cnw.medical_system.entity.HoSoBenhAn;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.repository.HoSoBenhAnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaydayRecordService {
    private final HoSoBenhAnRepository records;
    private final JdbcTemplate jdbc;
    private final MaydayAuthService auth;

    @Transactional
    public Map<String, Object> create(TaiKhoan actor, Map<String, Object> body) {
        Object templateId = body.get("template_id");
        if (templateId == null) throw new MaydayApiException("1002", "Thiếu template_id");
        if (!isInteger(templateId)) throw new MaydayApiException("1003", "template_id sai kiểu dữ liệu");
        if (asLong(templateId) <= 0) throw new MaydayApiException("1004", "template_id không hợp lệ");

        long patientId = actor.getNguoi().getMaNguoi();
        if (body.containsKey("patient_id")) {
            Object requestedPatient = body.get("patient_id");
            if (!auth.isStaff(actor)) throw new MaydayApiException("9998", "Không có quyền tạo hồ sơ cho bệnh nhân khác");
            if (!isInteger(requestedPatient)) throw new MaydayApiException("1003", "patient_id sai kiểu dữ liệu");
            patientId = asLong(requestedPatient);
            if (patientId <= 0) throw new MaydayApiException("1004", "patient_id không hợp lệ");
        }

        HoSoBenhAn record = records.saveAndFlush(HoSoBenhAn.builder()
                .patientId(patientId)
                .type("MAN")
                .status("draft")
                .build());
        // The database uses a deferred subtype constraint for chronic records.
        jdbc.update("INSERT INTO benhanmaydayman (ma_ho_so) VALUES (?)", record.getId());
        return recordData(record);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id, TaiKhoan actor) {
        HoSoBenhAn record = records.findById(id)
                .orElseThrow(() -> new MaydayApiException("9992", "Không tìm thấy hồ sơ bệnh án"));
        verifyAccess(record, actor);
        return recordData(record);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String status, TaiKhoan actor) {
        HoSoBenhAn record = records.findById(id)
                .orElseThrow(() -> new MaydayApiException("9992", "Không tìm thấy hồ sơ bệnh án"));
        verifyAccess(record, actor);
        if (status == null || status.isBlank()) throw new MaydayApiException("1002", "Thiếu status");
        if (!status.equals("draft") && !status.equals("pending") && !status.equals("in_progress") &&
                !status.equals("completed") && !status.equals("cancelled")) {
            throw new MaydayApiException("1004", "status không hợp lệ");
        }
        record.setStatus(status);
        return recordData(records.save(record));
    }

    @Transactional
    public void delete(Long id, TaiKhoan actor) {
        HoSoBenhAn record = records.findById(id)
                .orElseThrow(() -> new MaydayApiException("9992", "Không tìm thấy hồ sơ bệnh án"));
        if (!auth.isAdmin(actor)) verifyAccess(record, actor);

        jdbc.update("DELETE FROM mayday_answer_images WHERE answer_id IN (SELECT answer_id FROM mayday_medical_record_answers WHERE medical_record_id = ?)", id);
        jdbc.update("DELETE FROM mayday_medical_record_answers WHERE medical_record_id = ?", id);
        jdbc.update("DELETE FROM nhatkyuashangngay WHERE ma_ho_so_man = ?", id);
        jdbc.update("DELETE FROM anhchuptonthuong WHERE ma_dot_ton_thuong_da IN (SELECT ma_dot_ton_thuong_da FROM dottonthuongda WHERE ma_ho_so_man = ?)", id);
        jdbc.update("DELETE FROM anhchuptonthuong WHERE ma_vi_tri_ton_thuong IN (SELECT v.ma_vi_tri_ton_thuong FROM vitritonthuong v JOIN dottonthuongda e ON e.ma_dot_ton_thuong_da = v.ma_dot_ton_thuong_da WHERE e.ma_ho_so_man = ?)", id);
        jdbc.update("DELETE FROM vitritonthuong WHERE ma_dot_ton_thuong_da IN (SELECT ma_dot_ton_thuong_da FROM dottonthuongda WHERE ma_ho_so_man = ?)", id);
        jdbc.update("DELETE FROM dottonthuongda WHERE ma_ho_so_man = ?", id);
        jdbc.update("DELETE FROM tepdinhkem WHERE ma_ho_so = ?", id);
        jdbc.update("DELETE FROM benhanmaydayman WHERE ma_ho_so = ?", id);
        records.delete(record);
    }

    private void verifyAccess(HoSoBenhAn record, TaiKhoan actor) {
        if (!auth.isStaff(actor) && !record.getPatientId().equals(actor.getNguoi().getMaNguoi())) {
            throw new MaydayApiException("9998", "Không có quyền truy cập hồ sơ");
        }
    }

    private Map<String, Object> recordData(HoSoBenhAn record) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("medical_record_id", record.getId());
        data.put("patient_id", record.getPatientId());
        data.put("template_id", 1);
        data.put("status", record.getStatus());
        return data;
    }

    private boolean isInteger(Object value) {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) return true;
        if (value instanceof String text) return text.matches("[0-9]+");
        return false;
    }

    private long asLong(Object value) {
        try { return Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException exception) { return Long.MAX_VALUE; }
    }
}
