package com.cnw.medical_system.controller;

import com.cnw.medical_system.dto.request.BranchRequest;
import com.cnw.medical_system.dto.request.RoomRequest;
import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.entity.ChiNhanh;
import com.cnw.medical_system.entity.PhongKham;
import com.cnw.medical_system.exception.ApiContractException;
import com.cnw.medical_system.service.MasterDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api", "/api/v1"})
public class MasterDataController {
    private final MasterDataService service;
    public MasterDataController(MasterDataService service) { this.service = service; }

    @GetMapping("/branches") public ApiResponse<List<Map<String,Object>>> branches() { return ApiResponse.ok(service.branches().stream().map(this::branchDto).toList()); }
    @GetMapping("/branches/{id}") public ApiResponse<Map<String,Object>> branch(@PathVariable long id) { positiveId(id); return ApiResponse.ok(branchDto(service.branch(id))); }
    @PostMapping("/branches") public ApiResponse<Map<String,Object>> createBranch(@RequestBody(required=false) ObjectNode body) {
        if (body == null) throw error("1002",400,"Thiếu thông tin chi nhánh");
        String name = requiredString(body,"name"); String address = requiredString(body,"address"); String phone = requiredString(body,"phone");
        validateBranch(name,address,phone);
        if(service.branchNameExists(name.trim())) throw error("9996",409,"Tên chi nhánh đã tồn tại");
        return ApiResponse.ok(branchDto(service.createBranch(new BranchRequest(name.trim(),address.trim(),phone.trim(),true))));
    }
    @PutMapping("/branches/{id}") public ApiResponse<Map<String,Object>> updateBranch(@PathVariable long id,@RequestBody(required=false) ObjectNode body) {
        positiveId(id); service.branch(id); if(body==null||body.isEmpty())throw error("1002",400,"Cần ít nhất một trường để cập nhật");
        for(String k:List.of("name","address","phone")) if(body.has(k)&&!body.path(k).isTextual())throw error("1003",400,"Kiểu dữ liệu không hợp lệ: "+k);
        if(body.has("name")&&(body.path("name").asText().isBlank()||body.path("name").asText().length()>100||body.path("name").asText().contains("<")))throw error("1004",400,"Tên chi nhánh không hợp lệ");
        if(body.has("address")&&(body.path("address").asText().isBlank()||body.path("address").asText().length()>255))throw error("1004",400,"Địa chỉ chi nhánh không hợp lệ");
        if(body.has("phone")&&!validPhone(body.path("phone").asText()))throw error("1004",400,"Số điện thoại không hợp lệ");
        if(body.has("name")&&service.branchNameExists(body.path("name").asText().trim(),id))throw error("9996",409,"Tên chi nhánh đã tồn tại");
        ObjectNode changes=body.deepCopy();changes.remove(List.of("id","created_at","updated_at","active"));
        return ApiResponse.ok(branchDto(service.editBranch(id,changes)));
    }
    @DeleteMapping("/branches/{id}") public ApiResponse<Map<String,Object>> deleteBranch(@PathVariable long id){positiveId(id);service.deleteBranch(id);return ApiResponse.ok(Map.of());}

    @GetMapping("/departments") public Map<String,Object> departments(@RequestParam(name="branch_id",required=false) String branchId){
        Long filter=branchId==null?null:parseQueryId(branchId);List<JsonNode> rows=new ArrayList<>();for(JsonNode d:service.list("departments")){if(filter==null||Long.toString(filter).equals(d.path("branch_id").asText()))rows.add(d);}
        return listResponse(rows);
    }
    @GetMapping("/departments/{id}") public ApiResponse<JsonNode> department(@PathVariable String id){return ApiResponse.ok(service.detail("departments",Long.toString(pathId(id))));}
    @PostMapping("/departments") public ApiResponse<JsonNode> createDepartment(@RequestBody(required=false) ObjectNode body){
        requireBody(body);String name=requiredString(body,"name");JsonNode branch=required(body,"branch_id");if(!branch.isIntegralNumber())throw error("1003",400,"branch_id phải là số nguyên");long branchId=branch.asLong();validateName(name,100,"Tên ban");optionalText(body,"description",500);if(branchId<=0)throw error("1004",400,"branch_id phải là số dương");
        return ApiResponse.ok(service.create("departments",body));
    }
    @PutMapping("/departments/{id}") public ApiResponse<JsonNode> updateDepartment(@PathVariable String id,@RequestBody(required=false) ObjectNode body){
        long departmentId=pathId(id);JsonNode current=service.detail("departments",Long.toString(departmentId));if(body==null||body.isEmpty())throw error("1002",400,"Cần ít nhất một trường để cập nhật");
        if(body.has("name")){String name=typedString(body,"name");validateName(name,100,"Tên ban");}
        if(body.has("description"))optionalText(body,"description",500);
        if(body.has("branch_id")){if(!body.path("branch_id").isIntegralNumber())throw error("1003",400,"branch_id phải là số nguyên");if(body.path("branch_id").asLong()<=0)throw error("1004",400,"branch_id phải là số dương");}
        ObjectNode changes=body.deepCopy();changes.remove(List.of("id","created_at","updated_at","branchId"));return ApiResponse.ok(service.update("departments",Long.toString(departmentId),changes));
    }
    @DeleteMapping("/departments/{id}") public ApiResponse<Map<String,Object>> deleteDepartment(@PathVariable String id){service.delete("departments",Long.toString(pathId(id)));return ApiResponse.ok(Map.of());}

