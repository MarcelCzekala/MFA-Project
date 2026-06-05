package com.mfa.project.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public class EmployeeForm {
    @NotBlank(message = "Full name is required")
    private String fullName;
    @NotBlank(message = "Role is required")
    private String role;
    private String nfcUid;
    @Pattern(
            regexp = "^$|^(?:[1-9]|[1-9][0-9]|1[01][0-9]|12[0-7])$",
            message = "Fingerprint ID must be a number from 1 to 127 when provided")
    private String fingerprintId;
    @NotBlank(message = "QR secret is required")
    private String qrSecret;
    @NotBlank(message = "Login is required")
    private String login;
    @NotBlank(message = "Password is required")
    private String password;
    private boolean active = true;
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
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}