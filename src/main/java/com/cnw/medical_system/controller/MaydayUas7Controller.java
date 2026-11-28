package com.cnw.medical_system.controller;

import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.factory.ApiResponseFactory;
import com.cnw.medical_system.service.MaydayAuthService;
import com.cnw.medical_system.service.MaydayUas7Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/uas7-scores")
@RequiredArgsConstructor
public class MaydayUas7Controller {
    private final MaydayAuthService auth;
    private final MaydayUas7Service uas7;
    private final ApiResponseFactory apiResponseFactory;

    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<Map<String,Object>>> eligibility(
            @RequestHeader(value="Authorization",required=false) String authorization) {
        return apiResponseFactory.success(uas7.eligibility(auth.requireAccount(authorization)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String,Object>>> create(
            @RequestHeader(value="Authorization",required=false) String authorization,
            @RequestBody Map<String,Object> body) {
        return apiResponseFactory.success(uas7.create(auth.requireAccount(authorization),body));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<Map<String,Object>>>> list(
            @PathVariable String patientId,
            @RequestHeader(value="Authorization",required=false) String authorization) {
        return apiResponseFactory.success(uas7.list(parseId(patientId),auth.requireAccount(authorization)));
    }

    @GetMapping("/week/{patientId}")
    public ResponseEntity<ApiResponse<Map<String,Object>>> week(
            @PathVariable String patientId,
            @RequestParam(value="start_date",required=false) String startDate,
            @RequestHeader(value="Authorization",required=false) String authorization) {
        return apiResponseFactory.success(uas7.week(parseId(patientId),auth.requireAccount(authorization),startDate));
    }

    private long parseId(String v){if(!v.matches("[0-9]+"))throw new com.cnw.medical_system.service.MaydayApiException("1003","patient_id sai định dạng");try{long n=Long.parseLong(v);if(n<=0)throw new com.cnw.medical_system.service.MaydayApiException("1004","patient_id phải lớn hơn 0");return n;}catch(NumberFormatException e){throw new com.cnw.medical_system.service.MaydayApiException("1004","patient_id vượt giới hạn");}}
}
