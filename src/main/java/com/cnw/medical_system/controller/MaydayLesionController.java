package com.cnw.medical_system.controller;

import com.cnw.medical_system.dto.request.LesionEpisodeRequest;
import com.cnw.medical_system.dto.request.LesionLocationRequest;
import com.cnw.medical_system.dto.response.LesionEpisodeResponse;
import com.cnw.medical_system.dto.response.LesionLocationResponse;
import com.cnw.medical_system.entity.TaiKhoan;
import com.cnw.medical_system.service.LesionService;
import com.cnw.medical_system.service.MaydayApiException;
import com.cnw.medical_system.service.MaydayApiResponse;
import com.cnw.medical_system.service.MaydayAuthService;
import com.cnw.medical_system.service.MaydayRecordService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MaydayLesionController {
    private static final DateTimeFormatter INPUT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUTPUT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final MaydayAuthService auth;
    private final MaydayRecordService records;
    private final LesionService lesions;

    @GetMapping("/api/lesion-episodes")
    public ResponseEntity<Map<String, Object>> listEpisodes(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        List<LesionEpisodeResponse> episodes = auth.isStaff(actor)
                ? lesions.listEpisodes()
                : lesions.listEpisodesForPatient(actor.getNguoi().getMaNguoi());
        return ResponseEntity.ok(MaydayApiResponse.success(episodes.stream().map(this::episodeData).toList()));
    }

    @PostMapping("/api/lesion-episodes")
    public ResponseEntity<Map<String, Object>> createEpisode(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        TaiKhoan actor = auth.requireAccount(authorization);
        long recordId = requiredId(body, "medical_record_id");
        Map<String, Object> record = records.get(recordId, actor);
        assertRecordEditable(record);
        long patientId = number(record.get("patient_id"));
        assertCanAccess(patientId, actor);
        String name = requiredText(body, "name");
        if (name.length() > 100 || containsMarkup(name)) throw invalid("name không hợp lệ");

        LesionEpisodeRequest request = new LesionEpisodeRequest();
        request.setPatientId(patientId);
        request.setMedicalRecordId(recordId);
        request.setName(name);
        readEpisodeFields(body, request, false);
        return ResponseEntity.ok(MaydayApiResponse.success(episodeData(lesions.createEpisode(request))));
    }

    @GetMapping("/api/lesion-episodes/my")
    public ResponseEntity<Map<String, Object>> getMyEpisodes(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "medical_record_id", required = false) String medicalRecordId) {
        TaiKhoan actor = auth.requireAccount(authorization);
        List<LesionEpisodeResponse> episodes = lesions.listEpisodesForPatient(actor.getNguoi().getMaNguoi());
        if (medicalRecordId != null) {
            long recordId = parsePositiveQueryId(medicalRecordId);
            episodes = episodes.stream().filter(episode -> episode.getMedicalRecordId() != null && episode.getMedicalRecordId() == recordId).toList();
        }
        return ResponseEntity.ok(MaydayApiResponse.success(episodes.stream().map(this::episodeData).toList()));
    }

    @GetMapping("/api/lesion-episodes/record/{recordId}")
    public ResponseEntity<Map<String, Object>> getEpisodesByRecord(
            @PathVariable String recordId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        long id = parseId(recordId);
        Map<String, Object> record = records.get(id, actor);
        assertRecordEditable(record);
        assertCanAccess(number(record.get("patient_id")), actor);
        return ResponseEntity.ok(MaydayApiResponse.success(lesions.listEpisodesForRecord(id).stream().map(this::episodeData).toList()));
    }

    @GetMapping("/api/lesion-episodes/{id}")
    public ResponseEntity<Map<String, Object>> getEpisode(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        LesionEpisodeResponse episode = lesions.getEpisode(parseId(id));
        assertCanAccess(episode.getPatientId(), actor);
        return ResponseEntity.ok(MaydayApiResponse.success(episodeData(episode)));
    }

    @PatchMapping("/api/lesion-episodes/{id}")
    public ResponseEntity<Map<String, Object>> patchEpisode(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        TaiKhoan actor = auth.requireAccount(authorization);
        LesionEpisodeResponse current = lesions.getEpisode(parseId(id));
        assertCanAccess(current.getPatientId(), actor);
        assertRecordEditable(records.get(current.getMedicalRecordId(), actor));
        if (body.isEmpty()) throw new MaydayApiException("1002", "Thiếu trường cần cập nhật");
        LesionEpisodeRequest request = new LesionEpisodeRequest();
        request.setPatientId(current.getPatientId());
        request.setMedicalRecordId(current.getMedicalRecordId());
        request.setName(current.getName());
        request.setOccurredAt(current.getOccurredAt());
        request.setCircumstance(current.getCircumstance());
        request.setOtherCircumstance(current.getOtherCircumstance());
        request.setMedication(current.getMedication());
        request.setMedicationDuration(current.getMedicationDuration());
        request.setTimeUnit(current.getTimeUnit());
        request.setDetails(current.getDetails());
        if (body.containsKey("name")) {
            String name = requiredText(body, "name");
            if (name.length() > 100 || containsMarkup(name)) throw invalid("name không hợp lệ");
            request.setName(name);
        }
        readEpisodeFields(body, request, true);
        return ResponseEntity.ok(MaydayApiResponse.success(episodeData(lesions.updateEpisode(parseId(id), request))));
    }

    @DeleteMapping("/api/lesion-episodes/{id}")
    public ResponseEntity<Map<String, Object>> deleteEpisode(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        LesionEpisodeResponse episode = lesions.getEpisode(parseId(id));
        assertCanAccess(episode.getPatientId(), actor);
        lesions.deleteEpisode(episode.getId());
        return ResponseEntity.ok(MaydayApiResponse.success(Map.of("deleted", true)));
    }

    @PostMapping("/api/lesion-locations")
    public ResponseEntity<Map<String, Object>> createLocation(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        TaiKhoan actor = auth.requireAccount(authorization);
        long episodeId = requiredId(body, "episode_id");
        LesionEpisodeResponse episode = lesions.getEpisode(episodeId);
        assertCanAccess(episode.getPatientId(), actor);
        assertRecordEditable(records.get(episode.getMedicalRecordId(), actor));
        String bodyPart = requiredText(body, "body_part");
        if (bodyPart.length() > 100 || containsMarkup(bodyPart)) throw invalid("body_part không hợp lệ");

        LesionLocationRequest request = new LesionLocationRequest();
        request.setEpisodeId(episodeId);
        request.setBodyPart(bodyPart);
        readLocationFields(body, request);
        return ResponseEntity.ok(MaydayApiResponse.success(locationData(lesions.createLocation(request))));
    }

    @GetMapping("/api/lesion-locations/episode/{episodeId}")
    public ResponseEntity<Map<String, Object>> getLocations(
            @PathVariable String episodeId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        LesionEpisodeResponse episode = lesions.getEpisode(parseId(episodeId));
        assertCanAccess(episode.getPatientId(), actor);
        List<Map<String, Object>> data = lesions.listLocations(episode.getId()).stream().map(this::locationData).toList();
        return ResponseEntity.ok(MaydayApiResponse.success(data));
    }

    @PatchMapping("/api/lesion-locations/{id}")
    public ResponseEntity<Map<String, Object>> patchLocation(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        TaiKhoan actor = auth.requireAccount(authorization);
        LesionLocationResponse current = lesions.getLocation(parseId(id));
        LesionEpisodeResponse parent = lesions.getEpisode(current.getEpisodeId());
        assertCanAccess(parent.getPatientId(), actor);
        assertRecordEditable(records.get(parent.getMedicalRecordId(), actor));
        if (body.isEmpty()) throw new MaydayApiException("1002", "Thiếu trường cần cập nhật");

        LesionLocationRequest request = new LesionLocationRequest();
        request.setEpisodeId(current.getEpisodeId());
        request.setBodyPart(current.getBodyPart());
        request.setOccurredAt(current.getOccurredAt());
        request.setRelatedMedication(current.getRelatedMedication());
        request.setMedicationDuration(current.getMedicationDuration());
        request.setCircumstance(current.getCircumstance());
        request.setOtherCircumstance(current.getOtherCircumstance());
        request.setTimeUnit(current.getTimeUnit());
        if (body.containsKey("body_part")) {
            String bodyPart = requiredText(body, "body_part");
            if (bodyPart.length() > 100 || containsMarkup(bodyPart)) throw invalid("body_part không hợp lệ");
            request.setBodyPart(bodyPart);
        }
        readLocationFields(body, request);
        return ResponseEntity.ok(MaydayApiResponse.success(locationData(lesions.updateLocation(parseId(id), request))));
    }

    @DeleteMapping("/api/lesion-locations/{id}")
    public ResponseEntity<Map<String, Object>> deleteLocation(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TaiKhoan actor = auth.requireAccount(authorization);
        LesionLocationResponse location = lesions.getLocation(parseId(id));
        assertCanAccess(lesions.getEpisode(location.getEpisodeId()).getPatientId(), actor);
        lesions.deleteLocation(location.getId());
        return ResponseEntity.ok(MaydayApiResponse.success(Map.of("deleted", true)));
    }

    private void readEpisodeFields(Map<String, Object> body, LesionEpisodeRequest request, boolean partial) {
        readCommonFields(body, request::setOccurredAt, request::setCircumstance, request::setOtherCircumstance,
                request::setMedication, request::setMedicationDuration, request::setTimeUnit);
        if (body.containsKey("description")) request.setDetails(optionalText(body, "description", 1000));
    }

    private void readLocationFields(Map<String, Object> body, LesionLocationRequest request) {
        readCommonFields(body, request::setOccurredAt, request::setCircumstance, request::setOtherCircumstance,
                request::setRelatedMedication, request::setMedicationDuration, request::setTimeUnit);
    }

    private void readCommonFields(Map<String, Object> body,
                                  java.util.function.Consumer<OffsetDateTime> occurred,
                                  java.util.function.Consumer<String> tags,
                                  java.util.function.Consumer<String> contextOther,
                                  java.util.function.Consumer<String> medication,
                                  java.util.function.Consumer<String> ago,
                                  java.util.function.Consumer<String> unit) {
        if (body.containsKey("occurred_at")) occurred.accept(parseOccurredAt(body.get("occurred_at")));
        if (body.containsKey("context_tags")) tags.accept(validateTags(body.get("context_tags")));
        if (body.containsKey("context_other")) contextOther.accept(optionalText(body, "context_other", 255));
        if (body.containsKey("medication_name")) {
            String value = optionalText(body, "medication_name", 100);
            if (containsMarkup(value)) throw invalid("medication_name không hợp lệ");
            medication.accept(value);
        }
        if (body.containsKey("medication_taken_ago")) {
            Object raw = body.get("medication_taken_ago");
            if (!(raw instanceof String text)) throw new MaydayApiException("1003", "medication_taken_ago sai kiểu dữ liệu");
            if (!text.matches("[1-9][0-9]{0,3}")) throw invalid("medication_taken_ago không hợp lệ");
            ago.accept(text);
        }
        if (body.containsKey("medication_unit")) {
            String value = requiredText(body, "medication_unit");
            if (!List.of("minute", "hour", "day", "week", "month").contains(value)) throw invalid("medication_unit không hợp lệ");
            unit.accept(value);
        }
    }

    private OffsetDateTime parseOccurredAt(Object raw) {
        if (!(raw instanceof String text)) throw new MaydayApiException("1003", "occurred_at sai kiểu dữ liệu");
        try {
            LocalDateTime local = LocalDateTime.parse(text, INPUT_TIME);
            OffsetDateTime parsed = local.atOffset(ZoneOffset.UTC);
            if (parsed.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) throw invalid("occurred_at không được ở tương lai");
            return parsed;
        } catch (DateTimeParseException exception) {
            throw invalid("occurred_at không đúng định dạng");
        }
    }

    private String validateTags(Object raw) {
        if (!(raw instanceof String text)) throw new MaydayApiException("1003", "context_tags sai kiểu dữ liệu");
        try {
            List<String> values = JSON.readValue(text, new TypeReference<>() {});
            if (values.isEmpty() || values.size() > 20 || values.stream().anyMatch(value -> value == null || value.isBlank() || value.length() > 50)) {
                throw invalid("context_tags không hợp lệ");
            }
            return text;
        } catch (MaydayApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid("context_tags không hợp lệ");
        }
    }

    private String requiredText(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || value instanceof String text && text.isBlank()) throw new MaydayApiException("1002", "Thiếu trường bắt buộc: " + key);
        if (!(value instanceof String)) throw new MaydayApiException("1003", "Sai kiểu dữ liệu: " + key);
        return (String) value;
    }

    private String optionalText(Map<String, Object> body, String key, int maxLength) {
        Object value = body.get(key);
        if (value == null) return null;
        if (!(value instanceof String text)) throw new MaydayApiException("1003", "Sai kiểu dữ liệu: " + key);
        if (text.length() > maxLength || containsMarkup(text)) throw invalid(key + " không hợp lệ");
        return text;
    }

    private long requiredId(Map<String, Object> body, String key) {
        Object raw = body.get(key);
        if (raw == null) throw new MaydayApiException("1002", "Thiếu trường bắt buộc: " + key);
        if (raw instanceof Boolean || raw instanceof List<?> || raw instanceof Map<?, ?> || raw instanceof Number number && number.doubleValue() % 1 != 0) {
            throw new MaydayApiException("1003", "Sai kiểu dữ liệu: " + key);
        }
        if (!(raw instanceof Number) && !(raw instanceof String)) throw new MaydayApiException("1003", "Sai kiểu dữ liệu: " + key);
        String text = String.valueOf(raw);
        if (!text.matches("[0-9]+")) throw new MaydayApiException("1003", "Sai kiểu dữ liệu: " + key);
        long id = parseId(text);
        if (id <= 0) throw invalid(key + " phải lớn hơn 0");
        return id;
    }

    private long parseId(String text) {
        if (text == null || !text.matches("[0-9]+")) throw new MaydayApiException("1003", "ID sai định dạng");
        try { return Long.parseLong(text); }
        catch (NumberFormatException exception) { throw invalid("ID vượt giới hạn"); }
    }

    private long parsePositiveQueryId(String text) {
        if (!text.matches("[0-9]+")) throw new MaydayApiException("1003", "medical_record_id sai định dạng");
        long id = parseId(text);
        if (id <= 0) throw invalid("medical_record_id phải lớn hơn 0");
        return id;
    }

    private void assertRecordEditable(Map<String, Object> record) {
        Object status = record.get("status");
        if ("completed".equals(status) || "cancelled".equals(status)) {
            throw new MaydayApiException("1010", "Không thể sửa hồ sơ đã đóng");
        }
    }

    private long number(Object value) { return Long.parseLong(String.valueOf(value)); }

    private void assertCanAccess(Long patientId, TaiKhoan actor) {
        if (!auth.isStaff(actor) && !patientId.equals(actor.getNguoi().getMaNguoi())) {
            throw new MaydayApiException("9998", "Không có quyền truy cập tài nguyên");
        }
    }

    private boolean containsMarkup(String value) { return value != null && value.matches("(?s).*<[^>]+>.*"); }

    private MaydayApiException invalid(String message) { return new MaydayApiException("1004", message); }

    private Map<String, Object> episodeData(LesionEpisodeResponse episode) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("episode_id", String.valueOf(episode.getId()));
        data.put("medical_record_id", String.valueOf(episode.getMedicalRecordId()));
        data.put("patient_id", String.valueOf(episode.getPatientId()));
        data.put("name", episode.getName());
        data.put("occurred_at", format(episode.getOccurredAt()));
        data.put("context_tags", episode.getCircumstance());
        data.put("context_other", episode.getOtherCircumstance());
        data.put("medication_name", episode.getMedication());
        data.put("medication_taken_ago", episode.getMedicationDuration());
        data.put("medication_unit", episode.getTimeUnit());
        data.put("description", episode.getDetails());
        data.put("created_at", episode.getCreatedAt());
        data.put("updated_at", episode.getUpdatedAt());
        return data;
    }

    private Map<String, Object> locationData(LesionLocationResponse location) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lesion_location_id", String.valueOf(location.getId()));
        data.put("episode_id", String.valueOf(location.getEpisodeId()));
        data.put("body_part", location.getBodyPart());
        data.put("occurred_at", format(location.getOccurredAt()));
        data.put("context_tags", location.getCircumstance());
        data.put("context_other", location.getOtherCircumstance());
        data.put("medication_name", location.getRelatedMedication());
        data.put("medication_taken_ago", location.getMedicationDuration());
        data.put("medication_unit", location.getTimeUnit());
        data.put("created_at", location.getCreatedAt());
        data.put("updated_at", location.getUpdatedAt());
        data.put("photos", new ArrayList<>());
        return data;
    }

    private String format(OffsetDateTime time) { return time == null ? null : time.withOffsetSameInstant(ZoneOffset.UTC).format(OUTPUT_TIME); }
}
