package com.cnw.medical_system.controller;

import com.cnw.medical_system.dto.request.ClassifyMedicalRecordRequest;
import com.cnw.medical_system.dto.request.DoctorCreateMedicalRecordRequest;
import com.cnw.medical_system.dto.request.EditMedicalRecordAnswersRequest;
import com.cnw.medical_system.dto.request.EditSingleAnswerRequest;
import com.cnw.medical_system.dto.request.SaveMedicalRecordAnswersRequest;
import com.cnw.medical_system.dto.request.SelfCreateMedicalRecordRequest;
import com.cnw.medical_system.dto.request.UpdateMedicalRecordRequest;
import com.cnw.medical_system.dto.request.UpdateMedicalRecordStatusRequest;
import com.cnw.medical_system.dto.response.ClassifyMedicalRecordResponse;
import com.cnw.medical_system.dto.response.ClassifyQuestionListResponse;
import com.cnw.medical_system.dto.response.MedicalRecordAnswerListResponse;
import com.cnw.medical_system.dto.response.MedicalRecordAnswerResponse;
import com.cnw.medical_system.dto.response.MedicalRecordHistoryResponse;
import com.cnw.medical_system.dto.response.MedicalRecordListResponse;
import com.cnw.medical_system.dto.response.MedicalRecordResponse;
import com.cnw.medical_system.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping({"/medical-records", "/medical_records"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordController {

    MedicalRecordService medicalRecordService;

    // 1. get_medicalrecords_classifyquestions - Lấy bộ câu hỏi sàng lọc bệnh án
    @GetMapping({"/classify-questions", "/classify_questions"})
    public ResponseEntity<ClassifyQuestionListResponse> getClassifyQuestions() {
        return ResponseEntity.ok(medicalRecordService.getClassifyQuestions());
    }

    // 2. set_medicalrecords_classify - Lưu kết quả câu hỏi sàng lọc để phân loại bệnh
    @PostMapping("/classify")
    public ResponseEntity<ClassifyMedicalRecordResponse> classifyMedicalRecord(
            @RequestBody @Valid ClassifyMedicalRecordRequest request) {
        return ResponseEntity.ok(medicalRecordService.classifyMedicalRecord(request));
    }

    // 3. set_medicalrecords_selfcreate - Bệnh nhân tự khởi tạo bệnh án sơ bộ
    @PostMapping({"/self-create", "/selfcreate"})
    public ResponseEntity<MedicalRecordResponse> selfCreateMedicalRecord(
            @RequestBody @Valid SelfCreateMedicalRecordRequest request) {
        return ResponseEntity.ok(medicalRecordService.selfCreateByPatient(request));
    }

    // 4. set_medicalrecords_by_doctor - Bác sĩ khởi tạo bệnh án cho bệnh nhân
    @PostMapping({"/doctor-create", "/by-doctor", ""})
    public ResponseEntity<MedicalRecordResponse> createMedicalRecordByDoctor(
            @RequestBody @Valid DoctorCreateMedicalRecordRequest request) {
        return ResponseEntity.ok(medicalRecordService.createByDoctor(request));
    }

    // 5. get_medicalrecords_list - Lấy danh sách hồ sơ bệnh án (Bác sĩ/Bệnh nhân)
    @GetMapping
    public ResponseEntity<MedicalRecordListResponse> getMedicalRecordsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(name = "record_type", required = false) String recordType,
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(name = "doctor_id", required = false) Long doctorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "from_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordsList(
                page, limit, status, recordType, patientId, doctorId, keyword, fromDate, toDate));
    }

    // 6. get_medicalrecords_detail - Xem chi tiết một hồ sơ bệnh án
    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> getMedicalRecordDetail(@PathVariable Long id) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordDetail(id));
    }

    // 7. delete_medicalrecords - Hủy bỏ hồ sơ bệnh án (khi chưa có dữ liệu khám)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMedicalRecord(@PathVariable Long id) {
        return ResponseEntity.ok(medicalRecordService.deleteMedicalRecord(id));
    }

    // 8. get_medicalrecords_answers - Lấy danh sách các câu trả lời form động của bệnh án
    @GetMapping("/{id}/answers")
    public ResponseEntity<MedicalRecordAnswerListResponse> getMedicalRecordAnswers(
            @PathVariable Long id,
            @RequestParam(name = "section_id", required = false) String sectionId,
            @RequestParam(name = "filled_by", required = false) String filledBy) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordAnswers(id, sectionId, filledBy));
    }

    // 9. set_medicalrecords_answers - Lưu các câu trả lời form động vào bệnh án
    @PostMapping("/{id}/answers")
    public ResponseEntity<MedicalRecordAnswerListResponse> saveMedicalRecordAnswers(
            @PathVariable Long id,
            @RequestBody @Valid SaveMedicalRecordAnswersRequest request) {
        return ResponseEntity.ok(medicalRecordService.saveMedicalRecordAnswers(id, request));
    }

    // 10. edit_medicalrecords_answers - Chỉnh sửa câu trả lời form động (hàng loạt & đơn lẻ)
    @PutMapping("/{id}/answers")
    public ResponseEntity<MedicalRecordAnswerListResponse> editMedicalRecordAnswers(
            @PathVariable Long id,
            @RequestBody @Valid EditMedicalRecordAnswersRequest request) {
        return ResponseEntity.ok(medicalRecordService.editMedicalRecordAnswers(id, request));
    }

    @PutMapping("/{id}/answers/{answerId}")
    public ResponseEntity<MedicalRecordAnswerResponse> editSingleMedicalRecordAnswer(
            @PathVariable Long id,
            @PathVariable Long answerId,
            @RequestBody @Valid EditSingleAnswerRequest request) {
        return ResponseEntity.ok(medicalRecordService.editSingleMedicalRecordAnswer(id, answerId, request));
    }

    // 11. edit_medicalrecords - Cập nhật các thông tin chung của bệnh án
    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> editMedicalRecord(
            @PathVariable Long id,
            @RequestBody @Valid UpdateMedicalRecordRequest request) {
        return ResponseEntity.ok(medicalRecordService.editMedicalRecord(id, request));
    }

    // 12. edit_status_medicalrecords - Cập nhật trạng thái bệnh án (pending -> in_progress -> done)
    @PatchMapping("/{id}/status")
    public ResponseEntity<MedicalRecordResponse> patchMedicalRecordStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateMedicalRecordStatusRequest request) {
        return ResponseEntity.ok(medicalRecordService.editMedicalRecordStatus(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<MedicalRecordResponse> putMedicalRecordStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateMedicalRecordStatusRequest request) {
        return ResponseEntity.ok(medicalRecordService.editMedicalRecordStatus(id, request));
    }

    // 13. get_history - Truy xuất lịch sử khám, bệnh sử các đợt trước
    @GetMapping("/history")
    public ResponseEntity<MedicalRecordHistoryResponse> getMedicalHistory(
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(name = "record_type", required = false) String recordType,
            @RequestParam(name = "exclude_record_id", required = false) Long excludeRecordId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(medicalRecordService.getMedicalHistory(
                patientId, recordType, excludeRecordId, page, limit));
    }

    @GetMapping("/patients/{patientId}/history")
    public ResponseEntity<MedicalRecordHistoryResponse> getPatientMedicalHistory(
            @PathVariable Long patientId,
            @RequestParam(name = "record_type", required = false) String recordType,
            @RequestParam(name = "exclude_record_id", required = false) Long excludeRecordId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(medicalRecordService.getMedicalHistory(
                patientId, recordType, excludeRecordId, page, limit));
    }
}
