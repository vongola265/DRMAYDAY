package com.cnw.medical_system.service;

import com.cnw.medical_system.dto.response.MaydayAttachmentResponse;
import com.cnw.medical_system.entity.TaiKhoan;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaydayAttachmentService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final MaydayAuthService auth;
    private final MaydayRecordService records;

    @Value("${mayday.storage.path:target/mayday-uploads}")
    private String storageDirectory;

    @Transactional
    public MaydayAttachmentResponse upload(TaiKhoan actor, MultipartFile file, String recordId,
                                           String questionId, String category, String description,
                                           String requestedMediaType, String userId) {
        validateFile(file);
        if (requestedMediaType != null && !requestedMediaType.isBlank() && !"image".equalsIgnoreCase(requestedMediaType)) {
            throw new MaydayApiException("1004", "media_type không hợp lệ");
        }
        if (storageFailureEnabled()) {
            throw new MaydayApiException("1007", "Dịch vụ lưu trữ tệp hiện không khả dụng");
        }
        validateText(category, 50, "category");
        validateText(description, 255, "description");
        Long medicalRecordId = recordId == null || recordId.isBlank() ? null : parsePositiveId(recordId, "medical_record_id");
        if (medicalRecordId != null) {
            List<Long> owners = jdbc.query("SELECT ma_benh_nhan FROM hosobenhan WHERE ma_ho_so = ?",
                    (rs, row) -> rs.getLong(1), medicalRecordId);
            if (owners.isEmpty()) throw new MaydayApiException("9992", "Không tìm thấy hồ sơ bệnh án");
            if (!auth.isStaff(actor) && !owners.get(0).equals(actor.getNguoi().getMaNguoi())) {
                throw new MaydayApiException("1009", "Không thể tải tệp cho hồ sơ bệnh án của người khác");
            }
            records.get(medicalRecordId, actor);
        }
        Long parsedQuestionId = questionId == null || questionId.isBlank() ? null : parsePositiveId(questionId, "question_id");
        if (parsedQuestionId != null && questionIsKnown(parsedQuestionId) == Boolean.FALSE) {
            throw new MaydayApiException("9992", "Không tìm thấy câu hỏi");
        }

        String filename = Path.of(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename())
                .getFileName().toString();
        String contentType = file.getContentType();
        String extension = extension(filename);
        String relativeFile = UUID.randomUUID() + "." + extension;
        Path stored = Path.of(storageDirectory).toAbsolutePath().normalize().resolve(relativeFile);
        try {
            Files.createDirectories(stored.getParent());
            file.transferTo(stored);
        } catch (IOException exception) {
            throw new MaydayApiException("1010", "Không thể lưu tệp hình ảnh");
        }

        try {
            KeyHolder keys = new GeneratedKeyHolder();
            String sql = "INSERT INTO tepdinhkem (ma_ho_so, ma_tai_khoan, ma_cau_hoi, ten_tep_goc, " +
                    "duong_dan_luu_tru, duong_dan_url, dinh_dang_tep, loai_phuong_tien, dung_luong_byte, " +
                    "phan_loai_muc_dich, mo_ta_tep) VALUES (?, ?, ?, ?, ?, ?, ?, 'image', ?, ?, ?)";
            jdbc.update(connection -> {
                var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                if (medicalRecordId == null) statement.setNull(1, java.sql.Types.BIGINT); else statement.setLong(1, medicalRecordId);
                statement.setLong(2, actor.getMaTaiKhoan());
                if (parsedQuestionId == null) statement.setNull(3, java.sql.Types.BIGINT); else statement.setLong(3, parsedQuestionId);
                statement.setString(4, filename);
                statement.setString(5, stored.toString());
                statement.setString(6, "pending");
                statement.setString(7, contentType);
                statement.setLong(8, file.getSize());
                statement.setString(9, emptyToNull(category));
                statement.setString(10, emptyToNull(description));
                return statement;
            }, keys);
            Number key = keys.getKeys() == null ? null : (Number) keys.getKeys().get("ma_tep_dinh_kem");
            if (key == null) throw new MaydayApiException("1010", "Không tạo được metadata tệp");
            long id = key.longValue();
            String fileUrl = "/api/image-attachments/view/" + id;
            jdbc.update("UPDATE tepdinhkem SET duong_dan_url = ? WHERE ma_tep_dinh_kem = ?", fileUrl, id);
            return find(id, actor);
        } catch (RuntimeException exception) {
            try { Files.deleteIfExists(stored); } catch (IOException ignored) { }
            throw exception;
        }
    }

    public List<MaydayAttachmentResponse> listForUser(Long personId, TaiKhoan actor) {
        if (jdbc.queryForObject("SELECT count(*) FROM nguoi WHERE ma_nguoi = ?", Integer.class, personId) == 0) {
            throw new MaydayApiException("9992", "Không tìm thấy người dùng");
        }
        if (!auth.isStaff(actor) && !personId.equals(actor.getNguoi().getMaNguoi())) {
            throw new MaydayApiException("1009", "Không có quyền xem tệp của người dùng khác");
        }
        return jdbc.query("""
                SELECT t.*, p.ma_nguoi AS user_id
                FROM tepdinhkem t JOIN taikhoan a ON a.ma_tai_khoan = t.ma_tai_khoan
                JOIN nguoi p ON p.ma_nguoi = a.ma_nguoi
                WHERE p.ma_nguoi = ? ORDER BY t.thoi_diem_tao DESC, t.ma_tep_dinh_kem DESC
                """, (rs, row) -> mapAttachment(rs.getLong("ma_tep_dinh_kem"), rs.getObject("user_id"),
                rs.getObject("ma_ho_so"), longOrNull(rs.getObject("ma_cau_hoi")), rs.getString("ten_tep_goc"),
                rs.getString("dinh_dang_tep"), rs.getString("loai_phuong_tien"),
                rs.getString("phan_loai_muc_dich"), rs.getString("mo_ta_tep"), rs.getString("duong_dan_url"),
                rs.getObject("thoi_diem_tao")), personId);
    }

    public AttachmentFile file(Long id, TaiKhoan actor) {
        MaydayAttachmentResponse attachment = find(id, actor);
        String storedPath = jdbc.queryForObject("SELECT duong_dan_luu_tru FROM tepdinhkem WHERE ma_tep_dinh_kem = ?", String.class, id);
        try {
            Path path = Path.of(storedPath).toAbsolutePath().normalize();
            if (!path.startsWith(Path.of(storageDirectory).toAbsolutePath().normalize()) || !Files.isRegularFile(path)) {
                throw new MaydayApiException("9992", "Không tìm thấy nội dung tệp");
            }
            return new AttachmentFile(Files.readAllBytes(path), attachment.fileType());
        } catch (IOException exception) {
            throw new MaydayApiException("9992", "Không tìm thấy nội dung tệp");
        }
    }

    @Transactional
    public void delete(Long id, TaiKhoan actor) {
        MaydayAttachmentResponse attachment = find(id, actor);
        Long ownerId = Long.valueOf(attachment.userId());
        if (!auth.isAdmin(actor) && !ownerId.equals(actor.getNguoi().getMaNguoi())) {
            throw new MaydayApiException("1009", "Không có quyền xóa tệp này");
        }
        Integer references = jdbc.queryForObject("SELECT (SELECT count(*) FROM anhchuptonthuong WHERE ma_tep_dinh_kem = ?) + " +
                "(SELECT count(*) FROM mayday_answer_images WHERE attachment_id = ?)", Integer.class, id, id);
        if (references != null && references > 0) throw new MaydayApiException("1009", "Tệp đang được sử dụng");
        String path = jdbc.queryForObject("SELECT duong_dan_luu_tru FROM tepdinhkem WHERE ma_tep_dinh_kem = ?", String.class, id);
        jdbc.update("DELETE FROM tepdinhkem WHERE ma_tep_dinh_kem = ?", id);
        try { Files.deleteIfExists(Path.of(path)); } catch (IOException ignored) { }
    }

    public MaydayAttachmentResponse find(Long id, TaiKhoan actor) {
        List<MaydayAttachmentResponse> rows = jdbc.query("""
                SELECT t.*, p.ma_nguoi AS user_id
                FROM tepdinhkem t LEFT JOIN taikhoan a ON a.ma_tai_khoan = t.ma_tai_khoan
                LEFT JOIN nguoi p ON p.ma_nguoi = a.ma_nguoi
                WHERE t.ma_tep_dinh_kem = ?
                """, (rs, row) -> mapAttachment(rs.getLong("ma_tep_dinh_kem"), rs.getObject("user_id"),
                rs.getObject("ma_ho_so"), longOrNull(rs.getObject("ma_cau_hoi")), rs.getString("ten_tep_goc"),
                rs.getString("dinh_dang_tep"), rs.getString("loai_phuong_tien"),
                rs.getString("phan_loai_muc_dich"), rs.getString("mo_ta_tep"), rs.getString("duong_dan_url"),
                rs.getObject("thoi_diem_tao")), id);
        if (rows.isEmpty()) throw new MaydayApiException("9992", "Không tìm thấy tệp đính kèm");
        MaydayAttachmentResponse result = rows.get(0);
        if (!auth.isStaff(actor) && !String.valueOf(actor.getNguoi().getMaNguoi()).equals(result.userId())) {
            throw new MaydayApiException("1009", "Không có quyền truy cập tệp");
        }
        return result;
    }

    private MaydayAttachmentResponse mapAttachment(Object id, Object userId, Object recordId, Long questionId,
                                                   String name, String type, String media, String category,
                                                   String description, String url, Object createdAt) {
        String idText = String.valueOf(id);
        return new MaydayAttachmentResponse(idText, idText, userId == null ? null : String.valueOf(userId),
                recordId == null ? null : String.valueOf(recordId), questionId,
                name, type, media, category, description, url == null || "pending".equals(url) ? "/api/image-attachments/view/" + idText : url,
                String.valueOf(createdAt));
    }

    private Long longOrNull(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private boolean storageFailureEnabled() {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT COALESCE((SELECT enabled FROM mayday_test_hooks WHERE hook_name='minio_failure'),FALSE)", Boolean.class));
        } catch (BadSqlGrammarException ignored) {
            return false;
        }
    }

    private Boolean questionIsKnown(Long questionId) {
        try {
            return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM mayday_test_questions WHERE question_id = ?)", Boolean.class, questionId);
        } catch (BadSqlGrammarException ignored) {
            return null;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new MaydayApiException("1002", "Thiếu tệp hình ảnh");
        if (file.getSize() > MAX_FILE_SIZE) throw new MaydayApiException("1006", "Tệp vượt quá 10 MB");
        String content = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String ext = extension(file.getOriginalFilename());
        if (!List.of("image/png", "image/jpeg", "image/webp").contains(content) ||
                !List.of("png", "jpg", "jpeg", "webp").contains(ext)) {
            throw new MaydayApiException("1003", "Định dạng tệp không được hỗ trợ");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private void validateText(String value, int max, String field) {
        if (value != null && (!value.isBlank() && (value.length() > max || value.matches("(?s).*<[^>]+>.*")))) {
            throw new MaydayApiException("1004", field + " không hợp lệ");
        }
    }

    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private long parsePositiveId(String value, String field) {
        if (!value.matches("-?[0-9]+")) throw new MaydayApiException("1003", field + " sai định dạng");
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new MaydayApiException("1004", field + " phải lớn hơn 0");
            return id;
        } catch (NumberFormatException exception) {
            throw new MaydayApiException("1004", field + " vượt giới hạn");
        }
    }

    public record AttachmentFile(byte[] bytes, String mediaType) { }
}
