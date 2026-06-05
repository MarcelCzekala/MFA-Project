package com.mfa.project.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
public class QrToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String token;
    private LocalDateTime expiresAt;
    private boolean active = true;
    @ManyToOne
    private Employee employee;
    public QrToken() {
    }
    public QrToken(String token, LocalDateTime expiresAt, Employee employee) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.employee = employee;
        this.active = true;
    }
    public Long getId() {
        return id;
    }
    public String getToken() {
        return token;
    }
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    public boolean isActive() {
        return active;
    }
    public Employee getEmployee() {
        return employee;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}