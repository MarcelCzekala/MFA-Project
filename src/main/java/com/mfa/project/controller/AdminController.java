package com.mfa.project.controller;
import com.mfa.project.dto.EmployeeForm;
import com.mfa.project.dto.UserUpdateRequest;
import com.mfa.project.entity.Employee;
import com.mfa.project.service.AccessLogService;
import com.mfa.project.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.security.Principal;
import com.mfa.project.repository.EmployeeRepository;
import com.mfa.project.service.QrTokenService;
import com.mfa.project.entity.QrToken;
@Controller
public class AdminController {
    private final EmployeeService employeeService;
    private final AccessLogService accessLogService;
    private final QrTokenService qrTokenService;
    private final EmployeeRepository employeeRepository;
    public AdminController(EmployeeService employeeService,
                           AccessLogService accessLogService,
                           QrTokenService qrTokenService,
                           EmployeeRepository employeeRepository) {
        this.employeeService = employeeService;
        this.accessLogService = accessLogService;
        this.qrTokenService = qrTokenService;
        this.employeeRepository = employeeRepository;
    }
    @GetMapping("/admin")
    public String adminPage(Model model, Principal principal) {
        addAdminModel(model);
        model.addAttribute("employeeForm", new EmployeeForm());
        if (principal != null) {
            employeeRepository.findByLogin(principal.getName()).ifPresent(emp -> {
                QrToken qrToken = qrTokenService.generateTokenForEmployee(emp.getId());
                String qrContent = "QR_TOKEN:" + qrToken.getToken();
                String qrBase64 = qrTokenService.generateQrBase64(qrContent);
                model.addAttribute("employeeId", emp.getId());
                model.addAttribute("token", qrToken.getToken());
                model.addAttribute("expiresAt", qrToken.getExpiresAt());
                model.addAttribute("qrBase64", qrBase64);
            });
        }
        return "admin";
    }
    @PostMapping({"/admin/users", "/admin/employees"})
    public String createUser(@Valid @ModelAttribute("employeeForm") EmployeeForm form,
                             BindingResult result,
                             Model model) {
        if (result.hasErrors()) {
            addAdminModel(model);
            model.addAttribute("showAddEmployeeForm", true);
            return "admin";
        }
        try {
            employeeService.createEmployee(form);
        } catch (Exception e) {
            result.rejectValue("login", "error.employeeForm", "Login, NFC UID or Fingerprint ID already exists");
            addAdminModel(model);
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
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            employeeService.deleteEmployee(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    private void addAdminModel(Model model) {
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("logs", accessLogService.getAllLogs());
    }
}
