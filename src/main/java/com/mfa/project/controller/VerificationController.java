package com.mfa.project.controller;

import com.mfa.project.dto.MfaVerifyRequest;
import com.mfa.project.dto.VerifyResponse;
import com.mfa.project.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verify")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping
    public ResponseEntity<VerifyResponse> verify(@Valid @RequestBody MfaVerifyRequest request) {
        VerifyResponse response = verificationService.verifyMfa(request.getNfcUid(), request.getFingerprintId());
        return ResponseEntity.ok(response);
    }
}
