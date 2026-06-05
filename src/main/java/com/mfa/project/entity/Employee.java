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
    public enum Role {
        ADMIN,
        TEAM_LEADER,
        IT_DEPT,
        STAFF;
        public static Role from(String value) {
            if (value == null || value.isBlank()) {
                return STAFF;
            }
            String normalized = value.trim()
                    .toUpperCase()
                    .replace("ROLE_", "")
                    .replace("-", "_")
                    .replace(" ", "_");
            try {
                return Role.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                return STAFF;
            }
        }
        public String authority() {
            return "ROLE_" + name();
        }
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false)
    private String role = Role.STAFF.name();
    @Column(unique = true)
    private String nfcUid;
    @Column(unique = true)
    private String fingerprintId;
    @Column(unique = true)
    private String qrSecret;
    @Column(unique = true)
    private String login;
    @JsonIgnore
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
    public String getRole() { return getRoleEnum().name(); }
    public void setRole(String role) { this.role = Role.from(role).name(); }
    public void setRole(Role role) { this.role = role != null ? role.name() : Role.STAFF.name(); }
    @JsonIgnore
    public Role getRoleEnum() { return Role.from(role); }
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
