package com.cnw.medical_system.exception;

import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.cnw.medical_system.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Set<String> MISSING_CODES = Set.of("NotBlank", "NotNull", "NotEmpty");

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
        if (path.toLowerCase(Locale.ROOT).contains("%20") || path.chars().anyMatch(Character::isWhitespace)) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("1004", "Đường dẫn không hợp lệ", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>("9992", "Không tìm thấy dữ liệu", null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> malformed(HttpMessageNotReadableException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("Required request body is missing")) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("1002", "Thiếu nội dung yêu cầu", null));
        }
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.time.format.DateTimeParseException) {
                return ResponseEntity.badRequest().body(new ApiResponse<>("1004", "Định dạng ngày tháng không hợp lệ", null));
            }
            cause = cause.getCause();
        }
        return ResponseEntity.badRequest().body(new ApiResponse<>("1003", "JSON hoặc kiểu dữ liệu không hợp lệ", null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> unsupported(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.badRequest().body(new ApiResponse<>("1003", "Content-Type không được hỗ trợ", null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> typeMismatch(MethodArgumentTypeMismatchException e) {
        String value = e.getValue() == null ? "" : e.getValue().toString();
        String code = value.matches("-?\\d+") ? "1004" : "1003";
        return ResponseEntity.badRequest().body(new ApiResponse<>(code, "Tham số không hợp lệ", null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> invalid(MethodArgumentNotValidException e) {
        var errors = e.getBindingResult().getFieldErrors();
        if (errors.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("1004", "Dữ liệu không hợp lệ", null));
        }
        // Nếu giá trị gửi lên là null hoặc "" (rỗng) và vi phạm NotBlank/NotNull/NotEmpty -> 1002
        // Nếu giá trị là khoảng trắng "   " -> 1004
        FieldError missingError = errors.stream()
                .filter(err -> err.getCode() != null && MISSING_CODES.contains(err.getCode()))
                .filter(err -> err.getRejectedValue() == null || "".equals(err.getRejectedValue()))
                .findFirst()
                .orElse(null);
        if (missingError != null) {
            String message = missingError.getDefaultMessage() != null ? missingError.getDefaultMessage()
                    : missingError.getField() + " không được để trống";
            return ResponseEntity.badRequest().body(new ApiResponse<>("1002", message, null));
        }
        FieldError error = errors.get(0);
        String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getField() + " không hợp lệ";
        return ResponseEntity.badRequest().body(new ApiResponse<>("1004", message, null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> constraint(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>("9996", "Dữ liệu bị trùng hoặc vi phạm ràng buộc", null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiResponse<>("1004", e.getMessage(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>("1010", e.getMessage(), null));
    }

    // Bắt các lỗi nghiệp vụ đang ném RuntimeException trong Service
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuntime(RuntimeException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Lỗi xử lý nghiệp vụ";
        String lower = msg.toLowerCase(Locale.ROOT);

        if (lower.contains("không có quyền") || lower.contains("bị khóa") || lower.contains("tự khóa")
                || lower.contains("tự xóa") || lower.contains("duy nhất") || lower.contains("thu hồi quyền")
                || lower.contains("tự thay đổi")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>("1009", msg, null));
        }
        if (lower.contains("không tồn tại") || lower.contains("không tìm thấy")
                || lower.contains("chưa được đăng ký") || lower.contains("chưa được gán")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>("9992", msg, null));
        }
        if (lower.contains("đã tồn tại") || lower.contains("đã được sử dụng") || lower.contains("trùng với vai trò")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>("9996", msg, null));
        }
        if (lower.contains("mật khẩu") && (lower.contains("không chính xác") || lower.contains("sai"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>("9995", msg, null));
        }
        if (lower.contains("không được để trống")) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("1002", msg, null));
        }

        return ResponseEntity.badRequest().body(new ApiResponse<>("1004", msg, null));
    }
}
