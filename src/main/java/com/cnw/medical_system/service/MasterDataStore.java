package com.cnw.medical_system.service;

import com.cnw.medical_system.exception.ApiContractException;
import com.cnw.medical_system.entity.PhongKham;
import com.cnw.medical_system.repository.PhongKhamRepository;
import com.cnw.medical_system.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQL-backed store for organization and form metadata. */
@Component
public class MasterDataStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public MasterDataStore(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    public synchronized ArrayNode list(String collection) {
        String table = table(collection);
        String order = collection.equals("templates") ? "code_template" : "id";
        ArrayNode result = mapper.createArrayNode();
        jdbc.query("select payload from " + table + " order by " + order, (org.springframework.jdbc.core.RowCallbackHandler) rs -> result.add(read(rs)));
        return result;
    }

    public synchronized JsonNode find(String collection, String id) {
        String table = table(collection);
        String sql = collection.equals("templates")
                ? "select payload from " + table + " where code_template = ? or payload->>'id' = ?"
                : "select payload from " + table + " where id = ?";
        List<JsonNode> rows = collection.equals("templates")
                ? jdbc.query(sql, (rs, n) -> read(rs), id, id)
                : jdbc.query(sql, (rs, n) -> read(rs), parseLong(id));
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional
    public synchronized ObjectNode create(String collection, ObjectNode input) {
        ObjectNode value = input.deepCopy();
        String now = Instant.now().toString();
        value.put("created_at", now); value.put("updated_at", now);
        if (collection.equals("departments")) {
            long branchId = number(value, "branch_id", "branchId");
            value.put("branch_id", branchId); value.remove("branchId");
            Long id = jdbc.queryForObject("insert into drmayday_departments(name,branch_id,payload) values (?,?, '{}'::jsonb) returning id", Long.class, value.path("name").asText().trim(), branchId);
            value.put("id", id);
            jdbc.update("update drmayday_departments set payload=?::jsonb where id=?", json(value), id);
        } else {
            String code = value.path("code_template").asText();
            value.put("id", code); value.putIfAbsent("description", mapper.nullNode());
            if (!value.hasNonNull("version")) value.put("version", "1.0");
            if (!value.hasNonNull("is_active")) value.put("is_active", "1");
            if (!value.has("sections") || !value.path("sections").isArray()) value.putArray("sections");
            jdbc.update("insert into drmayday_templates(code_template,payload) values (?,?::jsonb)", code, json(value));
        }
        return value;
    }

    @Transactional
    public synchronized ObjectNode update(String collection, String id, ObjectNode changes) {
        ObjectNode current = (ObjectNode) find(collection, id);
        if (current == null) throw new ResourceNotFoundException("Không tìm thấy dữ liệu");
        String now = Instant.now().toString();
        for (String field : mutableFields(collection)) if (changes.has(field)) current.set(field, changes.get(field).deepCopy());
        current.put("updated_at", now);
        if (collection.equals("departments")) {
            long branchId = number(current, "branch_id", "branchId");
            jdbc.update("update drmayday_departments set name=?, branch_id=?, payload=?::jsonb where id=?", current.path("name").asText(), branchId, json(current), parseLong(current.path("id").asText()));
        } else {
            jdbc.update("update drmayday_templates set payload=?::jsonb where code_template=?", json(current), current.path("code_template").asText());
        }
        return current;
    }

    @Transactional
    public synchronized void delete(String collection, String id) {
        if (collection.equals("departments")) {
            if (find(collection,id) == null) throw new ResourceNotFoundException("Không tìm thấy dữ liệu");
            jdbc.update("delete from drmayday_departments where id=?", parseLong(id));
        } else {
            if (find(collection,id) == null) throw new ResourceNotFoundException("Không tìm thấy dữ liệu");
            if (isTemplateInUse(id)) throw new ApiContractException("1010", 409, "Biểu mẫu đang được sử dụng");
            jdbc.update("delete from drmayday_templates where code_template=(select code_template from drmayday_templates where code_template=? or payload->>'id'=?)", id, id);
        }
    }

    public boolean isTemplateInUse(String code) {
        Integer count = jdbc.queryForObject("select count(*) from drmayday_template_usage where code_template=?", Integer.class, code);
        return count != null && count > 0;
    }

    @Transactional
    public synchronized ObjectNode nestedCreate(String templateId, String parentId, String field, ObjectNode input) {
        ObjectNode template = requireTemplate(templateId);
        if (isTemplateInUse(templateId)) throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng");
        ObjectNode value = input.deepCopy();
        String now = Instant.now().toString();
        if (field.equals("sections")) {
            long id = nextNestedId();
            String title = value.has("title") ? value.path("title").asText() : value.path("name").asText();
            value.put("section_id", id); value.put("id", id); value.put("template_id", template.path("code_template").asText());
            value.put("title", title); value.remove("name");
            if (!value.hasNonNull("order")) value.put("order", "1");
            if (!value.hasNonNull("is_required")) value.put("is_required", "0");
            if (!value.hasNonNull("filled_by")) value.put("filled_by", "patient");
            value.put("created_at", now); value.put("updated_at", now); value.putArray("questions");
        } else {
            ObjectNode section = requireSection(template, parentId);
            normalizeQuestion(value, section, now);
            ArrayNode questions = array(section, "questions");
            for(JsonNode t:list("templates"))for(JsonNode s:t.path("sections"))for(JsonNode existing:s.path("questions"))if(existing.path("question_id").asText().equals(value.path("question_id").asText()))throw new ApiContractException("9996",409,"question_id đã tồn tại trong biểu mẫu");
            if (value.has("options") && value.path("options").isArray()) assignOptionIds((ArrayNode)value.path("options"));
            questions.add(value); saveTemplate(template); return value.deepCopy();
        }
        array(template, "sections").add(value); sortTemplate(template); saveTemplate(template); return value.deepCopy();
    }

    @Transactional
    public synchronized ObjectNode createQuestion(String sectionId, ObjectNode input) {
        for (JsonNode node : list("templates")) {
            ObjectNode template = (ObjectNode) node;
            ObjectNode section = findSection(template, sectionId);
            if (section != null) {
                if (isTemplateInUse(template.path("code_template").asText())) throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng");
                ObjectNode value = input.deepCopy(); normalizeQuestion(value, section, Instant.now().toString());
                for(JsonNode t:list("templates"))for(JsonNode s:t.path("sections"))for(JsonNode existing:s.path("questions"))if(existing.path("question_id").asText().equals(value.path("question_id").asText()))throw new ApiContractException("9996",409,"question_id đã tồn tại trong biểu mẫu");
                if (value.has("options") && value.path("options").isArray()) assignOptionIds((ArrayNode)value.path("options"));
                array(section,"questions").add(value); sortTemplate(template); saveTemplate(template); return value.deepCopy();
            }
        }
        throw new ResourceNotFoundException("Không tìm thấy phần biểu mẫu");
    }

    @Transactional
    public synchronized ObjectNode nestedUpdate(String templateId, String parentId, String field, String id, ObjectNode changes) {
        ObjectNode template = requireTemplate(templateId);
        if (isTemplateInUse(templateId)) throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng");
        if (field.equals("sections")) return editSection(template, id, changes);
        ObjectNode section = requireSection(template, parentId);
        return editQuestion(template, section, id, changes);
    }

    @Transactional
    public synchronized void nestedDelete(String templateId, String parentId, String field, String id) {
        ObjectNode template = requireTemplate(templateId);
        if (isTemplateInUse(templateId)) throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng");
        if (field.equals("sections")) {
            if (!removeById(array(template,"sections"), "section_id", id)) throw new ResourceNotFoundException("Không tìm thấy phần biểu mẫu");
        } else {
            ObjectNode section = requireSection(template,parentId);
            if (!removeById(array(section,"questions"), "question_id", id)) throw new ResourceNotFoundException("Không tìm thấy câu hỏi");
        }
        sortTemplate(template); saveTemplate(template);
    }

    @Transactional
    public synchronized ObjectNode updateSection(String id, ObjectNode changes) {
        for (JsonNode node : list("templates")) { ObjectNode template=(ObjectNode)node; if(findSection(template,id)!=null) { if(isTemplateInUse(template.path("code_template").asText())) throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng"); return editSection(template,id,changes); } }
        throw new ResourceNotFoundException("Không tìm thấy phần biểu mẫu");
    }
    @Transactional public synchronized void deleteSection(String id) {
        for(JsonNode node:list("templates")){ObjectNode template=(ObjectNode)node;if(findSection(template,id)!=null){if(isTemplateInUse(template.path("code_template").asText()))throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng");removeById(array(template,"sections"),"section_id",id);saveTemplate(template);return;}}
        throw new ResourceNotFoundException("Không tìm thấy phần biểu mẫu");
    }
    @Transactional public synchronized ObjectNode updateQuestion(String id,ObjectNode changes) {
        for(JsonNode node:list("templates")){ObjectNode template=(ObjectNode)node;for(JsonNode s:array(template,"sections")){ObjectNode section=(ObjectNode)s; if(findQuestion(section,id)!=null){if(isTemplateInUse(template.path("code_template").asText()))throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng");return editQuestion(template,section,id,changes);}}}
        throw new ResourceNotFoundException("Không tìm thấy câu hỏi");
    }
    @Transactional public synchronized void deleteQuestion(String id) {
        for(JsonNode node:list("templates")){ObjectNode template=(ObjectNode)node;for(JsonNode s:array(template,"sections")){ObjectNode section=(ObjectNode)s;if(findQuestion(section,id)!=null){if(isTemplateInUse(template.path("code_template").asText()))throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng");for(JsonNode other:array(section,"questions"))if(id.equals(other.path("parent_question_id").asText()))((ObjectNode)other).putNull("parent_question_id");removeById(array(section,"questions"),"question_id",id);saveTemplate(template);return;}}}
        throw new ResourceNotFoundException("Không tìm thấy câu hỏi");
    }

    public synchronized void saveRoomMeta(String roomId,String departmentId,String description) {
        jdbc.update("insert into drmayday_room_metadata(room_id,department_id,description) values (?,?,?) on conflict(room_id) do update set department_id=excluded.department_id,description=excluded.description",parseLong(roomId),departmentId==null?null:parseLong(departmentId),description);
    }
    public synchronized void removeRoomMeta(String roomId) { jdbc.update("delete from drmayday_room_metadata where room_id=?",parseLong(roomId)); }
    public synchronized JsonNode roomMeta(String roomId) {
        List<JsonNode> rows=jdbc.query("select department_id,description from drmayday_room_metadata where room_id=?",(rs,n)->{ObjectNode x=mapper.createObjectNode();long id=rs.getLong("department_id");if(rs.wasNull())x.putNull("department_id");else x.put("department_id",Long.toString(id));String d=rs.getString("description");if(d==null)x.putNull("description");else x.put("description",d);return x;},parseLong(roomId));
        return rows.isEmpty()?mapper.createObjectNode():rows.get(0);
    }
    public boolean hasDepartmentForBranch(long branchId) { Integer n=jdbc.queryForObject("select count(*) from drmayday_departments where branch_id=?",Integer.class,branchId);return n!=null&&n>0; }
    public void clearRoomDepartment(String departmentId) { jdbc.update("update drmayday_room_metadata set department_id=null where department_id=?",parseLong(departmentId)); }
    public JsonNode findDepartment(String id) { return find("departments",id); }
    public boolean departmentNameExists(String name,long branchId,long exceptId) { Integer n=jdbc.queryForObject("select count(*) from drmayday_departments where branch_id=? and lower(name)=lower(?) and id<>?",Integer.class,branchId,name,exceptId);return n!=null&&n>0; }
    public boolean departmentNameExists(String name,long branchId) { Integer n=jdbc.queryForObject("select count(*) from drmayday_departments where branch_id=? and lower(name)=lower(?)",Integer.class,branchId,name);return n!=null&&n>0; }
    public List<PhongKham> roomsForDepartment(long departmentId, PhongKhamRepository repository) { return repository.findAll().stream().filter(r->Long.toString(departmentId).equals(roomMeta(Long.toString(r.getId())).path("department_id").asText())).toList(); }
    public boolean roomNameExists(long departmentId,String name,long exceptId) { Integer n=jdbc.queryForObject("select count(*) from phongkham r join drmayday_room_metadata m on m.room_id=r.ma_phong_kham where m.department_id=? and lower(r.ten_phong_kham)=lower(?) and r.ma_phong_kham<>?",Integer.class,departmentId,name,exceptId);return n!=null&&n>0; }
    public boolean roomNameExists(long departmentId,String name) { Integer n=jdbc.queryForObject("select count(*) from phongkham r join drmayday_room_metadata m on m.room_id=r.ma_phong_kham where m.department_id=? and lower(r.ten_phong_kham)=lower(?)",Integer.class,departmentId,name);return n!=null&&n>0; }

    private ObjectNode editSection(ObjectNode template,String id,ObjectNode changes) {
        ObjectNode section=requireSection(template,id);
        for(String k:List.of("title","order","is_required","filled_by"))if(changes.has(k))section.set(k,changes.get(k).deepCopy());
        if(changes.has("title"))section.put("updated_at",Instant.now().toString());
        sortTemplate(template);saveTemplate(template);return section.deepCopy();
    }
    private ObjectNode editQuestion(ObjectNode template,ObjectNode section,String id,ObjectNode changes) {
        ObjectNode question=requireQuestion(section,id);
        for(String k:List.of("question_text","order","is_required","placeholder","help_text","allow_image","show_if_answer","trigger_image_upload"))if(changes.has(k))question.set(k,changes.get(k).deepCopy());
        if(changes.has("options")&&changes.path("options").isArray()){
            ArrayNode opts=(ArrayNode)changes.path("options"),saved=mapper.createArrayNode();
            for(JsonNode candidate:opts){ObjectNode option=((ObjectNode)candidate).deepCopy();if(option.hasNonNull("id")){JsonNode old=null;for(JsonNode current:array(question,"options"))if(option.path("id").asText().equals(current.path("id").asText())){old=current;break;}if(old==null)throw new ResourceNotFoundException("Không tìm thấy lựa chọn");}else option.put("id",nextNestedId());saved.add(option);}
            question.set("options",saved);
        }
        question.put("updated_at",Instant.now().toString());sortTemplate(template);saveTemplate(template);return question.deepCopy();
    }
    private void normalizeQuestion(ObjectNode value,ObjectNode section,String now) {
        value.put("section_id",section.path("section_id").asLong());
        if(!value.hasNonNull("order"))value.put("order","1");
        if(!value.hasNonNull("is_required"))value.put("is_required","0");
        if(!value.hasNonNull("allow_image"))value.put("allow_image","0");
        if(!value.hasNonNull("trigger_image_upload"))value.put("trigger_image_upload","0");
        value.put("created_at",now);value.put("updated_at",now);
    }
    private void assignOptionIds(ArrayNode options) { for(JsonNode n:options)if(n instanceof ObjectNode o&&!o.hasNonNull("id"))o.put("id",nextNestedId()); }
    private long nextNestedId(){return jdbc.queryForObject("select nextval('drmayday_nested_id_seq')",Long.class);}
    private ObjectNode requireTemplate(String code){JsonNode n=find("templates",code);if(n==null)throw new ResourceNotFoundException("Không tìm thấy biểu mẫu");return (ObjectNode)n;}
    private ObjectNode requireSection(ObjectNode template,String id){ObjectNode s=findSection(template,id);if(s==null)throw new ResourceNotFoundException("Không tìm thấy phần biểu mẫu");return s;}
    private ObjectNode requireQuestion(ObjectNode section,String id){ObjectNode q=findQuestion(section,id);if(q==null)throw new ResourceNotFoundException("Không tìm thấy câu hỏi");return q;}
    private ObjectNode findSection(ObjectNode template,String id){for(JsonNode n:array(template,"sections"))if(id.equals(n.path("section_id").asText())||id.equals(n.path("id").asText()))return (ObjectNode)n;return null;}
    private ObjectNode findQuestion(ObjectNode section,String id){for(JsonNode n:array(section,"questions"))if(id.equals(n.path("question_id").asText()))return (ObjectNode)n;return null;}
    private boolean removeById(ArrayNode rows,String key,String id){for(int i=0;i<rows.size();i++)if(id.equals(rows.get(i).path(key).asText())||id.equals(rows.get(i).path("id").asText())){rows.remove(i);return true;}return false;}
    private ArrayNode array(ObjectNode node,String field){if(!node.has(field)||!node.path(field).isArray())node.set(field,mapper.createArrayNode());return (ArrayNode)node.path(field);}
    private void sortTemplate(ObjectNode template) {
        for (JsonNode sn : array(template, "sections")) {
            ObjectNode section = (ObjectNode) sn;
            section.set("questions", sorted(array(section, "questions")));
        }
        template.set("sections", sorted(array(template, "sections")));
    }
    private ArrayNode sorted(ArrayNode source) {
        List<JsonNode> values = new ArrayList<>();
        source.forEach(values::add);
        values.sort(Comparator.comparingLong(n -> n.path("order").asLong(0)));
        ArrayNode result = mapper.createArrayNode();
        values.forEach(result::add);
        return result;
    }
    private void saveTemplate(ObjectNode template){jdbc.update("update drmayday_templates set payload=?::jsonb where code_template=?",json(template),template.path("code_template").asText());}
    private long number(ObjectNode n,String first,String second){JsonNode v=n.has(first)?n.path(first):n.path(second);try{return Long.parseLong(v.asText());}catch(Exception e){throw new ApiContractException("1004",400,"ID tham chiếu không hợp lệ");}}
    private long parseLong(String value){try{return Long.parseLong(value);}catch(Exception e){throw new ResourceNotFoundException("Không tìm thấy dữ liệu");}}
    private String json(JsonNode node){try{return mapper.writeValueAsString(node);}catch(Exception e){throw new IllegalStateException("Không thể mã hóa dữ liệu",e);}}
    private JsonNode read(ResultSet rs)throws SQLException{try{return mapper.readTree(rs.getString("payload"));}catch(Exception e){throw new SQLException("Dữ liệu JSON trong PostgreSQL không hợp lệ",e);}}
    private String table(String collection){return switch(collection){case "departments"->"drmayday_departments";case "templates"->"drmayday_templates";default->throw new IllegalArgumentException("Danh mục không hợp lệ");};}
    private List<String> mutableFields(String collection){return collection.equals("departments")?List.of("name","description","branch_id"):List.of("name","description","version","is_active");}
}
