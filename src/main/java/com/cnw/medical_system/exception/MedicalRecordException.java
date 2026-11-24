package com.cnw.medical_system.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordException extends RuntimeException {

    HttpStatus status;
    String code;

    public MedicalRecordException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static MedicalRecordException badRequest(String code, String message) {
        return new MedicalRecordException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static MedicalRecordException forbidden(String message) {
        return new MedicalRecordException(HttpStatus.FORBIDDEN, "1009", message);
    }

    public static MedicalRecordException notFound(String message) {
        return new MedicalRecordException(HttpStatus.NOT_FOUND, "9992", message);
    }

    public static MedicalRecordException conflict(String code, String message) {
        return new MedicalRecordException(HttpStatus.CONFLICT, code, message);
    }
}
