package com.mfa.project.service;
import com.mfa.project.dto.VerifyResponse;
import com.mfa.project.entity.Employee;
import com.mfa.project.entity.AccessLog;
import com.mfa.project.repository.AccessLogRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
@Service
public class VerificationService {
    private final EmployeeService employeeService;
    private final LogService logService;
    private final AccessLogRepository accessLogRepository;
    private volatile Employee pendingCameraEmployee = null;
    private volatile long pendingCameraTimestamp = 0;
    private volatile String pendingCameraStatus = null;
    public VerificationService(EmployeeService employeeService, LogService logService, AccessLogRepository accessLogRepository) {
        this.employeeService = employeeService;
        this.logService = logService;
        this.accessLogRepository = accessLogRepository;
    }
    // store user in memory
    public void cacheDumbCameraUser(Employee employee) {
        this.pendingCameraEmployee = employee;
        this.pendingCameraTimestamp = System.currentTimeMillis();
    }
    // cache failed qr status
    public void cacheDumbCameraStatus(Employee employee, String status) {
        this.pendingCameraEmployee = employee;
        this.pendingCameraStatus = status;
        this.pendingCameraTimestamp = System.currentTimeMillis();
    }
    public Employee getPendingCameraEmployee() {
        return this.pendingCameraEmployee;
    }
    public long getPendingCameraTimestamp() {
        return this.pendingCameraTimestamp;
    }
    public String getPendingCameraStatus() {
        return this.pendingCameraStatus;
    }
    // consume and clear user
    public void clearPendingCameraUser() {
        this.pendingCameraEmployee = null;
        this.pendingCameraStatus = null;
    }
    public VerifyResponse verifyMfa(String nfcUid, String fingerprintId, String location) {
        if (location == null) location = "Entrance"; 
        boolean hasNfc = nfcUid != null && !nfcUid.isEmpty();
        boolean hasFp = fingerprintId != null && !fingerprintId.isEmpty();
        Employee employee = null;
        if (hasNfc && hasFp) {
            Optional<Employee> byNfc = employeeService.findByNfcUid(nfcUid);
            if (byNfc.isEmpty()) {
                logService.saveAndPush(null, "MFA", location, "DENIED", "Invalid RFID Card");
                return new VerifyResponse(false, "Invalid RFID Card", null);
            }
            Optional<Employee> match = employeeService.findByFingerprintAndNfc(fingerprintId, nfcUid);
            if (match.isEmpty()) {
                logService.saveAndPush(null, "MFA", location, "DENIED", "Invalid Fingerprint");
                return new VerifyResponse(false, "Invalid Fingerprint", null);
            }
            employee = match.get();
        } else if (hasNfc) {
            Optional<Employee> byNfc = employeeService.findByNfcUid(nfcUid);
            if (byNfc.isEmpty()) {
                logService.saveAndPush(null, "MFA", location, "DENIED", "Invalid RFID Card");
                return new VerifyResponse(false, "Invalid RFID Card", null);
            }
            employee = byNfc.get();
        } else if (hasFp) {
            Optional<Employee> byFp = employeeService.findByFingerprintId(fingerprintId);
            if (byFp.isEmpty()) {
                logService.saveAndPush(null, "MFA", location, "DENIED", "Invalid Fingerprint");
                return new VerifyResponse(false, "Invalid Fingerprint", null);
            }
            employee = byFp.get();
        } else {
            logService.saveAndPush(null, "MFA", location, "DENIED", "Invalid Credential");
            return new VerifyResponse(false, "Invalid Credential", null);
        }
        if (!employee.isActive()) {
            logService.saveAndPush(employee, "MFA", location, "DENIED", "Invalid Credential");
            return new VerifyResponse(false, "Invalid Credential", null);
        }
        Employee.Role role = employee.getRoleEnum();
        int providedFactors = (hasNfc ? 1 : 0) + (hasFp ? 1 : 0);
        if (location.equals("Entrance") || location.equals("Kitchen")) {
            if (providedFactors < 1) {
                logService.saveAndPush(employee, "MFA", location, "DENIED", "Invalid Credential");
                return new VerifyResponse(false, "Invalid Credential", null);
            }
        } else if (location.equals("Warehouse")) {
            if (providedFactors < 2) {
                logService.saveAndPush(employee, "MFA", location, "DENIED", "Invalid Fingerprint");
                return new VerifyResponse(false, "Invalid Fingerprint", null);
            }
            if (role != Employee.Role.TEAM_LEADER && role != Employee.Role.IT_DEPT && role != Employee.Role.ADMIN) {
                logService.saveAndPush(employee, "MFA", location, "DENIED", "Unauthorized Role");
                return new VerifyResponse(false, "Unauthorized Role", null);
            }
        } else if (location.equals("Archive")) {
            if (providedFactors < 2) {
                logService.saveAndPush(employee, "MFA", location, "DENIED", "Invalid Fingerprint");
                return new VerifyResponse(false, "Invalid Fingerprint", null);
            }
            if (role != Employee.Role.TEAM_LEADER && role != Employee.Role.ADMIN) {
                logService.saveAndPush(employee, "MFA", location, "DENIED", "Unauthorized Role");
                return new VerifyResponse(false, "Unauthorized Role", null);
            }
        } else {
            logService.saveAndPush(employee, "MFA", location, "DENIED", "Invalid Credential");
            return new VerifyResponse(false, "Invalid Credential", null);
        }
        String logLoc = location.equals("Archive") ? "Archive 1" : location;
        logService.saveAndPush(employee, "MFA", logLoc, "SUCCESS", "Access granted to " + location);
        return new VerifyResponse(true, "Access granted", employee.getFullName());
    }
    public boolean checkCardExists(String nfcUid) {
        return employeeService.findByNfcUid(nfcUid).isPresent();
    }
    public boolean isQrScannedRecently(String nfcUid) {
        Optional<Employee> empOpt = employeeService.findByNfcUid(nfcUid);
        if (empOpt.isEmpty()) return false;
        LocalDateTime fiveMinsAgo = LocalDateTime.now().minusMinutes(5);
        return accessLogRepository.findFirstByEmployeeIdAndMethodAndStatusAndTimestampAfterOrderByTimestampDesc(
                empOpt.get().getId(), "QR", "SUCCESS", fiveMinsAgo).isPresent();
    }
}
