package com.mfa.project.dto;

import jakarta.validation.constraints.NotBlank;

public class MfaVerifyRequest {

    @NotBlank
    private String nfcUid;

    @NotBlank
    private String fingerprintId;

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
}
