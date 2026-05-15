package com.mfa.project.controller;

import com.mfa.project.dto.EmployeeForm;
import com.mfa.project.service.AccessLogService;
import com.mfa.project.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final EmployeeService employeeService;
    private final AccessLogService accessLogService;

    public AdminController(EmployeeService employeeService, AccessLogService accessLogService) {
        this.employeeService = employeeService;
        this.accessLogService = accessLogService;
    }

    @GetMapping("/admin")
    public String adminPage(Model model) {
        if (!model.containsAttribute("employeeForm")) {
            model.addAttribute("employeeForm", new EmployeeForm());
        }
        if (!model.containsAttribute("showAddEmployeeForm")) {
            model.addAttribute("showAddEmployeeForm", false);
        }
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("logs", accessLogService.getAllLogs());
        return "admin";
    }

    @PostMapping("/admin/employees")
    public String addEmployee(
            @Valid @ModelAttribute("employeeForm") EmployeeForm employeeForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.employeeForm", bindingResult);
            redirectAttributes.addFlashAttribute("employeeForm", employeeForm);
            redirectAttributes.addFlashAttribute("showAddEmployeeForm", true);
            redirectAttributes.addFlashAttribute("error", "Please correct the form errors.");
            return "redirect:/admin";
        }
        try {
            employeeService.createEmployee(employeeForm);
            redirectAttributes.addFlashAttribute("success", "The employee was saved successfully.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error",
                    "Could not save the employee: duplicate NFC UID, Fingerprint ID, or QR Secret.");
            redirectAttributes.addFlashAttribute("employeeForm", employeeForm);
            redirectAttributes.addFlashAttribute("showAddEmployeeForm", true);
        }
        return "redirect:/admin";
    }
}
