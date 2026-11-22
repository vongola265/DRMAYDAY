package com.cnw.medical_system.service;

import lombok.Getter;

@Getter
public class MaydayApiException extends RuntimeException {
    private final String code;

    public MaydayApiException(String code, String message) {
        super(message);
        this.code = code;
    }
}
