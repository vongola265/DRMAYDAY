package com.cnw.medical_system.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class ApiContractException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiContractException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
