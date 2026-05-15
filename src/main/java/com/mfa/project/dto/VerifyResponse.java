package com.mfa.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifyResponse {

    private final boolean accessGranted;
    private final String message;
    private final String employeeName;

    public VerifyResponse(boolean accessGranted, String message, String employeeName) {
        this.accessGranted = accessGranted;
        this.message = message;
        this.employeeName = employeeName;
    }

    public boolean isAccessGranted() {
        return accessGranted;
    }

    public String getMessage() {
        return message;
    }

    public String getEmployeeName() {
        return employeeName;
    }
}
