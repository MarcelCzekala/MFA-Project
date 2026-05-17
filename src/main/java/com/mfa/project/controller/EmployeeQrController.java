package com.mfa.project.controller;

import com.mfa.project.entity.Employee;
import com.mfa.project.entity.QrToken;
import com.mfa.project.repository.EmployeeRepository;
import com.mfa.project.service.EmployeeAuthService;
import com.mfa.project.service.QrTokenService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/employee")
public class EmployeeQrController {

    private final QrTokenService qrTokenService;
    private final EmployeeAuthService employeeAuthService;
    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public EmployeeQrController(QrTokenService qrTokenService,
                                EmployeeAuthService employeeAuthService,
                                EmployeeRepository employeeRepository,
                                BCryptPasswordEncoder passwordEncoder) {
        this.qrTokenService = qrTokenService;
        this.employeeAuthService = employeeAuthService;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "employee-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String login,
                        @RequestParam String password,
                        Model model) {

        Optional<Employee> employeeOptional = employeeAuthService.login(login, password);

        if (employeeOptional.isEmpty()) {
            model.addAttribute("error", "Nieprawidłowy login lub hasło.");
            return "employee-login";
        }

        Employee employee = employeeOptional.get();

        return "redirect:/employee/qr/" + employee.getId();
    }

    @GetMapping("/qr/{employeeId}")
    public String qrPage(@PathVariable Long employeeId, Model model) {
        QrToken qrToken = qrTokenService.generateTokenForEmployee(employeeId);

        String qrContent = "QR_TOKEN:" + qrToken.getToken();
        String qrBase64 = qrTokenService.generateQrBase64(qrContent);

        model.addAttribute("employeeId", employeeId);
        model.addAttribute("token", qrToken.getToken());
        model.addAttribute("expiresAt", qrToken.getExpiresAt());
        model.addAttribute("qrBase64", qrBase64);

        return "employee-qr";
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
        return "employee-login";
    }
}