package com.mfa.project.config;

import com.mfa.project.entity.Employee;
import com.mfa.project.repository.EmployeeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
public class SecurityConfig {

    // security config
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler authenticationSuccessHandler) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/verify/**",
                        "/api/enroll/**",
                        "/ws-native",
                        "/ws-native/**",
                        "/ws/**"
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/error", "/employee/**").permitAll()
                        .requestMatchers("/api/verify/**", "/api/enroll/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/admin/users", "/admin/users/**", "/admin/employees", "/admin/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**", "/admin/users/**", "/admin/employees/**").hasRole("ADMIN")
                        .requestMatchers("/api/logs/**", "/ws-native", "/ws-native/**", "/ws/**").hasAnyRole("ADMIN", "TEAM_LEADER")
                        .requestMatchers("/admin", "/admin/**").hasAnyRole("ADMIN", "TEAM_LEADER", "STAFF")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    // get user
    @Bean
    public UserDetailsService userDetailsService(EmployeeRepository employeeRepository) {
        return username -> {
            Employee employee = employeeRepository.findByLogin(username)
                    .filter(Employee::isActive)
                    .filter(user -> StringUtils.hasText(user.getPassword()))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return User.withUsername(employee.getLogin())
                    .password(employee.getPassword())
                    .roles(employee.getRoleEnum().name())
                    .disabled(!employee.isActive())
                    .build();
        };
    }

    // redirect after login
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication == null) {
                throw new AuthenticationCredentialsNotFoundException("Authentication is required");
            }

            String targetUrl = "/admin";
            response.sendRedirect(request.getContextPath() + targetUrl);
        };
    }

    private static boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> List.of(authorities).contains(authority));
    }
}
