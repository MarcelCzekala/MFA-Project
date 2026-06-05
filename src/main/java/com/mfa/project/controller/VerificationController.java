package com.mfa.project.controller;
import com.mfa.project.dto.MfaVerifyRequest;
import com.mfa.project.dto.QrVerifyRequest;
import com.mfa.project.dto.VerifyResponse;
import com.mfa.project.entity.Employee;
import com.mfa.project.entity.QrToken;
import com.mfa.project.repository.QrTokenRepository;
import com.mfa.project.service.LogService;
import com.mfa.project.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mfa.project.service.EmployeeService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
@RestController
@RequestMapping("/api/verify")
public class VerificationController {
    private final EmployeeService employeeService;
    private final VerificationService verificationService;
    private final LogService logService;
    private final QrTokenRepository qrTokenRepository;
    public VerificationController(EmployeeService employeeService, VerificationService verificationService, LogService logService, QrTokenRepository qrTokenRepository) {
        this.employeeService = employeeService;
        this.verificationService = verificationService;
        this.logService = logService;
        this.qrTokenRepository = qrTokenRepository;
    }
    @PostMapping
    public ResponseEntity<VerifyResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        VerifyResponse response = verificationService.verifyMfa(request.getNfcUid(), request.getFingerprintId(), request.getLocation());
        return ResponseEntity.ok(response);
    }
    @PostMapping("/alarm")
    public ResponseEntity<?> logHardwareAlarm(@RequestBody Map<String, String> payload) {
        String loc = payload.get("location");
        if ("Entrance".equals(loc) || "Kitchen".equals(loc)) {
            return ResponseEntity.ok().build();
        }
        logService.saveAndPush(null, "HARDWARE", loc != null ? loc : "Unknown", "DENIED", "SECURITY ALARM TRIGGERED");
        return ResponseEntity.ok().build();
    }
    @PostMapping("/camera-scan")
    public ResponseEntity<Map<String, Object>> cameraScan(@RequestBody QrVerifyRequest request) {
        String token = request.getToken();
        System.out.println("[QR SCANNER] Received dumb camera scan: " + token);
        Map<String, Object> response = new HashMap<>();
        Optional<QrToken> tokenOptional = qrTokenRepository.findByToken(token);
        if (tokenOptional.isEmpty()) {
            verificationService.cacheDumbCameraStatus(null, "UNKNOWN");
            response.put("message", "Unknown QR Code");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        QrToken qrToken = tokenOptional.get();
        Employee employee = qrToken.getEmployee();
        if (LocalDateTime.now().isAfter(qrToken.getExpiresAt()) || !qrToken.isActive()) {
            // cache failed qr status
            verificationService.cacheDumbCameraStatus(null, "USED");
            response.put("message", !qrToken.isActive() ? "Used QR Code" : "Expired QR Code");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (!employee.isActive()) {
            verificationService.cacheDumbCameraStatus(null, "UNKNOWN");
            response.put("message", "Employee account disabled");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        qrToken.setActive(false);
        qrTokenRepository.save(qrToken);
        verificationService.cacheDumbCameraStatus(employee, "SUCCESS");
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/qr-poll/1fa")
    public ResponseEntity<Map<String, Object>> pollQr(@RequestParam String location) {
        // poll qr status
        Employee pendingEmp = verificationService.getPendingCameraEmployee();
        String status = verificationService.getPendingCameraStatus();
        long ts = verificationService.getPendingCameraTimestamp();
        if (status != null) {
            if ((System.currentTimeMillis() - ts) > 15000) {
                verificationService.clearPendingCameraUser();
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            if ("USED".equals(status) || "UNKNOWN".equals(status)) {
                // abort on bad qr
                verificationService.clearPendingCameraUser();
                Map<String, Object> err = new HashMap<>();
                err.put("message", "USED".equals(status) ? "Used QR Code" : "Unknown QR Code");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
            }
            Employee emp = pendingEmp;
            Employee.Role role = emp.getRoleEnum();
            verificationService.clearPendingCameraUser();
            boolean allowed = true;
            // check user role
            if (location.equals("Warehouse") && role != Employee.Role.TEAM_LEADER && role != Employee.Role.IT_DEPT && role != Employee.Role.ADMIN) {
                allowed = false;
            } else if (location.equals("Archive") && role != Employee.Role.TEAM_LEADER && role != Employee.Role.ADMIN) {
                allowed = false;
            }
            if (!allowed) {
                logService.saveAndPush(emp, "QR", location, "DENIED", "Insufficient access level");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            String logLoc = location.equals("Archive") ? "Archive 2" : location;
            logService.saveAndPush(emp, "QR", logLoc, "SUCCESS", "Access Granted");
            Map<String, Object> res = new HashMap<>();
            res.put("granted", true);
            res.put("employeeName", emp.getFullName());
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    @PostMapping("/timeout")
    public ResponseEntity<?> logQrTimeout(@RequestBody Map<String, String> payload) {
        String loc = payload.get("location");
        logService.saveAndPush(null, "QR", loc != null ? loc : "Unknown", "DENIED", "QR Timeout");
        return ResponseEntity.ok().build();
    }
}
