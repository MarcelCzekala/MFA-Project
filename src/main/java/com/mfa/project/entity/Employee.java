package com.mfa.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role;

    @Column(unique = true)
    private String nfcUid;

    @Column(unique = true)
    private String fingerprintId;

    @Column(unique = true)
    private String qrSecret;

    @Column(unique = true)
    private String login;

    private String password;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AccessLog> logs = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<QrToken> qrTokens = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getNfcUid() { return nfcUid; }
    public void setNfcUid(String nfcUid) { this.nfcUid = nfcUid; }
    public String getFingerprintId() { return fingerprintId; }
    public void setFingerprintId(String fingerprintId) { this.fingerprintId = fingerprintId; }
    public String getQrSecret() { return qrSecret; }
    public void setQrSecret(String qrSecret) { this.qrSecret = qrSecret; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    @JsonProperty("active")
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public List<AccessLog> getLogs() { return logs; }
    public void setLogs(List<AccessLog> logs) { this.logs = logs; }
    public List<QrToken> getQrTokens() { return qrTokens; }
    public void setQrTokens(List<QrToken> qrTokens) { this.qrTokens = qrTokens; }
}