    @GetMapping("/rooms") public ApiResponse<List<Map<String,Object>>> rooms(@RequestParam(name="department_id",required=false) String departmentId,@RequestParam(name="branch_id",required=false) String branchId){
        Long filter=null;if(departmentId!=null)filter=parseQueryId(departmentId);List<PhongKham> values=service.roomsForDepartment(filter);if(branchId!=null){long b=parseQueryId(branchId);values=values.stream().filter(r->r.getBranchId()==b).toList();}return ApiResponse.ok(values.stream().map(this::roomDto).toList());
    }
    @GetMapping("/rooms/{id}") public ApiResponse<Map<String,Object>> room(@PathVariable long id){positiveId(id);return ApiResponse.ok(roomDto(service.room(id)));}
    @PostMapping("/rooms") public ApiResponse<Map<String,Object>> createRoom(@RequestBody(required=false) ObjectNode body){
        requireBody(body);String name=requiredString(body,"name");JsonNode dept=required(body,"department_id");if(!dept.isIntegralNumber())throw error("1003",400,"department_id phải là số nguyên");long departmentId=dept.asLong();validateName(name,100,"Tên phòng");String description=optionalText(body,"description",500);if(departmentId<=0)throw error("1004",400,"department_id phải là số dương");
        return ApiResponse.ok(roomDto(service.createRoom(new RoomRequest(Long.toString(departmentId),name.trim(),description))));
    }
    @PutMapping("/rooms/{id}") public ApiResponse<Map<String,Object>> updateRoom(@PathVariable long id,@RequestBody(required=false) ObjectNode body){
        positiveId(id);service.room(id);if(body==null||body.isEmpty())throw error("1002",400,"Cần ít nhất một trường để cập nhật");
        if(body.has("name")){String name=typedString(body,"name");validateName(name,100,"Tên phòng");}
        if(body.has("description"))optionalText(body,"description",500);
        if(body.has("department_id")){if(body.path("department_id").isNull())throw error("1004",400,"department_id phải là số dương");if(!body.path("department_id").isIntegralNumber())throw error("1003",400,"department_id phải là số nguyên");if(body.path("department_id").asLong()<=0)throw error("1004",400,"department_id phải là số dương");}
        return ApiResponse.ok(roomDto(service.editRoom(id,body)));
    }
    @DeleteMapping("/rooms/{id}") public ApiResponse<Map<String,Object>> deleteRoom(@PathVariable long id){positiveId(id);service.deleteRoom(id);return ApiResponse.ok(Map.of());}

