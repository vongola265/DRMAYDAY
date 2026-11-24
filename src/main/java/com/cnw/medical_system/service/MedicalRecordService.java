package com.cnw.medical_system.service;

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
import com.cnw.medical_system.dto.response.ClassifyQuestionResponse;
import com.cnw.medical_system.dto.response.MedicalRecordAnswerListResponse;
import com.cnw.medical_system.dto.response.MedicalRecordAnswerResponse;
import com.cnw.medical_system.dto.response.MedicalRecordHistoryResponse;
import com.cnw.medical_system.dto.response.MedicalRecordListResponse;
import com.cnw.medical_system.dto.response.MedicalRecordResponse;
import com.cnw.medical_system.entity.BenhNhan;
import com.cnw.medical_system.entity.CauHoiSangLoc;
import com.cnw.medical_system.entity.CauTraLoiBieuMau;
import com.cnw.medical_system.entity.HoSoBenhAn;
import com.cnw.medical_system.entity.KetQuaSangLoc;
import com.cnw.medical_system.entity.Nguoi;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.exception.MedicalRecordException;
import com.cnw.medical_system.mapper.MedicalRecordMapper;
import com.cnw.medical_system.repository.BenhNhanRepository;
import com.cnw.medical_system.repository.CauHoiSangLocRepository;
import com.cnw.medical_system.repository.CauTraLoiBieuMauRepository;
import com.cnw.medical_system.repository.HoSoBenhAnRepository;
import com.cnw.medical_system.repository.KetQuaSangLocRepository;
import com.cnw.medical_system.repository.NguoiRepository;
import com.cnw.medical_system.repository.TaiKhoanRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordService {

    static Set<String> VALID_RECORD_TYPES = Set.of("CAP", "MAN", "TAI_KHAM", "CHUA_PHAN_LOAI");
    static Set<String> VALID_STATUSES = Set.of("pending", "in_progress", "done", "cancelled");

    HoSoBenhAnRepository hoSoBenhAnRepository;
    CauHoiSangLocRepository cauHoiSangLocRepository;
    KetQuaSangLocRepository ketQuaSangLocRepository;
    CauTraLoiBieuMauRepository cauTraLoiBieuMauRepository;
    TaiKhoanRepository taiKhoanRepository;
    BenhNhanRepository benhNhanRepository;
    NguoiRepository nguoiRepository;
    MedicalRecordMapper medicalRecordMapper;
    ObjectMapper objectMapper;

    // =========================================================================
    // 1. get_medicalrecords_classifyquestions - Lấy bộ câu hỏi sàng lọc bệnh án
    // =========================================================================
    @Transactional(readOnly = true)
    public ClassifyQuestionListResponse getClassifyQuestions() {
        getCurrentAccount();
        List<CauHoiSangLoc> entities = cauHoiSangLocRepository
                .findByTrangThaiHoatDongTrueOrderByThuTuHienThiAscMaCauHoiSangLocAsc();

        List<ClassifyQuestionResponse> questions;
        if (entities.isEmpty()) {
            questions = defaultClassifyQuestions();
        } else {
            questions = entities.stream().map(this::mapClassifyQuestion).toList();
        }
        return ClassifyQuestionListResponse.builder()
                .total(questions.size())
                .data(questions)
                .build();
    }

    // =========================================================================
    // 2. set_medicalrecords_classify - Lưu kết quả câu hỏi sàng lọc phân loại
    // =========================================================================
    @Transactional
    public ClassifyMedicalRecordResponse classifyMedicalRecord(ClassifyMedicalRecordRequest request) {
        TaiKhoan currentAccount = getCurrentAccount();
        Nguoi actor = currentAccount.getNguoi();

        BenhNhan targetPatient = null;
        if (isPatient(currentAccount)) {
            if (request.getPatientId() != null && !request.getPatientId().equals(actor.getMaNguoi())) {
                throw MedicalRecordException.forbidden("Bệnh nhân không được phép phân loại sàng lọc cho người khác");
            }
            targetPatient = ensurePatientEntity(actor);
        } else if (request.getPatientId() != null) {
            targetPatient = findOrEnsurePatientById(request.getPatientId());
        }

        HoSoBenhAn linkedRecord = null;
        if (request.getMedicalRecordId() != null) {
            linkedRecord = findRecordOrThrow(request.getMedicalRecordId());
            verifyReadAccess(linkedRecord, currentAccount);
            assertRecordOpen(linkedRecord);
            if (targetPatient == null) {
                targetPatient = linkedRecord.getBenhNhan();
            } else if (!linkedRecord.getBenhNhan().getMaBenhNhan().equals(targetPatient.getMaBenhNhan())) {
                throw MedicalRecordException.badRequest("1004", "patient_id không khớp với hồ sơ bệnh án đã chọn");
            }
        }

        Map<String, String> answerMap = new HashMap<>();
        Set<String> seenCodes = new HashSet<>();
        for (ClassifyMedicalRecordRequest.ClassifyAnswerItem item : request.getAnswers()) {
            String code = item.getQuestionCode().trim().toUpperCase(Locale.ROOT);
            if (!seenCodes.add(code)) {
                throw MedicalRecordException.badRequest("1004", "Trùng lặp mã câu hỏi sàng lọc: " + code);
            }
            answerMap.put(code, item.getAnswerValue().trim());
        }

        ClassificationOutcome outcome = evaluateClassification(answerMap);
        String serializedAnswers = writeJson(request.getAnswers());

        KetQuaSangLoc ketQua = KetQuaSangLoc.builder()
                .benhNhan(targetPatient)
                .nguoiThucHien(actor)
                .duLieuTraLoiJson(serializedAnswers)
                .loaiBenhAnDeXuat(outcome.classifiedType())
                .maBieuMauDeXuat(outcome.templateCode())
                .mucDoUuTien(outcome.priorityLevel())
                .ghiChuPhanLoai(outcome.note())
                .build();
        ketQua = ketQuaSangLocRepository.save(ketQua);

        if (linkedRecord != null) {
            linkedRecord.setKetQuaSangLoc(ketQua);
            linkedRecord.setLoaiBenhAn(outcome.classifiedType());
            if (linkedRecord.getMaBieuMau() == null || linkedRecord.getMaBieuMau().isBlank()) {
                linkedRecord.setMaBieuMau(outcome.templateCode());
            }
            hoSoBenhAnRepository.save(linkedRecord);
        }

        return medicalRecordMapper.toClassifyResponse(
                ketQua,
                linkedRecord != null ? linkedRecord.getMaHoSo() : null
        );
    }

    // =========================================================================
    // 3. set_medicalrecords_selfcreate - Bệnh nhân tự khởi tạo bệnh án sơ bộ
    // =========================================================================
    @Transactional
    public MedicalRecordResponse selfCreateByPatient(SelfCreateMedicalRecordRequest request) {
        TaiKhoan currentAccount = getCurrentAccount();
        if (!isPatient(currentAccount)) {
            throw MedicalRecordException.forbidden("Chỉ Bệnh nhân mới được tự khởi tạo bệnh án sơ bộ qua chức năng này");
        }

        BenhNhan benhNhan = ensurePatientEntity(currentAccount.getNguoi());
        KetQuaSangLoc sangLoc = null;
        if (request.getClassifyId() != null) {
            sangLoc = ketQuaSangLocRepository.findById(request.getClassifyId())
                    .orElseThrow(() -> MedicalRecordException.notFound(
                            "Không tìm thấy kết quả sàng lọc ID " + request.getClassifyId()));
            if (sangLoc.getBenhNhan() != null
                    && !sangLoc.getBenhNhan().getMaBenhNhan().equals(benhNhan.getMaBenhNhan())) {
                throw MedicalRecordException.forbidden("Kết quả sàng lọc không thuộc về tài khoản bệnh nhân này");
            }
        }

        String recordType;
        if (request.getRecordType() != null && !request.getRecordType().isBlank()) {
            recordType = normalizeRecordType(request.getRecordType());
        } else if (sangLoc != null) {
            recordType = sangLoc.getLoaiBenhAnDeXuat();
        } else {
            recordType = "CHUA_PHAN_LOAI";
        }

        String templateCode = cleanNullable(request.getTemplateCode());
        if (templateCode == null && sangLoc != null) {
            templateCode = sangLoc.getMaBieuMauDeXuat();
        } else if (templateCode == null && !"CHUA_PHAN_LOAI".equals(recordType)) {
            templateCode = defaultTemplateForType(recordType);
        }

        HoSoBenhAn hoSo = HoSoBenhAn.builder()
                .benhNhan(benhNhan)
                .maPhongKham(request.getRoomId())
                .loaiBenhAn(recordType)
                .maBieuMau(templateCode)
                .ketQuaSangLoc(sangLoc)
                .trangThai("pending")
                .lyDoKham(request.getChiefComplaint().trim())
                .ghiChu(cleanNullable(request.getNotes()))
                .build();

        hoSo = hoSoBenhAnRepository.save(hoSo);
        return medicalRecordMapper.toMedicalRecordResponse(hoSo, 0L);
    }

    // =========================================================================
    // 4. set_medicalrecords_by_doctor - Bác sĩ khởi tạo bệnh án cho bệnh nhân
    // =========================================================================
    @Transactional
    public MedicalRecordResponse createByDoctor(DoctorCreateMedicalRecordRequest request) {
        TaiKhoan currentAccount = getCurrentAccount();
        if (!isMedicalStaff(currentAccount)) {
            throw MedicalRecordException.forbidden(
                    "Chỉ Bác sĩ hoặc Nhân viên y tế mới có quyền khởi tạo bệnh án cho bệnh nhân");
        }

        BenhNhan benhNhan = findOrEnsurePatientById(request.getPatientId());
        KetQuaSangLoc sangLoc = null;
        if (request.getClassifyId() != null) {
            sangLoc = ketQuaSangLocRepository.findById(request.getClassifyId())
                    .orElseThrow(() -> MedicalRecordException.notFound(
                            "Không tìm thấy kết quả sàng lọc ID " + request.getClassifyId()));
            if (sangLoc.getBenhNhan() != null
                    && !sangLoc.getBenhNhan().getMaBenhNhan().equals(benhNhan.getMaBenhNhan())) {
                throw MedicalRecordException.badRequest(
                        "1004", "Kết quả sàng lọc không thuộc bệnh nhân ID " + request.getPatientId());
            }
        }

        String recordType = normalizeRecordType(request.getRecordType());
        String initialStatus = "in_progress";
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            initialStatus = normalizeStatus(request.getStatus());
            if (!"pending".equals(initialStatus) && !"in_progress".equals(initialStatus)) {
                throw MedicalRecordException.badRequest(
                        "1004", "Trạng thái khởi tạo chỉ chấp nhận 'pending' hoặc 'in_progress'");
            }
        }

        String templateCode = cleanNullable(request.getTemplateCode());
        if (templateCode == null && sangLoc != null) {
            templateCode = sangLoc.getMaBieuMauDeXuat();
        } else if (templateCode == null) {
            templateCode = defaultTemplateForType(recordType);
        }

        HoSoBenhAn hoSo = HoSoBenhAn.builder()
                .benhNhan(benhNhan)
                .bacSi(currentAccount.getNguoi())
                .maPhongKham(request.getRoomId())
                .loaiBenhAn(recordType)
                .maBieuMau(templateCode)
                .ketQuaSangLoc(sangLoc)
                .trangThai(initialStatus)
                .lyDoKham(request.getChiefComplaint().trim())
                .chanDoanSoBo(cleanNullable(request.getPreliminaryDiagnosis()))
                .ghiChu(cleanNullable(request.getNotes()))
                .thoiDiemTiepNhan("in_progress".equals(initialStatus) ? LocalDateTime.now() : null)
                .build();

        hoSo = hoSoBenhAnRepository.save(hoSo);
        return medicalRecordMapper.toMedicalRecordResponse(hoSo, 0L);
    }

    // =========================================================================
    // 5. get_medicalrecords_list - Lấy danh sách hồ sơ bệnh án (BS / BN / Admin)
    // =========================================================================
    @Transactional(readOnly = true)
    public MedicalRecordListResponse getMedicalRecordsList(
            int page,
            int limit,
            String status,
            String recordType,
            Long patientId,
            Long doctorId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate) {

        TaiKhoan currentAccount = getCurrentAccount();
        validatePagination(page, limit);

        Long effectivePatientId = patientId;
        if (isPatient(currentAccount)) {
            Long selfId = currentAccount.getNguoi().getMaNguoi();
            if (patientId != null && !patientId.equals(selfId)) {
                throw MedicalRecordException.forbidden("Bệnh nhân chỉ được xem danh sách hồ sơ bệnh án của chính mình");
            }
            effectivePatientId = selfId;
        } else if (patientId != null && patientId <= 0) {
            throw MedicalRecordException.badRequest("1004", "patient_id phải là số nguyên dương");
        }

        if (doctorId != null && doctorId <= 0) {
            throw MedicalRecordException.badRequest("1004", "doctor_id phải là số nguyên dương");
        }

        String normalizedStatus = (status != null && !status.isBlank()) ? normalizeStatus(status) : null;
        String normalizedType = (recordType != null && !recordType.isBlank()) ? normalizeRecordType(recordType) : null;
        Long finalPatientId = effectivePatientId;

        Specification<HoSoBenhAn> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (finalPatientId != null) {
                predicates.add(cb.equal(root.get("benhNhan").get("maBenhNhan"), finalPatientId));
            }
            if (doctorId != null) {
                predicates.add(cb.equal(root.join("bacSi", JoinType.LEFT).get("maNguoi"), doctorId));
            }
            if (normalizedStatus != null) {
                predicates.add(cb.equal(cb.lower(root.get("trangThai")), normalizedStatus));
            }
            if (normalizedType != null) {
                predicates.add(cb.equal(cb.upper(root.get("loaiBenhAn")), normalizedType));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("thoiDiemTao"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("thoiDiemTao"), toDate.atTime(LocalTime.MAX)));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                var patientJoin = root.join("benhNhan", JoinType.LEFT).join("nguoi", JoinType.LEFT);
                Predicate byComplaint = cb.like(cb.lower(root.get("lyDoKham")), pattern);
                Predicate byPrelim = cb.like(cb.lower(root.get("chanDoanSoBo")), pattern);
                Predicate byFinal = cb.like(cb.lower(root.get("chanDoanXacDinh")), pattern);
                Predicate byPatientName = cb.like(cb.lower(patientJoin.get("hoTen")), pattern);
                Predicate byPatientPhone = cb.like(patientJoin.get("soDienThoai"), pattern);
                predicates.add(cb.or(byComplaint, byPrelim, byFinal, byPatientName, byPatientPhone));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Order.desc("thoiDiemTao"), Sort.Order.desc("maHoSo"))
        );
        Page<HoSoBenhAn> resultPage = hoSoBenhAnRepository.findAll(spec, pageable);

        List<MedicalRecordResponse> items = resultPage.getContent().stream()
                .map(hoSo -> medicalRecordMapper.toMedicalRecordResponse(
                        hoSo,
                        cauTraLoiBieuMauRepository.countByHoSoBenhAn_MaHoSo(hoSo.getMaHoSo())))
                .toList();

        return MedicalRecordListResponse.builder()
                .total(resultPage.getTotalElements())
                .page(page)
                .limit(limit)
                .data(items)
                .build();
    }

    // =========================================================================
    // 6. get_medicalrecords_detail - Xem chi tiết một hồ sơ bệnh án
    // =========================================================================
    @Transactional(readOnly = true)
    public MedicalRecordResponse getMedicalRecordDetail(Long recordId) {
        validatePositiveId(recordId, "ID hồ sơ bệnh án");
        TaiKhoan currentAccount = getCurrentAccount();
        HoSoBenhAn hoSo = findRecordOrThrow(recordId);
        verifyReadAccess(hoSo, currentAccount);
        long answerCount = cauTraLoiBieuMauRepository.countByHoSoBenhAn_MaHoSo(recordId);
        return medicalRecordMapper.toMedicalRecordResponse(hoSo, answerCount);
    }

    // =========================================================================
    // 7. delete_medicalrecords - Hủy bỏ hồ sơ bệnh án (khi chưa có dữ liệu khám)
    // =========================================================================
    @Transactional
    public Map<String, Object> deleteMedicalRecord(Long recordId) {
        validatePositiveId(recordId, "ID hồ sơ bệnh án");
        TaiKhoan currentAccount = getCurrentAccount();
        HoSoBenhAn hoSo = findRecordOrThrow(recordId);
        verifyReadAccess(hoSo, currentAccount);

        if (!"pending".equalsIgnoreCase(hoSo.getTrangThai())) {
            throw MedicalRecordException.conflict(
                    "1010",
                    "Chỉ được phép hủy bỏ hồ sơ bệnh án khi đang ở trạng thái chờ khám (pending)"
            );
        }

        if (hasClinicalExamData(hoSo)) {
            throw MedicalRecordException.conflict(
                    "1010",
                    "Không thể hủy bỏ hồ sơ bệnh án vì đã phát sinh dữ liệu khám lâm sàng"
            );
        }

        cauTraLoiBieuMauRepository.deleteByHoSoBenhAn_MaHoSo(recordId);
        hoSoBenhAnRepository.delete(hoSo);

        return Map.of(
                "deleted", true,
                "medical_record_id", recordId,
                "message", "Đã hủy bỏ hồ sơ bệnh án thành công"
        );
    }

    // =========================================================================
    // 8. get_medicalrecords_answers - Lấy danh sách câu trả lời form động
    // =========================================================================
    @Transactional(readOnly = true)
    public MedicalRecordAnswerListResponse getMedicalRecordAnswers(
            Long recordId,
            String sectionId,
            String filledBy) {

        validatePositiveId(recordId, "ID hồ sơ bệnh án");
        TaiKhoan currentAccount = getCurrentAccount();
        HoSoBenhAn hoSo = findRecordOrThrow(recordId);
        verifyReadAccess(hoSo, currentAccount);

        List<CauTraLoiBieuMau> entities = cauTraLoiBieuMauRepository
                .findByHoSoBenhAn_MaHoSoOrderByMaCauTraLoiAsc(recordId);

        List<MedicalRecordAnswerResponse> answers = entities.stream()
                .filter(a -> sectionId == null || sectionId.isBlank()
                        || sectionId.trim().equalsIgnoreCase(a.getMaPhanBieuMau()))
                .filter(a -> filledBy == null || filledBy.isBlank()
                        || filledBy.trim().equalsIgnoreCase(a.getVaiTroNguoiNhap()))
                .map(medicalRecordMapper::toAnswerResponse)
                .toList();

        return MedicalRecordAnswerListResponse.builder()
                .medicalRecordId(recordId)
                .total(answers.size())
                .data(answers)
                .build();
    }

    // =========================================================================
    // 9. set_medicalrecords_answers - Lưu các câu trả lời form động vào bệnh án
    // =========================================================================
    @Transactional
    public MedicalRecordAnswerListResponse saveMedicalRecordAnswers(
            Long recordId,
            SaveMedicalRecordAnswersRequest request) {

        validatePositiveId(recordId, "ID hồ sơ bệnh án");
        TaiKhoan currentAccount = getCurrentAccount();
        if (isAdmin(currentAccount)) {
            throw MedicalRecordException.forbidden("Quản trị viên không trực tiếp nhập câu trả lời khám bệnh");
        }

        HoSoBenhAn hoSo = findRecordOrThrow(recordId);
        verifyReadAccess(hoSo, currentAccount);
        assertRecordOpen(hoSo);

        Set<String> seenQuestions = new HashSet<>();
        for (SaveMedicalRecordAnswersRequest.AnswerItem item : request.getAnswers()) {
            String questionId = item.getQuestionId().trim();
            if (!seenQuestions.add(questionId.toUpperCase(Locale.ROOT))) {
                throw MedicalRecordException.badRequest("1004", "Trùng lặp question_id trong cùng yêu cầu: " + questionId);
            }
            if (cauTraLoiBieuMauRepository.existsByHoSoBenhAn_MaHoSoAndMaCauHoi(recordId, questionId)) {
                throw MedicalRecordException.conflict(
                        "9996",
                        "Câu hỏi '" + questionId + "' đã có câu trả lời trong hồ sơ này. Vui lòng dùng API chỉnh sửa"
                );
            }
        }

        String actorRole = normalizeActorRole(currentAccount);
        Nguoi actor = currentAccount.getNguoi();
        List<CauTraLoiBieuMau> toSave = new ArrayList<>();

        for (SaveMedicalRecordAnswersRequest.AnswerItem item : request.getAnswers()) {
            CauTraLoiBieuMau entity = CauTraLoiBieuMau.builder()
                    .hoSoBenhAn(hoSo)
                    .maPhanBieuMau(cleanNullable(item.getSectionId()))
                    .maCauHoi(item.getQuestionId().trim())
                    .giaTriTraLoi(item.getAnswerValue())
                    .kieuDuLieu(cleanNullable(item.getDataType()))
                    .ghiChu(cleanNullable(item.getNote()))
                    .vaiTroNguoiNhap(actorRole)
                    .nguoiNhap(actor)
                    .build();
            toSave.add(entity);
        }

        cauTraLoiBieuMauRepository.saveAll(toSave);

        List<MedicalRecordAnswerResponse> allAnswers = cauTraLoiBieuMauRepository
                .findByHoSoBenhAn_MaHoSoOrderByMaCauTraLoiAsc(recordId)
                .stream()
                .map(medicalRecordMapper::toAnswerResponse)
                .toList();

        return MedicalRecordAnswerListResponse.builder()
                .medicalRecordId(recordId)
                .total(allAnswers.size())
                .data(allAnswers)
                .build();
    }

    // =========================================================================
    // 10. edit_medicalrecords_answers - Chỉnh sửa câu trả lời form động
    // =========================================================================
    @Transactional
    public MedicalRecordAnswerListResponse editMedicalRecordAnswers(
            Long recordId,
            EditMedicalRecordAnswersRequest request) {

        validatePositiveId(recordId, "ID hồ sơ bệnh án");
        TaiKhoan currentAccount = getCurrentAccount();
        if (isAdmin(currentAccount)) {
            throw MedicalRecordException.forbidden("Quản trị viên không trực tiếp sửa câu trả lời khám bệnh");
        }

        HoSoBenhAn hoSo = findRecordOrThrow(recordId);
        verifyReadAccess(hoSo, currentAccount);
        assertRecordOpen(hoSo);

        boolean actorIsPatient = isPatient(currentAccount);
        String actorRole = normalizeActorRole(currentAccount);
        Nguoi actor = currentAccount.getNguoi();

        for (EditMedicalRecordAnswersRequest.EditAnswerItem item : request.getAnswers()) {
            CauTraLoiBieuMau target;
            if (item.getAnswerId() != null) {
                target = cauTraLoiBieuMauRepository
                        .findByMaCauTraLoiAndHoSoBenhAn_MaHoSo(item.getAnswerId(), recordId)
                        .orElseThrow(() -> MedicalRecordException.notFound(
                                "Không tìm thấy câu trả lời ID " + item.getAnswerId() + " trong bệnh án này"));
            } else if (item.getQuestionId() != null && !item.getQuestionId().isBlank()) {
                String qId = item.getQuestionId().trim();
                target = cauTraLoiBieuMauRepository
                        .findByHoSoBenhAn_MaHoSoAndMaCauHoi(recordId, qId)
                        .orElseThrow(() -> MedicalRecordException.notFound(
                                "Không tìm thấy câu trả lời cho câu hỏi '" + qId + "' trong bệnh án này"));
            } else {
                throw MedicalRecordException.badRequest(
                        "1002", "Cần cung cấp answer_id hoặc question_id để chỉnh sửa câu trả lời");
            }

            if (actorIsPatient && !"patient".equalsIgnoreCase(target.getVaiTroNguoiNhap())) {
                throw MedicalRecordException.forbidden(
                        "Bệnh nhân không được phép chỉnh sửa câu trả lời chuyên môn của Bác sĩ / Nhân viên y tế");
            }

            target.setGiaTriTraLoi(item.getAnswerValue());
            if (item.getSectionId() != null) {
                target.setMaPhanBieuMau(cleanNullable(item.getSectionId()));
            }
            if (item.getDataType() != null) {
                target.setKieuDuLieu(cleanNullable(item.getDataType()));
            }
            if (item.getNote() != null) {
                target.setGhiChu(cleanNullable(item.getNote()));
            }
            target.setVaiTroNguoiNhap(actorRole);
            target.setNguoiNhap(actor);
            cauTraLoiBieuMauRepository.save(target);
        }

        List<MedicalRecordAnswerResponse> updatedList = cauTraLoiBieuMauRepository
                .findByHoSoBenhAn_MaHoSoOrderByMaCauTraLoiAsc(recordId)
                .stream()
                .map(medicalRecordMapper::toAnswerResponse)
                .toList();

        return MedicalRecordAnswerListResponse.builder()
                .medicalRecordId(recordId)
                .total(updatedList.size())
                .data(updatedList)
                .build();
    }

    @Transactional
    public MedicalRecordAnswerResponse editSingleMedicalRecordAnswer(
            Long recordId,
            Long answerId,
            EditSingleAnswerRequest request) {

        validatePositiveId(recordId, "ID hồ sơ bệnh án");
        validatePositiveId(answerId, "ID câu trả lời");
        TaiKhoan currentAccount = getCurrentAccount();
        if (isAdmin(currentAccount)) {
            throw MedicalRecordException.forbidden("Quản trị viên không trực tiếp sửa câu trả lời khám bệnh");
        }

        HoSoBenhAn hoSo = findRecordOrThrow(recordId);
        verifyReadAccess(hoSo, currentAccount);
        assertRecordOpen(hoSo);

        CauTraLoiBieuMau target = cauTraLoiBieuMauRepository
                .findByMaCauTraLoiAndHoSoBenhAn_MaHoSo(answerId, recordId)
                .orElseThrow(() -> MedicalRecordException.notFound(
                        "Không tìm thấy câu trả lời ID " + answerId + " trong bệnh án này"));

        if (isPatient(currentAccount) && !"patient".equalsIgnoreCase(target.getVaiTroNguoiNhap())) {
            throw MedicalRecordException.forbidden(
                    "Bệnh nhân không được phép chỉnh sửa câu trả lời chuyên môn của Bác sĩ / Nhân viên y tế");
        }

        target.setGiaTriTraLoi(request.getAnswerValue());
        if (request.getSectionId() != null) {
            target.setMaPhanBieuMau(cleanNullable(request.getSectionId()));
        }
        if (request.getDataType() != null) {
            target.setKieuDuLieu(cleanNullable(request.getDataType()));
        }
        if (request.getNote() != null) {
            target.setGhiChu(cleanNullable(request.getNote()));
        }
        target.setVaiTroNguoiNhap(normalizeActorRole(currentAccount));
        target.setNguoiNhap(currentAccount.getNguoi());

        return medicalRecordMapper.toAnswerResponse(cauTraLoiBieuMauRepository.save(target));
    }

    // =========================================================================
    // 11. edit_medicalrecords - Cập nhật các thông tin chung của bệnh án
    // =========================================================================
    @Transactional
    public MedicalRecordResponse editMedicalRecord(Long recordId, UpdateMedicalRecordRequest request) {
        validatePositiveId(recordId, "ID hồ sơ bệnh án");
        if (request == null || request.isEmpty()) {
            throw MedicalRecordException.badRequest("1002", "Cần cung cấp ít nhất một trường dữ liệu để cập nhật");
        }

        TaiKhoan currentAccount = getCurrentAccount();
        HoSoBenhAn hoSo = findRecordOrThrow(recordId);
        verifyReadAccess(hoSo, currentAccount);
        assertRecordOpen(hoSo);

        if (isPatient(currentAccount)) {
            if (!"pending".equalsIgnoreCase(hoSo.getTrangThai())) {
                throw MedicalRecordException.conflict(
                        "1010",
                        "Hồ sơ bệnh án đã được bác sĩ tiếp nhận khám, bệnh nhân không thể sửa thông tin chung"
                );
            }
            if (request.getPreliminaryDiagnosis() != null
                    || request.getFinalDiagnosis() != null
                    || request.getTreatmentPlan() != null
                    || request.getDoctorId() != null
                    || request.getFollowUpDate() != null) {
                throw MedicalRecordException.forbidden(
                        "Bệnh nhân không được phép cập nhật chẩn đoán, hướng xử trí hoặc bác sĩ điều trị"
                );
            }
            if (request.getChiefComplaint() != null) {
                if (request.getChiefComplaint().isBlank()) {
                    throw MedicalRecordException.badRequest("1004", "Lý do khám không được để trống");
                }
                hoSo.setLyDoKham(request.getChiefComplaint().trim());
            }
            if (request.getNotes() != null) {
                hoSo.setGhiChu(cleanNullable(request.getNotes()));
            }
            if (request.getRecordType() != null && !request.getRecordType().isBlank()) {
                hoSo.setLoaiBenhAn(normalizeRecordType(request.getRecordType()));
            }
            if (request.getTemplateCode() != null) {
                hoSo.setMaBieuMau(cleanNullable(request.getTemplateCode()));
            }
            if (request.getRoomId() != null) {
                hoSo.setMaPhongKham(request.getRoomId());
            }
        } else {
            if (request.getRecordType() != null && !request.getRecordType().isBlank()) {
                hoSo.setLoaiBenhAn(normalizeRecordType(request.getRecordType()));
            }
            if (request.getTemplateCode() != null) {
                hoSo.setMaBieuMau(cleanNullable(request.getTemplateCode()));
            }
            if (request.getRoomId() != null) {
                hoSo.setMaPhongKham(request.getRoomId());
            }
            if (request.getDoctorId() != null) {
                Nguoi assignedDoctor = findDoctorNguoiOrThrow(request.getDoctorId());
                hoSo.setBacSi(assignedDoctor);
            } else if (hoSo.getBacSi() == null && isMedicalStaff(currentAccount)) {
                hoSo.setBacSi(currentAccount.getNguoi());
            }
            if (request.getChiefComplaint() != null) {
                if (request.getChiefComplaint().isBlank()) {
                    throw MedicalRecordException.badRequest("1004", "Lý do khám không được để trống");
                }
                hoSo.setLyDoKham(request.getChiefComplaint().trim());
            }
            if (request.getPreliminaryDiagnosis() != null) {
                hoSo.setChanDoanSoBo(cleanNullable(request.getPreliminaryDiagnosis()));
            }
            if (request.getFinalDiagnosis() != null) {
                hoSo.setChanDoanXacDinh(cleanNullable(request.getFinalDiagnosis()));
            }
            if (request.getTreatmentPlan() != null) {
                hoSo.setHuongXuTri(cleanNullable(request.getTreatmentPlan()));
            }
            if (request.getNotes() != null) {
                hoSo.setGhiChu(cleanNullable(request.getNotes()));
            }
            if (request.getFollowUpDate() != null) {
                if (request.getFollowUpDate().isBefore(LocalDate.now())) {
                    throw MedicalRecordException.badRequest("1004", "Ngày hẹn tái khám không được ở quá khứ");
                }
                hoSo.setNgayHenTaiKham(request.getFollowUpDate());
            }
        }

        hoSo = hoSoBenhAnRepository.save(hoSo);
        long answerCount = cauTraLoiBieuMauRepository.countByHoSoBenhAn_MaHoSo(recordId);
        return medicalRecordMapper.toMedicalRecordResponse(hoSo, answerCount);
    }

    // =========================================================================
    // 12. edit_status_medicalrecords - Cập nhật trạng thái (pending -> in_progress -> done)
    // =========================================================================
    @Transactional
    public MedicalRecordResponse editMedicalRecordStatus(Long recordId, UpdateMedicalRecordStatusRequest request) {
        validatePositiveId(recordId, "ID hồ sơ bệnh án");
        TaiKhoan currentAccount = getCurrentAccount();
        if (isPatient(currentAccount)) {
            throw MedicalRecordException.forbidden(
                    "Bệnh nhân không có quyền chuyển trạng thái quy trình khám bệnh án"
            );
        }

        HoSoBenhAn hoSo = findRecordOrThrow(recordId);
        String currentStatus = hoSo.getTrangThai().toLowerCase(Locale.ROOT);
        String nextStatus = normalizeStatus(request.getStatus());

        if (currentStatus.equals(nextStatus)) {
            throw MedicalRecordException.conflict(
                    "1010", "Hồ sơ bệnh án hiện đã ở trạng thái '" + currentStatus + "'"
            );
        }

        switch (currentStatus) {
            case "pending" -> {
                if ("in_progress".equals(nextStatus)) {
                    hoSo.setTrangThai("in_progress");
                    hoSo.setThoiDiemTiepNhan(LocalDateTime.now());
                    if (hoSo.getBacSi() == null && isMedicalStaff(currentAccount)) {
                        hoSo.setBacSi(currentAccount.getNguoi());
                    }
                } else if ("cancelled".equals(nextStatus)) {
                    if (hasClinicalExamData(hoSo)) {
                        throw MedicalRecordException.conflict(
                                "1010", "Không thể hủy hồ sơ bệnh án đã có dữ liệu khám lâm sàng"
                        );
                    }
                    hoSo.setTrangThai("cancelled");
                } else {
                    throw invalidTransitionException(currentStatus, nextStatus);
                }
            }
            case "in_progress" -> {
                if ("done".equals(nextStatus)) {
                    boolean hasDiagnosis = (hoSo.getChanDoanXacDinh() != null && !hoSo.getChanDoanXacDinh().isBlank())
                            || (hoSo.getChanDoanSoBo() != null && !hoSo.getChanDoanSoBo().isBlank())
                            || cauTraLoiBieuMauRepository.existsByHoSoBenhAn_MaHoSoAndVaiTroNguoiNhapIn(
                            recordId, List.of("doctor", "nurse"));
                    if (!hasDiagnosis) {
                        throw MedicalRecordException.badRequest(
                                "1004",
                                "Cần cập nhật chẩn đoán hoặc thông tin khám lâm sàng trước khi hoàn tất bệnh án (done)"
                        );
                    }
                    hoSo.setTrangThai("done");
                    hoSo.setThoiDiemHoanThanh(LocalDateTime.now());
                    if (hoSo.getBacSi() == null && isMedicalStaff(currentAccount)) {
                        hoSo.setBacSi(currentAccount.getNguoi());
                    }
                } else {
                    throw invalidTransitionException(currentStatus, nextStatus);
                }
            }
            default -> throw invalidTransitionException(currentStatus, nextStatus);
        }

        if (request.getNote() != null && !request.getNote().isBlank()) {
            String appended = (hoSo.getGhiChu() == null || hoSo.getGhiChu().isBlank())
                    ? request.getNote().trim()
                    : hoSo.getGhiChu() + " | [" + nextStatus + "] " + request.getNote().trim();
            hoSo.setGhiChu(appended);
        }

        hoSo = hoSoBenhAnRepository.save(hoSo);
        long answerCount = cauTraLoiBieuMauRepository.countByHoSoBenhAn_MaHoSo(recordId);
        return medicalRecordMapper.toMedicalRecordResponse(hoSo, answerCount);
    }

    // =========================================================================
    // 13. get_history - Truy xuất lịch sử khám, bệnh sử các đợt trước
    // =========================================================================
    @Transactional(readOnly = true)
    public MedicalRecordHistoryResponse getMedicalHistory(
            Long requestedPatientId,
            String recordType,
            Long excludeRecordId,
            int page,
            int limit) {

        TaiKhoan currentAccount = getCurrentAccount();
        validatePagination(page, limit);

        Long targetPatientId;
        if (isPatient(currentAccount)) {
            Long selfId = currentAccount.getNguoi().getMaNguoi();
            if (requestedPatientId != null && !requestedPatientId.equals(selfId)) {
                throw MedicalRecordException.forbidden("Bệnh nhân chỉ được truy xuất lịch sử khám của chính mình");
            }
            targetPatientId = selfId;
        } else {
            if (requestedPatientId == null) {
                throw MedicalRecordException.badRequest(
                        "1002", "Vui lòng truyền patient_id để tra cứu lịch sử khám của bệnh nhân"
                );
            }
            validatePositiveId(requestedPatientId, "patient_id");
            targetPatientId = requestedPatientId;
        }

        Nguoi patientPerson = nguoiRepository.findById(targetPatientId)
                .orElseThrow(() -> MedicalRecordException.notFound(
                        "Không tìm thấy bệnh nhân với ID " + targetPatientId));

        String normalizedType = (recordType != null && !recordType.isBlank())
                ? normalizeRecordType(recordType)
                : null;

        Specification<HoSoBenhAn> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("benhNhan").get("maBenhNhan"), targetPatientId));
            if (normalizedType != null) {
                predicates.add(cb.equal(cb.upper(root.get("loaiBenhAn")), normalizedType));
            }
            if (excludeRecordId != null && excludeRecordId > 0) {
                predicates.add(cb.notEqual(root.get("maHoSo"), excludeRecordId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Order.desc("thoiDiemTao"), Sort.Order.desc("maHoSo"))
        );
        Page<HoSoBenhAn> historyPage = hoSoBenhAnRepository.findAll(spec, pageable);

        List<MedicalRecordHistoryResponse.VisitHistoryItem> visits = historyPage.getContent().stream()
                .map(hoSo -> {
                    List<MedicalRecordAnswerResponse> answers = cauTraLoiBieuMauRepository
                            .findByHoSoBenhAn_MaHoSoOrderByMaCauTraLoiAsc(hoSo.getMaHoSo())
                            .stream()
                            .map(medicalRecordMapper::toAnswerResponse)
                            .toList();
                    return medicalRecordMapper.toVisitHistoryItem(hoSo, answers);
                })
                .toList();

        long totalVisits = hoSoBenhAnRepository.countByBenhNhan_MaBenhNhan(targetPatientId);
        long completedVisits = hoSoBenhAnRepository
                .countByBenhNhan_MaBenhNhanAndTrangThaiIgnoreCase(targetPatientId, "done");
        long chronicVisits = hoSoBenhAnRepository
                .countByBenhNhan_MaBenhNhanAndLoaiBenhAnIgnoreCase(targetPatientId, "MAN");

        return MedicalRecordHistoryResponse.builder()
                .patientId(targetPatientId)
                .patientName(patientPerson.getHoTen())
                .patientPhone(patientPerson.getSoDienThoai())
                .totalVisits(totalVisits)
                .completedVisits(completedVisits)
                .chronicVisits(chronicVisits)
                .page(page)
                .limit(limit)
                .visits(visits)
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private TaiKhoan getCurrentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw MedicalRecordException.forbidden("Không tìm thấy phiên đăng nhập hợp lệ");
        }
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(authentication.getName())
                .orElseThrow(() -> MedicalRecordException.notFound("Không tìm thấy tài khoản đang đăng nhập"));
        if (!Boolean.TRUE.equals(taiKhoan.getTrangThaiHoatDong())) {
            throw MedicalRecordException.forbidden("Tài khoản của bạn hiện đang bị khóa");
        }
        return taiKhoan;
    }

    private boolean isPatient(TaiKhoan account) {
        String role = roleName(account);
        return "benh_nhan".equals(role) || "patient".equals(role);
    }

    private boolean isMedicalStaff(TaiKhoan account) {
        String role = roleName(account);
        return "bac_si".equals(role) || "doctor".equals(role)
                || "dieu_duong".equals(role) || "nurse".equals(role);
    }

    private boolean isAdmin(TaiKhoan account) {
        return "admin".equals(roleName(account));
    }

    private String roleName(TaiKhoan account) {
        if (account.getVaiTro() == null || account.getVaiTro().getTenVaiTro() == null) {
            return "";
        }
        return account.getVaiTro().getTenVaiTro().trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeActorRole(TaiKhoan account) {
        String role = roleName(account);
        return switch (role) {
            case "bac_si", "doctor" -> "doctor";
            case "dieu_duong", "nurse" -> "nurse";
            case "benh_nhan", "patient" -> "patient";
            default -> role;
        };
    }

    private BenhNhan ensurePatientEntity(Nguoi nguoi) {
        return benhNhanRepository.findById(nguoi.getMaNguoi())
                .orElseGet(() -> benhNhanRepository.save(BenhNhan.builder().nguoi(nguoi).build()));
    }

    private BenhNhan findOrEnsurePatientById(Long patientId) {
        validatePositiveId(patientId, "patient_id");
        return benhNhanRepository.findById(patientId).orElseGet(() -> {
            Nguoi nguoi = nguoiRepository.findById(patientId)
                    .orElseThrow(() -> MedicalRecordException.notFound(
                            "Không tìm thấy bệnh nhân với ID " + patientId));
            return benhNhanRepository.save(BenhNhan.builder().nguoi(nguoi).build());
        });
    }

    private Nguoi findDoctorNguoiOrThrow(Long doctorNguoiId) {
        Nguoi nguoi = nguoiRepository.findById(doctorNguoiId)
                .orElseThrow(() -> MedicalRecordException.notFound("Không tìm thấy bác sĩ với ID " + doctorNguoiId));
        taiKhoanRepository.findAll().stream()
                .filter(tk -> tk.getNguoi() != null && doctorNguoiId.equals(tk.getNguoi().getMaNguoi()))
                .findFirst()
                .ifPresent(tk -> {
                    if (!isMedicalStaff(tk)) {
                        throw MedicalRecordException.badRequest(
                                "1004", "Tài khoản gắn với doctor_id không thuộc nhóm Bác sĩ / Nhân viên y tế");
                    }
                });
        return nguoi;
    }

    private HoSoBenhAn findRecordOrThrow(Long recordId) {
        return hoSoBenhAnRepository.findById(recordId)
                .orElseThrow(() -> MedicalRecordException.notFound("Không tìm thấy hồ sơ bệnh án ID " + recordId));
    }

    private void verifyReadAccess(HoSoBenhAn hoSo, TaiKhoan actor) {
        if (isPatient(actor)) {
            Long actorPersonId = actor.getNguoi().getMaNguoi();
            if (hoSo.getBenhNhan() == null || !actorPersonId.equals(hoSo.getBenhNhan().getMaBenhNhan())) {
                throw MedicalRecordException.forbidden("Bạn không có quyền truy cập hồ sơ bệnh án của người khác");
            }
        }
    }

    private void assertRecordOpen(HoSoBenhAn hoSo) {
        String status = hoSo.getTrangThai() != null ? hoSo.getTrangThai().toLowerCase(Locale.ROOT) : "";
        if ("done".equals(status) || "cancelled".equals(status)) {
            throw MedicalRecordException.conflict(
                    "1010",
                    "Hồ sơ bệnh án đã đóng (" + status + "), không thể tiếp tục chỉnh sửa"
            );
        }
    }

    private boolean hasClinicalExamData(HoSoBenhAn hoSo) {
        if (hoSo.getChanDoanSoBo() != null && !hoSo.getChanDoanSoBo().isBlank()) {
            return true;
        }
        if (hoSo.getChanDoanXacDinh() != null && !hoSo.getChanDoanXacDinh().isBlank()) {
            return true;
        }
        if (hoSo.getHuongXuTri() != null && !hoSo.getHuongXuTri().isBlank()) {
            return true;
        }
        return cauTraLoiBieuMauRepository.existsByHoSoBenhAn_MaHoSoAndVaiTroNguoiNhapIn(
                hoSo.getMaHoSo(),
                List.of("doctor", "nurse")
        );
    }

    private String normalizeRecordType(String raw) {
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        String mapped = switch (upper) {
            case "ACUTE", "MAY_DAY_CAP" -> "CAP";
            case "CHRONIC", "MAY_DAY_MAN" -> "MAN";
            case "FOLLOW_UP", "TAIKHAM" -> "TAI_KHAM";
            default -> upper;
        };
        if (!VALID_RECORD_TYPES.contains(mapped)) {
            throw MedicalRecordException.badRequest(
                    "1004",
                    "Loại bệnh án không hợp lệ: '" + raw + "'. Chấp nhận: CAP, MAN, TAI_KHAM, CHUA_PHAN_LOAI"
            );
        }
        return mapped;
    }

    private String normalizeStatus(String raw) {
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if ("completed".equals(lower)) {
            lower = "done";
        }
        if (!VALID_STATUSES.contains(lower)) {
            throw MedicalRecordException.badRequest(
                    "1004",
                    "Trạng thái không hợp lệ: '" + raw + "'. Chấp nhận: pending, in_progress, done, cancelled"
            );
        }
        return lower;
    }

    private MedicalRecordException invalidTransitionException(String from, String to) {
        return MedicalRecordException.conflict(
                "1010",
                "Không thể chuyển trạng thái từ '" + from + "' sang '" + to
                        + "'. Quy trình chuẩn: pending -> in_progress -> done"
        );
    }

    private String defaultTemplateForType(String recordType) {
        return switch (recordType) {
            case "MAN" -> "TMPL_MAYDAY_MAN";
            case "TAI_KHAM" -> "TMPL_MAYDAY_TAI_KHAM";
            case "CAP" -> "TMPL_MAYDAY_CAP";
            default -> null;
        };
    }

    private ClassificationOutcome evaluateClassification(Map<String, String> answerMap) {
        String duration = answerMap.getOrDefault("SYMPTOM_DURATION_WEEKS", "").toUpperCase(Locale.ROOT);
        String previousVisit = answerMap.getOrDefault("PREVIOUS_VISIT", "").toUpperCase(Locale.ROOT);
        String danger = answerMap.getOrDefault("ANGIOEDEMA_DANGER_SIGNS", "").toUpperCase(Locale.ROOT);

        String classifiedType;
        String note;
        if ("FOLLOW_UP".equals(previousVisit) || "TAI_KHAM".equals(previousVisit) || "YES".equals(previousVisit)) {
            classifiedType = "TAI_KHAM";
            note = "Phân loại bệnh án tái khám theo dõi mày đay";
        } else if ("OVER_6_WEEKS".equals(duration) || "MAN".equals(duration)
                || "TRUE".equals(duration) || "YES".equals(duration)) {
            classifiedType = "MAN";
            note = "Triệu chứng kéo dài >= 6 tuần: Phân loại Mày đay mạn tính (MAN)";
        } else {
            classifiedType = "CAP";
            note = "Triệu chứng mới khởi phát < 6 tuần: Phân loại Mày đay cấp tính (CAP)";
        }

        String priority = "normal";
        if ("BREATHING_DIFFICULTY".equals(danger) || "URGENT".equals(danger)) {
            priority = "urgent";
            note = note + " | Cảnh báo: Có dấu hiệu phù mạch kèm khó thở/nghẹn họng, cần ưu tiên khám ngay!";
        }

        return new ClassificationOutcome(
                classifiedType,
                defaultTemplateForType(classifiedType),
                priority,
                note
        );
    }

    private ClassifyQuestionResponse mapClassifyQuestion(CauHoiSangLoc entity) {
        List<ClassifyQuestionResponse.OptionItem> options = List.of();
        if (entity.getLuaChonJson() != null && !entity.getLuaChonJson().isBlank()) {
            try {
                options = objectMapper.readValue(
                        entity.getLuaChonJson(),
                        new TypeReference<List<ClassifyQuestionResponse.OptionItem>>() {}
                );
            } catch (Exception ignored) {
                options = List.of();
            }
        }
        return ClassifyQuestionResponse.builder()
                .questionId(entity.getMaCauHoiSangLoc())
                .questionCode(entity.getMaCode())
                .questionText(entity.getNoiDungCauHoi())
                .questionType(entity.getLoaiCauHoi())
                .options(options)
                .isRequired(entity.getBatBuoc())
                .displayOrder(entity.getThuTuHienThi())
                .build();
    }

    private List<ClassifyQuestionResponse> defaultClassifyQuestions() {
        return List.of(
                ClassifyQuestionResponse.builder()
                        .questionId(1L)
                        .questionCode("SYMPTOM_DURATION_WEEKS")
                        .questionText("Các sẩn phù hoặc phù mạch của bạn đã kéo dài từ 6 tuần trở lên chưa?")
                        .questionType("single_choice")
                        .options(List.of(
                                new ClassifyQuestionResponse.OptionItem("UNDER_6_WEEKS", "Dưới 6 tuần"),
                                new ClassifyQuestionResponse.OptionItem("OVER_6_WEEKS", "Từ 6 tuần trở lên")
                        ))
                        .isRequired(true)
                        .displayOrder(1)
                        .build(),
                ClassifyQuestionResponse.builder()
                        .questionId(2L)
                        .questionCode("PREVIOUS_VISIT")
                        .questionText("Bạn khám lần đầu hay tái khám theo dõi mày đay?")
                        .questionType("single_choice")
                        .options(List.of(
                                new ClassifyQuestionResponse.OptionItem("FIRST_VISIT", "Khám lần đầu"),
                                new ClassifyQuestionResponse.OptionItem("FOLLOW_UP", "Tái khám theo dõi")
                        ))
                        .isRequired(true)
                        .displayOrder(2)
                        .build(),
                ClassifyQuestionResponse.builder()
                        .questionId(3L)
                        .questionCode("ANGIOEDEMA_DANGER_SIGNS")
                        .questionText("Bạn có kèm sưng môi, mí mắt, lưỡi hoặc khó thở, nghẹn họng không?")
                        .questionType("single_choice")
                        .options(List.of(
                                new ClassifyQuestionResponse.OptionItem("NONE", "Không có"),
                                new ClassifyQuestionResponse.OptionItem("SWELLING_ONLY", "Chỉ sưng môi / mí mắt"),
                                new ClassifyQuestionResponse.OptionItem("BREATHING_DIFFICULTY", "Có khó thở / nghẹn họng")
                        ))
                        .isRequired(true)
                        .displayOrder(3)
                        .build()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw MedicalRecordException.badRequest("1003", "Không thể mã hóa dữ liệu câu trả lời sàng lọc");
        }
    }

    private void validatePositiveId(Long id, String label) {
        if (id == null || id <= 0) {
            throw MedicalRecordException.badRequest("1004", label + " phải là số nguyên dương");
        }
    }

    private void validatePagination(int page, int limit) {
        if (page <= 0) {
            throw MedicalRecordException.badRequest("1004", "Tham số page phải lớn hơn hoặc bằng 1");
        }
        if (limit <= 0 || limit > 100) {
            throw MedicalRecordException.badRequest("1004", "Tham số limit phải nằm trong khoảng từ 1 đến 100");
        }
    }

    private String cleanNullable(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private record ClassificationOutcome(
            String classifiedType,
            String templateCode,
            String priorityLevel,
            String note
    ) {}
}
