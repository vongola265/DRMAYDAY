package com.cnw.medical_system.exception;

import com.cnw.medical_system.dto.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiContractException.class)
    public ResponseEntity<ApiResponse<Void>> contract(ApiContractException e) {
        return ResponseEntity.status(e.getStatus()).body(new ApiResponse<>(e.getCode(), e.getMessage(), null));
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>("9992", e.getMessage(), null));
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> noRoute(NoResourceFoundException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.toLowerCase(java.util.Locale.ROOT).contains("%20") || path.chars().anyMatch(Character::isWhitespace)) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("1004", "Đường dẫn không hợp lệ", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>("9992", "Không tìm thấy dữ liệu", null));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> malformed(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ApiResponse<>("1003", "JSON hoặc kiểu dữ liệu không hợp lệ", null));
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> unsupported(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.badRequest().body(new ApiResponse<>("1002", "Thiếu hoặc không hỗ trợ nội dung yêu cầu", null));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> typeMismatch(MethodArgumentTypeMismatchException e) {
        String value = e.getValue() == null ? "" : e.getValue().toString();
        String code = value.matches("-?\\d+") ? "1004" : "1003";
        return ResponseEntity.badRequest().body(new ApiResponse<>(code, "Tham số không hợp lệ", null));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> invalid(MethodArgumentNotValidException e) {
        String message=e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+" không hợp lệ").orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(new ApiResponse<>("1004",message,null));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> constraint(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>("9996","Dữ liệu bị trùng hoặc vi phạm ràng buộc",null));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>("1004",e.getMessage(),null));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>("1010",e.getMessage(),null));
    }
}
