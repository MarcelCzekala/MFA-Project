package com.mfa.project.service;

import com.mfa.project.entity.Employee;
import com.mfa.project.repository.EmployeeRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmployeeAuthService {

    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public EmployeeAuthService(EmployeeRepository employeeRepository,
                               BCryptPasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Employee> login(String login, String password) {
        Optional<Employee> employeeOptional = employeeRepository.findByLogin(login);

        if (employeeOptional.isEmpty()) {
            return Optional.empty();
        }

        Employee employee = employeeOptional.get();

        if (!employee.isActive()) {
            return Optional.empty();
        }

        if (employee.getPassword() == null) {
            return Optional.empty();
        }

        boolean passwordMatches = passwordEncoder.matches(password, employee.getPassword());

        if (!passwordMatches) {
            return Optional.empty();
        }

        return Optional.of(employee);
    }
}