package com.mfa.project.dto;
import jakarta.validation.constraints.NotBlank;
public class UserUpdateRequest {
    @NotBlank
    private String fullName;
    @NotBlank
    private String role;
    private String nfcUid;
    private String fingerprintId;
    private String qrSecret;
    private boolean active = true;
    private String login;
    private String password;
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getNfcUid() {
        return nfcUid;
    }
    public void setNfcUid(String nfcUid) {
        this.nfcUid = nfcUid;
    }
    public String getFingerprintId() {
        return fingerprintId;
    }
    public void setFingerprintId(String fingerprintId) {
        this.fingerprintId = fingerprintId;
    }
    public String getQrSecret() {
        return qrSecret;
    }
    public void setQrSecret(String qrSecret) {
        this.qrSecret = qrSecret;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
