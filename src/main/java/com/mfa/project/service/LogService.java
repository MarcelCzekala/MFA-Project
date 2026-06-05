package com.mfa.project.service;
import com.mfa.project.entity.AccessLog;
import com.mfa.project.entity.Employee;
import org.springframework.stereotype.Service;
@Service
public class LogService {
    private final AccessLogService accessLogService;
    public LogService(AccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }
    public AccessLog saveAndPush(Employee employee, String method, String location, String status, String message) {
        return accessLogService.saveAndPush(employee, method, location, status, message);
    }
}
