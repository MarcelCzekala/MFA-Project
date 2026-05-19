package com.mfa.project.controller;

import com.mfa.project.dto.EnrollRegisterRequest;
import com.mfa.project.entity.Employee;
import com.mfa.project.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/enroll")
public class EnrollController {

    private final EmployeeService employeeService;

    public EnrollController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/next-available-id")
    public ResponseEntity<Map<String, Integer>> getNextId() {
        int nextId = employeeService.computeNextFingerprintSlot();
        System.out.println("[ENROLL] ESP32 requested next ID. Returning: " + nextId);
        return ResponseEntity.ok(Map.of("nextId", nextId));
    }

    @PostMapping
    public ResponseEntity<?> register(@Valid @RequestBody EnrollRegisterRequest request) {
        System.out.println("[ENROLL] Received enrollment request from ESP32!");
        System.out.println(" - Next ID: " + request.getNextId());
        System.out.println(" - NFC UID: " + request.getNfcUid());
        System.out.println(" - Finger ID: " + request.getFingerprintId());

        try {
            Employee saved = employeeService.registerFromDevice(request);
            System.out.println("[ENROLL] SUCCESS! User created: " + saved.getLogin());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (DataIntegrityViolationException ex) {
            System.err.println("[ENROLL] FAILURE: Database conflict. User with this NFC or Fingerprint already exists.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Conflict: User already exists");
        } catch (Exception ex) {
            System.err.println("[ENROLL] CRITICAL FAILURE: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + ex.getMessage());
        }
    }
}