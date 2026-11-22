package com.cnw.medical_system.controller;

import com.cnw.medical_system.repository.TaiKhoanRepository;
import com.cnw.medical_system.service.MaydayAuthService;
import com.cnw.medical_system.service.MaydayApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Profile("maydaytest")
@RequestMapping("/_mayday_test")
@RequiredArgsConstructor
public class MaydayTestAdapterController {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final TaiKhoanRepository accounts;
    @Value("${mayday.test-adapter-token:}")
    private String adapterToken;

    @PostMapping("/{operation}")
    public Map<String, Object> operation(
            @PathVariable String operation,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Mayday-Test-Adapter", required = false) String token,
            HttpServletRequest request) throws Exception {
        authorize(token, request);
        Map<String, Object> input = body == null ? Map.of() : body;
        return switch (operation) {
            case "assert-database" -> assertDatabase(input);
            case "roles" -> roleIds();
            case "promote-user" -> promoteUser(input);
            case "delete-user" -> deleteUser(input);
            case "expired-token" -> expiredToken(input);
            case "prepare-week7" -> Map.of("fixtureIds", List.of("w7-" + UUID.randomUUID()), "template", Map.of("id", 1));
            case "cleanup-week7", "reset-hooks" -> Map.of("cleaned", true, "reset", true);
            case "prepare-photo" -> preparePhoto(input);
            case "cleanup-photos" -> cleanupPhotos(input);
            default -> throw new MaydayApiException("9992", "Unknown test adapter operation");
        };
    }

    private Map<String, Object> assertDatabase(Map<String, Object> input) throws Exception {
        String expected = text(input, "expectedDatabaseName");
        return inspectDatabase(expected);
    }

    private Map<String, Object> inspectDatabase(String expected) throws Exception {
        try (var connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            String product = connection.getMetaData().getDatabaseProductName();
            String name;
            if (url.startsWith("jdbc:h2:mem:")) name = url.substring("jdbc:h2:mem:".length()).split(";", 2)[0];
            else if (url.startsWith("jdbc:postgresql:")) name = url.substring(url.lastIndexOf('/') + 1).split("\\?", 2)[0];
            else name = "";
            boolean valid = expected.equals(name) && expected.toLowerCase().contains("test") &&
                    (product.equalsIgnoreCase("H2") || product.toLowerCase().contains("postgresql"));
            return Map.of("isTestDatabase", valid, "databaseName", name, "databaseProduct", product);
        }
    }

