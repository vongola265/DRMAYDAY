package com.cnw.medical_system.exception;

import com.cnw.medical_system.dto.response.ApiResponse;
import com.cnw.medical_system.service.MaydayApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMediaTypeNotSupportedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MaydayApiException.class)
    public ResponseEntity<ApiResponse<Void>> mayday(MaydayApiException exception) {
        return ResponseEntity.ok(error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(ApiContractException.class)
    public ResponseEntity<ApiResponse<Void>> contract(ApiContractException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("9992", exception.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> noRoute(
            NoResourceFoundException exception, HttpServletRequest request) {
        String path = path(request);
        if (path.toLowerCase(java.util.Locale.ROOT).contains("%20")
                || path.chars().anyMatch(Character::isWhitespace)) {
            return response(request, HttpStatus.BAD_REQUEST, "1004", "Đường dẫn không hợp lệ");
        }
        return response(request, HttpStatus.NOT_FOUND, "9992", "Không tìm thấy dữ liệu");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> malformed(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "1003", "JSON hoặc kiểu dữ liệu không hợp lệ");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> unsupported(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "1002", "Thiếu hoặc không hỗ trợ nội dung yêu cầu");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> typeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String value = exception.getValue() == null ? "" : exception.getValue().toString();
        String code = value.matches("-?\\d+") ? "1004" : "1003";
        return response(request, HttpStatus.BAD_REQUEST, code, "Tham số không hợp lệ");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ApiResponse<Void>> invalid(
            Exception exception, HttpServletRequest request) {
        String message = exception instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream().findFirst()
                    .map(fieldError -> fieldError.getField() + " không hợp lệ")
                    .orElse("Dữ liệu không hợp lệ")
                : "Dữ liệu không hợp lệ";
        return response(request, HttpStatus.BAD_REQUEST, "1004", message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> constraint(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        if (isMaydayRequest(request)) {
            return ResponseEntity.ok(error("1004", "Dữ liệu vi phạm ràng buộc"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("9996", "Dữ liệu bị trùng hoặc vi phạm ràng buộc"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> badRequest(
            IllegalArgumentException exception, HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "1004", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(
            IllegalStateException exception, HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "1010", exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> responseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String code = status == HttpStatus.NOT_FOUND ? "9992" : "1004";
        String message = exception.getReason() == null ? "Yêu cầu không hợp lệ" : exception.getReason();
        return response(request, status, code, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(
            Exception exception, HttpServletRequest request) {
        return response(request, HttpStatus.INTERNAL_SERVER_ERROR, "1010", "Lỗi xử lý yêu cầu");
    }

    private ResponseEntity<ApiResponse<Void>> response(
            HttpServletRequest request, HttpStatus status, String code, String message) {
        if (isMaydayRequest(request)) {
            return ResponseEntity.ok(error(code, message));
        }
        return ResponseEntity.status(status).body(error(code, message));
    }

    private boolean isMaydayRequest(HttpServletRequest request) {
        String path = path(request);
        return matches(path, "/api/auth")
                || matches(path, "/api/medical_records")
                || matches(path, "/api/lesion-episodes")
                || matches(path, "/api/lesion-locations")
                || matches(path, "/api/lesion-photos")
                || matches(path, "/api/image-attachments")
                || matches(path, "/_mayday_test");
    }

    private boolean matches(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }

    private String path(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        return !contextPath.isEmpty() && path.startsWith(contextPath)
                ? path.substring(contextPath.length())
                : path;
    }

    private ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
