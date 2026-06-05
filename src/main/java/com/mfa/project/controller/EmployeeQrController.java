package com.mfa.project.controller;
import com.mfa.project.entity.Employee;
import com.mfa.project.repository.EmployeeRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
@Controller
@RequestMapping("/employee")
public class EmployeeQrController {
    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    public EmployeeQrController(EmployeeRepository employeeRepository,
                                BCryptPasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @GetMapping("/change-password/{employeeId}")
    public String changePasswordPage(@PathVariable Long employeeId, Model model) {
        model.addAttribute("employeeId", employeeId);
        return "employee-change-password";
    }
    @PostMapping("/change-password/{employeeId}")
    public String changePassword(@PathVariable Long employeeId,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        Optional<Employee> employeeOptional = employeeRepository.findById(employeeId);
        if (employeeOptional.isEmpty()) {
            model.addAttribute("error", "Pracownik nie istnieje.");
            model.addAttribute("employeeId", employeeId);
            return "employee-change-password";
        }
        Employee employee = employeeOptional.get();
        if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
            model.addAttribute("error", "Stare hasło jest nieprawidłowe.");
            model.addAttribute("employeeId", employeeId);
            return "employee-change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Nowe hasła nie są takie same.");
            model.addAttribute("employeeId", employeeId);
            return "employee-change-password";
        }
        if (newPassword.length() < 6) {
            model.addAttribute("error", "Nowe hasło musi mieć minimum 6 znaków.");
            model.addAttribute("employeeId", employeeId);
            return "employee-change-password";
        }
        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
        model.addAttribute("success", "Hasło zostało zmienione. Możesz zalogować się ponownie.");
        return "redirect:/login?logout";
    }
}