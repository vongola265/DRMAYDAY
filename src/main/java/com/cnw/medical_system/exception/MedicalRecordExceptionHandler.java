package com.cnw.medical_system.exception;

import com.cnw.medical_system.controller.MedicalRecordController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = MedicalRecordController.class)
public class MedicalRecordExceptionHandler {

    @ExceptionHandler(MedicalRecordException.class)
    public ResponseEntity<Map<String, Object>> handleMedicalRecordException(MedicalRecordException ex) {
        return buildErrorResponse(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String code = "1004";
        String message = "Dữ liệu đầu vào không hợp lệ";
        if (fieldError != null) {
            message = fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : (fieldError.getField() + " không hợp lệ");
            String constraint = fieldError.getCode();
            if ("NotBlank".equals(constraint) || "NotNull".equals(constraint) || "NotEmpty".equals(constraint)) {
                code = "1002";
            }
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, code, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "1002", "Thiếu tham số bắt buộc: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "1003", "Sai kiểu dữ liệu tham số: " + ex.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "1003", "Nội dung JSON không hợp lệ hoặc bị thiếu");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("status", status.value());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
