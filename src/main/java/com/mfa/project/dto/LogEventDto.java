package com.mfa.project.dto;
import java.time.LocalDateTime;
public class LogEventDto {
    private Long id;
    private LocalDateTime timestamp;
    private String method;
    private String status;
    private String employeeName;
    private String message;
    private String location;
    public LogEventDto() {
    }
    public LogEventDto(Long id, LocalDateTime timestamp, String method, String status, String employeeName, String message, String location) {
        this.id = id;
        this.timestamp = timestamp;
        this.method = method;
        this.status = status;
        this.employeeName = employeeName;
        this.message = message;
        this.location = location;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getEmployeeName() {
        return employeeName;
    }
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
}
