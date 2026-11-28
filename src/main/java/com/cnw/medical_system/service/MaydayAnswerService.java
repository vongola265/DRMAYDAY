package com.cnw.medical_system.service;

import com.cnw.medical_system.dto.response.MaydayAnswerImageResponse;
import com.cnw.medical_system.entity.TaiKhoan;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.BadSqlGrammarException;
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
        if(auth.isAdmin(actor)) throw new MaydayApiException("1009","Quản trị viên không thể sửa câu trả lời bệnh án");
        var record=record(recordId,actor,true);
        Object raw=body.get("answers");
        if(raw==null) throw new MaydayApiException("1002","Thiếu answers");
        if(!(raw instanceof List<?> input)) throw new MaydayApiException("1003","answers sai kiểu dữ liệu");
        if(input.isEmpty()) throw new MaydayApiException("1004","answers không được rỗng");
        if(input.size()>200) throw new MaydayApiException("1004","Tối đa 200 câu trả lời mỗi lần lưu");
        List<Map<String,Object>> saved=new ArrayList<>();
        java.util.Set<Long> questionIds=new java.util.HashSet<>();
        for(Object item:input) {
            if(!(item instanceof Map<?,?> values)) throw new MaydayApiException("1003","answer sai kiểu dữ liệu");
            Object q=values.get("question_id"); long questionId=id(q,"question_id");
            if(!questionIds.add(questionId)) throw new MaydayApiException("1004","question_id không được trùng lặp");
            Object option=values.get("question_option_id"); Long optionId=option==null?null:id(option,"question_option_id");
            validateQuestion(questionId, optionId);
            Object text=values.get("answer_text"); Object json=values.containsKey("answer_value")?values.get("answer_value"):values.get("answer_json");
            if(text!=null && !(text instanceof String)) throw new MaydayApiException("1003","answer_text sai kiểu dữ liệu");
            if(text==null && optionId==null && json==null) throw new MaydayApiException("1002","Thiếu giá trị câu trả lời");
            if(text instanceof String s && (s.length()>2000 || s.matches("(?s).*<[^>]+>.*"))) throw new MaydayApiException("1004","answer_text không hợp lệ");
            if(json!=null && !(json instanceof Map<?,?>)) throw new MaydayApiException("1003","answer_value phải là một object");
            String jsonText=json==null?null:toJson(json);
            Long answerId=jdbc.queryForObject("""
                    INSERT INTO mayday_medical_record_answers(medical_record_id,question_id,question_option_id,answer_text,answer_json,answered_by,updated_at)
                    VALUES(?,?,?,?,?::jsonb,?,CURRENT_TIMESTAMP) ON CONFLICT(medical_record_id,question_id) DO UPDATE SET
                    question_option_id=EXCLUDED.question_option_id,answer_text=EXCLUDED.answer_text,answer_json=EXCLUDED.answer_json,
                    answered_by=EXCLUDED.answered_by,updated_at=CURRENT_TIMESTAMP
                    RETURNING answer_id
                    """,Long.class,recordId,questionId,optionId,text,jsonText,actor.getNguoi().getMaNguoi());
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("answer_id",String.valueOf(answerId)); result.put("question_id",String.valueOf(questionId));
            if(optionId!=null) result.put("question_option_id",String.valueOf(optionId)); saved.add(result);
        }
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("answers", saved);
        response.put("count", saved.size());
        if (!saved.isEmpty()) {
            response.put("id", saved.get(0).get("answer_id"));
            response.put("answer_id", saved.get(0).get("answer_id"));
            response.put("question_id", saved.get(0).get("question_id"));
        }
        return response;
    }

    public List<Map<String,Object>> listAnswers(Long recordId,TaiKhoan actor) {
        record(recordId,actor,false);
        return jdbc.query("SELECT answer_id,medical_record_id,question_id,question_option_id,answer_text,answer_json::text answer_json,answered_by,created_at,updated_at FROM mayday_medical_record_answers WHERE medical_record_id=? ORDER BY answer_id",
                (rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",rs.getLong("answer_id"));m.put("answer_id",String.valueOf(rs.getLong("answer_id")));m.put("medical_record_id",rs.getLong("medical_record_id"));m.put("question_id",String.valueOf(rs.getLong("question_id")));Object o=rs.getObject("question_option_id");if(o!=null)m.put("question_option_id",((Number)o).longValue());m.put("answer_text",rs.getString("answer_text"));m.put("answer_value",jsonValue(rs.getObject("answer_json")));m.put("answer_json",jsonValue(rs.getObject("answer_json")));m.put("answered_by",rs.getObject("answered_by")==null?null:rs.getLong("answered_by"));m.put("created_at",rs.getObject("created_at"));m.put("updated_at",rs.getObject("updated_at"));return m;},recordId);
    }

    @Transactional
    public MaydayAnswerImageResponse createImage(TaiKhoan actor,Map<String,Object> body) {
        if(auth.isAdmin(actor)) throw new MaydayApiException("1009","Quản trị viên không thể gắn ảnh câu trả lời");
        long answerId=id(body.get("answer_id"),"answer_id"); long attachmentId=id(body.get("attachment_id"),"attachment_id");
        String option=null; Object raw=body.get("option_value"); if(raw!=null){if(!(raw instanceof String s))throw new MaydayApiException("1003","option_value sai kiểu dữ liệu");if(s.length()>100)throw new MaydayApiException("1004","option_value quá dài");option=s;}
        Map<String,Object> answer=answer(answerId,actor,true);
        var file=attachments.find(attachmentId,actor);
        if(file.userId()!=null&&!file.userId().equals(String.valueOf(answer.get("patient_id")))) throw new MaydayApiException("1009","Ảnh phải thuộc cùng bệnh nhân với câu trả lời");
        Integer duplicate=jdbc.queryForObject("SELECT count(*) FROM mayday_answer_images WHERE answer_id=? AND attachment_id=?",Integer.class,answerId,attachmentId);
        if(duplicate!=null&&duplicate>0) throw new MaydayApiException("1010","Ảnh đã được gắn vào câu trả lời");
        Integer linkCount=jdbc.queryForObject("SELECT count(*) FROM mayday_answer_images WHERE answer_id=?",Integer.class,answerId);
        if(linkCount!=null&&linkCount>=10) throw new MaydayApiException("1008","Tối đa 10 ảnh cho mỗi câu trả lời");
        Long link=jdbc.queryForObject("INSERT INTO mayday_answer_images(answer_id,attachment_id,option_value) VALUES(?,?,?) RETURNING answer_image_id",Long.class,answerId,attachmentId,option);
        return image(link);
    }

    @Transactional
    public Map<String,Object> createImages(TaiKhoan actor,Map<String,Object> body) {
        if(auth.isAdmin(actor)) throw new MaydayApiException("1009","Quản trị viên không thể gắn ảnh câu trả lời");
        long answerId=id(body.get("answer_id"),"answer_id");
        Object raw=body.get("attachments"); if(raw==null) throw new MaydayApiException("1002","Thiếu attachments");
        if(!(raw instanceof List<?> entries)) throw new MaydayApiException("1003","attachments sai kiểu dữ liệu");
        if(entries.isEmpty()) throw new MaydayApiException("1004","attachments không được rỗng");
        if(entries.size()>10) throw new MaydayApiException("1008","Tối đa 10 ảnh mỗi lần gắn");
        Map<String,Object> answer=answer(answerId,actor,true);
        Integer existingCount=jdbc.queryForObject("SELECT count(*) FROM mayday_answer_images WHERE answer_id=?",Integer.class,answerId);
        List<Map<String,Object>> normalized=new ArrayList<>();
        for(Object entry:entries){if(!(entry instanceof Map<?,?> map))throw new MaydayApiException("1003","attachment sai kiểu dữ liệu");Map<String,Object> data=new LinkedHashMap<>();for(var e:map.entrySet())data.put(String.valueOf(e.getKey()),e.getValue());data.put("answer_id",answerId);Object rawAttachment=data.get("attachment_id");long attachmentId=id(rawAttachment,"attachment_id");if(normalized.stream().anyMatch(item->String.valueOf(item.get("attachment_id")).equals(String.valueOf(attachmentId)))) throw new MaydayApiException("1004","Không thể lặp lại cùng tệp trong attachments");normalized.add(data);}
        int newCount=0;
        for(Map<String,Object> item:normalized){long attachmentId=id(item.get("attachment_id"),"attachment_id");if(jdbc.queryForObject("SELECT count(*) FROM mayday_answer_images WHERE answer_id=? AND attachment_id=?",Integer.class,answerId,attachmentId)==0)newCount++;}
        if((existingCount==null?0:existingCount)+newCount>10)throw new MaydayApiException("1008","Tối đa 10 ảnh cho mỗi câu trả lời");
        List<MaydayAnswerImageResponse> results=new ArrayList<>();
        for(Map<String,Object> data:normalized){long attachmentId=id(data.get("attachment_id"),"attachment_id");Integer duplicate=jdbc.queryForObject("SELECT count(*) FROM mayday_answer_images WHERE answer_id=? AND attachment_id=?",Integer.class,answerId,attachmentId);if(duplicate!=null&&duplicate>0)continue;results.add(createImage(actor,data));}
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
        if(!auth.isStaff(actor)&&!Long.valueOf(actor.getNguoi().getMaNguoi()).equals(r.get("patient_id")))throw new MaydayApiException("1009","Không có quyền truy cập hồ sơ");
        if(editable&&("completed".equals(r.get("status"))||"cancelled".equals(r.get("status"))))throw new MaydayApiException("1010","Hồ sơ đã đóng"); return r;
    }
    private Map<String,Object> answer(Long id,TaiKhoan actor,boolean editable) {
        List<Map<String,Object>> rows=jdbc.query("SELECT a.answer_id,h.ma_benh_nhan patient_id,h.ma_ho_so,h.trang_thai FROM mayday_medical_record_answers a JOIN hosobenhan h ON h.ma_ho_so=a.medical_record_id WHERE a.answer_id=?",
                (rs,n)->Map.of("answer_id",rs.getLong(1),"patient_id",rs.getLong(2),"record_id",rs.getLong(3),"status",rs.getString(4)),id);
        if(rows.isEmpty())throw new MaydayApiException("9992","Không tìm thấy câu trả lời");Map<String,Object> a=rows.get(0);
        if(!auth.isStaff(actor)&&!Long.valueOf(actor.getNguoi().getMaNguoi()).equals(a.get("patient_id")))throw new MaydayApiException("1009","Không có quyền truy cập câu trả lời");
        if(editable&&("completed".equals(a.get("status"))||"cancelled".equals(a.get("status"))))throw new MaydayApiException("1010","Hồ sơ đã đóng");return a;
    }
    private MaydayAnswerImageResponse image(Long id) { List<MaydayAnswerImageResponse> rows=jdbc.query("SELECT answer_image_id,answer_id,attachment_id,option_value,created_at FROM mayday_answer_images WHERE answer_image_id=?",(rs,n)->image(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4),rs.getObject(5)),id);if(rows.isEmpty())throw new MaydayApiException("9992","Không tìm thấy liên kết ảnh");return rows.get(0); }
    private MaydayAnswerImageResponse image(Object id,Object answer,Object attachment,String option,Object created){String x=String.valueOf(id);return new MaydayAnswerImageResponse(x,x,String.valueOf(answer),String.valueOf(attachment),option,String.valueOf(created));}
    private long id(Object v,String field){if(v==null)throw new MaydayApiException("1002","Thiếu trường bắt buộc: "+field);if(!(v instanceof String)&&!(v instanceof Number)||v instanceof Number n&&n.doubleValue()%1!=0)throw new MaydayApiException("1003",field+" sai kiểu dữ liệu");try{long x=Long.parseLong(String.valueOf(v));if(x<=0)throw new MaydayApiException("1004",field+" phải lớn hơn 0");return x;}catch(NumberFormatException e){throw new MaydayApiException("1003",field+" sai định dạng");}}
    private String toJson(Object v){try{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(v);}catch(Exception e){throw new MaydayApiException("1003","answer_json sai định dạng");}}
    private Object jsonValue(Object value){if(value==null)return null;if(value instanceof String s){try{return new com.fasterxml.jackson.databind.ObjectMapper().readValue(s,Object.class);}catch(Exception ignored){return s;}}return value;}
    private void validateQuestion(long questionId,Long optionId){
        try {
            List<Long> options=jdbc.query("SELECT unnest(option_ids) FROM mayday_test_questions WHERE question_id=?",(rs,n)->rs.getLong(1),questionId);
            Integer found=jdbc.queryForObject("SELECT count(*) FROM mayday_test_questions WHERE question_id=?",Integer.class,questionId);
            if(found==0) throw new MaydayApiException("9992","Không tìm thấy câu hỏi");
            if(optionId!=null&&!options.contains(optionId)) throw new MaydayApiException("1004","Lựa chọn không thuộc câu hỏi");
        } catch(BadSqlGrammarException ignored) { /* Non-local deployments may use their own question catalog. */ }
    }
}
