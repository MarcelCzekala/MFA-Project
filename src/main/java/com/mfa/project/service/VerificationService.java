package com.mfa.project.service;

import com.mfa.project.dto.VerifyResponse;
import com.mfa.project.entity.Employee;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VerificationService {

    private final EmployeeService employeeService;
    private final LogService logService;

    public VerificationService(EmployeeService employeeService, LogService logService) {
        this.employeeService = employeeService;
        this.logService = logService;
    }

    // verify mfa
    public VerifyResponse verifyMfa(String nfcUid, String fingerprintId) {
        Optional<Employee> match = employeeService.findByFingerprintAndNfc(fingerprintId, nfcUid);

        if (match.isEmpty()) {
            Optional<Employee> byFp = employeeService.findByFingerprintId(fingerprintId);
            Optional<Employee> byNfc = employeeService.findByNfcUid(nfcUid);
            if (byFp.isPresent() && byNfc.isPresent() && !byFp.get().getId().equals(byNfc.get().getId())) {
                logService.saveAndPush(null, "MFA", "FAILURE", "MFA verification failed: Card and fingerprint mismatch");
                return new VerifyResponse(false, "Access denied: card and fingerprint belong to different users", null);
            }
            logService.saveAndPush(null, "MFA", "FAILURE", "MFA verification failed: User not found");
            return new VerifyResponse(false, "Access denied: user not found", null);
        }

        Employee employee = match.get();
        if (!employee.isActive()) {
            logService.saveAndPush(employee, "MFA", "FAILURE", "MFA verification failed: User account is inactive");
            return new VerifyResponse(false, "Access denied: account inactive", null);
        }

        logService.saveAndPush(employee, "MFA", "SUCCESS", "NFC and fingerprint verified successfully");
        return new VerifyResponse(true, "Access granted", employee.getFullName());
    }

    // check card
    public boolean checkCardExists(String nfcUid) {
        return employeeService.findByNfcUid(nfcUid).isPresent();
    }
}