package com.mfa.project.controller;

import com.mfa.project.dto.UserUpdateRequest;
import com.mfa.project.entity.Employee;
import com.mfa.project.repository.EmployeeRepository;
import com.mfa.project.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserController {

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;

    public UserController(EmployeeService employeeService, EmployeeRepository employeeRepository) {
        this.employeeService = employeeService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/api/enroll/next-available-id")
    public ResponseEntity<Map<String, Integer>> getNextId() {
        Integer maxId = employeeRepository.findMaxFingerprintId();
        int next = (maxId == null) ? 1 : maxId + 1;
        return ResponseEntity.ok(Map.of("nextId", next));
    }

    @GetMapping("/api/verify/card/{uid}")
    public ResponseEntity<Map<String, Boolean>> checkCard(@PathVariable String uid) {
        boolean exists = employeeRepository.existsByNfcUid(uid);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PutMapping("/api/users/{id}")
    public ResponseEntity<Employee> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        try {
            Employee updated = employeeService.updateUser(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        employeeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}