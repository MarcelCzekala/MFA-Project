package com.mfa.project.service;

import com.mfa.project.entity.AccessLog;
import com.mfa.project.repository.AccessLogRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    public AccessLogService(AccessLogRepository accessLogRepository) {
        this.accessLogRepository = accessLogRepository;
    }

    public List<AccessLog> getAllLogs() {
        return accessLogRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(AccessLog::getTimestamp).reversed())
                .toList();
    }
}
