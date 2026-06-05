package com.mfa.project.repository;
import com.mfa.project.entity.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    java.util.Optional<AccessLog> findFirstByEmployeeIdAndMethodAndStatusAndTimestampAfterOrderByTimestampDesc(
            Long employeeId, String method, String status, java.time.LocalDateTime timestamp);
}
