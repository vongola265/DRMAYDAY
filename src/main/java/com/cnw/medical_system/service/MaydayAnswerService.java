package com.cnw.medical_system.service;

import com.cnw.medical_system.dto.response.MaydayAnswerImageResponse;
import com.cnw.medical_system.entity.TaiKhoan;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaydayAnswerService {
    private final JdbcTemplate jdbc;
    private final MaydayAuthService auth;
    private final MaydayAttachmentService attachments;

    @Transactional
    public Map<String,Object> saveAnswers(Long recordId,TaiKhoan actor,Map<String,Object> body) {
        var record=record(recordId,actor,true);
        Object raw=body.get("answers");
        if(raw==null) throw new MaydayApiException("1002","Thiếu answers");
        if(!(raw instanceof List<?> input)) throw new MaydayApiException("1003","answers sai kiểu dữ liệu");
        if(input.isEmpty()) throw new MaydayApiException("1004","answers không được rỗng");
        List<Map<String,Object>> saved=new ArrayList<>();
        for(Object item:input) {
            if(!(item instanceof Map<?,?> values)) throw new MaydayApiException("1003","answer sai kiểu dữ liệu");
            Object q=values.get("question_id"); long questionId=id(q,"question_id");
            Object option=values.get("question_option_id"); Long optionId=option==null?null:id(option,"question_option_id");
            Object text=values.get("answer_text"); Object json=values.get("answer_json");
            if(text!=null && !(text instanceof String)) throw new MaydayApiException("1003","answer_text sai kiểu dữ liệu");
            String jsonText=json==null?null:json instanceof String s?s:toJson(json);
            Long answerId=jdbc.queryForObject("""
                    INSERT INTO mayday_medical_record_answers(medical_record_id,question_id,question_option_id,answer_text,answer_json)
                    VALUES(?,?,?,?,?::jsonb) ON CONFLICT(medical_record_id,question_id) DO UPDATE SET
                    question_option_id=EXCLUDED.question_option_id,answer_text=EXCLUDED.answer_text,answer_json=EXCLUDED.answer_json
                    RETURNING answer_id
                    """,Long.class,recordId,questionId,optionId,text,jsonText);
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("answer_id",String.valueOf(answerId)); result.put("question_id",String.valueOf(questionId));
            if(optionId!=null) result.put("question_option_id",String.valueOf(optionId)); saved.add(result);
        }
        return Map.of("answers",saved,"count",String.valueOf(saved.size()));
    }

    public List<Map<String,Object>> listAnswers(Long recordId,TaiKhoan actor) {
        record(recordId,actor,false);
        return jdbc.query("SELECT answer_id,question_id,question_option_id,answer_text,answer_json FROM mayday_medical_record_answers WHERE medical_record_id=? ORDER BY answer_id",
                (rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("answer_id",String.valueOf(rs.getLong("answer_id")));m.put("question_id",String.valueOf(rs.getLong("question_id")));Object o=rs.getObject("question_option_id");if(o!=null)m.put("question_option_id",String.valueOf(o));m.put("answer_text",rs.getString("answer_text"));m.put("answer_json",rs.getObject("answer_json"));return m;},recordId);
    }

    @Transactional
    public MaydayAnswerImageResponse createImage(TaiKhoan actor,Map<String,Object> body) {
        if(auth.isAdmin(actor)) throw new MaydayApiException("1009","Quản trị viên không thể gắn ảnh câu trả lời");
        long answerId=id(body.get("answer_id"),"answer_id"); long attachmentId=id(body.get("attachment_id"),"attachment_id");
        String option=null; Object raw=body.get("option_value"); if(raw!=null){if(!(raw instanceof String s))throw new MaydayApiException("1003","option_value sai kiểu dữ liệu");if(s.length()>100)throw new MaydayApiException("1004","option_value quá dài");option=s;}
        Map<String,Object> answer=answer(answerId,actor,true);
        var file=attachments.find(attachmentId,actor);
        if(file.userId()!=null&&!file.userId().equals(String.valueOf(answer.get("patient_id")))) throw new MaydayApiException("1009","Ảnh phải thuộc cùng bệnh nhân với câu trả lời");
        Long link=jdbc.queryForObject("INSERT INTO mayday_answer_images(answer_id,attachment_id,option_value) VALUES(?,?,?) RETURNING answer_image_id",Long.class,answerId,attachmentId,option);
        return image(link);
    }

    @Transactional
    public Map<String,Object> createImages(TaiKhoan actor,Map<String,Object> body) {
        Object raw=body.get("attachments"); if(raw==null) throw new MaydayApiException("1002","Thiếu attachments");
        if(!(raw instanceof List<?> entries)) throw new MaydayApiException("1003","attachments sai kiểu dữ liệu");
        if(entries.isEmpty()) throw new MaydayApiException("1004","attachments không được rỗng");
        long answerId=id(body.get("answer_id"),"answer_id"); List<MaydayAnswerImageResponse> results=new ArrayList<>();
        for(Object entry:entries){if(!(entry instanceof Map<?,?> map))throw new MaydayApiException("1003","attachment sai kiểu dữ liệu");Map<String,Object> data=new LinkedHashMap<>();for(var e:map.entrySet())data.put(String.valueOf(e.getKey()),e.getValue());data.put("answer_id",answerId);results.add(createImage(actor,data));}
        return Map.of("total",String.valueOf(results.size()),"items",results);
    }

    public List<MaydayAnswerImageResponse> listImages(Long answerId,TaiKhoan actor) {
        answer(answerId,actor,false);
        return jdbc.query("SELECT answer_image_id,answer_id,attachment_id,option_value,created_at FROM mayday_answer_images WHERE answer_id=? ORDER BY answer_image_id",
                (rs,n)->image(rs.getLong("answer_image_id"),rs.getLong("answer_id"),rs.getLong("attachment_id"),rs.getString("option_value"),rs.getObject("created_at")),answerId);
    }

    @Transactional
    public void deleteImage(Long linkId,TaiKhoan actor) {
        MaydayAnswerImageResponse link=image(linkId);
        if(auth.isAdmin(actor))throw new MaydayApiException("1009","Quản trị viên không gỡ ảnh câu trả lời");
        answer(Long.parseLong(link.answerId()),actor,true);
        jdbc.update("DELETE FROM mayday_answer_images WHERE answer_image_id=?",linkId);
    }

    private Map<String,Object> record(Long id,TaiKhoan actor,boolean editable) {
        List<Map<String,Object>> rows=jdbc.query("SELECT ma_ho_so,ma_benh_nhan,trang_thai FROM hosobenhan WHERE ma_ho_so=?",(rs,n)->Map.of("id",rs.getLong(1),"patient_id",rs.getLong(2),"status",rs.getString(3)),id);
        if(rows.isEmpty())throw new MaydayApiException("9992","Không tìm thấy hồ sơ bệnh án"); Map<String,Object> r=rows.get(0);
        if(!auth.isStaff(actor)&&!Long.valueOf(actor.getNguoi().getMaNguoi()).equals(r.get("patient_id")))throw new MaydayApiException("9998","Không có quyền truy cập hồ sơ");
        if(editable&&("completed".equals(r.get("status"))||"cancelled".equals(r.get("status"))))throw new MaydayApiException("1010","Hồ sơ đã đóng"); return r;
    }
    private Map<String,Object> answer(Long id,TaiKhoan actor,boolean editable) {
        List<Map<String,Object>> rows=jdbc.query("SELECT a.answer_id,h.ma_benh_nhan patient_id,h.ma_ho_so,h.trang_thai FROM mayday_medical_record_answers a JOIN hosobenhan h ON h.ma_ho_so=a.medical_record_id WHERE a.answer_id=?",
                (rs,n)->Map.of("answer_id",rs.getLong(1),"patient_id",rs.getLong(2),"record_id",rs.getLong(3),"status",rs.getString(4)),id);
        if(rows.isEmpty())throw new MaydayApiException("9992","Không tìm thấy câu trả lời");Map<String,Object> a=rows.get(0);
        if(!auth.isStaff(actor)&&!Long.valueOf(actor.getNguoi().getMaNguoi()).equals(a.get("patient_id")))throw new MaydayApiException("9998","Không có quyền truy cập câu trả lời");
        if(editable&&("completed".equals(a.get("status"))||"cancelled".equals(a.get("status"))))throw new MaydayApiException("1010","Hồ sơ đã đóng");return a;
    }
    private MaydayAnswerImageResponse image(Long id) { List<MaydayAnswerImageResponse> rows=jdbc.query("SELECT answer_image_id,answer_id,attachment_id,option_value,created_at FROM mayday_answer_images WHERE answer_image_id=?",(rs,n)->image(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4),rs.getObject(5)),id);if(rows.isEmpty())throw new MaydayApiException("9992","Không tìm thấy liên kết ảnh");return rows.get(0); }
    private MaydayAnswerImageResponse image(Object id,Object answer,Object attachment,String option,Object created){String x=String.valueOf(id);return new MaydayAnswerImageResponse(x,x,String.valueOf(answer),String.valueOf(attachment),option,String.valueOf(created));}
    private long id(Object v,String field){if(v==null)throw new MaydayApiException("1002","Thiếu trường bắt buộc: "+field);if(!(v instanceof String)&&!(v instanceof Number)||v instanceof Number n&&n.doubleValue()%1!=0)throw new MaydayApiException("1003",field+" sai kiểu dữ liệu");try{long x=Long.parseLong(String.valueOf(v));if(x<=0)throw new MaydayApiException("1004",field+" phải lớn hơn 0");return x;}catch(NumberFormatException e){throw new MaydayApiException("1003",field+" sai định dạng");}}
    private String toJson(Object v){try{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(v);}catch(Exception e){throw new MaydayApiException("1003","answer_json sai định dạng");}}
}
