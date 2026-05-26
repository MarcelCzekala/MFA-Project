package com.mfa.project.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // show login
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // redirect to home
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean consoleUser = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ROLE_TEAM_LEADER"));

        return consoleUser ? "redirect:/admin" : "redirect:/login?unauthorized";
    }
}
