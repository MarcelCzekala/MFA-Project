package com.mfa.project.dto;
public class QrVerifyRequest {
    private String token;
    private String location;
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
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
}