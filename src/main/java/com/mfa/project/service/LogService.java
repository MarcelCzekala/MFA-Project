package com.mfa.project.service;

import com.mfa.project.dto.LogEventDto;
import com.mfa.project.entity.AccessLog;
import com.mfa.project.entity.Employee;
import com.mfa.project.repository.AccessLogRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    private final AccessLogRepository accessLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LogService(AccessLogRepository accessLogRepository, SimpMessagingTemplate messagingTemplate) {
        this.accessLogRepository = accessLogRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public AccessLog saveAndPush(Employee employee, String method, String status, String message) {
        AccessLog log = new AccessLog();
        log.setEmployee(employee);
        if (employee != null) {
            employee.getLogs().add(log);
        }
        log.setMethod(method);
        log.setStatus(status);
        log.setMessage(message);
        AccessLog saved = accessLogRepository.save(log);

        String name = employee != null ? employee.getFullName() : "Unknown";
        LogEventDto event = new LogEventDto(
                saved.getId(),
                saved.getTimestamp(),
                saved.getMethod(),
                saved.getStatus(),
                name,
                saved.getMessage()
        );
        messagingTemplate.convertAndSend("/topic/logs", event);
        return saved;
    }
}
