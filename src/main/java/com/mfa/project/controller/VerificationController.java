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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/verify")
public class VerificationController {

    private final VerificationService verificationService;
    private final QrTokenRepository qrTokenRepository;
    private final LogService logService;

    public VerificationController(VerificationService verificationService,
                                  QrTokenRepository qrTokenRepository,
                                  LogService logService) {
        this.verificationService = verificationService;
        this.qrTokenRepository = qrTokenRepository;
        this.logService = logService;
    }

    // check nfc and fingerprint
    @PostMapping
    public ResponseEntity<VerifyResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        VerifyResponse response = verificationService.verifyMfa(request.getNfcUid(), request.getFingerprintId());
        return ResponseEntity.ok(response);
    }

    // check qr token
    @PostMapping("/qr")
    public ResponseEntity<Map<String, Object>> verifyQrToken(@RequestBody QrVerifyRequest request) {
        System.out.println("[QR SCANNER] Received verification request with token: " + request.getToken());
        Map<String, Object> response = new HashMap<>();

        Optional<QrToken> tokenOptional = qrTokenRepository.findByTokenAndActiveTrue(request.getToken());

        if (tokenOptional.isEmpty()) {
            System.out.println("[QR SCANNER] Verification failed: Token not found or inactive");
            logService.saveAndPush(null, "QR", "FAILURE", "Invalid or inactive token");
            response.put("accessGranted", false);
            response.put("message", "Invalid token structure, expired or token not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        QrToken qrToken = tokenOptional.get();
        Employee employee = qrToken.getEmployee();

        if (LocalDateTime.now().isAfter(qrToken.getExpiresAt())) {
            System.out.println("[QR SCANNER] Verification failed: Token expired for employee " + employee.getFullName());
            logService.saveAndPush(employee, "QR", "FAILURE", "Token has expired");
            response.put("accessGranted", false);
            response.put("message", "Token has expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (!employee.isActive()) {
            System.out.println("[QR SCANNER] Verification failed: Account disabled for employee " + employee.getFullName());
            logService.saveAndPush(employee, "QR", "FAILURE", "Employee account is disabled");
            response.put("accessGranted", false);
            response.put("message", "Employee account is disabled");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        System.out.println("[QR SCANNER] Verification success for employee: " + employee.getFullName());
        logService.saveAndPush(employee, "QR", "SUCCESS", "Access granted via QR Code");

        response.put("accessGranted", true);
        response.put("message", "Access granted via QR Code");
        response.put("employeeName", employee.getFullName());

        return ResponseEntity.ok(response);
    }
}