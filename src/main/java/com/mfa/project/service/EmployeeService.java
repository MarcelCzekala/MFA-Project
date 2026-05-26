package com.mfa.project.service;

import com.mfa.project.dto.EmployeeEventDto;
import com.mfa.project.dto.EmployeeForm;
import com.mfa.project.dto.EnrollRegisterRequest;
import com.mfa.project.dto.UserUpdateRequest;
import com.mfa.project.entity.Employee;
import com.mfa.project.repository.EmployeeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

    public EmployeeService(EmployeeRepository employeeRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           SimpMessagingTemplate messagingTemplate) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.messagingTemplate = messagingTemplate;
    }

    // get all employees
    public List<Employee> getAllEmployees() { return employeeRepository.findAll(Sort.by(Sort.Direction.DESC, "id")); }

    // find by fingerprint
    public Optional<Employee> findByFingerprintId(String fingerprintId) { return employeeRepository.findByFingerprintId(fingerprintId); }
    // find by nfc
    public Optional<Employee> findByNfcUid(String nfcUid) { return employeeRepository.findByNfcUid(nfcUid); }
    // find by fingerprint nfc
    public Optional<Employee> findByFingerprintAndNfc(String fingerprintId, String nfcUid) { return employeeRepository.findByFingerprintIdAndNfcUid(fingerprintId, nfcUid); }

    // create employee
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
        Employee saved = employeeRepository.save(employee);
        broadcastNewUser(saved);
        return saved;
    }

    // register user
    public Employee registerFromDevice(EnrollRegisterRequest request) {
        Employee employee = new Employee();
        String identifier = StringUtils.hasText(request.getNfcUid()) ? request.getNfcUid().trim() : request.getFingerprintId().trim();
        employee.setFullName(StringUtils.hasText(request.getFullName()) ? request.getFullName().trim() : "User " + request.getNextId());
        employee.setRole("STAFF");
        employee.setNfcUid(request.getNfcUid().trim());
        employee.setFingerprintId(request.getFingerprintId().trim());
        employee.setQrSecret("QR-" + request.getNextId());
        employee.setLogin(identifier);
        employee.setPassword(passwordEncoder.encode(identifier));
        employee.setActive(true);
        Employee saved = employeeRepository.save(employee);
        broadcastNewUser(saved);
        return saved;
    }

    // update user
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

    // delete user
    @Transactional
    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
    }

    // get next fingerprint id
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

    // send new user event
    private void broadcastNewUser(Employee employee) {
        messagingTemplate.convertAndSend("/topic/users", EmployeeEventDto.from(employee));
    }

    // handle null
    private static String blankToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
