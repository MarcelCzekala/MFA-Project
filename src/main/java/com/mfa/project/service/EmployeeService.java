package com.mfa.project.service;

import com.mfa.project.dto.EmployeeForm;
import com.mfa.project.dto.EnrollRegisterRequest;
import com.mfa.project.dto.UserUpdateRequest;
import com.mfa.project.entity.Employee;
import com.mfa.project.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class EmployeeService {

    private static final int SLOT_MIN = 1;
    private static final int SLOT_MAX = 127;
    private static final Pattern NUMERIC = Pattern.compile("^\\d+$");

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> findById(Long id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> findByFingerprintId(String fingerprintId) {
        return employeeRepository.findByFingerprintId(fingerprintId);
    }

    public Optional<Employee> findByNfcUid(String nfcUid) {
        return employeeRepository.findByNfcUid(nfcUid);
    }

    public Optional<Employee> findByFingerprintAndNfc(String fingerprintId, String nfcUid) {
        return employeeRepository.findByFingerprintIdAndNfcUid(fingerprintId, nfcUid);
    }

    public Employee createEmployee(EmployeeForm form) {
        Employee employee = new Employee();
        employee.setFullName(form.getFullName().trim());
        employee.setRole(form.getRole().trim());
        employee.setNfcUid(blankToNull(form.getNfcUid()));
        employee.setFingerprintId(blankToNull(form.getFingerprintId()));
        employee.setQrSecret(form.getQrSecret().trim());
        employee.setActive(form.isActive());
        return employeeRepository.save(employee);
    }

    public Employee registerFromDevice(EnrollRegisterRequest request) {
        Employee employee = new Employee();
        String name = StringUtils.hasText(request.getFullName())
                ? request.getFullName().trim()
                : "User " + request.getNextId();
        String role = StringUtils.hasText(request.getRole()) ? request.getRole().trim() : "Staff";
        employee.setFullName(name);
        employee.setRole(role);
        employee.setNfcUid(request.getNfcUid().trim());
        employee.setFingerprintId(request.getFingerprintId().trim());
        employee.setQrSecret("QR-" + request.getNextId());
        employee.setActive(true);
        return employeeRepository.save(employee);
    }

    public Employee updateUser(Long id, UserUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        employee.setFullName(request.getFullName().trim());
        employee.setRole(request.getRole().trim());
        employee.setNfcUid(blankToNull(request.getNfcUid()));
        employee.setFingerprintId(blankToNull(request.getFingerprintId()));
        employee.setQrSecret(request.getQrSecret().trim());
        employee.setActive(request.isActive());
        return employeeRepository.save(employee);
    }

    public int computeNextFingerprintSlot() {
        int maxUsed = 0;
        for (Employee e : employeeRepository.findAll()) {
            String fp = e.getFingerprintId();
            if (!StringUtils.hasText(fp) || !NUMERIC.matcher(fp.trim()).matches()) {
                continue;
            }
            try {
                int v = Integer.parseInt(fp.trim());
                if (v >= SLOT_MIN && v <= SLOT_MAX) {
                    maxUsed = Math.max(maxUsed, v);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        int next = maxUsed + 1;
        if (next < SLOT_MIN) {
            next = SLOT_MIN;
        }
        if (next > SLOT_MAX) {
            next = SLOT_MAX;
        }
        return next;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
