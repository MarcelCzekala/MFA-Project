package com.mfa.project.controller;

import com.mfa.project.dto.EmployeeForm;
import com.mfa.project.dto.UserUpdateRequest;
import com.mfa.project.entity.Employee;
import com.mfa.project.repository.AccessLogRepository;
import com.mfa.project.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class AdminController {

    private final EmployeeService employeeService;
    private final AccessLogRepository accessLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AdminController(EmployeeService employeeService,
                           AccessLogRepository accessLogRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.employeeService = employeeService;
        this.accessLogRepository = accessLogRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("employeeForm", new EmployeeForm());
        model.addAttribute("logs", accessLogRepository.findAll());
        return "admin";
    }

    @PostMapping("/admin/employees")
    public String createUser(@Valid @ModelAttribute("employeeForm") EmployeeForm form,
                             BindingResult result,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("employees", employeeService.getAllEmployees());
            model.addAttribute("logs", accessLogRepository.findAll());
            model.addAttribute("showAddEmployeeForm", true);
            return "admin";
        }
        try {
            employeeService.createEmployee(form);
            messagingTemplate.convertAndSend("/topic/users", employeeService.getAllEmployees());
        } catch (Exception e) {
            result.rejectValue("login", "error.employeeForm", "Login, NFC UID or Fingerprint ID already exists");
            model.addAttribute("employees", employeeService.getAllEmployees());
            model.addAttribute("logs", accessLogRepository.findAll());
            model.addAttribute("showAddEmployeeForm", true);
            return "admin";
        }
        return "redirect:/admin";
    }

    @PutMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        try {
            Employee updated = employeeService.updateUser(id, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            employeeService.deleteEmployee(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}