package com.mfa.project.dto;

import com.mfa.project.entity.Employee;

public class EmployeeEventDto {

    private Long id;
    private String fullName;
    private String role;
    private String login;
    private String nfcUid;
    private String fingerprintId;
    private String qrSecret;
    private boolean active;

    public EmployeeEventDto() {
    }

    public EmployeeEventDto(Long id,
                            String fullName,
                            String role,
                            String login,
                            String nfcUid,
                            String fingerprintId,
                            String qrSecret,
                            boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.role = role;
        this.login = login;
        this.nfcUid = nfcUid;
        this.fingerprintId = fingerprintId;
        this.qrSecret = qrSecret;
        this.active = active;
    }

    // map employee
    public static EmployeeEventDto from(Employee employee) {
        return new EmployeeEventDto(
                employee.getId(),
                employee.getFullName(),
                employee.getRole(),
                employee.getLogin(),
                employee.getNfcUid(),
                employee.getFingerprintId(),
                employee.getQrSecret(),
                employee.isActive()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
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
}