    @GetMapping({"/templates","/templates/all"}) public Map<String,Object> templates(@RequestParam(name="limit",required=false) String limit,@RequestParam(name="page",required=false) String page){
        int size=pageNumber(limit,100,1,100),number=pageNumber(page,1,1,Integer.MAX_VALUE);List<JsonNode> rows=new ArrayList<>();service.list("templates").forEach(rows::add);int start=Math.min(rows.size(),(number-1)*size);List<JsonNode> pageRows=rows.subList(start,Math.min(rows.size(),start+size));return listResponse(pageRows,rows.size());
    }
    @GetMapping("/templates/{id}") public ApiResponse<JsonNode> template(@PathVariable String id){validateTemplatePath(id);return ApiResponse.ok(service.detail("templates",id));}
    @PostMapping("/templates") public ApiResponse<JsonNode> createTemplate(@RequestBody(required=false) ObjectNode body){
        requireBody(body);String code=requiredString(body,"code_template"),name=requiredString(body,"name");validateTemplateCode(code);validateName(name,100,"Tên biểu mẫu");if(body.has("description"))optionalText(body,"description",500);if(body.has("version")){String version=typedString(body,"version");if(!version.matches("\\d+(\\.\\d+){0,2}"))throw error("1004",400,"version không hợp lệ");}if(body.has("is_active")){String active=typedString(body,"is_active");if(!active.equals("0")&&!active.equals("1"))throw error("1004",400,"is_active không hợp lệ");}
        return ApiResponse.ok(service.create("templates",body));
    }
    @PutMapping("/templates/{id}") public ApiResponse<JsonNode> updateTemplate(@PathVariable String id,@RequestBody(required=false) ObjectNode body){
        validateTemplatePath(id);service.detail("templates",id);if(body==null||body.isEmpty())throw error("1002",400,"Cần ít nhất một trường để cập nhật");if(body.has("name"))validateName(typedString(body,"name"),100,"Tên biểu mẫu");if(body.has("description"))optionalText(body,"description",500);if(body.has("version")){String v=typedString(body,"version");if(!v.matches("\\d+(\\.\\d+){0,2}"))throw error("1004",400,"version không hợp lệ");}if(body.has("is_active")){String a=typedString(body,"is_active");if(!a.equals("0")&&!a.equals("1"))throw error("1004",400,"is_active không hợp lệ");}ObjectNode changes=body.deepCopy();changes.remove(List.of("id","code_template","created_at","updated_at","sections"));return ApiResponse.ok(service.update("templates",id,changes));
    }
    @DeleteMapping("/templates/{id}") public ApiResponse<Map<String,Object>> deleteTemplate(@PathVariable String id){validateTemplatePath(id);JsonNode current=service.detail("templates",id);boolean hasChildren=current.path("sections").isArray()&&!current.path("sections").isEmpty();service.delete("templates",id);if(hasChildren)throw error("9992",404,"Biểu mẫu không còn tồn tại");return ApiResponse.ok(Map.of());}

    @PostMapping("/templates/{templateId}/sections") public ApiResponse<JsonNode> addSection(@PathVariable String templateId,@RequestBody(required=false) ObjectNode body){requireBody(body);return ApiResponse.ok(service.addNested(templateId,null,"sections",body));}
    @PutMapping("/templates/{templateId}/sections/{sectionId}") public ApiResponse<JsonNode> editSection(@PathVariable String templateId,@PathVariable String sectionId,@RequestBody(required=false) ObjectNode body){requireBody(body);return ApiResponse.ok(service.updateNested(templateId,null,"sections",sectionId,body));}
    @DeleteMapping("/templates/{templateId}/sections/{sectionId}") public ApiResponse<Map<String,Object>> deleteSection(@PathVariable String templateId,@PathVariable String sectionId){service.deleteNested(templateId,null,"sections",sectionId);return ApiResponse.ok(Map.of());}
    @PutMapping("/templates/sections/{id}") public ApiResponse<JsonNode> editSectionById(@PathVariable String id,@RequestBody(required=false) ObjectNode body){requireBody(body);return ApiResponse.ok(service.updateSection(id,body));}
    @DeleteMapping("/templates/sections/{id}") public ApiResponse<Map<String,Object>> deleteSectionById(@PathVariable String id){service.deleteSection(id);return ApiResponse.ok(Map.of());}
    @PostMapping("/templates/{templateId}/sections/{sectionId}/questions") public ApiResponse<JsonNode> addQuestion(@PathVariable String templateId,@PathVariable String sectionId,@RequestBody(required=false) ObjectNode body){requireBody(body);return ApiResponse.ok(service.addNested(templateId,sectionId,"questions",body));}
    @PutMapping("/templates/{templateId}/sections/{sectionId}/questions/{questionId}") public ApiResponse<JsonNode> editQuestion(@PathVariable String templateId,@PathVariable String sectionId,@PathVariable String questionId,@RequestBody(required=false) ObjectNode body){requireBody(body);return ApiResponse.ok(service.updateNested(templateId,sectionId,"questions",questionId,body));}
    @DeleteMapping("/templates/{templateId}/sections/{sectionId}/questions/{questionId}") public ApiResponse<Map<String,Object>> deleteQuestion(@PathVariable String templateId,@PathVariable String sectionId,@PathVariable String questionId){service.deleteNested(templateId,sectionId,"questions",questionId);return ApiResponse.ok(Map.of());}
    @PostMapping("/templates/sections/{sectionId}/questions") public ApiResponse<JsonNode> createQuestionBySection(@PathVariable String sectionId,@RequestBody(required=false) ObjectNode body){requireBody(body);return ApiResponse.ok(service.createQuestion(sectionId,body));}
    @PutMapping("/templates/questions/{id}") public ApiResponse<JsonNode> editQuestionById(@PathVariable String id,@RequestBody(required=false) ObjectNode body){requireBody(body);return ApiResponse.ok(service.updateQuestion(id,body));}
    @DeleteMapping("/templates/questions/{id}") public ApiResponse<Map<String,Object>> deleteQuestionById(@PathVariable String id){service.deleteQuestion(id);return ApiResponse.ok(Map.of());}

