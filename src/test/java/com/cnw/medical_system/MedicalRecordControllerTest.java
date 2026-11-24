package com.cnw.medical_system;

import com.cnw.medical_system.entity.Nguoi;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.entity.VaiTro;
import com.cnw.medical_system.repository.NguoiRepository;
import com.cnw.medical_system.repository.TaiKhoanRepository;
import com.cnw.medical_system.repository.VaiTroRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MedicalRecordControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    VaiTroRepository vaiTroRepository;

    @Autowired
    NguoiRepository nguoiRepository;

    @Autowired
    TaiKhoanRepository taiKhoanRepository;

    String patient1Token;
    Long patient1PersonId;
    String patient2Token;
    Long patient2PersonId;
    String doctorToken;
    Long doctorPersonId;

    @BeforeEach
    void setUpAccounts() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        VaiTro patientRole = vaiTroRepository.findByTenVaiTro("benh_nhan").orElseThrow();
        VaiTro doctorRole = vaiTroRepository.findByTenVaiTro("bac_si").orElseThrow();

        TaiKhoan p1 = ensureAccount("bn_quynhanh_1", "Nguyễn Văn Bệnh Nhân 1", "0912345601", patientRole, encoder);
        TaiKhoan p2 = ensureAccount("bn_quynhanh_2", "Trần Thị Bệnh Nhân 2", "0912345602", patientRole, encoder);
        TaiKhoan doc = ensureAccount("bs_quynhanh_1", "BS. Lê Chuyên Khoa", "0987654301", doctorRole, encoder);

        patient1PersonId = p1.getNguoi().getMaNguoi();
        patient2PersonId = p2.getNguoi().getMaNguoi();
        doctorPersonId = doc.getNguoi().getMaNguoi();

        patient1Token = loginAndGetToken("bn_quynhanh_1", "123456");
        patient2Token = loginAndGetToken("bn_quynhanh_2", "123456");
        doctorToken = loginAndGetToken("bs_quynhanh_1", "123456");
    }

    @Test
    void fullEndToEndWorkflowAcrossAll13Apis() throws Exception {
        // 1. Unauthenticated request must be rejected
        mockMvc.perform(get("/medical-records/classify-questions"))
                .andExpect(status().isForbidden());

        // 1. API get_medicalrecords_classifyquestions
        mockMvc.perform(get("/medical-records/classify-questions")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.data[0].question_code").value("SYMPTOM_DURATION_WEEKS"));

        // 2. API set_medicalrecords_classify (Validation failure + Happy case -> MAN + urgent)
        mockMvc.perform(post("/medical-records/classify")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1002"));

        MvcResult classifyResult = mockMvc.perform(post("/medical-records/classify")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"question_code": "SYMPTOM_DURATION_WEEKS", "answer_value": "OVER_6_WEEKS"},
                                    {"question_code": "PREVIOUS_VISIT", "answer_value": "FIRST_VISIT"},
                                    {"question_code": "ANGIOEDEMA_DANGER_SIGNS", "answer_value": "BREATHING_DIFFICULTY"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classified_type").value("MAN"))
                .andExpect(jsonPath("$.recommended_template_code").value("TMPL_MAYDAY_MAN"))
                .andExpect(jsonPath("$.priority_level").value("urgent"))
                .andReturn();
        long classifyId = readJson(classifyResult).get("classify_id").asLong();

        // 3. API set_medicalrecords_selfcreate (Doctor forbidden, Patient succeeds)
        mockMvc.perform(post("/medical-records/self-create")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chief_complaint\":\"Bác sĩ gọi nhầm endpoint\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1009"));

        MvcResult selfRecordResult = mockMvc.perform(post("/medical-records/self-create")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classify_id": %d,
                                  "chief_complaint": "Nổi sẩn phù ngứa toàn thân kéo dài 8 tuần",
                                  "notes": "Ngứa tăng nhiều về đêm"
                                }
                                """.formatted(classifyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.record_type").value("MAN"))
                .andExpect(jsonPath("$.template_code").value("TMPL_MAYDAY_MAN"))
                .andReturn();
        long recordId = readJson(selfRecordResult).get("medical_record_id").asLong();

        // 11. API edit_medicalrecords (Patient updates complaint while pending, blocked from setting diagnosis)
        mockMvc.perform(put("/medical-records/{id}", recordId)
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"final_diagnosis\":\"Tự chẩn đoán\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/medical-records/{id}", recordId)
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chief_complaint\":\"Nổi sẩn phù toàn thân 8 tuần kèm sưng môi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chief_complaint").value("Nổi sẩn phù toàn thân 8 tuần kèm sưng môi"));

        // 9. API set_medicalrecords_answers (Patient saves dynamic form answers; duplicate blocked)
        MvcResult savePatientAnswers = mockMvc.perform(post("/medical-records/{id}/answers", recordId)
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "section_id": "SEC_BENH_SU",
                                      "question_id": "Q_ITCH_SCORE",
                                      "answer_value": "8",
                                      "data_type": "number",
                                      "note": "Ngứa dữ dội"
                                    },
                                    {
                                      "section_id": "SEC_BENH_SU",
                                      "question_id": "Q_WHEAL_COUNT",
                                      "answer_value": ">50 nốt/ngày",
                                      "data_type": "radio"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.data[0].filled_by_role").value("patient"))
                .andReturn();
        long firstAnswerId = readJson(savePatientAnswers).get("data").get(0).get("answer_id").asLong();

        // Duplicate question_id on POST /answers returns 409 Conflict
        mockMvc.perform(post("/medical-records/{id}/answers", recordId)
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"question_id": "Q_ITCH_SCORE", "answer_value": "9"}
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("9996"));

        // 12. API edit_status_medicalrecords: invalid jump pending -> done blocked; patient blocked; pending -> in_progress succeeds
        mockMvc.perform(patch("/medical-records/{id}/status", recordId)
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"in_progress\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/medical-records/{id}/status", recordId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("1010"));

        mockMvc.perform(patch("/medical-records/{id}/status", recordId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"in_progress\",\"note\":\"Bác sĩ bắt đầu khám\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.doctor_id").value(doctorPersonId));

        // in_progress -> done blocked before clinical exam / diagnosis is entered
        mockMvc.perform(patch("/medical-records/{id}/status", recordId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1004"));

        // Doctor saves clinical answers & updates diagnosis
        mockMvc.perform(post("/medical-records/{id}/answers", recordId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "section_id": "SEC_KHAM_LAM_SANG",
                                      "question_id": "Q_DERMOGRAPHISM",
                                      "answer_value": "Dương tính",
                                      "data_type": "radio"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));

        // 8. API get_medicalrecords_answers (filter by filled_by=doctor)
        mockMvc.perform(get("/medical-records/{id}/answers", recordId)
                        .param("filled_by", "doctor")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].question_id").value("Q_DERMOGRAPHISM"));

        // 10. API edit_medicalrecords_answers: patient edits own answer OK, blocked from editing doctor's answer
        mockMvc.perform(put("/medical-records/{id}/answers/{answerId}", recordId, firstAnswerId)
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer_value\":\"9\",\"note\":\"Cập nhật mức độ ngứa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer_value").value("9"));

        mockMvc.perform(put("/medical-records/{id}/answers", recordId)
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"question_id": "Q_DERMOGRAPHISM", "answer_value": "Âm tính"}
                                  ]
                                }
                                """))
                .andExpect(status().isForbidden());

        // 11. Doctor updates diagnosis & treatment plan
        String followUp = LocalDate.now().plusDays(14).toString();
        mockMvc.perform(put("/medical-records/{id}", recordId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "room_id": 101,
                                  "preliminary_diagnosis": "Mày đay mạn tính tự phát",
                                  "final_diagnosis": "Mày đay mạn tính tự phát mức độ nặng kèm phù mạch (L50.1)",
                                  "treatment_plan": "Bilastine 20mg x 2 viên/ngày, theo dõi UAS7 hàng ngày",
                                  "follow_up_date": "%s"
                                }
                                """.formatted(followUp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.final_diagnosis")
                        .value("Mày đay mạn tính tự phát mức độ nặng kèm phù mạch (L50.1)"));

        // 7. API delete_medicalrecords must fail on in_progress record with exam data
        mockMvc.perform(delete("/medical-records/{id}", recordId)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("1010"));

        // 12. Transition in_progress -> done succeeds now
        mockMvc.perform(patch("/medical-records/{id}/status", recordId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"done\",\"note\":\"Hoàn tất buổi khám\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.completed_at").isNotEmpty());

        // Editing closed (done) record must be blocked
        mockMvc.perform(put("/medical-records/{id}", recordId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Sửa sau khi đóng\"}"))
                .andExpect(status().isConflict());

        // 6. API get_medicalrecords_detail (owner & doctor succeed, other patient forbidden)
        mockMvc.perform(get("/medical-records/{id}", recordId)
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer_count").value(3));

        mockMvc.perform(get("/medical-records/{id}", recordId)
                        .header("Authorization", "Bearer " + patient2Token))
                .andExpect(status().isForbidden());

        // 4. API set_medicalrecords_by_doctor + 7. API delete_medicalrecords (on pending clean record)
        MvcResult doctorPendingRecord = mockMvc.perform(post("/medical-records/doctor-create")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patient_id": %d,
                                  "record_type": "TAI_KHAM",
                                  "status": "pending",
                                  "chief_complaint": "Tái khám sau 2 tuần điều trị kháng Histamin"
                                }
                                """.formatted(patient1PersonId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.record_type").value("TAI_KHAM"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andReturn();
        long cancellableRecordId = readJson(doctorPendingRecord).get("medical_record_id").asLong();

        // 5. API get_medicalrecords_list (Patient sees own 2 records; blocked from querying patient2)
        mockMvc.perform(get("/medical-records")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(get("/medical-records")
                        .param("patient_id", String.valueOf(patient2PersonId))
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isForbidden());

        // 7. Cancel/delete the clean pending record
        mockMvc.perform(delete("/medical-records/{id}", cancellableRecordId)
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        // 13. API get_history (Patient & Doctor retrieve medical history + previous answers)
        mockMvc.perform(get("/medical-records/history")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_visits").value(1))
                .andExpect(jsonPath("$.completed_visits").value(1))
                .andExpect(jsonPath("$.chronic_visits").value(1))
                .andExpect(jsonPath("$.visits[0].answers.length()").value(3));

        mockMvc.perform(get("/medical-records/patients/{patientId}/history", patient1PersonId)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient_id").value(patient1PersonId))
                .andExpect(jsonPath("$.visits[0].final_diagnosis")
                        .value("Mày đay mạn tính tự phát mức độ nặng kèm phù mạch (L50.1)"));
    }

    private TaiKhoan ensureAccount(
            String username,
            String fullName,
            String phone,
            VaiTro role,
            BCryptPasswordEncoder encoder) {
        return taiKhoanRepository.findByTenDangNhap(username).orElseGet(() -> {
            Nguoi nguoi = nguoiRepository.save(Nguoi.builder()
                    .hoTen(fullName)
                    .soDienThoai(phone)
                    .build());
            return taiKhoanRepository.save(TaiKhoan.builder()
                    .nguoi(nguoi)
                    .vaiTro(role)
                    .tenDangNhap(username)
                    .matKhauMaHoa(encoder.encode("123456"))
                    .trangThaiHoatDong(true)
                    .build());
        });
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenDangNhap":"%s","matKhau":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(loginResult).get("token").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
