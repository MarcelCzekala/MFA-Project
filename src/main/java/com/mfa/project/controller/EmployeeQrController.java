package com.mfa.project.controller;

import com.mfa.project.entity.QrToken;
import com.mfa.project.service.QrTokenService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/employee")
public class EmployeeQrController {

    private final QrTokenService qrTokenService;

    public EmployeeQrController(QrTokenService qrTokenService) {
        this.qrTokenService = qrTokenService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "employee-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam Long employeeId) {
        return "redirect:/employee/qr/" + employeeId;
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
}