    private Map<String,Object> branchDto(ChiNhanh b){Map<String,Object> r=new LinkedHashMap<>();r.put("id",b.getId());r.put("name",b.getName());r.put("address",b.getAddress());r.put("phone",b.getPhone()==null?"":b.getPhone());r.put("active",b.getActive());r.put("created_at",b.getCreatedAt());r.put("updated_at",b.getUpdatedAt());return r;}
    private Map<String,Object> roomDto(PhongKham r){JsonNode m=service.roomMeta(r.getId());Map<String,Object> d=new LinkedHashMap<>();d.put("id",r.getId());d.put("branch_id",r.getBranchId());d.put("department_id",m.hasNonNull("department_id")?m.path("department_id").asLong():null);d.put("description",m.hasNonNull("description")?m.path("description").asText():null);d.put("name",r.getName());d.put("created_at",r.getCreatedAt());d.put("updated_at",r.getUpdatedAt());return d;}
    private Map<String,Object> listResponse(List<?> rows){return listResponse(rows,rows.size());}
    private Map<String,Object> listResponse(List<?> rows,int total){Map<String,Object> m=new LinkedHashMap<>();m.put("code","1000");m.put("message","OK");m.put("data",rows);m.put("total",total);return m;}
    private ApiContractException error(String code,int status,String message){return new ApiContractException(code,status,message);}
    private void requireBody(ObjectNode body){if(body==null)throw error("1002",400,"Thiếu nội dung yêu cầu");}
    private JsonNode required(ObjectNode b,String k){if(!b.has(k)||b.path(k).isNull())throw error("1002",400,"Thiếu trường "+k);return b.path(k);}
    private String requiredString(ObjectNode b,String k){JsonNode n=required(b,k);if(!n.isTextual())throw error("1003",400,"Kiểu dữ liệu không hợp lệ: "+k);return n.asText();}
    private String typedString(ObjectNode b,String k){JsonNode n=b.path(k);if(!n.isTextual())throw error("1003",400,"Kiểu dữ liệu không hợp lệ: "+k);return n.asText();}
    private String optionalText(ObjectNode b,String k,int max){JsonNode n=b.path(k);if(n.isMissingNode()||n.isNull())return null;if(!n.isTextual())throw error("1003",400,"Kiểu dữ liệu không hợp lệ: "+k);if(n.asText().length()>max)throw error("1004",400,k+" vượt giới hạn");return n.asText();}
    private void validateName(String v,int max,String label){if(v.trim().isEmpty()||v.length()>max||v.contains("<"))throw error("1004",400,label+" không hợp lệ");}
    private void validateBranch(String n,String a,String p){validateName(n,100,"Tên chi nhánh");if(a.trim().isEmpty()||a.length()>255)throw error("1004",400,"Địa chỉ chi nhánh không hợp lệ");if(!validPhone(p))throw error("1004",400,"Số điện thoại không hợp lệ");}
    private boolean validPhone(String p){return p!=null&&p.matches("(?:0[35789]\\d{8}|02\\d{8,9})");}
    private void validateTemplatePath(String id){if(id.chars().anyMatch(Character::isWhitespace)||id.toUpperCase(Locale.ROOT).contains("%20")||id.startsWith("tmpl_"))throw error("1004",400,"Mã biểu mẫu không hợp lệ");}
    private void positiveId(long id){if(id<=0)throw error("1004",400,"ID phải là số dương");}
    private long pathId(String s){if(!s.matches("-?\\d+"))throw error("1003",400,"ID không hợp lệ");try{long n=Long.parseLong(s);if(n<=0)throw error("1004",400,"ID phải là số dương");return n;}catch(NumberFormatException e){throw error("1004",400,"ID vượt giới hạn");}}
    private long parseQueryId(String s){if(!s.matches("-?\\d+"))throw error("1003",400,"ID bộ lọc không hợp lệ");try{long n=Long.parseLong(s);if(n<=0)throw error("1004",400,"ID bộ lọc phải dương");return n;}catch(NumberFormatException e){throw error("1004",400,"ID bộ lọc vượt giới hạn");}}
    private void validateTemplateCode(String c){if(!c.matches("TMPL_[A-Z0-9_]{3,40}"))throw error("1004",400,"Mã biểu mẫu không hợp lệ");}
    private int pageNumber(String value,int fallback,int min,int max){if(value==null)return fallback;if(!value.matches("-?\\d+"))throw error("1003",400,"Tham số phân trang không hợp lệ");try{int n=Integer.parseInt(value);if(n<min||n>max)throw error("1004",400,"Tham số phân trang ngoài giới hạn");return n;}catch(NumberFormatException e){throw error("1004",400,"Tham số phân trang ngoài giới hạn");}}
}
