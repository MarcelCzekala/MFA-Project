package com.mfa.project.controller;

import com.mfa.project.entity.Employee;
import com.mfa.project.service.EmployeeService;
import com.mfa.project.service.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/verify")
public class UserController {

    private final EmployeeService employeeService;
    private final LogService logService;

    public UserController(EmployeeService employeeService, LogService logService) {
        this.employeeService = employeeService;
        this.logService = logService;
    }

    // check nfc
    @GetMapping("/card/{uid}")
    public ResponseEntity<Map<String, Boolean>> checkCard(@PathVariable String uid) {
        Optional<Employee> employeeOpt = employeeService.findByNfcUid(uid);
        boolean exists = employeeOpt.isPresent();

        if (!exists) {
            logService.saveAndPush(null, "MFA", "FAILURE", "Unauthorized access attempt: Unknown card UID " + uid);
        }

        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}