    private Map<String, Object> roleIds() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("admin", roleId("admin", "director", "quan_tri_vien"));
        result.put("patient", roleId("patient", "benh_nhan"));
        result.put("doctor", roleId("doctor", "bac_si"));
        result.put("nurse", roleId("nurse", "dieu_duong"));
        return result;
    }

    private Integer roleId(String... names) {
        for (String name : names) {
            List<Integer> rows = jdbc.query("SELECT ma_vai_tro FROM vaitro WHERE LOWER(ten_vai_tro) = LOWER(?)",
                    (rs, row) -> rs.getInt(1), name);
            if (!rows.isEmpty()) return rows.get(0);
        }
        return null;
    }

    private Map<String, Object> promoteUser(Map<String, Object> input) {
        String phone = text(input, "phone");
        long roleId = number(input, "roleId");
        int changed = jdbc.update("UPDATE taikhoan SET ma_vai_tro = ? WHERE ma_nguoi = " +
                "(SELECT ma_nguoi FROM nguoi WHERE so_dien_thoai = ?)", roleId, phone);
        if (changed != 1) throw new MaydayApiException("9992", "Test user not found for promotion");
        return Map.of("phone", phone, "roleId", roleId);
    }

    private Map<String, Object> deleteUser(Map<String, Object> input) {
        String phone = text(input, "phone");
        List<Long> people = jdbc.query("SELECT ma_nguoi FROM nguoi WHERE so_dien_thoai = ?",
                (rs, row) -> rs.getLong(1), phone);
        if (people.isEmpty()) return Map.of("deleted", true);
        long personId = people.get(0);
        List<Long> records = jdbc.query("SELECT ma_ho_so FROM hosobenhan WHERE ma_benh_nhan = ?",
                (rs, row) -> rs.getLong(1), personId);
        for (long recordId : records) {
            jdbc.update("DELETE FROM anhchuptonthuong WHERE ma_dot_ton_thuong_da IN " +
                    "(SELECT ma_dot_ton_thuong_da FROM dottonthuongda WHERE ma_ho_so_man = ?)", recordId);
            jdbc.update("DELETE FROM anhchuptonthuong WHERE ma_vi_tri_ton_thuong IN " +
                    "(SELECT v.ma_vi_tri_ton_thuong FROM vitritonthuong v JOIN dottonthuongda e " +
                    "ON e.ma_dot_ton_thuong_da = v.ma_dot_ton_thuong_da WHERE e.ma_ho_so_man = ?)", recordId);
            jdbc.update("DELETE FROM vitritonthuong WHERE ma_dot_ton_thuong_da IN " +
                    "(SELECT ma_dot_ton_thuong_da FROM dottonthuongda WHERE ma_ho_so_man = ?)", recordId);
            jdbc.update("DELETE FROM dottonthuongda WHERE ma_ho_so_man = ?", recordId);
            jdbc.update("DELETE FROM tepdinhkem WHERE ma_ho_so = ?", recordId);
            jdbc.update("DELETE FROM benhanmaydayman WHERE ma_ho_so = ?", recordId);
            jdbc.update("DELETE FROM hosobenhan WHERE ma_ho_so = ?", recordId);
        }
        jdbc.update("DELETE FROM taikhoan WHERE ma_nguoi = ?", personId);
        jdbc.update("DELETE FROM benhnhan WHERE ma_benh_nhan = ?", personId);
        jdbc.update("DELETE FROM nguoi WHERE ma_nguoi = ?", personId);
        return Map.of("deleted", true);
    }

    private Map<String, Object> expiredToken(Map<String, Object> input) {
        String phone = text(input, "phone");
        var account = accounts.findByNguoi_SoDienThoai(phone)
                .orElseThrow(() -> new MaydayApiException("9992", "Test user not found"));
        String token = MaydayAuthService.issueToken(account, java.time.Instant.now().minusSeconds(600));
        return Map.of("token", token);
    }

    private Map<String, Object> preparePhoto(Map<String, Object> input) {
        long recordId = number(input, "medicalRecordId");
        long locationId = number(input, "locationId");
        long episodeId = number(input, "episodeId");
        long patientId = number(input, "patientId");
        long accountId;
        try {
            accountId = jdbc.queryForObject("SELECT ma_tai_khoan FROM taikhoan WHERE ma_nguoi = ?", Long.class, patientId);
        } catch (EmptyResultDataAccessException exception) {
            throw new MaydayApiException("9992", "Photo fixture account not found");
        }
        String fileUrl = "https://mayday-test.invalid/" + UUID.randomUUID() + ".jpg";
        String fileName = "mayday-test-" + UUID.randomUUID() + ".jpg";
        KeyHolder attachmentKey = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO tepdinhkem (ma_ho_so, ma_tai_khoan, ten_tep_goc, duong_dan_luu_tru,
                        duong_dan_url, dinh_dang_tep, loai_phuong_tien, dung_luong_byte, phan_loai_muc_dich)
                    VALUES (?, ?, ?, ?, ?, 'image/jpeg', 'image', 1, 'lesion_photo')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, recordId);
            statement.setLong(2, accountId);
            statement.setString(3, fileName);
            statement.setString(4, fileUrl);
            statement.setString(5, fileUrl);
            return statement;
        }, attachmentKey);
        Number attachmentId = generatedId(attachmentKey, "ma_tep_dinh_kem");
        KeyHolder photoKey = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO anhchuptonthuong (ma_tep_dinh_kem, ma_dot_ton_thuong_da,
                        ma_vi_tri_ton_thuong, thoi_diem_chup, url_anh)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, attachmentId.longValue());
            statement.setLong(2, episodeId);
            statement.setLong(3, locationId);
            statement.setObject(4, OffsetDateTime.now());
            statement.setString(5, fileUrl);
            return statement;
        }, photoKey);
        Number photoId = generatedId(photoKey, "ma_anh");
        return Map.of("fixtureId", "w8-photo-" + photoId.longValue() + "-" + attachmentId.longValue(),
                "attachmentId", attachmentId.longValue(), "fileUrl", fileUrl);
    }

    private Map<String, Object> cleanupPhotos(Map<String, Object> input) {
        Object rawIds = input.get("fixtureIds");
        if (rawIds instanceof List<?> ids) {
            for (Object raw : ids) {
                if (!(raw instanceof String id) || !id.matches("w8-photo-[0-9]+-[0-9]+")) continue;
                String[] parts = id.split("-");
                long photoId = Long.parseLong(parts[2]);
                long attachmentId = Long.parseLong(parts[3]);
                jdbc.update("DELETE FROM anhchuptonthuong WHERE ma_anh = ?", photoId);
                jdbc.update("DELETE FROM tepdinhkem WHERE ma_tep_dinh_kem = ?", attachmentId);
            }
        }
        return Map.of("cleaned", true);
    }

    private void authorize(String token, HttpServletRequest request) {
        boolean loopback = request.getRemoteAddr().equals("127.0.0.1") || request.getRemoteAddr().equals("::1") || request.getRemoteAddr().equals("0:0:0:0:0:0:0:1");
        if (adapterToken == null || adapterToken.length() < 24 || !adapterToken.equals(token) || !loopback) {
            throw new MaydayApiException("9998", "Test adapter access denied");
        }
    }

    private Number generatedId(KeyHolder keyHolder, String columnName) {
        if (keyHolder.getKeys() != null) {
            Object exact = keyHolder.getKeys().get(columnName);
            if (exact == null) exact = keyHolder.getKeys().get(columnName.toUpperCase());
            if (exact instanceof Number number) return number;
        }
        if (keyHolder.getKey() != null) return keyHolder.getKey();
        throw new MaydayApiException("1010", "Database did not return a generated ID");
    }

    private String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) throw new MaydayApiException("1002", "Missing test adapter argument " + key);
        return String.valueOf(value);
    }

    private long number(Map<String, Object> values, String key) {
        try { return Long.parseLong(text(values, key)); }
        catch (NumberFormatException exception) { throw new MaydayApiException("1003", "Invalid test adapter argument " + key); }
    }
}
