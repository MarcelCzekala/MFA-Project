package com.mfa.project.dto;
import jakarta.validation.constraints.NotBlank;
public class MfaVerifyRequest {
    private String nfcUid;
    private String fingerprintId;
    private String location;
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
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
}
