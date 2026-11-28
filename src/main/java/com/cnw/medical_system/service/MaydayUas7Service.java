package com.cnw.medical_system.service;

import com.cnw.medical_system.entity.TaiKhoan;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaydayUas7Service {
    private final JdbcTemplate jdbc;
    private final MaydayAuthService auth;
    private final MaydayRecordService records;

    public Map<String,Object> eligibility(TaiKhoan actor) {
        patientOnly(actor);
        Long userId=actor.getNguoi().getMaNguoi();
        List<Long> rows=jdbc.query("SELECT ma_ho_so FROM hosobenhan WHERE ma_benh_nhan=? AND trang_thai NOT IN ('completed','cancelled') ORDER BY ma_ho_so DESC LIMIT 1",(rs,n)->rs.getLong(1),userId);
        Map<String,Object> result=new LinkedHashMap<>(); result.put("eligible",String.valueOf(!rows.isEmpty()));
        if(!rows.isEmpty())result.put("medical_record_id",String.valueOf(rows.get(0)));
        return result;
    }

    @Transactional
    public Map<String,Object> create(TaiKhoan actor,Map<String,Object> body) {
        patientOnly(actor);
        long recordId=id(body.get("medical_record_id"),"medical_record_id");
        Object rawDate=body.get("score_date"); if(rawDate==null)throw new MaydayApiException("1002","Thiếu score_date");
        if(!(rawDate instanceof String dateText))throw new MaydayApiException("1003","score_date sai kiểu dữ liệu");
        LocalDate date; try{date=LocalDate.parse(dateText);}catch(DateTimeParseException e){throw new MaydayApiException("1004","score_date không hợp lệ");}
        if(date.isAfter(LocalDate.now()))throw new MaydayApiException("1004","score_date không được ở tương lai");
        Integer iss=score(body,"iss7"),hss=score(body,"hss7");
        Object noteRaw=body.get("note"); if(noteRaw!=null&&!(noteRaw instanceof String))throw new MaydayApiException("1003","note sai kiểu dữ liệu");
        String note=(String)noteRaw; if(note!=null&&(note.length()>500||note.matches("(?s).*<[^>]+>.*")))throw new MaydayApiException("1004","note không hợp lệ");
        Map<String,Object> record=records.get(recordId,actor);
        Object status=record.get("status"); if("completed".equals(status)||"cancelled".equals(status))throw new MaydayApiException("1010","Hồ sơ đã đóng");
        try {
            Long id=jdbc.queryForObject("INSERT INTO nhatkyuashangngay(ma_ho_so_man,ma_benh_nhan,ngay_cham_diem,diem_san_phu_hss,diem_ngua_iss,trang_thai_ngay) VALUES(?,?,?,?,?,'HOP_LE') RETURNING ma_nhat_ky_uas",Long.class,recordId,actor.getNguoi().getMaNguoi(),date,hss,iss);
            Map<String,Object> out=new LinkedHashMap<>();out.put("id",String.valueOf(id));out.put("patient_id",String.valueOf(actor.getNguoi().getMaNguoi()));out.put("medical_record_id",String.valueOf(recordId));out.put("score_date",date.toString());out.put("iss7",String.valueOf(iss));out.put("hss7",String.valueOf(hss));out.put("uas7",String.valueOf(iss+hss));if(note!=null)out.put("note",note);return out;
        } catch(DuplicateKeyException e){throw new MaydayApiException("1010","Đã có điểm UAS7 cho ngày này");}
    }

    public List<Map<String,Object>> list(Long patientId,TaiKhoan actor) {
        assertPatientAccess(patientId,actor);
        return jdbc.query("SELECT ma_nhat_ky_uas,ma_benh_nhan,ma_ho_so_man,ngay_cham_diem,diem_ngua_iss,diem_san_phu_hss,diem_uas_ngay FROM nhatkyuashangngay WHERE ma_benh_nhan=? ORDER BY ngay_cham_diem DESC,ma_nhat_ky_uas DESC",
                (rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",String.valueOf(rs.getLong(1)));m.put("patient_id",String.valueOf(rs.getLong(2)));m.put("medical_record_id",String.valueOf(rs.getLong(3)));m.put("score_date",rs.getDate(4).toLocalDate().toString());m.put("iss7",String.valueOf(rs.getInt(5)));m.put("hss7",String.valueOf(rs.getInt(6)));m.put("uas7",String.valueOf(rs.getInt(7)));return m;},patientId);
    }

    public Map<String,Object> week(Long patientId,TaiKhoan actor,String startDate) {
        assertPatientAccess(patientId,actor);
        LocalDate end=startDate==null?LocalDate.now():parseDate(startDate); if(end.isAfter(LocalDate.now()))throw new MaydayApiException("1004","start_date không được ở tương lai");
        LocalDate start=end.minusDays(6);
        Map<String,Object> agg=jdbc.queryForObject("SELECT COALESCE(sum(diem_uas_ngay),0) score,count(*) FILTER (WHERE trang_thai_ngay='HOP_LE') days FROM nhatkyuashangngay WHERE ma_benh_nhan=? AND ngay_cham_diem BETWEEN ? AND ?",(rs,n)->Map.of("score",rs.getInt("score"),"days",rs.getInt("days")),patientId,start,end);
        int score=(Integer)agg.get("score"),days=(Integer)agg.get("days"); String label=score==0?"Không có":score<=15?"Nhẹ":score<=27?"Trung bình":"Nặng";
        Map<String,Object> out=new LinkedHashMap<>();out.put("patient_id",String.valueOf(patientId));out.put("start_date",start.toString());out.put("end_date",end.toString());out.put("week_score",String.valueOf(score));out.put("severity_label",label);out.put("completed_days",String.valueOf(days));return out;
    }

    private void assertPatientAccess(Long patientId,TaiKhoan actor){if(patientId<=0)throw new MaydayApiException("1004","patient_id phải lớn hơn 0");if(jdbc.queryForObject("SELECT count(*) FROM benhnhan WHERE ma_benh_nhan=?",Integer.class,patientId)==0)throw new MaydayApiException("9992","Không tìm thấy bệnh nhân");if(!auth.isStaff(actor)&&!patientId.equals(actor.getNguoi().getMaNguoi()))throw new MaydayApiException("1009","Không có quyền truy cập dữ liệu bệnh nhân");}
    private void patientOnly(TaiKhoan actor){if(auth.isStaff(actor))throw new MaydayApiException("1009","Chức năng dành cho bệnh nhân");}
    private int score(Map<String,Object> body,String key){Object v=body.get(key);if(v==null)throw new MaydayApiException("1002","Thiếu "+key);if(!(v instanceof String s)||!s.matches("[0-9]+"))throw new MaydayApiException("1003",key+" sai kiểu dữ liệu");try{int n=Integer.parseInt(s);if(n<0||n>3)throw new MaydayApiException("1004",key+" phải từ 0 đến 3");return n;}catch(NumberFormatException e){throw new MaydayApiException("1004",key+" không hợp lệ");}}
    private long id(Object v,String key){if(v==null)throw new MaydayApiException("1002","Thiếu "+key);if(!(v instanceof String)&&!(v instanceof Number)||v instanceof Number n&&n.doubleValue()%1!=0)throw new MaydayApiException("1003",key+" sai kiểu dữ liệu");try{long x=Long.parseLong(String.valueOf(v));if(x<=0)throw new MaydayApiException("1004",key+" phải lớn hơn 0");return x;}catch(NumberFormatException e){throw new MaydayApiException("1003",key+" sai định dạng");}}
    private LocalDate parseDate(String text){try{return LocalDate.parse(text);}catch(DateTimeParseException e){throw new MaydayApiException("1003","start_date sai định dạng");}}
}
