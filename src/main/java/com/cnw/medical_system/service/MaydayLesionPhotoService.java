package com.cnw.medical_system.service;

import com.cnw.medical_system.dto.response.MaydayImageAttachmentResponse;
import com.cnw.medical_system.dto.response.MaydayLesionPhotoResponse;
import com.cnw.medical_system.entity.TaiKhoan;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaydayLesionPhotoService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> MILESTONES = List.of("start", "peak", "resolution");
    private final JdbcTemplate jdbc;
    private final MaydayAuthService auth;
    private final MaydayAttachmentService attachments;
    private final LesionService lesions;
    private final MaydayRecordService records;

    public List<MaydayLesionPhotoResponse> list(Long locationId, TaiKhoan actor) {
        var location = lesions.getLocation(locationId);
        var episode = lesions.getEpisode(location.getEpisodeId());
        records.get(episode.getMedicalRecordId(), actor);
        return jdbc.query("""
                SELECT a.ma_anh photo_id, a.ma_dot_ton_thuong_da episode_id,
                       a.ma_vi_tri_ton_thuong location_id, a.ma_tep_dinh_kem attachment_id,
                       COALESCE(t.duong_dan_url,t.duong_dan_luu_tru) file_url,
                       a.thoi_diem_chup captured_at,a.moc_tien_trien milestone,
                       a.diem_ngua_nrs itch_score,a.diem_dau_nrs pain_score,
                       a.diem_bong_rat_nrs burn_score,a.so_luong_not_san hives_count,
                       a.chi_so_auas_cuc_bo local_auas,a.url_anh annotated_url
                FROM anhchuptonthuong a JOIN tepdinhkem t ON t.ma_tep_dinh_kem=a.ma_tep_dinh_kem
                WHERE a.ma_vi_tri_ton_thuong=? ORDER BY a.thoi_diem_chup,a.ma_anh
                """, (rs, row) -> photo(rs.getObject("photo_id"), rs.getObject("episode_id"),
                rs.getObject("location_id"), rs.getObject("attachment_id"), rs.getString("file_url"),
                rs.getObject("captured_at"), rs.getString("milestone"), rs.getObject("itch_score"),
                rs.getObject("pain_score"), rs.getObject("burn_score"), rs.getObject("hives_count"),
                rs.getObject("local_auas"), rs.getString("annotated_url")), locationId);
    }

    @Transactional
    public MaydayLesionPhotoResponse create(TaiKhoan actor, Map<String, Object> body) {
        if (auth.isAdmin(actor)) throw new MaydayApiException("1009", "Quản trị viên không tạo ảnh tổn thương");
        long episodeId = requiredId(body, "episode_id");
        long attachmentId = requiredId(body, "attachment_id");
        String milestone = requiredText(body, "milestone");
        if (!MILESTONES.contains(milestone)) throw invalid("milestone không hợp lệ");
        var episode = lesions.getEpisode(episodeId);
        var record = accessibleRecord(episode.getMedicalRecordId(), actor);
        ensureEditable(record);
        Long locationId = body.containsKey("location_id") ? requiredId(body, "location_id") : null;
        if (locationId != null && !lesions.getLocation(locationId).getEpisodeId().equals(episodeId)) throw invalid("location_id không thuộc episode_id");
        var file = attachments.find(attachmentId, actor);
        if (file.medicalRecordId() != null && !file.medicalRecordId().equals(String.valueOf(episode.getMedicalRecordId()))) throw new MaydayApiException("1009", "Tệp không thuộc hồ sơ bệnh án");
        OffsetDateTime takenAt = body.containsKey("taken_at") ? time(body.get("taken_at")) : OffsetDateTime.now(ZoneOffset.UTC);
        Integer itch = score(body, "itch_score", 10); Integer pain = score(body, "pain_score", 10);
        Integer burn = score(body, "burn_score", 10); Integer hives = score(body, "hives_count", 500);
        Integer auas = score(body, "local_auas", 100);
        String annotated = url(body.get("annotated_url"));
        Long id = jdbc.queryForObject("""
                INSERT INTO anhchuptonthuong(ma_tep_dinh_kem,ma_dot_ton_thuong_da,ma_vi_tri_ton_thuong,
                  thoi_diem_chup,moc_tien_trien,diem_ngua_nrs,diem_dau_nrs,diem_bong_rat_nrs,
                  so_luong_not_san,chi_so_auas_cuc_bo,url_anh)
                VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING ma_anh
                """, Long.class, attachmentId, episodeId, locationId, takenAt, milestone, itch, pain, burn, hives, auas, annotated);
        return get(id, actor);
    }

    @Transactional
    public MaydayLesionPhotoResponse update(Long id, TaiKhoan actor, Map<String, Object> body) {
        if (auth.isAdmin(actor)) throw new MaydayApiException("1009", "Quản trị viên không sửa ảnh tổn thương");
        var current = get(id, actor);
        var episode = lesions.getEpisode(Long.parseLong(current.episodeId()));
        ensureEditable(records.get(episode.getMedicalRecordId(), actor));
        if (body.isEmpty()) throw new MaydayApiException("1002", "Thiếu trường cần cập nhật");
        if (body.containsKey("milestone")) {
            String value = requiredText(body, "milestone");
            if (!MILESTONES.contains(value)) throw invalid("milestone không hợp lệ");
            jdbc.update("UPDATE anhchuptonthuong SET moc_tien_trien=? WHERE ma_anh=?", value, id);
        }
        for (String field : List.of("itch_score", "pain_score", "burn_score", "hives_count", "local_auas")) {
            if (!body.containsKey(field)) continue;
            Integer value = score(body, field, field.equals("hives_count") ? 500 : field.equals("local_auas") ? 100 : 10);
            String column = switch (field) { case "itch_score" -> "diem_ngua_nrs"; case "pain_score" -> "diem_dau_nrs"; case "burn_score" -> "diem_bong_rat_nrs"; case "hives_count" -> "so_luong_not_san"; default -> "chi_so_auas_cuc_bo"; };
            jdbc.update("UPDATE anhchuptonthuong SET " + column + "=? WHERE ma_anh=?", value, id);
        }
        if (body.containsKey("annotated_url")) jdbc.update("UPDATE anhchuptonthuong SET url_anh=? WHERE ma_anh=?", url(body.get("annotated_url")), id);
        if (body.containsKey("taken_at")) jdbc.update("UPDATE anhchuptonthuong SET thoi_diem_chup=? WHERE ma_anh=?", time(body.get("taken_at")), id);
        return get(id, actor);
    }

    @Transactional
    public void delete(Long id, TaiKhoan actor) {
        if (auth.isAdmin(actor)) throw new MaydayApiException("1009", "Quản trị viên không xóa ảnh tổn thương");
        var current = get(id, actor);
        var episode = lesions.getEpisode(Long.parseLong(current.episodeId()));
        ensureEditable(records.get(episode.getMedicalRecordId(), actor));
        jdbc.update("DELETE FROM anhchuptonthuong WHERE ma_anh=?", id);
    }

    public MaydayLesionPhotoResponse get(Long id, TaiKhoan actor) {
        List<MaydayLesionPhotoResponse> rows = jdbc.query("""
                SELECT a.ma_anh photo_id,a.ma_dot_ton_thuong_da episode_id,a.ma_vi_tri_ton_thuong location_id,
                  a.ma_tep_dinh_kem attachment_id,COALESCE(t.duong_dan_url,t.duong_dan_luu_tru) file_url,
                  a.thoi_diem_chup captured_at,a.moc_tien_trien milestone,a.diem_ngua_nrs itch_score,
                  a.diem_dau_nrs pain_score,a.diem_bong_rat_nrs burn_score,a.so_luong_not_san hives_count,
                  a.chi_so_auas_cuc_bo local_auas,a.url_anh annotated_url
                FROM anhchuptonthuong a JOIN tepdinhkem t ON t.ma_tep_dinh_kem=a.ma_tep_dinh_kem WHERE a.ma_anh=?
                """, (rs, row) -> photo(rs.getObject("photo_id"),rs.getObject("episode_id"),rs.getObject("location_id"),
                rs.getObject("attachment_id"),rs.getString("file_url"),rs.getObject("captured_at"),rs.getString("milestone"),
                rs.getObject("itch_score"),rs.getObject("pain_score"),rs.getObject("burn_score"),rs.getObject("hives_count"),
                rs.getObject("local_auas"),rs.getString("annotated_url")), id);
        if (rows.isEmpty()) throw new MaydayApiException("9992", "Không tìm thấy ảnh tổn thương");
        var photo = rows.get(0);
        if (photo.locationId() != null) {
            var loc = lesions.getLocation(Long.parseLong(photo.locationId()));
            var episode = lesions.getEpisode(loc.getEpisodeId());
            accessibleRecord(episode.getMedicalRecordId(), actor);
        } else accessibleRecord(lesions.getEpisode(Long.parseLong(photo.episodeId())).getMedicalRecordId(), actor);
        return photo;
    }

    private MaydayLesionPhotoResponse photo(Object id,Object ep,Object loc,Object att,String url,Object time,String milestone,
            Object itch,Object pain,Object burn,Object hives,Object auas,String annotated) {
        String attId=String.valueOf(att);
        return new MaydayLesionPhotoResponse(String.valueOf(id),String.valueOf(ep),loc==null?null:String.valueOf(loc),attId,
                new MaydayImageAttachmentResponse(attId,url),time,time,milestone,itch,pain,burn,hives,auas,annotated);
    }
    private long requiredId(Map<String,Object> body,String key) {
        Object value=body.get(key); if(value==null) throw new MaydayApiException("1002","Thiếu trường bắt buộc: "+key);
        if(!(value instanceof Number)&&!(value instanceof String) || value instanceof Number n && n.doubleValue()%1!=0) throw new MaydayApiException("1003",key+" sai kiểu dữ liệu");
        try { long id=Long.parseLong(String.valueOf(value)); if(id<=0) throw invalid(key+" phải lớn hơn 0"); return id; }
        catch(NumberFormatException e){throw new MaydayApiException("1003",key+" sai định dạng");}
    }
    private String requiredText(Map<String,Object> body,String key) { Object v=body.get(key); if(v==null) throw new MaydayApiException("1002","Thiếu trường bắt buộc: "+key); if(!(v instanceof String s)) throw new MaydayApiException("1003",key+" sai kiểu dữ liệu"); return s; }
    private Integer score(Map<String,Object> body,String key,int max) {
        Object v=body.get(key); if(v==null) return null; if(!(v instanceof String s) || !s.matches("-?[0-9]+")) throw new MaydayApiException("1003",key+" sai kiểu dữ liệu");
        try { int n=Integer.parseInt(s); if(n<0||n>max) throw invalid(key+" không hợp lệ"); return n; } catch(NumberFormatException e){throw invalid(key+" không hợp lệ");}
    }
    private OffsetDateTime time(Object raw) {
        if(!(raw instanceof String s)) throw new MaydayApiException("1003","taken_at sai kiểu dữ liệu");
        try { OffsetDateTime v=LocalDateTime.parse(s,TIME).atOffset(ZoneOffset.UTC); if(v.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) throw invalid("taken_at không được ở tương lai"); return v; }
        catch(java.time.format.DateTimeParseException e){throw invalid("taken_at không đúng định dạng");}
    }
    private String url(Object raw) {
        if(raw==null) return null; if(!(raw instanceof String s)) throw new MaydayApiException("1003","annotated_url sai kiểu dữ liệu");
        if(s.length()>500||!(s.startsWith("https://")||s.startsWith("http://"))) throw invalid("annotated_url không hợp lệ"); return s;
    }
    private void ensureEditable(Map<String,Object> record) { Object s=record.get("status"); if("completed".equals(s)||"cancelled".equals(s)) throw new MaydayApiException("1010","Không thể sửa hồ sơ đã đóng"); }
    private Map<String,Object> accessibleRecord(Long recordId, TaiKhoan actor) {
        try { return records.get(recordId, actor); }
        catch (MaydayApiException exception) {
            if ("9998".equals(exception.getCode())) throw new MaydayApiException("1009", "Hồ sơ thuộc bệnh nhân khác");
            throw exception;
        }
    }
    private MaydayApiException invalid(String m){return new MaydayApiException("1004",m);}
}
