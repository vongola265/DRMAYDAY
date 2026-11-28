package com.cnw.medical_system.service;

import com.cnw.medical_system.dto.request.BranchRequest;
import com.cnw.medical_system.dto.request.RoomRequest;
import com.cnw.medical_system.entity.ChiNhanh;
import com.cnw.medical_system.entity.PhongKham;
import com.cnw.medical_system.repository.ChiNhanhRepository;
import com.cnw.medical_system.repository.PhongKhamRepository;
import com.cnw.medical_system.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.cnw.medical_system.exception.ApiContractException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterDataService {
    private final ChiNhanhRepository branches;
    private final PhongKhamRepository rooms;
    private final MasterDataStore store;
    private final ObjectMapper mapper;

    public boolean branchNameExists(String name) { return branches.existsByNameIgnoreCase(name); }
    public boolean branchNameExists(String name, long exceptId) { return branches.existsByNameIgnoreCaseAndIdNot(name, exceptId); }
    @Transactional(readOnly=true) public List<ChiNhanh> branches() { return branches.findAll(); }
    @Transactional(readOnly=true) public ChiNhanh branch(long id) { return branches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh")); }
    @Transactional public ChiNhanh createBranch(BranchRequest r) { return branches.save(ChiNhanh.builder().name(clean(r.name())).address(clean(r.address())).phone(cleanNullable(r.phone())).active(r.active()==null?true:r.active()).build()); }
    @Transactional public ChiNhanh updateBranch(long id, BranchRequest r) { ChiNhanh b=branch(id); b.setName(clean(r.name())); b.setAddress(clean(r.address())); b.setPhone(cleanNullable(r.phone())); if(r.active()!=null)b.setActive(r.active()); return branches.save(b); }
    @Transactional public void deleteBranch(long id) {
        ChiNhanh b=branch(id);
        if(rooms.existsByBranchId(id)||store.hasDepartmentForBranch(id)) throw new ApiContractException("1009", 403, "Không thể xóa chi nhánh đang được sử dụng");
        branches.delete(b);
    }
    @Transactional public ChiNhanh editBranch(long id, ObjectNode body) {
        ChiNhanh branch = branch(id);
        if (body.has("name")) branch.setName(body.path("name").asText());
        if (body.has("address")) branch.setAddress(body.path("address").asText());
        if (body.has("phone")) branch.setPhone(body.path("phone").asText());
        return branches.save(branch);
    }
    @Transactional(readOnly=true) public List<PhongKham> rooms(Long branchId) { return branchId==null?rooms.findAll():rooms.findByBranchId(branchId); }
    @Transactional(readOnly=true) public PhongKham room(long id) { return rooms.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy phòng khám")); }
    @Transactional public synchronized PhongKham createRoom(RoomRequest r) { JsonNode dept=requiredDepartment(r.department_id()); long departmentId=Long.parseLong(dept.path("id").asText()); long branchId=dept.path("branch_id").asLong(dept.path("branchId").asLong()); branch(branchId); if(store.roomNameExists(departmentId,clean(r.name())))throw new ApiContractException("9996",409,"Tên phòng đã tồn tại trong ban"); PhongKham saved=rooms.save(PhongKham.builder().branchId(branchId).name(clean(r.name())).build()); store.saveRoomMeta(saved.getId().toString(),Long.toString(departmentId),cleanNullable(r.description())); return saved; }
    @Transactional public synchronized PhongKham updateRoom(long id, RoomRequest r) { PhongKham x=room(id); JsonNode dept=requiredDepartment(r.department_id()); long departmentId=Long.parseLong(dept.path("id").asText()); long branchId=dept.path("branch_id").asLong(dept.path("branchId").asLong()); branch(branchId); if(store.roomNameExists(departmentId,clean(r.name()),id))throw new ApiContractException("9996",409,"Tên phòng đã tồn tại trong ban"); x.setBranchId(branchId); x.setName(clean(r.name())); PhongKham saved=rooms.save(x); store.saveRoomMeta(saved.getId().toString(),Long.toString(departmentId),cleanNullable(r.description())); return saved; }
    @Transactional public synchronized PhongKham editRoom(long id,ObjectNode changes) { PhongKham current=room(id); String name=changes.has("name")?changes.path("name").asText():current.getName(); String deptId=changes.has("department_id")?changes.path("department_id").asText():store.roomMeta(Long.toString(id)).path("department_id").asText(); JsonNode dept=requiredDepartment(deptId); long branchId=dept.path("branch_id").asLong(); long departmentId=Long.parseLong(dept.path("id").asText()); if(store.roomNameExists(departmentId,name,id))throw new ApiContractException("9996",409,"Tên phòng đã tồn tại trong ban"); current.setName(clean(name)); current.setBranchId(branchId); PhongKham saved=rooms.save(current); JsonNode old=store.roomMeta(Long.toString(id)); String description=changes.has("description")?(changes.path("description").isNull()?null:changes.path("description").asText()):old.path("description").asText(null); store.saveRoomMeta(Long.toString(id),Long.toString(departmentId),cleanNullable(description)); return saved; }
    public List<PhongKham> roomsForDepartment(Long departmentId) { return departmentId==null?rooms.findAll():store.roomsForDepartment(departmentId,rooms); }
    @Transactional public void deleteRoom(long id) { rooms.delete(room(id)); store.removeRoomMeta(Long.toString(id)); }
    public JsonNode roomMeta(long id) { return store.roomMeta(Long.toString(id)); }

    public JsonNode list(String resource) { return store.list(resource); }
    public JsonNode detail(String resource,String id) { JsonNode n=store.find(resource,id); if(n==null)throw new ResourceNotFoundException("Không tìm thấy dữ liệu"); return n; }
    public JsonNode create(String resource,ObjectNode body) { validateCatalog(resource,body,null); if(resource.equals("departments")) { long branchId=body.path("branch_id").asLong(); branch(branchId); if(store.departmentNameExists(body.path("name").asText().trim(),branchId))throw new ApiContractException("9996",409,"Tên ban đã tồn tại trong chi nhánh"); } return store.create(resource,body); }
    public JsonNode update(String resource,String id,ObjectNode body) { JsonNode current=detail(resource,id); validateCatalog(resource,body,id); if(resource.equals("departments")) { long branchId=body.has("branch_id")?body.path("branch_id").asLong():current.path("branch_id").asLong(); branch(branchId); String name=body.has("name")?body.path("name").asText():current.path("name").asText(); if(store.departmentNameExists(name.trim(),branchId,Long.parseLong(id)))throw new ApiContractException("9996",409,"Tên ban đã tồn tại trong chi nhánh"); } return store.update(resource,id,body); }
    public void delete(String resource,String id) { detail(resource,id); if(resource.equals("departments"))store.clearRoomDepartment(id); store.delete(resource,id); }
    public JsonNode addNested(String templateId,String parent,String field,ObjectNode body) { JsonNode template=detail("templates",templateId);JsonNode section=parent==null?null:findSection(templateId,parent);if(parent!=null&&section==null)throw new ResourceNotFoundException("Không tìm thấy phần biểu mẫu");if(field.equals("sections"))validateSection(body,true);else{validateQuestion(body,true);validateParent(template,section,body);}if(store.isTemplateInUse(templateId))throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng");return store.nestedCreate(templateId,parent,field,body); }
    public JsonNode updateNested(String templateId,String parent,String field,String id,ObjectNode body) { JsonNode template=detail("templates",templateId);JsonNode section=parent==null?null:findSection(templateId,parent);if(parent!=null&&section==null)throw new ResourceNotFoundException("Không tìm thấy phần biểu mẫu");if(body==null||body.isEmpty())throw contract("1002","Cần ít nhất một trường để cập nhật");if(field.equals("sections"))validateSection(body,false);else{JsonNode old=findQuestionNode(template,id);ObjectNode safe=questionChanges(body);ObjectNode merged=((ObjectNode)old).deepCopy();safe.fields().forEachRemaining(e->merged.set(e.getKey(),e.getValue()));validateQuestion(merged,true);validateParent(template,section,merged);body=safe;}if(store.isTemplateInUse(templateId))throw new ApiContractException("1010",409,"Biểu mẫu đang được sử dụng");return store.nestedUpdate(templateId,parent,field,id,body); }
    public void deleteNested(String templateId,String parent,String field,String id) { detail("templates",templateId); if(parent!=null&&findSection(templateId,parent)==null)throw new IllegalArgumentException("Không tìm thấy phần biểu mẫu"); store.nestedDelete(templateId,parent,field,id); }
    public JsonNode createQuestion(String sectionId,ObjectNode body) {JsonNode section=findSectionNode(sectionId),template=findTemplateForSection(sectionId);validateQuestion(body,true);validateParent(template,section,body);return store.createQuestion(sectionId,body); }
    public JsonNode updateSection(String id,ObjectNode body) { if(body==null||body.isEmpty())throw contract("1002","Cần ít nhất một trường để cập nhật");validateSection(body,false);return store.updateSection(id,body); }
    public void deleteSection(String id) { store.deleteSection(id); }
    public JsonNode updateQuestion(String id,ObjectNode body) {if(body==null||body.isEmpty())throw contract("1002","Cần ít nhất một trường để cập nhật");if(body.has("options")&&body.path("options").isArray())for(JsonNode option:body.path("options")){if(!option.isObject())throw contract("1003","Mỗi lựa chọn phải là object");for(String key:List.of("option_text","option_value","order"))if(option.has(key)&&!option.path(key).isNull()&&!option.path(key).isTextual())throw contract("1003",key+" phải là chuỗi");for(String key:List.of("option_text","option_value","order"))if(!option.hasNonNull(key))throw contract("1004","Lựa chọn thiếu trường "+key);}JsonNode old=findQuestionNodeAnywhere(id);ObjectNode safe=questionChanges(body);ObjectNode merged=((ObjectNode)old).deepCopy();safe.fields().forEachRemaining(e->merged.set(e.getKey(),e.getValue()));validateQuestion(merged,true);return store.updateQuestion(id,safe); }
    public void deleteQuestion(String id) { store.deleteQuestion(id); }
    private JsonNode findSection(String templateId,String sectionId) { for(JsonNode s:detail("templates",templateId).path("sections"))if(sectionId.equals(s.path("section_id").asText())||sectionId.equals(s.path("id").asText()))return s; return null; }
    private JsonNode findSectionNode(String id){for(JsonNode t:store.list("templates"))for(JsonNode s:t.path("sections"))if(id.equals(s.path("section_id").asText())||id.equals(s.path("id").asText()))return s;throw new ResourceNotFoundException("Không tìm thấy phần biểu mẫu");}
    private JsonNode findTemplateForSection(String id){for(JsonNode t:store.list("templates"))for(JsonNode s:t.path("sections"))if(id.equals(s.path("section_id").asText())||id.equals(s.path("id").asText()))return t;throw new ResourceNotFoundException("Không tìm thấy phần biểu mẫu");}
    private JsonNode findQuestionNode(JsonNode t,String id){for(JsonNode s:t.path("sections"))for(JsonNode q:s.path("questions"))if(id.equals(q.path("question_id").asText()))return q;throw new ResourceNotFoundException("Không tìm thấy câu hỏi");}
    private JsonNode findQuestionNodeAnywhere(String id){for(JsonNode t:store.list("templates"))for(JsonNode s:t.path("sections"))for(JsonNode q:s.path("questions"))if(id.equals(q.path("question_id").asText()))return q;throw new ResourceNotFoundException("Không tìm thấy câu hỏi");}
    private void validateCatalog(String resource,ObjectNode body,String excludedId) {
        if (excludedId == null || body.has("name")) required(body,"name",resource.equals("templates")?"Tên biểu mẫu":"Tên ban");
        if(resource.equals("templates")) {
            if (excludedId == null) required(body,"code_template","Mã biểu mẫu");
            if (body.has("code_template")) for(JsonNode existing:store.list("templates")) if(body.path("code_template").asText().equalsIgnoreCase(existing.path("code_template").asText())&&!existing.path("id").asText().equals(excludedId)) throw new ApiContractException("9996",409,"Mã biểu mẫu đã tồn tại");
        }
        if(resource.equals("departments")) {
            JsonNode branch = body.has("branch_id")?body.path("branch_id"):body.path("branchId");
            if (excludedId == null || body.has("branch_id") || body.has("branchId")) {
                if(!branch.isIntegralNumber()) throw new ApiContractException("1003",400,"branch_id phải là số nguyên");
                if(branch.asLong()<=0) throw new ApiContractException("1004",400,"branch_id phải là số dương");
                branch(branch.asLong());
            }
        }
    }
    private void required(ObjectNode n,String key,String label) { if(!n.hasNonNull(key)||n.path(key).asText().isBlank())throw contract("1002",label+" không được để trống"); }
    private void validateSection(ObjectNode b,boolean create){
        List<String> fields=List.of("title","order","is_required","filled_by");
        if(create)for(String k:List.of("title","order"))if(!b.hasNonNull(k))throw contract("1002",k+" là bắt buộc");
        for(String k:fields)if(b.has(k)&&!b.path(k).isTextual())throw contract("1003",k+" phải là chuỗi");
        if(b.has("title")){String v=b.path("title").asText();if(v.isBlank()||v.length()>200||v.contains("<"))throw contract("1004","title không hợp lệ");}
        if(b.has("order"))validOrder(b.path("order").asText());
        if(b.has("is_required")&&!List.of("0","1").contains(b.path("is_required").asText()))throw contract("1004","is_required không hợp lệ");
        if(b.has("filled_by")&&!List.of("patient","doctor","patdoc").contains(b.path("filled_by").asText()))throw contract("1004","filled_by không hợp lệ");
    }
    private void validateQuestion(ObjectNode b,boolean create){
        List<String> requiredFields=List.of("question_id","question_text","type_question","order");
        if(create)for(String k:requiredFields)if(!b.hasNonNull(k))throw contract("1002",k+" là bắt buộc");
        List<String> strings=List.of("question_id","question_text","type_question","order","is_required","placeholder","help_text","allow_image","show_if_answer","trigger_image_upload","parent_question_id");
        for(String k:strings)if(b.has(k)&&!b.path(k).isNull()&&!b.path(k).isTextual())throw contract("1003",k+" phải là chuỗi");
        if(b.has("question_id")&&!b.path("question_id").asText().matches("[A-Z][A-Z0-9_]{1,48}"))throw contract("1004","question_id không hợp lệ");
        if(b.has("question_text")){String v=b.path("question_text").asText();if(v.isBlank()||v.length()>500||v.contains("<"))throw contract("1004","question_text không hợp lệ");}
        String type=b.path("type_question").asText();List<String> types=List.of("text","textarea","number","date","radio","checkbox","select");
        if(b.has("type_question")&&!types.contains(type))throw contract("1004","type_question không hợp lệ");
        if(b.has("order"))validOrder(b.path("order").asText());
        for(String flag:List.of("is_required","allow_image","trigger_image_upload"))if(b.has(flag)&&!List.of("0","1").contains(b.path(flag).asText()))throw contract("1004",flag+" không hợp lệ");
        if(b.has("show_if_answer")){try{mapper.readTree(b.path("show_if_answer").asText());}catch(Exception e){throw contract("1004","show_if_answer phải là JSON hợp lệ");}}
        boolean choice=List.of("radio","checkbox","select").contains(type);
        if(b.has("options")){
            if(!b.path("options").isArray())throw contract("1003","options phải là mảng");
            ArrayNode options=(ArrayNode)b.path("options");if(options.size()>50)throw contract("1004","Tối đa 50 lựa chọn");
            for(JsonNode option:options){if(!option.isObject())throw contract("1003","Mỗi lựa chọn phải là object");for(String k:List.of("option_text","option_value","order"))if(option.has(k)&&!option.path(k).isNull()&&!option.path(k).isTextual())throw contract("1003",k+" phải là chuỗi");}
            if(!choice)throw contract("1004","options chỉ dùng với câu hỏi lựa chọn");
            java.util.Set<String> values=new java.util.HashSet<>();
            for(JsonNode option:options){if(!option.isObject())throw contract("1003","Mỗi lựa chọn phải là object");ObjectNode o=(ObjectNode)option;
                for(String k:List.of("option_text","option_value","order"))if(!o.hasNonNull(k))throw contract("1002",k+" là bắt buộc");
                String text=o.path("option_text").asText(),value=o.path("option_value").asText();if(text.isBlank()||text.length()>200||text.contains("<")||value.isBlank()||value.length()>100||value.contains("<"))throw contract("1004","Lựa chọn không hợp lệ");
                validOrder(o.path("order").asText());if(!values.add(value))throw contract("1004","Giá trị lựa chọn bị trùng");
            }
            if(choice&&options.isEmpty())throw contract("1004","Câu hỏi lựa chọn cần options");
        }else if(create&&choice)throw contract("1004","Câu hỏi lựa chọn cần options");
    }
    private void validateParent(JsonNode template,JsonNode section,ObjectNode question){if(!question.hasNonNull("parent_question_id"))return;String parent=question.path("parent_question_id").asText();if(parent.isBlank())return;for(JsonNode t:store.list("templates"))for(JsonNode s:t.path("sections"))for(JsonNode q:s.path("questions"))if(parent.equals(q.path("question_id").asText())){if(!t.path("code_template").asText().equals(template.path("code_template").asText()))throw contract("1004","Câu hỏi cha phải cùng biểu mẫu");return;}throw new ResourceNotFoundException("Không tìm thấy câu hỏi cha");}
    private void validOrder(String value){try{int n=Integer.parseInt(value);if(n<0||n>9999)throw contract("1004","order ngoài phạm vi");}catch(NumberFormatException e){throw contract("1004","order không hợp lệ");}}
    private ObjectNode questionChanges(ObjectNode body){ObjectNode copy=body.deepCopy();copy.remove(List.of("id","question_id","section_id","type_question","type","parent_question_id","created_at","updated_at"));return copy;}
    private ApiContractException contract(String code,String message){return new ApiContractException(code,400,message);}
    private String clean(String s) { return s==null?null:s.trim(); }
    private String cleanNullable(String s) { return s==null||s.isBlank()?null:s.trim(); }
    private JsonNode requiredDepartment(String id) { JsonNode d=store.findDepartment(id); if(d==null)throw new ResourceNotFoundException("Không tìm thấy khoa"); return d; }
}
