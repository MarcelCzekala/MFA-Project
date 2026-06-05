package com.mfa.project.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public class EnrollRegisterRequest {
    @NotNull
    private Integer nextId;
    @NotBlank
    private String nfcUid;
    @NotBlank
    private String fingerprintId;
    private String fullName;
    private String role;
    public Integer getNextId() {
        return nextId;
    }
    public void setNextId(Integer nextId) {
        this.nextId = nextId;
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
}
