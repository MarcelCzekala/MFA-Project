package com.mfa.project.service;

import com.mfa.project.dto.EmployeeForm;
import com.mfa.project.dto.EnrollRegisterRequest;
import com.mfa.project.dto.UserUpdateRequest;
import com.mfa.project.entity.Employee;
import com.mfa.project.repository.EmployeeRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final BCryptPasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, BCryptPasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Employee> getAllEmployees() { return employeeRepository.findAll(); }

    public Optional<Employee> findByFingerprintId(String fingerprintId) { return employeeRepository.findByFingerprintId(fingerprintId); }
    public Optional<Employee> findByNfcUid(String nfcUid) { return employeeRepository.findByNfcUid(nfcUid); }
    public Optional<Employee> findByFingerprintAndNfc(String fingerprintId, String nfcUid) { return employeeRepository.findByFingerprintIdAndNfcUid(fingerprintId, nfcUid); }

    public Employee createEmployee(EmployeeForm form) {
        Employee employee = new Employee();
        employee.setFullName(form.getFullName().trim());
        employee.setRole(form.getRole().trim());
        employee.setNfcUid(blankToNull(form.getNfcUid()));
        employee.setFingerprintId(blankToNull(form.getFingerprintId()));
        employee.setQrSecret(form.getQrSecret().trim());
        employee.setLogin(form.getLogin().trim());
        employee.setPassword(passwordEncoder.encode(form.getPassword()));
        employee.setActive(form.isActive());
        return employeeRepository.save(employee);
    }

    public Employee registerFromDevice(EnrollRegisterRequest request) {
        Employee employee = new Employee();
        String login = "user" + request.getNextId();
        employee.setFullName(StringUtils.hasText(request.getFullName()) ? request.getFullName().trim() : "User " + request.getNextId());
        employee.setRole("Staff");
        employee.setNfcUid(request.getNfcUid().trim());
        employee.setFingerprintId(request.getFingerprintId().trim());
        employee.setQrSecret("QR-" + request.getNextId());
        employee.setLogin(login);
        employee.setPassword(passwordEncoder.encode(login));
        employee.setActive(true);
        return employeeRepository.save(employee);
    }

    public Employee updateUser(Long id, UserUpdateRequest request) {
        Employee employee = employeeRepository.findById(id).orElseThrow();
        employee.setFullName(request.getFullName().trim());
        employee.setRole(request.getRole().trim());
        employee.setLogin(blankToNull(request.getLogin()));
        employee.setNfcUid(blankToNull(request.getNfcUid()));
        employee.setFingerprintId(blankToNull(request.getFingerprintId()));
        employee.setQrSecret(request.getQrSecret().trim());
        employee.setActive(request.isActive());
        if (StringUtils.hasText(request.getPassword())) employee.setPassword(passwordEncoder.encode(request.getPassword()));
        return employeeRepository.save(employee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
    }

    public int computeNextFingerprintSlot() {
        int maxUsed = 0;
        for (Employee e : employeeRepository.findAll()) {
            String fp = e.getFingerprintId();
            if (StringUtils.hasText(fp) && NUMERIC.matcher(fp.trim()).matches()) {
                int v = Integer.parseInt(fp.trim());
                if (v >= SLOT_MIN && v <= SLOT_MAX) maxUsed = Math.max(maxUsed, v);
            }
        }
        return Math.min(maxUsed + 1, SLOT_MAX);
    }

    private static String blankToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}