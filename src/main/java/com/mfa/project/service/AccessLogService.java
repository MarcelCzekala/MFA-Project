package com.mfa.project.service;

import com.mfa.project.dto.LogEventDto;
import com.mfa.project.entity.AccessLog;
import com.mfa.project.entity.Employee;
import com.mfa.project.repository.AccessLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AccessLogService(AccessLogRepository accessLogRepository, SimpMessagingTemplate messagingTemplate) {
        this.accessLogRepository = accessLogRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // get all logs
    public List<AccessLog> getAllLogs() {
        return accessLogRepository.findAll(Sort.by(
                Sort.Order.desc("timestamp"),
                Sort.Order.desc("id")
        ));
    }

    // save log
    public AccessLog saveAndPush(Employee employee, String method, String status, String message) {
        AccessLog log = new AccessLog();
        log.setEmployee(employee);
        log.setMethod(method);
        log.setStatus(status);
        log.setMessage(message);
        
        if ("QR".equalsIgnoreCase(method)) {
            log.setLocation("Entrance A");
        } else {
            log.setLocation("Document Warehouse");
        }

        AccessLog saved = accessLogRepository.save(log);
        messagingTemplate.convertAndSend("/topic/logs", toLogEvent(saved));
        return saved;
    }

    private static LogEventDto toLogEvent(AccessLog log) {
        Employee employee = log.getEmployee();
        String employeeName = employee != null ? employee.getFullName() : "Unknown";

        return new LogEventDto(
                log.getId(),
                log.getTimestamp(),
                log.getMethod(),
                log.getStatus(),
                employeeName,
                log.getMessage(),
                log.getLocation()
        );
    }
}
