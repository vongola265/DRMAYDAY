package com.cnw.medical_system.exception;

public class ApiContractException extends RuntimeException {
    private final String code;
    private final int status;
    public ApiContractException(String code, int status, String message) { super(message); this.code = code; this.status = status; }
    public String getCode() { return code; }
    public int getStatus() { return status; }
}
