package com.mfa.project.dto;

public class QrVerifyRequest {
    private String token;

    public QrVerifyRequest() {
    }

    public QrVerifyRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}